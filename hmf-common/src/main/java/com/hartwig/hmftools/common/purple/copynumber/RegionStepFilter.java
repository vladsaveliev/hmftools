package com.hartwig.hmftools.common.purple.copynumber;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;

import static com.hartwig.hmftools.common.numeric.Doubles.equal;
import static com.hartwig.hmftools.common.numeric.Doubles.lessThan;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.region.GenomeRegion;

import org.jetbrains.annotations.NotNull;

@Deprecated
enum RegionStepFilter {
    ;

    private static final long MIN_SIZE = 1000;

    @NotNull
    static List<PurpleCopyNumber> filter(@NotNull final List<PurpleCopyNumber> region) {
        List<PurpleCopyNumber> results = Lists.newArrayList();

        int i = 0;
        while (i < region.size()) {
            PurpleCopyNumber first = region.get(i);
            PurpleCopyNumber second = i + 1 < region.size() ? region.get(i + 1) : null;
            PurpleCopyNumber third = i + 2 < region.size() ? region.get(i + 2) : null;

            if (second != null && third != null && second.bases() == MIN_SIZE && isSameChromosome(first, third)) {
                double myFirstSecondDifference = first.averageTumorCopyNumber() - second.averageTumorCopyNumber();
                double mySecondThirdDifference = second.averageTumorCopyNumber() - third.averageTumorCopyNumber();

                if (equal(signum(myFirstSecondDifference), signum(mySecondThirdDifference))) {
                    if (lessThan(abs(myFirstSecondDifference), abs(mySecondThirdDifference))) {
                        results.add(merge(first, second));
                        i += 2;

                    } else {
                        results.add(first);
                        results.add(merge(third, second));
                        i += 3;
                    }
                    continue;
                }
            }

            results.add(first);
            i++;
        }

        return results;
    }

    @NotNull
    private static PurpleCopyNumber merge(@NotNull final PurpleCopyNumber primary,
            @NotNull final PurpleCopyNumber secondary) {
        return ImmutablePurpleCopyNumber.builder().from(primary).start(min(primary.start(), secondary.start())).end(
                max(primary.end(), secondary.end())).build();
    }

    private static boolean isSameChromosome(@NotNull final GenomeRegion region1, @NotNull final GenomeRegion region2) {
        return region1.chromosome().equals(region2.chromosome());
    }
}
