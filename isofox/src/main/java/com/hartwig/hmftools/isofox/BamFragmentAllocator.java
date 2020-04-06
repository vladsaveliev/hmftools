package com.hartwig.hmftools.isofox;

import static java.lang.Math.max;
import static java.lang.Math.min;

import static com.hartwig.hmftools.common.utils.io.FileWriterUtils.createBufferedWriter;
import static com.hartwig.hmftools.isofox.IsofoxConfig.ISF_LOGGER;
import static com.hartwig.hmftools.isofox.common.FragmentType.ALT;
import static com.hartwig.hmftools.isofox.common.FragmentType.CHIMERIC;
import static com.hartwig.hmftools.isofox.common.FragmentType.DUPLICATE;
import static com.hartwig.hmftools.isofox.common.FragmentType.READ_THROUGH;
import static com.hartwig.hmftools.isofox.common.FragmentType.TOTAL;
import static com.hartwig.hmftools.isofox.common.FragmentType.TRANS_SUPPORTING;
import static com.hartwig.hmftools.isofox.common.FragmentType.UNSPLICED;
import static com.hartwig.hmftools.isofox.common.ReadRecord.calcFragmentLength;
import static com.hartwig.hmftools.isofox.common.ReadRecord.findOverlappingRegions;
import static com.hartwig.hmftools.isofox.common.ReadRecord.generateMappedCoords;
import static com.hartwig.hmftools.isofox.common.ReadRecord.getUniqueValidRegion;
import static com.hartwig.hmftools.isofox.common.ReadRecord.hasSkippedExons;
import static com.hartwig.hmftools.isofox.common.ReadRecord.markRegionBases;
import static com.hartwig.hmftools.isofox.common.ReadRecord.validRegionMatchType;
import static com.hartwig.hmftools.isofox.common.ReadRecord.validTranscriptType;
import static com.hartwig.hmftools.isofox.common.RegionMatchType.EXON_INTRON;
import static com.hartwig.hmftools.isofox.common.RnaUtils.deriveCommonRegions;
import static com.hartwig.hmftools.isofox.common.TransMatchType.OTHER_TRANS;
import static com.hartwig.hmftools.isofox.common.TransMatchType.SPLICE_JUNCTION;
import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_END;

import static com.hartwig.hmftools.common.utils.sv.StartEndIterator.SE_START;
import static com.hartwig.hmftools.isofox.adjusts.GcRatioCounts.calcGcRatioFromReadRegions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.genome.region.GenomeRegion;
import com.hartwig.hmftools.common.utils.PerformanceCounter;
import com.hartwig.hmftools.isofox.common.BamSlicer;
import com.hartwig.hmftools.isofox.common.FragmentMatchType;
import com.hartwig.hmftools.isofox.common.FragmentTracker;
import com.hartwig.hmftools.isofox.common.GeneCollection;
import com.hartwig.hmftools.isofox.common.FragmentType;
import com.hartwig.hmftools.isofox.common.GeneReadData;
import com.hartwig.hmftools.isofox.common.ReadRecord;
import com.hartwig.hmftools.isofox.common.RegionMatchType;
import com.hartwig.hmftools.isofox.common.RegionReadData;
import com.hartwig.hmftools.isofox.common.TransMatchType;
import com.hartwig.hmftools.isofox.exp_rates.CategoryCountsData;
import com.hartwig.hmftools.isofox.adjusts.GcRatioCounts;
import com.hartwig.hmftools.isofox.novel.AltSpliceJunctionFinder;
import com.hartwig.hmftools.isofox.novel.RetainedIntronFinder;
import com.hartwig.hmftools.isofox.results.ResultsWriter;

import org.jetbrains.annotations.NotNull;
import htsjdk.samtools.CigarOperator;
import htsjdk.samtools.QueryInterval;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;

public class BamFragmentAllocator
{
    private final IsofoxConfig mConfig;
    private final SamReader mSamReader;
    private final BamSlicer mBamSlicer;

