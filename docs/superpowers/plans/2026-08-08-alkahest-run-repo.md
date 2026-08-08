# External Alkahest Runner Repository Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the local Alkahest server launcher and reproducible run configuration into an independent `dev/run-alkahest` Gradle repository.

**Architecture:** The new runner project owns a `runAlkahest` `Exec` task. By default it invokes `../paper/gradlew createPaperclipJar`, resolves the actual Paperclip jar from the build output, and runs that jar in the runner's ignored `run/` directory. An explicit `-Pjar` path bypasses the sibling build. The Paper root build loses its custom launcher and points developers at the runner repository.

**Tech Stack:** Gradle 9.4.1 Kotlin DSL, Gradle `Exec`, Gradle Java toolchains, Paperclip jar, Java 25, Git.

## Global Constraints

- Keep all new runner source under `/home/jlo/dev/run-alkahest`; do not add runner implementation code to Paper.
- Initialize `/home/jlo/dev/run-alkahest` as its own Git repository; it must not be a Paper worktree or share Paper's `.git` directory.
- Default the runner's Paper checkout to `../paper`; do not require a network release or published plugin.
- Resolve the sibling build's actual jar output; do not assume a guessed filename when more than one candidate exists.
- Preserve Java 25, 2 GiB initial/maximum heap, `--nogui`, stdin forwarding, optional test-plugin loading, and configurable JVM/server/plugin arguments.
- Never delete or overwrite existing runtime state in `run/`.
- Do not stage or commit the existing unrelated `bench/`, `serve/`, heap dump, or `.gitignore` changes in the Paper checkout.
- Run focused runner/Paper smoke checks; do not run unrelated project-wide suites.

---

### Task 1: Scaffold the independent runner repository

**Files:**
- Create: `/home/jlo/dev/run-alkahest/.gitignore`
- Create: `/home/jlo/dev/run-alkahest/settings.gradle.kts`
- Create: `/home/jlo/dev/run-alkahest/build.gradle.kts`
- Create: `/home/jlo/dev/run-alkahest/gradle.properties`
- Create: `/home/jlo/dev/run-alkahest/gradlew`, `/home/jlo/dev/run-alkahest/gradlew.bat`, `/home/jlo/dev/run-alkahest/gradle/wrapper/*`
- Create: `/home/jlo/dev/run-alkahest/run/.gitkeep`

**Interfaces:**
- Produces an independent Gradle project named `run-alkahest` with a future `runAlkahest` task registration point.
- Uses Gradle 9.4.1, matching the Paper checkout's wrapper.

- [ ] **Step 1: Create and initialize the sibling Git repository**

```bash
mkdir -p /home/jlo/dev/run-alkahest
cd /home/jlo/dev/run-alkahest
git init -b main
```

Expected: a new Git repository exists at `/home/jlo/dev/run-alkahest`; no Paper files are moved or deleted.

- [ ] **Step 2: Copy only the Gradle wrapper machinery**

Copy the existing wrapper scripts, wrapper jar, and `gradle-wrapper.properties` from `/home/jlo/dev/paper` into the new repository. Keep the wrapper distribution URL at `gradle-9.4.1-bin.zip`.

Expected paths:

```text
run-alkahest/gradlew
run-alkahest/gradlew.bat
run-alkahest/gradle/wrapper/gradle-wrapper.jar
run-alkahest/gradle/wrapper/gradle-wrapper.properties
```

- [ ] **Step 3: Add runner project settings and defaults**

Create `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "run-alkahest"
```

Create the initial `build.gradle.kts` so the scaffold has a valid build before the launcher is added:

```kotlin
plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
```

The launcher task is added in Task 2; this initial file must not define a server task.

Create an executable `start-ecology.sh` wrapper:

```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
exec ./gradlew runAlkahest "$@"
```

This replaces the old script's hardcoded Paper runtime classpath and delegates all launch behavior to the runner task.

Mark the script executable with `chmod +x start-ecology.sh`.

Update the scaffold commit command to include `build.gradle.kts` and `start-ecology.sh`.

Create `gradle.properties`:

```properties
paperDir=../paper
runDir=run
memoryGb=2
nogui=true
jvmArgs=
serverArgs=
pluginJars=
autoInstallTestPlugin=true
```

Create `.gitignore`:

```gitignore
.gradle/

# Generated server runtime
run/*
!run/.gitkeep
!run/server.properties
!run/bukkit.yml
!run/spigot.yml
!run/commands.yml
!run/eula.txt
!run/config/
!run/config/paper-global.yml
!run/config/paper-world-defaults.yml
!run/config/mintychochip/
!run/config/mintychochip/ecology.json
run/world*/
run/logs/
run/libraries/
run/cache/
run/versions/
run/plugins/
run/resourcepacks/
run/mintychochip/
run/*.jar
run/*.log
run/agent-server.log

# Local build/download artifacts
*.hprof
*.jar
build/
```

