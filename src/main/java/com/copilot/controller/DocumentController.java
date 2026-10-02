package com.copilot.controller;

import com.copilot.service.DocumentService;
import com.copilot.service.DocumentService.UploadedDocument;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(
            DocumentService documentService) {

        this.documentService =
                documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file")
            MultipartFile file) {

        if (file.isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Please select a file."
                    );
        }

        try {

            String message =
                    documentService
                            .processUploadedDocument(
                                    file
                            );

            return ResponseEntity.ok(
                    message
            );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Error processing document: "
                                    + e.getMessage()
                    );
        }
    }

    @GetMapping
    public List<UploadedDocument> getDocuments() {

        return documentService
                .getAllDocuments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(
            @PathVariable String id) {

        UploadedDocument document =
                documentService.getDocument(id);

        if (document == null) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(
                document
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(
            @PathVariable String id) {

        boolean deleted =
                documentService
                        .deleteDocument(id);

        if (!deleted) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity.ok(
                "Document deleted successfully."
        );
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<String> reindexDocument(
            @PathVariable String id) {

        try {

            String result =
                    documentService
                            .reindexDocument(id);

            if (result == null) {

                return ResponseEntity
                        .notFound()
                        .build();
            }

            return ResponseEntity.ok(
                    result
            );

        } catch (IOException e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Error reindexing document: "
                                    + e.getMessage()
                    );
        }
    }
}