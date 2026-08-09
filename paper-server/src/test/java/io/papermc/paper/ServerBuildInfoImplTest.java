package io.papermc.paper;

import java.util.jar.Manifest;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Normal

class ServerBuildInfoImplTest {
    @Test
    void readsSpecificationVersionFromManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Brand-Id", "mintychochip:alkahest");
        manifest.getMainAttributes().putValue("Brand-Name", "Alkahest");
        manifest.getMainAttributes().putValue("Specification-Version", "2026.08.08.1");
        manifest.getMainAttributes().putValue("Build-Number", "2026080801");
        manifest.getMainAttributes().putValue("Build-Time", "2026-08-08T00:00:00Z");

        ServerBuildInfoImpl info = new ServerBuildInfoImpl(manifest);

        assertEquals("2026.08.08.1", info.releaseVersion());
    }

    @Test
    void fallsBackWhenManifestHasNoReleaseVersion() {
        ServerBuildInfoImpl info = new ServerBuildInfoImpl(new Manifest());

        assertEquals(info.asString(ServerBuildInfo.StringRepresentation.VERSION_SIMPLE), info.releaseVersion());
    }
}
