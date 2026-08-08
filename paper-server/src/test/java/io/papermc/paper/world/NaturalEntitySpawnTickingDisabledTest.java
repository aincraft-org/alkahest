package io.papermc.paper.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerChunkCache;
import org.bukkit.support.environment.Normal;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Natural entity spawn ticking is hard-disabled in this fork.
 * Drives the shipped {@link ServerChunkCache} bytecode (not a reimplementation):
 * the flag is false and the chunk-tick path must not call NaturalSpawner spawn APIs.
 */
@Normal
public class NaturalEntitySpawnTickingDisabledTest {

    private static final String NATURAL_SPAWNER = "net/minecraft/world/level/NaturalSpawner";
    private static final String SERVER_CHUNK_CACHE = "net/minecraft/server/level/ServerChunkCache";

    @Test
    public void naturalEntitySpawnTickingFlagIsDisabled() {
        assertFalse(
            ServerChunkCache.NATURAL_ENTITY_SPAWN_TICKING_ENABLED,
            "NATURAL_ENTITY_SPAWN_TICKING_ENABLED must be false so chunk tick never runs natural spawn"
        );
    }

    @Test
    public void serverChunkCacheDoesNotInvokeNaturalSpawnApis() throws IOException {
        final ClassNode classNode = readClassNode(ServerChunkCache.class);
        final List<String> forbiddenCalls = new ArrayList<>();

        boolean hasTickChunks = false;
        boolean hasTickSpawningChunk = false;
        boolean tickSpawningCallsInhabited = false;
        boolean tickSpawningCallsThunder = false;

        for (final MethodNode method : classNode.methods) {
            if ("tickChunks".equals(method.name)) {
                hasTickChunks = true;
            }
            if ("tickSpawningChunk".equals(method.name)) {
                hasTickSpawningChunk = true;
            }

            for (final AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) {
                    continue;
                }
                if (NATURAL_SPAWNER.equals(call.owner)) {
                    if ("spawnForChunk".equals(call.name)
                        || "createState".equals(call.name)
                        || "getFilteredSpawningCategories".equals(call.name)) {
                        forbiddenCalls.add(method.name + " -> NaturalSpawner." + call.name + call.desc);
                    }
                }
                if ("tickSpawningChunk".equals(method.name)) {
                    if ("incrementInhabitedTime".equals(call.name)) {
                        tickSpawningCallsInhabited = true;
                    }
                    if ("tickThunder".equals(call.name)) {
                        tickSpawningCallsThunder = true;
                    }
                }
            }
        }

        assertTrue(hasTickChunks, "ServerChunkCache must still define tickChunks");
        assertTrue(hasTickSpawningChunk, "ServerChunkCache must still define tickSpawningChunk for inhabited/thunder work");
        assertTrue(tickSpawningCallsInhabited, "tickSpawningChunk must still increment inhabited time");
        assertTrue(tickSpawningCallsThunder, "tickSpawningChunk must still tick thunder");

        if (!forbiddenCalls.isEmpty()) {
            fail(
                "ServerChunkCache must not call natural spawn bookkeeping/spawn APIs, but found:\n"
                    + String.join("\n", forbiddenCalls)
            );
        }
    }

    @Test
    public void naturalSpawnTickingFlagFieldIsPresentAndFalseInBytecode() throws IOException {
        final ClassNode classNode = readClassNode(ServerChunkCache.class);
        FieldNode flag = null;
        for (final FieldNode field : classNode.fields) {
            if ("NATURAL_ENTITY_SPAWN_TICKING_ENABLED".equals(field.name)) {
                flag = field;
                break;
            }
        }
        if (flag == null) {
            fail("Missing NATURAL_ENTITY_SPAWN_TICKING_ENABLED on ServerChunkCache");
        }
        assertTrue((flag.access & Opcodes.ACC_STATIC) != 0, "flag must be static");
        assertTrue((flag.access & Opcodes.ACC_FINAL) != 0, "flag must be final");
        assertFalse(Boolean.TRUE.equals(flag.value), "bytecode constant value must not be true");
        // static final boolean false is stored as Integer 0 or null with initializer; accept either non-true
        if (flag.value instanceof Integer i) {
            assertTrue(i == 0, "flag constant must be false (0), was " + i);
        }
    }

    private static ClassNode readClassNode(final Class<?> type) throws IOException {
        final String resource = type.getName().replace('.', '/') + ".class";
        try (InputStream in = type.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                fail("Could not load bytecode for " + type.getName());
            }
            final ClassReader reader = new ClassReader(in);
            final ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_DEBUG);
            return node;
        }
    }
}
