package dev.mintychochip.customblock.pack;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds, hosts, and delivers the mintychochip custom-block resource pack.
 */
public final class CustomBlockPackService implements AutoCloseable {

    /** Stable pack id so clients can replace the same pack on rejoin/reload. */
    public static final UUID PACK_ID = UUID.nameUUIDFromBytes("mintychochip:custom-blocks".getBytes(StandardCharsets.UTF_8));

    private final Logger logger;
    private final CustomBlockPackSettings settings;
    private final CustomBlockPackArchive archive;
    private final URI publicPackUri;
    private final @Nullable CustomBlockPackHttpServer http;

    private CustomBlockPackService(
        final Logger logger,
        final CustomBlockPackSettings settings,
        final CustomBlockPackArchive archive,
        final URI publicPackUri,
        final @Nullable CustomBlockPackHttpServer http
    ) {
        this.logger = logger;
        this.settings = settings;
        this.archive = archive;
        this.publicPackUri = publicPackUri;
        this.http = http;
    }

    public static @Nullable CustomBlockPackService start(
        @NotNull final Path serverRoot,
        @NotNull final Logger logger
    ) {
        Objects.requireNonNull(serverRoot, "serverRoot");
        Objects.requireNonNull(logger, "logger");
        try {
            final CustomBlockPackSettings settings = CustomBlockPackSettings.loadOrCreate(serverRoot);
            if (!settings.enabled()) {
                logger.info("[mintychochip] resource pack hosting disabled (config/mintychochip/resource-pack.json)");
                return null;
            }

            final Path fsPack = firstExisting(
                serverRoot.resolve("resourcepacks/mintychochip"),
                serverRoot.resolve("run/resourcepacks/mintychochip")
            );
            final CustomBlockPackArchive archive = CustomBlockPackArchive.build(fsPack);
            archive.writeTo(serverRoot.resolve("mintychochip/pack.zip"));

            final CustomBlockPackHttpServer http = CustomBlockPackHttpServer.start(settings.port(), () -> archive);
            final URI publicUri = resolvePublicUri(settings, http.port(), archive.sha1Hex(), logger);

            logger.info("[mintychochip] resource pack hosting on port " + http.port()
                + " sha1=" + archive.sha1Hex()
                + " size=" + archive.size()
                + " url=" + publicUri);

            return new CustomBlockPackService(logger, settings, archive, publicUri, http);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, "[mintychochip] failed to start resource pack host", e);
            return null;
        }
    }

    public CustomBlockPackSettings settings() {
        return this.settings;
    }

    public CustomBlockPackArchive archive() {
        return this.archive;
    }

    public URI publicPackUri() {
        return this.publicPackUri;
    }

    /** Send the pack to a player (Adventure request). */
    public void sendTo(@NotNull final Player player) {
        Objects.requireNonNull(player, "player");
        try {
            final ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(
                PACK_ID,
                this.publicPackUri,
                this.archive.sha1Hex()
            );
            final ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(info)
                .required(this.settings.force())
                .replace(true)
                .prompt(Component.text("mintychochip custom blocks", NamedTextColor.GOLD))
                .build();
            player.sendResourcePacks(request);
        } catch (final Exception e) {
            this.logger.log(Level.WARNING, "[mintychochip] failed to send resource pack to " + player.getName(), e);
        }
    }

    public void sendToAllOnline() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            sendTo(player);
        }
    }

    private static URI resolvePublicUri(
        final CustomBlockPackSettings settings,
        final int boundPort,
        final String sha1Hex,
        final Logger logger
    ) {
        final String base;
        if (settings.publicUrl() != null) {
            base = trimTrailingSlash(settings.publicUrl());
        } else {
            final String host = detectPublicHost(logger);
            base = "http://" + host + ":" + boundPort;
        }
        return URI.create(base + "/pack/" + sha1Hex + ".zip");
    }

    private static String detectPublicHost(final Logger logger) {
        // Prefer server-ip from properties when set (LAN / public bind).
        try {
            final String serverIp = Bukkit.getIp();
            if (serverIp != null && !serverIp.isBlank() && !isWildcard(serverIp)) {
                return bracketIfNeeded(serverIp.trim());
            }
        } catch (final Throwable ignored) {
            // Bukkit may not be fully ready in tests
        }
        final String nonLoopback = firstNonLoopback();
        if (nonLoopback != null) {
            logger.info("[mintychochip] resource pack public host auto-detected: " + nonLoopback
                + " (set config/mintychochip/resource-pack.json publicUrl if clients cannot reach this)");
            return bracketIfNeeded(nonLoopback);
        }
        logger.warning("[mintychochip] no non-loopback address found; using 127.0.0.1 for pack URL");
        return "127.0.0.1";
    }

    private static @Nullable String firstNonLoopback() {
        try {
            final Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                final NetworkInterface nif = ifaces.nextElement();
                if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) {
                    continue;
                }
                final Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    final InetAddress addr = addrs.nextElement();
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) {
                        continue;
                    }
                    // Prefer IPv4 for client pack URLs
                    if (addr.getAddress().length == 4) {
                        return addr.getHostAddress();
                    }
                }
            }
            // second pass: any non-loopback including IPv6
            final Enumeration<NetworkInterface> ifaces2 = NetworkInterface.getNetworkInterfaces();
            while (ifaces2.hasMoreElements()) {
                final NetworkInterface nif = ifaces2.nextElement();
                if (!nif.isUp() || nif.isLoopback()) {
                    continue;
                }
                final Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    final InetAddress addr = addrs.nextElement();
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }

    private static boolean isWildcard(final String host) {
        return "0.0.0.0".equals(host) || "::".equals(host) || "*".equals(host);
    }

    private static String bracketIfNeeded(final String host) {
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            return "[" + host + "]";
        }
        return host;
    }

    private static String trimTrailingSlash(final String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static @Nullable Path firstExisting(final Path... paths) {
        for (final Path p : paths) {
            if (p != null && java.nio.file.Files.isDirectory(p)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void close() {
        if (this.http != null) {
            this.http.close();
        }
    }
}
