package com.hartwig.hmftools.linx.annotators;

import static com.hartwig.hmftools.common.genome.refgenome.RefGenomeVersion.refGenomeChromosome;
import static com.hartwig.hmftools.linx.LinxConfig.LNX_LOGGER;
import static com.hartwig.hmftools.linx.LinxConfig.RG_VERSION;

import static htsjdk.tribble.AbstractFeatureReader.getFeatureReader;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hartwig.hmftools.common.genome.region.ReplicationOriginRegion;
import com.hartwig.hmftools.linx.types.SvBreakend;

import htsjdk.tribble.AbstractFeatureReader;
import htsjdk.tribble.bed.BEDCodec;
import htsjdk.tribble.bed.BEDFeature;
import htsjdk.tribble.readers.LineIterator;

public class ReplicationOriginAnnotator
{
    Map<String, List<ReplicationOriginRegion>> mReplicationOrigins; // regions by chromosome

    public ReplicationOriginAnnotator()
    {
        mReplicationOrigins = Maps.newHashMap();
    }

    public void loadReplicationOrigins(final String filename)
    {
        if(filename.isEmpty())
            return;

        try
        {
            String currentChr = "";
            List<ReplicationOriginRegion> regionList = null;
            int regionCount = 0;

            final AbstractFeatureReader<BEDFeature, LineIterator> reader = getFeatureReader(filename, new BEDCodec(), false);

            for (final BEDFeature bedFeature : reader.iterator())
            {
                final String chromosome = refGenomeChromosome(bedFeature.getContig(), RG_VERSION);

                if (!chromosome.equals(currentChr))
                {
                    currentChr = chromosome;
                    regionList = mReplicationOrigins.get(chromosome);

                    if (regionList == null)
                    {
                        regionList = Lists.newArrayList();
                        mReplicationOrigins.put(currentChr, regionList);
                    }
                }

                ++regionCount;

                double originValue = Double.parseDouble(bedFeature.getName()) / 100;

                regionList.add(new ReplicationOriginRegion(
                        chromosome,
                        bedFeature.getStart(),
                        bedFeature.getEnd(),
                        originValue));
            }

            LNX_LOGGER.debug("loaded {} replication origins", regionCount);
        }
        catch(IOException exception)
        {
            LNX_LOGGER.error("Failed to read replication origin BED file({})", filename);
        }
    }

    public void setReplicationOrigins(final Map<String, List<SvBreakend>> chrBreakendMap)
    {
        for (final Map.Entry<String, List<SvBreakend>> entry : chrBreakendMap.entrySet())
        {
            final String chromosome = entry.getKey();
            final List<SvBreakend> breakendList = entry.getValue();

            List<ReplicationOriginRegion> regions = mReplicationOrigins.get(chromosome);

            if(regions == null || regions.isEmpty())
                continue;

            int regionIndex = 0;
            ReplicationOriginRegion currentRegion = regions.get(regionIndex);

            for(final SvBreakend breakend : breakendList)
            {
                while(currentRegion.End < breakend.position())
                {
                    ++regionIndex;

                    if(regionIndex >= regions.size())
                    {
                        // the origins data may not extend all the way to the telomeres or cover all SV breakends
                        currentRegion = null;
                        break;
                    }

                    currentRegion = regions.get(regionIndex);
                }

                if(currentRegion != null)
                {
                    breakend.getSV().setReplicationOrigin(breakend.usesStart(), currentRegion.OriginValue);
                }
                else
                {
                    break;
                }
            }
        }
    }

    // general purpose method
    public double getReplicationOriginValue(final String chromosome, int position)
    {
        if(mReplicationOrigins.isEmpty())
            return 0;

        List<ReplicationOriginRegion> regions = mReplicationOrigins.get(chromosome);

        if(regions == null || regions.isEmpty())
            return 0;

        for(final ReplicationOriginRegion region : regions)
        {
            if(region.Start <= position && region.End >= position)
            {
                return region.OriginValue;

            }
        }

        return 0;
    }

}
