package com.hartwig.hmftools.sage.read;

import static com.hartwig.hmftools.common.variant.Microhomology.expandMicrohomologyRepeats;
import static com.hartwig.hmftools.common.variant.Microhomology.microhomologyAtDeleteFromReadSequence;
import static com.hartwig.hmftools.common.variant.Microhomology.microhomologyAtInsert;

import java.util.Optional;

import com.hartwig.hmftools.common.variant.MicrohomologyContext;
import com.hartwig.hmftools.common.variant.repeat.RepeatContext;
import com.hartwig.hmftools.common.variant.repeat.RepeatContextFactory;

import org.apache.logging.log4j.util.Strings;
import org.jetbrains.annotations.NotNull;

import htsjdk.samtools.SAMRecord;

public class ReadContextFactory {

    private static final int DEFAULT_BUFFER = 25;
    private static final int MIN_CORE_DISTANCE = 2;

    @NotNull
    public static ReadContext createDelContext(@NotNull final String ref, int refPosition, int readIndex, @NotNull final SAMRecord record,
            final IndexedBases refBases) {
        int refIndex = refPosition - refBases.position() + refBases.index();

        final MicrohomologyContext microhomologyContext = microhomologyAtDeleteFromReadSequence(readIndex, ref, record.getReadBases());
        final MicrohomologyContext microhomologyContextWithRepeats = expandMicrohomologyRepeats(microhomologyContext);

        int startIndex = microhomologyContextWithRepeats.position() - MIN_CORE_DISTANCE;
        int length = Math.max(microhomologyContext.length(), microhomologyContextWithRepeats.length() - ref.length() + 1) + 1;
        int endIndex = Math.max(microhomologyContextWithRepeats.position() + MIN_CORE_DISTANCE, microhomologyContextWithRepeats.position() + length);

        final Optional<RepeatContext> refRepeatContext = RepeatContextFactory.repeats(refIndex + 1, refBases.bases());
        if (refRepeatContext.isPresent()) {
            final RepeatContext repeat = refRepeatContext.get();
            startIndex = Math.min(startIndex, repeat.startIndex() - 1);
            endIndex = Math.max(endIndex, repeat.endIndex() + 1);
        }

        final Optional<RepeatContext> readRepeatContext = RepeatContextFactory.repeats(readIndex + 1, record.getReadBases());
        if (readRepeatContext.isPresent()) {
            final RepeatContext repeat = readRepeatContext.get();
            startIndex = Math.min(startIndex, repeat.startIndex() - 1);
            endIndex = Math.max(endIndex, repeat.endIndex() + 1);
        }

        return new ReadContext(microhomologyContext.toString(),
                readRepeatContext.map(RepeatContext::count).orElse(0),
                readRepeatContext.map(RepeatContext::sequence).orElse(Strings.EMPTY),
                refPosition,
                readIndex,
                Math.max(startIndex, 0),
                Math.min(endIndex, record.getReadBases().length - 1),
                DEFAULT_BUFFER,
                refBases,
                record);
    }

    @NotNull
    public static ReadContext createInsertContext(@NotNull final String alt, int refPosition, int readIndex,
            @NotNull final SAMRecord record, final IndexedBases refBases) {
        int refIndex = refPosition - refBases.position() + refBases.index();

        final MicrohomologyContext microhomologyContext = microhomologyAtInsert(readIndex, alt.length(), record.getReadBases());
        final MicrohomologyContext microhomologyContextWithRepeats = expandMicrohomologyRepeats(microhomologyContext);

        int startIndex = microhomologyContextWithRepeats.position() - MIN_CORE_DISTANCE;
        int length = Math.max(microhomologyContextWithRepeats.length() + 1, alt.length());
        int endIndex = Math.max(microhomologyContextWithRepeats.position() + MIN_CORE_DISTANCE, microhomologyContextWithRepeats.position() + length);

        final Optional<RepeatContext> refRepeatContext = RepeatContextFactory.repeats(refIndex + 1, refBases.bases());
        if (refRepeatContext.isPresent()) {
            final RepeatContext repeat = refRepeatContext.get();
            startIndex = Math.min(startIndex, repeat.startIndex() - 1);
            endIndex = Math.max(endIndex, repeat.endIndex() + 1);
        }

        final Optional<RepeatContext> readRepeatContext = RepeatContextFactory.repeats(readIndex + 1, record.getReadBases());
        if (readRepeatContext.isPresent()) {
            final RepeatContext repeat = readRepeatContext.get();
            startIndex = Math.min(startIndex, repeat.startIndex() - 1);
            endIndex = Math.max(endIndex, repeat.endIndex() + 1);
        }

        return new ReadContext(microhomologyContext.toString(),
                readRepeatContext.map(RepeatContext::count).orElse(0),
                readRepeatContext.map(RepeatContext::sequence).orElse(Strings.EMPTY),
                refPosition,
                readIndex,
                Math.max(startIndex, 0),
                Math.min(endIndex, record.getReadBases().length - 1),
                DEFAULT_BUFFER,
                refBases,
                record);
    }

    @NotNull
    public static ReadContext createSNVContext(int refPosition, int readIndex, @NotNull final SAMRecord record,
            final IndexedBases refBases) {

        int refIndex = refPosition - refBases.position() + refBases.index();
        int startIndex = readIndex - MIN_CORE_DISTANCE;
        int endIndex = readIndex + MIN_CORE_DISTANCE;

        final Optional<RepeatContext> refRepeatContext = RepeatContextFactory.repeats(refIndex + 1, refBases.bases());
        if (refRepeatContext.isPresent()) {
            final RepeatContext repeat = refRepeatContext.get();
            startIndex = Math.min(startIndex, repeat.startIndex() - 1);
            endIndex = Math.max(endIndex, repeat.endIndex() + 1);
        }

        final Optional<RepeatContext> readRepeatContext = RepeatContextFactory.repeats(readIndex, record.getReadBases());
        if (readRepeatContext.isPresent()) {
            final RepeatContext repeat = readRepeatContext.get();
            startIndex = Math.min(startIndex, repeat.startIndex() - 1);
            endIndex = Math.max(endIndex, repeat.endIndex() + 1);
        }

        return new ReadContext(Strings.EMPTY,
                readRepeatContext.map(RepeatContext::count).orElse(0),
                readRepeatContext.map(RepeatContext::sequence).orElse(Strings.EMPTY),
                refPosition,
                readIndex,
                Math.max(startIndex, 0),
                Math.min(endIndex, record.getReadBases().length - 1),
                DEFAULT_BUFFER,
                refBases,
                record);
    }

    @NotNull
    public static ReadContext dummy(int refPosition, @NotNull final String alt) {
        return new ReadContext(Strings.EMPTY, refPosition, 0, 0, 0, DEFAULT_BUFFER, alt.getBytes());
    }

}
