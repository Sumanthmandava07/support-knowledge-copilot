package com.copilot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class EvaluationService {

    private final RetrievalService retrievalService;

    public EvaluationService(
            RetrievalService retrievalService) {

        this.retrievalService =
                retrievalService;
    }

    public void evaluate() {

        try {

            InputStream inputStream =
                    getClass()
                            .getClassLoader()
                            .getResourceAsStream(
                                    "evaluation/evaluation.json"
                            );

            if (inputStream == null) {

                System.out.println(
                        "Evaluation file not found."
                );

                return;
            }

            ObjectMapper objectMapper =
                    new ObjectMapper();

            List<EvaluationQuestion> questions =
                    objectMapper.readValue(
                            inputStream,
                            new TypeReference<List<EvaluationQuestion>>() {
                            }
                    );

            if (questions.isEmpty()) {

                System.out.println(
                        "No evaluation questions found."
                );

                return;
            }

            int correct = 0;
            int failed = 0;

            System.out.println();
            System.out.println(
                    "========== RETRIEVAL EVALUATION =========="
            );

            for (EvaluationQuestion question :
                    questions) {

                List<Document> results =
                        retrievalService.search(
                                question.getQuestion()
                        );

                boolean found = false;

                for (Document document :
                        results) {

                    Object source =
                            document.getMetadata()
                                    .get("source");

                    if (source != null &&
                            source.toString().equals(
                                    question.getExpectedSource()
                            )) {

                        found = true;
                        break;
                    }
                }

                if (found) {
                    correct++;
                } else {
                    failed++;
                }

                System.out.println();
                System.out.println(
                        "Question: "
                                + question.getQuestion()
                );

                System.out.println(
                        "Expected Source: "
                                + question.getExpectedSource()
                );

                System.out.println(
                        "Found in Top 5: "
                                + found
                );
            }

            double recallAt5 =
                    (double) correct
                            / questions.size()
                            * 100;

            System.out.println();
            System.out.println(
                    "--------------- SUMMARY ---------------"
            );

            System.out.println(
                    "Total Questions: "
                            + questions.size()
            );

            System.out.println(
                    "Correct Retrievals: "
                            + correct
            );

            System.out.println(
                    "Failed Retrievals: "
                            + failed
            );

            System.out.println(
                    "Recall@5: "
                            + String.format(
                                    "%.2f",
                                    recallAt5
                            )
                            + "%"
            );

            System.out.println(
                    "=========================================="
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error during retrieval evaluation",
                    e
            );
        }
    }

    public static class EvaluationQuestion {

        private String question;
        private String expectedSource;

        public EvaluationQuestion() {
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(
                String question) {

            this.question = question;
        }

        public String getExpectedSource() {
            return expectedSource;
        }

        public void setExpectedSource(
                String expectedSource) {

            this.expectedSource =
                    expectedSource;
        }
    }
}