# Alkahest Date-Versioned Release Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rename the fork repository to Alkahest and publish `v2026.08.08.1` as a date-versioned GitHub release containing a verified Paperclip jar.

**Architecture:** Gradle keeps its current Minecraft/build version fallback and accepts an explicit `alkahestVersion` property for release builds. A tag-triggered GitHub Actions workflow validates `vYYYY.MM.DD.N`, derives the numeric manifest build number, compiles and packages the tagged source with JDK 25, then uses the GitHub CLI to create the release and attach the jar. Repository administration and pushes happen only after local artifact verification, with `origin` changed to the renamed fork and `upstream` left untouched.

**Tech Stack:** Gradle 9.4.1 Kotlin DSL, Paperweight, GitHub Actions, GitHub CLI (`gh`), JDK 25, Paperclip jar.

## Global Constraints

- Paper remains upstream at `https://github.com/PaperMC/Paper.git`; never push to `upstream`.
- The fork remote is renamed to `https://github.com/mintychochip/alkahest.git` after the GitHub repository rename and is the only push destination.
- Preserve `io.papermc.paper`, `paper-api`, `org.bukkit`, `mcVersion=26.2`, and API compatibility names.
- Use the exact first release tag `v2026.08.08.1` and release version `2026.08.08.1`.
- Preserve existing Alkahest runtime branding (`Brand-Id`, `Brand-Name`, root project, archive, console, and local version reporting).
- Work only in `/home/jlo/dev/paper/.worktrees/alkahest-release-2026-08-08`; do not stage the dirty main checkout's files.
- The existing `:paper-api:test` annotation audit fails with 469 missing annotations; treat that as a recorded baseline failure, while requiring API/server jar compilation and manifest verification for this release.
- Leave `.github/workflows/build.yml` and `.github/workflows/publish_pr.yml` unchanged; their inherited Paper-specific coupling is outside the release path.

---

## Task 1: Add date-version override and renamed fork links

**Files:**
- Modify: `settings.gradle.kts:22-25,58-67`
- Modify: `.github/ISSUE_TEMPLATE/bug-or-incompatibility.yml` self-link
- Modify: `.github/ISSUE_TEMPLATE/new-feature.yml` self-link
- Modify: `.github/ISSUE_TEMPLATE/performance-problem.yml` self-link
- Test: Gradle version-printing task and clean worktree status

**Interfaces:**
- Consumes: existing `mcVersion`, `channel`, and numeric `BUILD_NUMBER` version fallback.
- Produces: optional `-PalkahestVersion=2026.08.08.1` override used as every project’s `project.version`; fork issue links point to `mintychochip/alkahest`.

- [ ] **Step 1: Replace the settings version selection with an override-first expression**

Keep the existing fallback unchanged and insert the override before it:

```kotlin
gradle.lifecycle.beforeProject {
    val mcVersion = providers.gradleProperty("mcVersion").get().trim()
    val alkahestVersionChannel = providers.gradleProperty("channel").get().trim()
    val alkahestBuildNumber = providers.environmentVariable("BUILD_NUMBER").orNull?.trim()?.toInt()
    val alkahestVersionOverride = providers.gradleProperty("alkahestVersion").orNull?.trim()
    val versionString = alkahestVersionOverride?.takeIf { it.isNotEmpty() }
        ?: if (alkahestBuildNumber == null) {
            "$mcVersion.local-SNAPSHOT"
        } else {
            "$mcVersion.build.$alkahestBuildNumber-${alkahestVersionChannel.lowercase()}"
        }
    version = versionString
}
```

- [ ] **Step 2: Point only fork-owned issue links at the renamed repository**

Replace `https://github.com/mintychochip/paper/issues` with `https://github.com/mintychochip/alkahest/issues` in the three listed issue templates. Do not change Paper upstream URLs, API documentation URLs, or compatibility text.

- [ ] **Step 3: Verify the override and fallback**

Run from the isolated worktree:

```bash
./gradlew printAlkahestVersion -PalkahestVersion=2026.08.08.1 --no-daemon
./gradlew printAlkahestVersion --no-daemon
```

Expected output includes exactly `2026.08.08.1` for the override and `26.2.local-SNAPSHOT` for the no-property fallback.

- [ ] **Step 4: Review and commit the atomic version/link change**

```bash
git status --short
git diff -- settings.gradle.kts .github/ISSUE_TEMPLATE/bug-or-incompatibility.yml .github/ISSUE_TEMPLATE/new-feature.yml .github/ISSUE_TEMPLATE/performance-problem.yml
git diff --check
git add settings.gradle.kts .github/ISSUE_TEMPLATE/bug-or-incompatibility.yml .github/ISSUE_TEMPLATE/new-feature.yml .github/ISSUE_TEMPLATE/performance-problem.yml
git diff --cached --check
git commit -m "build: support Alkahest date-versioned releases"
```

The staged paths must contain only the version override and fork self-link updates.

---

## Task 2: Add the tagged Alkahest release workflow

