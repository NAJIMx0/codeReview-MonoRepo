# codeReview-MonoRepo

<p align="center">
  <h3 align="center">AI-Powered Automated Code Review Platform</h3>
</p>

<p align="center">
  Automatically analyzes code changes on every GitHub push using a scalable event-driven microservices architecture powered by Spring Boot, FastAPI, Apache Kafka, and Large Language Models.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange">
  <img src="https://img.shields.io/badge/Spring_Boot-4.0-success">
  <img src="https://img.shields.io/badge/Python-3.x-blue">
  <img src="https://img.shields.io/badge/FastAPI-Enabled-green">
  <img src="https://img.shields.io/badge/Kafka-Event_Driven-black">
  <img src="https://img.shields.io/badge/Docker-Containerized-blue">
  <img src="https://img.shields.io/badge/React-19-61DAFB">
</p>

---

## Overview

**codeReview-MonoRepo** is an AI-powered automated code review platform built using a microservices architecture. The platform listens to GitHub push events, retrieves modified source files, performs multiple stages of static analysis, and generates intelligent code review feedback using Large Language Models.

The system combines Spring Boot microservices, FastAPI analysis services, Apache Kafka event streaming, Docker containerization, and AI-powered review generation to provide automated insights regarding code quality, complexity, style compliance, duplication, and maintainability.

---

## Key Features

* GitHub OAuth2 authentication
* Automatic repository connection and webhook creation
* Real-time GitHub push event processing
* Event-driven communication using Apache Kafka
* Cyclomatic complexity analysis
* PEP 8 style compliance verification
* Duplicate code detection
* AI-generated code review using LLaMA 3.3 70B
* Service discovery with Eureka
* Centralized configuration with Spring Cloud Config
* MongoDB event persistence
* PostgreSQL user and repository management
* Fully containerized deployment with Docker Compose

---

## Table of Contents

