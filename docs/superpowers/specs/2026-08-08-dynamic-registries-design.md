# Dynamic registries across the Paper API

**Date:** 2026-08-08  
**Status:** proposed  
**Scope:** every registry exposed by `io.papermc.paper.registry.RegistryKey`, plus the fork’s catalog-only `Material` surface

## Goal

Make every Paper registry extensible through one lifecycle-aware registration model without pretending that all Minecraft registries have the same storage, bootstrap, or client behavior.

“Open” means that a plugin can register a new, namespaced value through the supported API at the registry’s valid lifecycle phase and then resolve it through the corresponding registry view. It does not mean that a frozen NMS registry may be mutated arbitrarily during normal gameplay.

The design must cover:

- the static/built-in registries;
- datapack/worldgen and reloadable registries;
- API-only registry views;
- the fork’s existing `Material` and `EntityType` catalog extensions;
- holder identity, tags, serialization, client synchronization, save/load, and reload behavior.

## Current state

Paper currently classifies `RegistryKey` values as:

- **Built-in:** initialized by vanilla bootstrap and not changed by datapacks;
- **Data-driven:** created from vanilla and other datapacks;
- **API-only:** exposed to the Bukkit/Paper API without a normal server-backed `PaperRegistries` entry.

The server metadata in `paper-server/src/main/java/io/papermc/paper/registry/PaperRegistries.java` adds a second classification. Entries can be plain Craft conversions or buildable entries with one of `NONE`, `ADDABLE`, `MODIFIABLE`, or `WRITABLE` mutation supports. This checkout uses `WRITABLE` for the existing registration API and does not currently use `ADDABLE` or `MODIFIABLE` entries.

The NMS `MappedRegistry` rejects writes after `freeze()`. Built-in registries are frozen during `BuiltInRegistries.bootStrap`; data-driven registries are loaded by `RegistryDataLoader`; predicate, item-modifier, and loot-table registries are rebuilt by `ReloadableServerRegistries`.

The existing fork-specific catalogs deliberately avoid creating new vanilla registry IDs:

- `CustomBlockCatalog` exposes custom blocks through carrier blocks, packets, and item identity;
- `CustomEntities` exposes custom entity definitions through carrier entities and identity data;
- `Registry.MATERIAL` merges vanilla materials with the custom-block catalog;
- `Registry.ENTITY_TYPE` merges vanilla entity types with the custom-entity catalog.

Those catalogs are valid dynamic API surfaces, but they are not equivalent to adding native block, item, or entity IDs to the NMS registries.

## Decision

Use one public registration model with an explicit backend manifest. Every registry key is open, but each key chooses the backend that preserves its lifecycle and runtime invariants.

### Backend kinds

| Backend | Registry value is stored in | Valid registration phase | Native NMS identity |
|---|---|---|---|
| `NATIVE_STATIC` | A built-in `MappedRegistry` | Plugin bootstrap, before built-in freeze | Yes |
| `NATIVE_DATA` | A worldgen/datapack `MappedRegistry` | After normal data load and before dependent lookup finalization | Yes |
| `NATIVE_RELOADABLE` | A reloadable server registry | Each initial load and each datapack reload | Yes, for the active registry snapshot |
| `CATALOG` | A Paper/Bukkit catalog | Catalog bootstrap and documented runtime registration points | No |
| `MERGED` | Vanilla native registry plus a fork catalog | Native lifecycle for vanilla values; catalog lifecycle for custom values | Native values yes; catalog values no |

The manifest is authoritative. No registry may be silently treated as a different backend based on the concrete registry implementation at runtime.

### Registry-to-backend policy

#### Native static registries

These use `NATIVE_STATIC` and receive registry-specific builders and NMS converters:

- `GAME_EVENT`
- `STRUCTURE_TYPE`
- `MOB_EFFECT`
- `BLOCK`
- `ITEM`
- `VILLAGER_PROFESSION`
- `POINT_OF_INTEREST_TYPE`
- `VILLAGER_TYPE`
- `MAP_DECORATION_TYPE`
- `MENU`
- `ATTRIBUTE`
- `FLUID`
- `SOUND_EVENT`
- `DATA_COMPONENT_TYPE`
- `GAME_RULE`

`ENTITY_TYPE` is the deliberate exception. Vanilla entries remain native, while fork custom entity definitions remain catalog-backed through a `MERGED` view. The merged view is authoritative for Bukkit lookup and iteration; a catalog custom entity is not serialized as a new NMS `EntityType` unless a separate native entity implementation is supplied. This preserves the existing carrier contract instead of claiming that a carrier is a native type.

`BLOCK` and `ITEM` remain native registry surfaces. The existing custom-block material catalog is additive and does not masquerade as a native `Block` or `Item` entry.

#### Native data-driven registries

These use `NATIVE_DATA`:

- `BIOME`
- `STRUCTURE`
- `TRIM_MATERIAL`
- `TRIM_PATTERN`
- `DAMAGE_TYPE`
- `WOLF_VARIANT`
- `WOLF_SOUND_VARIANT`
- `ENCHANTMENT`
- `JUKEBOX_SONG`
- `BANNER_PATTERN`
- `PAINTING_VARIANT`
- `INSTRUMENT`
- `CAT_VARIANT`
- `CAT_SOUND_VARIANT`
- `FROG_VARIANT`
- `CHICKEN_VARIANT`
- `CHICKEN_SOUND_VARIANT`
- `COW_VARIANT`
- `COW_SOUND_VARIANT`
- `PIG_VARIANT`
- `PIG_SOUND_VARIANT`
- `ZOMBIE_NAUTILUS_VARIANT`
- `SULFUR_CUBE_ARCHETYPE`
- `DIALOG`

The registry’s normal datapack values load first. Plugin composition then adds namespaced values through the same `MappedRegistry` and holder graph. Worldgen values must be present before dependent world creation or lookup construction; they are not live-edited in already-generated chunks.

#### API-only and catalog-backed surfaces

These use `CATALOG` unless a later native implementation is explicitly added:

- `PARTICLE_TYPE`
- `POTION`
- `MEMORY_MODULE_TYPE`
- the custom side of `ENTITY_TYPE`
- `Registry.MATERIAL`, which is not a `RegistryKey` but is part of the fork’s dynamic registry goal

Catalog values must still have stable namespaced keys, deterministic iteration, tag behavior where supported, and a defined serialization identity. They must not be inserted into an unrelated native registry merely to make a lookup pass.

#### Reloadable server registries

The current public `RegistryKey` catalog does not expose the NMS-only reloadable keys `LOOT_TABLE`, `ITEM_MODIFIER`, or `PREDICATE` as typed Paper registries. They are not acceptance items for this scope. If one of those keys is promoted into the public catalog, it must use `NATIVE_RELOADABLE`: plugin values are re-applied to every new snapshot after datapack values are scanned and before validation/tags are finalized. The implementation must not retain stale holders from the previous snapshot.

## Public API model

### One lifecycle-aware registration surface

Every exposed registry gets a registry event provider and a writable registration path appropriate to its backend. The existing `RegistryComposeEvent` and `WritableRegistry` concepts remain the center of the API:

- compose handlers run after the normal registry values are loaded;
- registration creates a unique namespaced key and a value from a typed builder;
- the registry view includes the resulting value after the lifecycle phase completes;
- entry-add handlers may modify values only where the manifest grants modification support.

The API must not expose a generic untyped object escape hatch. Each registry gets a typed builder/filler and conversion rules that can validate its invariants before any NMS write or catalog publication.

### Mutation support

The existing four-level support model remains meaningful:

- `NONE`: internal-only registry; forbidden in the final manifest for an exposed registry;
- `ADDABLE`: new values can be registered, existing values are not changed;
- `MODIFIABLE`: existing values can be transformed during entry-add processing, but new values are not composed;
- `WRITABLE`: both adding and supported entry modification are available.

A registry’s support level is independent of its lifecycle backend. For example, a static registry can be `ADDABLE`, while a data-driven registry can be `WRITABLE`.

The initial opening target is additive registration for every registry. Existing-value replacement and removal remain forbidden unless the registry-specific builder proves that holder, codec, client, and runtime invariants remain valid.

### Key and namespace rules

- Plugin-created keys must use a non-`minecraft` namespace.
- Duplicate keys fail before modifying the registry or catalog.
- Duplicate object identity fails with the same semantics as `MappedRegistry`.
- A registration failure leaves the previous registry snapshot unchanged.
- Registration order is deterministic: vanilla/datapack values first, then plugin registrations in lifecycle priority and registration order.
- The API must report whether a returned value is native, catalog-backed, or merged-view custom data where that distinction affects serialization or behavior.

## Server architecture

### Registry manifest

Add one manifest entry for each `RegistryKey` containing:

- API key and NMS resource key, when one exists;
- backend kind;
- mutation support;
- typed builder/filler;
- Bukkit-to-NMS and NMS-to-Bukkit conversion;
- lifecycle hook (`STATIC`, `WORLDGEN`, `DIMENSIONS`, `RELOADABLE`, or `CATALOG`);
- tag and network synchronization policy;
- serialization and persistence policy.

`PaperRegistryAccess` uses the manifest to create the correct registry view. `getWritableRegistry` must reject only keys that are genuinely not exposed, never merely because a key has not yet received a generic fallback.

A manifest test must enumerate all `RegistryKey` constants and fail if any key lacks an explicit backend entry.

### Native static registration

Use the existing `BuiltInRegistries.bootStrap(Runnable)` window. Plugin handlers run after vanilla bootstrap values exist and before the registry freeze sequence. The native path must:

1. validate the typed builder;
2. create the NMS value and its holder;
3. register through `PaperRegistryListenerManager`;
4. apply tags before freeze;
5. lock reference-holder creation only after all plugin registrations;
6. freeze and validate the completed registry.

Registry-specific integration is required for types whose constructors register intrusive holders or populate global tables. Blocks, items, fluids, menus, and entity types cannot use a generic reflective constructor.

### Native data-driven registration

Extend the existing delayed registry path rather than replacing it. The loader sequence is:

1. create the normal data-driven registry;
2. scan vanilla and datapack values;
3. run entry-add handlers where supported;
4. fire compose handlers;
5. register plugin values through typed builders;
6. load and bind tags;
7. validate codecs and holder references;
8. publish the completed registry layer.

`BIOME` and `STRUCTURE` require explicit worldgen builders and codec adapters. Their values must be available before dependent worldgen lookup providers are constructed. Adding a value after a world has already generated terrain does not retroactively change existing chunks.

### Reloadable registration

For registries rebuilt by `ReloadableServerRegistries`, plugin registrations are declarations, not one-time mutations. Each reload creates a fresh registry snapshot and replays declarations after datapack scan. The implementation must:

- avoid retaining holders from the old snapshot;
- rebuild tags against the new holders;
- rerun validation;
- publish the new Paper registry view atomically;
- make concurrent readers observe either the old complete snapshot or the new complete snapshot.

### Catalog and merged registration

Catalog backends use immutable published snapshots, matching the existing custom-block and custom-entity catalogs. Registration builds a new snapshot, validates it, and publishes it atomically.

`ENTITY_TYPE` remains a merged view:

- vanilla values resolve through the native NMS-backed registry;
- custom carrier definitions resolve through `CustomEntities`;
- iteration is vanilla values followed by custom values in deterministic key order;
- tags remain native-only until a catalog tag model is added;
- serialization of a custom value uses its namespaced catalog identity and never invents an NMS numeric/entity ID.

