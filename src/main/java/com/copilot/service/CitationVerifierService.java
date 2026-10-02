package com.copilot.service;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CitationVerifierService {

    private static final double SUPPORT_THRESHOLD = 0.20;

    public boolean verify(
            String answer,
            List<Document> documents) {

        if (answer == null ||
                answer.isBlank() ||
                documents == null ||
                documents.isEmpty()) {

            return false;
        }

        for (Document document : documents) {

            double score =
                    calculateSupportScore(
                            answer,
                            document
                    );

            if (score >= SUPPORT_THRESHOLD) {
                return true;
            }
        }

        return false;
    }

    public List<SupportingDocument> findSupportingDocuments(
            String answer,
            List<Document> documents) {

        List<SupportingDocument> results =
                new ArrayList<>();

        if (answer == null ||
                answer.isBlank() ||
                documents == null) {

            return results;
        }

        for (Document document : documents) {

            double score =
                    calculateSupportScore(
                            answer,
                            document
                    );

            if (score >= SUPPORT_THRESHOLD) {

                results.add(
                        new SupportingDocument(
                                document,
                                score
                        )
                );
            }
        }

        return results;
    }

    public double calculateSupportScore(
            String answer,
            Document document) {

        if (answer == null ||
                answer.isBlank() ||
                document == null ||
                document.getText() == null) {

            return 0.0;
        }

        String normalizedAnswer =
                normalize(answer);

        String normalizedDocument =
                normalize(document.getText());

        String[] answerWords =
                normalizedAnswer.split("\\s+");

        if (answerWords.length == 0) {
            return 0.0;
        }

        int meaningfulWords = 0;
        int matchingWords = 0;

        for (String word : answerWords) {

            if (word.length() < 3) {
                continue;
            }

            meaningfulWords++;

            if (normalizedDocument.contains(word)) {
                matchingWords++;
            }
        }

        if (meaningfulWords == 0) {
            return 0.0;
        }

        double wordCoverage =
                (double) matchingWords
                        / meaningfulWords;

        double phraseScore =
                calculatePhraseScore(
                        normalizedAnswer,
                        normalizedDocument
                );

        double score =
                (wordCoverage * 0.70)
                        + (phraseScore * 0.30);

        if (score > 1.0) {
            score = 1.0;
        }

        return score;
    }

    private double calculatePhraseScore(
            String answer,
            String document) {

        String[] sentences =
                answer.split("[.!?]");

        if (sentences.length == 0) {
            return 0.0;
        }

        int matchedPhrases = 0;
        int checkedPhrases = 0;

        for (String sentence : sentences) {

            String[] words =
                    sentence.trim()
                            .split("\\s+");

            if (words.length < 3) {
                continue;
            }

            checkedPhrases++;

            StringBuilder phrase =
                    new StringBuilder();

            int limit =
                    Math.min(
                            words.length,
                            6
                    );

            for (int i = 0;
                 i < limit;
                 i++) {

                if (i > 0) {
                    phrase.append(" ");
                }

                phrase.append(words[i]);
            }

            if (document.contains(
                    phrase.toString())) {

                matchedPhrases++;
            }
        }

        if (checkedPhrases == 0) {
            return 0.0;
        }

        return (double) matchedPhrases
                / checkedPhrases;
    }

    private String normalize(String text) {

        if (text == null) {
            return "";
        }

        return text
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static class SupportingDocument {

        private final Document document;
        private final double score;

        public SupportingDocument(
                Document document,
                double score) {

            this.document = document;
            this.score = score;
        }

        public Document getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }
    }
}