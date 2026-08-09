package org.bukkit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

public class MaterialInitializationOrderTest {

    @Test
    public void interfaceConstantsFollowAlreadyInitializedVanillaEnum() {
        assertNotNull(VanillaMaterial.STONE);
        assertNotNull(Material.STONE);
        assertSame(VanillaMaterial.STONE, Material.STONE);
    }
}
