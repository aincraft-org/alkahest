# Material as interface (vanilla + custom blocks)

**Date:** 2026-08-07  
**Status:** approved  
**Packages:** `org.bukkit` (Material surface), `dev.mintychochip.customblock` (custom implementors)

## Goal

Turn Bukkit `Material` from an enum into an **interface type**, the same way `EntityType` was opened for custom entities:

1. Vanilla materials remain as named constants (`Material.STONE`, …).
2. **`CustomBlockDefinition` implements `Material`**, so custom blocks can be passed anywhere a `Material` is accepted.
3. Registration stays on **`CustomBlocks.register`** — no second material registry.

Live world/item identity stays **carrier-only**: `Block#getType()` / `ItemStack#getType()` continue to return the vanilla host/base material. Logical custom identity remains `getCustomKey()` / `getCustomBlock()` / the definition itself as a `Material` value.

## Precedent

| | EntityType (done) | Material (this work) |
|--|-------------------|----------------------|
| Interface | `EntityType` | `Material` |
| Vanilla enum | `VanillaEntityType` | `VanillaMaterial` |
| Custom implementor | `CustomEntityDefinition` | `CustomBlockDefinition` |
| Carrier on live objects | `Entity#getType()` → vanilla | `Block`/`ItemStack#getType()` → vanilla |
| Logical identity | `getCustomEntity()` / key | `getCustomBlock()` / key |
| `values()` / `valueOf` | vanilla-only | vanilla-only |
| Key lookup | `EntityType.getByKey` | `Material.getByKey` + `matchMaterial` for key-like names |

## Decisions

| Decision | Choice |
|----------|--------|
| Live `getType()` | **Carrier only** (mirror EntityType) |
| Custom lookup | **`getByKey` + `matchMaterial`**; not `valueOf` / not `values()` |
| Custom method body | **Host carrier + itemMaterial + BlockFeel** |
| Registration | Existing `CustomBlocks` catalog only |
| New NMS block/item IDs | **Out of scope** |

## Type shape

```
Material (interface)
  ├── static constants: STONE = VanillaMaterial.STONE, …
  ├── instance API (hardness, isBlock, createBlockData, …)
  ├── statics: values(), valueOf(), getMaterial(), matchMaterial(), getByKey()
  │
  ├── VanillaMaterial (enum implements Material)
  │     all current constants + fields (id, legacy, key, block/item suppliers)
  │     all switch(this) / ordinal-dependent logic
  │
  └── CustomBlockDefinition (implements Material)
        isCustom() == true
        key = definition NamespacedKey
```

Source-compatible for the common case:

```java
Material m = Material.STONE;           // still works
if (m == Material.AIR) { ... }         // still works (same instances)
Material custom = CustomBlocks.get(...).orElseThrow(); // is a Material
world.getBlockAt(x,y,z).getType();     // still vanilla carrier
```

### Compat helpers (defaults on interface)

```java
default String name() {
    return this instanceof Enum<?> e ? e.name() : getKey().toString();
}

default boolean isVanilla() {
    return this instanceof VanillaMaterial;
}

default boolean isCustom() {
    return !isVanilla(); // no UNKNOWN equivalent for materials
}
```

## Lookup

| API | Vanilla | Custom |
|-----|---------|--------|
| `Material.values()` | All `VanillaMaterial` constants | **No** |
| `Material.valueOf(String)` | Enum name (`"STONE"`) | **No** |
| `Material.getMaterial(String)` | `BY_NAME` on vanilla | **No** |
| `Material.getMaterial(String, legacy)` | existing legacy path | **No** |
| `Material.matchMaterial(String)` | existing normalize → vanilla name | **Yes** if input looks like a namespaced key (`ns:path`) → `CustomBlocks` |
| `Material.getByKey(NamespacedKey)` **new** | Registry / vanilla by key | then `CustomBlocks.get(key)` |

`matchMaterial` key-like detection (order of attempts):

1. Existing strip/`minecraft:` / uppercase / non-word filter for **vanilla** names.
2. If the raw (or lightly trimmed) string is a valid `NamespacedKey` and **not** resolved as vanilla, look up `CustomBlocks`.
3. Prefer: if string contains `:` and parses as `NamespacedKey`, try custom catalog **before** aggressive name scrubbing so `mintychochip:electrum_ore` is not destroyed.

`getByKey` algorithm (mirror EntityType):

```text
if key == null → empty
custom = CustomBlocks.get(key) → if present, return it
else resolve vanilla (Registry.MATERIAL or VanillaMaterial by path)
```

## CustomBlockDefinition as Material

### Identity

