package com.copilot.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Service
public class DocumentService {

    private final VectorStore vectorStore;

    private final ITesseract tesseract;

    private final List<Document> chunks =
            new ArrayList<>();

    private final List<UploadedDocument> uploadedDocuments =
            new ArrayList<>();

public DocumentService(
        VectorStore vectorStore) {

    this.vectorStore = vectorStore;

    this.tesseract =
            new Tesseract();

    try {

        var tessDataFolder =
                LoadLibs.extractTessResources(
                        "tessdata"
                );

        this.tesseract.setDatapath(
                tessDataFolder.getAbsolutePath()
        );

        this.tesseract.setLanguage("eng");

    } catch (Exception e) {

        throw new RuntimeException(
                "Could not initialize Tesseract OCR.",
                e
        );
    }
}

    // =========================================================
    // UPLOAD DOCUMENT
    // =========================================================

    public String processUploadedDocument(
            MultipartFile file) throws IOException {

        Path uploadDirectory =
                Path.of("data/uploads");

        Files.createDirectories(uploadDirectory);

        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                fileName.isBlank()) {

            throw new IOException(
                    "Invalid file name."
            );
        }

        // Prevent duplicate uploads
        for (UploadedDocument document :
                uploadedDocuments) {

            if (document.getFileName()
                    .equalsIgnoreCase(fileName)) {

                throw new IOException(
                        "Document already uploaded: "
                                + fileName
                );
            }
        }

        String documentId =
                UUID.randomUUID().toString();

