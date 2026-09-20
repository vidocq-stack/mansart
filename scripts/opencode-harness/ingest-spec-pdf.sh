#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="${1:-}"

if [[ ! "$SPEC" =~ ^[A-Z]{3}$ ]]; then
    echo "usage: scripts/opencode-harness/ingest-spec-pdf.sh <SPEC>" >&2
    exit 2
fi

PDF="$ROOT/docs/specifications/$SPEC"
INPUT="$(find "$PDF" -maxdepth 1 -type f -iname '*.pdf' -print -quit 2>/dev/null || true)"
if [[ -z "$INPUT" ]]; then
    echo "no PDF found in docs/specifications/$SPEC" >&2
    exit 3
fi
command -v pdftotext >/dev/null || {
    echo "pdftotext is required (install with: brew install poppler)" >&2
    exit 4
}

OUTDIR="$ROOT/docs/spec-src/$SPEC"
OUT="$OUTDIR/$(basename "${INPUT%.*}").md"
TMP="$(mktemp "${TMPDIR:-/tmp}/spec-ingest.XXXXXX")"
trap 'rm -f "$TMP"' EXIT
mkdir -p "$OUTDIR"

{
    printf '# Extracted specification: %s\n\n' "$(basename "$INPUT")"
    printf '> Generated from `%s` with Poppler `pdftotext -layout`.\n' "${INPUT#$ROOT/}"
    printf '> This file is source material for OpenCode; it is not a task plan.\n\n'
    pdftotext -layout "$INPUT" -
} > "$TMP"
mv "$TMP" "$OUT"
printf 'generated: %s\n' "${OUT#$ROOT/}"