Expected: tracked source configuration can live under `run/`, while generated runtime content remains ignored.

- [ ] **Step 4: Verify the scaffold configures**

Run:

```bash
./gradlew help --no-daemon
```

Expected: Gradle 9.4.1 starts and reports the project name `run-alkahest`; no server task exists yet.

- [ ] **Step 5: Commit the independent scaffold**

```bash
git add .gitignore settings.gradle.kts build.gradle.kts gradle.properties start-ecology.sh gradlew gradlew.bat gradle/wrapper
git commit -m "build: scaffold Alkahest runner repository"
```

---

### Task 2: Implement the sibling-build and explicit-jar launcher

**Files:**
- Modify: `/home/jlo/dev/run-alkahest/build.gradle.kts`
- Test: Gradle task inspection and bounded server smoke launches from the new repository

**Interfaces:**
- Consumes `paperDir`, `jar`, `runDir`, `memoryGb`, `nogui`, `jvmArgs`, `serverArgs`, `pluginJars`, and `autoInstallTestPlugin` Gradle properties.
- Produces `buildAlkahest` and `runAlkahest` tasks.

- [ ] **Step 1: Add the Java 25 toolchain and typed property providers**

Apply the `java` plugin only to obtain Gradle's toolchain service, set language version 25, and map the documented Gradle properties to providers. Keep paths resolved relative to the runner project through `project.file(...)`.

The `build.gradle.kts` imports must include:

```kotlin
import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
```

Use `java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }`. Expose providers for all properties and parse `memoryGb` as an integer with a configuration-time error when it is not positive.

- [ ] **Step 2: Implement deterministic argument and candidate-jar helpers**

Add private Kotlin DSL functions:

```kotlin
fun splitArguments(value: String): List<String>
fun paperclipCandidates(paperDir: File): List<File>
fun resolvePaperclipJar(paperDir: File): File
fun resolveSinglePluginJar(directory: File, prefix: String): File
```

`splitArguments` returns whitespace-separated tokens and strips matching single or double quotes. Empty values return an empty list.

`paperclipCandidates` scans `paper-server/build/libs` for regular `.jar` files whose names contain `paperclip` case-insensitively, sorted by last-modified time descending. `resolvePaperclipJar` returns the only candidate; if there are zero or more than one, throw `GradleException` naming the directory, candidates, and `-Pjar` override.

`resolveSinglePluginJar` applies the same zero/ambiguous error behavior to `test-plugin/build/libs` and the configured plugin directory.

- [ ] **Step 3: Register `buildAlkahest` for sibling builds**

Register an `Exec` task named `buildAlkahest` that is skipped when `-Pjar` is set. In `doFirst`:

1. Validate `paperDir` exists and contains the platform wrapper (`gradlew` on Unix, `gradlew.bat` on Windows).
2. Set the task working directory to `paperDir`.
3. Invoke the wrapper with `createPaperclipJar`.
4. Add `:test-plugin:jar` when `autoInstallTestPlugin=true` and `test-plugin/build.gradle.kts` exists.
5. Leave nested Gradle's exit code as the task result.

Do not hardcode a jar filename. The follow-on `runAlkahest` task will scan the output directory after this task completes.

- [ ] **Step 4: Register `runAlkahest` and construct the executable command**

Register an `Exec` task that depends on `buildAlkahest`, attaches `System.`in`` as standard input, and in `doFirst`:

1. Resolve `-Pjar` when present, otherwise call `resolvePaperclipJar(paperDir)`.
2. Validate every `pluginJars` entry exists.
3. Auto-detect the single `test-plugin` jar when enabled and the nested project exists.
4. Create `runDir` with `mkdirs()` without deleting its contents.
5. Obtain the Java 25 launcher from `JavaToolchainService`.
6. Build the command as:

```text
<java-25> -Xms<memoryGb>G -Xmx<memoryGb>G <jvmArgs...> -jar <paperclip.jar> [--nogui] <serverArgs...> [-add-plugin=<plugin>...]
```

7. Set the working directory to `runDir` and leave `ignoreExitValue=false`.

The task must support `-Pnogui=false`, explicit `-Pjar=/path/to/jar`, and comma-separated `-PpluginJars=/path/a.jar,/path/b.jar`.

- [ ] **Step 5: Verify task wiring before launching**

Run from `/home/jlo/dev/run-alkahest`:

```bash
./gradlew tasks --all --no-daemon
./gradlew runAlkahest --dry-run --no-daemon
./gradlew runAlkahest -Pjar=/does/not/exist --no-daemon
```

Expected:

- `runAlkahest` and `buildAlkahest` appear under the run/build groups.
- The dry-run includes `buildAlkahest`.
- The invalid explicit jar fails with the actionable missing-file error before Java starts.

- [ ] **Step 6: Commit the launcher implementation**

```bash
git add build.gradle.kts
git commit -m "feat: add external Alkahest launcher task"
```

---

