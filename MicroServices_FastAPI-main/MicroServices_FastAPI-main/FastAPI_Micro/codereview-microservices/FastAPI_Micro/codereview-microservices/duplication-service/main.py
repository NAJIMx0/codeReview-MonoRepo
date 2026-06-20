"""
CodeReview AI — Duplication Service  (port 8003)
─────────────────────────────────────────────────
Single responsibility: AST-based duplicate function / variable detection.
Returns a list of per-file results to whoever calls /analyze
(normally the Orchestrator).
"""

import ast
import re

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List

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
app = FastAPI(title="CodeReview AI — Duplication Service", version="1.0.0")
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
def analyze_duplication(filename: str, source: str) -> dict:
    try:
        tree = ast.parse(source)
    except SyntaxError as e:
        return {
            "file":                  filename,
            "error":                 f"SyntaxError: {e}",
            "total_duplications":    0,
            "duplicated_functions":  [],
            "duplicated_variables":  [],
        }

    # ── Duplicate function bodies ─────────────────────────────────────────────
    func_bodies: dict[str, list] = {}
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            body = node.body
            # Strip leading docstring if present
            if (body
                    and isinstance(body[0], ast.Expr)
                    and isinstance(body[0].value, ast.Constant)
                    and isinstance(body[0].value.value, str)):
                body = body[1:]
            try:
                body_src = "\n".join(ast.unparse(s) for s in body)
            except Exception:
                continue
            key = re.sub(r"\s+", " ", body_src).strip()
            func_bodies.setdefault(key, []).append({
                "name": node.name,
                "line": node.lineno,
            })

    duplicated_functions = []
    for funcs in func_bodies.values():
        if len(funcs) > 1:
            names = [f["name"] for f in funcs]
            lines = [f["line"] for f in funcs]
            duplicated_functions.append({
                "functions":  names,
                "lines":      lines,
                "suggestion": (
                    f"Functions {names} have identical bodies. "
                    "Extract into one shared helper."
                ),
            })

    # ── Duplicate variable assignments ────────────────────────────────────────
    assignments: dict[str, list] = {}
    for node in ast.walk(tree):
        if isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name):
                    try:
                        val = ast.unparse(node.value)
                    except Exception:
                        continue
                    key = f"{target.id}={val}"
                    assignments.setdefault(key, []).append(node.lineno)

    duplicated_variables = []
    for key, lines in assignments.items():
        if len(lines) > 1:
            var, val = key.split("=", 1)
            duplicated_variables.append({
                "variable":   var,
                "value":      val,
                "lines":      lines,
                "suggestion": (
                    f"'{var}' is assigned the same value '{val}' on lines {lines}. "
                    "Define it once as a module-level constant."
                ),
            })

    total = len(duplicated_functions) + len(duplicated_variables)
    return {
        "file":                 filename,
        "total_duplications":   total,
        "duplicated_functions": duplicated_functions,
        "duplicated_variables": duplicated_variables,
    }

# ── Routes ────────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok", "service": "duplication"}


@app.post("/analyze")
def analyze(request: AnalyzeRequest):
    log_header("DUPLICATION — analyze")
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
            result = analyze_duplication(file.Filename, file.Content)
            log_success(
                f"{file.Filename}  "
                f"dup_fns={len(result['duplicated_functions'])}  "
                f"dup_vars={len(result['duplicated_variables'])}"
            )
        except Exception as e:
            log_error(f"{file.Filename} — {e}")
            result = {
                "file":                 file.Filename,
                "error":                str(e),
                "total_duplications":   0,
                "duplicated_functions": [],
                "duplicated_variables": [],
            }

        results.append(result)

    log_footer()
    return results


# ── Entry point ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8003, reload=True)
