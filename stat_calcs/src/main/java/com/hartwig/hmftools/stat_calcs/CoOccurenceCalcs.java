package com.hartwig.hmftools.stat_calcs;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.jetbrains.annotations.NotNull;

public class CoOccurenceCalcs
{
    private ThreeVarCoOccurence mThreeVarCoOccurence;
    private TwoVarCoOccurence mTwoVarCoOccurence;
    private SampleCountsCoOccurence mSampleCountsCoOccurence;

    private static final String LOG_DEBUG = "log_debug";
    private static final String DATA_OUTPUT_DIR = "output_dir";
    private static final String DRIVER_GENES_FILE = "driver_genes_file";
    private static final String SAMPLE_COUNTS_FILE = "sample_counts_file";
    private static final String THREE_VAR_INPUT_FILE = "three_var_input_file";
    private static final String TWO_VAR_INPUT_FILE = "two_var_input_file";

    private static final Logger LOGGER = LogManager.getLogger(CoOccurenceCalcs.class);

    public static void main(@NotNull final String[] args) throws ParseException
    {
        final Options options = createBasicOptions();
        final CommandLine cmd = createCommandLine(args, options);

        if (cmd.hasOption(LOG_DEBUG))
        {
            Configurator.setRootLevel(Level.DEBUG);
        }

        String outputDir = cmd.getOptionValue(DATA_OUTPUT_DIR);

        CoOccurenceCalcs statsRoutines = new CoOccurenceCalcs();
        statsRoutines.loadConfig(cmd, outputDir);
        statsRoutines.runStatistics();
        LOGGER.info("run complete");
    }

    private static Options createBasicOptions()
    {
        final Options options = new Options();
        options.addOption(DRIVER_GENES_FILE, true, "Drive genes file");
        options.addOption(SAMPLE_COUNTS_FILE, true, "Sample counts file");
        options.addOption(THREE_VAR_INPUT_FILE, true, "Sample data with grouping and 2 variable");
        options.addOption(TWO_VAR_INPUT_FILE, true, "Sample data with 2 variables");
        options.addOption(DATA_OUTPUT_DIR, true, "Output directory");
        options.addOption(LOG_DEBUG, false, "Verbose logging");
        return options;
    }

    @NotNull
    private static CommandLine createCommandLine(@NotNull final String[] args, @NotNull final Options options) throws ParseException
    {
        final CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    public CoOccurenceCalcs()
    {
        mThreeVarCoOccurence = null;
        mSampleCountsCoOccurence = null;
        mTwoVarCoOccurence = null;
    }

    public boolean loadConfig(final CommandLine cmd, final String outputDir)
    {
        boolean valid = true;

        if(cmd.hasOption(DRIVER_GENES_FILE) && cmd.hasOption(SAMPLE_COUNTS_FILE))
        {
            mSampleCountsCoOccurence = new SampleCountsCoOccurence();

            valid = mSampleCountsCoOccurence.initialise(
                    cmd.getOptionValue(SAMPLE_COUNTS_FILE), cmd.getOptionValue(DRIVER_GENES_FILE), outputDir);
        }

        if(cmd.hasOption(THREE_VAR_INPUT_FILE))
        {
            mThreeVarCoOccurence = new ThreeVarCoOccurence();
            valid = mThreeVarCoOccurence.initialise(cmd.getOptionValue(THREE_VAR_INPUT_FILE), outputDir);
        }

        if(cmd.hasOption(TWO_VAR_INPUT_FILE))
        {
            mTwoVarCoOccurence = new TwoVarCoOccurence();
            valid = mTwoVarCoOccurence.initialise(cmd.getOptionValue(TWO_VAR_INPUT_FILE), outputDir);
        }

        return valid;
    }

    public void runStatistics()
    {
        if(mSampleCountsCoOccurence != null)
            mSampleCountsCoOccurence.run();

        if(mThreeVarCoOccurence != null)
            mThreeVarCoOccurence.run();

        if(mTwoVarCoOccurence != null)
            mTwoVarCoOccurence.run();
    }

}

