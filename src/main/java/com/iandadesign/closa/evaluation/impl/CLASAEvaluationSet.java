package com.iandadesign.closa.evaluation.impl;

import com.google.common.collect.ImmutableMap;
import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.TokenUtil;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CLASAEvaluationSet extends EvaluationSet<String> {

    private static final String databaseName = "euBilingual";
    private static final MongoDatabase database;

    // text stores documents with format:
    //
    // {
    //   native: "entities",
    //   foreign: [
    //          {
    //              translation: "encargadas",
    //              probability: 0.0268065
    //          }
    //      ]
    // }
    private static final String translationsCollectionName = "translations";

    private static double enEsMean = 1.138;
    private static double enEsStandardDeviation = 0.631;

    private static double enFrMean = 1.093;
    private static double enFrStandardDeviation = 0.175;

    static {
        database = WikidataDumpUtil.getMongoClient().getDatabase(databaseName);
    }

    private static final Map<String, List<String>> languagePairs = ImmutableMap.of(
            "es-en", Collections.singletonList("EnEs"),
            "fr-en", Collections.singletonList("EnFr"),
            "zh-en", Collections.singletonList("EnZh")
    );

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @throws IOException if files not found.
     */
    public CLASAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage);
    }

    /**
     * Initializes the evaluationSet with max file count. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @param maxFileCount       maximum file count.
     */
    public CLASAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int maxFileCount) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, maxFileCount);
    }

    /**
     * Reads the files from the EU dictionary and stores them to MongoDB.
     */
    public static void extractTranslationProbabilitiesAndStore() {
        try {
            for (Map.Entry<String, List<String>> languageEntry : languagePairs.entrySet()) {

                for (String languagePair : languageEntry.getValue()) {
                    MongoCollection<Document> translationsCollection = database.getCollection(translationsCollectionName + languagePair);

                    if (translationsCollection.count() == 0) {
                        String translationDirection = languagePair.substring(0, 1).equals("En") ? "e2f" : "f2e";
                        boolean englishToForeign = translationDirection.equals("e2f");
                        boolean chinese = languagePair.contains("Zh");

                        if (englishToForeign) {
                            translationsCollection.createIndex(new Document("native", 1), new IndexOptions().unique(true));
                        }

                        Path translationFilePath = chinese
                                ? Paths.get(System.getProperty("user.home") + "/TED_Paracorpus/out/align.1.pt")
                                : Paths.get(System.getProperty("user.home") + "/eu-bilingual/"
                                + languageEntry.getKey() + "/lex." + translationDirection);

                        // check translation files
                        if (Files.notExists(translationFilePath)) {
                            throw new FileNotFoundException("The translation file is missing: " + translationFilePath.toString());
                        }

                        ProgressBar progressBar = new ProgressBar("Preprocess translation files: " + languagePair,
                                Files.lines(translationFilePath).count(), ProgressBarStyle.ASCII);
                        progressBar.start();

                        List<Document> currentTranslationsToInsert = new ArrayList<>();

                        List<String> lines = Files.lines(translationFilePath).sorted().collect(Collectors.toList());

                        for (int i = 0; i < lines.size(); i++) {
                            String line = lines.get(i);
                            String[] parts = line.split(chinese ? "|||" : "\\s");

                            String nativeWord = chinese ? parts[0].trim() : parts[0];
                            String foreignWord = chinese ? parts[1].trim() : parts[1];
                            Double probability = chinese
                                    ? Double.parseDouble(parts[2].trim().split("\\s")[1])
                                    : Double.parseDouble(parts[2]);

                            currentTranslationsToInsert.add(new Document("translation", foreignWord)
                                    .append("probability", probability));

                            // if next native word is different
                            if (i < lines.size() - 1 && !lines.get(i + 1).split("\\s")[0].equals(nativeWord)) {
                                translationsCollection.insertOne(new Document("native", nativeWord)
                                        .append("foreign", currentTranslationsToInsert));

                                currentTranslationsToInsert = new ArrayList<>();
                            }


                            progressBar.step();
                        }

                        progressBar.stop();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("The translation file was missing.");
        }
    }

    @Override
    protected List<String> preProcess(String documentPath, String documentLanguage) {
        try {
            List<Token> tokens = TokenUtil.tokenize(FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8), documentLanguage);
            tokens = TokenUtil.removeStopwords(tokens, documentLanguage);
            tokens = TokenUtil.removePunctuation(tokens);

            return tokens.stream()
                    .map(Token::getToken)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not preprocess file " + documentPath);
        }
    }

    @Override
    protected void performAnalysis() {
        ProgressBar progressBar = new ProgressBar("Calculate similarities:",
                suspiciousIdTokensMap.size(),
                ProgressBarStyle.ASCII);
        progressBar.start();

        AtomicInteger current = new AtomicInteger(0);

        suspiciousIdCandidateScoresMap = suspiciousIdTokensMap.entrySet()
                .parallelStream()
                .filter(suspiciousEntry -> suspiciousIdLanguageMap.containsKey(suspiciousEntry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        suspiciousEntry -> {
                            Path probabilitiesFilePath = Paths.get(System.getProperty("user.home") + "/preprocessed-clasa/" + suspiciousEntry.getKey());
                            File probabilitiesFile = new File(probabilitiesFilePath.toUri());

                            String suspiciousLanguage = suspiciousIdLanguageMap.get(suspiciousEntry.getKey());
                            String candidateLanguage = candidateIdLanguageMap.get(candidateIdLanguageMap.keySet().iterator().next());

                            double mean = candidateLanguage.equals("fr") ? enFrMean : enEsMean;
                            double standardDeviation = candidateLanguage.equals("fr") ? enFrStandardDeviation : enEsStandardDeviation;

                            int suspiciousSize = suspiciousEntry.getValue().size();

                            try {
                                if (Files.exists(probabilitiesFilePath) &&
                                        FileUtils.readLines(probabilitiesFile, StandardCharsets.UTF_8).size() == candidateIdTokensMap.size()) {

                                    List<String> lines = FileUtils.readLines(probabilitiesFile, StandardCharsets.UTF_8);

                                    return lines.stream()
                                            .collect(Collectors.toMap(line -> line.split(";")[0],
                                                    line -> {
                                                        String candidateId = line.split(";")[0];
                                                        double candidateSize = candidateIdTokensMap.get(candidateId).size();
                                                        double lengthModel =
                                                                Math.exp(-0.5
                                                                        * Math.pow((candidateSize / suspiciousSize - mean) / standardDeviation, 2.0));

                                                        return lengthModel * Double.parseDouble(line.split(";")[1]);
                                                    }));
                                } else {
                                    probabilitiesFile.getParentFile().mkdirs();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            Map<String, Double> candidateIdProbabilityMap = new HashMap<>();

                            for (Map<String, List<String>> subMap : getSubMaps(candidateIdTokensMap, 50)) {
                                candidateIdProbabilityMap.putAll(getTranslationProbabilitiesByCandidate(
                                        suspiciousEntry.getValue(),
                                        subMap,
                                        suspiciousLanguage,
                                        candidateLanguage)
                                        .entrySet()
                                        .stream()
                                        .collect(Collectors.toMap(Map.Entry::getKey,
                                                candidateEntry -> {
                                                    double candidateSize = candidateIdTokensMap.get(candidateEntry.getKey()).size();
                                                    double lengthModel = Math.exp(-0.5 * Math.pow((candidateSize / suspiciousSize - mean) / standardDeviation, 2.0));
                                                    return lengthModel * candidateEntry.getValue();
                                                })));
                            }

                            progressBar.stepTo(current.incrementAndGet());

                            try {
                                List<String> lines = candidateIdProbabilityMap.entrySet()
                                        .stream()
                                        .map(entry -> entry.getKey() + ";" + entry.getValue())
                                        .collect(Collectors.toList());

                                FileUtils.writeStringToFile(probabilitiesFile, StringUtils.join(lines, "\n"), StandardCharsets.UTF_8);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            return candidateIdProbabilityMap;
                        }));

        progressBar.stop();
    }


    private List<Map<String, List<String>>> getSubMaps(Map<String, List<String>> map, int chunks) {
        List<Map<String, List<String>>> chunkedList = new ArrayList<>();
        int chunkSize = map.size() / chunks;

        int i = 0;

        Map<String, List<String>> currentMap = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            currentMap.put(entry.getKey(), entry.getValue());

            if (i >= chunkSize) {
                chunkedList.add(currentMap);
                currentMap = new HashMap<>();
                i = 0;
            } else {
                i++;
            }
        }

        if (chunkedList.size() < chunks) {
            chunkedList.add(currentMap);
        }

        return chunkedList;
    }

    /**
     * CL-ASA algorithm.
     *
     * @param nativeWords
     * @param foreignWords
     * @param nativeLanguage
     * @param foreignLanguage
     * @return
     */
    private double getTranslationProbability(
            List<String> nativeWords,
            List<String> foreignWords,
            String nativeLanguage,
            String foreignLanguage
    ) {
        MongoCollection<Document> translationsCollection = database.getCollection(translationsCollectionName
                + StringUtils.capitalize(nativeLanguage)
                + StringUtils.capitalize(foreignLanguage));


        Document totalProbabilityDocument = translationsCollection.aggregate(Arrays.asList(
                new Document("$match",
                        new Document("$or", nativeWords.stream()
                                .map(nativeWord -> new Document("native", nativeWord))
                                .collect(Collectors.toList()))),
                new Document("$unwind", "$foreign"),
                new Document("$match",
                        new Document("$or", foreignWords.stream()
                                .map(foreignWord -> new Document("foreign.translation", foreignWord))
                                .collect(Collectors.toList()))),
                new Document("$group",
                        new Document("_id", null)
                                .append("totalProbability",
                                        new Document("$sum", "$foreign.probability")))
        )).first();

        if (totalProbabilityDocument == null) {
            return 0.0;
        }

        double totalProbability = totalProbabilityDocument.containsKey("totalProbability")
                ? totalProbabilityDocument.getDouble("totalProbability")
                : 0.0;

        return totalProbability;
    }

    /**
     * CL-ASA algorithm.
     *
     * @param nativeWords
     * @param foreignWordsMap
     * @param nativeLanguage
     * @param foreignLanguage
     * @return
     */
    private Map<String, Double> getTranslationProbabilitiesByCandidate(
            List<String> nativeWords,
            Map<String, List<String>> foreignWordsMap,
            String nativeLanguage,
            String foreignLanguage
    ) {
        Map<String, Double> translationProbabilitiesByCandidate = new HashMap<>();

        MongoCollection<Document> translationsCollection = database.getCollection(translationsCollectionName
                + StringUtils.capitalize(nativeLanguage)
                + StringUtils.capitalize(foreignLanguage));

        AggregateIterable<Document> totalProbabilityDocuments = translationsCollection.aggregate(Arrays.asList(
                new Document("$match",
                        new Document("$or", nativeWords.stream()
                                .map(nativeWord -> new Document("native", nativeWord))
                                .collect(Collectors.toList()))),
                new Document("$unwind", "$foreign"),
                new Document("$addFields",
                        new Document("candidateId", foreignWordsMap.keySet())),
                new Document("$unwind", "$candidateId"),
                new Document("$match",
                        new Document("$or",
                                foreignWordsMap.entrySet()
                                        .stream()
                                        .map(foreignWordEntry ->
                                                new Document("foreign.translation",
                                                        new Document("$in", foreignWordEntry.getValue()))
                                                        .append("candidateId", foreignWordEntry.getKey()))
                                        .collect(Collectors.toList()))),
                new Document("$group",
                        new Document("_id", "$candidateId")
                                .append("totalProbability",
                                        new Document("$sum", "$foreign.probability")))
        ));

        int i = 0;

        for (Document totalProbabilityDocument : totalProbabilityDocuments) {
            // System.out.println("totalProbDoc = " + totalProbabilityDocument);
            String candidateId = totalProbabilityDocument.getString("_id");
            double totalProbability = totalProbabilityDocument.getDouble("totalProbability");
            translationProbabilitiesByCandidate.put(candidateId, totalProbability);
            i++;
        }

        if (i == 0) {
            System.out.println("No matches!");
        }

        int j = 0;

        for (String foreignWordsKey : foreignWordsMap.keySet()) {
            if (!translationProbabilitiesByCandidate.containsKey(foreignWordsKey)) {
                translationProbabilitiesByCandidate.put(foreignWordsKey, 0.0);

            } else {
                j++;
            }
        }

        // System.out.println("Zero probs = " + j);

        return translationProbabilitiesByCandidate;
    }
}
