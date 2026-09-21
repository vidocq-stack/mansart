---
description: Review and correct one generated specification card
agent: lead
---
Review and correct exactly the card `$ARGUMENTS` (format `SPEC/Mn-Tnnn`).

Your first action MUST be one task call to `reviewer`, with the exact card
path, its cited source sections, and the local Jakarta API jar. The reviewer
must execute `javap` for every Jakarta API type or method named by this card,
and return findings with severity, command evidence, and exact corrections.

After the reviewer returns, YOU (the lead) must apply every evidence-backed
BLOCKER and HIGH finding to that card only. Do not edit another card, source
material, production code, generated TASKS files, or Git state. If a claimed
API method is absent from `javap`, correct the card; do not rationalize it.

Finish with the card path, findings, corrections made, unresolved risks, and a
clear PASS/FAIL verdict. A PASS requires that no blocker remains and that the
reviewer's executable evidence is present in the report.
