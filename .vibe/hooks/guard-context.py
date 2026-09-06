#!/usr/bin/env python3
"""pre_tool guard for `bash` and `read_file`.

Enforces the rules Vibe's own tool permissions CANNOT express. Every rule here
is anchored on a number measured across the 10 `ybl/jpa-vibe` sessions of
3-6 Sep 2026 (39 transcripts, parents + subagents, 1 701 steps, $43.98):

  1. BUILD EXIT CODES ARE LOST. `tools.bash.denylist` matches command prefixes
     against parts a tree-sitter parse has already split on pipes (bash.py
     `_extract_commands`), so no pattern can express "mvn must not be piped" —
     the split hides the pipe. 262 Maven invocations were logged exit_code: 0
     while 38 outputs contained BUILD FAILURE. A pre_tool hook sees the raw
     command, before any split, and can rule on it.

  2. DISCOVERY LOOPS. 218 `find` calls, 124 distinct — the same
     `find … -name "EntityModel.java"` ran 75 times across three spelling
     variants that differ only by their stderr redirect.

  3. RE-READS. 476 read_file calls, 190 of them (40%) re-reading a file already
     read in that session, unchanged. read_file is 42% of all tool-result
     volume — the largest single source of context.

730 of 1 312 bash calls arrive through the `rtk` proxy, so every pattern is
matched against a NORMALISED command (leading `cd … &&`, a leading `rtk`, and
trailing stderr redirects removed).

Wire protocol: JSON on stdin, JSON on stdout, exit 0. See the builtin `vibe`
skill, "Hooks". Stdlib only, and fails open — a bug here must never block a
legitimate call.
"""

from __future__ import annotations

import json
import os
import re
import sys
import tempfile
from pathlib import Path

MAX_CAT_LINES = 200
# Identical discovery invocations tolerated before denying. The 3rd identical
# `find` in one session has never returned anything new.
DISCOVERY_BUDGET = 2

TCK_PATH = re.compile(r"(^|/)([\w.-]*-tck|tck)(/|$)|persistence-tck|/ee/jakarta/tck/")

WRITE_ISH = re.compile(
    r"(^|[\s;|&])(tee|dd)\b"
    r"|(^|[\s;|&])sed\s+(-[^\s]*\s+)*-i\b"
    r"|(^|[\s;|&])(cp|mv|install|rsync|touch|mkdir|rm|ln)\b"
    r"|>>?\s*\S"
)

BUILD_CMD = re.compile(r"(^|[\s;|&(])(\./)?(mvnw?|gradlew?)\b")
PIPE_AFTER_BUILD = re.compile(r"(^|[\s;&(])(\./)?(mvnw?|gradlew?)\b[^|]*\|")

# No trailing \b on the group: it would apply to the whole alternation, and
# `jar tf` ends the `jar\s+[tx]` branch on "t" right before the word char "f".
JAR_INSPECT = re.compile(r"(^|[\s;|&])(jar\s+[tx]|unzip\s+-[lp]|javap\b)")

DISCOVERY = re.compile(r"^(find|locate|rg|ag)\b|^grep\s+-\S*[rR]")

CAT_FILE = re.compile(r"(^|[\s;|&])cat\s+(?!-)(?P<path>[^\s|;&>]+)")

_LEADING_CD = re.compile(r"^\s*cd\s+[^\s;&|]+\s*&&\s*")
_LEADING_PROXY = re.compile(r"^\s*(rtk|command|env|time|nice)\s+")
_TRAILING_REDIR = re.compile(r"\s*2>\s*(/dev/null|&1)\s*$")


def normalise(command: str) -> str:
    """Collapse the spellings that made 75 identical searches look distinct."""
    c = " ".join(command.split())
    prev = None
    while prev != c:
        prev = c
        c = _LEADING_CD.sub("", c)
        c = _LEADING_PROXY.sub("", c)
        c = _TRAILING_REDIR.sub("", c)
    return c.strip()


def deny(reason: str) -> None:
    # Do NOT self-name: Vibe wraps this as
    # "Tool 'X' was denied by hook 'Y': {reason}".
    json.dump({"decision": "deny", "reason": reason}, sys.stdout)
    sys.exit(0)


def allow() -> None:
    sys.exit(0)


def _state_file(session_id: str) -> Path:
    safe = re.sub(r"[^A-Za-z0-9_-]", "", session_id or "nosession")[:64]
    return Path(tempfile.gettempdir()) / f"vibe-guard-{safe}.json"


def _load(session_id: str) -> dict:
    p = _state_file(session_id)
    try:
        return json.loads(p.read_text()) if p.exists() else {}
    except (OSError, ValueError):
        return {}


def _save(session_id: str, data: dict) -> None:
    try:
        _state_file(session_id).write_text(json.dumps(data))
    except OSError:
        pass


