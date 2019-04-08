package com.hartwig.hmftools.svanalysis.analyser;

import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.addGeneData;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.addTransExonData;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createDel;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createDup;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createEnsemblGeneData;
import static com.hartwig.hmftools.svanalysis.analysis.FusionDisruptionAnalyser.PRE_GENE_BREAKEND_DISTANCE;
import static com.hartwig.hmftools.svanalysis.analysis.FusionDisruptionAnalyser.setSvGeneData;
import static com.hartwig.hmftools.svannotation.SvGeneTranscriptCollection.PRE_GENE_PROMOTOR_DISTANCE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;
import com.hartwig.hmftools.common.variant.structural.annotation.EnsemblGeneData;
import com.hartwig.hmftools.common.variant.structural.annotation.GeneFusion;
import com.hartwig.hmftools.common.variant.structural.annotation.TranscriptExonData;
import com.hartwig.hmftools.svanalysis.types.SvCluster;
import com.hartwig.hmftools.svanalysis.types.SvVarData;
import com.hartwig.hmftools.svannotation.SvGeneTranscriptCollection;

import org.junit.Test;

public class FusionTests
{
    @Test
    public void testSimpleFusions()
    {

    }

    @Test
    public void testChainedFusions()
    {
        SvTestHelper tester = new SvTestHelper();
        tester.logVerbose(true);

        SvGeneTranscriptCollection geneTransCache = new SvGeneTranscriptCollection();

        tester.initialiseFusions(geneTransCache);

        String geneName1 = "GENE1";
        String geneId1 = "ENSG0001";
        String chromosome = "1";

        List<EnsemblGeneData> geneList = Lists.newArrayList();
        geneList.add(createEnsemblGeneData(geneId1, geneName1, chromosome, 1, 1000, 2000));

        List<TranscriptExonData> transExonList = Lists.newArrayList();

        String transName1 = "ENST0001";
        int transId1 = 1;

        byte strand = (byte)1;
        boolean isCanonical = true;
        long transStart = 1000;
        long transEnd = 2000;
        long codingStart = 1400;
        long codingEnd = 1900;
        transExonList.add(new TranscriptExonData(geneId1, transName1, transId1, isCanonical, strand, transStart, transEnd,
                1000, 1100, 1, -1, -1, codingStart, codingEnd, ""));

        transExonList.add(new TranscriptExonData(geneId1, transName1, transId1, isCanonical, strand, transStart, transEnd,
                1300, 1500, 2, -1, 1, codingStart, codingEnd, ""));

        transExonList.add(new TranscriptExonData(geneId1, transName1, transId1, isCanonical, strand, transStart, transEnd,
                1600, 1700, 3, 1, 2, codingStart, codingEnd, ""));

        transExonList.add(new TranscriptExonData(geneId1, transName1, transId1, isCanonical, strand, transStart, transEnd,
                1800, 1900, 4, 2, -1, codingStart, codingEnd, ""));

        addTransExonData(geneTransCache, geneId1, transExonList);

        String geneName2 = "GENE2";
        String geneId2 = "ENSG0002";

        geneList.add(createEnsemblGeneData(geneId2, geneName2, chromosome, 1, 10000, 12000));
        addGeneData(geneTransCache, chromosome, geneList);

        String transName2 = "ENST0002";
        int transId2 = 2;

        transStart = 11000;
        transEnd = 12000;
        codingStart = 11050;
        codingEnd = 11980;

        transExonList = Lists.newArrayList();

        transExonList.add(new TranscriptExonData(geneId2, transName2, transId2, isCanonical, strand, transStart, transEnd,
                11000, 11100, 1, -1, 1, codingStart, codingEnd, ""));

        transExonList.add(new TranscriptExonData(geneId2, transName2, transId2, isCanonical, strand, transStart, transEnd,
                11300, 11500, 2, 1, 2, codingStart, codingEnd, ""));

        transExonList.add(new TranscriptExonData(geneId2, transName2, transId2, isCanonical, strand, transStart, transEnd,
                11600, 11700, 3, 2, 0, codingStart, codingEnd, ""));

        transExonList.add(new TranscriptExonData(geneId2, transName2, transId2, isCanonical, strand, transStart, transEnd,
                11950, 12000, 4, 2, -1, codingStart, codingEnd, ""));

        addTransExonData(geneTransCache, geneId2, transExonList);

        PRE_GENE_PROMOTOR_DISTANCE = 200;

        // test 1: create a chain of DELs with a single-SV fusion which link between exon 2-3 of upstream to 2-3 of downstream

        // pre-gene
        SvVarData var1 = createDel("0", chromosome, 300,400);

        // intronic del
        SvVarData var2 = createDel("1", chromosome, 1150,1250);

        // fusing del
        SvVarData var3 = createDel("2", chromosome, 1550,11200);

        // intronic del which then runs out remainder of transcript
        SvVarData var4 = createDel("3", chromosome, 11800,11900);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);

        tester.preClusteringInit();
        tester.Analyser.clusterAndAnalyse();

        assertEquals(1, tester.Analyser.getClusters().size());
        SvCluster cluster = tester.Analyser.getClusters().get(0);

        assertEquals(1, cluster.getChains().size());

        tester.FusionAnalyser.setSvGeneData(tester.AllVariants, geneTransCache, true, false);
        tester.FusionAnalyser.run(tester.SampleId, tester.AllVariants, tester.getClusters(), tester.ClusteringMethods.getChrBreakendMap());

        assertEquals(1, tester.FusionAnalyser.getFusions().size());

        GeneFusion fusion = tester.FusionAnalyser.getFusions().get(0);
        assertEquals(var3.dbId(), fusion.upstreamTrans().parent().id());
        assertEquals(var3.dbId(), fusion.downstreamTrans().parent().id());
        assertTrue(validateFusionAnnotations(fusion, true, true));

