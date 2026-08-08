# Alkahest External Runner Repository Design

## Goal

Move Alkahest's local server-run workflow out of the Paper source repository into a sibling source repository at `/home/jlo/dev/run-alkahest`.

The new repository owns the launcher task, reproducible run configuration, and runtime-directory conventions. The Paper checkout remains responsible only for building the server artifact and for its existing Paperweight development tasks.

## Scope

The canonical developer command becomes:

```bash
cd /home/jlo/dev/run-alkahest
./gradlew runAlkahest
```

The runner will use the sibling Paper checkout at `../paper` by default. It will invoke that checkout's `createPaperclipJar` task, resolve the actual produced Paperclip output, and launch it in the runner repository's `run/` directory. An explicit jar path remains available for running a previously built or downloaded jar.

This migration does not add a published Gradle plugin or a GitHub Releases downloader. The current Alkahest repository has no published latest release, while the sibling checkout is the reproducible local source already used by development. A future plugin or release resolver can consume the runner's launcher contract without moving the run configuration back into Paper.

## Architecture

`run-alkahest` is a small standalone Gradle project:

- `settings.gradle.kts` names the runner project and enables the Gradle plugin management needed by the build.
- `build.gradle.kts` defines the `runAlkahest` task and its configuration properties.
- `gradle.properties` records conservative local defaults without embedding machine-specific absolute paths.
- `run/` contains source-controlled server configuration only; generated runtime state is ignored.
- `README.md` documents the sibling checkout layout, explicit jar override, arguments, and runtime ownership.
- `.gitignore` excludes worlds, logs, libraries, caches, downloaded/build jars, plugin runtime data, and other server-generated files.

The Paper root build removes the `AlkahestRunExtension`, root `runAlkahest` task, and their imports. The Paper README keeps the packaged-jar build instructions and points local developers to `../run-alkahest` for server execution. Existing `paper-server:run*` tasks remain unchanged.

## Run data flow

1. Gradle validates that `paperDir` exists unless an explicit `jar` is configured.
2. Without `jar`, the runner executes the sibling checkout's Gradle wrapper with `createPaperclipJar` and propagates a non-zero exit status.
3. The runner resolves the produced artifact from the `createPaperclipJar` output rather than relying on a guessed filename. `-Pjar=/path/to/server.jar` bypasses the build step.
4. The task creates the configured working directory if necessary. Source-controlled files already present under `run/` are used directly; it never deletes or overwrites existing world or runtime state.
5. The task starts Java 25 with the configured heap, JVM arguments, `-jar`, the Paperclip jar, `--nogui` by default, and configured server arguments. Standard input remains attached to the launched server.
6. The launched process exits with its own status, so startup/build failures are visible to the caller.

## Configuration contract

The runner exposes Gradle properties with sibling-safe defaults:

- `paperDir`: Paper checkout directory, default `../paper`.
- `jar`: explicit Paperclip jar path; when set, no sibling build is invoked.
- `runDir`: runtime directory, default `run`.
- `memoryGb`: equal initial and maximum heap, default `2`.
- `jvmArgs`: optional space-delimited JVM arguments.
- `serverArgs`: optional space-delimited server arguments; `--nogui` is enabled by default and can be disabled with `nogui=false`.
- `nogui`: default `true`.

Command-line properties override `gradle.properties`. Paths may be absolute or relative to the runner repository. The task rejects a non-positive heap size, a missing Paper checkout, and a missing explicit jar with actionable messages.

## Source-controlled run configuration

Keep only reproducible configuration from the existing local run directory under the new repository's `run/` directory. The initial set is:

- `run/server.properties`
- `run/bukkit.yml`
- `run/spigot.yml`
- `run/commands.yml`
- `run/eula.txt`
- `run/config/paper-global.yml`
- `run/config/paper-world-defaults.yml`
- `run/config/mintychochip/ecology.json`
- the existing ecology launcher as a runner-owned script if it remains useful after the Gradle task is installed

The task uses those files in place and creates the directory when it is absent. Do not copy `world/`, `logs/`, `libraries/`, `cache/`, `versions/`, `plugins/`, player/operator/ban state, generated resource-pack output, server jars, heap dumps, or agent logs. Those are local runtime artifacts, not source configuration.


## Error handling and compatibility

The launcher uses the Paperclip jar's executable manifest, so all Paper-compatible command-line arguments and plugin behavior remain the server's responsibility. The runner adds no plugin API or server behavior.

Failures are explicit:

- missing `paperDir` reports the expected path and suggests `-Pjar`;
- missing or ambiguous build output reports the `createPaperclipJar` task and suggests an explicit `-Pjar`;
- invalid heap size fails during configuration;
- nested build and server exit codes propagate unchanged;
- existing runtime state is never removed or silently replaced.

## Verification

Verification must cover both artifact modes:

1. `./gradlew tasks --all` from `run-alkahest` exposes `runAlkahest`.
2. A dry-run or task inspection proves the sibling `createPaperclipJar` invocation is wired when no explicit jar is configured.
3. A real launch using `-Pjar` starts the existing Paperclip jar in an isolated temporary run directory, reaches the Alkahest/Paper startup banner, and shuts down normally.
4. A real sibling-build invocation resolves the jar created by `../paper` and reaches the same startup path.
5. Paper's Gradle configuration no longer exposes the removed root `runAlkahest` task, while its existing build/run tasks still configure.
6. The source repository's ignore rules keep generated runtime artifacts out of version control.
