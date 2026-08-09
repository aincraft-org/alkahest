# Alkahest Server Branding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make release-facing server output say `Alkahest version YYYY.MM.DD.N` while preserving Paper API and plugin compatibility contracts.

**Architecture:** Read the existing manifest `Specification-Version` through `ServerBuildInfoImpl` and expose it through an additive default `ServerBuildInfo.releaseVersion()` method. Use that value only in display paths; leave the existing `ServerBuildInfo.asString(...)` and `Server#getVersion()` formats unchanged.

**Tech Stack:** Java 25, Paper API/server Gradle modules, JUnit 5, Java `Manifest` metadata.

## Global Constraints

- Preserve `io.papermc.paper`, `org.bukkit`, Paper commands, permissions, config keys, plugin identifiers, and internal compatibility names.
- The release display version comes from manifest `Specification-Version`, populated by `-PalkahestVersion`.
- Display output must use the existing manifest `Brand-Name` (`Alkahest`) and release version (`YYYY.MM.DD.N`).
- Do not alter `ServerBuildInfo.asString(...)` or `Server#getVersion()` semantics.
- Do not modify vanilla patch-managed sources; all target files are normal API/server sources.
- Existing `AnnotationTest` missing-annotation failures are unrelated and must remain explicitly reported.

---

### Task 1: Add the release-version display contract

**Files:**
- Modify: `paper-api/src/main/java/io/papermc/paper/ServerBuildInfo.java`
- Modify: `paper-api/src/main/java/org/bukkit/Bukkit.java`
- Modify: `paper-api/src/test/java/io/papermc/paper/TestServerBuildInfo.java`
- Create: `paper-api/src/test/java/org/bukkit/ServerVersionMessageTest.java`

**Interfaces:**
- Produces `ServerBuildInfo.releaseVersion(): String` with a default fallback to `asString(StringRepresentation.VERSION_SIMPLE)`.
- `Bukkit.getVersionMessage()` consumes `releaseVersion()` for its display version.

- [ ] **Step 1: Write the failing API display test**

Add a test that calls the existing `TestServer` fixture and asserts the release-version text is present:

```java
package org.bukkit;

import org.bukkit.support.TestServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerVersionMessageTest {
    @Test
    void versionMessageUsesReleaseVersion() {
        TestServer.setup();

        assertTrue(Bukkit.getVersionMessage().contains("version 2026.08.08.1"));
    }
}
```

Make the test fixture's build-info implementation return `2026.08.08.1` from its new release-version method so the test models a release manifest. The production server name remains manifest-driven and is verified in the packaged manifest check.

- [ ] **Step 2: Run the focused test and verify the expected failure**

Run:

```bash
./gradlew :paper-api:test --tests 'org.bukkit.ServerVersionMessageTest' --no-daemon
```

Expected result before implementation: the test fails because `Bukkit.getVersionMessage()` still formats the Minecraft/build string and `ServerBuildInfo.releaseVersion()` does not exist yet.

- [ ] **Step 3: Add the additive contract and use it in Bukkit output**

Add this default method to `ServerBuildInfo`:

```java
/**
 * Gets the distribution release version used in user-facing server branding.
 *
 * @return the release version, or the simple build string when no release metadata exists
 */
default String releaseVersion() {
    return this.asString(StringRepresentation.VERSION_SIMPLE);
}
```

Change `Bukkit.getVersionMessage()` to use `version.releaseVersion()` while retaining the existing name and API-version suffix.

- [ ] **Step 4: Run the focused API test**

Run the same command. Expected result: `ServerVersionMessageTest` passes.

---

### Task 2: Parse release metadata and update server display paths

**Files:**
- Modify: `paper-server/src/main/java/io/papermc/paper/ServerBuildInfoImpl.java`
- Modify: `paper-server/src/main/java/com/destroystokyo/paper/PaperVersionFetcher.java`
- Modify: `paper-server/src/main/java/io/papermc/paper/PaperBootstrap.java`
- Modify: `paper-server/src/main/java/org/spigotmc/WatchdogThread.java`
- Create: `paper-server/src/test/java/io/papermc/paper/ServerBuildInfoImplTest.java`

**Interfaces:**
- `ServerBuildInfoImpl.releaseVersion()` returns manifest `Specification-Version`, falling back to the interface default when absent.
- Display paths consume `ServerBuildInfo.releaseVersion()` and `brandName()` only.

- [ ] **Step 1: Write the failing manifest parsing test**

Add a package-local test in `io.papermc.paper` that builds a manifest with release metadata, constructs `ServerBuildInfoImpl`, and asserts the release version; also assert the empty-manifest fallback equals `VERSION_SIMPLE`:

```java
package io.papermc.paper;

import java.util.jar.Manifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerBuildInfoImplTest {
    @Test
    void readsSpecificationVersionFromManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Brand-Id", "mintychochip:alkahest");
        manifest.getMainAttributes().putValue("Brand-Name", "Alkahest");
        manifest.getMainAttributes().putValue("Specification-Version", "2026.08.08.1");
        manifest.getMainAttributes().putValue("Build-Number", "2026080801");
        manifest.getMainAttributes().putValue("Build-Time", "2026-08-08T00:00:00Z");

        ServerBuildInfoImpl info = new ServerBuildInfoImpl(manifest);

        assertEquals("2026.08.08.1", info.releaseVersion());
    }

    @Test
    void fallsBackWhenManifestHasNoReleaseVersion() {
        ServerBuildInfoImpl info = new ServerBuildInfoImpl(new Manifest());

        assertEquals(info.asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE), info.releaseVersion());
    }
}
```

