package com.hartwig.hmftools.knowledgebasegenerator.eventtype;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hartwig.hmftools.knowledgebasegenerator.GenomicEvents;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.ActionableAmplificationDeletion;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.CnvExtractor;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.ImmutableActionableAmplificationDeletion;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.ImmutableKnownAmplificationDeletion;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.KnownAmplificationDeletion;
import com.hartwig.hmftools.knowledgebasegenerator.fusion.ImmutableKnownFusions;
import com.hartwig.hmftools.knowledgebasegenerator.fusion.KnownFusions;
import com.hartwig.hmftools.knowledgebasegenerator.hotspot.HotspotExtractor;
import com.hartwig.hmftools.knowledgebasegenerator.sourceknowledgebase.Source;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class DetermineEventOfGenomicMutation {
    private static final Logger LOGGER = LogManager.getLogger(DetermineEventOfGenomicMutation.class);

    private static final List<String> AMPLIFICATION = Lists.newArrayList("Amplification",
            "amplification",
            "Overexpression",
            "overexpression",
            "amp",
            "OVEREXPRESSION",
            "Transcript Amplification",
            "Gain-of-function Mutations");
    private static final List<String> DELETION = Lists.newArrayList("Copy Number Loss",
            "Deletion",
            "del",
            "DELETION",
            "UNDEREXPRESSION",
            "Exon Loss Variant",
            "Loss Of Function Variant",
            "Transcript Ablation");
    private static final Set<String> VARIANTS = Sets.newHashSet("missense_variant", "inframe_deletion", "inframe_insertion");
    private static final Set<String> FUSIONS = Sets.newHashSet("Fusion", "Fusions", "FUSIONS", "Gene Fusion", "Transcript Fusion");
    private static final Set<String> RANGE = Sets.newHashSet();
    private static final Set<String> SIGNATURE = Sets.newHashSet("Microsatellite Instability-High", "Microsatellite");

    @NotNull
    public static KnownAmplificationDeletion checkKnownAmplification(@NotNull ViccEntry viccEntry, @NotNull EventType type,
            @NotNull HotspotExtractor hotspotExtractor) throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());
        if (AMPLIFICATION.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Amplification");
            return CnvExtractor.determineKnownAmplificationDeletion(source, typeEvent.toString(), type.gene());
        }
        return ImmutableKnownAmplificationDeletion.builder().gene("").eventType("").source("").sourceLink("").build();
    }

    @NotNull
    public static KnownAmplificationDeletion checkKnownDeletion(@NotNull ViccEntry viccEntry, @NotNull EventType type,
            @NotNull HotspotExtractor hotspotExtractor) throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());

        if (DELETION.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Deletion");
            return CnvExtractor.determineKnownAmplificationDeletion(source, typeEvent.toString(), type.gene());
        }
        return ImmutableKnownAmplificationDeletion.builder().gene("").eventType("").source("").sourceLink("").build();
    }

    @NotNull
    public static ActionableAmplificationDeletion checkActionableAmplification(@NotNull ViccEntry viccEntry, @NotNull EventType type,
            @NotNull HotspotExtractor hotspotExtractor) throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());
        if (AMPLIFICATION.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Amplification");
            return CnvExtractor.determineActionableAmplificationDeletion(source, typeEvent.toString(), type.gene(), viccEntry);

        }
        return ImmutableActionableAmplificationDeletion.builder()
                .gene("")
                .eventType("")
                .source("")
                .drug("")
                .drugType("")
                .cancerType("")
                .level("")
                .direction("")
                .sourceLink("").build();
    }

    @NotNull
    public static ActionableAmplificationDeletion checkActionableDeletion(@NotNull ViccEntry viccEntry, @NotNull EventType type,
            @NotNull HotspotExtractor hotspotExtractor) throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());

        if (DELETION.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Deletion");
            return CnvExtractor.determineActionableAmplificationDeletion(source, typeEvent.toString(), type.gene(), viccEntry);
        }
        return ImmutableActionableAmplificationDeletion.builder()
                .gene("")
                .eventType("")
                .source("")
                .drug("")
                .drugType("")
                .cancerType("")
                .level("")
                .direction("")
                .sourceLink("").build();
    }

    public static void checkVariants(@NotNull ViccEntry viccEntry, @NotNull EventType type, @NotNull HotspotExtractor hotspotExtractor)
            throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());

        if (VARIANTS.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Variants");
        }
    }

    public static void checkRange(@NotNull ViccEntry viccEntry, @NotNull EventType type, @NotNull HotspotExtractor hotspotExtractor)
            throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());

        if (RANGE.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Range");
        }
    }

    @NotNull
    public static KnownFusions checkFusions(@NotNull ViccEntry viccEntry, @NotNull EventType type,
            @NotNull HotspotExtractor hotspotExtractor) throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());

        if (FUSIONS.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Fusions");
        }
        return ImmutableKnownFusions.builder().gene("").eventType("").source("").sourceLink("").build();
    }

    public static void checkSignatures(@NotNull ViccEntry viccEntry, @NotNull EventType type, @NotNull HotspotExtractor hotspotExtractor)
            throws IOException, InterruptedException {
        Source source = Source.sourceFromKnowledgebase(viccEntry.source());

        if (SIGNATURE.contains(type.eventType())) {
            GenomicEvents typeEvent = GenomicEvents.genomicEvents("Signature");
        }
    }
}