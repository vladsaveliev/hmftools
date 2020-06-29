package com.hartwig.hmftools.serve.vicc.curation;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

final class CurationFactory {

    static final Map<CurationKey, String> CGI_FEATURE_NAME_MAPPINGS = Maps.newHashMap();
    static final Map<CurationKey, String> CIVIC_FEATURE_NAME_MAPPINGS = Maps.newHashMap();
    static final Map<CurationKey, String> JAX_FEATURE_NAME_MAPPINGS = Maps.newHashMap();
    static final Map<CurationKey, String> ONCOKB_FEATURE_NAME_MAPPINGS = Maps.newHashMap();

    static final Set<CurationKey> CGI_FEATURE_BLACKLIST = Sets.newHashSet();
    static final Set<CurationKey> CIVIC_FEATURE_BLACKLIST = Sets.newHashSet();
    static final Set<CurationKey> JAX_FEATURE_BLACKLIST = Sets.newHashSet();
    static final Set<CurationKey> ONCOKB_FEATURE_BLACKLIST = Sets.newHashSet();

    static {
        populateCGICuration();
        populateCIViCCuration();
        populateJaxCuration();
        populateOncoKBCuration();
    }

    private static void populateCGICuration() {
        // Below is not wrong but leads to inconsistencies downstream
        CGI_FEATURE_NAME_MAPPINGS.put(new CurationKey("PIK3R1", null, "PIK3R1 p.E439delE"), "PIK3R1 p.E439del");
        CGI_FEATURE_NAME_MAPPINGS.put(new CurationKey("PIK3R1", null, "PIK3R1 p.D560_S565delDKRMNS"), "PIK3R1 p.D560_S565del");
        CGI_FEATURE_NAME_MAPPINGS.put(new CurationKey("PIK3R1", null, "PIK3R1 p.T576delT"), "PIK3R1 p.T576del");
    }

