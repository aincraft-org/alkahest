package dev.mintychochip.customblock.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime knobs for auto resource-pack hosting / delivery.
 * File: {@code config/mintychochip/resource-pack.json}
 */
public final class CustomBlockPackSettings {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final boolean enabled;
    private final int port;
    /** Explicit public base like {@code http://play.example.com:8765}, or empty for auto. */
    private final @Nullable String publicUrl;
    private final boolean force;
    private final int joinDelayTicks;

    public CustomBlockPackSettings(
        final boolean enabled,
        final int port,
        final @Nullable String publicUrl,
        final boolean force,
        final int joinDelayTicks
    ) {
        this.enabled = enabled;
        this.port = port;
        this.publicUrl = publicUrl == null || publicUrl.isBlank() ? null : publicUrl.trim();
        this.force = force;
        this.joinDelayTicks = Math.max(0, joinDelayTicks);
    }

    public static CustomBlockPackSettings defaults() {
        return new CustomBlockPackSettings(true, 8765, null, true, 20);
    }

    public boolean enabled() {
        return this.enabled;
    }

    public int port() {
        return this.port;
    }

    public @Nullable String publicUrl() {
        return this.publicUrl;
    }

    public boolean force() {
        return this.force;
    }

    public int joinDelayTicks() {
        return this.joinDelayTicks;
    }

    public static @NotNull CustomBlockPackSettings loadOrCreate(@NotNull final Path serverRoot) throws IOException {
        Objects.requireNonNull(serverRoot, "serverRoot");
        final Path file = serverRoot.resolve("config/mintychochip/resource-pack.json");
        if (!Files.isRegularFile(file)) {
            final CustomBlockPackSettings def = defaults();
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(def.toJson(), w);
            }
            return def;
        }
        try (Reader r = Files.newBufferedReader(file)) {
            final JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            return new CustomBlockPackSettings(
                o.has("enabled") ? o.get("enabled").getAsBoolean() : true,
                o.has("port") ? o.get("port").getAsInt() : 8765,
                o.has("publicUrl") && !o.get("publicUrl").isJsonNull()
                    ? o.get("publicUrl").getAsString()
                    : null,
                o.has("force") ? o.get("force").getAsBoolean() : true,
                o.has("joinDelayTicks") ? o.get("joinDelayTicks").getAsInt() : 20
            );
        }
    }

    private JsonObject toJson() {
        final JsonObject o = new JsonObject();
        o.addProperty("enabled", this.enabled);
        o.addProperty("port", this.port);
        o.addProperty("publicUrl", this.publicUrl == null ? "" : this.publicUrl);
        o.addProperty("force", this.force);
        o.addProperty("joinDelayTicks", this.joinDelayTicks);
        return o;
    }
}
