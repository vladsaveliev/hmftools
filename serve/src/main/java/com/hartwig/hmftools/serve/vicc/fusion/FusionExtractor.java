package com.hartwig.hmftools.serve.vicc.fusion;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.datamodel.ViccSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public class FusionExtractor {

    private static final Logger LOGGER = LogManager.getLogger(FusionExtractor.class);
    @NotNull
    private final Set<String> uniqueFusionsPair = Sets.newHashSet();
    @NotNull
    private final Set<String> uniqueFusionsPromiscuous = Sets.newHashSet();
    private static final Set<String> SEARCH_FUSION_PAIRS = Sets.newHashSet("Fusion", "Disruptive Inframe Deletion", "Gene Fusion", "EGFRvII");
    private static final Set<String> SEARCH_FUSION_PROMISCUOUS =
            Sets.newHashSet("REARRANGEMENT", "Fusions", "fusion", "rearrange", "Transcript Fusion");
    private static final Set<String> IGNORE = Sets.newHashSet("3' EXON DELETION");
    private static final Set<String> INTERNAL_FUSION = Sets.newHashSet("(Partial");

    @NotNull
    public Set<String> uniqueFusionsPair() {
        return uniqueFusionsPair;
    }

    @NotNull
    public Set<String> uniqueFusionsPromiscuous() {
        return uniqueFusionsPromiscuous;
    }

    private boolean isFusion(@NotNull Feature feature) {
        FusionEvent eventKeyFusion = extractKeyFusion(feature);
        if (eventKeyFusion.equals(FusionEvent.FUSION_PAIR)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isFusionPromiscuous(@NotNull Feature feature) {
        FusionEvent eventKeyFusion = extractKeyFusion(feature);
        if (eventKeyFusion.equals(FusionEvent.FUSION_PROMISCUOUS)) {
            return true;
        } else {
            return false;
        }
    }

    @NotNull
    private FusionEvent extractKeyFusion(@NotNull Feature feature) {
        if (!IGNORE.contains(feature.name())) { // Extract internal fusion
            if (INTERNAL_FUSION.contains(feature.proteinAnnotation())) {
                return FusionEvent.FUSION_PAIR;
            } else if (SEARCH_FUSION_PAIRS.contains(feature.proteinAnnotation())) {
                return FusionEvent.FUSION_PAIR;
            } else if (SEARCH_FUSION_PROMISCUOUS.contains(feature.proteinAnnotation())) {
                if (feature.name().contains("-")) {
                    return FusionEvent.FUSION_PAIR;
                } else {
                    return FusionEvent.FUSION_PROMISCUOUS;
                }
            } else if (feature.biomarkerType() != null) {
                if (SEARCH_FUSION_PROMISCUOUS.contains(feature.biomarkerType())) {
                    if (feature.name().contains("-")) {
                        return FusionEvent.FUSION_PAIR;
                    } else {
                        return FusionEvent.FUSION_PROMISCUOUS;
                    }
                }
                if (SEARCH_FUSION_PAIRS.contains(feature.biomarkerType())) {
                    return FusionEvent.FUSION_PAIR;
                }
            } else if (feature.name().toLowerCase().contains("exon") && feature.name().toLowerCase().contains("deletion") && feature.name()
                    .contains("-") || feature.name().contains("&")) {// Extract internal fusion
                return FusionEvent.FUSION_PAIR;
            } else {
                return FusionEvent.UNKNOWN;
            }
        }

        //TODO: check why this is needed??
        return FusionEvent.UNKNOWN;
    }

    public Map<Feature, FusionAnnotation> extractKnownFusions(@NotNull ViccEntry viccEntry) {
        Map<Feature, FusionAnnotation> fusionsPerFeature = Maps.newHashMap();

        for (Feature feature : viccEntry.features()) {
            if (isFusion(feature)) {
                fusionsPerFeature.put(feature, ImmutableFusionAnnotation.builder().fusion(feature.name()).fusionEvent(FusionEvent.FUSION_PAIR).build());
            } else if (isFusionPromiscuous(feature)) {
                fusionsPerFeature.put(feature,
                        ImmutableFusionAnnotation.builder().fusion(feature.name()).fusionEvent(FusionEvent.FUSION_PROMISCUOUS).build());
            }
        }
        return fusionsPerFeature;
    }

    @NotNull
    public static KnownFusions determinePromiscuousFusions(@NotNull ViccSource source, @NotNull String typeEvent, @NotNull String gene,
            @NotNull String function) {
        if (function.equals("Likely Loss-of-function")) {
            gene = Strings.EMPTY;
            typeEvent = Strings.EMPTY;
        }

        return ImmutableKnownFusions.builder()
                .gene(gene)
                .eventType(typeEvent)
                .source(source.toString())
                .sourceLink(source.toString())
                .build();
    }

    @NotNull
    public static KnownFusions determineKnownFusionsPairs(@NotNull ViccSource source, @NotNull String typeEvent, @NotNull String gene,
            @NotNull String function) {
        if (gene.equals("ZNF198-FGFR1")) {
            gene = "ZMYM2-FGFR1";
        } else if (gene.equals("NPM-ALK")) {
            gene = "NPM1-ALK";
        } else if (gene.equals("ABL1-BCR")) {
            gene = "BCR-ABL1";
        } else if (gene.equals("ROS1-CD74")) {
            gene = "CD74-ROS1";
        } else if (gene.equals("RET-CCDC6")) {
            gene = "CCDC6-RET";
        } else if (gene.equals("EP300-MOZ")) {
            gene = "KAT6A-EP300";
        } else if (gene.equals("EP300-MLL")) {
            gene = "KMT2A-EP300";
        } else if (gene.equals("BRD4-NUT")) {
            gene = "BRD4-NUTM1";
        } else if (gene.equals("CEP110-FGFR1")) {
            gene = "CNTRL-FGFR1";
        } else if (gene.equals("FGFR2-KIAA1967")) {
            gene = "FGFR2-CCAR2";
        } else if (gene.equals("FIG-ROS1")) {
            gene = "GOPC-ROS1";
        } else if (gene.equals("GPIAP1-PDGFRB")) {
            gene = "CAPRIN1-PDGFRB";
        } else if (gene.equals("IGL-MYC")) {
            gene = "IGLC6-MYC";
        } else if (gene.equals("KIAA1509-PDGFRB")) {
            gene = "CCDC88C-PDGFRB";
        } else if (gene.equals("MLL-TET1")) {
            gene = "KMT2A-TET1";
        } else if (gene.equals("PAX8-PPAR?")) {
            gene = "PAX8-PPARA";
        } else if (gene.equals("SEC16A1-NOTCH1")) {
            gene = "SEC16A-NOTCH1";
        } else if (gene.equals("TEL-JAK2")) {
            gene = "ETV6-JAK2";
        } else if (gene.equals("TRA-NKX2-1")) {
            gene = "TRAC-NKX2-1";
        } else if (gene.equals("FGFR1OP1-FGFR1")) {
            gene = "FGFR1OP-FGFR1";
        } else if (gene.equals("PDGFRA-FIP1L1")) {
            gene = "FIP1L1-PDGFRA";
        } else if (gene.equals("PDGFB-COL1A1")) {
            gene = "COL1A1-PDGFB";
        } else if (gene.equals("BRD4-C15orf55")) {
            gene = "BRD4-NUTM1";
        } else if (gene.equals("MLL-MLLT3")) {
            gene = "KMT2A-MLLT3";
        } else if (gene.equals("BCR-ABL")) {
            gene = "BCR-ABL1";
        } else if (gene.equals("ERLIN2?FGFR1")) {
            gene = "ERLIN2-FGFR1";
        } else if (gene.equals("FGFR2?PPHLN1")) {
            gene = "FGFR2-PPHLN1";
        } else if (gene.equals("FGFR3 - BAIAP2L1")) {
            gene = "FGFR3-BAIAP2L1";
        } else if (gene.equals("PAX8-PPAR?")) {
            gene = "PAX8-PPARA";
        } else if (gene.contains("IGH") || gene.contains("IGK") || gene.contains("TRB") || gene.contains("Delta")
                || gene.equals("RET-TPCN1") || gene.equals("PVT1-MYC") || gene.equals("ESR1-CCDC170") || gene.equals("BRAF-CUL1")
                || function.contains("Loss-of-function")) {
            gene = Strings.EMPTY;
            typeEvent = Strings.EMPTY;
        }
        return ImmutableKnownFusions.builder()
                .gene(gene)
                .eventType(typeEvent)
                .source(source.toString())
                .sourceLink(source.toString())
                .build();
    }
}
