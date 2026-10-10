# PDF Q&A with RAG (Spring Boot + Spring AI + pgvector + Claude)

REST API that answers questions about uploaded PDFs, with page-level source citations.

## Architecture
```
PDF upload -> read per page -> split into ~250-token chunks -> local embeddings (MiniLM, 384-d)
           -> store in PostgreSQL (pgvector, HNSW, cosine)

Question -> embed -> top-4 similar chunks -> Claude answers using ONLY those chunks -> answer + sources
```

## Stack
Java 17, Spring Boot 3.4, Spring AI 1.0, PostgreSQL + pgvector, Claude API, Docker Compose

## Run
```bash
docker compose up -d                     # starts PostgreSQL with pgvector
export ANTHROPIC_API_KEY=your_key        # Windows PowerShell: $env:ANTHROPIC_API_KEY="your_key"
mvn spring-boot:run
```
The first start downloads the embedding model, so internet is needed once.

## API
Upload a PDF:
```bash
curl -F "file=@notes.pdf" http://localhost:8080/api/documents
```
Ask a question:
```bash
curl -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is the main topic of the document?"}'
```
Both endpoints also work from Postman.

## Design decisions
- Embeddings run locally, so there is no per-document embedding cost.
- The prompt restricts Claude to retrieved context and requires citations, which reduces hallucination.
- Each chunk carries its file name and page number as metadata, which powers the citations.

## Ideas for extending
- Retrieval evaluation (hit rate on a hand-made question set)
- Spring Security + JWT, per-user document collections
- Dockerfile for the app itself, GitHub Actions CI
