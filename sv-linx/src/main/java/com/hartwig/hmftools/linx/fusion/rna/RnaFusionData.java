package com.hartwig.hmftools.linx.fusion.rna;

import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_DOWNSTREAM;
import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_PAIR;
import static com.hartwig.hmftools.common.fusion.FusionCommon.FS_UPSTREAM;
import static com.hartwig.hmftools.common.fusion.GeneFusion.REPORTABLE_TYPE_NONE;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.linx.chaining.SvChain.CHAIN_LENGTH;
import static com.hartwig.hmftools.linx.chaining.SvChain.CHAIN_LINK_COUNT;
import static com.hartwig.hmftools.linx.fusion.rna.RnaJunctionType.KNOWN;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.fusion.Transcript;
import com.hartwig.hmftools.common.genome.chromosome.HumanChromosome;
import com.hartwig.hmftools.linx.types.SvBreakend;
import com.hartwig.hmftools.linx.chaining.SvChain;
import com.hartwig.hmftools.linx.types.SvCluster;
import com.hartwig.hmftools.linx.types.SvVarData;

public class RnaFusionData
{
    public final String FusionId;
    public final String SampleId;
    public final String[] GeneIds;
    public final String[] GeneNames;
    public final String[] Chromosomes;
    public final int[] Positions;
    public final byte[] Strands;

    public final int JunctionFragments;
    public final int DiscordantFragments;
    public final RnaJunctionType[] JunctionTypes;

    private boolean mIsValid;
    private List<String> mExonMatchedTransIdUp; // transcripts with an exon matching the RNA position
    private List<String> mExonMatchedTransIdDown;

    // annotations and matching results

    // transcripts matching SV breakends
    private Transcript mTransUp;
    private Transcript mTransDown;

    // canonical exon positions based on RNA positions
    private int mExonRankUp;
    private int mExonPhaseUp;
    private int mExonRankDown;
    private int mExonPhaseDown;

    // transcripts matching RNA positions if in a phased fusion
    private String mRnaTransIdUp;
    private String mRnaTransIdDown;

    private boolean mViableFusion; // the pair of transcripts satisfied standard fusion rules
    private boolean mPhaseMatchedFusion;
    private String mKnownFusionType;
    private boolean mTransViableUp; // the transcript fell in the correct location relative to the RNA position
    private boolean mTransViableDown;
    private boolean mTransCorrectLocationUp; // the transcript fell on the correct side and orientation for the RNA position
    private boolean mTransCorrectLocationDown;
    private int mExonsSkippedUp; // where no valid breakend was found, record the number of exons skipped between the breakend and the RNA
    private int mExonsSkippedDown;

    // SVA match data
    private SvBreakend mBreakendUp;
    private SvBreakend mBreakendDown;

    private DnaRnaMatchType mDnaFusionMatchType; // no match, match on gene, match on exact SVs
    private String mDnaFusionMatchInfo;

    private String mClusterInfoUp;
    private String mClusterInfoDown;
    private String mChainInfo;

    public RnaFusionData(
            final String fusionId, final String sampleId, final String[] geneIds, final String[] geneNames, final String[] chromosomes,
            final int[] positions, int junctionReadCount, int spanningFragCount, final RnaJunctionType[] junctionTypes)
    {
        FusionId = fusionId;
        SampleId = sampleId;
        GeneIds = geneIds;
        GeneNames = geneNames;
        Chromosomes = chromosomes;
        Positions = positions;
        JunctionFragments = junctionReadCount;
        DiscordantFragments = spanningFragCount;
        JunctionTypes = junctionTypes;

        Strands = new byte[FS_PAIR];

        mIsValid = true;
        mExonRankUp = 0;
        mExonPhaseUp = 0;
        mExonRankDown = 0;
        mExonPhaseDown = 0;

        mExonMatchedTransIdDown = Lists.newArrayList();
        mExonMatchedTransIdUp = Lists.newArrayList();

        mTransUp = null;
        mTransDown = null;
        mBreakendUp = null;
        mBreakendDown = null;

        mRnaTransIdUp = "";
        mRnaTransIdDown = "";

        mViableFusion = false;
        mPhaseMatchedFusion = false;
        mKnownFusionType = REPORTABLE_TYPE_NONE;
        mTransViableUp = false;
        mTransViableDown = false;
        mTransCorrectLocationUp = false;
        mTransCorrectLocationDown = false;
        mExonsSkippedUp = 0;
        mExonsSkippedDown = 0;

        mDnaFusionMatchType = DnaRnaMatchType.NONE;
        mDnaFusionMatchInfo = "";

        mClusterInfoUp = "";
        mClusterInfoDown = "";
        mChainInfo = "0;0";
    }

