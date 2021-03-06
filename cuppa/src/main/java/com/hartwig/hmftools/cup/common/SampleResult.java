package com.hartwig.hmftools.cup.common;

import java.util.Map;

public class SampleResult
{
    public final String SampleId;
    public final CategoryType Category;
    public final String DataType;
    public final Object Value;

    public final Map<String,Double> CancerTypeValues;

    public SampleResult(
            final String sampleId, final CategoryType category, final String dataType, final Object value,
            final Map<String,Double> cancerTypeValues)
    {
        SampleId = sampleId;
        Category = category;
        DataType = dataType;
        Value = value;
        CancerTypeValues = cancerTypeValues;
    }

    // public static double formatPercentile(double percentile) { return }
}
