package dev.mintychochip.genetics.dna;

import java.util.Map;

/**
 * Standard genetic code (DNA triplets → amino acids). {@code '*'} is stop.
 */
public final class CodonTable {

    public static final char STOP = '*';

    private static final Map<String, Character> TABLE = Map.ofEntries(
        Map.entry("TTT", 'F'), Map.entry("TTC", 'F'),
        Map.entry("TTA", 'L'), Map.entry("TTG", 'L'),
        Map.entry("TCT", 'S'), Map.entry("TCC", 'S'), Map.entry("TCA", 'S'), Map.entry("TCG", 'S'),
        Map.entry("TAT", 'Y'), Map.entry("TAC", 'Y'),
        Map.entry("TAA", STOP), Map.entry("TAG", STOP), Map.entry("TGA", STOP),
        Map.entry("TGT", 'C'), Map.entry("TGC", 'C'),
        Map.entry("TGG", 'W'),
        Map.entry("CTT", 'L'), Map.entry("CTC", 'L'), Map.entry("CTA", 'L'), Map.entry("CTG", 'L'),
        Map.entry("CCT", 'P'), Map.entry("CCC", 'P'), Map.entry("CCA", 'P'), Map.entry("CCG", 'P'),
        Map.entry("CAT", 'H'), Map.entry("CAC", 'H'),
        Map.entry("CAA", 'Q'), Map.entry("CAG", 'Q'),
        Map.entry("CGT", 'R'), Map.entry("CGC", 'R'), Map.entry("CGA", 'R'), Map.entry("CGG", 'R'),
        Map.entry("ATT", 'I'), Map.entry("ATC", 'I'), Map.entry("ATA", 'I'),
        Map.entry("ATG", 'M'),
        Map.entry("ACT", 'T'), Map.entry("ACC", 'T'), Map.entry("ACA", 'T'), Map.entry("ACG", 'T'),
        Map.entry("AAT", 'N'), Map.entry("AAC", 'N'),
        Map.entry("AAA", 'K'), Map.entry("AAG", 'K'),
        Map.entry("AGT", 'S'), Map.entry("AGC", 'S'),
        Map.entry("AGA", 'R'), Map.entry("AGG", 'R'),
        Map.entry("GTT", 'V'), Map.entry("GTC", 'V'), Map.entry("GTA", 'V'), Map.entry("GTG", 'V'),
        Map.entry("GCT", 'A'), Map.entry("GCC", 'A'), Map.entry("GCA", 'A'), Map.entry("GCG", 'A'),
        Map.entry("GAT", 'D'), Map.entry("GAC", 'D'),
        Map.entry("GAA", 'E'), Map.entry("GAG", 'E'),
        Map.entry("GGT", 'G'), Map.entry("GGC", 'G'), Map.entry("GGA", 'G'), Map.entry("GGG", 'G')
    );

    private CodonTable() {
    }

    public static char translate(final String codon) {
        final Character aminoAcid = TABLE.get(codon.toUpperCase());
        if (aminoAcid == null) {
            throw new IllegalArgumentException("Not a valid codon: " + codon);
        }
        return aminoAcid;
    }

    public static boolean isStopCodon(final String codon) {
        return translate(codon) == STOP;
    }
}
