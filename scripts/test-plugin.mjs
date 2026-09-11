// Drives the REAL plugin hook, not just the pure rules: the rules were 44/44
// while a scoping bug in the plugin's catch replaced every refusal with
// "st is not defined" for a whole session. Test the wiring, not only the parts.
import { MansartGuard } from "../.opencode/plugin/mansart-guard.js"
const hooks = await MansartGuard({ directory: process.cwd() })
const before = hooks["tool.execute.before"]
let pass = 0, fail = 0
const ck = (name, ok, got) => { ok ? pass++ : fail++; console.log(`${ok ? "PASS" : "FAIL"} ${name}${ok ? "" : "  got: " + got}`) }
async function deny(tool, args, sid = "t1") {
  try { await before({ tool, sessionID: sid, callID: "c" }, { args }); return null }
  catch (e) { return String(e?.message ?? e) }
}
const cmd = { command: "unzip -l /tmp/x.jar" }
const m1 = await deny("bash", cmd), m2 = await deny("bash", cmd), m3 = await deny("bash", cmd)
ck("refusal carries the guard's message", m1?.startsWith("mansart-guard:") && !/is not defined/.test(m1), m1)
ck("second refusal still explains", m2?.startsWith("mansart-guard:") && !/STOP/.test(m2), m2)
ck("third identical refusal says STOP", /STOP/.test(m3 ?? ""), m3)
const w = await deny("write", { filePath: "STATUS-JKP.md" }, "t2")
ck("write to STATUS refused with a reason", w?.startsWith("mansart-guard:") && /card-done/.test(w), w)
const ok = await deny("bash", { command: "ls" }, "t3")
ck("harmless command passes", ok === null, ok)
console.log(`RESULT pass=${pass} fail=${fail}`)
process.exit(fail ? 1 : 0)
