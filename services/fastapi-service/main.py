from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List
import ast
import io
import sys
import re
import httpx

import radon.complexity as radon_cc
import radon.metrics as radon_metrics
import pycodestyle

# ── ANSI colours ────────────────────────────────────────────────────────────
RESET  = "\033[0m"
BOLD   = "\033[1m"
GREEN  = "\033[32m"
CYAN   = "\033[36m"
YELLOW = "\033[33m"
RED    = "\033[31m"
DIM    = "\033[2m"

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

# ── App setup ────────────────────────────────────────────────────────────────
app = FastAPI(title="CodeReview AI — Analyzer", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Models ───────────────────────────────────────────────────────────────────
class FileInput(BaseModel):
    Filename: str
    Content: str

class AnalyzeRequest(BaseModel):
    files: List[FileInput]
    repoName: str = ""


# ── Complexity ───────────────────────────────────────────────────────────────
def complexity_label(cc: int) -> tuple[str, str]:
    if cc == 1:
        return "O(1)", "Single straight-line path — no branches or loops."
    elif cc == 2:
        return "O(log n)", "One conditional / simple loop — logarithmic at worst."
    elif cc <= 4:
        return "O(n)", "A few branches or a single loop over input — linear."
    elif cc <= 7:
        return "O(n log n)", "Nested conditional logic or loop with sub-sorting."
    elif cc <= 10:
        return "O(n²)", "Nested loops or multiple branching paths — quadratic."
    else:
        return "O(2ⁿ) / O(n!)", "Very high branching factor — refactoring strongly recommended."


def analyze_complexity(filename: str, source: str) -> dict:
    try:
        results = radon_cc.cc_visit(source)
    except SyntaxError as e:
        return {"error": f"SyntaxError: {e}", "functions": []}

    functions = []
    for block in results:
        big_o, reason = complexity_label(block.complexity)
        functions.append({
            "name": block.name,
            "line": block.lineno,
            "cyclomatic_complexity": block.complexity,
            "rank": block.letter,
            "big_o": big_o,
            "reason": reason,
        })

    overall_cc = max((b.complexity for b in results), default=1)
    overall_big_o, overall_reason = complexity_label(overall_cc)
    return {
        "file": filename,
        "overall_big_o": overall_big_o,
        "overall_reason": overall_reason,
        "functions": functions,
    }


# ── Style ────────────────────────────────────────────────────────────────────
def analyze_style(filename: str, source: str) -> dict:
    issues = []
    old_stdout = sys.stdout
    sys.stdout = buffer = io.StringIO()

    checker = pycodestyle.Checker(
        filename=filename,
        lines=source.splitlines(keepends=True),
        show_source=False,
        show_pep8=False,
    )
    checker.check_all()

    sys.stdout = old_stdout
    raw = buffer.getvalue()

    for line in raw.splitlines():
        match = re.match(r".+?:(\d+):(\d+): ([A-Z]\d+) (.+)", line)
        if match:
            issues.append({
                "line": int(match.group(1)),
                "col": int(match.group(2)),
                "code": match.group(3),
                "message": match.group(4),
            })

    return {"file": filename, "total_issues": len(issues), "issues": issues}


# ── Duplication ──────────────────────────────────────────────────────────────
def analyze_duplication(filename: str, source: str) -> dict:
    duplicated_functions = []
    duplicated_variables = []

    try:
        tree = ast.parse(source)
    except SyntaxError as e:
        return {"error": f"SyntaxError: {e}", "duplicated_functions": [], "duplicated_variables": []}

    func_bodies: dict[str, list] = {}
    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            body = node.body
            if (body and isinstance(body[0], ast.Expr)
                    and isinstance(body[0].value, ast.Constant)
                    and isinstance(body[0].value.value, str)):
                body = body[1:]
            try:
                body_src = "\n".join(ast.unparse(s) for s in body)
            except Exception:
                continue
            key = re.sub(r"\s+", " ", body_src).strip()
            func_bodies.setdefault(key, []).append({"name": node.name, "line": node.lineno})

    for key, funcs in func_bodies.items():
        if len(funcs) > 1:
            names = [f["name"] for f in funcs]
            lines = [f["line"] for f in funcs]
            duplicated_functions.append({
                "functions": names,
                "lines": lines,
                "suggestion": f"Functions {names} have identical bodies. Extract into one shared function.",
            })

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

    for key, lines in assignments.items():
        if len(lines) > 1:
            var, val = key.split("=", 1)
            duplicated_variables.append({
                "variable": var,
                "value": val,
                "lines": lines,
                "suggestion": f"'{var}' assigned same value '{val}' on lines {lines}. Define once as a constant.",
            })

    total = len(duplicated_functions) + len(duplicated_variables)
    return {
        "file": filename,
        "total_duplications": total,
        "duplicated_functions": duplicated_functions,
        "duplicated_variables": duplicated_variables,
    }


# ── Scoring ──────────────────────────────────────────────────────────────────
def score_file(complexity: dict, style: dict, duplication: dict) -> int:
    score = 100
    for fn in complexity.get("functions", []):
        cc = fn["cyclomatic_complexity"]
        if cc > 10:
            score -= 10
        elif cc > 7:
            score -= 5
        elif cc > 4:
            score -= 2
    score = max(score, 60)
    score -= min(style.get("total_issues", 0), 30)
    score -= min(duplication.get("total_duplications", 0) * 5, 30)
    return max(score, 0)


# ── Routes ───────────────────────────────────────────────────────────────────
@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze")
def analyze(request: AnalyzeRequest):
    log_header("FASTAPI — analyzing")
    log_field("repo",  request.repoName or "(none)")
    log_field("files", str(len(request.files)))

    results = []

    for file in request.files:
        filename = file.Filename
        source   = file.Content

        if not filename.endswith(".py"):
            log_warn(f"skipped (non-Python): {filename}")
            results.append({
                "file": filename,
                "skipped": True,
                "reason": "Only Python files are analysed in this version.",
            })
            continue

        print(f"{CYAN}│  {DIM}  analysing → {RESET}{filename}")

        # wrap each step so we can see exactly which one crashes
        try:
            complexity = analyze_complexity(filename, source)
            log_success(f"complexity OK")
        except Exception as e:
            log_error(f"complexity FAILED — {e}")
            complexity = {"error": str(e), "functions": []}

        try:
            style = analyze_style(filename, source)
            log_success(f"style OK  issues={style['total_issues']}")
        except Exception as e:
            log_error(f"style FAILED — {e}")
            style = {"total_issues": 0, "issues": [], "error": str(e)}

        try:
            duplication = analyze_duplication(filename, source)
            log_success(f"duplication OK")
        except Exception as e:
            log_error(f"duplication FAILED — {e}")
            duplication = {"total_duplications": 0, "duplicated_functions": [], "duplicated_variables": [], "error": str(e)}

        quality = score_file(complexity, style, duplication)
        log_success(f"score={quality}  [{filename}]")

        results.append({
            "file": filename,
            "repo": request.repoName,
            "quality_score": quality,
            "complexity": complexity,
            "style": style,
            "duplication": duplication,
        })

    response_payload = {
        "repo":           request.repoName,
        "files_analyzed": len(results),
        "results":        results,
    }

    log_field("analyzed", f"{len(results)} files")
    log_field("forwarding to", "http://generate-service:8998/api/generate/holler")

    try:
        r = httpx.post(
            "http://generate-service:8998/api/generate/holler",
            json=response_payload,
            timeout=5,
        )
        log_success(f"holler accepted  HTTP {r.status_code}")
    except Exception as e:
        log_error(f"holler failed — {e}")

    log_footer()
    return response_payload

@app.post("/complexity")
def route_complexity(request: AnalyzeRequest):
    return [analyze_complexity(f.Filename, f.Content) for f in request.files]


@app.post("/style")
def route_style(request: AnalyzeRequest):
    return [analyze_style(f.Filename, f.Content) for f in request.files]


@app.post("/duplication")
def route_duplication(request: AnalyzeRequest):
    return [analyze_duplication(f.Filename, f.Content) for f in request.files]


# ── Entry point ──────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8181, reload=True)