* [Architecture](#architecture)
* [Services](#services)
* [Tech Stack](#tech-stack)
* [Docker Infrastructure](#docker-infrastructure)
* [Prerequisites](#prerequisites)
* [Getting Started](#getting-started)
* [System Workflow](#system-workflow)
* [Event Pipeline](#event-pipeline)
* [API Endpoints](#api-endpoints)
* [GitHub OAuth Setup](#github-oauth-setup)
* [Project Structure](#project-structure)
* [Future Improvements](#future-improvements)

---

# Architecture

<p align="center">
  <img width="100%" src="https://github.com/user-attachments/assets/811e0867-3e96-47a9-8251-7e75320887dd">
</p>

Services register with **Eureka** (`discovery-service:8761`) and retrieve their configurations from **Spring Cloud Config Server** (`config-server:8888`).

---

# Services

## Java / Spring Boot Services

| Service           | Port | Description                                            |
| ----------------- | ---- | ------------------------------------------------------ |
| config-server     | 8888 | Centralized configuration server                       |
| discovery-service | 8761 | Eureka service registry                                |
| auth-service      | 8080 | GitHub OAuth2 authentication and repository management |
| webhook-service   | 8999 | Receives GitHub webhook events                         |
| generate-service  | 8998 | Retrieves changed files and publishes events to Kafka  |

---

## Python / FastAPI Services

| Service             | Port        | Description                                   |
| ------------------- | ----------- | --------------------------------------------- |
| orchestrator        | 8181 (8000) | Coordinates all analysis services             |
| complexity-service  | 8001        | Cyclomatic complexity analysis using Radon    |
| style-service       | 8002        | PEP 8 compliance analysis                     |
| duplication-service | 8003        | Duplicate code detection                      |
| ai-review-service   | -           | AI-powered review generation using Groq LLaMA |

---

## Frontend

| Application | Port | Technology                                        |
| ----------- | ---- | ------------------------------------------------- |
| frontend    | 5173 | React 19, Vite, Tailwind CSS, Axios, React Router |

---

# Tech Stack

| Category          | Technologies                         |
| ----------------- | ------------------------------------ |
| Backend           | Java 17, Spring Boot, Spring Cloud   |
| Security          | Spring Security OAuth2 Client        |
| Data Access       | Spring Data JPA, Spring Data MongoDB |
| Databases         | PostgreSQL, MongoDB                  |
| Messaging         | Apache Kafka, Confluent Platform     |
| Service Discovery | Eureka                               |
| Configuration     | Spring Cloud Config                  |
| Analysis          | Python, FastAPI, Radon, Pycodestyle  |
| AI                | Groq SDK, LLaMA 3.3 70B              |
| Frontend          | React 19, Vite, Tailwind CSS         |
| DevOps            | Docker, Docker Compose               |

---

# Docker Infrastructure

The entire platform is fully containerized using Docker Compose.

Infrastructure includes:

* PostgreSQL
* MongoDB
* Kafka
* Zookeeper
* pgAdmin
* Mongo Express

Application services include:

* Config Server
* Discovery Service
* Auth Service
* Webhook Service
* Generate Service
* Orchestrator
* Complexity Service
* Style Service
* Duplication Service
* AI Review Service
* Frontend

All containers communicate through a shared bridge network:

```text
microservices-net
```

<p align="center">
  <img src="https://github.com/user-attachments/assets/9952077e-488e-495e-9ae2-beaa47b21ddd">
</p>

## Service Ports

| Service       | Port  |
| ------------- | ----- |
| PostgreSQL    | 5433  |
| pgAdmin       | 5050  |
| MongoDB       | 27017 |
| Mongo Express | 8081  |
| Kafka         | 9092  |
| Zookeeper     | 2181  |

---

# Prerequisites

Before running the project, ensure the following tools are installed:

* Java 17+
* Maven
* Docker Desktop
* Python 3.x
* Node.js 18+
* Git
* ngrok account

---

# Getting Started

## 1. Clone Repository

```bash
git clone <repository-url>
cd codeReview-MonoRepo
```

## 2. Configure Environment Variables

Create a `.env` file:

```env
GROQ_API_KEY=your_groq_api_key_here
```

---

## 3. Start Infrastructure

```bash
docker compose up -d
```

---

## 4. Start Spring Boot Services

```bash
# Config Server
cd services/config-server
mvn spring-boot:run

# Discovery Service
cd services/discovery-service
mvn spring-boot:run

# Webhook Service
cd services/webhook-service
mvn spring-boot:run

# Auth Service
cd services/auth-service
mvn spring-boot:run

# Generate Service
cd services/generate-service
mvn spring-boot:run
```

---

## 5. Start Python Services

```bash
# Orchestrator
cd services/orchestrator
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Repeat the process for:

* complexity-service
* style-service
* duplication-service
* ai-review-service

---

## 6. Expose Webhook Endpoint

```bash
ngrok http 8999
```

Copy the generated URL and update the webhook URL used inside the authentication service.

---

## 7. Start Frontend

```bash
cd frontend

npm install

npm run dev
```

Open:

```text
http://localhost:5173
```

---

# System Workflow

```text
Developer Pushes Code
          |
          v
GitHub Push Event
          |
          v
Webhook Service
          |
          v
MongoDB Storage
          |
          v
Generate Service
          |
          v
Kafka Event
          |
          v
Orchestrator
      /    |    \
     /     |     \
Complexity Style Duplication
     \      |      /
      \     |     /
          AI Review
              |
              v
      Review Results
```

---

# Event Pipeline

```text
GitHub Push
      |
      v
Webhook Service
      |
      v
MongoDB
      |
      v
Generate Service
      |
      v
Kafka Topic (push.event)
      |
      v
Orchestrator
      |
      +---- Complexity Service
      |
      +---- Style Service
      |
      +---- Duplication Service
      |
      +---- AI Review Service
                   |
                   v
Kafka Topic (review.result.ai)
```

---

# API Endpoints

| Method | Endpoint                         | Description                           |
| ------ | -------------------------------- | ------------------------------------- |
| GET    | /oauth2/authorization/github     | GitHub OAuth login                    |
| GET    | /api/auth/me                     | Authenticated user information        |
| GET    | /api/auth/repo                   | List GitHub repositories              |
| POST   | /api/auth/connect/{owner}/{repo} | Connect repository and create webhook |
| GET    | /api/auth/connected-repos        | List connected repositories           |
| POST   | /api/webhook/github              | Receive GitHub push events            |

---

# GitHub OAuth Setup

### Step 1

Navigate to:

```text
GitHub Settings
    └── Developer Settings
            └── OAuth Apps
```

### Step 2

Create a new OAuth Application.

### Step 3

Configure:

```text
Homepage URL:
http://localhost:5173

Callback URL:
http://localhost:8080/login/oauth2/code/github
```

### Step 4

Copy:

* Client ID
* Client Secret

### Step 5

Update:

```text
config-server/configurations/auth-service.yml
```

with the generated credentials.

---

# Project Structure

```text
codeReview-MonoRepo
│
├── docker-compose.yml
├── .env
│
├── services
│   │
│   ├── config-server
│   ├── discovery-service
│   ├── auth-service
│   ├── webhook-service
│   ├── generate-service
│   ├── orchestrator
│   ├── complexity-service
│   ├── style-service
│   ├── duplication-service
│   └── ai-review-service
│
├── frontend
│   │
│   ├── src
│   │   ├── components
│   │   ├── hooks
│   │   ├── pages
│   │   └── services
│   ├── public
│   ├── package.json
│   └── vite.config.js
│
└── README.md
```

---

# Future Improvements

* Pull Request review support
* Multi-language analysis
* SonarQube integration
* Historical review dashboard
* Email notifications
* Slack integration
* AI-generated fix suggestions
* Review metrics and analytics

---

<p align="center">
Built with ❤️ as a learning-by-doing project focused on Microservices, Event-Driven Architecture, Cloud-Native Development, and AI-Powered Code Analysis.
</p>
