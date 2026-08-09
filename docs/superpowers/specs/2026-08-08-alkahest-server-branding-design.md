# Alkahest Server Branding Design

**Date:** 2026-08-08
**Status:** Approved scope; implementation pending

## Goal

Release-facing server output must identify the distribution as `Alkahest` and use the date-versioned release value, for example:

```text
Alkahest version 2026.08.08.1
```

The release version is the manifest `Specification-Version`, populated by the existing `-PalkahestVersion` release build override.

## Scope

Change visible distribution labels in:

- Bukkit startup/version output.
- `/version` output and its local version-fetcher message.
- Paper bootstrap startup text.
- Watchdog diagnostic label and version value.

Remove the user-facing phrase `private fork of Paper` from Alkahest version output.

Keep these compatibility surfaces unchanged:

- `io.papermc.paper` and `org.bukkit` packages and API names.
- Paper command names, permissions, configuration keys, and plugin identifiers.
- `ServerBuildInfo.asString(...)` semantics.
- `Server#getVersion()` semantics used by plugins.
- Paper-compatible metrics and internal class names.

## Design

Add a release-version value to the existing `ServerBuildInfo` contract with a compatibility-preserving default that falls back to the current simple build string. `ServerBuildInfoImpl` reads `Specification-Version` from the server manifest and returns it as the release version. Release builds therefore expose the exact `year.month.day.version` value without changing the existing Minecraft/build string API.

Use that value for display-only paths. The existing brand name remains manifest-driven (`Brand-Name: Alkahest`), so `Bukkit#getName()` continues to be the source for the distribution name.

Expected release output:

```text
This server is running Alkahest version 2026.08.08.1 ...
Alkahest version 2026.08.08.1
Loading Alkahest 2026.08.08.1 for Minecraft 26.2
```

## Testing

Add focused assertions for the release-version contract and visible output. Verify:

1. A manifest-provided `Specification-Version` is surfaced as the release version.
2. The default contract preserves the existing simple build string when no release version is available.
3. Release packaging embeds `Brand-Name: Alkahest` and `Specification-Version: YYYY.MM.DD.N`.
4. The release workflow's manifest check still passes.

The existing unrelated `AnnotationTest` failure is not part of this change; it must remain explicitly reported if still present.