    public String name() { return GeneNames[FS_UPSTREAM] + "_" + GeneNames[FS_DOWNSTREAM]; }

    public boolean isValid() { return mIsValid; }
    public void setValid(boolean toggle) { mIsValid = toggle; }

    public boolean matchesKnownSpliceSite() { return JunctionTypes[FS_UPSTREAM] == KNOWN && JunctionTypes[FS_DOWNSTREAM] == KNOWN; }

    public void setExonData(boolean isUpstream, int rank, int phase)
    {
        if(isUpstream)
        {
            mExonPhaseUp = phase;
            mExonRankUp = rank;
        }
        else
        {
            mExonPhaseDown = phase;
            mExonRankDown = rank;
        }
    }

    public final List<String> getExactMatchTransIds(boolean isUpstream)
    {
        return isUpstream ? mExonMatchedTransIdUp : mExonMatchedTransIdDown;
    }

    public int exonRank(boolean isUpstream) { return isUpstream ? mExonRankUp : mExonRankDown; }
    public int exonPhase(boolean isUpstream) {return isUpstream ? mExonPhaseUp : mExonPhaseDown; }

    public void setRnaPhasedFusionData(final String transIdUp, final String transIdDown)
    {
        mRnaTransIdUp = transIdUp;
        mRnaTransIdDown = transIdDown;
    }

    public boolean hasRnaPhasedFusion() { return !mRnaTransIdUp.isEmpty() && !mRnaTransIdDown.isEmpty(); }
    public String getRnaPhasedFusionTransId(boolean isUpstream)
    {
        return isUpstream ? mRnaTransIdUp : mRnaTransIdDown;
    }

    public void setViableFusion(boolean viable, boolean phaseMatched)
    {
        mViableFusion = viable;
        mPhaseMatchedFusion = phaseMatched;
    }

    public void setDnaFusionMatch(final DnaRnaMatchType matchType, final String matchInfo)
    {
        mDnaFusionMatchType = matchType;
        mDnaFusionMatchInfo = matchInfo;
    }

    public final DnaRnaMatchType getDnaFusionMatchType() { return mDnaFusionMatchType; }
    public final String getDnaFusionMatchInfo() { return mDnaFusionMatchInfo; }

    public void setTranscriptData(boolean isUpstream, final Transcript trans, final SvBreakend breakend,
            boolean matchedRnaBoundary, boolean correctLocation, int exonsSkipped)
    {
        if(isUpstream)
        {
            mTransUp = trans;
            mBreakendUp = breakend;
            mTransViableUp = matchedRnaBoundary;
            mTransCorrectLocationUp = correctLocation;
            mExonsSkippedUp = exonsSkipped;
        }
        else
        {
            mTransDown = trans;
            mBreakendDown = breakend;
            mTransViableDown = matchedRnaBoundary;
            mTransCorrectLocationDown = correctLocation;
            mExonsSkippedDown = exonsSkipped;
        }
    }

    public final Transcript getTrans(boolean isUpstream) { return isUpstream ? mTransUp : mTransDown; }
    public final SvBreakend getBreakend(boolean isUpstream) { return isUpstream ? mBreakendUp : mBreakendDown; }
    public final boolean isTransViable(boolean isUpstream) { return isUpstream ? mTransViableUp : mTransViableDown; }
    public final boolean isTransCorrectLocation(boolean isUpstream) { return isUpstream ? mTransCorrectLocationUp : mTransCorrectLocationDown; }
    public final int getExonsSkipped(boolean isUpstream) { return isUpstream ? mExonsSkippedUp : mExonsSkippedDown; }

