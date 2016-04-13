package com.hartwig.hmftools.boggs;

import com.hartwig.hmftools.boggs.flagstatreader.SambambaFlagstatParser;
import com.hartwig.hmftools.boggs.runreader.PatientExtractor;
import org.apache.commons.cli.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class BoggsRunner {

    private static final String RUN_DIRECTORY = "rundir";

    public static void main(String[] args) throws ParseException, IOException {
        Options options = createOptions();
        CommandLine cmd = createCommandLine(args, options);

        String runDirectory = cmd.getOptionValue(RUN_DIRECTORY);

        if (runDirectory == null) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("boggs", options);
        } else {
            PatientExtractor extractor = new PatientExtractor(new SambambaFlagstatParser());
            PatientData patient = extractor.extractFromRunDirectory(runDirectory);
            System.out.println(patient);
        }
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull String[] args, @NotNull Options options)
            throws ParseException {
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    @NotNull
    private static Options createOptions() {
        Options options = new Options();
        options.addOption(RUN_DIRECTORY, true, "The path containing the data for a single run");
        return options;
    }
}
