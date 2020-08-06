package com.hartwig.hmftools.cup.drivers;

import static com.hartwig.hmftools.cup.SampleAnalyserConfig.DATA_DELIM;

public class SampleDriverData
{
    public final String SampleId;
    public final String Gene;
    public final String Type;

    public static final String DRIVER_TYPE_ALL = "ALL";

    public SampleDriverData(final String sampleId, final String gene, final String type)
    {
        SampleId = sampleId;
        Gene = gene;
        Type = type;
    }

    public static SampleDriverData from(final String data)
    {
        final String[] items = data.split(DATA_DELIM, -1);
        if(items.length != 3)
            return null;

        return new SampleDriverData(items[0], items[1], items[2]);
    }

    public boolean isTypeAll() { return Type.equals(DRIVER_TYPE_ALL); }

    public String toString() { return String.format("sample(%s) gene(%s) type(%s)", SampleId, Gene, Type); }
}
