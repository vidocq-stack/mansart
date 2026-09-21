---
description: Audit and correct a generated specification plan
agent: lead
---
Review the generated plan for specification `$ARGUMENTS`.

Your first action MUST be one task call to `reviewer`, asking it to inspect
every `tasks/$ARGUMENTS/M*-T*.md` card against the extracted normative source
under `docs/spec-src/$ARGUMENTS`, the local Jakarta API jars, and repository
conventions. The reviewer is read-only and must report exact path/line
findings, severity, evidence, and proposed corrections. It must explicitly
check API names/signatures, dependency order, scope boundaries, acceptance
commands, missing major specification areas, and performance criteria.

After the reviewer returns, YOU (the lead) may apply only evidence-backed
corrections to files under `tasks/$ARGUMENTS/`. Do not modify production code,
the source PDF, extracted specification material, generated TASKS files, or
Git state. Do not invent requirements: cite the API jar, source section, or
repository path for every correction.

You MUST apply every BLOCKER finding before finishing. If the reviewer reports
an API signature mismatch, edit the affected card and add the `javap` command
and result to its cited requirements or acceptance notes. You may not report
"no correction" while a claimed method is absent from the inspected API class.

Finish with a report listing: corrected files, rejected findings with reasons,
remaining gaps, and a proposed next card. If no correction is justified, leave
the cards unchanged and say so.
