package com.iandadesign.closa.commandLine;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.language.LanguageDetector;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    private static final Options options = new Options();

    static {
        Option suspiciousPath = new Option("s", "suspiciousPath", true, "Suspicious file path.");
        suspiciousPath.setRequired(true);
        options.addOption(suspiciousPath);

        Option candidateFolderPath = new Option("c", "candidateFolderPath", true, "Candidate folder path.");
        candidateFolderPath.setRequired(true);
        options.addOption(candidateFolderPath);

        Option outputPath = new Option("o", "outputPath", true, "Output file path (will be overwritten).");
        outputPath.setRequired(true);
        options.addOption(outputPath);

        Option languages = new Option("l", "languages", true, "Specify language codes to consider.");
        options.addOption(languages);

        Option topics = new Option("t", "topics", true, "Specify topics to consider (biology, neutral, fiction).");
        options.addOption(topics);
    }


    /**
     * Compute CL-OSA scores and write to file.
     *
     * @param suspiciousPath   suspicious file path.
     * @param candidatePaths   candidate file paths.
     * @param outputPath       output file path.
     * @param languageDetector language detector.
     * @param textClassifier   text classifer.
     */
    private static void computeScores(
            String suspiciousPath,
            List<String> candidatePaths,
            String outputPath,
            LanguageDetector languageDetector,
            TextClassifier textClassifier) {

        try {
            OntologyBasedSimilarityAnalysis analysis = new OntologyBasedSimilarityAnalysis(languageDetector, textClassifier);

            Map<String, Double> scores = analysis.executeAlgorithmAndComputeScores(suspiciousPath, candidatePaths);

            List<String> outputLines = new ArrayList<>();
            outputLines.add(String.format("Candidate file path;Similarity score to %s", suspiciousPath));

            outputLines.addAll(scores.entrySet().stream()
                    .map(entry -> entry.getKey() + ";" + entry.getValue())
                    .collect(Collectors.toList()));

            FileUtils.writeLines(new File(outputPath), outputLines);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Main method for entity extractor.
     *
     * @param args input arguments
     */
    public static void main(String[] args) {
        // create the parser
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine line;

        try {
            // parse the command line arguments
            line = parser.parse(options, args);
        } catch (ParseException e) {
            // something went wrong
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            return;
        }

        String suspiciousPath = line.getOptionValue("s");
        String candidateFolderPath = line.getOptionValue("c");

        List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        String outputPath = line.getOptionValue("o");

        // build language detector
        LanguageDetector languageDetector;

        if (line.hasOption("l")) {
            languageDetector = new LanguageDetector(Arrays.stream(line.getOptionValues("l"))
                    .collect(Collectors.toList()));
        } else {
            languageDetector = new LanguageDetector();
        }

        // build text classifier
        TextClassifier textClassifier;

        if (line.hasOption("t")) {
            textClassifier = new TextClassifier(Arrays.stream(line.getOptionValues("t"))
                    .collect(Collectors.toList()));
        } else {
            textClassifier = new TextClassifier();
        }

        computeScores(suspiciousPath, candidatePaths, outputPath, languageDetector, textClassifier);

    }

}
