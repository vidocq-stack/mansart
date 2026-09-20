#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

jq -e '.model == "omlx/Qwen3.8-27B-oQ4e-mtp"' opencode.json >/dev/null
jq -e '.provider.omlx.models["Qwen3.8-27B-oQ4e-mtp"].limit.context == 65536' opencode.json >/dev/null
jq -e '.agent.lead.steps == 40 and .agent.lead.permission["ctx_*"] == "deny"' opencode.json >/dev/null

for agent in architect implementer reviewer; do
    test -s ".opencode/agents/$agent.md"
    opencode debug agent "$agent" >/dev/null
done

opencode debug agent lead >/dev/null
printf 'OpenCode harness: PASS\n'
