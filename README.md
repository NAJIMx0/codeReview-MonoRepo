# codeReview-MonoRepo

AI-powered code review platform that automatically analyzes code changes on every GitHub push using a microservices architecture.

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Services](#services)
- [Infrastructure](#infrastructure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Event Pipeline](#event-pipeline)
- [API Endpoints](#api-endpoints)
- [GitHub OAuth Setup](#github-oauth-setup)
- [Project Structure](#project-structure)

---

## Architecture

```
                    +------------------------------------------------------+
                    |                    GitHub                            |
                    |         (Push Event -> Webhook -> OAuth)              |
                    +------------+---------------------+------------------+
                                 |                     |
                         +-------v-------+    +--------v--------+
                         |  auth-service |    | webhook-service |
                         |   (8080)      |    |    (8999)       |
                         | GitHub OAuth  |    | Receive Events  |
                         | Webhook Mgmt  |    | Store to MongoDB|
                         +-------+-------+    +--------+--------+
                                 |                     |
                         +-------v---------------------v--------+
                         |           generate-service           |
                         |             (8998)                   |
                         |     Fetch changed files via API      |
                         +----------------+---------------------+
                                          |
                                   +------v------+
                                   |    Kafka    |
                                   |   (9092)    |
                                   +------+------+
                          +---------------v----------------+
                          |         orchestrator           |
                          |       (8181 / :8000)           |
                          |  Coordinates review pipeline   |
                          +--+----------+----------+-------+
                             |          |          |
                     +-------v--+ +-----v----+ +---v--------+
                     |complexity| |  style   | |duplication  |
                     | (radon)  | |(pycodest)| |  detection  |
                     +----------+ +----------+ +-------------+
                             |
                     +-------v----------+
                     | ai-review-service |
                     |  Groq LLaMA 3.3  |
                     +------------------+
```

Services register with **Eureka** (discovery-service :8761) and pull config from **Spring Cloud Config** (config-server :8888).

---

## Services

### Java / Spring Boot

| Service | Port | Description |
|---------|------|-------------|
| config-server | `8888` | Centralized configuration for all services |
| discovery-service | `8761` | Eureka service registry and discovery |
| auth-service | `8080` | GitHub OAuth2 authentication, user management, and webhook creation |
| webhook-service | `8999` | Handles incoming GitHub push events and persists to MongoDB |
| generate-service | `8998` | Retrieves changed file content from GitHub's API and publishes to Kafka |

### Python / FastAPI

| Service | Port | Description |
|---------|------|-------------|
| orchestrator | `8181` (:8000) | Orchestrates multi-stage review pipeline across analysis services |
| complexity-service | `8001` | Cyclomatic complexity analysis using radon |
| style-service | `8002` | PEP 8 code style compliance via pycodestyle |
| duplication-service | `8003` | Detects duplicated code blocks |
| ai-review-service | -- | AI-powered review via Groq API (llama-3.3-70b-versatile), Kafka consumer |

### Frontend

| App | Port | Stack |
|-----|------|-------|
| frontend | `5173` | React 19, Vite, Tailwind CSS, Axios, React Router (separate repository) |

---

## Tech Stack

| Category | Technologies |
|----------|-------------|
| Backend | Java 17, Spring Boot 4.0.6, Spring Cloud 2025.1.1 |
| Security | Spring Security OAuth2 Client |
| Data Access | Spring Data JPA, Spring Data MongoDB |
| AI / Analysis | Python 3.x, FastAPI, radon, pycodestyle |
| AI Review | Groq SDK, llama-3.3-70b-versatile |
| Databases | PostgreSQL (users, connected repos), MongoDB (push events) |
| Messaging | Apache Kafka, Confluent CP 7.6, Zookeeper |
| Infrastructure | Docker, Docker Compose, Eureka, Spring Cloud Config |
| Frontend | React 19, Vite 8, Tailwind CSS 4, Axios, React Router 7 |

---

## Infrastructure

All infrastructure runs via Docker Compose:

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | `5433` | Relational database (users, repos) |
| pgAdmin | `5050` | Database administration UI |
| MongoDB | `27017` | Document store (push events) |
| Mongo Express | `8081` | MongoDB administration UI |
| Zookeeper | `2181` | Kafka coordination |
| Kafka | `9092` | Event streaming between services |

---

## Prerequisites

- Java 17+
- Docker Desktop
- Python 3.x (for local Python service development)
- Node.js 18+
- ngrok account (for GitHub webhook tunneling)

---

## Getting Started

### 1. Clone and configure

```bash
git clone <repo-url>
cd codeReview-MonoRepo
```

Copy `.env.example` to `.env` and set your `GROQ_API_KEY`:

```env
GROQ_API_KEY=your_groq_api_key_here
```

### 2. Start infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL, MongoDB, pgAdmin, Mongo Express, Kafka, Zookeeper, and all containerized services.

### 3. Start services (development mode)

Start Java services in order (each in a separate terminal):

```bash
# 1. Config Server
cd services/config-server
mvn spring-boot:run

# 2. Discovery (Eureka)
cd services/discovery-service
mvn spring-boot:run

# 3. Webhook Service
cd services/webhook-service
mvn spring-boot:run

# 4. Auth Service
cd services/auth-service
mvn spring-boot:run

# 5. Generate Service
cd services/generate-service
mvn spring-boot:run
```

### 4. Start Python services

```bash
# Orchestrator
cd services/orchestrator
pip install -r requirements.txt
uvicorn main:app --reload --port 8000

# Repeat for complexity-service, style-service, duplication-service, ai-review-service
```

### 5. Expose webhook endpoint

```bash
ngrok http 8999
```

Copy the generated ngrok URL and update the webhook URL in `auth-service`'s `connectRepo()` method.

### 6. Start the frontend

```bash
cd path/to/frontend
npm install
npm run dev
```

### 7. Open application

Navigate to **http://localhost:5173**.

---

## Event Pipeline

```
GitHub Push -> Webhook Service -> MongoDB
                                    |
                         Generate Service
                                    |
                         Kafka Topic (push.event)
                                    |
                         Orchestrator
                         +-- Complexity Service
                         +-- Style Service
                         +-- Duplication Service
                         +-- ai-review-service -> Groq LLaMA
                                                    |
                         Kafka Topic (review.result.ai)
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/oauth2/authorization/github` | Initiate GitHub OAuth2 login flow |
| GET | `/api/auth/me` | Retrieve authenticated user info |
| GET | `/api/auth/repo` | List user's GitHub repositories |
| POST | `/api/auth/connect/{owner}/{repo}` | Create webhook and register repository |
| GET | `/api/auth/connected-repos` | List connected repositories |
| POST | `/api/webhook/github` | Receive GitHub push event payloads |

---

## GitHub OAuth Setup

1. Navigate to **GitHub Settings -> Developer Settings -> OAuth Apps**
2. Click **New OAuth App** and configure:
   - **Homepage URL:** `http://localhost:5173`
   - **Callback URL:** `http://localhost:8080/login/oauth2/code/github`
3. Copy the generated **Client ID** and **Client Secret**
4. Update `config-server/configurations/auth-service.yml` with these values

---

## Project Structure

```
codeReview-MonoRepo/
+-- docker-compose.yml          # Infrastructure & service orchestration
+-- .env                        # Environment variables (GROQ_API_KEY, etc.)
+-- services/
|   +-- config-server/          # Spring Cloud Config Server
|   +-- discovery-service/      # Eureka Service Registry
|   +-- auth-service/           # GitHub OAuth2 authentication
|   +-- webhook-service/        # GitHub webhook event receiver
|   +-- generate-service/       # File content fetcher + Kafka producer
|   +-- orchestrator/           # Python review pipeline coordinator
|   +-- complexity-service/     # Cyclomatic complexity analysis
|   +-- style-service/          # Code style compliance
|   +-- duplication-service/    # Code duplication detection
|   +-- ai-review-service/      # Groq-powered AI review
+-- README.md
```

---

> Built with love -- learning by doing
