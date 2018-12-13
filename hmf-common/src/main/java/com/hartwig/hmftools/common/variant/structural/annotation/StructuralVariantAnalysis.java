package com.hartwig.hmftools.common.variant.structural.annotation;

import java.util.List;
import java.util.stream.Collectors;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Value.Immutable
@Value.Style(allParameters = true,
             passAnnotations = { NotNull.class, Nullable.class })
public abstract class StructuralVariantAnalysis {

    @NotNull
    public abstract List<StructuralVariantAnnotation> annotations();

    @NotNull
    public abstract List<GeneFusion> fusions();

    @NotNull
    public abstract List<GeneDisruption> disruptions();

    @NotNull
    public List<GeneFusion> reportableFusions() {
        return fusions().stream().filter(GeneFusion::reportable).collect(Collectors.toList());
    }

    @NotNull
    public List<GeneDisruption> reportableDisruptions() {
        return disruptions().stream().filter(GeneDisruption::reportable).collect(Collectors.toList());
    }
}