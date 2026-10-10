package com.example.pdfrag;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class RagController {

    public record AskRequest(String question) {}

    private final IngestionService ingestionService;
    private final QaService qaService;

    public RagController(IngestionService ingestionService, QaService qaService) {
        this.ingestionService = ingestionService;
        this.qaService = qaService;
    }

    /** Upload and index a PDF. */
    @PostMapping("/documents")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Please upload a non-empty PDF file.");
        }
        int chunks = ingestionService.ingest(file);
        return Map.of("file", file.getOriginalFilename(), "chunksIndexed", chunks);
    }

    /** Ask a question about the indexed documents. */
    @PostMapping("/ask")
    public AnswerResponse ask(@RequestBody AskRequest request) {
        if (request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("Question must not be empty.");
        }
        return qaService.ask(request.question());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
}
