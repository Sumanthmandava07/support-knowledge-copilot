package com.copilot.controller;

import com.copilot.model.ChatRequest;
import com.copilot.model.ChatResponse;
import com.copilot.service.ChatService;
import com.copilot.service.EvaluationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final EvaluationService evaluationService;

    public ChatController(
            ChatService chatService,
            EvaluationService evaluationService) {

        this.chatService = chatService;
        this.evaluationService = evaluationService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestBody ChatRequest request) {

        if (request == null ||
                request.getQuestion() == null ||
                request.getQuestion().isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Question cannot be empty."
                    );
        }

        try {

            ChatResponse response =
                    chatService.askQuestion(
                            request.getQuestion(),
                            request.getSessionId()
                    );

            return ResponseEntity.ok(
                    response
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            e.getMessage()
                    );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Error processing question: "
                                    + e.getMessage()
                    );
        }
    }

    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<?> getHistory(
            @PathVariable String sessionId) {

        if (sessionId == null ||
                sessionId.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Session ID cannot be empty."
                    );
        }

        return ResponseEntity.ok(
                chatService
                        .getConversationHistory(
                                sessionId
                        )
        );
    }

    @DeleteMapping("/chat/history/{sessionId}")
    public ResponseEntity<String> clearHistory(
            @PathVariable String sessionId) {

        boolean deleted =
                chatService
                        .clearConversationHistory(
                                sessionId
                        );

        if (!deleted) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(
                "Conversation history cleared successfully."
        );
    }

    @GetMapping("/evaluate")
    public ResponseEntity<String> evaluate() {

        evaluationService.evaluate();

        return ResponseEntity.ok(
                "Retrieval evaluation completed. Check the application console."
        );
    }
}