    public boolean isViableFusion() { return mViableFusion; }
    public boolean isPhaseMatchedFusion() { return mPhaseMatchedFusion; }

    public void setKnownType(final String knownType) { mKnownFusionType = knownType; }
    public final String getKnownType() { return mKnownFusionType; }

    public final String getClusterInfo(boolean isUpstream)
    {
        return isUpstream ? mClusterInfoUp : mClusterInfoDown;
    }

    public final String getChainInfo()
    {
        return mChainInfo;
    }

    public void setFusionClusterChainInfo()
    {
        if(mBreakendUp == null && mBreakendDown == null)
            return;

        if(mBreakendUp != null && mBreakendDown != null)
        {
            SvVarData varUp = mBreakendUp.getSV();
            SvCluster clusterUp = varUp.getCluster();
            SvVarData varDown = mBreakendDown.getSV();
            SvCluster clusterDown = varDown.getCluster();

            SvChain matchingChain = null;

            if(varUp != varDown && clusterUp == clusterDown)
            {
                // check for a matching chain if the clusters are the same
                matchingChain = clusterUp.findSameChainForSVs(varUp, varDown);

                if (matchingChain != null)
                {
                    final int chainData[] =
                            matchingChain.breakendsAreChained(varUp, !mBreakendUp.usesStart(), varDown, !mBreakendDown.usesStart());
                    mChainInfo = String.format("%d;%d", chainData[CHAIN_LINK_COUNT], chainData[CHAIN_LENGTH]);
                }
            }

            setFusionClusterInfo(mBreakendUp, true, matchingChain);
            setFusionClusterInfo(mBreakendDown, false, matchingChain);
        }
        else
        {
            SvBreakend breakend = mBreakendUp != null ? mBreakendUp : mBreakendDown;
            setFusionClusterInfo(breakend, mBreakendUp != null, null);
        }
    }

    private void setFusionClusterInfo(final SvBreakend breakend, boolean isUpstream, SvChain matchingChain)
    {
        // data: ClusterId;ClusterCount;ChainId;ChainCount
        SvCluster cluster = breakend.getCluster();

        SvChain chain = matchingChain != null ? matchingChain : cluster.findChain(breakend.getSV());

        final String clusterInfo = String.format("%d;%d;%d;%d",
                cluster.id(), cluster.getSvCount(),
                chain != null ? chain.id() : cluster.getChainId(breakend.getSV()), chain != null ? chain.getSvCount() : 1);

        if(isUpstream)
            mClusterInfoUp = clusterInfo;
        else
            mClusterInfoDown = clusterInfo;
    }

    public static String RNA_FUSION_SOURCE_ISOFOX = "ISOFOX";
    public static String RNA_FUSION_SOURCE_ARRIBA = "ARRIBA";
    public static String RNA_FUSION_SOURCE_STARFUSION = "STARFUSION";

    public static String getRnaSourceDelimiter(final String source)
    {
        return source.equals(RNA_FUSION_SOURCE_ARRIBA) ? "\t" : ",";
    }

    public static RnaFusionData from(final String source, final int index, final String data, final Map<String,Integer> fieldIndexMap)
    {
        if(source.equals(RNA_FUSION_SOURCE_ISOFOX))
            return fromIsofox(data, fieldIndexMap);
        else if(source.equals(RNA_FUSION_SOURCE_ARRIBA))
            return fromArriba(index, data, fieldIndexMap);
        else if(source.equals(RNA_FUSION_SOURCE_STARFUSION))
            return fromStarFusion(index, data, fieldIndexMap);
        else
            return null;
    }

