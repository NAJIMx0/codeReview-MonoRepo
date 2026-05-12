from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List
import ast
import textwrap
import io
import sys
import re

import radon.complexity as radon_cc
import radon.metrics as radon_metrics
import pycodestyle

app = FastAPI(title="CodeReview AI — Analyzer", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ──────────────────────────────────────────────
#  Input / Output models
# ──────────────────────────────────────────────

class FileInput(BaseModel):
    Filename: str
    Content: str

class AnalyzeRequest(BaseModel):
    files: List[FileInput]
    repoName: str = ""


# ──────────────────────────────────────────────
#  Helpers
# ──────────────────────────────────────────────

def complexity_label(cc: int) -> str:
    """Map radon cyclomatic complexity score to Big-O estimate + explanation."""
    if cc == 1:
        return "O(1)", "Single straight-line path — no branches or loops."
    elif cc == 2:
        return "O(log n)", "One conditional / simple loop — logarithmic at worst."
    elif cc <= 4:
        return "O(n)", "A few branches or a single loop over input — linear."
    elif cc <= 7:
        return "O(n log n)", "Nested conditional logic or loop with sub-sorting — common in divide-and-conquer."
    elif cc <= 10:
        return "O(n²)", "Nested loops or multiple branching paths — quadratic, watch for large inputs."
    else:
        return "O(2ⁿ) / O(n!)", "Very high branching factor — exponential or factorial; refactoring strongly recommended."


def analyze_complexity(filename: str, source: str) -> dict:
    """Run radon cyclomatic complexity on every function/method."""
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
            "rank": block.letter,   # A-F scale from radon
            "big_o": big_o,
            "reason": reason,
        })

    # overall file score
    overall_cc = max((b.complexity for b in results), default=1)
    overall_big_o, overall_reason = complexity_label(overall_cc)

    return {
        "file": filename,
        "overall_big_o": overall_big_o,
        "overall_reason": overall_reason,
        "functions": functions,
    }


def analyze_style(filename: str, source: str) -> dict:
    """Run pycodestyle (PEP 8) on the source string."""
    issues = []

    # pycodestyle works on files; we feed it via a fake stdin
    style_guide = pycodestyle.StyleGuide(quiet=True)

    # capture output
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
        # format: filename:line:col: Exxxx message
        match = re.match(r".+?:(\d+):(\d+): ([A-Z]\d+) (.+)", line)
        if match:
            issues.append({
                "line": int(match.group(1)),
                "col": int(match.group(2)),
                "code": match.group(3),
                "message": match.group(4),
            })

    return {
        "file": filename,
        "total_issues": len(issues),
        "issues": issues,
    }


def analyze_duplication(filename: str, source: str) -> dict:
    """
    Detect duplicated logic:
      1. Identical function bodies (token-normalised)
      2. Repeated variable assignments with the same value
    """
    duplicated_functions = []
    duplicated_variables = []

    try:
        tree = ast.parse(source)
    except SyntaxError as e:
        return {"error": f"SyntaxError: {e}", "duplicated_functions": [], "duplicated_variables": []}

    # ── 1. Duplicate function bodies ──────────────────────────
    func_bodies: dict[str, list] = {}

    for node in ast.walk(tree):
        if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            # normalise: strip docstring, unparse body
            body = node.body
            if (body and isinstance(body[0], ast.Expr)
                    and isinstance(body[0].value, ast.Constant)
                    and isinstance(body[0].value.value, str)):
                body = body[1:]   # remove docstring

            try:
                body_src = "\n".join(ast.unparse(s) for s in body)
            except Exception:
                continue

            # strip whitespace for comparison
            key = re.sub(r"\s+", " ", body_src).strip()
            func_bodies.setdefault(key, []).append({
                "name": node.name,
                "line": node.lineno,
            })

    for key, funcs in func_bodies.items():
        if len(funcs) > 1:
            names = [f["name"] for f in funcs]
            lines = [f["line"] for f in funcs]
            duplicated_functions.append({
                "functions": names,
                "lines": lines,
                "suggestion": (
                    f"Functions {names} have identical bodies. "
                    f"Extract into a single shared function and call it from both places."
                ),
            })

    # ── 2. Duplicate variable assignments ─────────────────────
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
                "suggestion": (
                    f"'{var}' is assigned the same value '{val}' on lines {lines}. "
                    f"Define it once (e.g. as a constant at the top) and reuse it."
                ),
            })

    total = len(duplicated_functions) + len(duplicated_variables)

    return {
        "file": filename,
        "total_duplications": total,
        "duplicated_functions": duplicated_functions,
        "duplicated_variables": duplicated_variables,
    }


def score_file(complexity: dict, style: dict, duplication: dict) -> int:
    """Compute a 0-100 quality score."""
    score = 100

    # complexity penalty (max -40)
    for fn in complexity.get("functions", []):
        cc = fn["cyclomatic_complexity"]
        if cc > 10:
            score -= 10
        elif cc > 7:
            score -= 5
        elif cc > 4:
            score -= 2

    score = max(score, 60)  # floor after complexity

    # style penalty (max -30, 1 pt per issue, cap 30)
    style_penalty = min(style.get("total_issues", 0), 30)
    score -= style_penalty

    # duplication penalty (max -30, 5 pt each)
    dup_penalty = min(duplication.get("total_duplications", 0) * 5, 30)
    score -= dup_penalty

    return max(score, 0)


# ──────────────────────────────────────────────
#  Routes
# ──────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze")
def analyze(request: AnalyzeRequest):
    """
    Main endpoint — called by generate-service.
    Accepts a list of { Filename, Content } objects + repoName.
    Returns full analysis JSON per file.
    """
    results = []

    for file in request.files:
        filename = file.Filename
        source = file.Content

        # skip non-Python files gracefully
        if not filename.endswith(".py"):
            results.append({
                "file": filename,
                "skipped": True,
                "reason": "Only Python files are analysed in this version.",
            })
            continue

        complexity = analyze_complexity(filename, source)
        style = analyze_style(filename, source)
        duplication = analyze_duplication(filename, source)
        quality_score = score_file(complexity, style, duplication)

        results.append({
            "file": filename,
            "repo": request.repoName,
            "quality_score": quality_score,
            "complexity": complexity,
            "style": style,
            "duplication": duplication,
        })

    return {"repo": request.repoName, "files_analyzed": len(results), "results": results}


@app.post("/complexity")
def route_complexity(request: AnalyzeRequest):
    """Standalone complexity endpoint."""
    return [analyze_complexity(f.Filename, f.Content) for f in request.files]


@app.post("/style")
def route_style(request: AnalyzeRequest):
    """Standalone style endpoint."""
    return [analyze_style(f.Filename, f.Content) for f in request.files]


@app.post("/duplication")
def route_duplication(request: AnalyzeRequest):
    """Standalone duplication endpoint."""
    return [analyze_duplication(f.Filename, f.Content) for f in request.files]


# ──────────────────────────────────────────────
#  Entry point
# ──────────────────────────────────────────────

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8181, reload=True)