    // state relating to the current gene
    private GeneCollection mCurrentGenes;
    private final FragmentTracker mFragmentReads; // delay processing of read until both have been read

    private int mGeneReadCount;
    private int mTotalBamReadCount;
    private int mTotalDuplicateCount;

    public static final int DEFAULT_MIN_MAPPING_QUALITY = 1;

    private final Map<Long,List<long[]>> mDuplicateCache;
    private final List<String> mDuplicateReadIds;

    private final List<CategoryCountsData> mTransComboData;
    private final AltSpliceJunctionFinder mAltSpliceJunctionFinder;
    private final RetainedIntronFinder mRetainedIntronFinder;

    private final BufferedWriter mReadDataWriter;
    private final GcRatioCounts mGcRatioCounts;
    private final GcRatioCounts mGeneGcRatioCounts;

    public BamFragmentAllocator(final IsofoxConfig config, final ResultsWriter resultsWriter)
    {
        mConfig = config;

        // mCurrentGene = null;
        mCurrentGenes = null;
        mFragmentReads = new FragmentTracker();
        mTransComboData = Lists.newArrayList();

        mGeneReadCount = 0;
        mTotalBamReadCount = 0;
        mTotalDuplicateCount = 0;

        mSamReader = mConfig.BamFile != null ?
                SamReaderFactory.makeDefault().referenceSequence(mConfig.RefGenomeFile).open(new File(mConfig.BamFile)) : null;

        mBamSlicer = new BamSlicer(DEFAULT_MIN_MAPPING_QUALITY, false);

        mDuplicateCache = Maps.newHashMap();
        mDuplicateReadIds = Lists.newArrayList();

        mReadDataWriter = resultsWriter.getReadDataWriter();
        mGcRatioCounts = mConfig.requireGcRatioCalcs() ? new GcRatioCounts() : null;
        mGeneGcRatioCounts = mConfig.requireGcRatioCalcs() ? new GcRatioCounts() : null;

        mAltSpliceJunctionFinder = new AltSpliceJunctionFinder(mConfig, resultsWriter.getAltSpliceJunctionWriter());
        mRetainedIntronFinder = new RetainedIntronFinder(resultsWriter.getRetainedIntronWriter());
    }

    public int totalReadCount() { return mTotalBamReadCount; }
    public int duplicateReadCount() { return mTotalDuplicateCount; }
    public final GcRatioCounts getGcRatioCounts() { return mGcRatioCounts; }
    public final GcRatioCounts getGeneGcRatioCounts() { return mGeneGcRatioCounts; }

    public void produceBamCounts(final GeneCollection geneCollection, final GenomeRegion genomeRegion)
    {
        mFragmentReads.clear();

        clearDuplicates();

        mCurrentGenes = geneCollection;
        mGeneReadCount = 0;
        mTransComboData.clear();
        mAltSpliceJunctionFinder.setGeneData(mCurrentGenes);
        mRetainedIntronFinder.setGeneData(mCurrentGenes);

        if(mGeneGcRatioCounts != null)
            mGeneGcRatioCounts.clearCounts();

        mBamSlicer.slice(mSamReader, Lists.newArrayList(genomeRegion), this::processSamRecord);

        ISF_LOGGER.debug("gene({}) bamReadCount({})", mCurrentGenes.geneNames(), mGeneReadCount);
    }

    public void annotateNovelLocations()
    {
        recordNovelLocationReadDepth();
        mAltSpliceJunctionFinder.prioritiseGenes();
        mAltSpliceJunctionFinder.writeAltSpliceJunctions();
        mRetainedIntronFinder.writeRetainedIntrons();
    }

    private void processSamRecord(@NotNull final SAMRecord record)
    {
        if(checkDuplicates(record))
        {
            ++mTotalDuplicateCount;

            if(mConfig.DropDuplicates)
            {
                if(record.getFirstOfPairFlag())
                    mCurrentGenes.addCount(DUPLICATE, 1);

                return;
            }
        }

        ++mTotalBamReadCount;
        ++mGeneReadCount;

        processRead(ReadRecord.from(record));
    }

