package com.hartwig.hmftools.linx.drivers;

import static com.hartwig.hmftools.common.drivercatalog.DriverType.AMP;
import static com.hartwig.hmftools.common.drivercatalog.DriverType.DEL;
import static com.hartwig.hmftools.linx.analysis.SvUtilities.getChromosomalArm;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.drivercatalog.DriverCatalog;
import com.hartwig.hmftools.common.ensemblcache.EnsemblGeneData;
import com.hartwig.hmftools.common.ensemblcache.TranscriptData;
import com.hartwig.hmftools.linx.types.ChromosomeArm;

public class DriverGeneData
{
    public final DriverCatalog DriverData;
    public final EnsemblGeneData GeneData;
    public final TranscriptData TransData;
    public final GeneCopyNumberRegion CopyNumberRegion;
    public final ChromosomeArm Arm;

    private final List<DriverGeneEvent> mEvents;

    public DriverGeneData(final DriverCatalog driverData, final EnsemblGeneData geneData,
            final TranscriptData transData, final GeneCopyNumberRegion copyNumberRegion)
    {
        DriverData = driverData;
        GeneData = geneData;
        TransData = transData;
        CopyNumberRegion = copyNumberRegion;

        Arm = getChromosomalArm(geneData.Chromosome, geneData.GeneStart);

        mEvents = Lists.newArrayList();
    }

    public final List<DriverGeneEvent> getEvents() {return mEvents; }
    public void addEvent(final DriverGeneEvent event) { mEvents.add(event); }

    public boolean fullyMatched()
    {
        if(DriverData.driver() == AMP)
            return !mEvents.isEmpty();

        if(DriverData.driver() == DEL)
            return mEvents.size() >= 2;

        return mEvents.size() >= 1;
    }

    public String toString()
    {
        return String.format("%s: %s ",
            DriverData.likelihoodMethod(), DriverData.gene());
    }
}
