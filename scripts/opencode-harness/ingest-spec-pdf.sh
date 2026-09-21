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
CHAPTERDIR="$OUTDIR/chapters"
INDEX="$OUTDIR/INDEX.md"
TMP="$(mktemp "${TMPDIR:-/tmp}/spec-ingest.XXXXXX")"
trap 'rm -f "$TMP"' EXIT
mkdir -p "$OUTDIR"
rm -rf "$CHAPTERDIR"
mkdir -p "$CHAPTERDIR"

{
    pdftotext -layout "$INPUT" -
} > "$TMP"

awk -v out="$CHAPTERDIR" '
function close_current() {
    if (file != "") close(file)
}
function start_file(prefix, heading, slug) {
    close_current()
    slug = heading
    sub(/^[[:space:]]*/, "", slug)
    gsub(/[^A-Za-z0-9]+/, "-", slug)
    gsub(/^-+/, "", slug)
    gsub(/-+$/, "", slug)
    file = out "/" prefix "-" slug ".md"
    print "# " heading > file
    print "" > file
    print "> Extracted from the official PDF with Poppler pdftotext -layout." > file
    print "> This file is normative source material, not an implementation plan." > file
    print "" > file
}
BEGIN { start_file("CH-00", "Front matter") }
{
    line = $0
    if (line ~ /^[[:space:]]*Chapter [0-9]+\./) {
        heading = line
        sub(/^[[:space:]]*/, "", heading)
        match(heading, /Chapter [0-9]+/)
        prefix = substr(heading, RSTART, RLENGTH)
        gsub(/[[:space:]]+/, "-", prefix)
        start_file(prefix, heading)
    } else if (line ~ /^[[:space:]]*Appendix [A-Z]\./) {
        heading = line
        sub(/^[[:space:]]*/, "", heading)
        match(heading, /Appendix [A-Z]/)
        prefix = substr(heading, RSTART, RLENGTH)
        gsub(/[[:space:]]+/, "-", prefix)
        start_file(prefix, heading)
    } else {
        print line > file
    }
}
END { close_current() }
' "$TMP"

{
    printf '# Extracted specification: %s\n\n' "$(basename "$INPUT")"
    printf '> Generated from `%s` with Poppler `pdftotext -layout`.\n' "${INPUT#$ROOT/}"
    printf '> Source is split by top-level chapters and appendices for bounded model context.\n\n'
    printf '## Chapters and appendices\n\n'
    find "$CHAPTERDIR" -maxdepth 1 -type f -name '*.md' -print | sort | while read -r file; do
        printf '%s\n' "- [$(basename "$file")]($(basename "$file"))"
    done
} > "$INDEX"
printf 'generated: %s\n' "${INDEX#$ROOT/}"
printf 'generated chapters: %s\n' "$(find "$CHAPTERDIR" -maxdepth 1 -type f -name '*.md' | wc -l | tr -d ' ')"
