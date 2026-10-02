package com.copilot.service;

import com.copilot.model.ChatResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private final RetrievalService retrievalService;
    private final CitationVerifierService citationVerifierService;
    private final ChatClient chatClient;

    private final ConcurrentHashMap<String, List<ConversationMessage>>
            conversationHistory =
            new ConcurrentHashMap<>();

    public ChatService(
            RetrievalService retrievalService,
            CitationVerifierService citationVerifierService,
            ChatClient.Builder chatClientBuilder) {

        this.retrievalService = retrievalService;
        this.citationVerifierService =
                citationVerifierService;

        this.chatClient =
                chatClientBuilder.build();
    }

    public ChatResponse askQuestion(
            String question,
            String requestedSessionId) {

        if (question == null ||
                question.isBlank()) {

            throw new IllegalArgumentException(
                    "Question cannot be empty."
            );
        }

        String sessionId =
                requestedSessionId;

        if (sessionId == null ||
                sessionId.isBlank()) {

            sessionId =
                    UUID.randomUUID().toString();
        }

        List<Document> documents =
                retrievalService.search(question);

        StringBuilder context =
                new StringBuilder();

        for (Document document :
                documents) {

            Object source =
                    document.getMetadata()
                            .get("source");

            Object title =
                    document.getMetadata()
                            .get("title");

            Object section =
                    document.getMetadata()
                            .get("section");

            Object page =
                    document.getMetadata()
                            .get("page");

            context.append(
                    "Source: "
                            + safeValue(source)
                            + "\n"
            );

            context.append(
                    "Title: "
                            + safeValue(title)
                            + "\n"
            );

            context.append(
                    "Section: "
                            + safeValue(section)
                            + "\n"
            );

            context.append(
                    "Page: "
                            + safeValue(page)
                            + "\n"
            );

            context.append(
                    "Content:\n"
            );

            context.append(
                    document.getText()
            );

            context.append("\n\n");
        }

        String history =
                buildConversationHistory(
                        sessionId
                );

        String prompt = """
                You are a support knowledge assistant.

                Answer the user's question using ONLY the
                information provided in the knowledge base context.

                If the answer is not available in the context,
                say:
                "I don't have enough information in the knowledge base to answer that."

                Do not make up information.

                Previous conversation:
                %s

                Knowledge base context:
                %s

                User Question:
                %s
                """.formatted(
                history,
                context,
                question
        );

        String answer =
                chatClient
                        .prompt()
                        .user(prompt)
                        .call()
                        .content();

        if (answer == null ||
                answer.isBlank()) {

            answer =
                    "I don't have enough information in the knowledge base to answer that.";
        }

        List<CitationVerifierService.SupportingDocument>
                supportingDocuments =
                citationVerifierService
                        .findSupportingDocuments(
                                answer,
                                documents
                        );

        boolean verified =
                !supportingDocuments.isEmpty();

        List<ChatResponse.Citation> citations =
                new ArrayList<>();

        int citationLimit = 5;

        for (CitationVerifierService.SupportingDocument supportingDocument :
                supportingDocuments) {

            if (citations.size() >= citationLimit) {
                break;
            }

            Document document =
                    supportingDocument.getDocument();

            double score =
                    supportingDocument.getScore();

            Object source =
                    document.getMetadata()
                            .get("source");

            if (source == null) {
                continue;
            }

            Object title =
                    document.getMetadata()
                            .get("title");

            Object section =
                    document.getMetadata()
                            .get("section");

            Object accessLevel =
                    document.getMetadata()
                            .get("access_level");

            Object page =
                    document.getMetadata()
                            .get("page");

            ChatResponse.Citation citation =
                    new ChatResponse.Citation(
                            source.toString(),
                            valueOrNull(title),
                            valueOrNull(section),
                            valueOrNull(page),
                            valueOrNull(accessLevel),
                            document.getText(),
                            roundScore(score),
                            true
                    );

            citations.add(citation);
        }

        saveConversation(
                sessionId,
                question,
                answer
        );

        return new ChatResponse(
                answer,
                citations,
                verified,
                sessionId
        );
    }

    public List<ConversationMessage> getConversationHistory(
            String sessionId) {

        if (sessionId == null ||
                sessionId.isBlank()) {

            return new ArrayList<>();
        }

        List<ConversationMessage> history =
                conversationHistory.get(
                        sessionId
                );

        if (history == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(history);
    }

    public boolean clearConversationHistory(
            String sessionId) {

        if (sessionId == null ||
                sessionId.isBlank()) {

            return false;
        }

        return conversationHistory
                .remove(sessionId) != null;
    }

    private void saveConversation(
            String sessionId,
            String question,
            String answer) {

        conversationHistory
                .computeIfAbsent(
                        sessionId,
                        key -> new ArrayList<>()
                );

        List<ConversationMessage> history =
                conversationHistory.get(
                        sessionId
                );

        synchronized (history) {

            history.add(
                    new ConversationMessage(
                            "user",
                            question
                    )
            );

            history.add(
                    new ConversationMessage(
                            "assistant",
                            answer
                    )
            );

            int maximumMessages = 10;

            while (history.size() >
                    maximumMessages) {

                history.remove(0);
            }
        }
    }

    private String buildConversationHistory(
            String sessionId) {

        List<ConversationMessage> history =
                conversationHistory.get(
                        sessionId
                );

        if (history == null ||
                history.isEmpty()) {

            return "No previous conversation.";
        }

        StringBuilder result =
                new StringBuilder();

        synchronized (history) {

            for (ConversationMessage message :
                    history) {

                result.append(
                        message.getRole()
                );

                result.append(": ");

                result.append(
                        message.getContent()
                );

                result.append("\n");
            }
        }

        return result.toString();
    }

    private String safeValue(
            Object value) {

        if (value == null) {
            return "N/A";
        }

        return value.toString();
    }

    private String valueOrNull(
            Object value) {

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private double roundScore(
            double score) {

        return Math.round(
                score * 100.0
        ) / 100.0;
    }

    public static class ConversationMessage {

        private final String role;
        private final String content;

        public ConversationMessage(
                String role,
                String content) {

            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}