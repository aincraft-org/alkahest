package io.papermc.paper.registry;

/**
 * The kind of backend backing a Paper registry. Every public {@link RegistryKey}
 * must map to exactly one backend. This is server-internal metadata; plugins
 * observe lifecycle behavior through the existing registry event APIs instead.
 */
public enum RegistryBackendKind {
    NATIVE_STATIC,
    NATIVE_DATA,
    NATIVE_RELOADABLE,
    CATALOG,
    MERGED
}