        Path filePath =
                uploadDirectory.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                filePath,
                StandardCopyOption.REPLACE_EXISTING
        );

        List<Document> documents =
                extractDocuments(
                        filePath,
                        fileName
                );

        if (documents.isEmpty()) {

            throw new IOException(
                    "No readable content found in the document."
            );
        }

        TokenTextSplitter splitter =
                TokenTextSplitter.builder()
                        .withChunkSize(500)
                        .withMinChunkSizeChars(100)
                        .build();

        List<Document> newChunks =
                splitter.split(documents);

        // Add document ID to every chunk
        for (Document chunk : newChunks) {

            chunk.getMetadata().put(
                    "document_id",
                    documentId
            );
        }

        // Keep chunks in application memory
        chunks.addAll(newChunks);

        // Create embeddings and store in Qdrant
        vectorStore.add(newChunks);

        UploadedDocument uploadedDocument =
                new UploadedDocument(
                        documentId,
                        fileName,
                        newChunks.size()
                );

        uploadedDocuments.add(
                uploadedDocument
        );

        return
                "Document uploaded and indexed successfully. "
                        + "Document ID: "
                        + documentId
                        + ". Chunks created: "
                        + newChunks.size();
    }

    // =========================================================
    // GET ALL DOCUMENTS
    // =========================================================

    public List<UploadedDocument> getAllDocuments() {

        return uploadedDocuments;
    }

    // =========================================================
    // GET ONE DOCUMENT
    // =========================================================

    public UploadedDocument getDocument(
            String documentId) {

        for (UploadedDocument document :
                uploadedDocuments) {

            if (document.getId()
                    .equals(documentId)) {

                return document;
            }
        }

        return null;
    }

    // =========================================================
    // DELETE DOCUMENT
    // =========================================================

    public boolean deleteDocument(
            String documentId) {

        UploadedDocument document =
                getDocument(documentId);

        if (document == null) {

            return false;
        }

        // Delete vectors from Qdrant
        try {

            vectorStore.delete(
                    "document_id == '"
                            + documentId
                            + "'"
            );

        } catch (Exception e) {

            System.out.println(
                    "Warning: Could not delete Qdrant vectors: "
                            + e.getMessage()
            );

            return false;
        }

        // Delete physical file
        Path filePath =
                Path.of("data/uploads")
                        .resolve(
                                document.getFileName()
                        );

        try {

            Files.deleteIfExists(filePath);

        } catch (IOException e) {

            return false;
        }

        // Remove chunks from memory
        chunks.removeIf(chunk -> {

            Object id =
                    chunk.getMetadata()
                            .get("document_id");

            return id != null &&
                    id.toString()
                            .equals(documentId);
        });

        // Remove document from memory
        uploadedDocuments.remove(document);

        return true;
    }

    // =========================================================
    // REINDEX DOCUMENT
    // =========================================================

    public String reindexDocument(
            String documentId) throws IOException {

        UploadedDocument document =
                getDocument(documentId);

        if (document == null) {

            return null;
        }

        Path filePath =
                Path.of("data/uploads")
                        .resolve(
                                document.getFileName()
                        );

        if (!Files.exists(filePath)) {

            throw new IOException(
                    "Document file not found."
            );
        }

        // Delete old vectors
        try {

            vectorStore.delete(
                    "document_id == '"
                            + documentId
                            + "'"
            );

        } catch (Exception e) {

            throw new IOException(
                    "Could not remove old Qdrant vectors: "
                            + e.getMessage(),
                    e
            );
        }

        // Extract again
        List<Document> documents =
                extractDocuments(
                        filePath,
                        document.getFileName()
                );

        if (documents.isEmpty()) {

            throw new IOException(
                    "No readable content found in the document."
            );
        }

        TokenTextSplitter splitter =
                TokenTextSplitter.builder()
                        .withChunkSize(500)
                        .withMinChunkSizeChars(100)
                        .build();

        List<Document> newChunks =
                splitter.split(documents);

        // Add same document ID
        for (Document chunk : newChunks) {

            chunk.getMetadata().put(
                    "document_id",
                    documentId
            );
        }

        // Remove old in-memory chunks
        chunks.removeIf(chunk -> {

            Object id =
                    chunk.getMetadata()
                            .get("document_id");

            return id != null &&
                    id.toString()
                            .equals(documentId);
        });

        // Add new chunks
        chunks.addAll(newChunks);

        // Add new vectors
        vectorStore.add(newChunks);

        // Update count
        document.setChunkCount(
                newChunks.size()
        );

        return
                "Document reindexed successfully. "
                        + "Chunks: "
                        + newChunks.size();
    }

    // =========================================================
    // MAIN DOCUMENT EXTRACTION
    // =========================================================

    private List<Document> extractDocuments(
            Path filePath,
            String fileName)
            throws IOException {

        String extension =
                getFileExtension(fileName);

        switch (extension) {

            case "txt":
            case "md":

                return extractTextFile(
                        filePath,
                        fileName
                );

            case "pdf":

                return extractPdf(
                        filePath,
                        fileName
                );

            case "docx":

                return extractDocx(
                        filePath,
                        fileName
                );

            default:

                throw new IOException(
                        "Unsupported file type: ."
                                + extension
                                + ". Supported types: "
                                + ".txt, .md, .pdf, .docx"
                );
        }
    }

    // =========================================================
    // TXT / MARKDOWN
    // =========================================================

    private List<Document> extractTextFile(
            Path filePath,
            String fileName)
            throws IOException {

        String content =
                Files.readString(filePath);

        Document document =
                createDocument(
                        content,
                        fileName,
                        "N/A",
                        "text"
                );

        return List.of(document);
    }

    // =========================================================
    // PDF EXTRACTION
    // =========================================================

    private List<Document> extractPdf(
            Path filePath,
            String fileName)
            throws IOException {

        List<Document> documents =
                new ArrayList<>();

        byte[] fileBytes =
                Files.readAllBytes(filePath);

        try (PDDocument pdfDocument =
                     Loader.loadPDF(fileBytes)) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            int totalPages =
                    pdfDocument.getNumberOfPages();

            for (int pageNumber = 1;
                 pageNumber <= totalPages;
                 pageNumber++) {

                stripper.setStartPage(
                        pageNumber
                );

                stripper.setEndPage(
                        pageNumber
                );

                String pageText =
                        stripper.getText(
                                pdfDocument
                        );

                if (pageText == null) {

                    pageText = "";
                }

                StringBuilder pageContent =
                        new StringBuilder();

                // Normal text
                if (!pageText.isBlank()) {

                    pageContent.append(
                            pageText
                    );
                }

                // Extract images from this page
                PDPage page =
                        pdfDocument.getPage(
                                pageNumber - 1
                        );

                List<String> imageOcrTexts =
        extractPdfImages(
                page
        );

for (String ocrText :
        imageOcrTexts) {

    pageContent.append(
            "\n\n[IMAGE OCR CONTENT]\n"
    );

    pageContent.append(
            ocrText
    );
}

                /*
                 * If the page has no readable text,
                 * treat it as a scanned page.
                 */
                if (pageContent
                        .toString()
                        .isBlank()) {

                    String ocrText =
                            performPdfPageOcr(
                                    pdfDocument,
                                    pageNumber
                            );

                    if (ocrText != null &&
                            !ocrText.isBlank()) {

                        pageContent.append(
                                "[OCR CONTENT]\n"
                        );

                        pageContent.append(
                                ocrText
                        );
                    }
                }

                if (pageContent
                        .toString()
                        .isBlank()) {

                    continue;
                }

                Document document =
                        createDocument(
                                pageContent.toString(),
                                fileName,
                                String.valueOf(
                                        pageNumber
                                ),
                                "pdf"
                        );

                documents.add(document);
            }
        }

        return documents;
    }

    // =========================================================
    // PDF IMAGE EXTRACTION
    // =========================================================

    private List<String> extractPdfImages(
        PDPage page) {

    List<String> ocrTexts =
            new ArrayList<>();

    try {

        for (var entry :
                page.getResources()
                        .getXObjectNames()) {

            var xObject =
                    page.getResources()
                            .getXObject(entry);

            if (!(xObject
                    instanceof PDImageXObject)) {

                continue;
            }

            PDImageXObject image =
                    (PDImageXObject) xObject;

            BufferedImage bufferedImage =
                    image.getImage();

            if (bufferedImage == null) {

                continue;
            }

            String ocrText =
                    tesseract.doOCR(
                            bufferedImage
                    );

            if (ocrText != null &&
                    !ocrText.isBlank()) {

                ocrTexts.add(
                        ocrText.trim()
                );
            }
        }

    } catch (TesseractException e) {

        System.out.println(
                "Tesseract image OCR failed: "
                        + e.getMessage()
        );

    } catch (Exception e) {

        System.out.println(
                "Could not process PDF image: "
                        + e.getMessage()
        );
    }

    return ocrTexts;
}
    // =========================================================
    // TEMPORARY IMAGE
    // =========================================================

    // =========================================================
    // SCANNED PDF OCR
    // =========================================================
    private String performPdfPageOcr(
        PDDocument pdfDocument,
        int pageNumber) {

    try {

        PDFRenderer renderer =
                new PDFRenderer(
                        pdfDocument
                );

        BufferedImage pageImage =
                renderer.renderImageWithDPI(
                        pageNumber - 1,
                        300
                );

        String result =
                tesseract.doOCR(
                        pageImage
                );

        if (result == null) {
            return "";
        }

        return result.trim();

    } catch (TesseractException e) {

        System.out.println(
                "Tesseract OCR failed for PDF page "
                        + pageNumber
                        + ": "
                        + e.getMessage()
        );

    } catch (IOException e) {

        System.out.println(
                "Could not render PDF page "
                        + pageNumber
                        + ": "
                        + e.getMessage()
        );
    }

    return "";
}
    // =========================================================
    // GEMINI IMAGE / OCR ANALYSIS
    // =========================================================

    // =========================================================
    // DOCX EXTRACTION
    // =========================================================

    private List<Document> extractDocx(
            Path filePath,
            String fileName)
            throws IOException {

        List<Document> documents =
                new ArrayList<>();

        StringBuilder text =
                new StringBuilder();

        List<byte[]> images =
                new ArrayList<>();

        try (InputStream inputStream =
                     Files.newInputStream(filePath);
             XWPFDocument document =
                     new XWPFDocument(inputStream)) {

            // ---------------------------------------------
            // Paragraphs
            // ---------------------------------------------

            List<XWPFParagraph> paragraphs =
                    document.getParagraphs();

            for (XWPFParagraph paragraph :
                    paragraphs) {

                String paragraphText =
                        paragraph.getText();

                if (paragraphText != null &&
                        !paragraphText.isBlank()) {

                    text.append(
                            paragraphText
                    );

                    text.append("\n");
                }
            }

            // ---------------------------------------------
            // Tables
            // ---------------------------------------------

            List<XWPFTable> tables =
                    document.getTables();

            int tableNumber = 1;

            for (XWPFTable table :
                    tables) {

                text.append(
                        "\n[TABLE "
                                + tableNumber
                                + "]\n"
                );

                List<XWPFTableRow> rows =
                        table.getRows();

                for (XWPFTableRow row :
                        rows) {

                    List<XWPFTableCell> cells =
                            row.getTableCells();

                    for (int i = 0;
                         i < cells.size();
                         i++) {

                        String cellText =
                                cells.get(i)
                                        .getText();

                        text.append(
                                cellText
                        );

                        if (i < cells.size() - 1) {

                            text.append(
                                    " | "
                            );
                        }
                    }

                    text.append("\n");
                }

                tableNumber++;
            }

            // ---------------------------------------------
            // Embedded Images
            // ---------------------------------------------

            List<XWPFPictureData> pictureData =
                    document.getAllPictures();

            for (XWPFPictureData picture :
                    pictureData) {

                images.add(
                        picture.getData()
                );
            }
        }

        // ---------------------------------------------
        // Analyze DOCX images
        // ---------------------------------------------

        int imageNumber = 1;

        for (int i = 0;
             i < images.size();
             i++) {

            String description =
        performImageOcr(
                images.get(i)
        );

            if (description != null &&
                    !description.isBlank()) {

                text.append(
                        "\n[IMAGE "
                                + imageNumber
                                + " CONTENT]\n"
                );

                text.append(
                        description
                );

                text.append("\n");
            }

            imageNumber++;
        }

        if (text.toString().isBlank()) {

            return List.of();
        }

        Document document =
                createDocument(
                        text.toString(),
                        fileName,
                        "N/A",
                        "docx"
                );

        documents.add(document);

        return documents;
    }

    // =========================================================
    // IMAGE MIME TYPE
    // =========================================================
