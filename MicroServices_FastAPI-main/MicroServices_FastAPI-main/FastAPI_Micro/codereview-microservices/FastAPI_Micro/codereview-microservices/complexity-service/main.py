"""
CodeReview AI — Complexity Service  (port 8001)
────────────────────────────────────────────────
Single responsibility: cyclomatic-complexity analysis via Radon.
Returns a list of per-file results to whoever calls /analyze
(normally the Orchestrator).
"""

import sys
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List

import radon.complexity as radon_cc

# ── ANSI colours ──────────────────────────────────────────────────────────────
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
app = FastAPI(title="CodeReview AI — Complexity Service", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"], allow_methods=["*"], allow_headers=["*"],
)

# ── Models ────────────────────────────────────────────────────────────────────
class FileInput(BaseModel):
    Filename: str
    Content: str

class AnalyzeRequest(BaseModel):
    files: List[FileInput]
    repoName: str = ""

# ── Big-O label mapping ───────────────────────────────────────────────────────
def complexity_label(cc: int) -> tuple[str, str]:
    if cc == 1:
        return "O(1)",          "Single straight-line path — no branches or loops."
    elif cc == 2:
        return "O(log n)",      "One conditional / simple loop — logarithmic at worst."
    elif cc <= 4:
        return "O(n)",          "A few branches or a single loop over input — linear."
    elif cc <= 7:
        return "O(n log n)",    "Nested conditional logic or loop with sub-sorting."
    elif cc <= 10:
        return "O(n²)",         "Nested loops or multiple branching paths — quadratic."
    else:
        return "O(2ⁿ) / O(n!)", "Very high branching factor — refactoring strongly recommended."

# ── Core analysis ─────────────────────────────────────────────────────────────
def analyze_complexity(filename: str, source: str) -> dict:
    try:
        blocks = radon_cc.cc_visit(source)
    except SyntaxError as e:
        return {"file": filename, "error": f"SyntaxError: {e}", "functions": []}

    functions = []
    for block in blocks:
        big_o, reason = complexity_label(block.complexity)
        functions.append({
            "name":                  block.name,
            "line":                  block.lineno,
            "cyclomatic_complexity": block.complexity,
            "rank":                  block.letter,
            "big_o":                 big_o,
            "reason":                reason,
        })

    overall_cc = max((b.complexity for b in blocks), default=1)
    overall_big_o, overall_reason = complexity_label(overall_cc)

    return {
        "file":           filename,
        "overall_big_o":  overall_big_o,
        "overall_reason": overall_reason,
        "functions":      functions,
    }

# ── Routes ────────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok", "service": "complexity"}


@app.post("/analyze")
def analyze(request: AnalyzeRequest):
    log_header("COMPLEXITY — analyze")
    log_field("repo",  request.repoName or "(none)")
    log_field("files", str(len(request.files)))

    results = []
    for file in request.files:
        if not file.Filename.endswith(".py"):
            log_warn(f"skipped: {file.Filename}")
            results.append({
                "file":    file.Filename,
                "skipped": True,
                "reason":  "Only Python files are analysed.",
            })
            continue

        try:
            result = analyze_complexity(file.Filename, file.Content)
            log_success(f"{file.Filename}  overall={result['overall_big_o']}")
        except Exception as e:
            log_error(f"{file.Filename} — {e}")
            result = {"file": file.Filename, "error": str(e), "functions": []}

        results.append(result)

    log_footer()
    return results


# ── Entry point ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8001, reload=True)
