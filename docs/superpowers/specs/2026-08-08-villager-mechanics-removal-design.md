# Villager mechanics reduction (merchant-only villagers)

**Date:** 2026-08-08
**Status:** approved direction; written specification awaiting user review
**Scope:** vanilla villager AI, village defense, and village siege behavior
**Compatibility boundary:** preserve the villager trading economy and public Bukkit/Paper merchant APIs

## Goal

Reduce villagers from autonomous village simulators to persistent merchants. Villagers keep their entity identity, professions, offers, player trading, restocking, demand, experience, and leveling. They stop creating and maintaining village simulation state through workstations, beds, bells, social exchange, breeding, gossip, raids, and golem defense.

No implementation has started. This document records the approved removal boundary before an implementation plan is written.

## Current implementation boundary

Villager behavior is vanilla/Paper/CraftBukkit code. There is no existing `dev.mintychochip` villager package or hook to preserve.

The primary behavior graph is built in:

- `paper-server/src/minecraft/java/net/minecraft/world/entity/npc/villager/Villager.java`
- `paper-server/src/minecraft/java/net/minecraft/world/entity/ai/behavior/VillagerGoalPackages.java`

The shared merchant base is `AbstractVillager`. It is also used by the wandering trader, so the base merchant contract must not be reduced as part of this change.

## Behavioral design

### 1. Minimal villager brain

Replace the current schedule matrix with a minimal activity set:

- `CORE`: swimming, door interaction, looking, trading-player following, and generic danger response.
- `IDLE`: basic movement/look behavior plus player-facing trade display behavior.
- `PANIC`: flee from ordinary hostile or damage conditions, without requesting an iron golem.

Remove active villager use of `WORK`, `MEET`, `REST`, `PRE_RAID`, `RAID`, `HIDE`, and autonomous child `PLAY` behavior. The brain must no longer require or acquire `HOME`, `JOB_SITE`, `POTENTIAL_JOB_SITE`, or `MEETING_POINT` memories.

Keep only sensors needed by the retained behaviors. Remove active use of item, bed, baby, secondary-POI, and golem-detection sensors from the villager brain. Built-in registry keys and serialized memory identifiers should not be removed solely for this behavior change; old worlds and datapacks must continue to decode safely.

Retain `VillagerHostilesSensor` and the generic panic path so villagers still flee immediate danger. `VillagerPanicTrigger` must lose its villager-driven golem-spawn call but continue ordinary panic behavior.

### 2. No automatic job-site or work simulation

Villagers no longer:

- acquire job sites;
- assign or reset professions from nearby blocks;
- compete for or yield job sites;
- walk to workstations;
- use workstations;
- harvest farmland;
- apply bonemeal;
- collect items for work or food; or
- fire automatic career-change events.

Existing profession data remains authoritative. A villager loaded with a profession continues to use that profession's trades. An unemployed villager does not acquire a profession merely because a workstation is placed nearby. Plugins and commands may still set a profession explicitly.

Affected behavior sources include `AcquirePoi`, `AssignProfessionFromJobSite`, `ResetProfession`, `YieldJobSite`, `PoiCompetitorScan`, `GoToPotentialJobSite`, `WorkAtPoi`, `WorkAtComposter`, `HarvestFarmland`, `UseBonemeal`, `SecondaryPoiSensor`, and the corresponding entries in `VillagerGoalPackages`.

### 3. Preserve trading and move restocking out of work AI

Player trading remains unchanged at the merchant boundary:

- `mobInteract` opens the merchant screen;
- offers remain generated from the profession and level;
- trade acquisition, purchase, replenishment, demand, XP, level-up, and merchant events remain available;
- `CraftVillager` and `AbstractVillager` trade APIs remain source-compatible; and
- existing offers and trade XP continue to persist.

Vanilla currently triggers automatic restocking from `WorkAtPoi`. Since work AI is being removed, restocking must be moved to a villager-owned server-side scheduler. The scheduler must reuse `shouldRestock` and `restock` semantics, including the existing maximum cadence and demand catch-up, and must continue to work for villagers that are inactive under Paper entity activation rules. Explicit Bukkit/Paper `restock()` and `updateDemand()` calls remain unchanged.