    public void processRead(ReadRecord read)
    {
        // for each record find all exons with an overlap
        // skip records if either end isn't in one of the exons for this gene

        if(mGeneReadCount > 0 && (mGeneReadCount % 100000) == 0)
        {
            ISF_LOGGER.debug("genes({}) bamRecordCount({})", mCurrentGenes.geneNames(), mGeneReadCount);
        }

        if(mConfig.GeneReadLimit > 0 && mGeneReadCount >= mConfig.GeneReadLimit)
        {
            if(mGeneReadCount == mConfig.GeneReadLimit)
            {
                mBamSlicer.haltProcessing();
                ISF_LOGGER.warn("genes({}) readCount({}) exceeds max read count", mCurrentGenes.geneNames(), mGeneReadCount);
            }

            return;
        }

        if(read.isTranslocation())
        {
            if(read.IsFirstOfPair)
            {
                mCurrentGenes.addCount(TOTAL, 1);
                mCurrentGenes.addCount(CHIMERIC, 1);

                if(read.IsDuplicate)
                    mCurrentGenes.addCount(DUPLICATE, 1);
            }
            return;
        }

        final List<RegionReadData> overlappingRegions = findOverlappingRegions(mCurrentGenes.getExonRegions(), read);

        if (!overlappingRegions.isEmpty())
        {
            read.processOverlappingRegions(overlappingRegions);
        }

        checkFragmentRead(read);
    }

    private boolean checkFragmentRead(ReadRecord read)
    {
        // check if the 2 reads from a fragment exist and if so handle them a pair, returning true
        if(read.isTranslocation())
            return false;

        ReadRecord otherRead = mFragmentReads.checkRead(read);

        if(otherRead != null)
        {
            processFragmentReads(read, otherRead);
            return true;
        }

        return false;
    }

