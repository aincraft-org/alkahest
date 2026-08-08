package dev.mintychochip.customblock.pack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds and holds the mintychochip custom-block resource pack ZIP + SHA-1.
 */
public final class CustomBlockPackArchive {

    private final byte[] zipBytes;
    private final byte[] sha1;
    private final String sha1Hex;

    private CustomBlockPackArchive(final byte[] zipBytes, final byte[] sha1) {
        this.zipBytes = zipBytes;
        this.sha1 = sha1;
        this.sha1Hex = HexFormat.of().formatHex(sha1);
    }

    public byte[] zipBytes() {
        return this.zipBytes;
    }

    public byte[] sha1() {
        return this.sha1.clone();
    }

    public String sha1Hex() {
        return this.sha1Hex;
    }

    public int size() {
        return this.zipBytes.length;
    }

    /**
     * Build pack preferring filesystem directory, then classpath {@code mintychochip-pack/}.
     *
     * @param filesystemPackDir e.g. {@code run/resourcepacks/mintychochip} or server-root relative
     */
    public static @NotNull CustomBlockPackArchive build(@Nullable final Path filesystemPackDir) throws IOException {
        if (filesystemPackDir != null && Files.isDirectory(filesystemPackDir)) {
            return fromDirectory(filesystemPackDir);
        }
        return fromClasspath("mintychochip-pack/");
    }

    public static @NotNull CustomBlockPackArchive fromDirectory(@NotNull final Path root) throws IOException {
        Objects.requireNonNull(root, "root");
        final List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                if (!file.getFileName().toString().equals("README.txt")
                    && !file.getFileName().toString().endsWith("_preview.png")) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        files.sort(Comparator.comparing(p -> root.relativize(p).toString().replace('\\', '/')));

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (final Path file : files) {
                final String entryName = root.relativize(file).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(entryName));
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
        return ofBytes(bos.toByteArray());
    }

    public static @NotNull CustomBlockPackArchive fromClasspath(@NotNull final String prefix) throws IOException {
        Objects.requireNonNull(prefix, "prefix");
        final String normalized = prefix.endsWith("/") ? prefix : prefix + "/";
        final ClassLoader cl = CustomBlockPackArchive.class.getClassLoader();
        final URL rootUrl = cl.getResource(normalized + "pack.mcmeta");
        if (rootUrl == null) {
            throw new IOException("classpath pack missing: " + normalized + "pack.mcmeta");
        }

        final String protocol = rootUrl.getProtocol();
        if ("file".equals(protocol)) {
            try {
                // running from exploded classes dir
                final Path meta = Path.of(rootUrl.toURI());
                final Path dir = meta.getParent();
                return fromDirectory(dir);
            } catch (final java.net.URISyntaxException e) {
                throw new IOException("bad classpath pack URI: " + rootUrl, e);
            }
        }

        // jar:file:...!/mintychochip-pack/pack.mcmeta
        if ("jar".equals(protocol)) {
            final String path = rootUrl.getPath(); // file:/.../app.jar!/mintychochip-pack/pack.mcmeta
            final int bang = path.indexOf('!');
            String jarPath = path.substring(0, bang);
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring(5);
            }
            // URL-decode spaces etc.
            jarPath = java.net.URLDecoder.decode(jarPath, StandardCharsets.UTF_8);
            try (JarFile jar = new JarFile(jarPath)) {
                final List<JarEntry> entries = new ArrayList<>();
                final Enumeration<JarEntry> en = jar.entries();
                while (en.hasMoreElements()) {
                    final JarEntry e = en.nextElement();
                    if (e.isDirectory()) {
                        continue;
                    }
                    final String name = e.getName();
                    if (!name.startsWith(normalized)) {
                        continue;
                    }
                    final String relative = name.substring(normalized.length());
                    if (relative.isEmpty()
                        || relative.equals("README.txt")
                        || relative.endsWith("_preview.png")) {
                        continue;
                    }
                    entries.add(e);
                }
                entries.sort(Comparator.comparing(JarEntry::getName));

                final ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (ZipOutputStream zos = new ZipOutputStream(bos)) {
                    for (final JarEntry e : entries) {
                        final String relative = e.getName().substring(normalized.length());
                        zos.putNextEntry(new ZipEntry(relative));
                        try (InputStream in = jar.getInputStream(e)) {
                            in.transferTo(zos);
                        }
                        zos.closeEntry();
                    }
                }
                return ofBytes(bos.toByteArray());
            }
        }

        throw new IOException("unsupported pack resource protocol: " + protocol + " (" + rootUrl + ")");
    }

    public static @NotNull CustomBlockPackArchive ofBytes(final byte[] zipBytes) {
        Objects.requireNonNull(zipBytes, "zipBytes");
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            final byte[] digest = md.digest(zipBytes);
            return new CustomBlockPackArchive(zipBytes, digest);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }

    public void writeTo(@NotNull final Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(this.zipBytes);
        }
    }
}
