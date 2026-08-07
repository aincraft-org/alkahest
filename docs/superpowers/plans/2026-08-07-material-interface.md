# Material Interface Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert Bukkit `Material` from an enum into an interface (mirroring `EntityType`/`VanillaEntityType`) so `CustomBlockDefinition` can implement `Material` and participate in lookups while live `getType()` stays carrier-only.

**Architecture:** Move the current enum body to `VanillaMaterial implements Material`. Turn `Material` into an interface that re-exports every vanilla constant (`Material.STONE = VanillaMaterial.STONE`), declares instance methods, and hosts static lookup (`values`, `valueOf`, `getMaterial`, `matchMaterial`, `getByKey`). Custom blocks register via existing `CustomBlocks`; definitions implement Material using host carrier + itemMaterial + BlockFeel.

**Tech Stack:** Paper fork (paper-api + paper-server), JUnit 5, JDK 25, no new NMS block IDs, no patches for mintychochip packages.

**Spec:** `docs/superpowers/specs/2026-08-07-material-interface-design.md`

**Precedent:** `EntityType` + `VanillaEntityType` + `CustomEntityDefinition` (same session / worktree).

---

## File map

| File | Responsibility |
|------|----------------|
| `paper-api/.../org/bukkit/VanillaMaterial.java` | **Create** — enum: all constants + fields + method bodies currently in Material |
| `paper-api/.../org/bukkit/Material.java` | **Rewrite** — interface: constants, method signatures, statics, defaults |
| `paper-api/.../org/bukkit/Registry.java` | `MATERIAL` SimpleRegistry → `VanillaMaterial.class` |
| `paper-api/.../co/aikar/timings/TimingHistory.java` | EnumMap→HashMap for Material (like EntityType) |
| `paper-server/.../craftbukkit/util/CraftLegacy.java` | `ordinal` / `values` slices use `VanillaMaterial` |
| `paper-server/.../craftbukkit/legacy/CraftLegacy.java` | Same ordinal/name fixes |
| `paper-server/.../craftbukkit/util/CraftMagicNumbers.java` | values loop vanilla-only; custom → carrier/itemMaterial |
| `paper-api/.../customblock/CustomBlockDefinition.java` | `implements Material` + method bodies + `carrierMaterial()` |
| `paper-server/.../customblock/CustomBlockPlacement.java` | Delegate carrier to API definition method |
| `paper-api/.../test/.../customblock/CustomMaterialParityTest.java` | **Create** — parity tests |
| `paper-server/.../test/.../PerMaterialTest.java` | `@EnumSource(VanillaMaterial.class)` |
| Javadocs on `CustomBlocks`, `Block`, `ItemStack` | Carrier vs Material-as-definition notes |

Generator (`MaterialRewriter`) can stay targeting the enum file name after rename; update only if generators are re-run in this work.

---

### Task 1: Mechanical split — VanillaMaterial enum

**Files:**
- Create: `paper-api/src/main/java/org/bukkit/VanillaMaterial.java`
- Modify: `paper-api/src/main/java/org/bukkit/Material.java` (will be replaced in Task 2; keep enum temporarily or do Tasks 1–2 as one edit session)
- Test: compile only in this task

- [ ] **Step 1: Copy Material enum to VanillaMaterial**

```bash
cp paper-api/src/main/java/org/bukkit/Material.java \
   paper-api/src/main/java/org/bukkit/VanillaMaterial.java
```

In `VanillaMaterial.java`:

1. Change declaration:
```java
/**
 * Vanilla Minecraft materials. Implements {@link Material} so constants remain usable
 * as {@code Material.STONE} via static fields on the interface.
 *
 * <p>Prefer typing against {@link Material} so custom materials can participate.
 */
public enum VanillaMaterial implements Material {
```

2. Keep all constants, fields (`id`, `ctor`, `data`, `legacy`, `key`, `itemType`, `blockType`), constructors, and **all instance method bodies**.

3. Change constructors from `Material(` to `VanillaMaterial(`.

4. Inside method bodies that refer to `Material.LEGACY_AIR`, `Material.AIR`, etc. for self-comparisons: either keep as `Material.AIR` (after interface exists) or use `VanillaMaterial.AIR` / `this`. Prefer **`Material.AIR`** once Task 2 lands (same instance). During intermediate compile, use `VanillaMaterial.AIR`.

