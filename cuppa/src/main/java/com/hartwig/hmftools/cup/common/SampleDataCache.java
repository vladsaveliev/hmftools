package com.hartwig.hmftools.cup.common;

import static com.hartwig.hmftools.common.utils.io.FileWriterUtils.createFieldsIndexMap;
import static com.hartwig.hmftools.cup.SampleAnalyserConfig.CANCER_SUBTYPE_OTHER;
import static com.hartwig.hmftools.cup.SampleAnalyserConfig.CUP_LOGGER;
import static com.hartwig.hmftools.cup.SampleAnalyserConfig.DATA_DELIM;
import static com.hartwig.hmftools.cup.SampleAnalyserConfig.SUBSET_DELIM;
import static com.hartwig.hmftools.cup.common.CupConstants.CANCER_TYPE_UNKNOWN;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class SampleDataCache
{
    public final List<String> SampleIds;
    public final List<SampleData> SampleDataList;
    public SampleData SpecificSample;

    public final Map<String,List<SampleData>> RefCancerSampleData;
    public final Map<String,String> RefSampleCancerTypeMap;

    public SampleDataCache()
    {
        SampleIds = Lists.newArrayList();
        SampleDataList = Lists.newArrayList();
        RefCancerSampleData = Maps.newHashMap();
        RefSampleCancerTypeMap = Maps.newHashMap();
        SpecificSample = null;
    }

    public boolean isSingleSample() { return SpecificSample != null; }
    public boolean isMultiSample() { return isSingleSample(); }

    public void loadSampleData(final String specificSampleData, final String sampleDataFile)
    {
        if(specificSampleData != null)
        {
            final String[] sampleItems = specificSampleData.split(SUBSET_DELIM, -1);
            String sampleId = sampleItems[0];
            String cancerType = CANCER_TYPE_UNKNOWN;
            String cancerSubtype = CANCER_SUBTYPE_OTHER;

            if(sampleItems.length >= 2)
                cancerType = sampleItems[1];

            if(sampleItems.length == 3)
                cancerSubtype = sampleItems[2];

            SpecificSample = new SampleData(sampleId, cancerType, cancerSubtype, cancerType);
            SampleDataList.add(SpecificSample);
        }

        if(sampleDataFile != null)
        {
            try
            {
                final List<String> fileData = Files.readAllLines(new File(sampleDataFile).toPath());

                final String header = fileData.get(0);
                fileData.remove(0);

                final Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(header, DATA_DELIM);

                for(final String line : fileData)
                {
                    SampleData sample = SampleData.from(fieldsIndexMap, line);

                    if(SpecificSample != null && SpecificSample.Id.equals(sample.Id))
                        continue;

                    SampleDataList.add(sample);
                }
            }
            catch (IOException e)
            {
                CUP_LOGGER.error("failed to read sample data file({}): {}", sampleDataFile, e.toString());
            }
        }

        SampleDataList.forEach(x -> SampleIds.add(x.Id));
    }

    public void loadReferenceSampleData(final String refSampleDataFile)
    {
        if(refSampleDataFile == null)
            return;

        try
        {
            final List<String> fileData = Files.readAllLines(new File(refSampleDataFile).toPath());

            final String header = fileData.get(0);
            fileData.remove(0);

            final Map<String,Integer> fieldsIndexMap = createFieldsIndexMap(header, DATA_DELIM);

            for(final String line : fileData)
            {
                SampleData sample = SampleData.from(fieldsIndexMap, line);

                List<SampleData> cancerSampleData = RefCancerSampleData.get(sample.CancerType);

                if(cancerSampleData == null)
                    RefCancerSampleData.put(sample.CancerType, Lists.newArrayList(sample));
                else
                    cancerSampleData.add(sample);

                RefSampleCancerTypeMap.put(sample.Id, sample.CancerType);
            }
        }
        catch (IOException e)
        {
            CUP_LOGGER.error("failed to read sample data file({}): {}", refSampleDataFile, e.toString());
        }
    }

}
