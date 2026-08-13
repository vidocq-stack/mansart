---
name: log-bench
description: Append a new benchmark run entry to the BENCH.md of the mansart sub-project. Use whenever JMH/wrk/comparative-perf numbers are produced — even informally during a debugging session. Creates BENCH.md if missing. Triggers on "log this benchmark", "add to BENCH.md", "record these numbers", or after running a JMH/perf command.
user_invocable: true
---

# log-bench

Append a benchmark run to `BENCH.md` following the Vidocq convention defined in the
workspace root `CLAUDE.md`. **No perf number is allowed in a README or commit message
without a corresponding BENCH.md entry.**

## When to invoke

- A JMH suite was just run.
- Any load tool (`wrk`, `wrk2`, `bombardier`, `oha`, `hey`) was just run.
- A comparative measurement was done vs Hibernate, EclipseLink, HikariCP, etc.
- The user says: "log this bench", "record these numbers", "add to BENCH.md".

## Procedure

1. **Locate or create** `BENCH.md` at the mansart root. If the benchmark belongs to
   another Vidocq sub-project, use that sub-project's `BENCH.md` instead.

2. **Capture the environment** — ask the user only for what you cannot determine yourself:
   - JVM: `java -version` output (vendor + version + flags if non-default).
   - Hardware: CPU model + core count + RAM (`system_profiler SPHardwareDataType` on macOS).
   - OS: `uname -a`.
   - Git commit hash of the code under test (`git rev-parse --short HEAD`).

3. **Append a section** at the bottom of `BENCH.md` with this structure:

   ```markdown
   ## BENCH-YYYYMMDD-NN — <short title: what was measured>

   - **Date** : YYYY-MM-DD
   - **Commit** : <short hash> (<branch>)
   - **JVM** : <vendor version, e.g. Temurin 25.0.0+36>
   - **Hardware** : <CPU> / <cores> cores / <RAM> GB RAM
   - **OS** : <uname output, condensed>
   - **Commande exacte** :
     ```bash
     <one-liner that reproduces>
     ```
   - **Résultats** :
     ```
     <raw output — keep it short, paste the summary table only>
     ```
   - **Comparaison vs run précédent** : <delta % vs BENCH-id, or "premier run">
   - **Notes** : <anything non-obvious — warmup behaviour, GC pauses, allocations>
   ```

4. **Create the file with a header** if it does not exist:

   ```markdown
   # BENCH.md — mansart

   Historique des mesures de performance. Convention : voir `../CLAUDE.md` (workspace root).

   Tout chiffre publié (README, commit, post) doit pointer vers une entrée ici.

   ---
   ```

5. **Compute the delta** vs the most recent matching benchmark (same title prefix or same
   command). State it as a percentage. If first run, write "premier run".

6. **Do not commit** — only edit the file. Report the file path and the new BENCH-id to the user.