5. Static block that fills `BY_NAME` — keep on enum:
```java
private static final Map<String, VanillaMaterial> BY_NAME = Maps.newHashMap();

static {
    for (VanillaMaterial material : values()) {
        BY_NAME.put(material.name(), material);
    }
}
```

6. Move static lookup helpers used only by vanilla to package-visible statics on `VanillaMaterial` (called from interface later):
```java
@Nullable
static VanillaMaterial byName(@NotNull String name) {
    return BY_NAME.get(name);
}
```

7. Add overrides for interface defaults:
```java
@Override
public boolean isVanilla() {
    return true;
}

@Override
public boolean isCustom() {
    return false;
}
```

8. **Do not** leave `public static Material getMaterial(...)` on the enum long-term — interface owns those (Task 2). Temporarily keep compile-working stubs if needed.

- [ ] **Step 2: Confirm file size sanity**

```bash
wc -l paper-api/src/main/java/org/bukkit/VanillaMaterial.java
# expect ~3700 lines, similar to old Material
```

- [ ] **Step 3: Commit skeleton (optional if Task 2 same session)**

```bash
git add paper-api/src/main/java/org/bukkit/VanillaMaterial.java
git commit -m "refactor: add VanillaMaterial enum (Material split WIP)"
```

---

### Task 2: Material as interface + static constants

**Files:**
- Rewrite: `paper-api/src/main/java/org/bukkit/Material.java`
- Modify: `paper-api/src/main/java/org/bukkit/Registry.java` (if compile requires)

- [ ] **Step 1: Generate interface constant fields**

From the enum constant names (non-body), generate:

```java
package org.bukkit;

// imports: Keyed, Translatable, NamespacedKey, BlockData, ItemType, BlockType, ...
// same imports Material methods need for signatures

/**
 * A material identity (block and/or item) accepted by Bukkit APIs.
 *
 * <p>Vanilla types are the constants on this interface (e.g. {@link #STONE}); they are instances of
 * {@link VanillaMaterial}. Custom types ({@link dev.mintychochip.customblock.CustomBlockDefinition})
 * implement this interface so they can be used anywhere a {@code Material} is accepted.
 *
 * <p>{@link org.bukkit.block.Block#getType()} / {@link org.bukkit.inventory.ItemStack#getType()} return
 * the <strong>carrier</strong> vanilla material for custom blocks/items; use
 * {@code getCustomBlock()} / {@code getCustomKey()} for logical custom identity.
 */
public interface Material extends Keyed, Translatable, net.kyori.adventure.translation.Translatable {

    // ---- vanilla constants (source-compatible with former enum constants) ----
    Material ACACIA_BOAT = VanillaMaterial.ACACIA_BOAT;
    Material ACACIA_CHEST_BOAT = VanillaMaterial.ACACIA_CHEST_BOAT;
    Material AIR = VanillaMaterial.AIR;
    // ... every constant including LEGACY_* ...
    Material LEGACY_AIR = VanillaMaterial.LEGACY_AIR;
    // ... rest of LEGACY_* ...

    @Deprecated(since = "1.13", forRemoval = true)
    String LEGACY_PREFIX = "LEGACY_";
```

Script idea (run from repo root; adjust if needed):

```bash
# Extract enum constant names from VanillaMaterial (lines like "    STONE(-1)," or "    STONE,")
rg -o '^\s+([A-Z][A-Z0-9_]+)\(' paper-api/src/main/java/org/bukkit/VanillaMaterial.java \
  | sed 's/(.*//;s/^[[:space:]]*//' | sort -u > /tmp/mat_names.txt
# Then emit "    Material NAME = VanillaMaterial.NAME;" for each
```

- [ ] **Step 2: Declare instance methods on the interface**

Copy every former **public instance** method signature from Material onto the interface (no bodies except defaults below). Include:

