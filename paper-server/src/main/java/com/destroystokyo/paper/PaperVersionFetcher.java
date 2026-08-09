package com.destroystokyo.paper;

import com.destroystokyo.paper.util.VersionFetcher;
import io.papermc.paper.ServerBuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.apache.logging.log4j.LogManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;

import static net.kyori.adventure.text.Component.text;

/**
 * Version fetcher for Alkahest.
 *
 * <p>Alkahest does not participate in Paper's (or any) public download/update channel,
 * and it never contacts PaperMC infrastructure. Version reporting is therefore purely
 * local: the version string shown by {@code /version} comes from the jar manifest,
 * and the startup/version-check messages identify the local release.</p>
 */
@DefaultQualifier(NonNull.class)
public class PaperVersionFetcher implements VersionFetcher {
    private static final ComponentLogger COMPONENT_LOGGER = ComponentLogger.logger(LogManager.getRootLogger().getName());

    private static final ServerBuildInfo BUILD_INFO = ServerBuildInfo.buildInfo();

    @Override
    public long getCacheTime() {
        // No external version source; a trivial cache time keeps /version responsive.
        return 60_000;
    }

    @Override
    public Component getVersionMessage() {
        return text(BUILD_INFO.brandName() + " version " + BUILD_INFO.releaseVersion());
    }

    public static void getUpdateStatusStartupMessage() {
        COMPONENT_LOGGER.info(text("*** " + BUILD_INFO.brandName() + " ***"));
        COMPONENT_LOGGER.info(text("*** Running " + BUILD_INFO.brandName() + " version " + BUILD_INFO.releaseVersion() + " ***"));
        COMPONENT_LOGGER.info(text("*** Updates are distributed privately; no upstream update channel is checked ***"));
    }

    private @Nullable Component getHistory() {
        final VersionHistoryManager.@Nullable VersionData data = VersionHistoryManager.INSTANCE.getVersionData();
        if (data == null) {
            return null;
        }

        final @Nullable String oldVersion = data.getOldVersion();
        if (oldVersion == null) {
            return null;
        }

        return text("Previous version: " + oldVersion, net.kyori.adventure.text.format.NamedTextColor.GRAY, TextDecoration.ITALIC);
    }
}
