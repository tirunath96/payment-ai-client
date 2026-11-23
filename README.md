# Payment AI Client

An AI-powered Payment Operations Agent built using Java, Spring Boot, Spring AI, and Model Context Protocol (MCP).

The application investigates payment failures, retrieves transaction and gateway information, and helps identify possible causes and next steps.

## Overview

The Payment AI Client acts as the AI integration layer between the application and the Payment MCP Server.

It enables intelligent payment investigation using locally hosted Large Language Models (LLMs).

## Technology Stack

- Java 21
- Spring Boot
- Spring AI
- Ollama
- Qwen 2.5 (3B)
- Model Context Protocol (MCP)
- PostgreSQL
- Maven
- REST APIs

## Architecture

Dashboard / API Request  
↓  
Spring Boot AI Client  
↓  
Spring AI + Ollama  
↓  
MCP Client  
↓  
Payment MCP Server  
↓  
Payment Database (PostgreSQL)  
↓  
Transaction and Gateway Information  
↓  
AI-powered Payment Investigation  
↓  
Investigation Result

## Key Capabilities

- Payment transaction investigation.
- Transaction and gateway status retrieval.
- AI-assisted payment failure analysis.
- MCP-based communication between AI client and payment tools.
- Payment event producer and simulator components.

## Project Structure

```text
payment-ai-client/
│
├── src/
│   └── main/
│       └── java/
│           └── com/tiru/
│               ├── PaymentEvent.java
│               ├── PaymentEventController.java
│               ├── PaymentEventProducer.java
│               └── PaymentEventSimulator.java
│
├── pom.xml
└── README.md
```

## AI Model

The project uses Ollama to run Qwen 2.5 (3B) locally, avoiding dependency on paid cloud LLM APIs during development.

## Project Goal

Build an intelligent payment operations system that goes beyond traditional transaction monitoring by combining AI reasoning, tool execution, and payment-system integration.

## Future Enhancements

- Proactive payment failure monitoring.
- Kafka-based real-time event processing.
- Intelligent anomaly detection.
- Automated root-cause investigation.
- AI-driven payment recovery recommendations.
- Customer notification workflows.

## Author

**Tiru**

Java | Spring Boot | Microservices | AI Engineering | MCP
