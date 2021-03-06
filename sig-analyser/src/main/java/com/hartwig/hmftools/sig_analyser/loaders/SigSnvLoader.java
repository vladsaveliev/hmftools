package com.hartwig.hmftools.sig_analyser.loaders;

import static com.hartwig.hmftools.common.utils.io.FileWriterUtils.createBufferedWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.variant.SomaticVariant;
import com.hartwig.hmftools.common.variant.VariantType;
import com.hartwig.hmftools.patientdb.dao.DatabaseAccess;
import com.hartwig.hmftools.common.sigs.SigMatrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SigSnvLoader
{
    private final VariantFilters mFilters;
    private final List<String> mSampleIds;

    private final Map<String,Integer> mBucketStringToIndex;
    private SigMatrix mSampleBucketCounts;

    private PositionFrequencies mPositionFrequencies;

    private static final int SNV_BUCKET_COUNT = 96;

    private static final Logger LOGGER = LogManager.getLogger(SigSnvLoader.class);

    public SigSnvLoader(final VariantFilters filters, final List<String> sampleIds)
    {
        mFilters = filters;
        mSampleIds = sampleIds;

        mBucketStringToIndex = Maps.newHashMap();
        buildBucketMap();

        mPositionFrequencies = null;
    }

    public void initialisePositionFrequencies(final String outputDir, final int bucketSize)
    {
        mPositionFrequencies = new PositionFrequencies(outputDir, bucketSize);
    }

    public SigMatrix getSampleBucketCounts() { return mSampleBucketCounts; }

    private void buildBucketMap()
    {
        char[] refBases = {'C', 'T'};
        char[] bases = {'A','C', 'G', 'T'};
        int index = 0;

        for(int i = 0; i < refBases.length; ++i)
        {
            char ref = refBases[i];

            for(int j = 0; j < bases.length; ++j)
            {
                char alt = bases[j];

                if(ref != alt)
                {
                    String baseChange = String.format("%c>%c", ref, alt);

                    for (int k = 0; k < bases.length; ++k)
                    {
                        char before = bases[k];

                        for (int l = 0; l < bases.length; ++l)
                        {
                            char after = bases[l];

                            String context = String.format("%c%c%c", before, ref, after);

                            String bucketName = baseChange + "_" + context;

                            mBucketStringToIndex.put(bucketName, index);
                            ++index;
                        }
                    }
                }
            }
        }
    }

    public void loadData(final DatabaseAccess dbAccess)
    {
        mSampleBucketCounts = new SigMatrix(SNV_BUCKET_COUNT, mSampleIds.size());

        LOGGER.info("retrieving SNV data for {} samples", mSampleIds.size());

        for(int sampleIndex = 0; sampleIndex < mSampleIds.size(); ++sampleIndex)
        {
            String sampleId = mSampleIds.get(sampleIndex);
            final List<SomaticVariant> variants = dbAccess.readSomaticVariants(sampleId, VariantType.SNP);

            LOGGER.info("sample({}:{}) processing {} variants", sampleIndex, sampleId, variants.size());

            processSampleVariants(sampleId, variants, sampleIndex);

            if(mPositionFrequencies != null)
            {
                mPositionFrequencies.writeResults(sampleId);
                mPositionFrequencies.clear();
            }
        }

        if(mPositionFrequencies != null)
            mPositionFrequencies.close();
    }

    public void writeSampleCounts(final String filename)
    {
        try
        {
            BufferedWriter writer = createBufferedWriter(filename, false);

            writer.write("BucketName");

            for(int i = 0; i < mSampleIds.size(); ++i)
            {
                writer.write(String.format(",%s", mSampleIds.get(i)));
            }

            writer.newLine();

            double[][] scData = mSampleBucketCounts.getData();

            for(int i = 0; i < mSampleBucketCounts.Rows; ++i)
            {
                writer.write(getBucketNameByIndex(mBucketStringToIndex, i));

                for(int j = 0; j < mSampleBucketCounts.Cols; ++j) {

                    writer.write(String.format(",%.0f", scData[i][j]));
                }

                writer.newLine();
            }

            writer.close();
        }
        catch (final IOException e)
        {
            LOGGER.error("error writing to outputFile: {}", e.toString());
        }
    }

    private void processSampleVariants(final String sampleId, List<SomaticVariant> variants, int sampleIndex)
    {
        /* required fields
        - chromosome
        - position
        - ref
        - alt
        - trinucleotideContext
        - variantCopyNumber
        - subclonalLikelihood
        */

        double[][] sampleCounts = mSampleBucketCounts.getData();

        for(final SomaticVariant variant : variants)
        {
            if(variant.isFiltered() || !variant.isSnp())
                continue;

            if(variant.alt().length() != 1)
                continue;

            String rawContext = variant.trinucleotideContext();

            if(rawContext.contains("N"))
                continue;

            // check filters
            if(mFilters != null && !mFilters.passesFilters(variant))
                continue;

            if(mPositionFrequencies != null)
                mPositionFrequencies.addPosition(variant.chromosome(), (int)variant.position());

            // convert base change to standard set and the context accordingly
            String baseChange;
            String context;
            if(variant.ref().charAt(0) == 'A' || variant.ref().charAt(0) == 'G')
            {
                baseChange = String.format("%c>%c", convertBase(variant.ref().charAt(0)), convertBase(variant.alt().charAt(0)));

                // convert the context as well
                context = String.format("%c%c%c",
                        convertBase(rawContext.charAt(2)), convertBase(rawContext.charAt(1)), convertBase(rawContext.charAt(0)));
            }
            else
            {
                baseChange = variant.ref() + ">" + variant.alt();
                context = rawContext;
            }

            String bucketName = baseChange + "_" + context;
            Integer bucketIndex = mBucketStringToIndex.get(bucketName);

            if(bucketIndex == null)
            {
                LOGGER.error("sample({}) invalid bucketName({}) from baseChange({} raw={}>{}) context({} raw={}",
                        sampleId, bucketName, baseChange, variant.ref(), variant.alt(), context, rawContext);

                return;
            }

            ++sampleCounts[bucketIndex][sampleIndex];
        }
    }

    public static char convertBase(char base)
    {
        if(base == 'A') return 'T';
        if(base == 'T') return 'A';
        if(base == 'C') return 'G';
        if(base == 'G') return 'C';
        return base;
    }

    public static String getBucketNameByIndex(final Map<String,Integer> bucketNameIndexMap, int index)
    {
        for(Map.Entry<String,Integer> entry : bucketNameIndexMap.entrySet())
        {
            if(entry.getValue() == index)
                return entry.getKey();
        }

        return String.format("MissingBucket_%d", index);
    }

        /*
    @NotNull
    public List<SomaticSnv> readPartialSnvInfo(@NotNull String sample) {
        List<SomaticSnv> variants = Lists.newArrayList();

        Result<Record> result =
                : context.select()
                .from(SOMATICVARIANT)
                .where(SOMATICVARIANT.SAMPLEID.eq(sample))
                .and(SOMATICVARIANT.TYPE.eq(VariantType.SNP.toString())
                        .and)
                .fetch();

        for (Record record : result) {

            variants.add(ImmutableSomaticVariantImpl.builder()
                    .chromosome(record.getValue(SOMATICVARIANT.CHROMOSOME))
                    .position(record.getValue(SOMATICVARIANT.POSITION))
                    .filter(record.getValue(SOMATICVARIANT.FILTER))
                    .type(VariantType.valueOf(record.getValue(SOMATICVARIANT.TYPE)))
                    .ref(record.getValue(SOMATICVARIANT.REF))
                    .alt(record.getValue(SOMATICVARIANT.ALT))
                    .variantCopyNumber(record.getValue(SOMATICVARIANT.VARIANTCOPYNUMBER))
                    .trinucleotideContext(record.getValue(SOMATICVARIANT.TRINUCLEOTIDECONTEXT))
                    .subclonalLikelihood(record.getValue(SOMATICVARIANT.SUBCLONALLIKELIHOOD))
                    .build());
        }
        return variants;
    }
    */
}
