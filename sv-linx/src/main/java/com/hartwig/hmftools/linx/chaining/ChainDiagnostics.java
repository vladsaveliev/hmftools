package com.hartwig.hmftools.linx.chaining;

import static java.lang.Math.max;

import static com.hartwig.hmftools.common.io.FileWriterUtils.closeBufferedWriter;
import static com.hartwig.hmftools.common.io.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.linx.analysis.SvUtilities.appendStr;
import static com.hartwig.hmftools.linx.analysis.SvUtilities.formatPloidy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.linx.types.SvBreakend;
import com.hartwig.hmftools.linx.types.SvLinkedPair;
import com.hartwig.hmftools.linx.types.SvVarData;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChainDiagnostics
{
    private List<String> mLogMessages;
    private String mOutputDir;
    private int mMaxClusterSize;
    private BufferedWriter mFileWriter;

    private final List<SvVarData> mInitialComplexDup;
    private final List<SvVarData> mInitialFoldbacks;
    private int mClusterCount;
    private int mUnlinkedBreakendCount;
    private int mUnlinkedSvCount;
    private int mWarnings;
    private int mLastProgressIndex;

    // references from chain finder
    private int mClusterId;
    private String mSampleId;
    private boolean mHasReplication;
    private final List<SvChain> mChains;
    private final List<SvChain> mUniqueChains;
    private final Map<SvVarData,SvChainState> mSvConnectionsMap;
    private final List<SvChainState> mSvCompletedConnections;
    private final Map<SvBreakend, List<SvLinkedPair>> mSvBreakendPossibleLinks;
    private final List<SvVarData> mDoubleMinuteSVs;
    private final List<SvLinkedPair> mUniquePairs;

    private static final Logger LOGGER = LogManager.getLogger(ChainDiagnostics.class);

    public ChainDiagnostics(final Map<SvVarData,SvChainState> svConnMap, final List<SvChainState> svCompleteConns,
            final List<SvChain> chains, final List<SvChain> uniqueChains,
            final Map<SvBreakend, List<SvLinkedPair>> svBreakendPossibleLinks,
            final List<SvVarData> doubleMinuteSVs, final List<SvLinkedPair> uniquePairs)
    {
        mLogMessages = Lists.newArrayList();
        mInitialComplexDup = Lists.newArrayList();
        mInitialFoldbacks = Lists.newArrayList();
        mOutputDir = null;
        mFileWriter = null;
        mMaxClusterSize = 0;
        mUnlinkedSvCount = 0;
        mUnlinkedBreakendCount = 0;
        mWarnings = 0;
        mClusterCount = 0;
        mLastProgressIndex = 0;
        mSampleId = "";

        mSvConnectionsMap = svConnMap;
        mSvCompletedConnections = svCompleteConns;
        mChains = chains;
        mUniqueChains = uniqueChains;
        mSvBreakendPossibleLinks = svBreakendPossibleLinks;
        mDoubleMinuteSVs = doubleMinuteSVs;
        mUniquePairs = uniquePairs;
    }

    public void setOutputDir(final String dir, int maxLogSize)
    {
        mOutputDir = dir;
        mMaxClusterSize = maxLogSize;
    }

    public void setSampleId(final String sampleId) { mSampleId = sampleId; }

    public int unlinkedSvCount() { return mUnlinkedSvCount; }

    public void clear()
    {
        mLogMessages.clear();
        mWarnings = 0;
        mClusterCount = 0;
        mLastProgressIndex = 0;
    }

    public void initialise(int clusterId, boolean hasReplication)
    {
        clear();

        mClusterId = clusterId;
        mHasReplication = hasReplication;

        mClusterCount = mSvConnectionsMap.size();
    }

    public void setPriorityData(List<SvVarData> complexDups, final List<SvVarData> foldbacks)
    {
        mInitialFoldbacks.clear();
        mInitialFoldbacks.addAll(foldbacks);
        mInitialComplexDup.clear();
        mInitialComplexDup.addAll(complexDups);
    }

    public void addMessage(final String msg)
    {
        mLogMessages.add(String.format("INFO: %s", msg));
    }

    public void diagnoseChains()
    {
        if(mChains.isEmpty() || mClusterCount < 3 || mClusterCount > mMaxClusterSize)
            return;

        if(!mDoubleMinuteSVs.isEmpty()) // for now skip these
            return;

        List<SvChainState> svConnections = Lists.newArrayList();
        svConnections.addAll(mSvCompletedConnections);
        svConnections.addAll(mSvConnectionsMap.values());

        int invalidBreakends = findMultiConnectionBreakends(svConnections);

        writeResults(svConnections, invalidBreakends);

        if(!LOGGER.isDebugEnabled())
            return;

        if(!mLogMessages.isEmpty())
        {
            LOGGER.debug("diagnostic log:");
            mLogMessages.stream().forEach(x -> LOGGER.debug(x));
        }
    }

    private int findMultiConnectionBreakends(List<SvChainState> svConnections)
    {
        int invalidCount = 0;

        for(final SvChainState svConn : svConnections)
        {
            if(svConn.uniqueConnections(true) <= 1 && svConn.uniqueConnections(false) <= 1)
                continue;

            // are multiple connections explained by foldbacks, DMs or complex DUPs?
            // ignore foldbacks formed by a single breakend and ignore DM SVs
            if(svConn.SV.isSingleBreakendFoldback() || mDoubleMinuteSVs.contains(svConn.SV))
                continue;

            // just take the end with the most connections or either if the same
            boolean useStart = svConn.uniqueConnections(true) > svConn.uniqueConnections(false);

            int connectionCount = svConn.uniqueConnections(useStart);

            int assembledLinks = svConn.SV.getAssembledLinkedPairs(useStart).size();

            if(connectionCount == assembledLinks)
                continue;

            int foldbackCons = 0;
            int compDupCons = 0;
            String otherSVs = "";

            for(final SvBreakend otherBreakend : svConn.getConnections(useStart))
            {
                final SvVarData otherVar = otherBreakend.getSV();

                if(mInitialFoldbacks.contains(otherVar))
                    ++foldbackCons;
                else if(mInitialComplexDup.contains(otherVar))
                    ++compDupCons;

                otherSVs = appendStr(otherSVs, otherVar.idStr(), ';');
            }

            if(connectionCount - foldbackCons - compDupCons > 1)
            {
                LOGGER.debug("cluster({} count={}) breakend({}) rep({}) conns({}) foldbacks({}) compDups({}) asmb({})",
                        mClusterId, mSvConnectionsMap.keySet().size(), svConn.SV.getBreakend(useStart).toString(),
                        svConn.Ploidy, connectionCount, foldbackCons, compDupCons, assembledLinks);

                logCsv("MULTI_CONN", svConn.SV,
                        String.format("conns(%d) ploidy(%s-%s-%s) foldbacks(%d) compDups(%d) asmb(%d) otherSVs(%s)",
                                connectionCount, formatPloidy(svConn.MinPloidy), formatPloidy(svConn.Ploidy), formatPloidy(svConn.MaxPloidy),
                                foldbackCons, compDupCons, assembledLinks, otherSVs));

                ++invalidCount;
            }
        }

        return invalidCount;
    }

    public void logCsv(final String type, final SvVarData var, final String otherInfo)
    {
        if(mMaxClusterSize == 0)
            return;

        LOGGER.info("CHAIN_DIAG: {},{},{},{},{}", type, mSampleId, mClusterId, var.id(), otherInfo);
    }

    public void chainingComplete()
    {
        mUnlinkedBreakendCount = mSvConnectionsMap.values().stream()
                .mapToInt(x -> (x.breakendCount(true) == 0 ? 1 : 0) + (x.breakendCount(false) == 0 ? 1 : 0)).sum();

        mUnlinkedSvCount = (int) mSvConnectionsMap.values().stream()
                .filter(x -> x.breakendCount(true) == 0 && x.breakendCount(false) == 0).count();

        if(!LOGGER.isDebugEnabled())
            return;

        if(mHasReplication)
        {
            double unlinkedPloidyBreakends = mSvConnectionsMap.values().stream()
                .mapToDouble(x -> x.unlinked(true) + x.unlinked(false)).sum();

            double unlinkedPloidySVs = mSvConnectionsMap.values().stream().mapToDouble(x -> x.unlinked()).sum();

            LOGGER.debug("cluster({}) chaining finished: chains({} unique={} links={}) SVs({}) unlinked SVs({} ploidy={}) breakends({} ploidy={})",
                    mClusterId, mChains.size(), mUniqueChains.size(), mUniquePairs.size(), mClusterCount,
                    mUnlinkedSvCount, formatPloidy(unlinkedPloidySVs), mUnlinkedBreakendCount, formatPloidy(unlinkedPloidyBreakends));
        }
        else
        {
            LOGGER.debug("cluster({}) chaining finished: chains({} links={}) SVs({}) unlinked SVs({}) breakends({})",
                    mClusterId, mUniqueChains.size(), mUniquePairs.size(), mClusterCount, mUnlinkedSvCount, mUnlinkedBreakendCount);
        }
    }

    private void writeResults(final List<SvChainState> svConnections, int invalidBreakends)
    {
        if(mOutputDir == null)
            return;

        try
        {
            if (mFileWriter == null)
            {
                String outputFileName = mOutputDir;

                outputFileName += "SVA_CHAINS.csv";

                mFileWriter = createBufferedWriter(outputFileName, false);

                mFileWriter.write("SampleId,ClusterId,Replication,SvCount,PloidyTotal,Chains,RepeatedChains,SGLs,Warnings");
                mFileWriter.write(",MaxPloidy,UnlinksSVs,UnlinkedBEs,InvalidBEs,Foldbacks,CompDups");
                mFileWriter.newLine();
            }

            int sglCount = (int) svConnections.stream().filter(x -> x.SV.isNullBreakend()).count();

            double ploidyTotal = svConnections.stream().mapToDouble(x -> x.Ploidy).sum();

            double maxPloidy = mHasReplication ? svConnections.stream().mapToDouble(x -> x.Ploidy).max().getAsDouble() : 1;

            mFileWriter.write(String.format("%s,%d,%s,%d,%.1f,%d,%d,%d,%d",
                    mSampleId, mClusterId, mHasReplication, mClusterCount, ploidyTotal,
                    mUniqueChains.size(), mChains.size() - mUniqueChains.size(), sglCount, mWarnings));

            mFileWriter.write(String.format(",%.1f,%d,%d,%d,%d,%d",
                    maxPloidy, mUnlinkedSvCount, mUnlinkedBreakendCount, invalidBreakends,
                    mInitialFoldbacks.size(), mInitialComplexDup.size()));

            mFileWriter.newLine();
        }
        catch (final IOException e)
        {
            LOGGER.error("error writing DM data: {}", e.toString());
        }
    }

    public void close()
    {
        closeBufferedWriter(mFileWriter);
    }

    public void checkProgress(int linkIndex)
    {
        if(!LOGGER.isDebugEnabled())
            return;

        if(!mHasReplication || mSvConnectionsMap.size() < 100)
            return;

        if(linkIndex >= mLastProgressIndex + 100)
        {
            mLastProgressIndex = linkIndex;
            LOGGER.debug("cluster({}) chaining progress: index({}) SVs(incomplete={} complete={}) partialChains({})",
                    mClusterId, linkIndex, mSvConnectionsMap.size(), mSvCompletedConnections.size(), mChains.size());
        }
    }

    public boolean checkHasValidState(int linkIndex)
    {
        // first check that the remaining possible links are supported by unlinked breakends
        boolean isValid = true;

        for(Map.Entry<SvBreakend,List<SvLinkedPair>> entry : mSvBreakendPossibleLinks.entrySet())
        {
            SvBreakend breakend = entry.getKey();

            List<SvBreakend> breakendList = Lists.newArrayList(); // mUnlinkedBreakendMap.get(breakend);

            if(breakendList == null)
            {
                LOGGER.error("cluster({}) runIndex({}): breakend({}) has {} possible pairs but no available breakends",
                        mClusterId, linkIndex, breakend.toString(), entry.getValue().size());

                isValid = false;
            }
        }

        return isValid;
    }

}