- `translationKey()`, `getItemRarity()`, `getItemAttributes`, `isCollidable`, `getId`, `isLegacy`, `getKey`, `getMaxStackSize`, `getMaxDurability`
- `createBlockData()` / consumer / string overloads
- `getData()`, `getNewData(byte)`
- `isBlock`, `isEdible`, `isRecord`, `isSolid`, `isAir`, `isEmpty`, `isTransparent`
- `isFlammable`, `isBurnable`, `isFuel`, `isOccluding`, `hasGravity`, `isItem`, `isInteractable`
- `getHardness`, `getBlastResistance`, `getSlipperiness`
- `getCraftingRemainingItem`, `getEquipmentSlot`, `getDefaultAttributeModifiers` (both), `getCreativeCategory`
- `getTranslationKey`, `getBlockTranslationKey`, `getItemTranslationKey`
- `isCompostable`, `getCompostChance`
- `asItemType`, `asBlockType`
- `getDefaultData`, `hasDefaultData`, `getDefaultDataTypes`

**Public field `data`:** cannot live on interface. Keep `public final Class<?> data` only on `VanillaMaterial`. Call sites using `material.data` must use `VanillaMaterial` (see Task 3 `PerMaterialTest`).

- [ ] **Step 3: Add default helpers (EntityType pattern)**

```java
    default @NotNull String name() {
        if (this instanceof Enum<?> e) {
            return e.name();
        }
        return getKey().toString();
    }

    default boolean isVanilla() {
        return this instanceof VanillaMaterial;
    }

    default boolean isCustom() {
        return !isVanilla();
    }
```

- [ ] **Step 4: Static values / valueOf / getMaterial / matchMaterial**

```java
    @NotNull
    static Material[] values() {
        final VanillaMaterial[] vanilla = VanillaMaterial.values();
        final Material[] out = new Material[vanilla.length];
        System.arraycopy(vanilla, 0, out, 0, vanilla.length);
        return out;
    }

    @NotNull
    static Material valueOf(@NotNull final String name) {
        return VanillaMaterial.valueOf(name);
    }

    @Nullable
    static Material getMaterial(@NotNull final String name) {
        return getMaterial(name, false);
    }

    @Nullable
    static Material getMaterial(@NotNull String name, boolean legacyName) {
        if (legacyName) {
            if (!name.startsWith(LEGACY_PREFIX)) {
                name = LEGACY_PREFIX + name;
            }
            final VanillaMaterial match = VanillaMaterial.byName(name);
            if (match == null) {
                return null;
            }
            return Bukkit.getUnsafe().fromLegacy(match);
        }
        return VanillaMaterial.byName(name);
    }

    @Nullable
    static Material matchMaterial(@NotNull final String name) {
        return matchMaterial(name, false);
    }

    @Nullable
    static Material matchMaterial(@NotNull final String name, boolean legacyName) {
        Preconditions.checkArgument(name != null, "Name cannot be null");

        // Custom / namespaced key path (Task 5 will fully wire catalog; stub safe for now)
        if (name.indexOf(':') >= 0) {
            final NamespacedKey key = NamespacedKey.fromString(name);
            if (key != null) {
                final Material byKey = getByKey(key).orElse(null);
                if (byKey != null) {
                    return byKey;
                }
            }
        }

        String filtered = name;
        if (filtered.startsWith(NamespacedKey.MINECRAFT + ":")) {
            filtered = filtered.substring((NamespacedKey.MINECRAFT + ":").length());
        }
        filtered = filtered.toUpperCase(java.util.Locale.ROOT);
        filtered = filtered.replaceAll("\\s+", "_").replaceAll("\\W", "");
        return getMaterial(filtered, legacyName);
    }

    /**
     * Resolve any material (vanilla or mintychochip custom catalog) by key.
     * Vanilla-only until Task 5 wires CustomBlocks; implement empty custom branch first.
     */
    @NotNull
    static java.util.Optional<Material> getByKey(@Nullable final NamespacedKey key) {
        if (key == null) {
            return java.util.Optional.empty();
        }
        // Task 5: CustomBlocks.get(key)
        try {
            final Material reg = Registry.MATERIAL.get(key);
            if (reg != null) {
                return java.util.Optional.of(reg);
            }
        } catch (final Throwable ignored) {
            // bootstrap
        }
        final VanillaMaterial byName = VanillaMaterial.byName(key.getKey().toUpperCase(java.util.Locale.ROOT));
        return java.util.Optional.ofNullable(byName);
    }
```

