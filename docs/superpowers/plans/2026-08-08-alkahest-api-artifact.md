# Alkahest API Artifact Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Rename the plugin-facing API module and publication so release builds produce `alkahest-api/build/libs/alkahest-api-YYYY.MM.DD.N.jar`.

**Architecture:** Move the Gradle API project from `paper-api/` to `alkahest-api/`, keep the existing Java packages and Maven group, and change only the project/artifact identity to `alkahest-api`. Root version selection accepts an explicit `alkahestVersion` release value before the existing build-number/local-snapshot fallback; GitHub releases upload the API jar beside the server jar.

**Tech Stack:** Gradle Kotlin DSL, Java 25, Maven Publish, GitHub Actions, JUnit 5.

## Global Constraints

- API Java packages remain `org.bukkit`, `io.papermc.paper`, and existing `dev.mintychochip` packages.
- Maven group remains `io.papermc.paper`; artifact ID becomes `alkahest-api`.
- Release versions use `YYYY.MM.DD.N`, supplied through `-PalkahestVersion`.
- Local builds retain the existing `26.2.local-SNAPSHOT` fallback when no override or CI build number is present.
- No `src/minecraft` files or patches are changed.
- Preserve unrelated user changes in `.gitignore`, `README.md`, `build.gradle.kts`, and `test-plugin/build.gradle.kts`.
- Do not add generated jars, runtime state, heap dumps, `bench/`, or `serve/` artifacts to commits.

---

### Task 1: Rename the API Gradle module

**Files:**
- Move: `paper-api/` → `alkahest-api/` (all tracked API source, tests, generated source, and build configuration)
- Modify: `settings.gradle.kts:32-36`
- Modify: `paper-server/build.gradle.kts:112`
- Modify: `paper-generator/build.gradle.kts:22,39,62`
- Modify: `test-plugin/build.gradle.kts:6`
- Modify: `alkahest-api/src/test/java/dev/mintychochip/customentity/CustomEntitySourcesPlacementTest.java`

**Interfaces:**
- Consumes: existing API source set and Gradle publication.
- Produces: project path `:alkahest-api`, source directory `alkahest-api/`, and internal dependencies resolving through `project(":alkahest-api")`.

- [ ] **Step 1: Move the module directory without changing package paths**

```bash
git mv paper-api alkahest-api
```

The resulting Java source path remains, for example:

```text
alkahest-api/src/main/java/org/bukkit/Material.java
alkahest-api/src/main/java/dev/mintychochip/customblock/CustomBlocks.java
```

- [ ] **Step 2: Change the root project include**

Replace:

```kotlin
for (name in listOf("paper-api", "paper-server")) {
```

with:

```kotlin
for (name in listOf("alkahest-api", "paper-server")) {
```

- [ ] **Step 3: Change internal Gradle project references**

Replace every live project dependency and generator source directory with the renamed project/path:

```kotlin
implementation(project(":alkahest-api"))
compileOnly(project(":alkahest-api"))
sourceSet = rootProject.layout.projectDirectory.dir("alkahest-api")
```

Do not alter comments or historical design documents until the live build references compile.

- [ ] **Step 4: Update source-placement test paths**

Change the test's root path assertions and module-working-directory discovery from `paper-api` to `alkahest-api`, preserving the existing test behavior and `paper-server` checks.

- [ ] **Step 5: Run the renamed module compilation**

Run:

```bash
./gradlew :alkahest-api:compileJava :paper-server:compileJava
```

Expected: both tasks succeed and Gradle reports no unknown `:paper-api` project dependency.

- [ ] **Step 6: Commit the module rename**

```bash
git add settings.gradle.kts paper-server/build.gradle.kts paper-generator/build.gradle.kts test-plugin/build.gradle.kts alkahest-api
git commit -m "refactor: rename paper API module for Alkahest"
```

---

### Task 2: Configure release versioning and API publication identity

**Files:**
- Modify: `settings.gradle.kts:58-67`
- Modify: `alkahest-api/build.gradle.kts:111-142,172-179`

**Interfaces:**
- Consumes: Gradle property `alkahestVersion` and existing `project.version`.
- Produces: `alkahest-api-${project.version}.jar` and Maven publication artifact ID `alkahest-api`.

- [ ] **Step 1: Add the explicit release-version precedence**

Use this version selection shape in `settings.gradle.kts`:

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

- [ ] **Step 2: Set the deterministic archive and publication names**

Inside `alkahest-api/build.gradle.kts`, configure the Java jar and Maven publication:

```kotlin
tasks.jar {
    archiveBaseName.set("alkahest-api")
    from(generateApiVersioningFile.flatMap { it.outputFile })
    manifest {
        attributes("Automatic-Module-Name" to "org.bukkit")
    }
}

configure<PublishingExtension> {
    publications.create<MavenPublication>("maven") {
        artifactId = "alkahest-api"
        outgoingVariants.forEach {
            suppressPomMetadataWarningsFor(it)
        }
        from(components["java"])
    }
}
```

Keep the existing group and compatibility capabilities. Do not rename `Automatic-Module-Name` or Java packages.

- [ ] **Step 3: Verify local and release naming**

Run:

