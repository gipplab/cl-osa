package com.iandadesign.closa.util.wikidata;

import com.google.common.collect.Sets;
import com.iandadesign.closa.model.WikidataEntity;
import com.iandadesign.closa.util.ConceptUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WikidataEntityExtractorEval {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";


    @Test
    public void evalExtractEntitiesFromMultipleWords() {
        List<String> words = Arrays.asList(
                "recess", "appointments",
                "crisis", "debt", "treasury", "income tax", "discuss",
                "Chinese",
                "state visit",
                "Protests", "acquitted",
                "vote", "Maldives", "controversy", "president",
                "deadline", "ISNA", "19", "bombings", "10", "Central African republic", "peacekeeping", "oil price",
                "arrested", "regrets"
                // problematic:
                // "high court", "nuclear"
                // "Asian", "Iraqi"
        );

        List<WikidataEntity> entitiesToFind = new ArrayList<>();
        entitiesToFind.add(new WikidataEntity("Q176274", "recess"));

        entitiesToFind.add(new WikidataEntity("Q24238221", "rendezvous"));
        // entitiesToFind.add(new WikidataEntity("", "high court"));
        entitiesToFind.add(new WikidataEntity("Q381072", "crisis"));
        entitiesToFind.add(new WikidataEntity("Q3196867", "debt"));
        entitiesToFind.add(new WikidataEntity("Q10756188", "treasury"));
        entitiesToFind.add(new WikidataEntity("Q179222", "income tax"));
        entitiesToFind.add(new WikidataEntity("Q3030248", "discussion"));

        entitiesToFind.add(new WikidataEntity("Q29520", "China"));
        entitiesToFind.add(new WikidataEntity("Q148", "People's Republic of China"));

        entitiesToFind.add(new WikidataEntity("Q2324916", "state visit"));

        entitiesToFind.add(new WikidataEntity("Q273120", "protest"));
        entitiesToFind.add(new WikidataEntity("Q1454723", "acquittal"));

        entitiesToFind.add(new WikidataEntity("Q42904171", "vote"));
        entitiesToFind.add(new WikidataEntity("Q826", "Maldives"));
        entitiesToFind.add(new WikidataEntity("Q1255828", "controversy"));
        entitiesToFind.add(new WikidataEntity("Q30461", "president"));

        entitiesToFind.add(new WikidataEntity("Q1465133", "time limit"));
        // entitiesToFind.add(new WikidataEntity("", "nuclear"));
        entitiesToFind.add(new WikidataEntity("Q1672387", "ISNA"));
        entitiesToFind.add(new WikidataEntity("Q39850", "19"));
        entitiesToFind.add(new WikidataEntity("Q891854", "bombing"));
        entitiesToFind.add(new WikidataEntity("Q23806", "10"));
        entitiesToFind.add(new WikidataEntity("Q929", "Central African republic"));
        entitiesToFind.add(new WikidataEntity("Q10954043", "peacekeeping"));
        entitiesToFind.add(new WikidataEntity("Q48", "Asia"));
        entitiesToFind.add(new WikidataEntity("Q297279", "oil price"));

        entitiesToFind.add(new WikidataEntity("Q1403016", "arrest"));
        entitiesToFind.add(new WikidataEntity("Q796", "Iraq"));

        entitiesToFind.add(new WikidataEntity("Q4729246", "regret"));

        for (String word : words) {
            List<WikidataEntity> foundEntities = WikidataEntityExtractor.extractEntitiesFromTextWithoutDisambiguation(word, "en")
                    .stream().flatMap(List::stream).collect(Collectors.toList());
            System.out.println(foundEntities);

            Assertions.assertTrue(foundEntities.stream().anyMatch(entitiesToFind::contains));
        }
    }


    @Test
    public void testExtractionRuntime() {
        // base number tokens: 222

        /*
            Parallel for extraction, querying and disambiguation only:
            2^0 length: 20.601 seconds
            2^1 length: 10.558 seconds
            2^2 length: 17.335 seconds
            2^3 length: 33.29 seconds
            2^4 length: 65.056 seconds

            Parallel for all streams:
            2^0 length: 20.351 seconds
            2^1 length: 10.339 seconds
            2^2 length: 14.84 seconds
            2^3 length: 29.323 seconds
            2^4 length: 62.042 seconds

            Single-threaded:
            2^0 length: 21.795 seconds
            2^1 length: 13.473 seconds
            2^2 length: 28.428 seconds
            2^3 length: 58.614 seconds
            2^4 length: 106.6 seconds
         */

        try {
            String textEn = FileUtils.readFileToString(new File("src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/en/35157967/0.txt"), StandardCharsets.UTF_8);

            for (int k = 0; k <= 10; k++) {
                long startTime = System.currentTimeMillis();

                WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

                long endTime = System.currentTimeMillis();
                long timeElapsed = endTime - startTime;

                System.out.printf("2^%d length: %s seconds%n", k, timeElapsed / 1000.0);

                textEn = textEn + textEn;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void evalExtractEntitiesFromWikifiedStories() {
        File wikifiedStoriesFolder = new File("src/eval/resources/com/iandadesign/closa/util/wikidata/wikifiedStories");

        int relevantElements = 0;
        int selectedElements = 0;
        int truePositives = 0;
        int falsePositives = 0;

        Map<String, Set<WikidataEntity>> documentContainedEntitiesMap =
                FileUtils.listFiles(wikifiedStoriesFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                        .stream()
                        .sorted()
                        .filter((File file) -> !file.getName().equals(".DS_Store"))
                        .map((File file) -> {
                            try {
                                String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                                Document document = Jsoup.parse(text);
                                return document.body().text();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .collect(Collectors.toMap(
                                (String bodyText) -> bodyText,
                                (String bodyText) -> {
                                    List<String> containedWikipediaTitles = new ArrayList<>();

                                    Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
                                    Matcher matcher = pattern.matcher(bodyText);

                                    while (matcher.find()) {
                                        String group = matcher.group(1);

                                        if (group.contains("|")) {
                                            containedWikipediaTitles.add(group.split("|")[0]);
                                        } else {
                                            containedWikipediaTitles.add(group);
                                        }
                                    }

                                    return containedWikipediaTitles.stream()
                                            .map(title -> ConceptUtil.getWikidataIdByTitle(title, "en"))
                                            .filter(title -> title != null)
                                            .map(WikidataSparqlUtil::getEntityById)
                                            .collect(Collectors.toSet());
                                }));

        for (Map.Entry<String, Set<WikidataEntity>> entry : documentContainedEntitiesMap.entrySet()) {
            Set<WikidataEntity> extractedEntities =
                    new HashSet<>(WikidataEntityExtractor.extractEntitiesFromText(entry.getKey(), "en"));

            Set<WikidataEntity> actualEntities = entry.getValue();

            System.out.println("Actual:     " + Sets.intersection(actualEntities, extractedEntities)
                    + ANSI_BLUE + Sets.difference(actualEntities, extractedEntities) + ANSI_RESET);
            System.out.println("Extracted:  " + ANSI_GREEN + Sets.intersection(actualEntities, extractedEntities)
                    + ANSI_RED + Sets.difference(extractedEntities, actualEntities) + ANSI_RESET);

            // update values
            relevantElements += actualEntities.size();
            selectedElements += extractedEntities.size();
            truePositives += Sets.intersection(actualEntities, extractedEntities).size();
            falsePositives += Sets.difference(extractedEntities, actualEntities).size();
        }

        double precision = (double) truePositives / (selectedElements);
        double recall = (double) truePositives / (relevantElements);
        double fMeasure = 2 * (precision * recall) / (precision + recall);

        System.out.println("Precision  = " + precision);
        System.out.println("Recall     = " + recall);
        System.out.println("F-Measure  = " + fMeasure);
    }


    /**
     * DumpUtil:
     * <p>
     * Precision  = 0.6372093023255814
     * Recall     = 0.6061946902654868
     * F-Measure  = 0.6213151927437641
     * <p>
     * SparqlUtil:
     * <p>
     * Precision  = 0.6089108910891089
     * Recall     = 0.5442477876106194
     * F-Measure  = 0.5747663551401868
     */
    @Test
    public void evalExtractEntitiesFromManuallyAnnotatedText() {
        File csvFile = new File("src/eval/resources/com/iandadesign/closa/util/wikidata/sts-hdl2016.csv");

        int relevantElements = 0;
        int selectedElements = 0;
        int truePositives = 0;
        int falsePositives = 0;

        try (Reader reader = new FileReader(csvFile)) {

            CSVFormat format = CSVFormat.DEFAULT;
            List<CSVRecord> records = CSVParser.parse(reader, format).getRecords();

            for (int i = 0; i < records.size(); i++) {
                System.out.println("////////////////// Pair " + i + " //////////////////");
                CSVRecord record = records.get(i);

                // get information from csv and from SPARQL
                String sentence1 = record.get(5);
                System.out.println("Sentence 1: " + sentence1);

                Set<WikidataEntity> manualEntities1 = Arrays.stream(record.get(9).split(" "))
                        .map(WikidataSparqlUtil::getEntityById)
                        .collect(Collectors.toSet());

                Set<WikidataEntity> extractedEntities1 = new HashSet<>(WikidataEntityExtractor.extractEntitiesFromText(sentence1, "en"));
                System.out.println("Manual:     " + Sets.intersection(manualEntities1, extractedEntities1)
                        + ANSI_BLUE + Sets.difference(manualEntities1, extractedEntities1) + ANSI_RESET);
                System.out.println("Extracted:  " + ANSI_GREEN + Sets.intersection(manualEntities1, extractedEntities1)
                        + ANSI_RED + Sets.difference(extractedEntities1, manualEntities1) + ANSI_RESET);


                String sentence2 = record.get(6);
                System.out.println("Sentence 2: " + sentence2);

                Set<WikidataEntity> manualEntities2 = Arrays.stream(record.get(10).split(" "))
                        .map(WikidataSparqlUtil::getEntityById)
                        .collect(Collectors.toSet());

                Set<WikidataEntity> extractedEntities2 = new HashSet<>(WikidataEntityExtractor.extractEntitiesFromText(sentence2, "en"));

                System.out.println("Manual:     " + Sets.intersection(manualEntities2, extractedEntities2)
                        + ANSI_BLUE + Sets.difference(manualEntities2, extractedEntities2) + ANSI_RESET);
                System.out.println("Extracted:  " + ANSI_GREEN + Sets.intersection(manualEntities2, extractedEntities2)
                        + ANSI_RED + Sets.difference(extractedEntities2, manualEntities2) + ANSI_RESET);


                // update values
                relevantElements += manualEntities1.size();
                relevantElements += manualEntities2.size();

                selectedElements += extractedEntities1.size();
                selectedElements += extractedEntities2.size();

                truePositives += Sets.intersection(manualEntities1, extractedEntities1).size();
                truePositives += Sets.intersection(manualEntities2, extractedEntities2).size();

                falsePositives += Sets.difference(extractedEntities1, manualEntities1).size();
                falsePositives += Sets.difference(extractedEntities2, manualEntities2).size();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double precision = (double) truePositives / (selectedElements);
        double recall = (double) truePositives / (relevantElements);
        double fMeasure = 2 * (precision * recall) / (precision + recall);

        System.out.println("Precision  = " + precision);
        System.out.println("Recall     = " + recall);
        System.out.println("F-Measure  = " + fMeasure);

    }


}