def line_count(path: Path) -> int | None:
    try:
        if not path.is_file() or path.stat().st_size > 8_000_000:
            return None
        with path.open("rb") as f:
            return sum(1 for _ in f)
    except OSError:
        return None


# --------------------------------------------------------------------------- bash


def guard_bash(command: str, cwd: Path, session_id: str) -> None:
    norm = normalise(command)

    # 1. A build command must never be piped, and never bypass the scripts.
    if PIPE_AFTER_BUILD.search(norm):
        deny(
            "A build command must not be piped: the shell reports the filter's "
            "exit code, not Maven's, so a BUILD FAILURE is indistinguishable "
            "from success. Run ./scripts/build.sh (or ./scripts/verify.sh) — "
            "they set -o pipefail, keep the full log in target/agent-build.log, "
            "print the last 60 lines and propagate Maven's exit code."
        )
    if BUILD_CMD.search(norm) and "scripts/build.sh" not in norm and (
        "scripts/verify.sh" not in norm
    ):
        deny(
            "Call ./scripts/build.sh or ./scripts/verify.sh rather than "
            "mvn/mvnw directly. They are the only invocations whose exit code "
            "is trustworthy and whose full output is kept on disk."
        )

    # 2. TCK directories are read-only, including through a shell redirect —
    #    which the write_file/edit path denylist cannot see.
    if TCK_PATH.search(norm) and WRITE_ISH.search(norm):
        deny(
            "Conformance-test directories are read-only. A failing TCK test is "
            "reported upward (FQCN, expected vs actual, suspected production "
            "line), never edited."
        )

    # 3. Never unpack an archive into the context.
    if JAR_INSPECT.search(norm):
        deny(
            "Do not inspect archive contents from a session — each call dumps "
            "hundreds of entries into a context that is re-sent on every later "
            "turn. What the TCK contains belongs in docs/spec-notes/, written "
            "once."
        )

    # 4. Discovery loops.
    if DISCOVERY.search(norm):
        state = _load(session_id)
        seen = state.setdefault("cmd", {})
        seen[norm] = int(seen.get(norm, 0)) + 1
        n = seen[norm]
        _save(session_id, state)
        if n > DISCOVERY_BUDGET:
            deny(
                f"This search has already run {n - 1} times in this session "
                "(spelling variants collapsed) and returned the same thing. "
                "Reuse the earlier result. If you are a subagent and a path is "
                "missing, stop and report it — the parent is responsible for "
                "passing absolute paths in the task prompt."
            )

    # 5. Whole-file dumps.
    for m in CAT_FILE.finditer(norm):
        raw = m.group("path")
        if raw.startswith(("-", "$")):
            continue
        n = line_count((cwd / raw).expanduser())
        if n is not None and n > MAX_CAT_LINES:
            deny(
                f"{raw} is {n} lines; the budget is {MAX_CAT_LINES}. Read the "
                "span you need with sed -n 'a,bp', or locate it with grep -n. A "
                "whole-file dump stays in the context for the rest of the "
                "session and is re-sent on every turn."
            )

    allow()


# ---------------------------------------------------------------------- read_file


def guard_read_file(tool_input: dict, cwd: Path, session_id: str) -> None:
    raw = tool_input.get("file_path")
    if not raw or not isinstance(raw, str):
        allow()

    path = (cwd / raw).expanduser()
    try:
        st = path.stat()
        fingerprint = f"{st.st_mtime_ns}:{st.st_size}"
    except OSError:
        allow()
        return

    # Same file, same content, same window == byte-identical result.
    window = f"{tool_input.get('offset')}:{tool_input.get('limit')}"
    key = f"{path.resolve()}|{fingerprint}|{window}"

    state = _load(session_id)
    reads = state.setdefault("read", {})
    n = int(reads.get(key, 0))
    reads[key] = n + 1
    _save(session_id, state)

    if n >= 1:
        deny(
            f"You already read this exact span of {raw} earlier in this "
            "session and the file has not changed since (same mtime and size). "
            "The content is still in your context — scroll back rather than "
            "re-reading. If you need a different part of the file, pass an "
            "explicit offset/limit; if you believe it changed, it did not."
        )
    allow()


def main() -> None:
    try:
        payload = json.load(sys.stdin)
    except (ValueError, OSError):
        allow()

    tool = payload.get("tool_name")
    tool_input = payload.get("tool_input") or {}
    cwd = Path(payload.get("cwd") or os.getcwd())
    session_id = payload.get("session_id") or ""

    if tool == "bash":
        command = tool_input.get("command") or ""
        if command.strip():
            guard_bash(command, cwd, session_id)
    elif tool == "read_file":
        guard_read_file(tool_input, cwd, session_id)

    allow()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # fail open, never block on a guard bug
        print(f"guard internal error: {exc}", file=sys.stderr)
        sys.exit(0)
