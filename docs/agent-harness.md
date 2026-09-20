# Local agent harness

The harness uses one local `Qwen3.8-27B-oQ4e-mtp` model through oMLX, but gives
each responsibility a separate OpenCode session. This bounds context and makes
architecture, implementation, and review independently inspectable.

## Roles

- `lead` orchestrates one card and is the interactive primary agent.
- `architect` is read-only and produces a bounded implementation contract.
- `implementer` edits and tests one approved card.
- `reviewer` is read-only and challenges the resulting diff and evidence.

No agent may commit or rewrite Git state. After review and verification, the
human or outer orchestrator creates one atomic commit. This makes rollback a
normal `git revert <commit>` operation rather than an attempt to reconstruct a
large mixed change.

## Usage

Start OpenCode from the repository root and select `lead`, or run a command:

```text
/spec-plan JKP
/card JKP/M1-T001
/review-card JKP/M1-T001
```

For unattended execution:

```shell
opencode run --agent lead --command card JKP/M1-T001
```

The project config intentionally declares no credential or endpoint. The local
`omlx` provider must be configured globally and point at the oMLX OpenAI-compatible
endpoint. The resolved config can be checked with `opencode debug config`.

## Context policy

OpenCode sees a 65,536-token model window even though oMLX accepts 131,072. The
lower operational ceiling encourages compaction before the cold-prefill cost
becomes excessive. Agents read only the card, its cited spec chapters, and the
affected module. A new card starts a new implementation session.
