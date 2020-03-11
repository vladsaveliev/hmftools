package com.hartwig.hmftools.knowledgebasegenerator.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.hartwig.hmftools.knowledgebasegenerator.AllGenomicEvents;
import com.hartwig.hmftools.knowledgebasegenerator.cnv.KnownAmplificationDeletion;

import org.jetbrains.annotations.NotNull;

public class GeneratingOutputFiles {

    private static final String DELIMITER = "\t";
    private static final String NEW_LINE = "\n";

    private static final String UNIQUE_KNOWN_AMPLIFICATION_TSV = "uniqueKnownAmplification.tsv";
    private static final String UNIQUE_KNOWN_DELETION_TSV = "uniqueKnownDeletion.tsv";
    private static final String KNOWN_AMPLIFICATION_INFO_TSV = "knownAmplificationInfo.tsv";
    private static final String KNOWN_DELETION_INFO_TSV = "knownDeletionInfo.tsv";
    private static final String ACTIONABLE_CNV_TSV = "actionableCNV.tsv";

    public static void generatingOutputFiles(@NotNull String outputDir, @NotNull AllGenomicEvents genomicEvents) throws IOException {
        generateUniqueKnownAmplification(outputDir + File.separator + UNIQUE_KNOWN_AMPLIFICATION_TSV, genomicEvents);
        generateUniqueKnownDeletions(outputDir + File.separator + UNIQUE_KNOWN_DELETION_TSV, genomicEvents);
        generateInfoKnownAmplification(outputDir + File.separator + KNOWN_AMPLIFICATION_INFO_TSV, genomicEvents);
        generateInfoKnownDeletions(outputDir + File.separator + KNOWN_DELETION_INFO_TSV, genomicEvents);
        generateActionableCNV(outputDir + File.separator + ACTIONABLE_CNV_TSV, genomicEvents);
    }

    private static void generateUniqueKnownAmplification(@NotNull String outputFile, @NotNull AllGenomicEvents genomicEvents) throws IOException {
        String headerknownCNV = "Gene" + NEW_LINE;

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
        writer.write(headerknownCNV);
        for (String amplification : genomicEvents.uniqueAmplification()) {
            writer.write(amplification + NEW_LINE);
        }
        writer.close();
    }

    private static void generateUniqueKnownDeletions(@NotNull String outputFile, @NotNull AllGenomicEvents genomicEvents) throws IOException {
        String headerknownCNV = "Gene" + NEW_LINE;
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
        writer.write(headerknownCNV);
        for (String deletion : genomicEvents.uniqueDeletions()) {
            writer.write(deletion + NEW_LINE);
        }
        writer.close();
    }

    private static void generateInfoKnownAmplification(@NotNull String outputFile, @NotNull AllGenomicEvents genomicEvents) throws IOException {
        String headerknownCNV = "Gene" + DELIMITER + "Source" + NEW_LINE;

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
        writer.write(headerknownCNV);
        for (KnownAmplificationDeletion amplification : genomicEvents.knownAmplifications()) {
            writer.write(amplification.gene() + DELIMITER + amplification.source() + NEW_LINE);
        }
        writer.close();
    }

    private static void generateInfoKnownDeletions(@NotNull String outputFile, @NotNull AllGenomicEvents genomicEvents) throws IOException {
        String headerknownCNV = "Gene" + DELIMITER + "Source" + NEW_LINE;
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
        writer.write(headerknownCNV);
        for (KnownAmplificationDeletion deletion : genomicEvents.knownDeletions()) {
            writer.write(deletion.gene() + DELIMITER + deletion.source() + NEW_LINE);
        }
        writer.close();
    }

    private static void generateActionableCNV(@NotNull String outputFile, @NotNull AllGenomicEvents genomicEvents) throws IOException {
        String headerActionableCNV =
                "Gene" + DELIMITER + "Type" + DELIMITER + "Source" + DELIMITER + "Links" + DELIMITER + "Drug" + DELIMITER + "Drug Type"
                        + DELIMITER + "Cancer Type" + DELIMITER + "Level" + DELIMITER + "Direction" + NEW_LINE;
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true));
        writer.write(headerActionableCNV);
        //TODO determine actionable CNVs
        writer.write("");
        writer.close();

    }
}
