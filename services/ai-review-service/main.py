"""
CodeReview AI — AI Review Service
──────────────────────────────────
Consumes raw analysis from Kafka topic "review.result" (published by the
orchestrator), asks Groq's Llama model to write a human-readable code
review comment for each file, then publishes the enriched result to
"review.result.ai" for generate-service to save + push to the frontend.

No web framework needed — this is just a consume/produce loop.
"""

import json
import os
import time

from confluent_kafka import Consumer, Producer
from groq import Groq

# ── Config ─────────────────────────────────────────────────────────────────
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
INPUT_TOPIC  = os.getenv("INPUT_TOPIC", "review.result")
OUTPUT_TOPIC = os.getenv("OUTPUT_TOPIC", "review.result.ai")
GROQ_API_KEY = os.getenv("GROQ_API_KEY")
GROQ_MODEL   = os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")

# ── ANSI colours (same style as the other services) ──────────────────────────
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


if not GROQ_API_KEY:
    log_error("GROQ_API_KEY is not set — the service will start but every AI call will fail.")

groq_client = Groq(api_key=GROQ_API_KEY) if GROQ_API_KEY else None


def build_prompt(file_result: dict) -> str:
    """Turn one file's raw analysis + actual source code into a prompt for the LLM."""
    filename = file_result.get("file", "unknown file")
    score = file_result.get("quality_score", "?")
    complexity = file_result.get("complexity", {})
    style = file_result.get("style", {})
    duplication = file_result.get("duplication", {})
    content = file_result.get("content", "")

    return f"""You are a senior software engineer reviewing a teammate's code in a pull request.

File: {filename}
Quality score: {score}/100

Static analysis results (these tools have real limitations — style only
checks PEP8 formatting, duplication only checks identical whole-function
bodies and identical variable assignments, so they can both miss
duplicated logic blocks within a single function):

Complexity analysis:
{json.dumps(complexity, indent=2)}

Style issues (PEP8 only):
{json.dumps(style, indent=2)}

Duplication issues (exact-match only):
{json.dumps(duplication, indent=2)}

Actual source code:
```python
{content}
```

Write a short, direct code review comment (3-5 sentences) based on all of
this. Specifically look at the actual source code yourself for:
- Repeated or near-identical logic blocks (e.g. two loops computing
  similar things with different variable names) — the static duplication
  checker above only catches exact matches, so check this yourself by
  reading the code.
- Naming or structure issues that a formatter wouldn't catch (e.g.
  numbered variable names like "total2", "count2" suggesting copy-paste).
- Anything genuinely concerning that the metrics above don't capture.

Be specific and actionable — mention concrete fixes, not generic advice.
If the code is genuinely clean with no real issues, say so briefly instead
of inventing problems. Do not repeat the raw numbers back verbatim;
explain what they mean in practice."""


def get_ai_review(file_result: dict) -> str:
    if groq_client is None:
        return "(AI review unavailable — GROQ_API_KEY not configured)"

    prompt = build_prompt(file_result)
    try:
        response = groq_client.chat.completions.create(
            model=GROQ_MODEL,
            messages=[{"role": "user", "content": prompt}],
            temperature=0.4,
            max_tokens=400,
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        log_error(f"Groq call failed — {e}")
        return f"(AI review failed: {e})"


def enrich_with_ai(payload: dict) -> dict:
    """Add an ai_review field to each non-skipped file result."""
    results = payload.get("results", [])
    for file_result in results:
        if file_result.get("skipped"):
            continue
        log_field("asking Groq/Llama for", file_result.get("file", "?"))
        file_result["ai_review"] = get_ai_review(file_result)
        log_success(f"ai_review added for {file_result.get('file', '?')}")
    return payload


def make_consumer() -> Consumer:
    return Consumer({
        "bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS,
        "group.id": "ai-review-service-group",
        "auto.offset.reset": "earliest",
    })


def make_producer() -> Producer:
    return Producer({"bootstrap.servers": KAFKA_BOOTSTRAP_SERVERS})


def delivery_callback(err, msg):
    if err is not None:
        log_error(f"Kafka delivery failed — {err}")
    else:
        log_success(f"delivered to {msg.topic()} [partition {msg.partition()}]")


def run():
    log_header("AI REVIEW SERVICE — starting")
    log_field("kafka", KAFKA_BOOTSTRAP_SERVERS)
    log_field("consuming", INPUT_TOPIC)
    log_field("producing", OUTPUT_TOPIC)
    log_field("model", GROQ_MODEL)
    log_footer()

    consumer = make_consumer()
    producer = make_producer()
    consumer.subscribe([INPUT_TOPIC])

    try:
        while True:
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                continue
            if msg.error():
                log_error(f"consumer error — {msg.error()}")
                continue

            log_header("AI REVIEW SERVICE — message received")
            try:
                payload = json.loads(msg.value().decode("utf-8"))
                repo_name = payload.get("repo", "unknown")
                log_field("repo", repo_name)
                log_field("files", str(payload.get("files_analyzed", "?")))

                enriched = enrich_with_ai(payload)

                producer.produce(
                    topic=OUTPUT_TOPIC,
                    key=repo_name.encode("utf-8"),
                    value=json.dumps(enriched).encode("utf-8"),
                    callback=delivery_callback,
                )
                producer.flush(timeout=10)

            except Exception as e:
                log_error(f"failed to process message — {e}")

            log_footer()

    except KeyboardInterrupt:
        pass
    finally:
        consumer.close()


if __name__ == "__main__":
    time.sleep(5)
    run()