**Files:**
- Create: `.github/workflows/release-alkahest.yml`
- Test: YAML shape inspection and local equivalent shell version calculation

**Interfaces:**
- Consumes: pushed tags matching `vYYYY.MM.DD.N`, the `alkahestVersion` Gradle property, and the existing `createPaperclipJar` task.
- Produces: a GitHub release titled `Alkahest 2026.08.08.1` with the tagged `alkahest-paperclip-*.jar` asset.

- [ ] **Step 1: Create the complete release workflow**

Write `.github/workflows/release-alkahest.yml` with this exact behavior:

```yaml
name: Release Alkahest

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    env:
      GH_TOKEN: ${{ github.token }}

    steps:
      - name: Check out tagged source
        uses: actions/checkout@v7
        with:
          fetch-depth: 0

      - name: Configure date version
        shell: bash
        run: |
          set -euo pipefail
          tag="${GITHUB_REF_NAME}"
          if [[ ! "$tag" =~ ^v([0-9]{4})\.([0-9]{2})\.([0-9]{2})\.([0-9]{1,2})$ ]]; then
            echo "::error::Release tag must match vYYYY.MM.DD.N: $tag"
            exit 1
          fi

          year="${BASH_REMATCH[1]}"
          month="${BASH_REMATCH[2]}"
          day="${BASH_REMATCH[3]}"
          sequence_text="${BASH_REMATCH[4]}"
          sequence=$((10#$sequence_text))
          if (( sequence > 99 )); then
            echo "::error::Release sequence must fit the two-digit numeric build suffix: $sequence_text"
            exit 1
          fi
          if [[ "$(date -u -d "$year-$month-$day" +%Y.%m.%d 2>/dev/null)" != "$year.$month.$day" ]]; then
            echo "::error::Release tag contains an invalid calendar date: $tag"
            exit 1
          fi

          printf -v build_number '%s%s%s%02d' "$year" "$month" "$day" "$sequence"
          if (( build_number > 2147483647 )); then
            echo "::error::Numeric build number exceeds the manifest integer range: $build_number"
            exit 1
          fi

          {
            echo "RELEASE_VERSION=${tag#v}"
            echo "BUILD_NUMBER=$build_number"
            echo "BUILD_STARTED_AT=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
          } >> "$GITHUB_ENV"

      - name: Set up JDK 25
        uses: actions/setup-java@v5
        with:
          java-version: 25
          distribution: zulu

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

      - name: Apply patches
        run: |
          git config --global user.email "no-reply@github.com"
          git config --global user.name "GitHub Actions"
          ./gradlew applyPatches --stacktrace --no-daemon

      - name: Compile and package Paperclip
        run: |
          ./gradlew :paper-api:jar :paper-server:jar --stacktrace --no-daemon -PalkahestVersion="$RELEASE_VERSION"
          ./gradlew createPaperclipJar --stacktrace --no-daemon -PalkahestVersion="$RELEASE_VERSION"

      - name: Verify release jar
        shell: bash
        run: |
          set -euo pipefail
          jar_path="$(find paper-server/build/libs -maxdepth 1 -type f -name 'alkahest-paperclip-*.jar' -print -quit)"
          if [[ -z "$jar_path" ]]; then
            echo "::error::No Alkahest Paperclip jar was produced"
            exit 1
          fi
          manifest="$(unzip -p "$jar_path" META-INF/MANIFEST.MF)"
          [[ "$manifest" == *$'Brand-Id: mintychochip:alkahest'* ]]
          [[ "$manifest" == *$'Brand-Name: Alkahest'* ]]
          [[ "$manifest" == *"Specification-Version: $RELEASE_VERSION"* ]]
          echo "PAPERCLIP_JAR=$jar_path" >> "$GITHUB_ENV"

      - name: Publish GitHub release
        run: |
          gh release create "$GITHUB_REF_NAME" "$PAPERCLIP_JAR" \
            --repo "$GITHUB_REPOSITORY" \
            --title "Alkahest $RELEASE_VERSION" \
            --generate-notes
```

- [ ] **Step 2: Validate the workflow file and its coupled values**

Run:

```bash
python3 - <<'PY'
from pathlib import Path
text = Path('.github/workflows/release-alkahest.yml').read_text()
required = (
    "name: Release Alkahest",
    "contents: write",
    "actions/checkout@v7",
    "java-version: 25",
    "-PalkahestVersion=",
    "gh release create",
    "alkahest-paperclip-*.jar",
)
missing = [value for value in required if value not in text]
if missing:
    raise SystemExit(f"missing release workflow values: {missing}")
print("release workflow contract present")
PY
```

Run the valid tag calculation locally with the same shell inputs used by the workflow:

```bash
bash -euo pipefail -c '
tag=v2026.08.08.1
if [[ "$tag" =~ ^v([0-9]{4})\.([0-9]{2})\.([0-9]{2})\.([0-9]+)$ ]]; then
  printf "version=%s\n" "${tag#v}"
  printf "build_number=%s%s%s%02d\n" \
    "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}" "${BASH_REMATCH[3]}" \
    "$((10#${BASH_REMATCH[4]}))"
else
  exit 1
fi
'
```

