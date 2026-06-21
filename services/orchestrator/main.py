"""
CodeReview AI — Orchestrator
────────────────────────────
Receives analysis requests from Spring Boot, fans them out concurrently
to all three downstream microservices, merges + scores the results,
then publishes the final payload to Kafka (topic: review.result) for
generate-service to consume.
"""

import asyncio
import json
import os
import httpx

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional

from confluent_kafka import Producer

# ── Service URLs (override via env in docker-compose) ────────────────────────
COMPLEXITY_URL  = os.getenv("COMPLEXITY_URL",  "http://complexity-service:8001")
STYLE_URL       = os.getenv("STYLE_URL",       "http://style-service:8002")
DUPLICATION_URL = os.getenv("DUPLICATION_URL", "http://duplication-service:8003")

# ── Kafka config ───────────────────────────────────────────────────────────
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
KAFKA_REVIEW_TOPIC = os.getenv("KAFKA_REVIEW_TOPIC", "review.result")

# ── ANSI colours ─────────────────────────────────────────────────────────────
RESET  = "\033[0m";  BOLD   = "\033[1m";  GREEN  = "\033[32m"
CYAN   = "\033[36m"; YELLOW = "\033[33m"; RED    = "\033[31m"; DIM = "\033[2m"

def log_header(label: str):
    bar = "─" * max(0, 50 - len(label))
    print(f"\n{CYAN}{BOLD}┌─── {label} {bar}{RESET}")

def log_field(key: str, value: str):
    print(f"{CYAN}│  {RESET}{YELLOW}{key}{RESET}{DIM} → {RESET}{value}")

def log_success(msg: str):
    print(f"{CYAN}│  {RESET}{GREEN}✔  {msg}{RESET}")

def log_warn(msg: str):
    print(f"{CYAN}│  {RESET}{YELLOW}⚠  {msg}{RESET}")

def log_error(msg: str):
    print(f"{CYAN}│  {RESET}{RED}✖  {msg}{RESET}")

def log_footer():
    print(f"{CYAN}└{'─' * 55}{RESET}")

# ── App setup ─────────────────────────────────────────────────────────────────
app = FastAPI(title="CodeReview AI — Orchestrator", version="1.3.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],
)

# ── Kafka producer (lazy singleton, created on first use) ────────────────────
_producer: Optional[Producer] = None