Remove the old enum body from `Material.java` completely.

- [ ] **Step 5: Fix Registry.MATERIAL**

In `paper-api/src/main/java/org/bukkit/Registry.java`:

```java
// was: new SimpleRegistry<>(Material.class, (mat) -> !mat.isLegacy());
// SimpleRegistry requires Enum — use VanillaMaterial, widen return type via cast wrapper if needed.

@SuppressWarnings({"unchecked", "rawtypes"})
Registry<Material> MATERIAL = (Registry) new SimpleRegistry<>(VanillaMaterial.class, (mat) -> !mat.isLegacy());
```

`SimpleRegistry` is `Registry.NotARegistry<T extends Enum & Keyed>`. Casting to `Registry<Material>` matches the EntityType pattern if they used a dedicated registry; for Material minimum slice, this cast is acceptable because every registered entry **is** a `Material`.

- [ ] **Step 6: Compile paper-api**

```bash
./gradlew :paper-api:compileJava 2>&1 | tail -50
```

Expected: errors only in remaining Material-as-enum call sites (Tasks 3–4). Fix pure API issues before server.

- [ ] **Step 7: Commit**

```bash
git add paper-api/src/main/java/org/bukkit/Material.java \
        paper-api/src/main/java/org/bukkit/VanillaMaterial.java \
        paper-api/src/main/java/org/bukkit/Registry.java
git commit -m "refactor: Material is interface; vanilla constants on VanillaMaterial"
```

---

### Task 3: Fix API-side enum-only call sites

**Files:**
- Modify: `paper-api/src/main/java/co/aikar/timings/TimingHistory.java`
- Modify: any paper-api file that fails compile with “enum expected” / missing `ordinal` / `EnumMap`
- Test: `paper-api/src/test/java/org/bukkit/MaterialTest.java` (should still pass)

- [ ] **Step 1: TimingHistory Material EnumMap → HashMap**

Find tile entity material counters (near EntityType HashMap change):

```java
// before
new EnumMap<Material, Counter>(Material.class)

// after (mirror EntityType comment)
new java.util.HashMap<Material, Counter>()
```

Remove unused `EnumMap` import if applicable.

- [ ] **Step 2: Fix any remaining paper-api compile errors**

Common patterns:

| Error | Fix |
|-------|-----|
| `Material.class` for EnumSet/EnumMap | `VanillaMaterial.class` or HashMap |
| `material.ordinal()` | `((VanillaMaterial) material).ordinal()` after `instanceof` / isLegacy checks |
| `switch (material)` on Material | `switch ((VanillaMaterial) material)` when known vanilla |

`isTransparent` switch lives **inside VanillaMaterial** already — no change.

- [ ] **Step 3: Run MaterialTest**

```bash
./gradlew :paper-api:test --tests 'org.bukkit.MaterialTest' --info 2>&1 | tail -40
```

Expected: PASS (uses `Material.values()`, `getMaterial`, `matchMaterial` on vanilla).

- [ ] **Step 4: Commit**

```bash
git add paper-api/src/main/java/co/aikar/timings/TimingHistory.java
# + any other API fixes
git commit -m "fix: paper-api call sites after Material interface split"
```

---

### Task 4: Fix paper-server enum / CraftLegacy / tests

**Files:**
- Modify: `paper-server/src/main/java/org/bukkit/craftbukkit/util/CraftLegacy.java`
- Modify: `paper-server/src/main/java/org/bukkit/craftbukkit/legacy/CraftLegacy.java`
- Modify: `paper-server/src/test/java/org/bukkit/PerMaterialTest.java`
- Modify: other server tests using `@EnumSource(Material.class)` or `ordinal`

- [ ] **Step 1: util CraftLegacy**

```java
public static Material[] modern_values() {
    Material[] values = Material.values();
    return Arrays.copyOfRange(values, 0, VanillaMaterial.LEGACY_AIR.ordinal());
}

public static int modern_ordinal(Material material) {
    if (material.isLegacy()) {
        throw new NoSuchFieldError("Legacy field ordinal: " + material);
    }
    if (!(material instanceof VanillaMaterial vanilla)) {
        throw new NoSuchFieldError("Non-vanilla material has no modern ordinal: " + material);
    }
    return vanilla.ordinal();
}
```

