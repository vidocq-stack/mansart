---
description: Show where the project actually stands — current card, last sessions, real TCK numbers, open blockers.
agent: tracker
subtask: true
---
Report the current state of mansart-jakarta-persistence, reading
`STATUS.md` and `TASKS.md` only:

```
focus:    <card id> — <goal>
tck:      <pass>/<total>   (measured <date>)
cards:    <n> DONE / <n> WIP / <n> TODO
blocked:  <card ids and one-line reasons, or "none">
next 3:   <the next three eligible cards, one line each>
```

If a number is recorded as `not measured`, print `not measured` — never
substitute an older value.
