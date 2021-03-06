package com.hartwig.hmftools.serve.vicc.curation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.hartwig.hmftools.serve.vicc.ViccTestFactory;
import com.hartwig.hmftools.vicc.datamodel.Feature;
import com.hartwig.hmftools.vicc.datamodel.ImmutableFeature;
import com.hartwig.hmftools.vicc.datamodel.ViccEntry;
import com.hartwig.hmftools.vicc.datamodel.ViccSource;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class FeatureCuratorTest {

    @Test
    public void canCurateFeatures() {
        CurationKey firstOncoKbKey = firstOncoKbMappingKey();
        String firstMappedFeature = CurationFactory.FEATURE_NAME_MAPPINGS.get(firstOncoKbKey);

        ViccEntry entry = ViccTestFactory.testViccEntryForSource(ViccSource.ONCOKB,
                ViccTestFactory.testOncoKbWithTranscript(firstOncoKbKey.transcript()));

        Feature feature = ImmutableFeature.builder().geneSymbol(firstOncoKbKey.gene()).name(firstOncoKbKey.featureName()).build();

        assertEquals(firstMappedFeature, new FeatureCurator().curate(entry, feature).name());
    }

    @Test
    public void canBlacklistFeatures() {
        CurationKey firstOncoKbKey = firstOncoKbBlacklistKey();
        ViccEntry entry = ViccTestFactory.testViccEntryForSource(ViccSource.ONCOKB,
                ViccTestFactory.testOncoKbWithTranscript(firstOncoKbKey.transcript()));

        Feature feature = ImmutableFeature.builder().geneSymbol(firstOncoKbKey.gene()).name(firstOncoKbKey.featureName()).build();
        assertNull(new FeatureCurator().curate(entry, feature));
    }

    @Test
    public void canKeepTrackOfFeatures() {
        FeatureCurator curator = new FeatureCurator();

        ViccEntry entry = ViccTestFactory.testViccEntryForSource(ViccSource.ONCOKB, ViccTestFactory.testOncoKbWithTranscript("any"));
        Feature feature = ImmutableFeature.builder().geneSymbol("any").name("any").build();

        assertNotNull(curator.curate(entry, feature));
        int unusedCurationKeyCount = curator.unusedCurationKeys().size();

        CurationKey blacklistKey = firstOncoKbBlacklistKey();
        ViccEntry blacklistEntry = ViccTestFactory.testViccEntryForSource(ViccSource.ONCOKB,
                ViccTestFactory.testOncoKbWithTranscript(blacklistKey.transcript()));

        Feature blacklistFeature = ImmutableFeature.builder().geneSymbol(blacklistKey.gene()).name(blacklistKey.featureName()).build();

        assertNull(curator.curate(blacklistEntry, blacklistFeature));
        int newUnusedCurationKeyCount = curator.unusedCurationKeys().size();
        assertEquals(1, unusedCurationKeyCount - newUnusedCurationKeyCount);
    }

    @NotNull
    private static CurationKey firstOncoKbMappingKey() {
        for (CurationKey key : CurationFactory.FEATURE_NAME_MAPPINGS.keySet()) {
            if (key.source() == ViccSource.ONCOKB) {
                return key;
            }
        }
        throw new IllegalStateException("No OncoKB mapping keys found!");
    }

    @NotNull
    private static CurationKey firstOncoKbBlacklistKey() {
        for (CurationKey key : CurationFactory.FEATURE_BLACKLIST) {
            if (key.source() == ViccSource.ONCOKB) {
                return key;
            }
        }
        throw new IllegalStateException("No OncoKB blacklist keys found!");
    }
}