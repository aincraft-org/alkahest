# Alkahest Date-Versioned Release Design

## Goal

Publish the first Alkahest release from the current committed `main` line, rename the GitHub repository from `mintychochip/Paper` to `mintychochip/alkahest`, and make future releases reproducible from date-versioned tags.

The first release is:

- Git tag: `v2026.08.08.1`
- Release version: `2026.08.08.1`
- GitHub release title: `Alkahest 2026.08.08.1`

## Constraints

- Paper remains the upstream project. The `upstream` remote stays `https://github.com/PaperMC/Paper.git` and receives no pushes.
- Pushes go only to the fork remote, renamed locally to `https://github.com/mintychochip/alkahest.git` after the GitHub repository rename.
- The public API remains `io.papermc.paper`, `paper-api`, and `org.bukkit`. No plugin-facing Paper coordinates or compatibility names are renamed.
- The distribution identity is Alkahest: root project name, Paperclip archive, manifest brand, console, and local version reporting remain Alkahest-branded.
- The current main checkout contains unrelated uncommitted work and runtime artifacts. Release work is isolated in a clean worktree from committed `main`; those files are not included.
- The baseline `:paper-api:test` task currently fails its existing annotation audit with 469 missing annotations. The baseline API/server jar compilation succeeds. The release workflow therefore verifies compilation and packages the jar without claiming that the unrelated annotation audit passes.

## Release versioning

`settings.gradle.kts` will accept an optional `-PalkahestVersion=YYYY.MM.DD.N` property. Without it, existing local and CI version calculation remains unchanged. With it, Gradle `project.version` and the existing jar `Specification-Version` use the release version while `mcVersion` and API compatibility remain `26.2`.

The release workflow validates tags against `vYYYY.MM.DD.N`, strips the leading `v`, and passes the result as `alkahestVersion`. It also derives a numeric `BUILD_NUMBER` as `YYYYMMDDN`; for the first release this is `2026080801`. This satisfies the existing manifest parser and keeps `/version` from reporting `DEV`. The release version remains available as the exact date-version in Gradle metadata, manifest `Specification-Version`, tag, and GitHub release title.

`BUILD_STARTED_AT` is set to the UTC workflow start time so the existing manifest build-time field is meaningful for published jars.

## Repository identity

The authenticated GitHub CLI renames `mintychochip/Paper` to `mintychochip/alkahest`. The repository must remain a fork with `main` as its default branch. After the rename, the local `origin` URL is changed to the canonical Alkahest URL and verified independently from `upstream`.

Self-referential fork links that still use the old repository slug may be updated to `/alkahest`. Links intentionally targeting Paper upstream, Paper API documentation, or upstream compatibility history remain unchanged.

## Release workflow

Add `.github/workflows/release-alkahest.yml` with these responsibilities:

1. Trigger on pushed `v*` tags.
2. Validate the date-version tag and export the release version, numeric build number, and UTC build timestamp.
3. Check out the tagged commit with full history.
4. Set up JDK 25 and Gradle caching.
5. Apply Paper source/resource/feature patches.
6. Compile the API and server jars and create the Alkahest Paperclip jar using the release version override.
7. Create a GitHub release titled `Alkahest 2026.08.08.1` for the first tag (and the corresponding date-version for later tags), with `contents: write`, and attach the generated `alkahest-paperclip-*.jar`.

The inherited `Build Paper` and `publish_pr.yml` workflows remain unchanged. Their names and PaperMC PR-publication action are coupled; they are not part of the release path, and changing them would add unrelated workflow migration risk.

## Delivery sequence

1. Create the design commit in the isolated release worktree.
2. Self-review the specification for placeholders, contradictions, and scope drift.
3. Obtain user review of the written specification.
4. Create an implementation plan and execute it in the same isolated worktree.
5. Verify the version override, compilation, Paperclip packaging, and jar manifest before any remote mutation.
6. Rename the GitHub repository, update and verify `origin`, and re-confirm that `upstream` is the Paper URL.
7. Push the release branch result to `origin/main` and push `v2026.08.08.1` to `origin`; never push to `upstream`.
8. Wait for the tag workflow, then verify its conclusion, the GitHub release, its exact tag, and the attached jar asset.

## Acceptance criteria

- GitHub repository name is `alkahest`, remains a fork, and uses `main` as default.
- Local `origin` points to `mintychochip/alkahest`; `upstream` points to `PaperMC/Paper`.
- The pushed `main` contains the Alkahest release workflow and version override without including the dirty main checkout's unrelated files.
- Tag `v2026.08.08.1` exists on `origin` and points at the pushed release commit.
- GitHub Actions completes the tagged release workflow successfully.
- GitHub Release `Alkahest 2026.08.08.1` exists with one Alkahest Paperclip jar asset.
- The packaged server manifest inside the Paperclip build reports `Brand-Id: mintychochip:alkahest`, `Brand-Name: Alkahest`, and `Specification-Version: 2026.08.08.1`; the outer Paperclip launcher is the published asset.
- No command pushes to or creates a branch on `upstream`.
