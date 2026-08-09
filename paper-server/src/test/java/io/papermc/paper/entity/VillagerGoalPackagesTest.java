package io.papermc.paper.entity;

import com.mojang.datafixers.util.Pair;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Normal
class VillagerGoalPackagesTest {
    private static Set<String> behaviorNames(final Iterable<? extends Pair<?, ?>> controls) {
        Set<String> names = new HashSet<>();
        for (Pair<?, ?> control : controls) {
            names.add(control.getSecond().getClass().getSimpleName());
        }
        return names;
    }

    @Test
    void corePackageContainsMerchantAndSafetyBehaviorsOnly() {
        Set<String> names = behaviorNames(VillagerGoalPackages.getCorePackage(0.5F));
        assertTrue(names.contains("LookAndFollowTradingPlayerSink"));
        assertTrue(names.contains("VillagerPanicTrigger"));
        assertFalse(names.contains("AcquirePoi"));
        assertFalse(names.contains("ValidateNearbyPoi"));
        assertFalse(names.contains("PoiCompetitorScan"));
        assertFalse(names.contains("GoToPotentialJobSite"));
        assertFalse(names.contains("YieldJobSite"));
        assertFalse(names.contains("AssignProfessionFromJobSite"));
        assertFalse(names.contains("ResetProfession"));
        assertFalse(names.contains("ReactToBell"));
        assertFalse(names.contains("SetRaidStatus"));
        assertFalse(names.contains("WakeUp"));
        assertFalse(names.contains("GoToWantedItem"));
    }

    @Test
    void idlePackageContainsPlayerTradePresentationOnly() {
        Set<String> names = behaviorNames(VillagerGoalPackages.getIdlePackage(0.5F));
        assertTrue(names.contains("ShowTradesToPlayer"));
        assertTrue(names.contains("SetLookAndInteract"));
        assertFalse(names.contains("TradeWithVillager"));
        assertFalse(names.contains("VillagerMakeLove"));
        assertFalse(names.contains("GiveGiftToHero"));
        assertFalse(names.contains("JumpOnBed"));
        assertFalse(names.contains("InteractWith"));
        assertFalse(names.contains("SocializeAtBell"));
    }

    @Test
    void panicPackageRemainsAvailableForOrdinaryDanger() {
        Set<String> names = behaviorNames(VillagerGoalPackages.getPanicPackage(0.5F));
        assertTrue(names.contains("VillagerCalmDown"));
        assertTrue(names.contains("SetWalkTargetAwayFrom"));
    }
}