- [ ] **Step 2: legacy CraftLegacy ordinal helpers**

```java
// modern_values / legacy_values style:
return Arrays.copyOfRange(values, VanillaMaterial.LEGACY_AIR.ordinal(), values.length);

// legacy_ordinal:
if (!(material instanceof VanillaMaterial vanilla)) {
    throw new NoSuchFieldError("Non-vanilla material: " + material);
}
return vanilla.ordinal() - VanillaMaterial.LEGACY_AIR.ordinal();
```

Keep `Material.valueOf` / `getMaterial` / `matchMaterial` for name paths (interface statics).

- [ ] **Step 3: PerMaterialTest**

```java
@EnumSource(value = VanillaMaterial.class, names = "LEGACY_.*", mode = EnumSource.Mode.MATCH_NONE)
```

Where `material.data` is used:

```java
Class<?> expectedClass = ((VanillaMaterial) material).data;
```

(If the parameter is already `VanillaMaterial` from `@EnumSource`, use `material.data` directly.)

- [ ] **Step 4: Compile + targeted tests**

```bash
./gradlew :paper-server:compileJava :paper-server:compileTestJava 2>&1 | tail -60
./gradlew :paper-server:test --tests 'org.bukkit.PerMaterialTest' --tests 'org.bukkit.MaterialTest' 2>&1 | tail -40
```

Expected: compile clean; those tests PASS (may need full test suite bootstrap).

- [ ] **Step 5: Commit**

```bash
git add paper-server/src/main/java/org/bukkit/craftbukkit/util/CraftLegacy.java \
        paper-server/src/main/java/org/bukkit/craftbukkit/legacy/CraftLegacy.java \
        paper-server/src/test/java/org/bukkit/PerMaterialTest.java
# + other fixes
git commit -m "fix: CraftLegacy and tests for VanillaMaterial ordinals"
```

---

### Task 5: Lookups — getByKey + matchMaterial for CustomBlocks

**Files:**
- Modify: `paper-api/src/main/java/org/bukkit/Material.java` (`getByKey`, `matchMaterial`)
- Test: `paper-api/src/test/java/dev/mintychochip/customblock/CustomMaterialParityTest.java` (create; partial)

- [ ] **Step 1: Write failing tests**

Create `paper-api/src/test/java/dev/mintychochip/customblock/CustomMaterialParityTest.java`:

```java
package dev.mintychochip.customblock;

import static org.junit.jupiter.api.Assertions.*;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.VanillaMaterial;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class CustomMaterialParityTest {

    @AfterEach
    public void tearDown() {
        CustomBlocks.reset();
    }

    @Test
    public void vanillaConstantsAreMaterialAndSameInstanceAsEnum() {
        assertTrue(Material.STONE instanceof VanillaMaterial);
        assertSame(VanillaMaterial.STONE, Material.STONE);
        assertEquals("STONE", Material.STONE.name());
        assertTrue(Material.STONE.isVanilla());
        assertFalse(Material.STONE.isCustom());
    }

    @Test
    public void valueOfAndValuesStillWorkForVanilla() {
        assertSame(Material.COBBLESTONE, Material.valueOf("COBBLESTONE"));
        boolean found = false;
        for (final Material m : Material.values()) {
            if (m == Material.STONE) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    public void getByKeyResolvesCustomAfterRegister() {
        final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:test_ore")
            .host(PacketHostSpec.defaults())
            .emulate(Material.IRON_ORE)
            .build();
        CustomBlocks.register(def);

        assertEquals(def, Material.getByKey(def.getKey()).orElseThrow());
        assertEquals(def, Material.matchMaterial("mintychochip:test_ore"));
        assertNull(Material.getMaterial("mintychochip:test_ore")); // vanilla name only
    }
}
```

Host construction mirrors `CustomBlockDefinitionTest` / `CustomBlockIdentityTest` (`PacketHostSpec.defaults()`, `ChorusHostSpec.unassigned()`, etc.).

- [ ] **Step 2: Run tests — expect fail on custom lookup / not implements Material**

```bash
./gradlew :paper-api:test --tests 'dev.mintychochip.customblock.CustomMaterialParityTest' 2>&1 | tail -40
```

