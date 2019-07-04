package com.hartwig.hmftools.common.region;

import java.util.List;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public final class GenomeRegions {

    @NotNull
    public static GenomeRegion create(@NotNull final String chromosome, final long start, final long end) {
        return ImmutableGenomeRegionImpl.builder().chromosome(chromosome).start(start).end(end).build();
    }

    private final String chromosome;
    private final int minGap;
    @NotNull
    private final List<GenomeRegion> regions;

    public GenomeRegions(@NotNull final String chromosome, final int minGap) {
        this.chromosome = chromosome;
        this.minGap = minGap;
        this.regions = Lists.newArrayList();
    }

    @NotNull
    public List<GenomeRegion> build() {
        return regions;
    }

    public void addPosition(final long position) {
        GenomeRegion prev = null;

        for (int i = 0; i < regions.size(); i++) {
            GenomeRegion current = regions.get(i);
            if (position >= current.start() && position <= current.end()) {
                return;
            }

            if (position < current.start()) {
                // Attach to previous?
                if (prev != null && prev.end() + minGap >= position) {

                    if (position + minGap >= current.start()) {
                        // Join previous and current
                        prev = create(prev.chromosome(), prev.start(), current.end());
                        regions.set(i - 1, prev);
                        regions.remove(i);
                        return;
                    } else {
                        // Add to previous
                        prev = create(prev.chromosome(), prev.start(), position);
                        regions.set(i - 1, prev);
                        return;
                    }
                }

                // Attach to current
                if (position + minGap >= current.start()) {
                    current = create(current.chromosome(), position, current.end());
                    regions.set(i, current);
                    return;
                }

                // Attach between
                regions.add(i, create(chromosome, position, position));
                return;
            }

            prev = current;
        }

        if (prev != null && prev.end() + minGap >= position) {
            prev = GenomeRegions.create(prev.chromosome(), prev.start(), position);
            regions.set(regions.size() - 1, prev);
        } else {
            regions.add(create(chromosome, position, position));
        }

    }

}