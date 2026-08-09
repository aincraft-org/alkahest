package org.bukkit.potion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.bukkit.FeatureFlag;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PotionTypeRegistryTest {

    @AfterEach
    void clearCatalog() {
        PotionTypeRegistry.reset();
    }

    @Test
    void vanillaCompatibilityHelpersRemainVanillaOnly() {
        assertSame(VanillaPotionType.WATER, PotionType.WATER);
        assertSame(VanillaPotionType.WATER, PotionType.valueOf("WATER"));
        assertEquals(VanillaPotionType.values().length, PotionType.values().length);
        assertTrue(PotionType.WATER.isVanilla());
        assertFalse(PotionType.WATER.isCustom());
    }

    @Test
    void customPotionPublishesMetadataAndFeatures() {
        final PotionType type = PotionTypeRegistry.builder(
                new NamespacedKey("mintychochip", "swift_custom"))
            .effects(List.of())
            .upgradeable(true)
            .extendable(true)
            .maxLevel(3)
            .requiredFeatures(Set.of(FeatureFlag.VANILLA))
            .build();

        assertTrue(type.isCustom());
        assertEquals(List.of(), type.getPotionEffects());
        assertNull(type.getEffectType());
        assertFalse(type.isInstant());
        assertTrue(type.isUpgradeable());
        assertTrue(type.isExtendable());
        assertEquals(3, type.getMaxLevel());
        assertEquals(Set.of(FeatureFlag.VANILLA), type.requiredFeatures());
        assertSame(type, PotionTypeRegistry.get(type.getKey()));
    }

    @Test
    void failedRegistrationLeavesPublishedSnapshot() {
        final PotionType first = PotionTypeRegistry.register(
            new NamespacedKey("mintychochip", "first"), List.of(), false, false, 1, Set.of());
        assertThrows(IllegalArgumentException.class,
            () -> PotionTypeRegistry.builder(NamespacedKey.minecraft("bad")).build());
        assertThrows(IllegalStateException.class,
            () -> PotionTypeRegistry.builder(first.getKey()).build());
        assertEquals(List.of(first), PotionTypeRegistry.values());
    }
}
