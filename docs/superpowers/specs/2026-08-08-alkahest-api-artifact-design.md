# Alkahest API Artifact Design

## Goal

Publish the plugin-facing API as an Alkahest-named artifact:

```text
alkahest-api/build/libs/alkahest-api-YYYY.MM.DD.N.jar
```

For a release tagged `v2026.08.08.3`, the API artifact is:

```text
alkahest-api/build/libs/alkahest-api-2026.08.08.3.jar
```

The API contract and Java package names remain the compatibility boundary. The
artifact name and Maven coordinate are intentionally Alkahest-specific.

## Coordinates and packages

The Gradle project and Maven artifact ID become `alkahest-api`. The existing
Maven group remains `io.papermc.paper`, and the public Java packages remain
`org.bukkit`, `io.papermc.paper`, and the existing `dev.mintychochip` API
packages.

A plugin can depend on:

```text
io.papermc.paper:alkahest-api:<version>
```

Changing API contracts is an API compatibility decision; changing the artifact
coordinate is an intentional distribution identity decision and is not treated
as a Java package rename.

## Module layout

Move the module directory from `paper-api/` to `alkahest-api/` and include it as
`:alkahest-api` in `settings.gradle.kts`. Update all internal project
references, including the server, optional generator, and optional test-plugin
projects. Source packages and generated API sources do not move between Java
packages.

Configure the API publication and jar archive base name as `alkahest-api` so
both the local output path and Maven publication use the same identity.

Existing compatibility capabilities used for dependency conflict resolution may
remain; they do not publish a second `paper-api` artifact.

## Versioning

The root project version remains the single source of truth. `settings.gradle.kts`
must first honor a non-empty `-PalkahestVersion` value, then fall back to the
existing build-number/local-snapshot calculation. Release automation passes the
tag version through that property, using the existing `YYYY.MM.DD.N` tag format.
Local builds retain the repository's local snapshot version unless an explicit
Alkahest version is supplied.

No date is duplicated in the API module build script. The jar name is derived
from `project.version`.

## Release output

The GitHub release workflow must:

1. Build `:alkahest-api:jar` with the release version.
2. Verify `alkahest-api/build/libs/alkahest-api-${RELEASE_VERSION}.jar` exists.
3. Upload the API jar alongside the Alkahest server jar in the GitHub release.

The source repository README must point plugin developers to the Alkahest API
module/artifact and document the release/download path plus local Maven
publication workflow.

## Verification

The implementation is complete when:

- `./gradlew :alkahest-api:jar` succeeds.
- The output is under `alkahest-api/build/libs/` and starts with
  `alkahest-api-`.
- `:paper-server:compileJava` succeeds against `:alkahest-api`.
- Optional generator and test-plugin projects, when enabled, resolve the renamed
  project dependency.
- A release-version build produces exactly
  `alkahest-api-${version}.jar`.
- The publication metadata uses artifact ID `alkahest-api` while API Java
  packages remain unchanged.
