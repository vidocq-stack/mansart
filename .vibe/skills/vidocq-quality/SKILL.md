---
name: vidocq-quality
description: The SonarQube-on-Docker quality gate mechanism shared across the whole Vidocq workspace (container, port, the -Pquality/verify gotcha, reading the gate via API), and what counts as a security review. Portable across any Vidocq/mansart sub-project. Load before running or interpreting a quality gate, or before reviewing a diff that touches a dependency or handles untrusted input.
user-invocable: false
---

# Quality gate: SonarQube on Docker, and security review

Workspace-wide mechanism, not specific to any one sub-project — the same
container serves every mansart (and wider Vidocq) sub-project, one
`projectKey` each. The project-specific `projectKey` for the current
target is stated in that project's own architecture skill (e.g.
`mansart-jpa`); everything else below is identical everywhere.

## The instance

Container **`vidocq-sonar`** (`sonarqube:community` 26.5), host port
**9001**. **Persistent volumes** — `docker stop`/`start`/`restart`
preserve account, token and project history; only `docker rm` **plus**
deleting the volumes wipes it. SonarQube 26.5 does not allow anonymous
analysis — auth is via `SONAR_TOKEN` (env var, export it before launching
`vibe`; mint one at `http://localhost:9001` → My Account → Security, or
`POST /api/user_tokens/generate` with an admin Basic-auth header).

```bash
docker start vidocq-sonar   # then wait for GET /api/system/status = "UP"
```

## Running a scan — the gotcha that costs an afternoon if missed

```bash
./mvnw -ntp -Pquality -pl <sub-project>/<module> -am verify \
  org.sonarsource.scanner.maven:sonar-maven-plugin:sonar
```

- **`-Pquality`**, or JaCoCo never runs at all — it lives behind that
  profile in `vidocq-parent`.
- **`verify`, not `test`** — JaCoCo's XML report is bound to the `verify`
  phase. Without both, Sonar reports 0% coverage and the failure looks
  like a bug in the code instead of the invocation.

Exclusions that matter everywhere: `**/generated/**,**/generated-sources/**,
**/target/**` — generated code (APT output, Class-File API output)
analysed as if hand-written drowns the report in issues nobody wrote, and
worse, invites "fixing" the generator's output instead of the generator.

## Reading the result — API, never the console log

A scanner log is thousands of lines; never read it. Query the API, which
answers in a few hundred bytes:

```
/api/qualitygates/project_status?projectKey=<key>
/api/issues/search?componentKeys=<key>&inNewCodePeriod=true&statuses=OPEN
/api/measures/component?component=<key>&metricKeys=new_coverage,new_violations
```

**Only issues on new code block a card.** Pre-existing debt is a separate
backlog — a card is accountable for the code it wrote, not for the
backlog. At milestone close, scan the whole sub-reactor and record the
gate status in `STATUS.md` next to the TCK number.

## Security review — part of the same gate, not a separate step

Sonar's issue taxonomy has two categories that matter here beyond the
usual Bug/Code Smell: **Vulnerability** and **Security Hotspot**. Treat
both **on new code** as blockers for a card, same as any other new-code
issue — query them explicitly, they don't always surface in the plain
`issues/search` call above without asking for the type:

```
/api/issues/search?componentKeys=<key>&inNewCodePeriod=true&types=VULNERABILITY&statuses=OPEN
/api/hotspots/search?projectKey=<key>&inNewCodePeriod=true&status=TO_REVIEW
```

**Known-vulnerable dependencies are a separate, currently-unclosed gap**:
no automated SCA (software composition analysis) tool — e.g. OWASP
Dependency-Check — is wired into the Maven build as of this writing, and
Sonar Community Edition's own dependency-vulnerability coverage is
limited. Until one is added, `guardian` treats any newly introduced
dependency as needing a manual check (ask `spec-reader` to look up known
CVEs for the exact artifact + version via `web_fetch`) rather than
assuming a clean Sonar scan means the dependency is safe. This is exactly
the kind of gap that's easy to forget precisely because the gate looks
green — say so explicitly in a card's notes if a new dependency was added
without that check, rather than silently skipping it.
