# codeReview-MonoRepo

AI-powered code review platform that automatically analyzes code changes on every GitHub push.

## Architecture

Microservices monorepo with the following services:

- **config-server** (8888) — Spring Cloud Config, serves configs for all services
- **discovery** (8761) — Eureka Server, service registry
- **auth-service** (8080) — GitHub OAuth2 login, user management, webhook creation
- **webhook-service** (8999) — Receives GitHub push events, stores to MongoDB
- **generate-service** (8998) — ⏳ Fetches changed file content from GitHub API
- **fastapi-service** (8000) — ⏳ AI code analysis (radon, pycodestyle)
- **frontend** (5173) — React + Vite + Tailwind, terminal-style UI

## Tech Stack

**Backend:** Java 17, Spring Boot 4.0.6, Spring Cloud 2025.1.1, Spring Security OAuth2, Spring Data JPA, Spring Data MongoDB

**Database:** PostgreSQL (users, connected repos), MongoDB (push events)

**Infrastructure:** Docker, Eureka, Spring Cloud Config

**Frontend:** React, Vite, Tailwind CSS, Axios, React Router

## Infrastructure (Docker)

```yaml
PostgreSQL    → localhost:5433
pgAdmin       → localhost:5050
MongoDB       → localhost:27017
Mongo Express → localhost:8081
MailDev       → localhost:1080 (UI), 1025 (SMTP)
```

## Getting Started

### Prerequisites
- Java 17+
- Docker Desktop
- Node.js 18+
- ngrok account

### 1. Start Infrastructure
```bash
docker compose up -d
```

### 2. Start Services (in order)
```bash
# 1. Config Server
cd services/config-server
mvn spring-boot:run

# 2. Discovery (Eureka)
cd services/discovery
mvn spring-boot:run

# 3. Webhook Service
cd services/webhook-service
mvn spring-boot:run

# 4. Auth Service
cd services/auth-service
mvn spring-boot:run
```

### 3. Start ngrok
```bash
ngrok http 8999
```
Update the ngrok URL in `auth-service/AuthService.java` → `connectRepo()` method.

### 4. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### 5. Open App
```
http://localhost:5173
```

## Features

- ✅ GitHub OAuth2 login
- ✅ Auto-save user + access token to PostgreSQL
- ✅ Fetch user's GitHub repositories
- ✅ One-click webhook creation on any repo
- ✅ Receive and store push events in MongoDB
- ✅ Track connected repositories
- ✅ Terminal-style React frontend
- ⏳ AI code review via FastAPI
- ⏳ Generate service for fetching file content

## Flow

```
User clicks Login → GitHub OAuth2 →
Auth Service saves user + token →
User selects repo → Webhook created on GitHub →
User pushes code → GitHub sends event to ngrok →
Webhook Service saves to MongoDB →
Generate Service fetches file content → (coming soon)
FastAPI analyzes code → (coming soon)
```

## GitHub OAuth App Setup

1. Go to GitHub → Settings → Developer Settings → OAuth Apps
2. Create new OAuth App:
    - Homepage: `http://localhost:5173`
    - Callback URL: `http://localhost:8080/login/oauth2/code/github`
3. Copy Client ID and Secret to `config-server/configurations/auth-service.yml`

## Key Endpoints

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/oauth2/authorization/github` | Start GitHub login |
| GET | `/api/auth/me` | Get logged in username |
| GET | `/api/auth/repo` | List GitHub repos |
| POST | `/api/auth/connect/{owner}/{repo}` | Create webhook + save connected repo |
| GET | `/api/auth/connected-repos` | Get connected repos |
| POST | `/api/webhook/github` | Receive GitHub push events |

## Project Structure

```
codeReview-MonoRepo/
├── docker-compose.yml
├── services/
│   ├── config-server/
│   ├── discovery/
│   ├── auth-service/
│   └── webhook-service/
└── frontend/
    ├── src/
    │   ├── components/
    │   │   ├── Navbar.jsx
    │   │   └── RepoCard.jsx
    │   ├── hooks/
    │   │   └── useAuth.js
    │   ├── pages/
    │   │   ├── LandingPage.jsx
    │   │   └── Dashboard.jsx
    │   └── services/
    │       ├── api.js
    │       └── auth.js
    └── package.json
```

---

> Built with ❤️ — learning by doing 🚀