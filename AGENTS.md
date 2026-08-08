# AGENTS.md — mintychochip Paper fork

This repository is a **private Paper fork** for a Minecraft server. Upstream is [PaperMC/Paper](https://github.com/PaperMC/Paper); our custom work lives under the **`dev.mintychochip`** Java package domain.

**Upstream policy: Paper is our upstream, and we never push to it.** All work flows from PaperMC/Paper → this repo via rebase/graft only. Never push to the `upstream` remote, never open PRs upstream from this fork, and never push fork commits onto any branch that tracks `upstream`. The fork's distribution identity is **Alkahest** (jar name, brand, F3/`/version`), while the API surface remains `io.papermc.paper` / `org.bukkit` for plugin compatibility.

Agents should treat this as a long-lived server fork: preserve upstream structure, keep custom code namespaced, and only touch vanilla/Paper files when a hook is required. The jar is branded **Alkahest** (`Brand-Id: mintychochip:alkahest`); upstream identity (`io.papermc.paper`, `paper-api`) is kept for plugin/API compatibility and is not renamed.

---

## Goals

Custom gameplay is implemented **in the server jar**, not as an external plugin for core systems.

Current systems:

| System | Package | Layer | Purpose |
|--------|---------|-------|---------|
| **Seasons** | `dev.mintychochip.season` | paper-api (+ thin NMS hooks) | Wall-clock seasons; temperature swing; winter snow via forced rain |
| **Ecology** | `dev.mintychochip.ecology` | paper-api (pure) + paper-server NMS façade + thin hooks | Climate/humidity crop suitability; growth gating; unsuitable plant pop |
| **Genetics** | `dev.mintychochip.genetics` | paper-api (pure engine + DTOs); server wiring later | Recombination breeding, sex-linked traits, point mutations |
| **Custom blocks** | `dev.mintychochip.customblock` | paper-api (definitions); server place/break later | Multi-host custom blocks: chorus / mushroom / tripwire baked + packet displays |
| **Provenance** | `dev.mintychochip.provenance` | paper-api (DTOs) + paper-server engine + thin NMS hooks | Stack UUID + birth/death/lineage; craft parents; dupe COLLISION |

Future custom work should continue under `dev.mintychochip.<feature>` unless it is a pure upstream-style fix.

---

## Critical: patches are only for Minecraft / upstream files

**`dev.mintychochip` code does not use the patch system.**

Paper’s patch workflow exists because **vanilla Minecraft sources are not normal git-tracked Java**. They are decompiled, then modified via patches. That applies only to:

- existing `net.minecraft.*` (and similar) files under the minecraft tree
- data/resources that ship with Minecraft
- large optional multi-file **feature** patches (upstream Paper style)

Wholly new packages we own — including NMS-using server logic — belong as **ordinary source files**:

| Layer | Path | Patch? |
|-------|------|--------|
| Public / pure API | `paper-api/src/main/java/dev/mintychochip/...` | **No** |
| API tests | `paper-api/src/test/java/dev/mintychochip/...` | **No** |
| Server implementation (may use `net.minecraft.*`) | `paper-server/src/main/java/dev/mintychochip/...` | **No** |
| Server tests | `paper-server/src/test/java/dev/mintychochip/...` | **No** |
| Edits to **vanilla** classes | `paper-server/src/minecraft/java/net/minecraft/...` | **Yes** → rebuild patches |

This matches how CraftBukkit/Paper already work: `paper-server/src/main/java` freely references NMS and is compiled with the server. No `.patch` files for those packages.

### Anti-pattern (avoid)

Do **not** add new files under:

```text
paper-server/patches/sources/dev/mintychochip/...
paper-server/src/minecraft/java/dev/mintychochip/...
```

That puts our code into the Minecraft patch tree, so every edit pays `applyPatches` / `fixupSourcePatches` / `rebuildPatches` overhead for no benefit. If you find ecology (or other mintychochip packages) living there, **move them to `paper-server/src/main/java/dev/mintychochip/`** and delete the corresponding patches.

**Vanilla hooks stay as patches** — only the thin `// mintychochip` call sites inside `net/minecraft/...`.

---

## Repository layout (what agents edit)

```
paper/
├── paper-api/
│   └── src/main/java/dev/mintychochip/
│       └── season/                 # EDIT — seasons API (normal sources)
├── paper-server/
│   ├── src/main/java/
│   │   ├── org/... io/...          # CraftBukkit / Paper (leave alone unless needed)
│   │   └── dev/mintychochip/       # EDIT — our server-side code (normal sources)
│   │       └── ecology/            # preferred home for ecology (not under patches)
│   ├── src/minecraft/java/         # Minecraft only — patch-managed
│   │   └── net/minecraft/...       # EDIT only for thin hooks, then rebuild patches
│   ├── patches/
│   │   ├── sources/net/minecraft/  # rebuilt from minecraft edits
│   │   ├── resources/
│   │   └── features/               # upstream-style large feature patches
│   └── src/test/java/dev/mintychochip/
├── run/                            # local server workdir (runtime; not source of truth)
│   └── config/mintychochip/        # e.g. ecology.json at runtime
├── bench/, serve/                  # local helpers
└── CONTRIBUTING.md                 # upstream Paper patch workflow
```

### Where to put new code

| Kind of change | Where | Versioned as |
|----------------|-------|--------------|
| Plugin-visible API, pure logic, clocks, enums | `paper-api/.../dev/mintychochip/<feature>/` | Normal `.java` files |
| Server logic that uses NMS (`ServerLevel`, blocks, biomes, …) | `paper-server/src/main/java/dev/mintychochip/<feature>/` | Normal `.java` files |
| Change behavior of an existing vanilla class | Edit applied file under `src/minecraft/java/net/minecraft/...` | Per-file patch under `patches/sources/net/minecraft/...` |
| Runtime config | Load from `config/mintychochip/<name>.json` (server root) | Defaults in code + optional committed example |

**Package rule:** always `dev.mintychochip.<area>`. No alternate roots.

**Scope rule:** do not bulk-edit upstream Paper packages unless a mintychochip feature needs a minimal integration point.

---

## When to put features in paper-api vs paper-server

**Default: `paper-server`.** Most mintychochip work is server-internal gameplay (NMS hooks, growth rules, config loaders, world simulation). That code lives in `paper-server/src/main/java/dev/mintychochip/` and is **not** on the plugin compile classpath.

Plugins and external modules only compile against **`paper-api`**. Anything that stays solely under `paper-server` cannot be imported by a plugin — there is no public jar surface for those classes.

### Use paper-api when plugins (or other modules) need to *import* it

Put types in `paper-api/src/main/java/dev/mintychochip/<feature>/` if any of these are true:

| Reason | Examples |
|--------|----------|
| Plugins should call or read the feature | `Seasons.current()`, season enum for UI / quests / economy |
| Stable contracts without NMS | enums, pure clocks, DTOs, service interfaces, events |
| Other features / API code need a shared pure dependency | climate math that must not pull `net.minecraft` |
| You want a compile-time dependency for external tools | tooling that depends on `paper-api` only |

**paper-api rules:**

- **No `net.minecraft.*`** — API is NMS-free (same as Bukkit/Paper).
- Prefer **stable, small surfaces**: enums, pure functions, read-only queries, events, interfaces.
- Implementation that needs blocks/biomes/levels stays on the server; the API exposes only what outsiders should see.
- Optional split: thin API façade + server implementation (API interface in `paper-api`, NMS-backed class in `paper-server`).

### Keep paper-server only when nothing external needs the types

Stay in `paper-server/src/main/java/dev/mintychochip/` when:

| Reason | Examples |
|--------|----------|
| Feature is internal server simulation only | crop growth gating, pop-if-unsuitable, water proximity scans |
| Logic requires NMS (`ServerLevel`, `BlockPos`, biomes, …) | ecology suitability engine |
| Config loaders / defaults only the server uses | `ecology.json` load path |
| No plugin is expected to call into it | “just make crops behave differently” |

Ecology is the model for this: full system on the server, plugins cannot `import dev.mintychochip.ecology.*` unless you later **promote** a deliberate API surface.

### Promoting server → API later

You do **not** need API on day one. When a plugin needs access:

1. Identify the **minimal** types/methods plugins need (e.g. “is this crop suitable here?”, current season).
2. Move pure/NMS-free pieces into `paper-api` (or add a new façade there).
3. Leave NMS-heavy code in `paper-server`; implement the API façade from the server if needed.
4. Do **not** dump entire server packages into `paper-api` just to “export everything.”

### Mental model (Paper-style)

```
plugin.jar  ──compiles against──►  paper-api  (public types only)
server jar  ──includes both──►    paper-api + paper-server (+ NMS)
```

| Layer | Who can import it? | Typical contents |
|-------|--------------------|------------------|
| **paper-api** | Plugins, API tests, pure tools | Enums, clocks, events, interfaces, pure helpers |
| **paper-server** | Server only (not plugin compile path) | NMS logic, config I/O, vanilla call targets |
| **vanilla hooks** | N/A (patches) | Thin `// mintychochip` call sites |

### Worked examples

| Feature | Layer | Why |
|---------|-------|-----|
| **Seasons** | **API** | Plugins/UI need `Season` / `Seasons.current()`; clock + climate math are pure (no NMS). Server only adds thin vanilla hooks. |
| **Ecology** | **Server** | Growth gating needs NMS levels/blocks; no plugin API required yet. If later plugins need “suitability query,” add a small API façade — not the whole engine. |

**Rule of thumb:** implement in **server** first. Add to **API** only when something outside the server jar must compile against your types — and keep that surface small and NMS-free.

---

## Patch system (vanilla only)

See `CONTRIBUTING.md` for full detail. Summary for agents:

### When you need patches

Only when modifying files that originate from Minecraft (or Minecraft data), e.g.:

- `net/minecraft/world/level/block/CropBlock.java`
- `net/minecraft/world/level/biome/Biome.java`
- `net/minecraft/server/level/ServerLevel.java`

### Typical vanilla-hook loop

```bash
./gradlew applyPatches          # if minecraft tree not applied yet

# Edit ONLY net/minecraft/... under paper-server/src/minecraft/java/

./gradlew fixupSourcePatches
./gradlew rebuildPatches
```

### Hook style (vanilla files only)

```java
// mintychochip start - short reason
if (!dev.mintychochip.ecology.CropEcology.shouldGrow(level, pos, this, random)) {
    return;
}
// mintychochip end - short reason
```

One-liner:

```java
return base + dev.mintychochip.season.Seasons.temperatureSwing(); // mintychochip - seasonal temp
```

Keep hooks thin: call into `dev.mintychochip.*` in **main** (or API); do not dump large feature logic into vanilla classes.

### Feature patches (`patches/features/`)

Upstream-style large multi-file systems. Prefer normal sources + thin vanilla hooks for mintychochip gameplay.

---

## Feature map (current)

### Seasons — `dev.mintychochip.season` (API) ✅ correct placement

| Class | Role |
|-------|------|
| `Season` | SPRING → SUMMER → AUTUMN → WINTER |
| `SeasonClock` | Pure wall-clock mapping (`DEFAULT_LENGTH_DAYS = 7`, fixed epoch anchor) |
| `SeasonClimate` | Temperature swing + snow threshold (`SNOW_THRESHOLD = 0.15F`) |
| `Seasons` | Global entry: `current()`, `temperatureSwing()`, `shouldForcePrecipitation()`, `setClock()` |

**Vanilla hooks (patches):**

- `Biome` — seasonal temperature swing for rain/snow choice
- `ServerLevel` — winter holds rain so precipitation can place snow

Design: wall-clock (not in-game days); downtime-safe; API usable without ecology.

### Ecology — `dev.mintychochip.ecology` (API + server)

**API** (`paper-api/src/main/java/dev/mintychochip/ecology/`): pure model + config + math

| Class | Role |
|-------|------|
| `Ecology` | Public entry: `load`, `settings`, `catalog`, `suitability`, … |
| `SuitabilityEngine` | Tier × humidity trapezoid |
| `WaterProximity` | Distance-weighted water bonus (probe interface) |
| `EcologyConfig` | Load/write `config/mintychochip/ecology.json` |
| `EcologyDefaults` / `EcologySettings` / `CropCatalog` / `CropProfile` / `BiomeCategories` / `ClimateSample` | Config model |

**Server** (`paper-server/src/main/java/dev/mintychochip/ecology/`): NMS only

| Class | Role |
|-------|------|
| `CropEcology` | World façade: `shouldGrow`, `popIfUnsuitable`, climate sample from `ServerLevel` |

**Runtime config:** `config/mintychochip/ecology.json` (server root; defaults written on first load).

**Vanilla growth hooks** (search `mintychochip` in `patches/sources/net/minecraft`):

- Growth: `CropBlock`, `StemBlock`, `PitcherCropBlock`, `CactusBlock`, `CocoaBlock`, `NetherWartBlock`, `SaplingBlock`, `SugarCaneBlock`, `SweetBerryBushBlock`, …
- Placement: `BlockItem` — `popIfUnsuitable` after place

Pattern:

1. Random tick → `CropEcology.shouldGrow(...)` (probabilistic)
2. Bone meal / forced grow → `CropEcology.allowsForcedGrowth(...)`
3. Place → `CropEcology.popIfUnsuitable(...)`

Unknown blocks (no catalog profile) pass through.

### Genetics — `dev.mintychochip.genetics` (API core)

**API** (`paper-api/src/main/java/dev/mintychochip/genetics/`): pure recombination engine (no NMS).

| Area | Role |
|------|------|
| `model` | `Sex`, `Genome`, `Allele` (DNA sequence), `LocusDefinition`, `InheritanceMode` |
| `dna` | `DnaSequence`, point mutation / indels via `MutationSettings` |
| `engine` | `MeiosisEngine`, `BreedingEngine` (opposite-sex cross → child genome) |
| `dto` | `GenotypeSnapshot`, `PhenotypeSnapshot`, `PhenotypeDecoder` (incl. calico) |
| `Genetics` | Public façade |

Design: meiosis + linkage + X/Y/maternal inheritance + germline point mutations — not Punnett tables.

**Server** (`paper-server/src/main/java/dev/mintychochip/genetics/`):

| Class | Role |
|-------|------|
| `AnimalGenetics` | Genome cache + NBT (`MintyGenome`), opposite-sex `allowsMate`, `onBreed` → `BreedingEngine` |

**Vanilla hooks:** `Animal` save/load, `canMate`, `spawnChildFromBreeding` (see `// mintychochip` + `Animal.java.patch`).

**Tests:**
```bash
./gradlew :paper-api:test --tests 'dev.mintychochip.genetics.*'
./gradlew :paper-server:test --tests 'dev.mintychochip.genetics.*'
```

Spec: `docs/superpowers/specs/2026-08-07-genetics-core-design.md`

### Custom blocks — `dev.mintychochip.customblock` (API definitions)

**API** (`paper-api/src/main/java/dev/mintychochip/customblock/`): pure definitions + catalog (no NMS).

| Class | Role |
|-------|------|
| `BlockHostType` | `CHORUS`, `MUSHROOM`, `TRIPWIRE` (baked), `PACKET` (client item displays) |
| `HostSpec` + `*HostSpec` | Sealed host params (state index, mushroom variant, packet transforms) |
| `CustomBlockDefinition` | Keyed definition: host + item + display + {@link BlockFeel} |
| `BlockFeel` | Hardness, blast resistance, preferred tool (often {@code emulate(Material)}) |
| `CustomBlockCatalog` / `CustomBlocks` | Register / lookup / `of(Block\|ItemStack)` / `createItemStack` |
| `CustomBlockItemTags` | PDC stamp for held form (`mintychochip:custom_block`) |
| `CustomBlockLookup` | World placement identity (server installs impl) |

**Additive Bukkit surface** (does not change `getType()`):

- `Block#getCustomKey()` / `getCustomBlock()` / `isCustomBlock()`
- `ItemStack#getCustomKey()` / `getCustomBlock()` / `isCustomBlockItem()`

**Server** (`paper-server/.../customblock/`):

| Class | Role |
|-------|------|
| `MemoryCustomBlockLookup` | In-memory `location → key` (not persisted yet) |
| `CustomBlockBootstrap` | Installs lookup + registers listeners at `POSTWORLD` |
| `CustomBlockListener` | Bukkit place / break / piston / explode |
| `CustomBlockLifecycle` | Carrier apply, tool-gated drops, identity clear |
| `CustomBlockMining` | Dig progress + blast resistance (NMS helpers for thin hooks) |
| `CustomBlockPlacement` | Host → carrier material (`GLASS`, `CHORUS_PLANT`, mushroom, `TRIPWIRE`) |
| `MintyInternalPlugin` | Minimal enabled plugin handle for `registerEvents` |
| `pack/*` | Auto resource-pack host + join delivery (`resource-pack.json`) |

Design: identity is `NamespacedKey`; host chooses how the client sees it. `getType()` remains vanilla carrier/base. Place/break use normal Bukkit events; custom drops via stamped item. Pack auto-served on join (HTTP port 8765 by default).

**Tests:** `./gradlew :paper-api:test --tests 'dev.mintychochip.customblock.*'`

Spec: `docs/superpowers/specs/2026-08-07-custom-block-definition-design.md`

### Provenance — `dev.mintychochip.provenance` (item identity + dupe detect)

**API** (`paper-api/.../provenance/`): pure enums/DTOs (`ProvenanceSource`, `LineageNode`, `ProvenanceEvent`, …).

**Server** (`paper-server/.../provenance/`):

| Class | Role |
|-------|------|
| `ItemProvenance` | Birth / split / merge / craft / death / claim / explain / simulateDupe |
| `StackStamp` | UUID + parents in `CUSTOM_DATA` → `MintyProvenance` |
| `LiveIndex` / `LineageStore` / `AuditLog` | Census, history walk, ring buffer |
| `ProvenanceBootstrap` | `/provenance` admin command |

**Vanilla hooks:** split/stackability; block place→persistent placement (SavedData); piston move; falling/enderman carry (+ entity NBT); block drop BLOCK_RECOVER/DROP; craft/crafter; furnace/campfire; smithing/stonecutter; trade; piglin barter; archaeology; `/give`; creative; loot/fishing; **all container merges** (player inv, hopper, Slot, menu, **copper golem** `TransportItemsBetweenContainers`); item entity pickup/despawn/merge. Audit JSONL under world `mintychochip/`.

**Tests:**
```bash
./gradlew :paper-server:test --tests 'org.bukkit.support.suite.ProvenanceTestSuite'
```

Spec: `docs/superpowers/specs/2026-08-07-item-provenance-design.md`

---

## Build, test, run

Requires **JDK 25** (see `CONTRIBUTING.md` / Gradle toolchains).

```bash
./gradlew applyPatches          # needed for vanilla tree / existing hooks
./gradlew :paper-api:test
./gradlew :paper-server:test --tests 'dev.mintychochip.*'
./gradlew createPaperclipJar
```

```bash
./gradlew :paper-api:test --tests 'dev.mintychochip.season.*'
./gradlew :paper-api:test --tests 'dev.mintychochip.ecology.*'
./gradlew :paper-api:test --tests 'dev.mintychochip.customblock.*'
./gradlew :paper-server:test --tests 'dev.mintychochip.*'
```

Local server workdir is typically `run/`. Do not commit worlds, heap dumps, or logs.

**Note:** Editing only `dev.mintychochip` under `paper-api` or `paper-server/src/main` does **not** require `rebuildPatches`. Only vanilla (`src/minecraft`) edits do.

---

## Conventions for agents

1. **Namespace** — `dev.mintychochip.<area>` only.
2. **Server by default; API only when exportable** — new features start in `paper-server`. Promote to `paper-api` only when plugins/external code must compile against the types; keep that surface minimal and NMS-free.
3. **No patches for our packages** — main/API sources only.
4. **Thin vanilla hooks** — logic in mintychochip classes; NMS only calls them; mark with `// mintychochip`.
5. **Patch hygiene** — after any `src/minecraft` edit: `fixupSourcePatches` + `rebuildPatches`.
6. **Do not** rewrite unrelated Paper feature patches while doing mintychochip work.
7. **Config** — knobs under `config/mintychochip/`; defaults in code must match written JSON.
8. **Tests** — seasons in paper-api tests; ecology/server logic in paper-server tests.
9. **Upstream sync** — on Paper rebases, re-apply only the thin vanilla hooks carefully.
10. **Scope** — implement what the task needs; no drive-by Paper/Moonrise refactors.

---

## Search cheatsheet

```bash
# Vanilla hooks only
rg -n 'mintychochip' paper-server/patches/sources/net

# Our sources (preferred locations)
find paper-api/src paper-server/src/main/java/dev -type f -name '*.java' 2>/dev/null
find paper-server/src/test/java/dev -type f -name '*.java' 2>/dev/null

# Accidental patch placement (should be empty after cleanup)
ls paper-server/patches/sources/dev/mintychochip 2>/dev/null
ls paper-server/src/minecraft/java/dev/mintychochip 2>/dev/null
```

---

## What not to touch casually

| Path | Why |
|------|-----|
| `paper-server/patches/features/*` | Large upstream opt patches; high conflict cost |
| `build-data/`, mappings | Upstream remapping pipeline |
| `build/`, `paper-*/build/` | Build outputs |
| `run/world*`, `*.hprof`, logs | Runtime / crash artifacts |
| Unrelated CraftBukkit / Paper API churn | Out of scope |

---

## Quick decision guide

```
Default for a new feature?
  → paper-server/src/main/java/dev/mintychochip/<feature>/
     (server-only; plugins cannot import these types)

Do plugins / external code need to import or call this?
  → YES: put the *minimal* NMS-free surface in
       paper-api/src/main/java/dev/mintychochip/<feature>/
     keep NMS implementation on the server
  → NO: stay entirely on paper-server

Needs net.minecraft.* (ServerLevel, blocks, biomes, …)?
  → paper-server only (never paper-api)

Change vanilla behavior?
  → thin hook in paper-server/src/minecraft/java/net/minecraft/...
  → call dev.mintychochip.* (server or API)
  → fixupSourcePatches + rebuildPatches

Runtime-tunable knobs?
  → config/mintychochip/<name>.json + loader on the server
    (API only if plugins must read the same knobs)
```

Mirror the **season (API, plugin-visible) + ecology (server-only) + thin vanilla hooks** split.