    private static void populateCIViCCuration() {
        // Below is not wrong but transvar struggles with interpreting start lost variants,
        CIVIC_FEATURE_NAME_MAPPINGS.put(new CurationKey("VHL", "ENST00000256474", "M1? (c.3G>A)"), "M1I (c.3G>A)");
        CIVIC_FEATURE_NAME_MAPPINGS.put(new CurationKey("VHL", "ENST00000256474", "M1? (c.1-1_20del21)"), "M1I (c.1-1_20del21)");
        // Below is a mutation followed by a frameshift. The FS should be in small letters.
        CIVIC_FEATURE_NAME_MAPPINGS.put(new CurationKey("VHL", "ENST00000256474", "M54IFS (c.162_166delGGAGG)"),
                "M54Ifs (c.162_166delGGAGG)");

        // The below variants don't exist
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("GNAS", "ENST00000371100", "T393C"));
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("TERT", "ENST00000310581", "C228T"));
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("VHL", null, "P81delRVV (c.243_251delGCGCGTCGT)"));
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("VHL", null, "A121I (c.364_365GC>AT)"));
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("VHL", null, "N167T (c.392A>C)"));
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("VHL", null, "X214L (c.641G>T)"));

        // The below variant is only possible through an MNV which spans an intron
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("VHL", null, "V155C (c.462delA)"));

        // Synonymous variant, can't have an impact or evidence
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("VHL", null, "R161R (c.481C>A)"));

        // The below variant is unlikely to be real as it spans multiple exons
        CIVIC_FEATURE_BLACKLIST.add(new CurationKey("VHL", null, "G114dup (c.342dupGGT)"));
    }

    private static void populateJaxCuration() {
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("PIK3CA", null, "PIK3CA E545k "), "PIK3CA E545K ");

        // These mappings are to work around the missing transcripts in JAX.
        // We map every mutation that appears on both the canonical + non-canonical form to its canonical form in ensembl.
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("EZH2", null, "EZH2 Y641F "), "EZH2 Y646F ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("EZH2", null, "EZH2 Y641H "), "EZH2 Y646H ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("EZH2", null, "EZH2 Y641N "), "EZH2 Y646N ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("EZH2", null, "EZH2 Y641S "), "EZH2 Y646S ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("FGFR2", null, "FGFR2 V564I "), "FGFR2 V565I ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("FGFR3", null, "FGFR3 Y373C "), "FGFR3 Y375C ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("FGFR3", null, "FGFR3 K650E "), "FGFR3 K652E ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("MET", null, "MET L1195V "), "MET L1213V ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("MET", null, "MET Y1230H "), "MET Y1248H ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("MET", null, "MET M1250T "), "MET M1268T ");

        // These mappings are identical but used concurrently. Confirmed to be replaced by I740_K745dup
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("EGFR", null, "EGFR I744_K745insKIPVAI "), "EGFR K745_E746insIPVAIK ");
        JAX_FEATURE_NAME_MAPPINGS.put(new CurationKey("KIT", null, "KIT V559del "), "KIT V560del ");

        // The below variants in FLT3 are from a paper where an additional R was added in the ref sequence, shifting all AAs by one position.
        // This has been corrected in current live CKB.
        JAX_FEATURE_BLACKLIST.add(new CurationKey("FLT3", null, "FLT3 L611_E612insCSSDNEYFYVDFREYEYDLKWEFPRENL "));
        JAX_FEATURE_BLACKLIST.add(new CurationKey("FLT3", null, "FLT3 E612_F613insGYVDFREYEYDLKWEFRPRENLEF "));

        // The transcript that should have this mutation (ENST00000507379.1) is annotated as 3' truncated with only 1135 AAs in ensembl)
        JAX_FEATURE_BLACKLIST.add(new CurationKey("APC", null, "APC S1197* "));

        // The below is pending investigation by JAX, possibly a mistake by the paper.
        JAX_FEATURE_BLACKLIST.add(new CurationKey("PIK3CA", null, "PIK3CA R425L "));
    }

    private static void populateOncoKBCuration() {
        ONCOKB_FEATURE_NAME_MAPPINGS.put(new CurationKey("EPAS1", "ENST00000263734", "533_534del"), "I533_P534del");
        ONCOKB_FEATURE_NAME_MAPPINGS.put(new CurationKey("EPAS1", "ENST00000263734", "534_536del"), "P534_D536del");
        ONCOKB_FEATURE_NAME_MAPPINGS.put(new CurationKey("KIT", "ENST00000288135", "V559del"), "V560del");
        ONCOKB_FEATURE_NAME_MAPPINGS.put(new CurationKey("PTEN", "ENST00000371953", "I32del"), "I33del");
        ONCOKB_FEATURE_NAME_MAPPINGS.put(new CurationKey("EGFR", "ENST00000275493", "E746_T751insIP"), "E746_L747insIP");

        // The below variant is fine, but interpretation currently not supported by SERVE
        // The unaligned insert lies outside of exonic range, but the left-aligned insert is fine
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("BRAF", "ENST00000288602", "R506_K507insVLR"));

        // The below variants are unlikely as they span multiple exons (and hence are more fusions than inframes)
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PDGFRA", "ENST00000257290", "E311_K312del"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("ETV6", "ENST00000396373", "385_418del"));

        // The below variants don't exist
        //  - Below is called a "silent promoter" according to https://pubmed.ncbi.nlm.nih.gov/11606402/
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("APC", "ENST00000257430", "A290T"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("AR", "ENST00000374690", "F876L"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("BRCA1", "ENST00000357654", "T47D"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("BRCA1", "ENST00000357654", "E60L"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("BRCA2", "ENST00000380152", "S1670A"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("BRCA2", "ENST00000380152", "R2304C"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("BRCA2", "ENST00000380152", "N2829R"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARD11", "ENST00000396946", "G116S"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARD11", "ENST00000396946", "F123I"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARD11", "ENST00000396946", "E127G"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARM1", "ENST00000327064", "S217A"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARM1", "ENST00000327064", "S217C"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARM1", "ENST00000327064", "S217E"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARM1", "ENST00000327064", "S217T"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CASP8", "ENST00000358485", "C248T"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CCND3", "ENST00000372991", "T286A"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CDK12", "ENST00000447079", "K765R"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CDK12", "ENST00000447079", "D887N"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CDKN2A", "ENST00000304494", "P73L"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CDKN2A", "ENST00000304494", "R79P"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CDKN2A", "ENST00000304494", "G93W"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CDKN2A", "ENST00000304494", "V118D"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("FLT3", "ENST00000241453", "L611_E612insCSSDNEYFYVDFREYEYDLKWEFPRENL"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("FLT3", "ENST00000241453", "E612_F613insGYVDFREYEYDLKWEFRPRENLEF"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("IGF1R", "ENST00000268035", "G119T"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("IGF1R", "ENST00000268035", "G1125A"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("IGF1R", "ENST00000268035", "A1374V"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("JAK1", "ENST00000342505", "G1079D"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("JAK1", "ENST00000342505", "G871E"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("KIT", "ENST00000288135", "A504_Y505ins"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("KIT", "ENST00000288135", "D814V"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PMS2", "ENST00000265849", "E541K"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("POLE", "ENST00000320574", "S279Y"));
        //  - This one comes from https://www.ncbi.nlm.nih.gov/pmc/articles/PMC4155059/ and probably should be L1237F.
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RAD50", "ENST00000265335", "L1273F"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RXRA", "ENST00000481739", "S247F"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RXRA", "ENST00000481739", "S247Y"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("SMAD4", "ENST00000342988", "D357Y"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("SOX9", "ENST00000245479", "F12L"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("SOX9", "ENST00000245479", "A19V"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("SOX9", "ENST00000245479", "H65Y"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TERT", "ENST00000310581", "C228T"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TERT", "ENST00000310581", "C250T"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TMPRSS2", "ENST00000398585", "M160V"));

        // The below probably have the wrong transcripts configured by OncoKB:
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CASP8", "ENST00000358485", "G325A"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("EZH2", "ENST00000320356", "A677G"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("FBXW7", "ENST00000281708", "R482Q"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("FGFR2", "ENST00000358487", "K525E"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("FGFR2", "ENST00000358487", "T730S"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("FGFR2", "ENST00000358487", "V755I"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("GNAS", "ENST00000371085", "R844H"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("JAK2", "ENST00000381652", "R277K"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("KRAS", "ENST00000256078", "D153V"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("NF1", "ENST00000358273", "R1391G"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("NF1", "ENST00000358273", "R1391S"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("NF1", "ENST00000358273", "V1398D"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("NF1", "ENST00000358273", "K1423E"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("NF1", "ENST00000358273", "K1436Q"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("NF1", "ENST00000358273", "S1463F"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("NKX2-1", "ENST00000354822", "A339V"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PRDM1", "ENST00000369096", "P48R"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PRDM1", "ENST00000369096", "P48T"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PRDM1", "ENST00000369096", "Y149D"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PRDM1", "ENST00000369096", "C569Y"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PTPRT", "ENST00000373198", "T844M"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PTPRT", "ENST00000373198", "D927G"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PTPRT", "ENST00000373198", "V995M"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PTPRT", "ENST00000373198", "A1022E"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("PTPRT", "ENST00000373198", "R1040L"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RAC1", "ENST00000356142", "C157Y"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RBM10", "ENST00000329236", "V354E"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "G42R"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "H78Q"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "R80C"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "K83E"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "K83N"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "Y113*"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "A122*"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "R139G"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "D171G"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "D171N"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "P173S"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "R174*"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "R174Q"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "R177*"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("RUNX1", "ENST00000300305", "R177Q"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TGFBR2", "ENST00000359013", "V419L"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TGFBR2", "ENST00000359013", "P525L"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TGFBR2", "ENST00000359013", "E526Q"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TGFBR2", "ENST00000359013", "R537P"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("TMPRSS2", "ENST00000398585", "T75M"));

        // The below are simply invalid protein annotations:
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("BRAF", "ENST00000288602", "V600D_K601insFGLAT"));
        ONCOKB_FEATURE_BLACKLIST.add(new CurationKey("CARD11", "ENST00000396946", "L225LI"));
    }

    private CurationFactory() {
    }

}
