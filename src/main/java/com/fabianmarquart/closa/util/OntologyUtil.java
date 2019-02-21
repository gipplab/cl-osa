package com.fabianmarquart.closa.util;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import com.fabianmarquart.closa.model.Dictionary;
import com.fabianmarquart.closa.model.WikidataEntity;
import com.fabianmarquart.closa.util.wikidata.WikidataDumpUtil;
import com.fabianmarquart.closa.util.wikidata.WikidataEntityExtractor;
import com.fabianmarquart.closa.util.wikidata.WikidataSimilarityUtil;

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

import static com.fabianmarquart.closa.util.wikidata.WikidataSimilarityUtil.retrieveCandidateByDocumentSimilarity;

public class OntologyUtil {


    /**
     * Whole CL-OSA pipeline.
     *
     * @param suspiciousDocumentPath path to the suspicious document (.txt)
     * @param candidateDocumentPaths paths to the candidate documents (.txt)
     * @return list of candidate paths matching the suspicious
     */
    public static List<String> executeAlgorithmAndGetCandidates(String suspiciousDocumentPath, List<String> candidateDocumentPaths) {
        Map<String, List<String>> suspiciousIdTokensMap = new HashMap<>();
        Map<String, List<String>> candidateIdTokensMap = new HashMap<>();

        try {
            suspiciousIdTokensMap.put(suspiciousDocumentPath,
                    preProcess(suspiciousDocumentPath,
                            TokenUtil.detectLanguage(FileUtils.readFileToString(new File(suspiciousDocumentPath), StandardCharsets.UTF_8))));

            for (String candidateDocumentPath : candidateDocumentPaths) {
                candidateIdTokensMap.put(candidateDocumentPath,
                        preProcess(candidateDocumentPath,
                                TokenUtil.detectLanguage(FileUtils.readFileToString(new File(candidateDocumentPath), StandardCharsets.UTF_8))));
            }

            return performCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap).get(suspiciousDocumentPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }

    /**
     * CL-OSA pre-processing: translation -> entity extraction
     *
     * @param documentPath     document path
     * @param documentLanguage the document's language
     * @return concepts.
     */
    public static List<String> preProcess(String documentPath, String documentLanguage) {
        try {
            // read in the file
            String documentText = FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8);

            String documentEntitiesPath = documentPath.contains(System.getProperty("user.home"))
                    ? documentPath.replace(System.getProperty("user.home"),
                    System.getProperty("user.home") + "/preprocessed/" + OntologyUtil.class.getSimpleName() + "/")
                    : Paths.get("preprocessed", documentPath).toAbsolutePath().toString();

            // if the file has already been pre-processed
            if (Files.exists(Paths.get(documentEntitiesPath)) // file exists and is not empty
                    && !FileUtils.readLines(new File(documentEntitiesPath), StandardCharsets.UTF_8).isEmpty()) {
                return new ArrayList<>(FileUtils.readLines(new File(documentEntitiesPath), StandardCharsets.UTF_8));
            }

            // pre-process the file
            List<String> documentEntities = preProcess(documentPath, documentText, documentLanguage);
            FileUtils.writeLines(new File(documentEntitiesPath), documentEntities);

            return documentEntities;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * CL-OSA pre-processing: translation -> entity extraction
     *
     * @param documentId       document id
     * @param documentText     document text
     * @param documentLanguage the document's language
     * @return concepts.
     */
    public static List<String> preProcess(String documentId, String documentText, String documentLanguage) {
        return WikidataEntityExtractor.extractEntitiesFromText(documentText, documentLanguage)
                .stream()
                .map(WikidataEntity::getId)
                .collect(Collectors.toList());
    }

    /**
     * Cosine similarity analysis.
     *
     * @param suspiciousIdTokensMap map: suspicious id -> tokens list
     * @param candidateIdTokensMap  map: candidate id -> tokens list
     * @return retrieved candidates.
     */
    public static Map<String, List<String>> performCosineSimilarityAnalysis(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap
    ) {
        // create dictionary
        System.out.println("Create dictionary \n");
        Dictionary<String> dictionary = new Dictionary<>(candidateIdTokensMap);

        Map<String, List<String>> suspiciousIdDetectedCandidateIdsMap = new HashMap<>();

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
            List<String> detectedSourceIds = dictionary.query(suspiciousConcepts);

            progressBar.step();
            suspiciousIdDetectedCandidateIdsMap.put(suspiciousId, detectedSourceIds);
        }
        progressBar.stop();

        return suspiciousIdDetectedCandidateIdsMap;
    }


    /**
     * Ontology-enhanced cosine similarity analysis.
     *
     * @param suspiciousIdTokensMap map: suspicious id -> tokens list
     * @param candidateIdTokensMap  map: candidate id -> tokens list
     * @return retrieved candidates.
     */
    public static Map<String, List<String>> performEnhancedCosineSimilarityAnalysis(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap
    ) {
        Map<String, List<String>> suspiciousIdDetectedCandidateIdsMap = new HashMap<>();

        // perform detailed analysis
        System.out.println("Perform detailed analysis \n");

        for (Map.Entry<String, List<String>> suspiciousEntry : suspiciousIdTokensMap.entrySet()) {
            suspiciousIdDetectedCandidateIdsMap.put(
                    suspiciousEntry.getKey(),
                    retrieveCandidateByDocumentSimilarity(
                            suspiciousEntry.getValue().stream().map(WikidataDumpUtil::getEntityById).collect(Collectors.toList()),
                            candidateIdTokensMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                                    entry -> entry.getValue().stream().map(WikidataDumpUtil::getEntityById).collect(Collectors.toList()))),
                            WikidataSimilarityUtil.SimilarityFunction.ENHANCED_COSINE));
        }

        return suspiciousIdDetectedCandidateIdsMap;
    }


}