    private void processFragmentReads(final ReadRecord read1, final ReadRecord read2)
    {
        /* process the pair of reads from a fragment:
            - fully outside the gene (due to the buffer used, ignore
            - read through a gene ie start or end outside
            - purely intronic
            - chimeric an inversion or translocation
            - supporting 1 or more transcripts
                - both reads fully with an exon - if exon has only 1 transcript then consider unambiguous
                - both reads within 2 exons (including spanning intermediary ones) and/or either exon at the boundary
            - not supporting any transcript - eg alternative splice sites or unspliced reads
        */

        final long[] genesRange = mCurrentGenes.regionBounds();
        boolean r1OutsideGene = read1.PosStart > genesRange[SE_END] || read1.PosEnd < genesRange[SE_START];
        boolean r2OutsideGene = read2.PosStart > genesRange[SE_END] || read2.PosEnd < genesRange[SE_START];

        if(r1OutsideGene && r2OutsideGene)
            return;

        if(read1.IsDuplicate || read2.IsDuplicate)
            mCurrentGenes.addCount(DUPLICATE, 1);

        long readPosMin = min(read1.PosStart, read2.PosStart);
        long readPosMax = max(read1.PosEnd, read2.PosEnd);

        final List<GeneReadData> overlapGenes = mCurrentGenes.findGenesCoveringRange(readPosMin, readPosMax);
        mCurrentGenes.addCount(TOTAL, 1);

        if(read1.isLocalInversion() || read2.isLocalInversion())
        {
            mCurrentGenes.addCount(CHIMERIC, 1);
            return;
        }

        if(read1.getMappedRegions().isEmpty() && read2.getMappedRegions().isEmpty())
        {
            // fully intronic read in every transcript and gene
            processIntronicReads(overlapGenes, read1, read2);
            return;
        }

        final Map<Integer,TransMatchType> firstReadTransTypes = read1.getTranscriptClassifications();
        final Map<Integer,TransMatchType> secondReadTransTypes = read2.getTranscriptClassifications();

        // first find valid transcripts in both reads
        final List<Integer> validTranscripts = Lists.newArrayList();
        final List<Integer> invalidTranscripts = Lists.newArrayList();
        int calcFragmentLength = calcFragmentLength(read1, read2);
        boolean validFragmentLength = calcFragmentLength <= mConfig.MaxFragmentLength;

        final List<RegionReadData> validRegions = getUniqueValidRegion(read1, read2);

        if(mConfig.RunValidations)
        {
            for(RegionReadData region : validRegions)
            {
                if(validRegions.stream().filter(x -> x == region).count() > 1)
                {
                    ISF_LOGGER.error("repeated exon region({})", region);
                }
            }
        }

        for(Map.Entry<Integer,TransMatchType> entry : firstReadTransTypes.entrySet())
        {
            int transId = entry.getKey();

            if(validFragmentLength && validTranscriptType(entry.getValue()))
            {
                if(secondReadTransTypes.containsKey(transId) && validTranscriptType(secondReadTransTypes.get(transId)))
                {
                    if(!hasSkippedExons(validRegions, transId, mConfig.MaxFragmentLength))
                    {
                        validTranscripts.add(transId);
                        continue;
                    }
                }
            }

            if(!invalidTranscripts.contains(transId))
                invalidTranscripts.add(transId);
        }

        for(Integer transId : secondReadTransTypes.keySet())
        {
            if(!validTranscripts.contains(transId) && !invalidTranscripts.contains(transId))
                invalidTranscripts.add(transId);
        }

        FragmentType fragmentType = UNSPLICED;

        // now mark all other transcripts which aren't valid either due to the read pair
        if(validTranscripts.isEmpty())
        {
            // no valid transcripts but record against the gene further information about these reads
            boolean checkRetainedIntrons = false;

            if(r1OutsideGene || r2OutsideGene)
            {
                fragmentType = READ_THROUGH;
            }
            else if(read1.containsSplit() || read2.containsSplit())
            {
                fragmentType = ALT;
                mAltSpliceJunctionFinder.evaluateFragmentReads(overlapGenes, read1, read2, invalidTranscripts);
                checkRetainedIntrons = true;
            }
            else
            {
                // look for alternative splicing from long reads involving more than one region and not spanning into an intron
                for(int transId : invalidTranscripts)
                {
                    List<RegionReadData> regions = read1.getMappedRegions().entrySet().stream()
                            .filter(x -> x.getKey().hasTransId(transId))
                            .filter(x -> x.getValue() != EXON_INTRON)
                            .map(x -> x.getKey()).collect(Collectors.toList());;

                    final List<RegionReadData> regions2 = read2.getMappedRegions().entrySet().stream()
                            .filter(x -> x.getKey().hasTransId(transId))
                            .filter(x -> x.getValue() != EXON_INTRON)
                            .map(x -> x.getKey()).collect(Collectors.toList());

                    for(RegionReadData region : regions2)
                    {
                        if (!regions.contains(region))
                            regions.add(region);
                    }

                    if(regions.size() > 1)
                    {
                        fragmentType = ALT;
                        break;
                    }
                }

                checkRetainedIntrons = true;
            }

            if(checkRetainedIntrons)
                mRetainedIntronFinder.evaluateFragmentReads(read1, read2);

            if(fragmentType == UNSPLICED)
            {
                List<String> unsplicedGeneIds = overlapGenes.stream().map(x -> x.GeneData.GeneId).collect(Collectors.toList());

                CategoryCountsData catCounts = getCategoryCountsData(validTranscripts, unsplicedGeneIds);
                final List<long[]> commonMappings = deriveCommonRegions(read1.getMappedRegionCoords(), read2.getMappedRegionCoords());
                addGcCounts(catCounts, commonMappings);
            }
        }
        else
        {
            // record valid read info against each region now that it is known
            fragmentType = TRANS_SUPPORTING;

            // first mark any invalid trans as 'other' meaning it doesn't require any further classification since a valid trans exists
            firstReadTransTypes.entrySet().stream()
                    .filter(x -> validTranscriptType(x.getValue()))
                    .filter(x -> !validTranscripts.contains(x.getKey()))
                    .forEach(x -> x.setValue(OTHER_TRANS));

            secondReadTransTypes.entrySet().stream()
                    .filter(x -> validTranscriptType(x.getValue()))
                    .filter(x -> !validTranscripts.contains(x.getKey()))
                    .forEach(x -> x.setValue(OTHER_TRANS));

            // now record the bases covered by the read in these matched regions
            final List<long[]> commonMappings = deriveCommonRegions(read1.getMappedRegionCoords(), read2.getMappedRegionCoords());

            if(mConfig.RunValidations)
            {
                for(long[] readRegion : commonMappings)
                {
                    if(commonMappings.stream().filter(x -> x[SE_START] == readRegion[SE_START] && x[SE_END] == readRegion[SE_END]).count() > 1)
                    {
                        ISF_LOGGER.error("repeated read region({} -> {})", readRegion[SE_START], readRegion[SE_END]);
                    }
                }
            }

            validRegions.forEach(x -> markRegionBases(commonMappings, x));

            // now set counts for each valid transcript
            boolean isUniqueTrans = validTranscripts.size() == 1;

            FragmentMatchType comboTransMatchType = FragmentMatchType.SHORT;

            for (int transId : validTranscripts)
            {
                int regionCount = (int)validRegions.stream().filter(x -> x.hasTransId(transId)).count();

                FragmentMatchType transMatchType;

                if(read1.getTranscriptClassification(transId) == SPLICE_JUNCTION || read2.getTranscriptClassification(transId) == SPLICE_JUNCTION)
                {
                    transMatchType = FragmentMatchType.SPLICED;
                    comboTransMatchType = FragmentMatchType.SPLICED;
                }
                else if(regionCount > 1)
                {
                    transMatchType = FragmentMatchType.LONG;

                    if(comboTransMatchType != FragmentMatchType.SPLICED)
                        comboTransMatchType = FragmentMatchType.LONG;
                }
                else
                {
                    transMatchType = FragmentMatchType.SHORT;
                }

                mCurrentGenes.addTranscriptReadMatch(transId, isUniqueTrans, transMatchType);

                // keep track of which regions have been allocated from this fragment as a whole, so not counting each read separately
                final List<RegionReadData> processedRegions = Lists.newArrayList();

                processValidTranscript(transId, read1, processedRegions, isUniqueTrans);
                processValidTranscript(transId, read2, processedRegions, isUniqueTrans);
            }

            List<String> unsplicedGeneIds = comboTransMatchType == FragmentMatchType.SHORT ?
                    overlapGenes.stream().map(x -> x.GeneData.GeneId).collect(Collectors.toList()) : Lists.newArrayList();

            CategoryCountsData catCounts = getCategoryCountsData(validTranscripts, unsplicedGeneIds);
            addGcCounts(catCounts, commonMappings);
        }

        mCurrentGenes.addCount(fragmentType, 1);

        if(mConfig.WriteReadData && mReadDataWriter != null)
        {
            for(final GeneReadData geneReadData : overlapGenes)
            {
                writeReadData(mReadDataWriter, geneReadData, 0, read1, fragmentType, validTranscripts.size(), calcFragmentLength);
                writeReadData(mReadDataWriter, geneReadData, 1, read2, fragmentType, validTranscripts.size(), calcFragmentLength);
            }
        }
    }

