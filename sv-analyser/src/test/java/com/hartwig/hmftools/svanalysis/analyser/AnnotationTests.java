package com.hartwig.hmftools.svanalysis.analyser;

import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.BND;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.DEL;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.DUP;
import static com.hartwig.hmftools.common.variant.structural.StructuralVariantType.INV;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createBnd;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createInv;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createSgl;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createTestSv;
import static com.hartwig.hmftools.svanalysis.types.SvCluster.CLUSTER_ANNONTATION_DM;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.svanalysis.analysis.CNAnalyser;
import com.hartwig.hmftools.svanalysis.types.SvCNData;
import com.hartwig.hmftools.svanalysis.types.SvCluster;
import com.hartwig.hmftools.svanalysis.types.SvVarData;

import org.junit.Test;

public class AnnotationTests
{
    @Test
    public void testUnderClusteredFoldbackAnnotations()
    {
        SvTestHelper tester = new SvTestHelper();
        tester.logVerbose(true);

        double[] chrCopyNumbers = {2.0, 2.0, 2.0};
        tester.ClusteringMethods.getChrCopyNumberMap().put("1", chrCopyNumbers);
        tester.ClusteringMethods.getChrCopyNumberMap().put("2", chrCopyNumbers);

        final SvVarData var1 = createInv("0", "1", 101000, 104000, -1);

        // straddling BNDs
        final SvVarData var2 = createBnd("1", "1", 1000, 1, "2", 1000, -1);
        final SvVarData var3 = createBnd("2", "1", 200000, 1, "2", 1100, 1);

        // single other cluster
        final SvVarData var4 = createSgl("3", "1", 150000, 1, false);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);

        tester.preClusteringInit();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(tester.Analyser.getClusters().size(), 3);

    }

    @Test
    public void testPotentialDoubleMinutes()
    {
        SvTestHelper tester = new SvTestHelper();
        tester.logVerbose(true);

        CNAnalyser cnAnalyser = new CNAnalyser("", null);

        // first a simple DUP
        final SvVarData var1 = createTestSv("0","1","1",500,600,-1,1, DUP,5,5,3,3,3,"");

        // filler
        final SvVarData var2 = createSgl("1", "1", 11500, 1, false);

        // then a pair of BNDs forming a remote TI
        final SvVarData var3 = createTestSv("2","1","2",21000,1000,-1,-1, BND,5,5,3,3,3,"");
        final SvVarData var4 = createTestSv("3","1","2",22000,1100,1,1, BND,5,5,3,3,3,"");

        // filler
        final SvVarData var5 = createSgl("4", "1", 31500, 1, false);

        // the a pair of overlapping inversions
        final SvVarData var6 = createTestSv("5","1","1",51000,61000,-1,-1, INV,5,5,3,3,3,"");
        final SvVarData var7 = createTestSv("6","1","1",52000,62000,1,1, INV,5,5,3,3,3,"");

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);
        tester.AllVariants.add(var5);
        tester.AllVariants.add(var6);
        tester.AllVariants.add(var7);

        tester.AllVariants.stream().forEach(x -> x.setPloidyRecalcData(x.getSvData().ploidy(), x.getSvData().ploidy()));

        tester.preClusteringInit();
        tester.addCopyNumberData();

        tester.Analyser.clusterAndAnalyse();

        assertEquals(5, tester.Analyser.getClusters().size());

        SvCluster cluster = tester.Analyser.getClusters().get(0);
        assertEquals(1, cluster.getUniqueSvCount());
        assertTrue(cluster.getSVs().contains(var1));
        assertTrue(cluster.getAnnotations().contains(CLUSTER_ANNONTATION_DM));

        cluster = tester.Analyser.getClusters().get(2);
        assertEquals(2, cluster.getUniqueSvCount());
        assertTrue(cluster.getSVs().contains(var3));
        assertTrue(cluster.getSVs().contains(var4));
        assertTrue(cluster.getAnnotations().contains(CLUSTER_ANNONTATION_DM));

        cluster = tester.Analyser.getClusters().get(4);
        assertEquals(2, cluster.getUniqueSvCount());
        assertTrue(cluster.getSVs().contains(var6));
        assertTrue(cluster.getSVs().contains(var7));
        assertTrue(cluster.getAnnotations().contains(CLUSTER_ANNONTATION_DM));

    }
}