| Method | Behavior |
|--------|----------|
| `getKey()` | Definition `NamespacedKey` |
| `name()` | `getKey().toString()` (e.g. `mintychochip:electrum_ore`) |
| `isVanilla()` | `false` |
| `isCustom()` | `true` |
| `isLegacy()` | `false` |
| `equals` / `hashCode` | By key (already true for catalog identity) |
| `toString()` | Keep definition-style or `name()` — key-bearing and stable |

### Placement / data

| Method | Behavior |
|--------|----------|
| `isBlock()` | `true` |
| `isItem()` | `true` |
| `createBlockData()` / overloads | Build via **host carrier** material (from `HostSpec` → placement carrier: glass, chorus, mushroom, tripwire, etc.) — same carrier `CustomBlockPlacement` would use |
| `asBlockType()` | `null` (not a vanilla block registry type) |
| `asItemType()` | `null` (not a vanilla item registry type) |

Callers that need a real registry type for NMS must use the **carrier** material (host / itemMaterial), not the custom Material alone.

### Feel / item defaults

| Method | Source |
|--------|--------|
| `getHardness()` | `feel().hardness()` (`BlockFeel`, including emulate) |
| `getBlastResistance()` | `feel().blastResistance()` |
| `getMaxStackSize()` | `itemMaterial.getMaxStackSize()` |
| `getMaxDurability()` | `itemMaterial` (usually 0 for blocks) |
| `getCraftingRemainingItem()` | `itemMaterial` or null |
| `isEdible` / record / fuel / compostable / … | Forward to **itemMaterial** where the query is item-ish |
| `isSolid` / collidable / gravity / occluding / slipperiness | Prefer **host carrier** block queries when meaningful; never treat custom as air |
| `isAir()` / `isEmpty()` | Always `false` for customs |
| `isTransparent()` | Carrier-based or conservative `false` |
| `getEquipmentSlot` / default attributes / data components | Forward to **itemMaterial** or empty/safe defaults |
| Translation keys | Fall back to `getKey().toString()` or itemMaterial keys |

### Legacy / unsupported

| Method | Behavior |
|--------|----------|
| `getId()` | Throw `IllegalArgumentException` (not legacy) |
| `getData()` / `getNewData(byte)` | Throw (not legacy MaterialData) |

Avoid calling into full registry during pure unit tests where possible (same constraint as current `itemMaterial == AIR` checks that avoid `isAir()`).

### Host carrier resolution

`CustomBlockDefinition` needs a stable **API-layer** method for “what vanilla material is the world carrier?” (no NMS):

- Add `CustomBlockDefinition#carrierMaterial()` (or `CustomBlocks.carrierMaterial(def)`) that maps host → vanilla material, matching existing server `CustomBlockPlacement.carrierMaterial` rules:
  - `CHORUS` → `CHORUS_PLANT`
  - `MUSHROOM` → red/brown mushroom block from variant
  - `TRIPWIRE` → `TRIPWIRE`
  - `PACKET` → configured base or `GLASS` / `BARRIER` fallback
- Server placement should call the same API helper so there is one source of truth.
- **Item base** remains `itemMaterial()` for held stacks.
- Material interface methods pick **carrier** for block-data / solidity; **itemMaterial** for stack/item queries; **feel** for hardness/blast.

## Mechanical conversion

### Files

| Action | Path |
|--------|------|
| Convert | `paper-api/.../org/bukkit/Material.java` → interface |
| Add | `paper-api/.../org/bukkit/VanillaMaterial.java` (enum body moved from Material) |
| Edit | `CustomBlockDefinition` implements `Material` |
| Edit | `Registry.MATERIAL` / `SimpleRegistry` like EntityType (`VanillaMaterial.class`) |
| Edit | Craft sites: `CraftLegacy`, `CraftMagicNumbers`, any `EnumMap`/`@EnumSource(Material)` |
| Edit | Generator `MaterialRewriter` → emit `VanillaMaterial` + interface constants (EntityType pattern) |
| Edit | Javadoc on `CustomBlocks`, `Block`, `ItemStack` |

### Enum-only call sites (must compile)

Anything that requires a real enum must use `VanillaMaterial`:

- `Material.values()` static on interface → `VanillaMaterial.values()` copy
- `ordinal()` — e.g. `CraftLegacy` slice using `LEGACY_AIR.ordinal()` → `VanillaMaterial.LEGACY_AIR.ordinal()`
- `@EnumSource(Material.class)` → `@EnumSource(VanillaMaterial.class)`
- `new EnumMap<>(Material.class)` → `VanillaMaterial.class` or `HashMap<Material, …>`
- `Registry.MATERIAL = new SimpleRegistry<>(Material.class, …)` → `VanillaMaterial.class` + optional wrapper if customs should appear in registry iteration (see below)
- `switch (material)` on interface type → switch on `(VanillaMaterial) material` after `instanceof`, or if-chains