The scheduler must not require a job site, profession-specific work package, or workstation.

### 4. No autonomous breeding, farming food loop, or social exchange

Remove autonomous villager-to-villager breeding, bed reservation for children, villager-to-villager item exchange, Hero of the Village gifts, bell socialization, and automatic bed-seeking/sleep scheduling.

Remove the active behavior paths for:

- `VillagerMakeLove`;
- `TradeWithVillager`;
- `GiveGiftToHero`;
- `SocializeAtBell`;
- `ReactToBell`;
- `RingBell`;
- `JumpOnBed`; and
- the corresponding idle, meet, rest, and work entries.

The public age/breeding surface is not removed. `Breedable` methods and offspring construction remain available for explicit plugin-controlled behavior, but vanilla villager AI no longer initiates breeding or supplies the food loop that made autonomous breeding possible. The villager inventory remains available to plugins; villagers simply stop autonomously collecting or distributing items.

Manual `Villager.sleep(Location)`, `wakeup()`, and related explicit APIs remain available. Removing automatic sleep does not prohibit a plugin from deliberately placing a villager in a bed.

### 5. Disable automatic gossip while retaining explicit reputation API compatibility

The public reputation methods on `org.bukkit.entity.Villager` remain intact to avoid an unnecessary plugin ABI break. Explicitly assigned reputations continue to be readable and usable by the existing API path.

Remove automatic reputation sources and propagation:

- trading no longer creates `TRADING` gossip;
- curing no longer creates positive gossip;
- hurting or killing a villager no longer creates negative gossip;
- villagers no longer transfer gossip to one another; and
- gossip no longer decays as part of villager ticking.

Existing serialized gossip data may remain readable and writable for API/data compatibility. The implementation must not add new automatic gossip entries. Existing explicit reputation values may still affect prices through the preserved merchant API path; this keeps plugin-controlled reputation meaningful without retaining village social simulation.

Remove the automatic `ServerLevel.onReputationEvent` call sites that represent the deleted gameplay, and remove villager witness/gossip behavior. Keep only the compatibility pieces needed by explicit API access and zombie-villager state transfer.

### 6. Remove villager raid response and nightly village sieges

Villagers no longer enter pre-raid, raid, hiding, or raid-celebration activities. They do not ring bells or seek hiding places for raids. Generic panic from seeing a hostile remains separate from raid-specific activity.

The general raid system, Bad Omen effect, raid commands, and raider entities are out of scope. Players may still use the general raid system; villagers simply do not provide autonomous raid participation behavior.

Remove the nightly zombie siege custom spawner from both server construction paths:

- `net.minecraft.server.MinecraftServer`; and
- `org.bukkit.craftbukkit.CraftServer`.

`VillageSiege` becomes unreachable and may be deleted as part of implementation after patch regeneration confirms no remaining references.

### 7. Remove village-only iron-golem defense

The approved scope includes village-only golem defense removal:

- villagers never request or spawn iron golems;
- `GolemSensor` is no longer active in villager brains;
- iron golems no longer move back toward villages;
- iron golems no longer use villager-driven village strolling; and
- iron golems no longer use `DefendVillageTargetGoal`.

Keep iron golem entities, player-created golems, melee combat, ordinary hostile targeting, anger, damage response, and non-village presentation behavior. This removes the village relationship rather than deleting the golem entity.

The `GOLEM_DETECTED_RECENTLY` memory and sensor registry identifiers should be retained or migrated conservatively if required for old serialized brains, but no villager runtime path may depend on them.

Affected active sources include `IronGolem`, `GolemRandomStrollInVillageGoal`, `MoveBackToVillageGoal`, `DefendVillageTargetGoal`, `GolemSensor`, `Villager.spawnGolemIfNeeded`, and the golem-related villager brain registrations. The registry-backed sensor and memory entries may remain as unused compatibility registrations where required for old serialized brains, so deleting `GolemSensor` itself is conditional on preserving registry bootstrap compatibility.

