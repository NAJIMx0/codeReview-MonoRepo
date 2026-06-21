"""
CodeReview AI — Duplication Service  (port 8003)
─────────────────────────────────────────────────
Detects three kinds of duplication:
  1. Identical whole-function bodies (original check)
  2. Identical variable assignments (original check)
  3. Structurally identical top-level loop blocks within a function,
     even when variable names differ only by a trailing number
     (e.g. "total" vs "total2") — this catches copy-pasted loops that
     the first two checks can't see, since they're not duplicate
     functions and not duplicate single assignments.
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
app = FastAPI(title="CodeReview AI — Duplication Service", version="1.1.0")
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


def normalize_block(node: ast.AST) -> str:
    """
    Convert a code block to a string for comparison, with variable names
    normalized so structurally-identical-but-differently-named code
    (e.g. "total" vs "total2") compares equal. Strips trailing digits
    from identifiers — a common copy-paste pattern.
    """
    try:
        code = ast.unparse(node)
    except Exception:
        return ""
    return re.sub(r'\b([a-zA-Z_]+)\d+\b', r'\1', code)


def find_duplicate_loops(tree: ast.AST) -> list:
    """
    Find top-level for-loops within each function that are structurally
    identical after variable-name normalization.
    """
    duplicates = []

    for func_node in ast.walk(tree):
        if not isinstance(func_node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            continue

        loop_groups: dict[str, list] = {}
        for stmt in func_node.body:
            if isinstance(stmt, (ast.For, ast.While)):
                norm = normalize_block(stmt)
                if norm:
                    loop_groups.setdefault(norm, []).append(stmt.lineno)

        for norm, lines in loop_groups.items():
            if len(lines) > 1:
                duplicates.append({
                    "function": func_node.name,
                    "lines": lines,
                    "suggestion": (
                        f"Loops at lines {lines} in function '{func_node.name}' "
                        "have the same structure (just different variable names). "
                        "Consider merging them or extracting a shared helper function."
                    ),
                })

    return duplicates


# ── Core analysis ─────────────────────────────────────────────────────────────
def analyze_duplication(filename: str, source: str) -> dict:
    duplicated_functions = []
    duplicated_variables = []
    duplicated_blocks = []

    try:
        tree = ast.parse(source)
    except SyntaxError as e:
        return {"error": f"SyntaxError: {e}", "duplicated_functions": [], "duplicated_variables": [], "duplicated_blocks": []}

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

    duplicated_blocks = find_duplicate_loops(tree)

    total = len(duplicated_functions) + len(duplicated_variables) + len(duplicated_blocks)
    return {
        "file": filename,
        "total_duplications": total,
        "duplicated_functions": duplicated_functions,
        "duplicated_variables": duplicated_variables,
        "duplicated_blocks": duplicated_blocks,
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
                f"dup_vars={len(result['duplicated_variables'])}  "
                f"dup_blocks={len(result.get('duplicated_blocks', []))}"
            )
        except Exception as e:
            log_error(f"{file.Filename} — {e}")
            result = {
                "file":                 file.Filename,
                "error":                str(e),
                "total_duplications":   0,
                "duplicated_functions": [],
                "duplicated_variables": [],
                "duplicated_blocks":    [],
            }

        results.append(result)

    log_footer()
    return results


# ── Entry point ───────────────────────────────────────────────────────────────
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8003, reload=True)