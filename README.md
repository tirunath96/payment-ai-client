# AI Payment Operations Agent

An AI-powered Payment Operations Agent built using Java, Spring Boot, Spring AI, Ollama, and Model Context Protocol (MCP).

The system investigates payment failures by combining live transaction data, payment gateway information, historical incident knowledge, semantic search, and LLM-based reasoning.

## Technology Stack

- Java 21
- Spring Boot
- Spring AI
- Ollama
- Qwen 2.5 (3B)
- embeddinggemma
- Model Context Protocol (MCP)
- PostgreSQL
- Spring Data JPA
- Maven
- REST APIs

## Implemented AI Capabilities

### 1. LLM Integration
- Local LLM integration using Ollama.
- Qwen 2.5 (3B) for payment investigation and reasoning.
- AI-generated payment failure analysis.

### 2. Embeddings
- Integrated embeddinggemma through Ollama.
- Generated 768-dimensional embeddings.
- Converted historical payment incidents into vector representations.

### 3. Vector Storage
- Integrated Spring AI SimpleVectorStore.
- Stored embeddings of historical payment incidents.
- Enabled similarity-based retrieval.

### 4. Semantic Search
- Implemented semantic similarity search.
- Retrieves the three most relevant historical incidents.
- Uses vector similarity rather than relying only on exact keyword matching.

### 5. Retrieval-Augmented Generation (RAG)
- Ingested four historical payment incident documents.
- Retrieves relevant incidents based on payment investigation context.
- Enriches the LLM prompt with retrieved historical knowledge.
- Combines historical incident context with live payment information.

### 6. MCP Integration and Tool Calling
- Separate MCP client and server architecture.
- Spring AI tool calling.
- Payment transaction status retrieval.
- Payment gateway status retrieval.
- Live payment evidence retrieval through MCP tools.

### 7. Payment Risk Classification
- Java-based risk classification.
- Combines payment investigation evidence with risk assessment.
- AI-generated investigation recommendations.

### 8. Payment Event Components
- Payment event model.
- Payment event controller.
- Payment event producer.
- Payment event simulator.

## System Architecture

```text
             Payment Dashboard
                    |
                    v
             Spring Boot API
                    |
                    v
              AIService.java
                    |
          +---------+----------+
          |                    |
          v                    v
     MCP Tool Calling       RAG Pipeline
          |                    |
          v                    v
    Payment MCP Server    embeddinggemma
          |                    |
          v                    v
    PostgreSQL Data       SimpleVectorStore
          |                    |
          v                    v
    Live Payment Data     Semantic Search
          |                    |
          |              Top 3 Incidents
          |                    |
          +---------+----------+
                    |
                    v
              Qwen 2.5 (3B)
                    |
                    v
          AI Payment Investigation
                    |
                    v
           Investigation Result
```

## RAG Implementation

The current RAG implementation follows this workflow:

1. Historical payment incidents are ingested.
2. embeddinggemma generates embeddings.
3. Embeddings are stored in Spring AI SimpleVectorStore.
4. A payment investigation triggers semantic similarity search.
5. The three most relevant historical incidents are retrieved.
6. Retrieved context is added to the LLM prompt.
7. Qwen generates an investigation using live payment evidence and historical context.

## Project Structure

```text
payment-ai-client/
│
├── src/
│   └── main/
│       └── java/
│           └── com/tiru/
│               ├── AIService.java
│               ├── PaymentEvent.java
│               ├── PaymentEventController.java
│               ├── PaymentEventProducer.java
│               └── PaymentEventSimulator.java
│
├── pom.xml
└── README.md
```

## Validation

Successfully tested payment investigation for TXN1001 using:

- Live transaction and gateway information retrieved through MCP.
- Java-based risk classification.
- Embedding generation and vector similarity retrieval.
- Historical payment incident context.
- LLM-based investigation using Qwen.

## Design Approach

The project separates responsibilities across components:

- **AI Client:** AI orchestration, RAG, prompt construction and tool invocation.
- **MCP Server:** Payment operations and database access.
- **PostgreSQL:** Payment transaction and gateway records.
- **Ollama:** Local LLM and embedding model execution.
- **SimpleVectorStore:** Historical incident vector storage and retrieval.

## Author

Tiru

Java | Spring Boot | Spring AI | LLM | RAG | Embeddings | Semantic Search | MCP | AI Engineering
