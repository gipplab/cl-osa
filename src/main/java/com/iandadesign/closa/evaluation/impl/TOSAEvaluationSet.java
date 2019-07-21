package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.model.Dictionary;
import com.iandadesign.closa.model.WikidataEntity;
import com.iandadesign.closa.util.TranslationUtil;
import com.iandadesign.closa.util.wikidata.WikidataEntityExtractor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cross-Language Ontology-based Similarity Analysis, by Fabian Marquart, 2018.
 * <p>
 * Created by Fabian Marquart on 2018/08/06.
 */
public class TOSAEvaluationSet extends EvaluationSet {

    public TOSAEvaluationSet(File suspiciousFolder, File candidateFolder) throws IOException {
        super(suspiciousFolder, candidateFolder);
    }

    public TOSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage);
    }

    public TOSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int fileCount) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, fileCount);
    }

    public TOSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage,
                             File candidateFolder, String candidateLanguage,
                             File extraCandidateFolder) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, extraCandidateFolder);
    }

    public TOSAEvaluationSet(File suspiciousFolder, File candidateFolder, int fileCount) {
        super(suspiciousFolder, candidateFolder, fileCount);
    }


    /**
     * T+OSA analysis.
     */
    @Override
    protected void performAnalysis() {
        performCosineSimilarityAnalysis();
    }


    /**
     * T+OSA analysis by querying preprocessed tokens from an inverted-index dictionary.
     */
    private void performCosineSimilarityAnalysis() {
        // create dictionary
        System.out.println("Create dictionary \n");
        Dictionary<String> dictionary = new Dictionary<>(candidateIdTokensMap);

        suspiciousIdCandidateScoresMap = new HashMap<>();

        // perform detailed analysis
        System.out.println("Perform detailed analysis \n");

        suspiciousIdTokensMap.forEach((suspiciousId, suspiciousConcepts) -> {

            // look in dictionary
            Map<String, Double> candidateScoreMap = dictionary.query(suspiciousConcepts);

            if (candidateScoreMap.isEmpty()) {
                System.out.println("False negative. Did not detect");
                System.out.println(suspiciousId);
                System.out.println("in");
                System.out.println(suspiciousIdCandidateIdMap.get(suspiciousId));
            } else {
                String retrievedCandidateId = candidateScoreMap.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .get()
                        .getKey();

                if (suspiciousIdCandidateIdMap.get(suspiciousId).equals(retrievedCandidateId)) {
                    System.out.println("True positive.");
                } else {
                    System.out.println("False positive. Falsely detected ");
                    System.out.println(suspiciousId);
                    System.out.println("in");
                    System.out.println(retrievedCandidateId);
                }

            }

            suspiciousIdCandidateScoresMap.put(suspiciousId, candidateScoreMap);

        });
    }


    /**
     * CL-OSA preprocessing: translation -> entity extraction
     *
     * @param documentPath     document text
     * @param documentLanguage the document's language
     * @return concepts.
     */
    @Override
    protected List<String> preProcess(String documentPath, String documentLanguage) {
        try {
            // read in the file
            String documentText = FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8);

            // translate the file
            String translatedDocumentText;

            String translatedDocumentPath = documentPath.replace("pds", "pds/preprocessed-t");

            // if the file has already been translated
            if (documentLanguage.equals("en")) {
                translatedDocumentText = documentText;
            } else if (Files.exists(Paths.get(translatedDocumentPath)) // file exists and is not empty
                    && !FileUtils.readFileToString(new File(translatedDocumentPath), StandardCharsets.UTF_8).equals("")) {
                // read the translation
                System.out.println("Read translation from file");
                translatedDocumentText = FileUtils.readFileToString(new File(translatedDocumentPath), StandardCharsets.UTF_8);
            } else {
                // translate
                System.out.println("Create new translation using Yandex");
                try {
                    translatedDocumentText = TranslationUtil.translate(documentText, documentLanguage, "en");
                    if (!documentLanguage.equals("en")) {
                        // save the translation
                        saveDocumentTranslationToFile(documentPath, translatedDocumentText);
                    }
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }

            String documentEntitiesPath = documentPath.replace("pds", "pds/preprocessed/" + this.getClass().getSimpleName());

            // if the file has already been pre-processed
            if (Files.exists(Paths.get(documentEntitiesPath)) // file exists and is not empty
                    && !FileUtils.readLines(new File(documentEntitiesPath), StandardCharsets.UTF_8).isEmpty()) {
                return new ArrayList<>(FileUtils.readLines(new File(documentEntitiesPath), StandardCharsets.UTF_8));
            }

            // pre-process the file
            return WikidataEntityExtractor.extractEntitiesFromText(translatedDocumentText,
                    "en",
                    new TextClassifier().classifyText(translatedDocumentText, "en"))
                    .stream()
                    .map(WikidataEntity::getId)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Save the translation to file.
     *
     * @param originalDocumentPath the original document's path
     * @param documentTranslation  the document's translation as string
     */
    private void saveDocumentTranslationToFile(String originalDocumentPath, String documentTranslation) {
        Path newFullPath = Paths.get(originalDocumentPath.replace("pds", "pds/preprocessed-t"));
        File newFile = new File(newFullPath.toString());

        try {
            if (!Files.exists(newFullPath.getParent())) {
                Files.createDirectories(newFullPath.getParent());
            }

            if (Files.exists(newFullPath)) {
                Files.delete(newFullPath);
            }
            Files.createFile(newFullPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileUtils.writeStringToFile(newFile, documentTranslation, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