private String performImageOcr(
        byte[] imageBytes) {

    try {

        java.io.ByteArrayInputStream input =
                new java.io.ByteArrayInputStream(
                        imageBytes
                );

        BufferedImage image =
                javax.imageio.ImageIO.read(
                        input
                );

        if (image == null) {

            return "";
        }

        String result =
                tesseract.doOCR(
                        image
                );

        if (result == null) {

            return "";
        }

        return result.trim();

    } catch (TesseractException e) {

        System.out.println(
                "Tesseract image OCR failed: "
                        + e.getMessage()
        );

    } catch (IOException e) {

        System.out.println(
                "Could not read image: "
                        + e.getMessage()
        );
    }

    return "";
}

    // =========================================================
    // CREATE DOCUMENT
    // =========================================================

    private Document createDocument(
            String content,
            String fileName,
            String page,
            String contentType) {

        Document document =
                new Document(content);

        document.getMetadata().put(
                "source",
                fileName
        );

        document.getMetadata().put(
                "title",
                fileName
        );

        document.getMetadata().put(
                "section",
                "General"
        );

        document.getMetadata().put(
                "page",
                page
        );

        document.getMetadata().put(
                "content_type",
                contentType
        );

        document.getMetadata().put(
                "access_level",
                "internal"
        );

        return document;
    }

    // =========================================================
    // FILE EXTENSION
    // =========================================================

    private String getFileExtension(
            String fileName) {

        int lastDot =
                fileName.lastIndexOf(".");

        if (lastDot == -1) {

            return "";
        }

        return fileName
                .substring(lastDot + 1)
                .toLowerCase();
    }

    // =========================================================
    // GET CHUNKS
    // =========================================================

    public List<Document> getChunks() {

        return chunks;
    }

    // =========================================================
    // UPLOADED DOCUMENT MODEL
    // =========================================================

    public static class UploadedDocument {

        private String id;

        private String fileName;

        private int chunkCount;

        public UploadedDocument() {
        }

        public UploadedDocument(
                String id,
                String fileName,
                int chunkCount) {

            this.id = id;

            this.fileName = fileName;

            this.chunkCount = chunkCount;
        }

        public String getId() {

            return id;
        }

        public void setId(
                String id) {

            this.id = id;
        }

        public String getFileName() {

            return fileName;
        }

        public void setFileName(
                String fileName) {

            this.fileName = fileName;
        }

        public int getChunkCount() {

            return chunkCount;
        }

        public void setChunkCount(
                int chunkCount) {

            this.chunkCount = chunkCount;
        }
    }
}