## Persistence and migration

Do not remove historical datafixers or registry entries in this change. Existing worlds may contain offers, profession data, XP, restock counters, food level, gossip, and brain memories from the old behavior.

On load or brain refresh:

1. preserve profession, type, offers, XP, restock state, and explicit reputation data;
2. release or clear stale villager POI claims for `HOME`, `JOB_SITE`, `POTENTIAL_JOB_SITE`, and `MEETING_POINT` so removed behaviors do not leave occupied tickets;
3. ignore obsolete behavior state rather than attempting to recreate removed activities; and
4. keep unknown historical NBT fields forward-compatible where the existing codec permits it.

The shared POI manager and POI registrations remain because they are used by non-villager systems and world compatibility paths. This change removes villager use of POIs, not the POI subsystem.

## Compatibility and non-goals

Preserve:

- `VILLAGER` and `ZOMBIE_VILLAGER` entity types;
- villager types and professions;
- profession trade sets and trade registry data;
- direct player trading and merchant packets;
- restocking, demand, XP, leveling, and trade events;
- `CraftVillager` trade, profession, type, sleep, wakeup, zombify, and reputation methods;
- zombie infection and curing, except deleted automatic gossip effects;
- wandering trader behavior through the untouched `AbstractVillager` merchant base; and
- global POI, raid, raider, and iron-golem registries/entities.

Out of scope:

- removing the villager or zombie-villager entities;
- removing all raids or Bad Omen;
- removing all POI support;
- removing the Bukkit/Paper villager API;
- redesigning trade prices or trade tables; and
- unrelated Paper, CraftBukkit, or datafixer refactors.

## Implementation impact map

| Area | Expected change |
|------|-----------------|
| `Villager.java` | Minimal brain, no autonomous POI/social/golem state, preserved merchant logic, independent restock scheduler, stale-POI cleanup |
| `VillagerGoalPackages.java` | Remove work/meet/rest/raid/social packages; retain minimal core/idle/panic packages |
| Villager behavior classes | Delete or make unreachable only after call-site audit; retain shared behaviors used elsewhere |
| Gossip/reputation paths | Remove automatic event generation/transfer/decay; preserve explicit API compatibility |
| `MinecraftServer.java` and `CraftServer.java` | Remove `VillageSiege` from both custom-spawner lists |
| `IronGolem.java` and village-only goals | Remove village movement and `DefendVillageTargetGoal`; preserve ordinary golem combat |
| `CraftVillager.java` and Bukkit API | Preserve public methods; adjust only implementation calls made unreachable by removed behavior |
| Registry/datafix sources | No destructive registry/datafix removal in the first implementation |

All edits to `paper-server/src/minecraft/java/net/minecraft/...` must follow the repository patch workflow: apply the vanilla tree when needed, then run `fixupSourcePatches` and `rebuildPatches`. Our code remains in ordinary source locations; no new `dev.mintychochip` patch package is introduced.

## Verification plan

The implementation is complete only when behavior tests or deterministic integration scenarios demonstrate:

1. an unemployed villager does not acquire a nearby workstation after sufficient ticks;
2. a profession-loaded villager continues opening and completing trades;
3. automatic restocking still occurs without a job site and preserves demand/XP semantics;
4. villagers do not harvest, bonemeal, collect, or share items autonomously;
5. fed villagers do not autonomously breed or reserve a child bed;
6. bells and nearby villagers do not create social exchange, gifts, or gossip;
7. trades, cures, damage, and deaths do not automatically mutate reputation;
8. explicit reputation API operations remain functional;
9. villagers do not enter raid-specific activities or spawn defensive golems;
10. nightly village sieges no longer spawn zombies;
11. existing iron golems do not navigate to or defend villages, while ordinary golem combat remains functional; and
12. existing saved villager data loads with trades, professions, XP, restock state, and explicit reputation intact.

Testing should be added in the repository's existing server test/GameTest conventions. No implementation or test files are part of this specification commit.
