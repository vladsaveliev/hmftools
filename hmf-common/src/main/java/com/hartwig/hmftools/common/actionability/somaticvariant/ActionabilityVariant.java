package com.hartwig.hmftools.common.actionability.somaticvariant;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class ActionabilityVariant {

    @NotNull
    abstract String gene();

    @NotNull
    abstract String chromosome();

    abstract long position();

    @NotNull
    abstract String ref();

    @NotNull
    abstract String alt();

    @NotNull
    abstract String source();

    @NotNull
    abstract String reference();

    @NotNull
    abstract String drug();

    @NotNull
    abstract String drugsType();

    @NotNull
    abstract String cancerType();

    @NotNull
    abstract String level();

    @NotNull
    abstract String response();
}