    private static RnaFusionData fromIsofox(final String data, final Map<String,Integer> fieldIndexMap)
    {
        final String[] items = data.split(getRnaSourceDelimiter(RNA_FUSION_SOURCE_ISOFOX), -1);

        final String sampleId = items[fieldIndexMap.get("SampleId")];

        String fusionId = items[fieldIndexMap.get("FusionId")];

        final String[] chromosomes = new String[] { items[fieldIndexMap.get("ChrUp")], items[fieldIndexMap.get("ChrDown")] };

        final int[] positions =
                new int[] { Integer.parseInt(items[fieldIndexMap.get("PosUp")]), Integer.parseInt(items[fieldIndexMap.get("PosDown")]) };

        // final byte[] junctionOrientations =
        //        new byte[] { Byte.parseByte(items[fieldIndexMap.get("OrientUp")]), Byte.parseByte(items[fieldIndexMap.get("OrientDown")]) };

        final RnaJunctionType[] junctionTypes = new RnaJunctionType[] {
                RnaJunctionType.valueOf(items[fieldIndexMap.get("JuncTypeUp")]),
                RnaJunctionType.valueOf(items[fieldIndexMap.get("JuncTypeDown")]) };

        final String[] geneIds = new String[] { items[fieldIndexMap.get("GeneIdUp")], items[fieldIndexMap.get("GeneIdDown")] };
        final String[] geneNames = new String[] { items[fieldIndexMap.get("GeneNameUp")], items[fieldIndexMap.get("GeneNameDown")] };

        int junctionFrags = Integer.parseInt(items[fieldIndexMap.get("SplitFrags")]) + Integer.parseInt(items[fieldIndexMap.get("RealignedFrags")]);
        int discordantFrags = Integer.parseInt(items[fieldIndexMap.get("DiscordantFrags")]);

        return new RnaFusionData(
                fusionId, sampleId, geneIds, geneNames, chromosomes, positions, junctionFrags, discordantFrags, junctionTypes);
    }

    private static RnaFusionData fromArriba(int index, final String data, final Map<String,Integer> fieldIndexMap)
    {
        // SampleId,GeneNameUp,GeneNameDown,ChrUp,ChrDown,PosUp,PosDown,JuncTypeUp,JuncTypeDown,JunctionFrags,DiscordantFrags,CoverageUp,CoverageDown

        final String[] items = data.split(getRnaSourceDelimiter(RNA_FUSION_SOURCE_ARRIBA), -1);

        final String sampleId = items[fieldIndexMap.get("SampleId")];

        final String[] geneIds = new String[FS_PAIR];

        final String[] geneNames = new String[] {
                items[fieldIndexMap.get("GeneNameUp")].replaceAll(",",";"),
                items[fieldIndexMap.get("GeneNameDown")].replaceAll(",",";") };

        final String[] chromosomes = new String[] { items[fieldIndexMap.get("ChrUp")], items[fieldIndexMap.get("ChrDown")] };

        if(!HumanChromosome.contains(chromosomes[SE_START]) || !HumanChromosome.contains(chromosomes[SE_END]))
            return null;

        final int[] positions = new int[] {
                Integer.parseInt(items[fieldIndexMap.get("PosUp")]), Integer.parseInt(items[fieldIndexMap.get("PosDown")]) };

        int splitFragments = Integer.parseInt(items[fieldIndexMap.get("JunctionFrags")]);

        int discordantFragments = Integer.parseInt(items[fieldIndexMap.get("DiscordantFrags")]);

        final String[] spliceSites = new String[] { items[fieldIndexMap.get("JuncTypeUp")], items[fieldIndexMap.get("JuncTypeDown")] };

        final RnaJunctionType[] junctionTypes = new RnaJunctionType[] { RnaJunctionType.UNKNOWN, RnaJunctionType.UNKNOWN };

        if(spliceSites[FS_UPSTREAM].equals("splice-site"))
            junctionTypes[FS_UPSTREAM] = KNOWN;

        if(spliceSites[FS_DOWNSTREAM].equals("splice-site"))
            junctionTypes[FS_DOWNSTREAM] = KNOWN;

        return new RnaFusionData(
                String.valueOf(index), sampleId, geneIds, geneNames, chromosomes, positions, splitFragments, discordantFragments, junctionTypes);
    }

