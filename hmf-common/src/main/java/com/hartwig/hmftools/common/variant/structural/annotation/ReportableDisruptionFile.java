package com.hartwig.hmftools.common.variant.structural.annotation;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.List;
import java.util.StringJoiner;

import com.google.common.collect.Lists;

import org.jetbrains.annotations.NotNull;

public class ReportableDisruptionFile
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0000");
    private static final String DELIMITER = "\t";
    private static final String HEADER_PREFIX = "#";

    public static final String FILE_EXTENSION = ".linx.disruptions.csv";

    @NotNull
    public static String generateFilename(@NotNull final String basePath, @NotNull final String sample)
    {
        return basePath + File.separator + sample + FILE_EXTENSION;
    }

    @NotNull
    public static List<ReportableDisruption> read(final String filePath) throws IOException
    {
        return fromLines(Files.readAllLines(new File(filePath).toPath()));
    }

    public static void write(@NotNull final String filename, @NotNull List<ReportableDisruption> disruptions) throws IOException
    {
        Files.write(new File(filename).toPath(), toLines(disruptions));
    }

    @NotNull
    static List<String> toLines(@NotNull final List<ReportableDisruption> disruptions)
    {
        final List<String> lines = Lists.newArrayList();
        lines.add(header());
        disruptions.stream().map(ReportableDisruptionFile::toString).forEach(lines::add);
        return lines;
    }

    @NotNull
    static List<ReportableDisruption> fromLines(@NotNull List<String> lines)
    {
        return lines.stream().filter(x -> !x.startsWith(HEADER_PREFIX)).map(ReportableDisruptionFile::fromString).collect(toList());
    }

    @NotNull
    private static String header() {
        return new StringJoiner(DELIMITER, HEADER_PREFIX, "")
                .add("svId")
                .add("chromosome")
                .add("orientation")
                .add("strand")
                .add("chrBand")
                .add("gene")
                .add("canonical")
                .add("type")
                .add("ploidy")
                .add("exonUp")
                .add("exonDown")
                .toString();
    }

    @NotNull
    private static String toString(@NotNull final ReportableDisruption disruption)
    {
        return new StringJoiner(DELIMITER)
                .add(String.valueOf(disruption.svId()))
                .add(String.valueOf(disruption.chromosome()))
                .add(String.valueOf(disruption.orientation()))
                .add(String.valueOf(disruption.strand()))
                .add(String.valueOf(disruption.chrBand()))
                .add(String.valueOf(disruption.gene()))
                .add(String.valueOf(disruption.canonical()))
                .add(String.valueOf(disruption.type()))
                .add(DECIMAL_FORMAT.format(disruption.ploidy()))
                .add(String.valueOf(disruption.exonUp()))
                .add(String.valueOf(disruption.exonDown()))
                .toString();
    }

    @NotNull
    private static ReportableDisruption fromString(@NotNull final String clusterData)
    {
        String[] values = clusterData.split(DELIMITER);

        int index = 0;

        return ImmutableReportableDisruption.builder()
                .svId(Integer.valueOf(values[index++]))
                .chromosome(values[index++])
                .orientation(Byte.valueOf(values[index++]))
                .orientation(Integer.valueOf(values[index++]))
                .chrBand(values[index++])
                .gene(values[index++])
                .type(values[index++])
                .canonical(Boolean.valueOf(values[index++]))
                .ploidy(Double.valueOf(values[index++]))
                .exonUp(Integer.valueOf(values[index++]))
                .exonDown(Integer.valueOf(values[index++]))
                .build();
    }
}