    public List<CategoryCountsData> getTransComboData() { return mTransComboData; }

    private CategoryCountsData getCategoryCountsData(final List<Integer> transcripts, final List<String> geneIds)
    {
        CategoryCountsData transComboCounts = mTransComboData.stream()
                .filter(x -> x.matches(transcripts, geneIds)).findFirst().orElse(null);

        if(transComboCounts == null)
        {
            transComboCounts = new CategoryCountsData(transcripts, geneIds);

            if(mGcRatioCounts != null && mConfig.ApplyGcBiasAdjust)
                transComboCounts.initialiseGcRatioCounts(mGcRatioCounts.getCounts().length);

            mTransComboData.add(transComboCounts);
        }

        return transComboCounts;
    }

    private void processValidTranscript(
            int transId, final ReadRecord read, final List<RegionReadData> processedRegions, boolean isUniqueTrans)
    {
        List<RegionReadData> regions = read.getMappedRegions().entrySet().stream()
                .filter(x -> x.getKey().hasTransId(transId))
                .filter(x -> validRegionMatchType(x.getValue()))
                .map(x -> x.getKey()).collect(Collectors.toList());

        for(RegionReadData region : regions)
        {
            if (!processedRegions.contains(region))
            {
                // register a read against this valid transcript region
                region.addTranscriptReadMatch(transId, isUniqueTrans);
            }
        }

        // any adjacent reads can record a splice junction count
        if(regions.size() > 1 && read.getTranscriptClassification(transId) == SPLICE_JUNCTION)
        {
            for(int r1 = 0; r1 < regions.size() - 1; ++r1)
            {
                RegionReadData region1 = regions.get(r1);

                for(int r2 = r1 + 1; r2 < regions.size(); ++r2)
                {
                    RegionReadData region2 = regions.get(r2);

                    if(processedRegions.contains(region1) && processedRegions.contains(region2))
                        continue;

                    if(region1.getPostRegions().contains(region2))
                    {
                        region1.addTranscriptJunctionMatch(transId, SE_END, isUniqueTrans);
                        region2.addTranscriptJunctionMatch(transId, SE_START, isUniqueTrans);
                    }
                    else if(region1.getPreRegions().contains(region2))
                    {
                        region1.addTranscriptJunctionMatch(transId, SE_START, isUniqueTrans);
                        region2.addTranscriptJunctionMatch(transId, SE_END, isUniqueTrans);
                    }
                }
            }
        }

        regions.forEach(x -> processedRegions.add(x));
    }

