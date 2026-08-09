package dev.mintychochip.provenance;

/**
 * Public API surface for item provenance types.
 *
 * <p>Runtime stamping, live census, and NMS hooks live on the server
 * ({@code dev.mintychochip.provenance.ItemProvenance}). This class documents the
 * model only so plugins can depend on the enums/DTOs without NMS.
 */
public final class Provenance {

    private Provenance() {
    }
}