Expected: vanilla tests may pass; custom getByKey fails until implementation.

- [ ] **Step 3: Wire getByKey to CustomBlocks**

```java
static Optional<Material> getByKey(@Nullable final NamespacedKey key) {
    if (key == null) {
        return Optional.empty();
    }
    final Optional<CustomBlockDefinition> custom = CustomBlocks.get(key);
    if (custom.isPresent()) {
        return Optional.of(custom.get()); // after Task 6 implements Material
    }
    try {
        final Material reg = Registry.MATERIAL.get(key);
        if (reg != null) {
            return Optional.of(reg);
        }
    } catch (final Throwable ignored) {
    }
    return Optional.ofNullable(VanillaMaterial.byName(key.getKey().toUpperCase(Locale.ROOT)));
}
```

**Note:** Until Task 6, `Optional.of(custom.get())` fails to compile if def is not Material — either implement Task 6 next in the same session, or temporarily cast after making def implement Material with stubs.

Recommended order: **start Task 6 method stubs** so `implements Material` compiles, then finish lookups.

- [ ] **Step 4: Commit when custom lookup tests pass with Task 6**

```bash
git add paper-api/src/main/java/org/bukkit/Material.java \
        paper-api/src/test/java/dev/mintychochip/customblock/CustomMaterialParityTest.java
git commit -m "feat: Material.getByKey and matchMaterial resolve custom blocks"
```

---

### Task 6: CustomBlockDefinition implements Material + carrierMaterial

**Files:**
- Modify: `paper-api/src/main/java/dev/mintychochip/customblock/CustomBlockDefinition.java`
- Modify: `paper-server/src/main/java/dev/mintychochip/customblock/CustomBlockPlacement.java`
- Test: extend `CustomMaterialParityTest`

- [ ] **Step 1: Add carrierMaterial() on definition (API)**

Port logic from server `CustomBlockPlacement.carrierMaterial` (no NMS):

```java
/** Vanilla world carrier material for this definition's host. */
public @NotNull Material carrierMaterial() {
    return switch (this.host.type()) {
        case CHORUS -> Material.CHORUS_PLANT;
        case MUSHROOM -> {
            final MushroomHostSpec mush = (MushroomHostSpec) this.host;
            yield mush.variant() == MushroomVariant.RED
                ? Material.RED_MUSHROOM_BLOCK
                : Material.BROWN_MUSHROOM_BLOCK;
        }
        case TRIPWIRE -> Material.TRIPWIRE;
        case PACKET -> packetCollisionMaterial();
    };
}

private Material packetCollisionMaterial() {
    final PacketHostSpec packet = (PacketHostSpec) this.host;
    final String key = packet.collisionMaterialKey();
    if ("minecraft:glass".equals(key) || "glass".equalsIgnoreCase(key)) {
        return Material.GLASS;
    }
    if ("minecraft:barrier".equals(key) || "barrier".equalsIgnoreCase(key)) {
        return Material.BARRIER;
    }
    // Prefer VanillaMaterial.byName / matchMaterial carefully to avoid recursion on customs
    final Material parsed = Material.getMaterial(
        key.startsWith("minecraft:") ? key.substring("minecraft:".length()).toUpperCase(Locale.ROOT)
            : key.toUpperCase(Locale.ROOT).replace(':', '_'));
    if (parsed != null && parsed.isVanilla() && parsed.isBlock()
        && parsed != Material.AIR && parsed != Material.CAVE_AIR && parsed != Material.VOID_AIR) {
        return parsed;
    }
    return Material.GLASS;
}
```

Server placement:

```java
public static @NotNull Material carrierMaterial(@NotNull final CustomBlockDefinition definition) {
    return definition.carrierMaterial();
}
```

- [ ] **Step 2: `implements Material` + method implementations**

```java
public final class CustomBlockDefinition implements Keyed, Material {
```

Implement every Material method. Suggested pattern:

