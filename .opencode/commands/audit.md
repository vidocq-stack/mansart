---
description: Audit mansart-jakarta-persistence for charter-violating shortcuts and refresh PERSISTENCE-DEBT.md
agent: persistence-dev
---
Run a remediation audit:

1. Ask `@persistence-auditor` for its punch list (scope: $ARGUMENTS, default = whole
   `mansart-jakarta-persistence/*/src/main`).
2. Ask `@tracker` to merge the punch list into `PERSISTENCE-DEBT.md`: add new items as
   `- [ ] DEBT-NN — <finding> (<file:line>) → <remediation>` under the matching section,
   keep existing ids stable, never delete closed items.
3. Summarise for me: new items, items that look already fixed (propose closing with the
   commit), and the recommended next `/tck-fix` target.
