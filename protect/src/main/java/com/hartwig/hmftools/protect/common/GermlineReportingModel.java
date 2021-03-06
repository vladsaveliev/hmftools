package com.hartwig.hmftools.protect.common;

import java.util.Map;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

public class GermlineReportingModel {

    private static final Logger LOGGER = LogManager.getLogger(GermlineReportingModel.class);

    @NotNull
    private final Map<String, Boolean> germlineGenesAndNotificationMap;

    GermlineReportingModel(@NotNull final Map<String, Boolean> germlineGenesAndNotificationMap) {
        this.germlineGenesAndNotificationMap = germlineGenesAndNotificationMap;
    }

    @NotNull
    @VisibleForTesting
    Set<String> reportableGermlineGenes() {
        return germlineGenesAndNotificationMap.keySet();
    }

    public boolean notifyAboutGene(@NotNull String germlineGene) {
        Boolean notify = germlineGenesAndNotificationMap.get(germlineGene);
        if (notify == null) {
            LOGGER.warn("Requested notification status for a gene that is not amongst set of reportable germline genes: {}", germlineGene);
        }

        return notify != null ? notify : false;
    }
}
