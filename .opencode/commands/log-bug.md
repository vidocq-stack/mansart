---
description: Append a structured bug entry to BUG.md (Vidocq convention)
agent: build
---
Append a bug entry to `BUG.md` at the mansart root, following the existing convention
in that file (entries named `MANSART-NNN`, take the next free number). Bug context:
$ARGUMENTS

Structure the entry exactly like the existing ones: `## MANSART-NNN — <one-line
symptom>`, then `- **Date**` (today, absolute), `- **Status**: OPEN`, `- **Severity**`,
followed by `### Symptom`, `### Minimal repro`, `### Root cause` (or hypothesis), and
`### Prevention` sections. Never delete or rewrite existing entries — they are the
audit trail. Do not commit; report the file path and the new bug ID.