    private void processIntronicReads(final List<GeneReadData> genes, final ReadRecord read1, final ReadRecord read2)
    {
        if(read1.Cigar.containsOperator(CigarOperator.N) || read2.Cigar.containsOperator(CigarOperator.N))
        {
            mCurrentGenes.addCount(ALT, 1);
            mAltSpliceJunctionFinder.evaluateFragmentReads(genes, read1, read2, Lists.newArrayList());
            return;
        }

        List<String> unsplicedGeneIds = genes.stream().map(x -> x.GeneData.GeneId).collect(Collectors.toList());

        CategoryCountsData catCounts = getCategoryCountsData(Lists.newArrayList(), unsplicedGeneIds);

        List<long[]> readRegions = deriveCommonRegions(read1.getMappedRegionCoords(), read2.getMappedRegionCoords());
        addGcCounts(catCounts, readRegions);

        mCurrentGenes.addCount(UNSPLICED, 1);
    }

    private void addGcCounts(final CategoryCountsData catCounts, List<long[]> readRegions)
    {
        if(mGcRatioCounts != null)
        {
            double gcRatio = calcGcRatioFromReadRegions(mConfig.RefFastaSeqFile, mCurrentGenes.chromosome(), readRegions);

            int[] gcRatioIndex = { -1, -1 };
            double[] gcRatioCounts = { 0, 0 };
            mGcRatioCounts.determineRatioData(gcRatio, gcRatioIndex, gcRatioCounts);

            for(int i = 0; i < gcRatioIndex.length; ++i)
            {
                if (gcRatioIndex[i] >= 0)
                {
                    mGcRatioCounts.addGcRatioCount(gcRatioIndex[i], gcRatioCounts[i]);
                    mGeneGcRatioCounts.addGcRatioCount(gcRatioIndex[i], gcRatioCounts[i]);
                }
            }

            if(mConfig.ApplyGcBiasAdjust)
                catCounts.addGcRatioCounts(1, gcRatioIndex, gcRatioCounts);
            else
                catCounts.addCounts(1);
        }
        else
        {
            catCounts.addCounts(1);
        }
    }

