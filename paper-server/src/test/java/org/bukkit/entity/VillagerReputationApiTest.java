package org.bukkit.entity;

import java.util.Map;
import java.util.UUID;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Normal
class VillagerReputationApiTest {
    @Test
    void reputationMethodsRemainAvailable() throws NoSuchMethodException {
        assertNotNull(Villager.class.getMethod("getReputation", UUID.class));
        assertNotNull(Villager.class.getMethod("getReputations"));
        assertNotNull(Villager.class.getMethod("setReputation", UUID.class, com.destroystokyo.paper.entity.villager.Reputation.class));
        assertNotNull(Villager.class.getMethod("setReputations", Map.class));
        assertNotNull(Villager.class.getMethod("clearReputations"));
        assertNotNull(Villager.class.getMethod("updateDemand"));
        assertNotNull(Villager.class.getMethod("restock"));
    }
}