`MATERIAL` follows the same catalog publication rules through `CustomBlocks`. This is an explicit merged/catalog policy, not an accidental second registry implementation.

## Closed-entry implementation gaps

The following server-backed entries currently use `.build()` and therefore have Bukkit conversion but no typed builder/filler:

- static: `STRUCTURE_TYPE`, `MOB_EFFECT`, `BLOCK`, `ITEM`, `VILLAGER_PROFESSION`, `POINT_OF_INTEREST_TYPE`, `VILLAGER_TYPE`, `MAP_DECORATION_TYPE`, `MENU`, `ATTRIBUTE`, `FLUID`, `DATA_COMPONENT_TYPE`, `GAME_RULE`;
- data-driven: `BIOME`, `STRUCTURE`, `WOLF_SOUND_VARIANT`, `CAT_SOUND_VARIANT`, `CHICKEN_SOUND_VARIANT`, `COW_SOUND_VARIANT`, `PIG_SOUND_VARIANT`.

`SOUND_EVENT` has a builder class but is explicitly created with `RegistryModificationApiSupport.NONE`; it needs an explicit native registration and client synchronization policy before it becomes addable.

The API-only entries use `RegistryEntryMeta.ApiOnly` and need catalog-specific builders rather than NMS conversions.

No implementation is complete by changing `.build()` to `.writable()` alone.

## Error handling

- Unsupported fields fail at builder validation with a registry-specific message.
- Invalid codec references fail before publication.
- Duplicate keys and invalid namespaces fail before mutation.
- Registration after a native freeze fails with a lifecycle error that identifies the required phase.
- A failed reload discards the candidate snapshot and keeps the last complete snapshot active.
- Native registrations that cannot be synchronized to a client fail during registration rather than producing a server-only value that clients cannot decode.
- Catalog-backed values remain usable only through APIs that understand their catalog identity; APIs requiring native holders reject them explicitly.

## Testing and verification

### Manifest and API tests

- Every `RegistryKey` has an explicit backend and support level.
- Every writable key has a typed registration path.
- Duplicate keys, namespace validation, deterministic iteration, and atomic publication are covered.
- API-only and merged views distinguish catalog values from native values where serialization requires it.

### Native static tests

- One custom value is registered in each static registry family before freeze.
- Holder lookup, direct lookup, tags, registry IDs, serialization, and server bootstrap validation succeed.
- Blocks, items, fluids, menus, and entity types cover their registry-specific factories and client synchronization.

### Data-driven and reload tests

- Datapack values load before plugin composition.
- Plugin values survive initial load and are visible through dependent lookup providers.
- Reload rebuilds tags and holders without stale references.
- Biome and structure registrations are validated before worldgen consumes them.

### Catalog tests

- Custom material and entity registrations publish atomically.
- Vanilla and custom values resolve through the merged views.
- Custom identity survives the existing carrier persistence path.
- Native-only tag and serialization APIs reject catalog values with clear errors.

### Smoke verification

Launch a server with one registration per backend family, exercise lookup and iteration, perform a datapack reload, save and reload a world containing catalog-backed values, and connect a client to verify that every native value advertised by the server is decodable.

## Rollout order

1. Make the manifest explicit and add the all-keys coverage test.
2. Generalize the existing writable builder/event path and preserve current writable registries.
3. Open the currently closed data-driven entries with typed builders, beginning with the sound/variant entries and then biome/structure worldgen handling.
4. Open static registries with registry-specific native definitions and bootstrap registration.
5. Formalize catalog and merged backends for API-only values, `Material`, and custom `EntityType`.
6. Add reloadable snapshot replay where Paper exposes those registries.
7. Run the full native/client/save-reload smoke matrix before declaring every registry open.

The final acceptance condition is not “all metadata entries say `WRITABLE`.” It is that every exposed registry has a tested backend, lifecycle, builder, lookup view, and serialization/client policy, with no silent closed path.
