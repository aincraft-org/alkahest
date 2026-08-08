package dev.mintychochip.customblock.pack;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lightweight HTTP host for the custom-block resource pack ZIP.
 *
 * <p>Route: {@code GET /pack/&lt;40 hex sha1&gt;.zip}
 */
public final class CustomBlockPackHttpServer implements AutoCloseable {

    private static final Pattern ROUTE = Pattern.compile("^/pack/([0-9a-f]{40})\\.zip$");
    private static final String CONTENT_TYPE = "application/zip";

    private final HttpServer server;
    private final Supplier<CustomBlockPackArchive> archive;

    private CustomBlockPackHttpServer(final HttpServer server, final Supplier<CustomBlockPackArchive> archive) {
        this.server = server;
        this.archive = archive;
    }

    public static @NotNull CustomBlockPackHttpServer start(
        final int preferredPort,
        @NotNull final Supplier<CustomBlockPackArchive> archive
    ) throws IOException {
        Objects.requireNonNull(archive, "archive");
        IOException last = null;
        // Prefer configured port, then a few fallbacks if another process holds it.
        final int[] candidates = {
            preferredPort,
            preferredPort + 1,
            preferredPort + 2,
            8877,
            0 // ephemeral
        };
        for (final int port : candidates) {
            try {
                final HttpServer http = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
                final CustomBlockPackHttpServer wrapper = new CustomBlockPackHttpServer(http, archive);
                http.createContext("/", wrapper::handle);
                http.setExecutor(Executors.newCachedThreadPool(r -> {
                    final Thread t = new Thread(r, "mintychochip-pack-http");
                    t.setDaemon(true);
                    return t;
                }));
                http.start();
                return wrapper;
            } catch (final IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("failed to bind pack HTTP port");
    }

    public int port() {
        return this.server.getAddress().getPort();
    }

    private void handle(final HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())
                && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            final Matcher matcher = ROUTE.matcher(exchange.getRequestURI().getPath());
            if (!matcher.matches()) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }
            final CustomBlockPackArchive active = this.archive.get();
            if (active == null || !active.sha1Hex().equals(matcher.group(1))) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            final byte[] bytes = active.zipBytes();
            final Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", CONTENT_TYPE);
            headers.set("Content-Length", String.valueOf(bytes.length));
            headers.set("Cache-Control", "public, max-age=3600");
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        this.server.stop(0);
    }
}