Expected output is `version=2026.08.08.1` and `build_number=2026080801`.

- [ ] **Step 3: Review and commit the atomic workflow change**

```bash
git status --short
git diff --stat
git diff --check
git add .github/workflows/release-alkahest.yml
git diff --cached --check
git commit -m "ci: publish tagged Alkahest releases"
```

The staged set must contain only the new release workflow.

---

## Task 3: Verify, rename, push, and publish the first release

**Files:**
- Modify repository administration state: GitHub repository name and local `origin` URL
- Push: `main` ref and `v2026.08.08.1` tag to `origin`
- Verify: generated Paperclip jar, manifest, GitHub Actions run, and GitHub release asset

**Interfaces:**
- Consumes: the two clean implementation commits and current release branch HEAD.
- Produces: `mintychochip/alkahest`, `origin` at its canonical URL, tag `v2026.08.08.1`, and the published release asset.

- [ ] **Step 1: Run the local release build before remote mutation**

Run:

```bash
BUILD_NUMBER=2026080801 \
BUILD_STARTED_AT=2026-08-08T00:00:00Z \
./gradlew :paper-api:jar :paper-server:jar --no-daemon --stacktrace -PalkahestVersion=2026.08.08.1
BUILD_NUMBER=2026080801 \
BUILD_STARTED_AT=2026-08-08T00:00:00Z \
./gradlew createPaperclipJar --no-daemon --stacktrace -PalkahestVersion=2026.08.08.1
```

Resolve the single `paper-server/build/libs/alkahest-paperclip-*.jar`, then inspect its manifest:

```bash
unzip -p paper-server/build/libs/alkahest-paperclip-*.jar META-INF/MANIFEST.MF
```

The manifest must contain `Brand-Id: mintychochip:alkahest`, `Brand-Name: Alkahest`, `Specification-Version: 2026.08.08.1`, and `Build-Number: 2026080801`. The pre-existing `./gradlew :paper-api:test --no-daemon` annotation-audit failure remains recorded and is not relabeled as a release failure.

- [ ] **Step 2: Confirm the implementation worktree is clean and remotes are known**

```bash
git status --short --branch
git log --oneline --decorate -4
git remote -v
```

Before any push, confirm `upstream` is exactly the Paper URL and identify `origin` as the only fork destination. Do not use `git push upstream`, a branch tracking `upstream`, or any force-push.

- [ ] **Step 3: Rename the GitHub repository and update origin**

Run:

```bash
gh repo rename alkahest --repo mintychochip/Paper --yes
git remote set-url origin https://github.com/mintychochip/alkahest.git
gh repo view mintychochip/alkahest --json name,nameWithOwner,isFork,defaultBranchRef,url
git remote -v
```

Expected repository metadata: `name=alkahest`, `nameWithOwner=mintychochip/alkahest`, `isFork=true`, default branch `main`; expected remotes: `origin` at the Alkahest URL and `upstream` at Paper.

- [ ] **Step 4: Push only origin main and the release tag**

Before creating or pushing the tag, reject any existing local or remote tag, then push the same commit to `main` and the tag:

```bash
if git rev-parse -q --verify refs/tags/v2026.08.08.1 >/dev/null; then
  echo "v2026.08.08.1 already exists locally" >&2
  exit 1
fi
if git ls-remote --exit-code origin refs/tags/v2026.08.08.1 >/dev/null 2>&1; then
  echo "v2026.08.08.1 already exists on origin" >&2
  exit 1
fi
git tag v2026.08.08.1
git push origin HEAD:main
git push origin v2026.08.08.1
git ls-remote origin refs/heads/main refs/tags/v2026.08.08.1
```

The remote branch and tag must resolve to the same release commit. Never force-update an existing tag.

- [ ] **Step 5: Verify the tagged workflow and GitHub release**

```bash
gh run list --repo mintychochip/alkahest --workflow release-alkahest.yml --limit 1 --json databaseId,status,conclusion,headBranch,event,url
```

Find the run created by this tag and wait for its final result:

```bash
workflow_id="$(gh run list --repo mintychochip/alkahest --workflow release-alkahest.yml --event push --limit 20 --json databaseId,headBranch --jq 'map(select(.headBranch == "v2026.08.08.1")) | .[0].databaseId')"
test -n "$workflow_id"
gh run watch "$workflow_id" --repo mintychochip/alkahest --exit-status
```

Then verify the final release:

```bash
gh release view v2026.08.08.1 --repo mintychochip/alkahest --json name,tagName,isDraft,isPrerelease,assets,url
```

Require a non-draft, non-prerelease release named `Alkahest 2026.08.08.1`, tag `v2026.08.08.1`, and exactly one `alkahest-paperclip-*.jar` asset with a nonzero size.

- [ ] **Step 6: Record final repository state**

```bash
git status --short --branch
git remote -v
git log --oneline --decorate -5
```

The release worktree remains available for audit; the original dirty main checkout remains unmodified by this plan.
