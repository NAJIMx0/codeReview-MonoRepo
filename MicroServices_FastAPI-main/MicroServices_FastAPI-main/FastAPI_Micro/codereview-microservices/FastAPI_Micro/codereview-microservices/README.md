# CodeReview AI — Microservices

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        EXTERNAL WORLD                           │
│                                                                 │
│   Spring Boot  ──POST /analyze──►  Orchestrator  :8000         │
│   (sends code)                           │                      │
│                                          │  asyncio.gather()   │
│                          ┌───────────────┼───────────────┐     │
│                          ▼               ▼               ▼     │
│                   Complexity       Style svc      Duplication   │
│                   svc :8001        :8002          svc :8003     │
│                          │               │               │     │
│                          └───────────────┴───────────────┘     │
│                                          │                      │
│                                   merge + score                 │
│                                          │                      │
│   Spring Boot  ◄──POST callbackUrl───────┘                      │
│   (receives results)                                            │
└─────────────────────────────────────────────────────────────────┘
```

All three analysis services are called **concurrently** by the orchestrator using
`asyncio.gather()` — total latency is the slowest single service, not the sum.

---

## Services

| Service            | Port | Responsibility                              |
|--------------------|------|---------------------------------------------|
| **orchestrator**   | 8000 | Fan-out, merge, score, Spring Boot callback |
| complexity-service | 8001 | Cyclomatic complexity via Radon              |
| style-service      | 8002 | PEP-8 violations via pycodestyle            |
| duplication-service| 8003 | Duplicate functions/variables via AST        |

---

## Quick Start

```bash
# 1. Clone / copy this folder, then:
cd codereview-microservices

# 2. (Optional) set your Spring Boot callback URL
export SPRINGBOOT_CALLBACK=http://your-springboot:8080/api/review/result

# 3. Build and start all services
docker compose up --build

# 4. Verify everything is healthy
curl http://localhost:8000/health/services
```

---

## Spring Boot Integration

Spring Boot sends a `POST` to `http://localhost:8000/analyze` (or however you've
exposed the orchestrator) with this JSON body:

```json
{
  "repoName": "my-java-project",
  "callbackUrl": "http://your-springboot:8080/api/review/result",
  "files": [
    {
      "Filename": "utils.py",
      "Content": "def add(a, b):\n    return a + b\n"
    }
  ]
}
```

`callbackUrl` is optional per-request. It overrides the `SPRINGBOOT_CALLBACK`
env var. The orchestrator **also returns the payload synchronously** in the HTTP
response, so Spring Boot can choose to read it directly instead of waiting for
the callback.

### Callback payload (sent back to Spring Boot)

```json
{
  "repo": "my-java-project",
  "files_analyzed": 1,
  "results": [
    {
      "file": "utils.py",
      "repo": "my-java-project",
      "quality_score": 87,
      "complexity": {
        "file": "utils.py",
        "overall_big_o": "O(1)",
        "overall_reason": "Single straight-line path — no branches or loops.",
        "functions": [
          {
            "name": "add",
            "line": 1,
            "cyclomatic_complexity": 1,
            "rank": "A",
            "big_o": "O(1)",
            "reason": "Single straight-line path — no branches or loops."
          }
        ]
      },
      "style": {
        "file": "utils.py",
        "total_issues": 0,
        "issues": []
      },
      "duplication": {
        "file": "utils.py",
        "total_duplications": 0,
        "duplicated_functions": [],
        "duplicated_variables": []
      }
    }
  ]
}
```

Non-Python files are returned with `"skipped": true` and a `"reason"` field.

---

## Quality Score

The orchestrator scores each file out of 100:

| Deduction trigger                        | Points lost    |
|------------------------------------------|----------------|
| Function with cyclomatic complexity > 10 | −10 per fn     |
| Function with cyclomatic complexity 8-10 | −5 per fn      |
| Function with cyclomatic complexity 5-7  | −2 per fn      |
| Style issues (PEP-8 violations)          | −1 per issue, max −30 |
| Duplicate functions/variables            | −5 per dup, max −30   |
| Floor after complexity deductions        | 60 minimum     |
| Absolute floor                           | 0              |

---

## Postman

Import `CodeReviewAI.postman_collection.json` into Postman.

The collection contains:
- Health checks for all 4 services
- Full orchestrator flow (single file, multi-file, syntax error, callback)
- Direct hits to each individual service for isolated testing
- Edge case tests (empty payload, non-Python files)

Each request has automated test scripts — run **Collection Runner** to execute
the full suite at once.

---

## API Reference

### `GET /health` — all services
```
200 { "status": "ok", "service": "<name>" }
```

### `GET /health/services` — orchestrator only
Pings all three downstream services and returns aggregated status.

### `POST /analyze` — orchestrator
Body: `{ "repoName": str, "callbackUrl": str?, "files": [{ "Filename": str, "Content": str }] }`

### `POST /analyze` — individual services (complexity / style / duplication)
Same body shape (minus `callbackUrl`). Returns a list of per-file results.