    // read depth count state
    private PerformanceCounter mReadDepthPerf = new PerformanceCounter("NovelSites ReadDepth");
    private FragmentTracker mFragmentTracker = new FragmentTracker();

    private void recordNovelLocationReadDepth()
    {
        if(mAltSpliceJunctionFinder.getAltSpliceJunctions().isEmpty() && mRetainedIntronFinder.getRetainedIntrons().isEmpty())
            return;

        BamSlicer slicer = new BamSlicer(DEFAULT_MIN_MAPPING_QUALITY, true);

        QueryInterval[] queryInterval = new QueryInterval[1];
        int chrSeqIndex = mSamReader.getFileHeader().getSequenceIndex(mCurrentGenes.chromosome());

        mFragmentTracker.clear();

        mReadDepthPerf.start();

        int[] readRange = {0, 0};

        if(!mAltSpliceJunctionFinder.getAltSpliceJunctions().isEmpty() && !mRetainedIntronFinder.getRetainedIntrons().isEmpty())
        {
            int[] asjPositionsRange = mAltSpliceJunctionFinder.getPositionsRange();
            int[] riPositionsRange = mRetainedIntronFinder.getPositionsRange();

            readRange[SE_START] = min(asjPositionsRange[SE_START], riPositionsRange[SE_START]);
            readRange[SE_END] = max(asjPositionsRange[SE_END], riPositionsRange[SE_END]);
        }
        else if(!mAltSpliceJunctionFinder.getAltSpliceJunctions().isEmpty())
        {
            readRange = mAltSpliceJunctionFinder.getPositionsRange();
        }
        else
        {
            readRange = mRetainedIntronFinder.getPositionsRange();
        }

        queryInterval[0] = new QueryInterval(chrSeqIndex, readRange[SE_START], readRange[SE_END]);

        slicer.slice(mSamReader, queryInterval, this::setPositionDepthFromRead);

        // finally feed through any unmatched reads (eg if the other read is out of the specified range)
        for(Object object : mFragmentTracker.getValues())
        {
            final List<long[]> readCoords = (List<long[]>)object;
            mAltSpliceJunctionFinder.setPositionDepthFromRead(readCoords);
            mRetainedIntronFinder.setPositionDepthFromRead(readCoords);
        }

        mReadDepthPerf.stop();
    }

    public void setPositionDepthFromRead(@NotNull final SAMRecord record)
    {
        final List<long[]> readCoords = generateMappedCoords(record.getCigar(), record.getStart());
        final List<long[]> otherReadCoords = (List<long[]>)mFragmentTracker.checkRead(record.getReadName(), readCoords);

        if(otherReadCoords == null)
            return;

        final List<long[]> commonMappings = deriveCommonRegions(readCoords, otherReadCoords);

        mAltSpliceJunctionFinder.setPositionDepthFromRead(commonMappings);
        mRetainedIntronFinder.setPositionDepthFromRead(commonMappings);
    }

    private static final int DUP_DATA_SECOND_START = 0;
    private static final int DUP_DATA_READ_LEN = 1;
    private static final int DUP_DATA_INSERT_SIZE = 2;

