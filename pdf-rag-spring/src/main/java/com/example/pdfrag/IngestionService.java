package com.example.pdfrag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Reads a PDF page by page, splits it into chunks, embeds them and stores them in pgvector. */
@Service
public class IngestionService {

    private final VectorStore vectorStore;

    // ~250 tokens per chunk keeps chunks within the embedding model's input limit
    private final TokenTextSplitter splitter = new TokenTextSplitter(250, 150, 5, 10000, true);

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "document.pdf" : file.getOriginalFilename();
        Path tmp = Files.createTempFile("upload-", ".pdf");
        try {
            file.transferTo(tmp);
            PagePdfDocumentReader reader =
                    new PagePdfDocumentReader(new FileSystemResource(tmp), PdfDocumentReaderConfig.defaultConfig());

            // Tag every page with the original file name so answers can cite their source
            List<Document> pages = reader.get().stream().map(page -> {
                Map<String, Object> metadata = new HashMap<>(page.getMetadata());
                metadata.put("source", name);
                return new Document(page.getText(), metadata);
            }).toList();

            List<Document> chunks = splitter.apply(pages);
            if (chunks.isEmpty()) {
                throw new IllegalArgumentException("No extractable text found in " + name);
            }
            vectorStore.add(chunks);
            return chunks.size();
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
