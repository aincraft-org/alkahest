# Alkahest Internal Run Task Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a configurable root `runAlkahest` JavaExec task that launches this fork's locally built Paperclip jar like jpenilla's run-paper workflow.

**Architecture:** Keep the implementation in the root Gradle build because this is an internal repository workflow, not a published plugin. Define a typed extension and root JavaExec task in `build.gradle.kts`; consume the lazily produced `:paper-server:createPaperclipJar` output and wire evaluated extension values into cache-safe task inputs. Document and verify the task through Gradle task introspection, dry-run dependency checks, and a launch smoke test.

**Tech Stack:** Gradle Kotlin DSL, Gradle `JavaExec`, Gradle Provider API, Paperweight Paperclip task, Java 25.

## Global Constraints

- Do not modify `paper-server/src/minecraft` or patch files.
- Do not download upstream Paper or change the existing `paper-server:run*` tasks.
- Keep runtime state in `run/` or an explicitly configured directory; never delete user runtime state.
- Preserve the existing Java 25 toolchain and Alkahest branding.
- Tests must exercise observable Gradle task behavior, not source text.

---

### Task 1: Define root run extension and launcher

**Files:**
- Modify: `build.gradle.kts`

**Interfaces:**
- Produces `alkahestRun` extension with `workingDir`, `jvmArgs`, `serverArgs`, `pluginJars`, `memoryGb`, `nogui`, and `autoInstallTestPlugin`.
- Produces root `runAlkahest` task of type `JavaExec`.

- [x] Add the extension type and defaults using Gradle-managed properties.
- [x] Register the root JavaExec task against the locally built Paperclip output and Java 25 toolchain.
- [x] Wire evaluated JVM, server, and plugin arguments; validate positive heap size and create the configured work directory without deleting it.
- [x] Verify `help --task runAlkahest` and `runAlkahest --dry-run`; both pass and the dry-run includes `:paper-server:createPaperclipJar` with configuration cache enabled.

### Task 2: Document the developer workflow

**Files:**
- Modify: `README.md`

**Interfaces:**
- Documents `./gradlew runAlkahest` and the `alkahestRun` extension.

- [x] Add basic invocation, configuration example, plugin loading behavior, and distinction from lower-level server-module tasks.
- [x] Verify the documented property names against the Gradle task configuration.

### Task 3: Exercise a real launch smoke test

**Files:**
- No source changes expected.

**Interfaces:**
- Verifies the built Paperclip jar launches through `runAlkahest` with an isolated working directory.

- [x] Build and launch in a temporary directory with a bounded normal shutdown.
- [x] Confirm startup reaches the Alkahest/Paper banner and the configured directory is used.
- [x] Run focused season API tests (4 tests passed); the full API suite still has an unrelated 469-missing-annotations failure, and the filtered mintychochip server command has no matching tests.