    public boolean checkDuplicates(final SAMRecord record)
    {
        if(record.getDuplicateReadFlag())
            return true;

        if(!mConfig.MarkDuplicates)
            return false;

        if(mDuplicateReadIds.contains(record.getReadName()))
        {
            mDuplicateReadIds.remove(record.getReadName());
            return true;
        }

        if(!record.getReferenceName().equals(record.getMateReferenceName()) || record.getReadNegativeStrandFlag() == record.getMateNegativeStrandFlag())
            return false;

        long firstStartPos = record.getFirstOfPairFlag() ? record.getStart() : record.getMateAlignmentStart();
        long secondStartPos = record.getFirstOfPairFlag() ? record.getMateAlignmentStart() : record.getStart();
        int readLength = record.getReadLength();
        int insertSize = record.getInferredInsertSize();

        List<long[]> dupDataList = mDuplicateCache.get(firstStartPos);

        if(dupDataList == null)
        {
            dupDataList = Lists.newArrayList();
            mDuplicateCache.put(firstStartPos, dupDataList);
        }
        else
        {
            // search for a match
            if(dupDataList.stream().anyMatch(x -> x[DUP_DATA_SECOND_START] == secondStartPos
                    && x[DUP_DATA_READ_LEN] == readLength && insertSize == x[DUP_DATA_INSERT_SIZE]))
            {
                ISF_LOGGER.trace("duplicate fragment: id({}) chr({}) pos({}->{}) otherReadStart({}) insertSize({})",
                        record.getReadName(), record.getReferenceName(), firstStartPos, record.getEnd(), secondStartPos, insertSize);

                // cache so the second read can be identified immediately
                mDuplicateReadIds.add(record.getReadName());
                return true;
            }
        }

        long[] dupData = {secondStartPos, readLength, insertSize};
        dupDataList.add(dupData);

        return false;
    }

    private void clearDuplicates()
    {
        mDuplicateCache.clear();
        mDuplicateReadIds.clear();
    }

    public static BufferedWriter createReadDataWriter(final IsofoxConfig config)
    {
        try
        {
            final String outputFileName = config.formOutputFile("read_data.csv");

            BufferedWriter writer = createBufferedWriter(outputFileName, false);
            writer.write("GeneId,GeneName,ReadIndex,ReadId,Chromosome,PosStart,PosEnd,Cigar,InsertSize,FragLength");
            writer.write(",GeneClass,TransId,TransClass,ValidTrans,ExonRank,ExonStart,ExonEnd,RegionClass");
            writer.newLine();
            return writer;
        }
        catch (IOException e)
        {
            ISF_LOGGER.error("failed to create read data writer: {}", e.toString());
            return null;
        }
    }

    private synchronized static void writeReadData(
            final BufferedWriter writer, final GeneReadData geneReadData, int readIndex, final ReadRecord read,
            FragmentType geneReadType, int validTranscripts, int calcFragmentLength)
    {
        if(read.getTranscriptClassifications().isEmpty())
            return;

        try
        {
            for(Map.Entry<Integer,TransMatchType> entry : read.getTranscriptClassifications().entrySet())
            {
                int transId = entry.getKey();
                TransMatchType transType = entry.getValue();

                for(Map.Entry<RegionReadData, RegionMatchType> rEntry : read.getMappedRegions().entrySet())
                {
                    RegionReadData region = rEntry.getKey();
                    RegionMatchType matchType = rEntry.getValue();

                    if(!region.hasTransId(transId))
                        continue;

                    writer.write(String.format("%s,%s,%d,%s",
                            geneReadData.GeneData.GeneId, geneReadData.GeneData.GeneName, readIndex, read.Id));

                    writer.write(String.format(",%s,%d,%d,%s,%d,%d",
                            read.Chromosome, read.PosStart, read.PosEnd, read.Cigar.toString(),
                            read.fragmentInsertSize(), calcFragmentLength));

                    writer.write(String.format(",%s,%d,%s,%s,%d,%d,%d,%s",
                            geneReadType, transId, transType, validTranscripts,
                            region.getExonRank(transId), region.start(), region.end(), matchType));

                    writer.newLine();
                }
            }
        }
        catch(IOException e)
        {
            ISF_LOGGER.error("failed to write read data file: {}", e.toString());
        }
    }

    @VisibleForTesting
    public void processReadRecords(final GeneCollection geneCollection, final List<ReadRecord> readRecords)
    {
        mCurrentGenes = geneCollection;
        mAltSpliceJunctionFinder.setGeneData(mCurrentGenes);
        mRetainedIntronFinder.setGeneData(mCurrentGenes);
        readRecords.forEach(x -> processRead(x));
    }


}