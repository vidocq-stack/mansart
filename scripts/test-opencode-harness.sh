#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

jq -e '.model == "omlx/Qwen3.8-27B-oQ4e-mtp"' opencode.json >/dev/null
jq -e '.provider.omlx.models["Qwen3.8-27B-oQ4e-mtp"].limit.context == 65536' opencode.json >/dev/null
jq -e '.provider.omlx.models["Qwen3.8-27B-oQ4e-mtp"].limit.output == 4096' opencode.json >/dev/null
jq -e '.provider.omlx.models["Qwen3-Coder-Next-MLX-4bit"].limit.output == 4096' opencode.json >/dev/null
jq -e '.provider.omlx.models["Qwen3-VL-8B-Instruct-MLX-5bit"].limit.output == 2048' opencode.json >/dev/null
jq -e '.agent.lead.steps == 24 and .agent.lead.permission["ctx_*"] == "deny"' opencode.json >/dev/null
jq -e '.agent.lead.permission.external_directory["/Users/yblazart/.m2/repository/**"] == "allow"' opencode.json >/dev/null
test -x scripts/opencode-card.sh

for agent in architect implementer reviewer; do
    test -s ".opencode/agents/$agent.md"
    opencode debug agent "$agent" >/dev/null
done

opencode debug agent lead >/dev/null
printf 'OpenCode harness: PASS\n'
