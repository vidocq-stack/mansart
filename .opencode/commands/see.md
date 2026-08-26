---
description: Read an image with the vision model — a screenshot of a stack trace, a SonarQube gate, an IntelliJ inspection — and get its exact text back.
agent: vision
subtask: true
---
$ARGUMENTS

Transcribe what the image actually shows. Exact text, exact numbers, preserved
structure. Mark anything cut off as `truncated:` and anything illegible as
`unreadable:` rather than guessing.

If no image reached you, say so in one line and stop — do not answer from the
text of this prompt.
