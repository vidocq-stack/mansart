---
description: Append a reproducible bug to mansart/BUG.md (short id, date, symptom, minimal repro, suspected cause, status).
agent: tracker
---
Append an entry to `BUG.md` at the mansart root for: $ARGUMENTS

Follow the format already used in that file. Required fields: short id, date
(absolute), symptom, minimal reproduction (a command or a test), hypothesis of
the cause, and status. Do not restructure existing entries; append only.

If the report lacks a minimal reproduction, say so and ask for one instead of
inventing it. A bug entry without a repro is worthless.