def get_producer() -> Producer:
    global _producer
    if _producer is None:
        _producer = Producer({"bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS})
    return _producer

def _delivery_callback(err, msg):
    if err is not None:
        log_error(f"Kafka delivery failed — {err}")
    else:
        log_success(f"delivered to {msg.topic()} [partition {msg.partition()}]")

def publish_review_result(repo_name: str, payload: dict) -> bool:
    try:
        producer = get_producer()
        producer.produce(
            topic=KAFKA_REVIEW_TOPIC,
            key=(repo_name or "unknown").encode("utf-8"),
            value=json.dumps(payload).encode("utf-8"),
            callback=_delivery_callback,
        )
        remaining = producer.flush(timeout=10)
        if remaining > 0:
            log_error(f"Kafka flush timed out, {remaining} message(s) undelivered")
            return False
        return True
    except Exception as e:
        log_error(f"Kafka publish failed — {e}")
        return False

# ── Models ────────────────────────────────────────────────────────────────────
class FileInput(BaseModel):
    Filename: str
    Content: str

class AnalyzeRequest(BaseModel):
    files: List[FileInput]
    repoName: str = ""

# ── Scoring (lives only in the orchestrator) ──────────────────────────────────
def score_file(complexity: dict, style: dict, duplication: dict) -> int:
    score = 100
    for fn in complexity.get("functions", []):
        cc = fn["cyclomatic_complexity"]
        if cc > 10:   score -= 10
        elif cc > 7:  score -= 5
        elif cc > 4:  score -= 2
    score = max(score, 60)
    score -= min(style.get("total_issues", 0), 30)
    score -= min(duplication.get("total_duplications", 0) * 5, 30)
    return max(score, 0)

# ── Async helper: call one downstream service ─────────────────────────────────
async def call_service(client: httpx.AsyncClient, url: str, payload: dict) -> list:
    try:
        r = await client.post(f"{url}/analyze", json=payload, timeout=30.0)
        r.raise_for_status()
        return r.json()
    except httpx.TimeoutException:
        return [{"error": f"Timeout reaching {url}"}]
    except httpx.HTTPStatusError as e:
        return [{"error": f"HTTP {e.response.status_code} from {url}"}]
    except Exception as e:
        return [{"error": str(e)}]

# ── Health ─────────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok", "service": "orchestrator"}

@app.get("/health/services")
async def health_services():
    services = {
        "complexity":  COMPLEXITY_URL,
        "style":       STYLE_URL,
        "duplication": DUPLICATION_URL,
    }
    statuses = {}
    async with httpx.AsyncClient() as client:
        for name, base in services.items():
            try:
                r = await client.get(f"{base}/health", timeout=5.0)
                statuses[name] = {"status": "ok", "http": r.status_code}
            except Exception as e:
                statuses[name] = {"status": "unreachable", "error": str(e)}
    overall = "ok" if all(v["status"] == "ok" for v in statuses.values()) else "degraded"
    return {"orchestrator": "ok", "services": statuses, "overall": overall}

# ── Main orchestration endpoint ────────────────────────────────────────────────
@app.post("/analyze")
async def analyze(request: AnalyzeRequest):
    log_header("ORCHESTRATOR — analyze request")
    log_field("repo",  request.repoName or "(none)")
    log_field("files", str(len(request.files)))

    downstream_payload = {
        "files":    [f.model_dump() for f in request.files],
        "repoName": request.repoName,
    }

    log_field("fan-out", "complexity | style | duplication")
    async with httpx.AsyncClient() as client:
        complexity_data, style_data, duplication_data = await asyncio.gather(
            call_service(client, COMPLEXITY_URL,  downstream_payload),
            call_service(client, STYLE_URL,       downstream_payload),
            call_service(client, DUPLICATION_URL, downstream_payload),
        )

    def index_by_file(result_list: list) -> dict:
        return {item.get("file", ""): item for item in result_list if isinstance(item, dict)}

    complexity_by_file  = index_by_file(complexity_data)
    style_by_file       = index_by_file(style_data)
    duplication_by_file = index_by_file(duplication_data)

    # Index original file content by filename so we can attach it to each
    # result — the AI review service needs the actual source code, not just
    # the metrics, to catch things static analysis rules don't define well
    # (e.g. structurally duplicated logic blocks within a single function).
    content_by_file = {f.Filename: f.Content for f in request.files}

    merged_results = []
    for file in request.files:
        fname = file.Filename

        if not fname.endswith(".py"):
            log_warn(f"skipped (non-Python): {fname}")
            merged_results.append({
                "file":    fname,
                "skipped": True,
                "reason":  "Only Python files are analysed in this version.",
            })
            continue

        complexity  = complexity_by_file.get(fname,  {"error": "no response", "functions": []})
        style       = style_by_file.get(fname,       {"error": "no response", "total_issues": 0, "issues": []})
        duplication = duplication_by_file.get(fname, {"error": "no response", "total_duplications": 0})

        quality = score_file(complexity, style, duplication)
        log_success(f"merged  score={quality}  [{fname}]")

        merged_results.append({
            "file":          fname,
            "repo":          request.repoName,
            "quality_score": quality,
            "complexity":    complexity,
            "style":         style,
            "duplication":   duplication,
            "content":       content_by_file.get(fname, ""),
        })

    response_payload = {
        "repo":           request.repoName,
        "files_analyzed": len(merged_results),
        "results":        merged_results,
    }

    # ── Publish to Kafka instead of an HTTP callback ──────────────────────────
    log_field("publishing to kafka", f"topic={KAFKA_REVIEW_TOPIC}")
    published = publish_review_result(request.repoName, response_payload)
    if published:
        log_success(f"published review result to '{KAFKA_REVIEW_TOPIC}'")
    else:
        log_warn("kafka publish failed — result still returned synchronously below")

    log_footer()
    return response_payload


@app.post("/complexity")
async def route_complexity(request: AnalyzeRequest):
    payload = {"files": [f.model_dump() for f in request.files], "repoName": request.repoName}
    async with httpx.AsyncClient() as client:
        return await call_service(client, COMPLEXITY_URL, payload)


@app.post("/style")
async def route_style(request: AnalyzeRequest):
    payload = {"files": [f.model_dump() for f in request.files], "repoName": request.repoName}
    async with httpx.AsyncClient() as client:
        return await call_service(client, STYLE_URL, payload)


@app.post("/duplication")
async def route_duplication(request: AnalyzeRequest):
    payload = {"files": [f.model_dump() for f in request.files], "repoName": request.repoName}
    async with httpx.AsyncClient() as client:
        return await call_service(client, DUPLICATION_URL, payload)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)