package com.hartwig.hmftools.knowledgebasegenerator.transvar;

import java.util.List;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class TransvarRecord {

    @NotNull
    public abstract String transcript();

    @NotNull
    public abstract String chromosome();

    public abstract long gdnaPosition();

    @NotNull
    public abstract String gdnaRef();

    @NotNull
    public abstract String gdnaAlt();

    // Field is only populated for SNV/MNV
    @Nullable
    public abstract String referenceCodon();

    // Field is only populated for SNV/MNV
    @Nullable
    public abstract List<String> candidateCodons();

    // Dups are not very well interpreted by transvar. The dup length is the only extractable piece of information.
    @Nullable
    public abstract Integer dupLength();

}