```bash
./gradlew :alkahest-api:clean :alkahest-api:jar
find alkahest-api/build/libs -maxdepth 1 -type f -name '*.jar' -printf '%f\n'
./gradlew :alkahest-api:clean :alkahest-api:jar -PalkahestVersion=2026.08.08.3
find alkahest-api/build/libs -maxdepth 1 -type f -name '*.jar' -printf '%f\n'
```

Expected outputs include:

```text
alkahest-api-26.2.local-SNAPSHOT.jar
alkahest-api-2026.08.08.3.jar
```

- [ ] **Step 4: Verify publication metadata**

Run:

```bash
./gradlew :alkahest-api:publishToMavenLocal -PalkahestVersion=2026.08.08.3
```

Expected artifact path:

```text
$HOME/.m2/repository/io/papermc/paper/alkahest-api/2026.08.08.3/alkahest-api-2026.08.08.3.pom
```

- [ ] **Step 5: Commit the publication identity**

```bash
git add settings.gradle.kts alkahest-api/build.gradle.kts
git commit -m "feat: publish Alkahest API artifact"
```

---

### Task 3: Upload the API jar in GitHub releases

**Files:**
- Create or modify: `.github/workflows/release-alkahest.yml`

**Interfaces:**
- Consumes: tag `vYYYY.MM.DD.N` and `RELEASE_VERSION` environment value.
- Produces: GitHub release assets `alkahest-api-${RELEASE_VERSION}.jar` and the existing Alkahest server jar.

- [ ] **Step 1: Build both Java artifacts with the tag version**

The release workflow must invoke:

```yaml
- name: Compile and package Alkahest
  run: |
    ./gradlew :alkahest-api:jar :paper-server:jar --stacktrace --no-daemon -PalkahestVersion="$RELEASE_VERSION"
    ./gradlew createPaperclipJar --stacktrace --no-daemon -PalkahestVersion="$RELEASE_VERSION"
```

- [ ] **Step 2: Verify the API artifact before publishing**

Add a shell check for:

```bash
api_jar="alkahest-api/build/libs/alkahest-api-${RELEASE_VERSION}.jar"
test -f "$api_jar"
echo "API_JAR=$api_jar" >> "$GITHUB_ENV"
```

Retain the existing server jar and manifest-brand checks.

- [ ] **Step 3: Upload both release assets**

Use the GitHub CLI release command with both paths:

```bash
gh release create "$GITHUB_REF_NAME" "$API_JAR" "$RELEASE_JAR" \
  --repo "$GITHUB_REPOSITORY" \
  --title "Alkahest $RELEASE_VERSION" \
  --generate-notes
```

- [ ] **Step 4: Commit the release asset workflow**

```bash
git add .github/workflows/release-alkahest.yml
git commit -m "ci: publish Alkahest API release asset"
```

---

### Task 4: Update live documentation and build-contract tests

**Files:**
- Modify: `README.md` (preserve existing CraftUX/run changes)
- Modify: `CONTRIBUTING.md`
- Modify: `AGENTS.md` (live repository layout and compatibility wording)
- Modify: `alkahest-api/src/test/java/dev/mintychochip/customentity/CustomEntitySourcesPlacementTest.java` if any remaining path text exists

**Interfaces:**
- Consumes: final module path and artifact coordinate.
- Produces: plugin-developer instructions that match the built artifact and source tree.

- [ ] **Step 1: Update the plugin dependency examples**

Document the Alkahest coordinate and release version shape:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.papermc.paper:alkahest-api:2026.08.08.3")
}
```

Add the local source build path:

```bash
./gradlew :alkahest-api:jar
```

- [ ] **Step 2: Update source-tree references**

Change live navigation instructions from `paper-api` to `alkahest-api`. Keep package names and Paper API javadoc URLs unchanged.

- [ ] **Step 3: Run the focused API tests**

Run:

```bash
./gradlew :alkahest-api:test --tests 'dev.mintychochip.*' --tests 'org.bukkit.*'
```

Expected: the selected API tests pass using the renamed project path.

- [ ] **Step 4: Commit documentation and tests**

```bash
git add README.md CONTRIBUTING.md AGENTS.md alkahest-api/src/test
 git commit -m "docs: document Alkahest API distribution"
```

---

### Task 5: Full artifact verification

**Files:**
- No source changes unless a verification failure identifies a required correction.

- [ ] **Step 1: Build API and server artifacts**

Run:

```bash
./gradlew :alkahest-api:jar :paper-server:jar -PalkahestVersion=2026.08.08.3
```

Expected: successful build with both API and server jars present.

- [ ] **Step 2: Verify exact output paths and names**

Run:

```bash
printf 'API: '; find alkahest-api/build/libs -maxdepth 1 -type f -name 'alkahest-api-2026.08.08.3.jar' -printf '%p\n'
printf 'Server: '; find paper-server/build/libs -maxdepth 1 -type f -name 'alkahest-2026.08.08.3.jar' -printf '%p\n'
```

Expected: one API path and one server path are printed.

- [ ] **Step 3: Verify no live `:paper-api` dependency remains**

Run:

```bash
./gradlew projects
```

Expected: `:alkahest-api` is listed and no live project task references `:paper-api`.

- [ ] **Step 4: Review the final worktree**

Run:

```bash
git status --short --branch
git diff --check
```

Expected: only intentional Alkahest API artifact commits are present; unrelated user files remain untouched and generated artifacts are not staged.
