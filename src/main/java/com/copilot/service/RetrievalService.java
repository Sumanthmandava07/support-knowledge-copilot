package com.copilot.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RetrievalService {

    private final VectorStore vectorStore;
    private final DocumentService documentService;

    private static final int TOP_K = 5;
    private static final int RRF_K = 60;

    public RetrievalService(
            VectorStore vectorStore,
            DocumentService documentService) {

        this.vectorStore = vectorStore;
        this.documentService = documentService;
    }

    public List<Document> search(String question) {

        // -----------------------------
        // 1. VECTOR SEARCH
        // -----------------------------

        SearchRequest searchRequest =
                SearchRequest.builder()
                        .query(question)
                        .topK(TOP_K)
                        .build();

        List<Document> vectorResults =
                vectorStore.similaritySearch(searchRequest);

        // -----------------------------
        // 2. BM25 SEARCH
        // -----------------------------

        List<Document> bm25Results =
                bm25Search(question);

        // -----------------------------
        // 3. RRF FUSION
        // -----------------------------

        Map<Document, Double> rrfScores =
                new HashMap<>();

        for (int i = 0;
             i < vectorResults.size();
             i++) {

            Document document =
                    vectorResults.get(i);

            int rank = i + 1;

            double score =
                    1.0 / (RRF_K + rank);

            rrfScores.merge(
                    document,
                    score,
                    Double::sum
            );
        }

        for (int i = 0;
             i < bm25Results.size();
             i++) {

            Document document =
                    bm25Results.get(i);

            int rank = i + 1;

            double score =
                    1.0 / (RRF_K + rank);

            rrfScores.merge(
                    document,
                    score,
                    Double::sum
            );
        }

        // -----------------------------
        // 4. CREATE RRF CANDIDATES
        // -----------------------------

        List<Document> candidates =
                new ArrayList<>(
                        rrfScores.keySet()
                );

        // -----------------------------
        // 5. RE-RANK CANDIDATES
        // -----------------------------

        Map<Document, Double> rerankScores =
                new HashMap<>();

        for (Document document : candidates) {

            double score =
                    calculateRerankScore(
                            question,
                            document,
                            rrfScores.get(document)
                    );

            rerankScores.put(
                    document,
                    score
            );
        }

        // -----------------------------
        // 6. SORT BY RE-RANK SCORE
        // -----------------------------

        candidates.sort(
                (a, b) ->
                        Double.compare(
                                rerankScores.get(b),
                                rerankScores.get(a)
                        )
        );

        // -----------------------------
        // 7. RETURN TOP 5
        // -----------------------------

        if (candidates.size() > TOP_K) {

            return candidates.subList(
                    0,
                    TOP_K
            );
        }

        return candidates;
    }

    // ==========================================
    // RE-RANKING
    // ==========================================

    private double calculateRerankScore(
            String question,
            Document document,
            double rrfScore) {

        String questionText =
                normalize(question);

        String documentText =
                normalize(document.getText());

        if (questionText.isBlank() ||
                documentText.isBlank()) {

            return rrfScore;
        }

        String[] queryTerms =
                questionText.split("\\s+");

        // --------------------------------
        // 1. Query term matching
        // --------------------------------

        int matchedTerms = 0;

        for (String queryTerm :
                queryTerms) {

            if (queryTerm.length() < 2) {
                continue;
            }

            if (documentText.contains(queryTerm)) {

                matchedTerms++;
            }
        }

        double queryCoverage = 0;

        if (queryTerms.length > 0) {

            queryCoverage =
                    (double) matchedTerms
                            / queryTerms.length;
        }

        // --------------------------------
        // 2. Exact phrase matching
        // --------------------------------

        double phraseScore = 0;

        if (documentText.contains(
                questionText)) {

            phraseScore = 1.0;
        }

        // --------------------------------
        // 3. Title matching
        // --------------------------------

        double titleScore =
                calculateMetadataMatch(
                        questionText,
                        document,
                        "title"
                );

        // --------------------------------
        // 4. Section matching
        // --------------------------------

        double sectionScore =
                calculateMetadataMatch(
                        questionText,
                        document,
                        "section"
                );

        // --------------------------------
        // 5. Word frequency
        // --------------------------------

        int totalMatches = 0;

        for (String queryTerm :
                queryTerms) {

            if (queryTerm.length() < 2) {
                continue;
            }

            totalMatches +=
                    countOccurrences(
                            documentText,
                            queryTerm
                    );
        }

        double frequencyScore =
                Math.min(
                        totalMatches / 10.0,
                        1.0
                );

        // --------------------------------
        // 6. Final score
        // --------------------------------

        double score =
                rrfScore * 10.0
                +
                queryCoverage * 3.0
                +
                phraseScore * 4.0
                +
                titleScore * 2.0
                +
                sectionScore * 2.0
                +
                frequencyScore;

        return score;
    }

    // ==========================================
    // METADATA MATCH
    // ==========================================

    private double calculateMetadataMatch(
            String question,
            Document document,
            String metadataKey) {

        Object value =
                document.getMetadata()
                        .get(metadataKey);

        if (value == null) {
            return 0;
        }

        String metadataText =
                normalize(
                        value.toString()
                );

        if (metadataText.isBlank()) {
            return 0;
        }

        String[] questionTerms =
                question.split("\\s+");

        int matches = 0;

        for (String term :
                questionTerms) {

            if (term.length() < 2) {
                continue;
            }

            if (metadataText.contains(term)) {

                matches++;
            }
        }

        if (matches == 0) {
            return 0;
        }

        return (double) matches
                / questionTerms.length;
    }

    // ==========================================
    // COUNT OCCURRENCES
    // ==========================================

    private int countOccurrences(
            String text,
            String word) {

        int count = 0;
        int index = 0;

        while ((index =
                text.indexOf(word, index)) != -1) {

            count++;

            index += word.length();
        }

        return count;
    }

    // ==========================================
    // BM25 SEARCH
    // ==========================================

    private List<Document> bm25Search(
            String question) {

        List<Document> documents =
                documentService.getChunks();

        if (documents.isEmpty()) {

            return new ArrayList<>();
        }

        String[] queryTerms =
                tokenize(question);

        int documentCount =
                documents.size();

        Map<String, Integer> documentFrequency =
                new HashMap<>();

        List<List<String>> tokenizedDocuments =
                new ArrayList<>();

        // --------------------------------
        // Tokenize documents
        // --------------------------------

        for (Document document :
                documents) {

            List<String> terms =
                    Arrays.asList(
                            tokenize(
                                    document.getText()
                            )
                    );

            tokenizedDocuments.add(
                    terms
            );

            Set<String> uniqueTerms =
                    new HashSet<>(terms);

            for (String term :
                    uniqueTerms) {

                documentFrequency.merge(
                        term,
                        1,
                        Integer::sum
                );
            }
        }

        // --------------------------------
        // BM25 parameters
        // --------------------------------

        double k1 = 1.5;
        double b = 0.75;

        double totalLength = 0;

        for (List<String> document :
                tokenizedDocuments) {

            totalLength +=
                    document.size();
        }

        double averageLength =
                totalLength / documentCount;

        // --------------------------------
        // Calculate BM25 score
        // --------------------------------

        Map<Document, Double> scores =
                new HashMap<>();

        for (int i = 0;
             i < documents.size();
             i++) {

            Document document =
                    documents.get(i);

            List<String> terms =
                    tokenizedDocuments.get(i);

            double score = 0;

            Map<String, Integer> termFrequency =
                    new HashMap<>();

            for (String term :
                    terms) {

                termFrequency.merge(
                        term,
                        1,
                        Integer::sum
                );
            }

            for (String queryTerm :
                    queryTerms) {

                int tf =
                        termFrequency.getOrDefault(
                                queryTerm,
                                0
                        );

                if (tf == 0) {
                    continue;
                }

                int df =
                        documentFrequency.getOrDefault(
                                queryTerm,
                                0
                        );

                double idf =
                        Math.log(
                                1 +
                                (documentCount
                                        - df
                                        + 0.5)
                                /
                                (df + 0.5)
                        );

                double numerator =
                        tf * (k1 + 1);

                double denominator =
                        tf +
                        k1 *
                        (
                                1 -
                                b +
                                b *
                                terms.size()
                                /
                                averageLength
                        );

                score +=
                        idf *
                        numerator /
                        denominator;
            }

            scores.put(
                    document,
                    score
            );
        }

        // --------------------------------
        // Sort BM25 results
        // --------------------------------

        List<Document> results =
                new ArrayList<>(
                        scores.keySet()
                );

        results.sort(
                (a, bDocument) ->
                        Double.compare(
                                scores.get(
                                        bDocument
                                ),
                                scores.get(a)
                        )
        );

        // --------------------------------
        // Remove zero-score documents
        // --------------------------------

        results.removeIf(
                document ->
                        scores.get(document) <= 0
        );

        if (results.size() > TOP_K) {

            return results.subList(
                    0,
                    TOP_K
            );
        }

        return results;
    }

    // ==========================================
    // TOKENIZER
    // ==========================================

    private String[] tokenize(
            String text) {

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9\\s]",
                        " "
                )
                .split("\\s+");
    }

    // ==========================================
    // NORMALIZE
    // ==========================================

    private String normalize(
            String text) {

        if (text == null) {

            return "";
        }

        return text
                .toLowerCase()
                .replaceAll(
                        "[^a-z0-9\\s]",
                        " "
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }
}