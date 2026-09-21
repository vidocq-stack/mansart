---
description: Decompose a PDF specification into bounded milestones, tasks and tracking
agent: lead
---
Plan specification `$ARGUMENTS` from the split source files in
`docs/spec-src/$ARGUMENTS/chapters/`. Do not use a single concatenated Markdown
spec as context and do not implement production code.

The output is a planning workspace, not an implementation. First ask the
architect to build a coverage matrix from the chapter files, identify every
normative area, dependencies, risks, and missing work. Then materialize these
artifacts:

1. `docs/spec-notes/$ARGUMENTS/coverage-matrix.md`: one row per normative area,
   with source chapter/section, API surface, planned milestone, task IDs, and
   coverage status.
2. `docs/spec-notes/$ARGUMENTS/milestones.md`: ordered milestones with goals,
   prerequisites, exit criteria, and explicit non-goals. A milestone must be a
   coherent capability, not a single class.
3. `tasks/$ARGUMENTS/README.md`: index and operating rules.
4. One directory per milestone under `tasks/$ARGUMENTS/Mn/`, containing
   atomic cards named `$ARGUMENTS-Mn-Tnn.md` (for example `JKP-M0-T01.md`).
   Each card must include YAML fields `id`, `milestone`, `status: TODO`,
   `depends_on`, `source`, and `weight`, followed by scope, cited requirements,
   design decisions, acceptance commands, risks, non-goals, and definition of
   done. Cards must be small enough to implement and review in one session.
5. `tasks/$ARGUMENTS/STATUS.md`, generated with
   `scripts/opencode-harness/update-status.sh $ARGUMENTS`; it must show global
   and per-milestone percentages and the full `ID/status` ledger.

Do not create flat `M*-T*.md` cards directly under `tasks/$ARGUMENTS/`. Do not
claim complete specification coverage unless every row in the coverage matrix
maps to a task or an explicitly documented deferral. Before finishing, verify
that only the planning artifacts above were created or changed.
