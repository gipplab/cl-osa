package com.fabianmarquart.closa.commandLine;

import com.fabianmarquart.closa.classification.TextClassifier;
import com.fabianmarquart.closa.language.LanguageDetector;
import com.fabianmarquart.closa.model.WikidataEntity;
import com.fabianmarquart.closa.util.wikidata.WikidataEntityExtractor;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WikidataEntityExtraction {

    private static final Options options = new Options();

    static {
        options.addOption(new Option("i", "inputPath", true, "Input file path."));
        options.addOption(new Option("o", "outputPath", true, "Output file path (will be overwritten)."));
        options.addOption(new Option("l", "languages", true, "Specify language codes to consider."));
        options.addOption(new Option("t", "topics", true, "Specify topics to consider (biology, neutral, fiction)."));
        options.addOption(new Option("a", "annotation", false, "Specify output of HTML annotated file."));
    }

    /**
     * Extract entities from input path given text file, with language detector, text classifier, annotation
     * flag and write result to output path file.
     *
     * @param inputPath        input path
     * @param outputPath       output path
     * @param languageDetector language detector
     * @param textClassifier   text classifer
     * @param annotationOutput annotate if true, entities only if false
     */
    private static void extractEntities(
            String inputPath,
            String outputPath,
            LanguageDetector languageDetector,
            TextClassifier textClassifier,
            boolean annotationOutput) {

        try {
            String inputText = FileUtils.readFileToString(new File(inputPath), StandardCharsets.UTF_8);
            String inputTextLanguage = languageDetector.detectLanguage(inputText);

            if (annotationOutput) {
                String annotatedText = WikidataEntityExtractor.annotateEntitiesInText(
                        inputText,
                        inputTextLanguage,
                        textClassifier.classifyText(inputText, inputTextLanguage));

                FileUtils.writeStringToFile(new File(outputPath), annotatedText, StandardCharsets.UTF_8);
            } else {
                List<WikidataEntity> extractedEntities = WikidataEntityExtractor.extractEntitiesFromText(
                        inputText,
                        inputTextLanguage,
                        textClassifier.classifyText(inputText, inputTextLanguage));

                FileUtils.writeLines(new File(outputPath), extractedEntities.stream().map(WikidataEntity::getId).collect(Collectors.toList()));
            }
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

        String inputPath = line.getOptionValue("i");
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

        boolean annotationOutput = line.hasOption("a");

        extractEntities(inputPath, outputPath, languageDetector, textClassifier, annotationOutput);
    }

}
