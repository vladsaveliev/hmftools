package com.hartwig.hmftools.common.purple.purity;

import java.util.List;

import com.google.common.collect.ListMultimap;
import com.hartwig.hmftools.common.chromosome.Chromosome;
import com.hartwig.hmftools.common.collect.Multimaps;
import com.hartwig.hmftools.common.numeric.Doubles;
import com.hartwig.hmftools.common.purple.copynumber.PurpleCopyNumber;

import org.jetbrains.annotations.NotNull;

public class WholeGenomeDuplication {

    static final double MIN_AVERAGE_PLOIDY = 1.5;
    static final int MIN_DUPLICATED_AUTOSOMES = 11;

    public static boolean wholeGenomeDuplication(@NotNull final List<PurpleCopyNumber> copyNumbers) {
        return duplicatedAutosomes(copyNumbers) >= MIN_DUPLICATED_AUTOSOMES;
    }

    private static int duplicatedAutosomes(@NotNull final List<PurpleCopyNumber> copyNumbers) {
        ListMultimap<Chromosome, PurpleCopyNumber> copyNumberMap = Multimaps.fromRegions(copyNumbers);

        int duplicatedAutosomes = 0;
        for (Chromosome chromosome : copyNumberMap.keys()) {
            if (chromosome.isAutosome() && Doubles.greaterOrEqual(averageMajorAllelePloidy(copyNumberMap.get(chromosome)),
                    MIN_AVERAGE_PLOIDY)) {
                duplicatedAutosomes++;
            }

        }

        return duplicatedAutosomes;
    }

    static double averageMajorAllelePloidy(@NotNull final List<PurpleCopyNumber> copyNumbers) {

        double weightedMajorAllelePloidy = 0;
        long totalBafCount = 0;

        for (PurpleCopyNumber copyNumber : copyNumbers) {
            weightedMajorAllelePloidy += copyNumber.majorAllelePloidy() * copyNumber.bafCount();
            totalBafCount += copyNumber.bafCount();
        }

        return totalBafCount > 0 ? weightedMajorAllelePloidy / totalBafCount : 0;
    }

}