Make the manifest constructor package-private only if required for this same-package test; do not expose it publicly.

- [ ] **Step 2: Run the server suite and verify the expected failure**

The server Gradle task includes only `**/**TestSuite.class`, so annotate the test with `@Normal` and run the containing suite:

```bash
./gradlew :paper-server:test --tests 'org.bukkit.support.suite.NormalTestSuite' --no-daemon
```

Expected result before implementation: compilation or assertions fail because `Specification-Version` is not parsed and the new release-version behavior is absent.

- [ ] **Step 3: Implement manifest parsing and display formatting**

Add `Specification-Version` parsing to `ServerBuildInfoImpl` using an optional record component. Override `releaseVersion()` to return the manifest value or `asString(VERSION_SIMPLE)` when absent.

Change display-only paths as follows:

```java
// PaperVersionFetcher
private static final String FORK_DESCRIPTION = "Alkahest";
return text(FORK_DESCRIPTION + " version " + BUILD_INFO.releaseVersion());

// PaperBootstrap
"Loading %s %s for Minecraft %s",
bi.brandName(),
bi.releaseVersion(),
bi.minecraftVersionId()

// WatchdogThread
"Alkahest version: " + ServerBuildInfo.buildInfo().releaseVersion()
```

Use the same `Alkahest version <releaseVersion>` text for the startup status message and remove `private fork of Paper` from user-facing output. Preserve the existing Paper compatibility URLs and API/class names in diagnostics that are not distribution branding.

- [ ] **Step 4: Run focused API and server tests**

Run:

```bash
./gradlew :paper-api:test --tests 'org.bukkit.ServerVersionMessageTest' --no-daemon
./gradlew :paper-server:test --tests 'org.bukkit.support.suite.NormalTestSuite' --no-daemon
```

Expected result: the API test passes and the server test report contains `ServerBuildInfoImplTest` with two passing test cases. Other pre-existing suite failures must be reported separately.

- [ ] **Step 5: Commit the implementation and tests atomically**

Review staged paths, then commit only the branding implementation and its tests:

```bash
git add paper-api/src/main/java/io/papermc/paper/ServerBuildInfo.java \
  paper-api/src/main/java/org/bukkit/Bukkit.java \
  paper-api/src/test/java/io/papermc/paper/TestServerBuildInfo.java \
  paper-api/src/test/java/org/bukkit/ServerVersionMessageTest.java \
  paper-server/src/main/java/io/papermc/paper/ServerBuildInfoImpl.java \
  paper-server/src/main/java/com/destroystokyo/paper/PaperVersionFetcher.java \
  paper-server/src/main/java/io/papermc/paper/PaperBootstrap.java \
  paper-server/src/main/java/org/spigotmc/WatchdogThread.java \
  paper-server/src/test/java/io/papermc/paper/ServerBuildInfoImplTest.java
git diff --cached --check
git commit -m "feat: brand visible server versions as Alkahest"
```

---

### Task 3: Verify release packaging and visible output

**Files:**
- Inspect: `paper-server/build.gradle.kts`
- Inspect: `.github/workflows/release-alkahest.yml`
- No source changes unless a verification exposes a real regression.

- [ ] **Step 1: Build a date-versioned release artifact**

Run:

```bash
BUILD_NUMBER=2026080801 \
BUILD_STARTED_AT=2026-08-08T00:00:00Z \
./gradlew :paper-server:clean createPaperclipJar --no-daemon --stacktrace \
  -PalkahestVersion=2026.08.08.1
```

Expected result: `BUILD SUCCESSFUL` and an archive named `alkahest-paperclip-2026.08.08.1.jar`.

- [ ] **Step 2: Inspect the packaged manifest**

Run:

```bash
unzip -p paper-server/build/libs/paper-server-2026.08.08.1.jar META-INF/MANIFEST.MF
```

Verify these exact values:

```text
Brand-Name: Alkahest
Specification-Version: 2026.08.08.1
Build-Number: 2026080801
```

- [ ] **Step 3: Verify output strings against the release metadata**

Use the focused tests plus a small package/runtime smoke check to confirm the version message, Paper version fetcher, bootstrap line, and watchdog label all use `Alkahest` and `2026.08.08.1`, with no `private fork of Paper` in those display paths.

- [ ] **Step 4: Run the relevant release workflow contract check**

Run the workflow's local equivalent: verify the tagged-version regex, artifact name, and manifest checks from `.github/workflows/release-alkahest.yml`. The GitHub workflow must continue to publish the same `alkahest-paperclip-2026.08.08.1.jar` asset.

- [ ] **Step 5: Report the existing unrelated test baseline accurately**

If the full API suite is run, report `AnnotationTest` separately if it still produces the exact baseline-equivalent 469-entry failure; do not describe the full suite as green.
