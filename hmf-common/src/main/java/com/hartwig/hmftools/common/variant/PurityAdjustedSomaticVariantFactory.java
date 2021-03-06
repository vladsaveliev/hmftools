package com.hartwig.hmftools.common.variant;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.hartwig.hmftools.common.genome.chromosome.Chromosome;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.common.genome.position.GenomePositions;
import com.hartwig.hmftools.common.genome.region.GenomeRegionSelector;
import com.hartwig.hmftools.common.genome.region.GenomeRegionSelectorFactory;
import com.hartwig.hmftools.common.purple.PurityAdjuster;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumber;
import com.hartwig.hmftools.common.purple.region.FittedRegion;
import com.hartwig.hmftools.common.purple.region.GermlineStatus;
import com.hartwig.hmftools.common.utils.Doubles;
import com.hartwig.hmftools.common.utils.collection.Multimaps;

import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.Genotype;
import htsjdk.variant.variantcontext.VariantContext;

public class PurityAdjustedSomaticVariantFactory {

    @NotNull
    private final PurityAdjuster purityAdjuster;
    @NotNull
    private final GenomeRegionSelector<PurpleCopyNumber> copyNumberSelector;
    @NotNull
    private final GenomeRegionSelector<FittedRegion> fittedRegionSelector;
    @NotNull
    private final String tumorSample;

    public PurityAdjustedSomaticVariantFactory(@NotNull String tumorSample, @NotNull PurityAdjuster purityAdjuster,
            @NotNull final List<PurpleCopyNumber> copyNumbers) {
        this(tumorSample, purityAdjuster, copyNumbers, Collections.emptyList());
    }

    public PurityAdjustedSomaticVariantFactory(@NotNull String tumorSample, @NotNull PurityAdjuster purityAdjuster,
            @NotNull final List<PurpleCopyNumber> copyNumbers, @NotNull final List<FittedRegion> fittedRegions) {
        this(tumorSample, purityAdjuster, Multimaps.fromRegions(copyNumbers), Multimaps.fromRegions(fittedRegions));
    }

    private PurityAdjustedSomaticVariantFactory(@NotNull String tumorSample, @NotNull PurityAdjuster purityAdjuster,
            @NotNull final Multimap<Chromosome, PurpleCopyNumber> copyNumbers,
            @NotNull final Multimap<Chromosome, FittedRegion> fittedRegions) {
        this.tumorSample = tumorSample;
        this.purityAdjuster = purityAdjuster;
        this.copyNumberSelector = GenomeRegionSelectorFactory.createImproved(copyNumbers);
        this.fittedRegionSelector = GenomeRegionSelectorFactory.createImproved(fittedRegions);
    }

    @NotNull
    public List<PurityAdjustedSomaticVariant> create(@NotNull final List<SomaticVariant> variants) {
        final List<PurityAdjustedSomaticVariant> result = Lists.newArrayList();

        for (SomaticVariant variant : variants) {
            final ImmutablePurityAdjustedSomaticVariantImpl.Builder builder = ImmutablePurityAdjustedSomaticVariantImpl.builder()
                    .from(variant)
                    .adjustedCopyNumber(0)
                    .adjustedVAF(0)
                    .minorAlleleCopyNumber(0)
                    .germlineStatus(GermlineStatus.UNKNOWN);

            enrich(variant, variant, builder);
            result.add(builder.build());
        }

        return result;
    }

    @NotNull
    public VariantContext enrich(@NotNull final VariantContext variant) {
        final Genotype genotype = variant.getGenotype(tumorSample);
        if (genotype != null && genotype.hasAD() && HumanChromosome.contains(variant.getContig())) {
            final GenomePosition position = GenomePositions.create(variant.getContig(), variant.getStart());
            final AllelicDepth depth = AllelicDepth.fromGenotype(genotype);
            enrich(position, depth, PurityAdjustedSomaticVariantBuilder.fromVariantContex(variant));
        }
        return variant;
    }

    private void enrich(@NotNull final GenomePosition position, @NotNull final AllelicDepth depth,
            @NotNull final PurityAdjustedSomaticVariantBuilder builder) {
        copyNumberSelector.select(position).ifPresent(x -> applyPurityAdjustment(x, depth, builder));
        fittedRegionSelector.select(position).ifPresent(x -> builder.germlineStatus(x.status()));
    }

    private void applyPurityAdjustment(@NotNull final PurpleCopyNumber purpleCopyNumber, @NotNull final AllelicDepth depth,
            @NotNull final PurityAdjustedSomaticVariantBuilder builder) {
        double copyNumber = purpleCopyNumber.averageTumorCopyNumber();
        double vaf = purityAdjuster.purityAdjustedVAF(purpleCopyNumber.chromosome(), Math.max(0.001, copyNumber), depth.alleleFrequency());
        double ploidy = Math.max(0, vaf * copyNumber);

        boolean biallelic = Doubles.lessOrEqual(copyNumber, 0) || Doubles.greaterOrEqual(ploidy, copyNumber - 0.5);

        builder.adjustedCopyNumber(copyNumber)
                .adjustedVAF(vaf)
                .variantCopyNumber(ploidy)
                .biallelic(biallelic)
                .minorAlleleCopyNumber(purpleCopyNumber.minorAlleleCopyNumber());
    }
}
