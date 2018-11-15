package com.hartwig.hmftools.common.variant.structural.annotation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Transcript {

    @NotNull
    private final GeneAnnotation parent;
    @NotNull
    private final String transcriptId;
    private final int exonUpstream;
    private final int exonUpstreamPhase;
    private final int exonDownstream;
    private final int exonDownstreamPhase;
    private final int exonMax;

    private final long mCodingBases;
    private final long mTotalCodingBases;

    private final boolean canonical;

    @Nullable
    private final Long codingStart;

    @Nullable
    private final Long codingEnd;

    public static String TRANS_REGION_TYPE_PROMOTOR = "Promotor";
    public static String TRANS_REGION_TYPE_EXONIC = "Exonic";
    public static String TRANS_REGION_TYPE_INTRONIC = "Intronic";

    public static String TRANS_CODING_TYPE_CODING = "Coding";
    public static String TRANS_CODING_TYPE_UPSTREAM = "Upstream";
    public static String TRANS_CODING_TYPE_DOWNSTREAM = "Downstream";
    public static String TRANS_CODING_TYPE_NONE = "None";

    private static int STOP_CODON_LENGTH = 3;

    public Transcript(@NotNull final GeneAnnotation parent, @NotNull final String transcriptId,
            final int exonUpstream, final int exonUpstreamPhase, final int exonDownstream, final int exonDownstreamPhase,
            final long codingBases, final long totalCodingBases,
            final int exonMax, final boolean canonical, @Nullable final Long codingStart, @Nullable final Long codingEnd)
    {
        this.parent = parent;
        this.transcriptId = transcriptId;

        this.exonUpstream = exonUpstream;
        this.exonUpstreamPhase = exonUpstreamPhase;
        this.exonDownstream = exonDownstream;
        this.exonDownstreamPhase = exonDownstreamPhase;

        this.exonMax = exonMax;

        if(totalCodingBases > STOP_CODON_LENGTH)
        {
            // remove the stop codon from what is consider coding
            mTotalCodingBases = totalCodingBases - STOP_CODON_LENGTH;
        }
        else
        {
            mTotalCodingBases = 0;
        }

        if(codingBases > mTotalCodingBases)
        {
            // limit as well
            mCodingBases = mTotalCodingBases;
        }
        else
        {
            mCodingBases = codingBases;
        }

        this.canonical = canonical;
        this.codingStart = codingStart;
        this.codingEnd = codingEnd;
    }

    @NotNull
    public String transcriptId() {
        return transcriptId;
    }

    public boolean isExonic()
    {
        return exonUpstream > 0 && exonUpstream == exonDownstream;
    }

    public boolean isPromoter()
    {
        return exonUpstream == 0 && exonDownstream == 1;
    }

    public boolean isIntronic()
    {
        return exonUpstream > 0 && (exonDownstream - exonUpstream) == 1;
    }

    public final String getRegionType()
    {
        if(isIntronic())
            return TRANS_REGION_TYPE_INTRONIC;

        if(isExonic())
            return TRANS_REGION_TYPE_EXONIC;

        return TRANS_REGION_TYPE_PROMOTOR;
    }

    public final String getCodingType()
    {
        if(mTotalCodingBases == 0)
            return TRANS_CODING_TYPE_NONE;
        else if(mCodingBases == 0)
            return TRANS_CODING_TYPE_UPSTREAM;
        else if(mCodingBases == mTotalCodingBases)
            return TRANS_CODING_TYPE_DOWNSTREAM;
        else
            return TRANS_CODING_TYPE_CODING;
    }

    public boolean isCanonical() { return canonical; }

    @NotNull
    public GeneAnnotation parent() { return parent; }

    @NotNull
    public String geneName() { return parent.geneName(); }

    public int exonUpstream() { return exonUpstream; }
    public int exonUpstreamPhase() { return exonUpstreamPhase; }

    public long codingBases() { return mCodingBases; }
    public long totalCodingBases() { return mTotalCodingBases; }

    public int exonDownstream() { return exonDownstream; }
    public int exonDownstreamPhase() { return exonDownstreamPhase; }

    public int exonMax() { return exonMax; }

    @Nullable
    public Long codingStart() { return codingStart; }

    @Nullable
    public Long codingEnd() { return codingEnd; }

    public final String toString()
    {
        return parent.geneName() + " " + transcriptId;
    }
}
