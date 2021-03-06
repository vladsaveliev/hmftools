package com.hartwig.hmftools.strelka;

import static com.hartwig.hmftools.strelka.StrelkaPostProcessApplication.generateOutputHeader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;

import com.hartwig.hmftools.strelka.mnv.MNVDetector;
import com.hartwig.hmftools.strelka.mnv.PotentialMNVRegion;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.writer.VariantContextWriter;
import htsjdk.variant.variantcontext.writer.VariantContextWriterBuilder;
import htsjdk.variant.vcf.VCFFileReader;
import htsjdk.variant.vcf.VCFHeader;

public class MNVDetectorApplication {

    private static final Logger LOGGER = LogManager.getLogger(MNVDetectorApplication.class);

    private static final String INPUT_VCF = "v";
    private static final String OUTPUT_BED = "bed_out";
    private static final String OUTPUT_VCF = "vcf_out";
    private static final String STRELKA = "strelka";

    public static void main(final String... args) throws ParseException, IOException {
        final Options options = createOptions();
        final CommandLine cmd = createCommandLine(options, args);

        final String inputVcf = cmd.getOptionValue(INPUT_VCF);
        final String outputBed = cmd.getOptionValue(OUTPUT_BED);
        final String outputVcf = cmd.getOptionValue(OUTPUT_VCF);

        if (inputVcf == null || outputBed == null || outputVcf == null) {
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("MNV Detector", options);
            System.exit(1);
        }
        LOGGER.info("Searching for mnvs in {}", inputVcf);
        processVariants(inputVcf, outputVcf, outputBed, cmd.hasOption(STRELKA));
    }

    @NotNull
    private static Options createOptions() {
        final Options options = new Options();
        options.addOption(INPUT_VCF, true, "Path towards the input VCF");
        options.addOption(OUTPUT_BED, true, "Path towards the output BED");
        options.addOption(OUTPUT_VCF, true, "Path towards the output VCF");
        options.addOption(STRELKA, false, "Expect raw strelka input");
        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final Options options, @NotNull final String... args) throws ParseException {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    private static void processVariants(@NotNull final String filePath, @NotNull final String outputVcf, @NotNull final String outputBed,
            boolean strelka) throws IOException {
        final VCFFileReader vcfReader = new VCFFileReader(new File(filePath), false);
        final VCFHeader outputHeader =
                strelka ? generateOutputHeader(vcfReader.getFileHeader(), StrelkaPostProcess.TUMOR_GENOTYPE) : vcfReader.getFileHeader();
        final BufferedWriter bedWriter = new BufferedWriter(new FileWriter(outputBed, false));
        final VariantContextWriter vcfWriter = new VariantContextWriterBuilder().setOutputFile(outputVcf)
                .setReferenceDictionary(outputHeader.getSequenceDictionary())
                .build();
        vcfWriter.writeHeader(outputHeader);

        Pair<PotentialMNVRegion, Optional<PotentialMNVRegion>> outputPair = ImmutablePair.of(PotentialMNVRegion.empty(), Optional.empty());
        for (final VariantContext rawVariant : vcfReader) {
            final VariantContext variant =
                    strelka ? StrelkaPostProcess.simplifyVariant(rawVariant, StrelkaPostProcess.TUMOR_GENOTYPE) : rawVariant;

            final PotentialMNVRegion potentialMNVregion = outputPair.getLeft();
            outputPair = MNVDetector.addMnvToRegion(potentialMNVregion, variant);
            outputPair.getRight()
                    .ifPresent(mnvRegion -> filterMnvRegion(mnvRegion).ifPresent(filteredRegion -> writeMnvRegionToFiles(filteredRegion,
                            vcfWriter,
                            bedWriter,
                            "\n")));
        }
        filterMnvRegion(outputPair.getLeft()).ifPresent(mnvRegion -> writeMnvRegionToFiles(mnvRegion, vcfWriter, bedWriter, ""));
        vcfWriter.close();
        vcfReader.close();
        bedWriter.close();
        LOGGER.info("Written output variants to {}. Written bed regions to {}.", outputVcf, outputBed);
    }

    private static void writeMnvRegionToFiles(@NotNull final PotentialMNVRegion mnvRegion, @NotNull final VariantContextWriter vcfWriter,
            @NotNull final BufferedWriter bedWriter, final String lineTerminator) {
        mnvRegion.variants().forEach(vcfWriter::add);
        try {
            bedWriter.write(mnvRegion.chromosome() + "\t" + mnvRegion.start() + "\t" + (mnvRegion.end() - 1) + lineTerminator);
        } catch (IOException e) {
            LOGGER.error("Couldn't write mnv to bed file");
        }
    }

    @NotNull
    private static Optional<PotentialMNVRegion> filterMnvRegion(@NotNull final PotentialMNVRegion potentialMnvRegion) {
        if (potentialMnvRegion.potentialMnvs().size() == 0) {
            return Optional.empty();
        } else {
            return Optional.of(potentialMnvRegion);
        }
    }
}
