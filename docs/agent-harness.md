# Local agent harness

The harness uses local models through oMLX and gives each responsibility a
separate OpenCode session. This bounds context and makes
architecture, implementation, and review independently inspectable.

## Roles

- `lead` orchestrates one card and is the interactive primary agent.
- `architect` is read-only and produces a bounded implementation contract.
- `implementer` uses Qwen3-Coder-Next 4-bit to edit and test one approved card.
- `reviewer` uses Qwen3-VL 8B and challenges the resulting diff and evidence.

No agent may commit or rewrite Git state. After review and verification, the
human or outer orchestrator creates one atomic commit. This makes rollback a
normal `git revert <commit>` operation rather than an attempt to reconstruct a
large mixed change.

## Usage

Start OpenCode from the repository root and select `lead`, or run a command:

```text
/spec-plan JKP
/card JKP/M1-T001
/design-card JKP/M2-T001
/review-card JKP/M1-T001
```

For unattended execution:

```shell
./scripts/opencode-harness/opencode-card.sh JKP/M1-T001
./scripts/opencode-harness/opencode-plan.sh JKP
./scripts/opencode-harness/ingest-spec-pdf.sh JKP
```

The project config intentionally declares no credential or endpoint. The local
`omlx` provider must be configured globally and point at the oMLX OpenAI-compatible
endpoint. The resolved config can be checked with `opencode debug config`.

The wrappers pass `--print-logs --log-level INFO`, so delegation, tool calls and
agent progress remain visible during a run. They do not request private thinking
blocks; the observable transcript is the useful audit trail.

## Context policy

OpenCode sees a 65,536-token model window even though oMLX accepts 131,072. The
lower operational ceiling encourages compaction before the cold-prefill cost
becomes excessive. Agents read only the card, its cited spec chapters, and the
affected module. A new card starts a new implementation session.

`/card` uses the 27B implementer followed by the local 8B reviewer. Use
`/design-card` when a card creates or changes a module boundary, public API/SPI,
cross-module contract, or requires non-obvious interpretation of the spec.

The 27B model is capped at 4,096 output tokens and the reviewer at 2,048. The unattended wrapper also
enforces a 30-minute wall-clock timeout so a looping session cannot monopolize
the local inference server indefinitely.