### Registry iteration

**Minimum (this PR):** `Registry.MATERIAL` remains **vanilla-only** (backed by `VanillaMaterial`), same as pre-change content. Customs are found via `getByKey` / `matchMaterial` / `CustomBlocks`, not by iterating the Bukkit material registry.

**Optional follow-up (EntityType-style):** a `MaterialRegistry` façade that merges vanilla + `CustomBlocks.all()` for `iterator()` / `get(key)`. Not required for the first slice if `getByKey` on the interface covers the main need.

### CraftMagicNumbers

Static maps `Material → Block/Item` are built from `Material.values()`. Keep that loop **vanilla-only**. Custom materials must **not** enter `MATERIAL_BLOCK` / `MATERIAL_ITEM` as new NMS mappings; NMS conversion for a custom Material uses **carrier** / **itemMaterial**:

```text
bukkitToMinecraft(custom Material)
  → if VanillaMaterial: existing map
  → if CustomBlockDefinition: map carrier or itemMaterial (API contract: document which path for block vs item)
```

Document:

- **Block context** (set block, createBlockData): carrier material  
- **Item context** (create item stack base): itemMaterial  

### Generator / rewrite

`MaterialRewriter` currently regenerates the items/blocks constant lists inside the enum. After the split:

- Constants live on `VanillaMaterial`
- Interface re-exports `Material.FOO = VanillaMaterial.FOO` (scripted or rewriter-generated)
- Mirror whatever was done for EntityType rewriter if already updated

## Out of scope

- Changing `Block#getType()` / `ItemStack#getType()` to return custom Materials
- Real new block/item registry entries in NMS
- Non-null `asBlockType()` / `asItemType()` for customs
- Auto-including customs in every `Tag<Material>` or `Registry.MATERIAL` iteration (unless optional follow-up)
- Making every plugin `switch (Material)` exhaustiveness work for customs (impossible; customs are open set)

## Testing

| Test | Assert |
|------|--------|
| Parity (like `CustomEntityTypeParityTest`) | `Material.STONE instanceof VanillaMaterial`; same instance; `name()`; `isVanilla` |
| Custom is Material | Register def → `assertTrue(def instanceof Material)`; `isCustom`; key |
| `getByKey` | Vanilla + custom catalog |
| `matchMaterial` | `"mintychochip:foo"` → custom; `"stone"` → vanilla |
| `valueOf` / `values` | Vanilla only; does not include custom |
| Feel | Def with `BlockFeel.emulate(IRON_ORE)` → `getHardness()` matches iron ore |
| Carrier createBlockData | Custom `createBlockData()` matches host carrier type family |
| Existing custom-block tests | Identity / lifecycle still pass |

Run:

```bash
./gradlew :paper-api:test --tests 'dev.mintychochip.customblock.*'
./gradlew :paper-api:test --tests 'org.bukkit.MaterialTest'
# after server wiring fixes:
./gradlew :paper-server:test --tests 'org.bukkit.PerMaterialTest'
./gradlew :paper-server:test --tests 'dev.mintychochip.customblock.*'
```

## Implementation order (PR plan sketch)

1. **Split enum** — `VanillaMaterial` + interface `Material` with static constants; fix compile breaks (Registry, CraftLegacy, EnumMap, tests).
2. **Lookups** — `getByKey`, extend `matchMaterial` for namespaced custom keys.
3. **CustomBlockDefinition implements Material** — method bodies (carrier / item / feel).
4. **Craft NMS mapping** — custom Material → carrier/itemMaterial for block vs item paths.
5. **Tests + javadoc**.

No vanilla `net.minecraft` patches required for this slice if Craft conversion stays in main sources.

## Risks

| Risk | Mitigation |
|------|------------|
| Huge constant file move | Mechanical rename/copy; keep generator in sync |
| Missed `EnumMap` / `@EnumSource` | Full compile + PerMaterialTest |
| Plugin code uses `Material.class.isEnum()` | Fork accepts this (same as EntityType) |
| Recursive hardness if feel.emulate points at custom | Emulate must be vanilla (document + validate in BlockFeel or definition build) |
| `matchMaterial` false positives | Only treat as custom when `NamespacedKey.fromString` succeeds and catalog hits |

## Success criteria

- `Material.STONE` and all prior constants still compile and compare with `==`.
- `CustomBlockDefinition` is a `Material`; `Material.getByKey(def.getKey())` returns it after register.
- `block.getType()` still returns carrier for placed custom blocks.
- Vanilla server tests that enumerate materials still pass against `VanillaMaterial` / interface statics.
- No `dev.mintychochip` under patch tree; no new NMS block IDs.
