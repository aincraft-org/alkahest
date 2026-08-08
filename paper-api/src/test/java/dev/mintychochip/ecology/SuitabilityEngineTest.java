package dev.mintychochip.ecology;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class SuitabilityEngineTest {

    private static CropProfile carrots() {
        return new CropProfile(
            "minecraft:carrots",
            Set.of("plains", "forest", "taiga"),
            Set.of("mountain"),
            0.20,
            0.85
        );
    }

    @Test
    public void nativeBiomeFullTier() {
        assertEquals(1.0, SuitabilityEngine.tier(carrots(), "plains"), 1e-9);
    }

    @Test
    public void tolerantBiomeReducedTier() {
        assertEquals(EcologyConfig.get().tolerantTier(), SuitabilityEngine.tier(carrots(), "mountain"), 1e-9);
    }

    @Test
    public void alienBiomeZeroTier() {
        assertEquals(0.0, SuitabilityEngine.tier(carrots(), "desert"), 1e-9);
    }

    @Test
    public void humidityInsideRangeSuitableInNative() {
        final ClimateSample cell = new ClimateSample(0.5, "plains");
        assertTrue(SuitabilityEngine.isSuitable(carrots(), cell));
        assertEquals(1.0, SuitabilityEngine.suitability(carrots(), cell), 1e-9);
    }

    @Test
    public void humidityOutsideRangeBlocksGrowth() {
        final ClimateSample arid = new ClimateSample(0.0, "plains");
        assertFalse(SuitabilityEngine.isSuitable(carrots(), arid));
        assertEquals(0.0, SuitabilityEngine.acceptProbability(carrots(), arid), 1e-9);
    }

    @Test
    public void alienBiomeBlocksEvenWithGoodHumidity() {
        final ClimateSample desert = new ClimateSample(0.5, "desert");
        assertFalse(SuitabilityEngine.isSuitable(carrots(), desert));
    }

    @Test
    public void catalogHasPlantableCrops() {
        final CropCatalog catalog = CropCatalog.builtin();
        assertTrue(catalog.forBlock("minecraft:carrots") != null);
        assertTrue(catalog.forBlock("minecraft:potatoes") != null);
        assertTrue(catalog.forBlock("minecraft:wheat") != null);
        assertTrue(catalog.forBlock("minecraft:pumpkin_stem") != null);
        assertTrue(catalog.forBlock("minecraft:oak_sapling") != null);
        assertTrue(catalog.forBlock("minecraft:cactus") != null);
    }
}