        // test 2: this time a chain from the first to the last variant with the middle 2 going out to non-disruptive locations
        tester.clearClustersAndSVs();

        // upstream trans
        var1 = createDel("0", chromosome, 1550,50000);

        var2 = createDel("1", chromosome, 50100,51000);

        // fusing del
        var3 = createDel("2", chromosome, 55000,56000);

        // intronic del which then runs out remainder of transcript
        var4 = createDup("3", chromosome, 10900, 56500);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);

        tester.preClusteringInit();
        tester.Analyser.clusterAndAnalyse();

        assertEquals(1, tester.Analyser.getClusters().size());
        cluster = tester.Analyser.getClusters().get(0);

        assertEquals(1, cluster.getChains().size());

        tester.FusionAnalyser.setSvGeneData(tester.AllVariants, geneTransCache, true, false);
        tester.FusionAnalyser.run(tester.SampleId, tester.AllVariants, tester.getClusters(), tester.ClusteringMethods.getChrBreakendMap());

        assertEquals(1, tester.FusionAnalyser.getFusions().size());

        fusion = tester.FusionAnalyser.getFusions().get(0);
        assertEquals(var1.dbId(), fusion.upstreamTrans().parent().id());
        assertEquals(var4.dbId(), fusion.downstreamTrans().parent().id());
        assertTrue(validateFusionAnnotations(fusion, true, true));

        // test 3: single-SV fusion from the 3rd SV, with the other ones simple SVs within introns leaving a valid traversal
        tester.clearClustersAndSVs();

        // intronic del
        var1 = createDel("0", chromosome, 1150,1250);

        // intronic dup
        var2 = createDup("1", chromosome, 1525,1575);

        // fusion the 2 genes
        var3 = createDel("2", chromosome, 1750,11550);

        // intronic del
        var4 = createDel("3", chromosome, 11725, 11775);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);

        tester.preClusteringInit();
        tester.Analyser.clusterAndAnalyse();

        assertEquals(1, tester.Analyser.getClusters().size());
        cluster = tester.Analyser.getClusters().get(0);

        assertEquals(1, cluster.getChains().size());

        tester.FusionAnalyser.setSvGeneData(tester.AllVariants, geneTransCache, true, false);
        tester.FusionAnalyser.run(tester.SampleId, tester.AllVariants, tester.getClusters(), tester.ClusteringMethods.getChrBreakendMap());

        assertEquals(2, tester.FusionAnalyser.getFusions().size());

        fusion = tester.FusionAnalyser.getFusions().get(0);
        assertEquals(var3.dbId(), fusion.upstreamTrans().parent().id());
        assertEquals(var3.dbId(), fusion.downstreamTrans().parent().id());

        assertTrue(validateFusionAnnotations(fusion, true, true));

        fusion = tester.FusionAnalyser.getFusions().get(1);
        assertEquals(var3.dbId(), fusion.upstreamTrans().parent().id());
        assertEquals(var4.dbId(), fusion.downstreamTrans().parent().id());

        assertTrue(validateFusionAnnotations(fusion, true, false));


        // test 4: invalid fusion, with a TI beyond the fusion ending in an exon upstream and skipping an exon downstream
        tester.clearClustersAndSVs();

        // del starting in an exon
        var1 = createDel("0", chromosome, 1150,1250);

        // intronic dup
        var2 = createDup("1", chromosome, 1350,1575);

        // fusion the 2 genes
        var3 = createDel("2", chromosome, 1750,11525);

        // del skips an exon
        var4 = createDel("3", chromosome, 11575, 111800);

        tester.AllVariants.add(var1);
        tester.AllVariants.add(var2);
        tester.AllVariants.add(var3);
        tester.AllVariants.add(var4);

        tester.preClusteringInit();
        tester.Analyser.clusterAndAnalyse();

        assertEquals(1, tester.Analyser.getClusters().size());
        cluster = tester.Analyser.getClusters().get(0);

        assertEquals(1, cluster.getChains().size());

        tester.FusionAnalyser.setSvGeneData(tester.AllVariants, geneTransCache, true, false);
        tester.FusionAnalyser.run(tester.SampleId, tester.AllVariants, tester.getClusters(), tester.ClusteringMethods.getChrBreakendMap());

        assertEquals(1, tester.FusionAnalyser.getFusions().size());

        fusion = tester.FusionAnalyser.getFusions().get(0);
        assertEquals(var3.dbId(), fusion.upstreamTrans().parent().id());
        assertEquals(var3.dbId(), fusion.downstreamTrans().parent().id());

        assertTrue(validateFusionAnnotations(fusion, false, true));
    }

    private static boolean validateFusionAnnotations(final GeneFusion fusion, boolean validEnds, boolean validTraversal)
    {
        String[] fields = fusion.getAnnotations().split(",");

        // ClusterId,ClusterCount,ResolvedType,OverlapUp,OverlapDown,ChainInfo
        if(fields.length != 6)
            return false;

        String[] chainInfo = fields[5].split(";");

        if(chainInfo.length != 4)
            return false;

        if(validTraversal != (chainInfo[3].equals("true")))
            return false;

        String[] disruptionsUp = fields[3].split(";");
        String[] disruptionsDown = fields[4].split(";");

        if(disruptionsUp.length != 6 || disruptionsDown.length != 6)
            return false;

        boolean validUp = disruptionsUp[4].equals("0") && disruptionsUp[5].equals("0");
        boolean validDown = disruptionsDown[4].equals("0") && disruptionsDown[5].equals("0");

        if(validEnds)
            return validUp && validDown;
        else
            return !validUp || !validDown;
    }

}