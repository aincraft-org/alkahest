package dev.mintychochip.genetics.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mintychochip.genetics.model.Allele;
import dev.mintychochip.genetics.model.GeneCopy;
import dev.mintychochip.genetics.model.Genome;
import dev.mintychochip.genetics.model.LocusId;
import dev.mintychochip.genetics.model.Sex;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Pure JSON encode/decode for {@link Genome}. NMS-free; used by server persistence
 * and tests for round-trip identity.
 */
public final class GenomeCodec {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private GenomeCodec() {
    }

    public static String encode(final Genome genome) {
        Objects.requireNonNull(genome, "genome");
        final JsonObject root = new JsonObject();
        root.addProperty("sex", genome.sex().name());
        final JsonObject genes = new JsonObject();
        for (final Map.Entry<LocusId, GeneCopy> entry : genome.genes().entrySet()) {
            genes.add(entry.getKey().key(), encodeCopy(entry.getValue()));
        }
        root.add("genes", genes);
        return GSON.toJson(root);
    }

    public static Genome decode(final String json) {
        Objects.requireNonNull(json, "json");
        final JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        final Sex sex = Sex.valueOf(root.get("sex").getAsString());
        final Genome.Builder builder = Genome.builder(sex);
        final JsonObject genes = root.getAsJsonObject("genes");
        if (genes != null) {
            for (final String key : genes.keySet()) {
                builder.put(LocusId.of(key), decodeCopy(genes.getAsJsonObject(key)));
            }
        }
        return builder.build();
    }

    /**
     * Structural equality for round-trip tests (sex, locus keys, sequences, labels).
     */
    public static boolean deepEquals(final Genome a, final Genome b) {
        if (a.sex() != b.sex()) {
            return false;
        }
        if (a.genes().size() != b.genes().size()) {
            return false;
        }
        for (final Map.Entry<LocusId, GeneCopy> entry : a.genes().entrySet()) {
            final GeneCopy other = b.getOrNull(entry.getKey());
            if (other == null || !copyEquals(entry.getValue(), other)) {
                return false;
            }
        }
        return true;
    }

    private static boolean copyEquals(final GeneCopy a, final GeneCopy b) {
        if (!alleleEquals(a.alleleA(), b.alleleA())) {
            return false;
        }
        if (a.isHemizygous() != b.isHemizygous()) {
            return false;
        }
        if (a.isHemizygous()) {
            return true;
        }
        return alleleEquals(a.alleleB(), b.alleleB());
    }

    private static boolean alleleEquals(final Allele a, final @Nullable Allele b) {
        if (b == null) {
            return false;
        }
        if (!a.sequence().equals(b.sequence())) {
            return false;
        }
        final String la = a.label();
        final String lb = b.label();
        if (la == null) {
            return lb == null;
        }
        return la.equals(lb);
    }

    private static JsonObject encodeCopy(final GeneCopy copy) {
        final JsonObject obj = new JsonObject();
        obj.add("a", encodeAllele(copy.alleleA()));
        if (copy.isDiploid()) {
            obj.add("b", encodeAllele(copy.alleleB()));
        }
        return obj;
    }

    private static GeneCopy decodeCopy(final JsonObject obj) {
        final Allele a = decodeAllele(obj.getAsJsonObject("a"));
        if (!obj.has("b") || obj.get("b").isJsonNull()) {
            return GeneCopy.hemizygous(a);
        }
        return GeneCopy.diploid(a, decodeAllele(obj.getAsJsonObject("b")));
    }

    private static JsonObject encodeAllele(final Allele allele) {
        final JsonObject obj = new JsonObject();
        obj.addProperty("seq", allele.sequence().asString());
        if (allele.label() != null) {
            obj.addProperty("label", allele.label());
        }
        return obj;
    }

    private static Allele decodeAllele(final JsonObject obj) {
        final String seq = obj.get("seq").getAsString();
        final String label = obj.has("label") ? obj.get("label").getAsString() : null;
        return Allele.of(seq, label);
    }
}
