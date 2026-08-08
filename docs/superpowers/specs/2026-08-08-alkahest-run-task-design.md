# Alkahest Internal Run Task Design

## Goal

Provide a repo-local equivalent of jpenilla's `run-paper` developer workflow for the Alkahest Paperclip jar: one root Gradle task builds the current jar, launches it with Java 25, forwards interactive input, and accepts convenient local-run configuration without downloading or mutating upstream Paper artifacts.

## Scope

The first version is for this repository's developer and integration-testing workflow. It does not publish a Gradle plugin, download Minecraft/Paper versions, support Folia/Velocity/Waterfall, or replace the existing `paper-server:run*` tasks.

The user-facing command is:

```bash
./gradlew runAlkahest
```

The task defaults to the repository's existing `run/` directory, two GiB minimum and maximum heap, `--nogui`, and automatic loading of the optional `test-plugin` jar when that project is enabled.

## Architecture

The root `build.gradle.kts` owns a typed `alkahestRun` extension and a root `JavaExec` task named `runAlkahest`. After the `paper-server` project is evaluated, the task consumes the lazily produced `:paper-server:createPaperclipJar` task output as its classpath, making the Paperclip build an explicit task dependency while keeping the launcher independently configurable.

The extension exposes only local-run concerns:

- `workingDir`: `DirectoryProperty`, defaulting to `paper.runWorkDir` or `run`.
- `jvmArgs`: `ListProperty<String>`, defaulting to an empty list.
- `serverArgs`: `ListProperty<String>`, defaulting to an empty list.
- `pluginJars`: `ConfigurableFileCollection` for explicit plugin jars.
- `memoryGb`: integer `Property`, defaulting to `2`, applied to both `-Xms` and `-Xmx`.
- `nogui`: boolean `Property`, defaulting to `true`.
- `autoInstallTestPlugin`: boolean `Property`, defaulting to `true` when `:test-plugin` exists and `false` otherwise.

The root build resolves the extension values after project evaluation and wires immutable values into the `JavaExec` task. This preserves configuration-cache compatibility while allowing all root build-script configuration to be applied before project evaluation completes. Explicit plugin jars and the optional test plugin are passed with Paper's `-add-plugin=<absolute-path>` option. The task creates the selected working directory but never deletes it or accepts the EULA automatically.

The existing `:paper-server:runServer`, `runDevServer`, `runBundler`, and `runPaperclip` tasks remain unchanged.

## Error handling

Gradle's normal validation handles missing files and invalid property values. A non-positive `memoryGb` fails before launch with a clear Gradle exception. The task does not hide server exit codes: a failed server process fails the Gradle invocation. `--nogui` can be disabled through the extension, while callers can still provide additional server arguments.

## Verification

The implementation will be verified with Gradle configuration checks that prove:

1. `runAlkahest` is registered at the root and is a `JavaExec` task.
2. Its dry-run graph includes `:paper-server:createPaperclipJar`.
3. The task help exposes the documented task and the full Gradle build still configures.
4. A real launch smoke test starts the produced Paperclip jar in an isolated temporary working directory, then exits through the normal server command path.

README usage will document the task, extension example, plugin-jars configuration, and the distinction from the lower-level server-module run tasks.
