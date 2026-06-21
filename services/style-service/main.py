"""
CodeReview AI — Style Service  (port 8002)
──────────────────────────────────────────
Single responsibility: PEP-8 / pycodestyle analysis.
Returns a list of per-file results to whoever calls /analyze
(normally the Orchestrator).
"""

import io
import re
import sys

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List

import pycodestyle

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
app = FastAPI(title="CodeReview AI — Style Service", version="1.0.0")
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

# ── Core analysis ─────────────────────────────────────────────────────────────
def analyze_style(filename: str, source: str) -> dict:
    """
    Run pycodestyle against the source string.
    pycodestyle only writes to stdout, so we capture it via StringIO.
    """
    issues = []
    old_stdout = sys.stdout
    sys.stdout = buffer = io.StringIO()

    try:
        checker = pycodestyle.Checker(
            filename=filename,
            lines=source.splitlines(keepends=True),
            show_source=False,
            show_pep8=False,
        )
        checker.check_all()
    finally:
        sys.stdout = old_stdout

    raw = buffer.getvalue()
    for line in raw.splitlines():
        m = re.match(r".+?:(\d+):(\d+): ([A-Z]\d+) (.+)", line)
        if m:
            issues.append({
                "line":    int(m.group(1)),
                "col":     int(m.group(2)),
                "code":    m.group(3),
                "message": m.group(4),
            })

    return {
        "file":         filename,
        "total_issues": len(issues),
        "issues":       issues,
    }

# ── Routes ────────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok", "service": "style"}


@app.post("/analyze")
def analyze(request: AnalyzeRequest):
    log_header("STYLE — analyze")
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
            result = analyze_style(file.Filename, file.Content)
            log_success(f"{file.Filename}  issues={result['total_issues']}")
        except Exception as e:
            log_error(f"{file.Filename} — {e}")
            result = {
                "file":         file.Filename,
                "error":        str(e),
                "total_issues": 0,
                "issues":       [],
            }

        results.append(result)

    log_footer()
    return results


# ── Entry point ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8002, reload=True)