### Task 3: Migrate reproducible run configuration and documentation

**Files:**
- Create in `/home/jlo/dev/run-alkahest`: `README.md`, `run/server.properties`, `run/bukkit.yml`, `run/spigot.yml`, `run/commands.yml`, `run/eula.txt`, `run/config/paper-global.yml`, `run/config/paper-world-defaults.yml`, `run/config/mintychochip/ecology.json`
- Create in `/home/jlo/dev/run-alkahest`: `start-ecology.sh`, delegating to `./gradlew runAlkahest`
- Modify in `/home/jlo/dev/paper`: `README.md`
- Remove from `/home/jlo/dev/paper`: the dedicated root launcher implementation and its README configuration block in the same feature change

**Interfaces:**
- The runner README is the user-facing source of truth for local server execution.
- The Paper README points to `../run-alkahest` and no longer documents `./gradlew runAlkahest`.

- [ ] **Step 1: Copy only reproducible files from the existing run directory**

Copy the explicitly listed configuration files from `/home/jlo/dev/paper/run` into the matching paths under `/home/jlo/dev/run-alkahest/run`. Do not copy worlds, logs, libraries, caches, jars, plugins, player state, generated resource-pack files, or heap dumps.

- [ ] **Step 2: Add runner documentation**

Document:

```bash
cd ../run-alkahest
./gradlew runAlkahest
```

Include the sibling layout, `-PpaperDir`, `-Pjar`, `-PmemoryGb`, `-PjvmArgs`, `-PserverArgs`, `-Pnogui`, `-PpluginJars`, and `-PautoInstallTestPlugin` examples. State that `run/` is persistent and generated contents are ignored. Explain that the default path builds the current sibling checkout, while `-Pjar` runs an existing/released jar.

- [ ] **Step 3: Remove Paper-owned launcher code and update its README**

Delete only the imports, `AlkahestRunExtension`, provider defaults, `projectsEvaluated` launcher registration, and launcher-specific README section added by the prior Alkahest run-task change. Keep Paper's version tasks and existing `paper-server:run*` tasks.

Add a concise Paper README pointer:

```markdown
For local server runs, use the external runner repository at `../run-alkahest`:

```bash
cd ../run-alkahest
./gradlew runAlkahest
```
```

- [ ] **Step 4: Commit the migration as one logical feature unit**

In the new repository, stage its docs/config and commit:

```bash
git add README.md run .gitignore
git commit -m "docs: add Alkahest runner configuration"
```

In Paper, stage only the launcher removal, README pointer, and any required tracked documentation. Do not stage the existing unrelated dirty files. Commit with:

```bash
git add build.gradle.kts README.md
git commit -m "refactor: move Alkahest runner out of Paper"
```

---

### Task 4: Verify both artifact modes end to end

**Files:**
- Verify: `/home/jlo/dev/run-alkahest/build.gradle.kts`, runner task output, and Paper root configuration

- [ ] **Step 1: Verify explicit-jar mode in an isolated runtime directory**

Use the existing built Paperclip jar or build one once from Paper, then run:

```bash
./gradlew runAlkahest \
  -Pjar=/home/jlo/dev/paper/run/paper-server-26.2.local-SNAPSHOT.jar \
  -PrunDir=/tmp/alkahest-run-explicit \
  -PserverArgs="--port 25566" \
  --no-daemon
```

Start it through the harness process manager, wait for the normal Alkahest/Paper ready log line, send the normal shutdown command, and confirm exit 0. Verify `/tmp/alkahest-run-explicit` contains runtime state and was not populated with Paper source files.

- [ ] **Step 2: Verify sibling-build mode**

Run:

```bash
./gradlew runAlkahest -PrunDir=/tmp/alkahest-run-sibling --no-daemon
```

Confirm the nested Paper `createPaperclipJar` task runs, exactly one Paperclip candidate is resolved, the server reaches the same ready log line, and graceful shutdown returns 0.

- [ ] **Step 3: Verify Paper configuration after removal**

Run from `/home/jlo/dev/paper`:

```bash
./gradlew help --no-daemon
./gradlew tasks --all --no-daemon
```

Expected: configuration succeeds; `runAlkahest` is absent from the Paper root task list; existing `:paper-server:runServer`, `runDevServer`, `runBundler`, and `runPaperclip` remain present.

- [ ] **Step 4: Verify ignore boundaries and preserve unrelated work**

Run:

```bash
cd /home/jlo/dev/run-alkahest
git status --short --ignored
cd /home/jlo/dev/paper
git status --short --untracked-files=all
```

Expected: generated runner runtime artifacts are ignored; the Paper checkout still shows the pre-existing `.gitignore`, `bench/`, `serve/`, and heap-dump changes untouched.

- [ ] **Step 5: Commit only after all smoke checks pass**

Re-run the atomic-commit inventory in both repositories, confirm staged paths match their commit messages, and leave unrelated Paper files unstaged.
