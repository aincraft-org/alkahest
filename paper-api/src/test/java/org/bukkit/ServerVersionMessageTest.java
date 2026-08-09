package org.bukkit;

import org.bukkit.support.TestServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerVersionMessageTest {
    @Test
    void versionMessageUsesReleaseVersion() {
        TestServer.setup();

        assertTrue(Bukkit.getVersionMessage().contains("version 2026.08.08.1"));
    }
}
