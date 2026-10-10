package com.example.pdfrag;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/** Retrieves the most similar chunks, then asks Claude to answer using only those chunks. */
@Service
public class QaService {

    private static final int TOP_K = 4;

    private static final String SYSTEM_PROMPT = """
            You answer questions using ONLY the numbered context passages provided.
            Cite passages like [1], [2] after the claims they support.
            If the answer is not in the context, say you couldn't find it in the documents.
            Do not use outside knowledge.""";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public QaService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public AnswerResponse ask(String question) {
        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder().query(question).topK(TOP_K).build());

        if (hits.isEmpty()) {
            return new AnswerResponse("No documents have been indexed yet. Upload a PDF first.", List.of());
        }

        StringBuilder context = new StringBuilder();
        List<AnswerResponse.Source> sources = new java.util.ArrayList<>();
        for (int i = 0; i < hits.size(); i++) {
            Document d = hits.get(i);
            Object source = d.getMetadata().get("source");
            Object page = d.getMetadata().get("page_number");
            context.append("[").append(i + 1).append("] (").append(source)
                   .append(", p.").append(page).append(")\n")
                   .append(d.getText()).append("\n\n");
            sources.add(new AnswerResponse.Source(i + 1, String.valueOf(source), page, d.getScore(), d.getText()));
        }

        // Build messages directly so any {braces} in PDF text are never treated as template variables
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT),
                new UserMessage("Context:\n" + context + "\nQuestion: " + question)));

        String answer = chatClient.prompt(prompt).call().content();
        return new AnswerResponse(answer, sources);
    }
}