```java
private Material carrier() {
    return carrierMaterial();
}

private Material item() {
    return this.itemMaterial;
}

@Override
public @NotNull NamespacedKey getKey() {
    return this.key;
}

@Override
public boolean isLegacy() {
    return false;
}

@Override
public boolean isBlock() {
    return true;
}

@Override
public boolean isItem() {
    return true;
}

@Override
public boolean isAir() {
    return false;
}

@Override
public boolean isEmpty() {
    return false;
}

@Override
public float getHardness() {
    return this.feel.hardness();
}

@Override
public float getBlastResistance() {
    return this.feel.blastResistance();
}

@Override
public int getMaxStackSize() {
    return item().getMaxStackSize();
}

@Override
public short getMaxDurability() {
    return item().getMaxDurability();
}

@Override
public @NotNull BlockData createBlockData() {
    return Bukkit.createBlockData(carrier());
}

@Override
public @NotNull BlockData createBlockData(@Nullable Consumer<? super BlockData> consumer) {
    return Bukkit.createBlockData(carrier(), consumer);
}

@Override
public @NotNull BlockData createBlockData(@Nullable String data) {
    return Bukkit.createBlockData(carrier(), data);
}

@Override
public @Nullable ItemType asItemType() {
    return null;
}

@Override
public @Nullable BlockType asBlockType() {
    return null;
}

@Override
public int getId() {
    throw new IllegalArgumentException("Cannot get ID of custom Material " + this.key);
}

@Override
public @NotNull Class<? extends MaterialData> getData() {
    throw new IllegalArgumentException("Cannot get data class of custom Material " + this.key);
}

@Override
public @NotNull MaterialData getNewData(final byte raw) {
    throw new IllegalArgumentException("Cannot get new data of custom Material " + this.key);
}

@Override
public @NotNull String translationKey() {
    return this.key.toString();
}

// For remaining isSolid/isFuel/etc.: forward to carrier() for block-ish, item() for item-ish.
// Example:
@Override
public boolean isSolid() {
    return carrier().isSolid();
}

@Override
public boolean isFuel() {
    return item().isFuel();
}

// getDefaultData* — require isItem; forward to item().asItemType() if non-null, else throw/empty
```

If forwarding to `item()`/`carrier()` that are always `VanillaMaterial`, no recursion.

`BlockFeel.emulate` must only accept vanilla; optional guard:

```java
// in BlockFeel.emulate or Builder.emulate:
if (blockMaterial.isCustom()) {
    throw new IllegalArgumentException("emulate target must be vanilla Material");
}
```

- [ ] **Step 3: Expand parity tests**

```java
@Test
public void customDefinitionIsMaterial() {
    final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:test_ore")
        .host(PacketHostSpec.defaults())
        .emulate(Material.IRON_ORE)
        .build();
    CustomBlocks.register(def);

    final Material type = def;
    assertTrue(type.isCustom());
    assertFalse(type.isVanilla());
    assertEquals("mintychochip:test_ore", type.getKey().toString());
    assertTrue(type.isBlock());
    assertTrue(type.isItem());
    assertFalse(type.isAir());
    assertEquals(Material.IRON_ORE.getHardness(), type.getHardness(), 0.001f);
}

@Test
public void carrierCreateBlockDataUsesHost() {
    final CustomBlockDefinition def = CustomBlockDefinition.builder("mintychochip:chorus_ore")
        .host(ChorusHostSpec.unassigned())
        .build();
    assertEquals(Material.CHORUS_PLANT, def.carrierMaterial());
}
```

- [ ] **Step 4: Run tests**

```bash
./gradlew :paper-api:test --tests 'dev.mintychochip.customblock.*' 2>&1 | tail -50
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add paper-api/src/main/java/dev/mintychochip/customblock/CustomBlockDefinition.java \
        paper-server/src/main/java/dev/mintychochip/customblock/CustomBlockPlacement.java \
        paper-api/src/test/java/dev/mintychochip/customblock/CustomMaterialParityTest.java \
        paper-api/src/main/java/org/bukkit/Material.java
git commit -m "feat: CustomBlockDefinition implements Material"
```

---

### Task 7: CraftMagicNumbers mapping for custom Material

**Files:**
- Modify: `paper-server/src/main/java/org/bukkit/craftbukkit/util/CraftMagicNumbers.java`

- [ ] **Step 1: Keep static init vanilla-only**

`Material.values()` already returns vanilla only — no change required if Task 2 statics are correct. Confirm custom never enters maps.

- [ ] **Step 2: getItem / getBlock resolve customs via carrier/item**