    private static RnaFusionData fromStarFusion(int index, final String data, final Map<String,Integer> fieldIndexMap)
    {
        // SampleId,FusionName,JunctionReadCount,SpanningFragCount,SpliceType,GeneNameUp,GeneIdUp,ChrUp,PosUp,OrientUp,GeneNameDown,GeneIdDown,
        // ChrDown,PosDown,OrientDown,JunctionReads,SpanningFrags,
        // LargeAnchorSupport,FFPM,LeftBreakDinuc,LeftBreakEntropy,RightBreakDinuc,RightBreakEntropy,annots
        final String[] items = data.split(getRnaSourceDelimiter(RNA_FUSION_SOURCE_STARFUSION), -1);

        final String sampleId = items[fieldIndexMap.get("SampleId")];

        final String[] geneIds = new String[FS_PAIR];

        // check that gene names match Ensembl
        final String[] geneNames = new String[] {
                checkAlternateGeneName(items[fieldIndexMap.get("GeneNameUp")]), checkAlternateGeneName(items[fieldIndexMap.get("GeneNameDown")]) };

        final String[] chromosomes = new String[] { items[fieldIndexMap.get("ChrUp")], items[fieldIndexMap.get("ChrDown")] };

        final int[] positions = new int[] {
                Integer.parseInt(items[fieldIndexMap.get("PosUp")]), Integer.parseInt(items[fieldIndexMap.get("PosDown")]) };

        final RnaJunctionType[] junctionTypes = new RnaJunctionType[FS_PAIR];

        if(items[fieldIndexMap.get("SpliceType")].equals("ONLY_REF_SPLICE"))
        {
            junctionTypes[FS_UPSTREAM] = KNOWN;
            junctionTypes[FS_DOWNSTREAM] = KNOWN;
        }

        int junctionCount = Integer.parseInt(items[fieldIndexMap.get("JunctionReadCount")]);
        int spanningCount = Integer.parseInt(items[fieldIndexMap.get("SpanningFragCount")]);

        return new RnaFusionData(
                sampleId, String.valueOf(index), geneIds, geneNames, chromosomes, positions, junctionCount, spanningCount, junctionTypes);
    }

    private static String checkAlternateGeneName(final String geneName)
    {
        if(geneName.equals("AC005152.2"))
            return "SOX9-AS1";

        if(geneName.equals("AC016683.6"))
            return "PAX8-AS1";

        if(geneName.equals("AC007092.1"))
            return "LINC01122";

        if(geneName.toUpperCase().equals("C10ORF112"))
            return "MALRD1";

        if(geneName.equals("C5orf50"))
            return "SMIM23";

        if(geneName.equals("C10orf68"))
            return geneName.toUpperCase();

        if(geneName.equals("C17orf76-AS1"))
            return "FAM211A-AS1";

        if(geneName.equals("IGH@") || geneName.equals("IGH-@"))
            return "IGHJ6";

        if(geneName.equals("IGL@") || geneName.equals("IGL-@"))
            return "IGLC6";

        if(geneName.equals("MKLN1-AS1"))
            return "LINC-PINT";

        if(geneName.equals("PHF15"))
            return "JADE2";

        if(geneName.equals("PHF17"))
            return "JADE1";

        if(geneName.equals("RP11-134P9.1"))
            return "LINC01136";

        if(geneName.equals("RP11-973F15.1"))
            return "LINC01151";

        if(geneName.equals("RP11-115K3.2"))
            return "YWHAEP7";

        if(geneName.equals("RP11-3B12.1"))
            return "POT1-AS1";

        if(geneName.equals("RP11-199O14.1"))
            return "CASC20";

        if(geneName.equals("RP11-264F23.3"))
            return "CCND2-AS1";

        if(geneName.equals("RP11-93L9.1"))
            return "LINC01091";

        return geneName;
    }


}
