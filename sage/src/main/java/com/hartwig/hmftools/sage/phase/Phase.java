package com.hartwig.hmftools.sage.phase;

import java.util.function.Consumer;

import com.hartwig.hmftools.sage.variant.SageVariant;
import com.hartwig.hmftools.sage.variant.SageVariantFactory;

import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.reference.IndexedFastaSequenceFile;

public class Phase implements Consumer<SageVariant>, AutoCloseable {
    private final SnvSnvMerge mnvMerge;
    private final DedupSnv snvIndelMerge;
    private final LocalPhaseSet localPhaseSet;
    private final DedupIndel dedupIndel;

    public Phase(@NotNull final IndexedFastaSequenceFile reference, @NotNull final SageVariantFactory sageVariantFactory,
            @NotNull final Consumer<SageVariant> consumer) {
        final MnvFactory mnvFactory = new MnvFactory(reference, sageVariantFactory);
        dedupIndel = new DedupIndel(consumer);
        snvIndelMerge = new DedupSnv(dedupIndel);
        mnvMerge = new SnvSnvMerge(snvIndelMerge, mnvFactory);
        localPhaseSet = new LocalPhaseSet(mnvMerge);

    }

    @Override
    public void accept(final SageVariant sageVariant) {
        localPhaseSet.accept(sageVariant);
    }

    @Override
    public void close() {
        localPhaseSet.flush();
        mnvMerge.flush();
        snvIndelMerge.flush();
        dedupIndel.flush();
    }
}