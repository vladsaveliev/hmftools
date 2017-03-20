package com.hartwig.hmftools.common.variant;

import java.util.List;

import com.google.common.collect.Lists;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

public enum VariantConsequence {
    // KODU: See also http://sequenceontology.org
    TRANSCRIPT_ABLATION("transcript_ablation"),
    TRANSCRIPT_AMPLIFICATION("transcript_amplification"),
    SPLICE_ACCEPTOR_VARIANT("splice_acceptor_variant"),
    SPLICE_DONOR_VARIANT("splice_donor_variant"),
    SPLICE_REGION_VARIANT("splice_region_variant", "exonic_splice_region_variant",
            "non_coding_transcript_splice_region_variant"),
    STOP_GAINED("stop_gained"),
    STOP_LOST("stop_lost"),
    START_LOST("start_lost"),
    FRAMESHIFT_VARIANT("frameshift_variant", "frame_restoring_variant", "frameshift_elongation",
            "frameshift_truncation", "minus_1_frameshift_variant", "minus_2_frameshift_variant",
            "plus_1_frameshift_variant", "plus_2_frameshift_variant"),
    INFRAME_INSERTION("inframe_insertion", "conservative_inframe_insertion", "disruptive_inframe_insertion"),
    INFRAME_DELETION("inframe_deletion", "conservative_inframe_insertion", "disruptive_inframe_insertion"),
    MISSENSE_VARIANT("missense_variant", "conservative_missense_variant", "non_conservative_missense_variant",
            "rare_amino_avid_variant", "pyrrolysine_loss", "selenocysteine_loss"),
    OTHER(Strings.EMPTY);

    @NotNull
    private final String parentSequenceOntologyTerm;
    @NotNull
    private final List<String> sequenceOntologySubTerms;

    VariantConsequence(@NotNull final String parentSequenceOntologyTerm,
            @NotNull final String... sequenceOntologySubTerms) {
        this.parentSequenceOntologyTerm = parentSequenceOntologyTerm;
        this.sequenceOntologySubTerms = Lists.newArrayList(sequenceOntologySubTerms);
    }

    public boolean isParentTypeOf(@NotNull final String annotation) {
        return annotation.equals(parentSequenceOntologyTerm) || sequenceOntologySubTerms.contains(annotation);
    }

    @NotNull
    public String readableSequenceOntologyTerm() {
        return parentSequenceOntologyTerm.replace("_", " ");
    }
}