```java
public static Item getItem(Material material) {
    if (material != null && material.isLegacy()) {
        material = CraftLegacy.fromLegacy(material);
    }
    if (material instanceof dev.mintychochip.customblock.CustomBlockDefinition def) {
        return getItem(def.itemMaterial());
    }
    return CraftMagicNumbers.MATERIAL_ITEM.get(material);
}

public static Block getBlock(Material material) {
    if (material != null && material.isLegacy()) {
        material = CraftLegacy.fromLegacy(material);
    }
    if (material instanceof dev.mintychochip.customblock.CustomBlockDefinition def) {
        return getBlock(def.carrierMaterial());
    }
    return CraftMagicNumbers.MATERIAL_BLOCK.get(material);
}
```

- [ ] **Step 3: Smoke compile server**

```bash
./gradlew :paper-server:compileJava 2>&1 | tail -30
```

- [ ] **Step 4: Commit**

```bash
git add paper-server/src/main/java/org/bukkit/craftbukkit/util/CraftMagicNumbers.java
git commit -m "feat: CraftMagicNumbers maps custom Material via carrier/item"
```

---

### Task 8: Docs + identity javadoc + final verification

**Files:**
- Modify: `paper-api/src/main/java/dev/mintychochip/customblock/CustomBlocks.java` (class javadoc)
- Modify: `paper-api/src/main/java/org/bukkit/block/Block.java` (custom identity notes)
- Modify: `paper-api/src/main/java/org/bukkit/inventory/ItemStack.java` (same)
- Optional: `AGENTS.md` feature map one-liner for Material interface

- [ ] **Step 1: Update CustomBlocks javadoc**

Replace “Material / getType() stay vanilla carriers” with:

```
 * <p>{@link org.bukkit.block.Block#getType()} / {@link ItemStack#getType()} stay vanilla
 * carriers. {@link CustomBlockDefinition} itself implements {@link org.bukkit.Material}, so
 * callers can pass definitions into Material-typed APIs. Resolve by key via
 * {@link org.bukkit.Material#getByKey(org.bukkit.NamespacedKey)} after {@link #register}.
```

- [ ] **Step 2: Full mintychochip + material tests**

```bash
./gradlew :paper-api:test --tests 'org.bukkit.MaterialTest' --tests 'dev.mintychochip.customblock.*'
./gradlew :paper-server:test --tests 'dev.mintychochip.customblock.*' --tests 'org.bukkit.PerMaterialTest'
```

Expected: all PASS.

- [ ] **Step 3: Commit**

```bash
git add paper-api/src/main/java/dev/mintychochip/customblock/CustomBlocks.java \
        paper-api/src/main/java/org/bukkit/block/Block.java \
        paper-api/src/main/java/org/bukkit/inventory/ItemStack.java
git commit -m "docs: Material interface + custom block identity notes"
```

---

## Self-review vs spec

| Spec requirement | Task |
|------------------|------|
| Material interface + VanillaMaterial | 1–2 |
| Constants source-compatible | 2 |
| values/valueOf vanilla-only | 2 |
| getByKey + matchMaterial customs | 5 |
| getMaterial vanilla-only | 2, 5 tests |
| Carrier-only getType() unchanged | 8 docs; no code change to getType |
| CustomBlockDefinition implements Material | 6 |
| Feel hardness/blast | 6 |
| createBlockData from carrier | 6 |
| carrierMaterial API + placement share | 6 |
| CraftMagicNumbers custom mapping | 7 |
| Registry vanilla-only (min) | 2 |
| No new NMS IDs | all |
| Parity tests | 5–6 |
| EnumMap/ordinal/PerMaterialTest | 3–4 |

**Out of scope (intentionally no task):** logical getType(), asBlockType non-null, full MaterialRegistry merge iteration, generator re-run unless broken.

---

## Execution notes

- **Mechanical constant export** is the highest-risk typing step — prefer a small script; do not hand-type 2000 constants.
- Intermediate state may not compile until Tasks 1–4 complete; prefer one agent session for 1–4.
- Tasks 5–6 tightly coupled (definition must implement Material for getByKey to type-check).
- Match host construction to existing tests in `paper-api/src/test/java/dev/mintychochip/customblock/`.
