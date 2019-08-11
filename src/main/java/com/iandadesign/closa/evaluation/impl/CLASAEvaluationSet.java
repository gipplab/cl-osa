package com.iandadesign.closa.evaluation.impl;

import com.google.common.collect.ImmutableMap;
import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.TokenUtil;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import com.mongodb.client.FindIterable;
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

    static {
        database = WikidataDumpUtil.getMongoClient().getDatabase(databaseName);
    }

    private static final Map<String, List<String>> languagePairs = ImmutableMap.of(
            "es-en", Collections.singletonList("EnEs"),
            "fr-en", Collections.singletonList("EnFr")
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
    private void extractTranslationProbabilitiesAndStore() {
        try {
            for (Map.Entry<String, List<String>> languageEntry : languagePairs.entrySet()) {

                for (String languagePair : languageEntry.getValue()) {
                    MongoCollection<Document> translationsCollection = database.getCollection(translationsCollectionName + languagePair);

                    if (translationsCollection.count() == 0) {

                        String translationDirection = languagePair.substring(0, 1).equals("En") ? "e2f" : "f2e";
                        boolean englishToForeign = translationDirection.equals("e2f");

                        if (englishToForeign) {
                            translationsCollection.createIndex(new Document("native", 1), new IndexOptions().unique(true));
                        }

                        Path translationFilePath = Paths.get(System.getProperty("user.home") + "/eu-bilingual/"
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
                            String[] parts = line.split("\\s");

                            String nativeWord = parts[0];
                            String foreignWord = parts[1];
                            Double probability = Double.parseDouble(parts[2]);

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
        extractTranslationProbabilitiesAndStore();

        try {
            List<String> tokens = TokenUtil.tokenize(FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8), documentLanguage)
                    .stream()
                    .map(Token::getToken)
                    .collect(Collectors.toList());

            return tokens;
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Could not preprocess file " + documentPath);
        }
    }

    @Override
    protected void performAnalysis() {
        ProgressBar progressBar = new ProgressBar("Calculate similarities:",
                suspiciousIdTokensMap.size() * candidateIdTokensMap.size(),
                ProgressBarStyle.ASCII);
        progressBar.start();

        AtomicInteger current = new AtomicInteger(0);

        suspiciousIdCandidateScoresMap = suspiciousIdTokensMap.entrySet()
                .parallelStream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        suspiciousEntry -> {
                            String suspiciousLanguage = suspiciousIdLanguageMap.get(suspiciousEntry.getKey());

                            return candidateIdTokensMap.entrySet()
                                    .parallelStream()
                                    .collect(Collectors.toMap(Map.Entry::getKey,
                                            candidateEntry -> {
                                                progressBar.stepTo(current.incrementAndGet());

                                                return getTranslationProbability(
                                                        suspiciousEntry.getValue(),
                                                        candidateEntry.getValue(),
                                                        suspiciousLanguage,
                                                        candidateIdLanguageMap.get(candidateEntry.getKey()));
                                            }));
                        }));

        progressBar.stop();
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

        List<Double> similarities = new ArrayList<>();

        Document query = new Document("$or", nativeWords.stream().map(nativeWord -> new Document("native", nativeWord)));
        FindIterable<Document> nativeWordDocuments = translationsCollection.find(query);

        for (Document document : nativeWordDocuments) {
            List<Document> foreignProbabilities = document.get("foreign", ArrayList.class);

            List<Document> foreignIntersection = foreignProbabilities.stream()
                    .filter((Document foreignProbabilityDocument) ->
                            foreignWords.contains(foreignProbabilityDocument.getString("translation")))
                    .collect(Collectors.toList());

            if (!foreignIntersection.isEmpty()) {
                similarities.add(foreignIntersection.stream()
                        .map(foreignDocument -> foreignDocument.getDouble("probability"))
                        .mapToDouble(Double::doubleValue)
                        .sum());
            }
        }

        double similarity = similarities.stream()
                .reduce(1.0, (a, b) -> a + b);

        double lengthModel = 1.0 / Math.pow(nativeWords.size() + 1.0, foreignWords.size());

        return lengthModel * similarity;
    }
}
