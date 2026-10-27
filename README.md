# Payment AI Client

A Spring Boot application that connects an AI model with payment investigation tools exposed by an MCP server.

## Features

- Connects to an MCP server and discovers available tools
- Retrieves payment transaction and gateway details
- Uses Ollama with Qwen 2.5 locally for AI-assisted investigation
- Classifies payment investigation risk
- Exposes a REST API for the React dashboard

## Technology Stack

- Java 21
- Spring Boot 4.1.2
- Spring AI 2.0.1
- Model Context Protocol (MCP)
- Ollama — Qwen 2.5
- Maven

## Architecture

React Dashboard → Spring Boot AI Client → MCP Server → PostgreSQL

The AI client retrieves transaction and gateway data through MCP tools, then combines that evidence with the user's question to generate an investigation response.

## Prerequisites

- Java 21
- Maven
- Ollama running locally with `qwen2.5:3b`
- Payment AI MCP server running on port `8081`

## Run Locally

Start the application:

```bash
mvn spring-boot:run
```

The client runs on port `8082`.

## API Endpoint

`POST /api/ai/investigate`

Example request:

```json
{
  "message": "Investigate TXN1001"
}
```

Example local URL:

`http://localhost:8082/api/ai/investigate`

## Purpose

This project demonstrates Spring Boot REST API development, MCP tool integration, local LLM integration, and evidence-based payment investigation.
