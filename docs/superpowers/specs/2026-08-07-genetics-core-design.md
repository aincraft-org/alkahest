# Genetics core (recombination + point mutations)

**Date:** 2026-08-07  
**Status:** approved direction (API-first)  
**Package:** `dev.mintychochip.genetics`

## Goal

Server genetics that is **recombination-first**, not Punnett-chart breeding:

- Alleles live on chromosomes (loci with position)
- Meiosis produces gametes with optional crossing-over
- **Point mutations** edit allele DNA sequences each generation
- Sex systems enable real X-linked traits (e.g. calico)
- Genotype / phenotype DTOs are public API (plugins can read / pure-cross)

## Layers

| Layer | Path | Role |
|-------|------|------|
| API | `paper-api/.../dev/mintychochip/genetics/` | Pure model, DNA, meiosis, mutation, breeding, DTOs |
| Server | `paper-server/.../dev/mintychochip/genetics/` | Entity storage, NMS phenotype apply (**later**) |
| Vanilla hooks | thin `// mintychochip` | `canMate`, offspring (**later**) |

This document covers the **API core**. No NMS in paper-api.

## Model

```
LocusDefinition (id, chromosome, position, InheritanceMode)
  → Allele (DnaSequence)
  → GeneCopy (diploid pair, or hemizygous single for XY)
  → Genome (Sex + map of locus → GeneCopy)
  → Meiosis → Gamete
  → Fertilization + Mutation → child Genome
  → GenotypeSnapshot → PhenotypeSnapshot
```

### Sex and inheritance

- `Sex.FEMALE` (XX), `Sex.MALE` (XY)
- `InheritanceMode`: `AUTOSOMAL`, `X_LINKED`, `Y_LINKED`, `MATERNAL`
- Males store one allele for X-linked loci (hemizygous)
- Sons receive X only from dam; daughters receive one X from each parent
- Maternal loci: dam only
- Y-linked: sire → sons only

### Alleles and point mutations

- Allele source of truth = **DNA sequence** (A/T/C/G)
- Germline mutation after gamete allele is chosen (or on zygote alleles):
  - **substitution** (point mutation): replace one base with another
  - **insertion / deletion**: small indels
- Function: premature stop codon → null (loss-of-function), recessive to functional
- Silent mutations (same protein) preserve function; sequence still diverges

`MutationSettings`: substitutionRate, insertionRate, deletionRate, maxIndelLength (per base / generation).

### Recombination

`MeiosisEngine` groups loci by chromosome, sorts by position, samples 0/1/2 chiasmata, flips distal segments, picks one chromatid. Independent assortment across chromosomes.

### Genotype / phenotype DTOs

- `LocusGenotype`, `Zygosity`, `GenotypeSnapshot`
- `PhenotypeTrait`, `PhenotypeSnapshot`
- `PhenotypeDecoder` maps genotype + catalog → traits (default: dominant functional, codominant X-hetero labels)

## Out of scope (later)

- Entity NBT persistence
- Vanilla `Animal` hooks
- Species texture / attribute appliers
- Config JSON catalogs (in-code test catalogs first)

## Success criteria

1. Child genomes are built locus-by-locus from gametes, not whole-genome coin flips
2. Linked loci co-segregate when crossovers = 0
3. X-linked male child has dam-only allele
4. Point mutation can change a sequence base and/or functional status
5. Codominant female X hetero can decode to a distinct phenotype; males never do under those rules
6. No `net.minecraft` in paper-api genetics
