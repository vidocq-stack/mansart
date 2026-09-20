---
description: Produce a staged implementation plan for a local specification
agent: lead
---
Plan specification `$ARGUMENTS` using its local material in docs/spec-src and
docs/spec-notes. Ask @architect to return a bounded plan identifying
architecture boundaries, dependency order, executable acceptance evidence, and
the smallest first vertical slice. The architect returns text only; never ask
it to create or resume files.

After the architect returns, YOU (the lead) must materialize the plan as
individual Markdown work orders under `tasks/$ARGUMENTS/`. Create the directory
and cards yourself, one card per coherent step, with scope, cited requirements,
acceptance commands, risks and non-goals. Do not implement production code and
do not overwrite generated TASKS files. Before finishing, list every created
card and verify that no file outside `tasks/$ARGUMENTS/` was edited.
