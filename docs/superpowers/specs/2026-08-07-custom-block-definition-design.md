# Custom block definitions (multi-host)

**Date:** 2026-08-07  
**Status:** implementing (definition layer first)  
**Package:** `dev.mintychochip.customblock`

## Goal

Own a **first-class custom block identity** in the Paper fork so gameplay (break/place/loot/skills) can target `mintychochip:foo` instead of special-casing vanilla materials forever.

Vanilla clients cannot receive real new block registry IDs. Each custom block is therefore:

1. A **definition** (keyed, host strategy, item/model metadata)
2. Later: a **placement** hosted on a vanilla carrier or client packets

This document covers **definitions only**. Placement, break events, chunk storage, and pack baking are out of scope for this slice.

## Research summary

### PacketBlocks (`../PacketBlocks`)

Plugin alternative to “baked” note/mushroom/chorus retextures:

- **Server carrier:** real block (placement uses normal place/break); clients see **fake glass** via `Player#sendBlockChange`
- **Visuals:** client-only **item display** packets (`ClientboundAddEntityPacket` + metadata); no real display entities ticking on the server
- **Identity store:** SQLite binding `location → resourceKey`
- **Definition shape:** keyed entry with `item-model`, optional display name / transform (translation, scale, rotations)
- **Events:** custom `PacketBlockBreakEvent` / Place / Interact layered on Bukkit `BlockBreakEvent` etc.

Good for **near-unlimited variants** with low server entity cost; higher client render cost.

### Baked hosts (chorus / mushroom / tripwire)

Classic Nexo/Oraxen/ItemsAdder style:

| Host | Vanilla block | How variants are encoded |
|------|---------------|---------------------------|
| **Chorus** | `CHORUS_PLANT` / related | Block-state combinations remapped in the resource pack |
| **Mushroom** | brown/red huge mushroom block | Face boolean states → model predicates |
| **Tripwire** | `TRIPWIRE` | Attached/disarmed/north/… state bits |

Properties:

- **Server-authoritative** real block states (persist in chunk data)
- **Finite capacity** per host type (state space)
- **Resource pack required** to replace the host’s models
- Normal `BlockBreakEvent` / place work on the real block; identity is “this state = our key”

### Decision

Support **four host strategies** on one definition API:

| `BlockHostType` | Server footprint | Capacity | Client cost |
|-----------------|------------------|----------|-------------|
| `CHORUS` | real block states | limited | low (baked) |
| `MUSHROOM` | real block states | limited | low (baked) |
| `TRIPWIRE` | real block states | limited | low (baked) |
| `PACKET` | binding + packets (PacketBlocks-style) | effectively unlimited | higher |

Definitions are host-agnostic at the catalog layer; host-specific fields live on a sealed `HostSpec`.

## Layers

| Layer | Path | Role |
|-------|------|------|
| **API** | `paper-api/.../dev/mintychochip/customblock/` | Pure definitions, catalog, host specs |
| **Server** | `paper-server/.../dev/mintychochip/customblock/` | Place/break, packet models, chunk/store (**later**) |
| **Vanilla hooks** | thin `// mintychochip` | only if needed (**later**) |

No patches for definition code. No `net.minecraft` in paper-api.

## Model

```
CustomBlockDefinition
  id: NamespacedKey
  host: BlockHostType
  hostSpec: HostSpec          // sealed per host
  itemMaterial: Material      // inventory base item (prefer a placeable block for place animation)
  itemModel: Key              // resource-pack item model
  displayName: Component?     // optional
  itemLore: List<Component>?  // optional
```

### Host specs

- **ChorusHostSpec** — baked chorus; optional reserved state index (assigned by pack tool later)
- **MushroomHostSpec** — `BROWN` or `RED` mushroom block family; optional state index
- **TripwireHostSpec** — baked tripwire; optional state index
- **PacketHostSpec** — item-display transform (translation / scale / left-right rotation) + optional collision material key for client fake block (default glass, matching PacketBlocks)

State indices are **optional** at definition time. Runtime assignment / pack composition is a later pipeline.

### Catalog

- `CustomBlockCatalog` — register, get, contains, all (immutable snapshot views)
- `CustomBlocks` — static façade for the active process catalog (mirrors `Ecology` / `Seasons` style)

Duplicate keys on register → fail fast.

## Identity API (additive — implemented)

`getType()` stays vanilla. Parallel queries:

| Call site | Methods |
|-----------|---------|
| `Block` | `getCustomKey()`, `getCustomBlock()`, `isCustomBlock()` (defaults → `CustomBlocks`) |
| `ItemStack` | `getCustomKey()`, `getCustomBlock()`, `isCustomBlockItem()` |
| Façade | `CustomBlocks.of(block\|stack)`, `keyOf(...)`, `createItemStack`, `stamp` |

Held form: PDC `mintychochip:custom_block` = namespaced id + item model from definition.

World form: `CustomBlockLookup` (server `MemoryCustomBlockLookup` for now; not persisted).

## Give command + sample (implemented)

```text
/customblock give electrum_ore
/customblock give electrum_ore 16
/customblock give mintychochip:electrum_ore 1
/cb give electrum_ore
/customblock list
```

Default definition: `mintychochip:electrum_ore` (PACKET host, **glass** base item for place animation, item model key `mintychochip:electrum_ore`).

Pack assets follow vanilla block-item shape (not a flat `item/generated` plane):

- `assets/mintychochip/items/electrum_ore.json` → model `mintychochip:block/electrum_ore`
- `assets/mintychochip/models/block/electrum_ore.json` → parent `minecraft:block/cube_all`, texture `mintychochip:block/electrum_ore`
- `assets/mintychochip/textures/block/electrum_ore.png`

Resource pack: auto-served on join via embedded HTTP (`CustomBlockPackService`).

- Config: `config/mintychochip/resource-pack.json` (`enabled`, `port` default 8765, `publicUrl`, `force`, `joinDelayTicks`)
- Pack source: `resourcepacks/mintychochip/` under server root, else classpath `mintychochip-pack/`
- Export: `mintychochip/pack.zip` written on boot
- Route: `http://<host>:<port>/pack/<sha1>.zip`

## Out of scope (later)

- Packet entity spawn / `sendBlockChange` visuals for placed PACKET hosts
- SQLite or chunk-attached placement store
- Resource-pack state allocation for baked hosts
- Bukkit events (`CustomBlockBreakEvent`, …) optional sugar
- Custom items as a separate registry (definitions carry item presentation only)

## Success criteria (this slice)

1. Can define blocks for all four host types in-code
2. Catalog registers and resolves by `NamespacedKey`
3. Host type and host spec are always consistent
4. Packet host defaults match PacketBlocks-ish transforms (centered, slight scale > 1)
5. Unit tests cover validation and registry behavior
6. No NMS / no patches

## Example (API)

```java
CustomBlockDefinition ore = CustomBlockDefinition.builder(
        NamespacedKey.fromString("mintychochip:electrum_ore"))
    .host(PacketHostSpec.defaults())
    .itemMaterial(Material.GLASS) // placeable block base → client place animation
    .itemModel(Key.key("mintychochip", "electrum_ore"))
    .displayName(Component.text("Electrum Ore"))
    .build();

CustomBlocks.catalog().register(ore);
```
