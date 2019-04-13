package com.fabianmarquart.closa;

import com.fabianmarquart.closa.classification.Category;
import com.fabianmarquart.closa.classification.TextClassifier;
import com.fabianmarquart.closa.language.LanguageDetector;
import com.fabianmarquart.closa.model.Dictionary;
import com.fabianmarquart.closa.model.WikidataEntity;
import com.fabianmarquart.closa.util.wikidata.WikidataEntityExtractor;
import com.fabianmarquart.closa.util.wikidata.WikidataSimilarityUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class OntologyBasedSimilarityAnalysis {

    private LanguageDetector languageDetector;
    private TextClassifier textClassifier;

    /**
     * Constructor.
     *
     * @param languageDetector language detector.
     * @param textClassifier   text classifer.
     */
    public OntologyBasedSimilarityAnalysis(LanguageDetector languageDetector, TextClassifier textClassifier) {
        this.languageDetector = languageDetector;
        this.textClassifier = textClassifier;
    }

    /**
     * CL-OSA pre-processing: translation -> entity extraction
     *
     * @param documentId       document id
     * @param documentText     document text
     * @param documentLanguage the document's language
     * @return concepts.
     */
    public static List<String> preProcess(String documentId, String documentText, String documentLanguage, Category documentCategory) {
        return WikidataEntityExtractor.extractEntitiesFromText(documentText, documentLanguage, documentCategory)
                .stream()
                .map(WikidataEntity::getId)
                .collect(Collectors.toList());
    }

    /**
     * Whole CL-OSA pipeline.
     *
     * @param suspiciousDocumentPath path to the suspicious document (.txt)
     * @param candidateDocumentPaths paths to the candidate documents (.txt)
     * @return list of candidate paths matching the suspicious
     */
    public Map<String, Double> executeAlgorithmAndComputeScores(String suspiciousDocumentPath, List<String> candidateDocumentPaths) {
        Map<String, List<String>> suspiciousIdTokensMap = new HashMap<>();
        Map<String, List<String>> candidateIdTokensMap = new HashMap<>();

        try {
            suspiciousIdTokensMap.put(suspiciousDocumentPath,
                    preProcess(suspiciousDocumentPath,
                            languageDetector.detectLanguage(FileUtils.readFileToString(new File(suspiciousDocumentPath), StandardCharsets.UTF_8))));

            for (String candidateDocumentPath : candidateDocumentPaths) {
                candidateIdTokensMap.put(candidateDocumentPath,
                        preProcess(candidateDocumentPath,
                                languageDetector.detectLanguage(FileUtils.readFileToString(new File(candidateDocumentPath), StandardCharsets.UTF_8))));
            }

            return performCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap).get(suspiciousDocumentPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    /**
     * CL-OSA pre-processing: translation -> entity extraction
     *
     * @param documentPath     document path
     * @param documentLanguage the document's language
     * @return concepts.
     */
    public List<String> preProcess(String documentPath, String documentLanguage) {
        try {
            // read in the file
            String documentText = FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8);

            String documentEntitiesPath;
            if (documentPath.contains("src/test/resources/org/sciplore/pds/")) {
                documentEntitiesPath = documentPath.replace("pds", String.format("pds/preprocessed/%s", this.getClass().getSimpleName()));
            } else if (documentPath.contains(System.getProperty("user.home"))) {
                documentEntitiesPath = documentPath.replace(System.getProperty("user.home"),
                        String.format("%s/preprocessed/%s/", System.getProperty("user.home"), this.getClass().getName()));
            } else {
                documentEntitiesPath = Paths.get("preprocessed", documentPath).toAbsolutePath().toString();
            }

            List<String> documentEntities;
            Category documentCategory = textClassifier.classifyText(documentText, documentLanguage);

            // document entities
            if (Files.exists(Paths.get(documentEntitiesPath))) {
                // if the file has already been pre-processed
                documentEntities = new ArrayList<>(FileUtils.readLines(new File(documentEntitiesPath), StandardCharsets.UTF_8));
            } else {
                // pre-process the file
                documentEntities = preProcess(documentPath, documentText, documentLanguage, documentCategory);
                FileUtils.writeLines(new File(documentEntitiesPath), documentEntities);
                System.out.println("Written entities to " + documentEntitiesPath);
            }

            return documentEntities;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Cosine similarity analysis.
     *
     * @param suspiciousIdTokensMap map: suspicious id -> tokens list
     * @param candidateIdTokensMap  map: candidate id -> tokens list
     * @return retrieved candidates.
     */
    public Map<String, Map<String, Double>> performCosineSimilarityAnalysis(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap
    ) {
        // create dictionary
        System.out.println("Create dictionary \n");
        Dictionary<String> dictionary = new Dictionary<>(candidateIdTokensMap);

        Map<String, Map<String, Double>> suspiciousIdCandidateScoresMap = new HashMap<>();

        // perform detailed analysis
        System.out.println("Perform detailed analysis \n");

        // progress bar
        ProgressBar progressBar = new ProgressBar("Perform cosine similarity analysis", suspiciousIdTokensMap.entrySet().size(), ProgressBarStyle.ASCII);
        progressBar.start();

        // iterate the suspicious documents
        for (Map.Entry<String, List<String>> entry : suspiciousIdTokensMap.entrySet()) {
            String suspiciousId = entry.getKey();
            List<String> suspiciousConcepts = entry.getValue();

            // look in dictionary
            Map<String, Double> detectedSourceIdScoreMap = dictionary.query(suspiciousConcepts);

            progressBar.step();
            suspiciousIdCandidateScoresMap.put(suspiciousId, detectedSourceIdScoreMap);
        }

        progressBar.stop();

        return suspiciousIdCandidateScoresMap;
    }


    /**
     * Ontology-enhanced cosine similarity analysis.
     *
     * @param suspiciousIdTokensMap map: suspicious id -> tokens list
     * @param candidateIdTokensMap  map: candidate id -> tokens list
     * @return retrieved candidates.
     */
    public Map<String, Map<String, Double>> performEnhancedCosineSimilarityAnalysis(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap) {
        Map<String, Map<String, Double>> suspiciousIdDetectedCandidateIdsMap = new HashMap<>();

        // perform detailed analysis
        System.out.println("Perform detailed analysis \n");

        for (Map.Entry<String, List<String>> suspiciousEntry : suspiciousIdTokensMap.entrySet()) {

            Map<String, Double> candidateScoreMap = candidateIdTokensMap.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> WikidataSimilarityUtil.cosineSimilarity(suspiciousEntry.getValue()
                                            .stream()
                                            .map(WikidataEntity::new)
                                            .collect(Collectors.toList()),
                                    entry.getValue()
                                            .stream()
                                            .map(WikidataEntity::new)
                                            .collect(Collectors.toList()))));

            suspiciousIdDetectedCandidateIdsMap.put(
                    suspiciousEntry.getKey(),
                    candidateScoreMap);
        }

        return suspiciousIdDetectedCandidateIdsMap;
    }


}
