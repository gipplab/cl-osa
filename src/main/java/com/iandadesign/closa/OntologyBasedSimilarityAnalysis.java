package com.iandadesign.closa;

import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.Dictionary;
import com.iandadesign.closa.model.WikidataEntity;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import com.iandadesign.closa.util.wikidata.WikidataEntityExtractor;
import com.iandadesign.closa.util.wikidata.WikidataSimilarityUtil;
import com.iandadesign.closa.util.wikidata.WikidataSparqlUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

public class OntologyBasedSimilarityAnalysis {

    private final LanguageDetector languageDetector;
    private final TextClassifier textClassifier;

    private final Logger logger = LoggerFactory.getLogger(OntologyBasedSimilarityAnalysis.class);

    /**
     * Simple constructor,
     */
    public OntologyBasedSimilarityAnalysis() {
        this.languageDetector = new LanguageDetector();
        this.textClassifier = new TextClassifier();
    }

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
     * CL-OSA pre-processing: translation and entity extraction
     *
     * @param documentId       document id
     * @param documentText     document text
     * @param documentLanguage the document's language
     * @return concepts.
     */
    private static List<String> preProcess(String documentId, String documentText, String documentLanguage, Category documentCategory) {
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
     * Whole CL-OSA pipeline.
     *
     * @param suspiciousDocumentPath path to the suspicious document (.txt)
     * @param candidateDocumentPaths paths to the candidate documents (.txt)
     * @return list of candidate paths matching the suspicious
     */
    public Map<String, Double> executeOntologyEnhancedAlgorithmAndComputeScores(String suspiciousDocumentPath, List<String> candidateDocumentPaths) {
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

            return performEnhancedCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap).get(suspiciousDocumentPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    /**
     * CL-OSA pre-processing: translation and entity extraction
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
            String userHome = System.getProperty("user.home");

            documentEntitiesPath = Paths.get(userHome, "preprocessed", documentPath.replace(userHome, ""))
                    .toAbsolutePath().toString();

            List<String> documentEntities;
            Category documentCategory = textClassifier.classifyText(documentText, documentLanguage);

            // document entities
            if (Files.exists(Paths.get(documentEntitiesPath)) && !FileUtils.readFileToString(new File(documentEntitiesPath), StandardCharsets.UTF_8).isEmpty()) {
                // if the file has already been pre-processed
                documentEntities = new ArrayList<>(FileUtils.readLines(new File(documentEntitiesPath), StandardCharsets.UTF_8));
            } else {
                // pre-process the file
                documentEntities = preProcess(documentPath, documentText, documentLanguage, documentCategory);

                if (documentEntities.size() == 0 && !Pattern.compile("(\\s)+").matcher(documentText).find()) {
                    // throw new IllegalStateException("Empty preprocessing.");
                }

                FileUtils.writeLines(new File(documentEntitiesPath), documentEntities);
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
     * @param suspiciousIdTokensMap map: suspicious id to tokens list
     * @param candidateIdTokensMap  map: candidate id to tokens list
     * @return retrieved candidates.
     */
    public Map<String, Map<String, Double>> performCosineSimilarityAnalysis(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap
    ) {
        // create dictionary
        logger.info("Create dictionary");
        Dictionary<String> dictionary = new Dictionary<>(candidateIdTokensMap);

        // perform detailed analysis
        logger.info("Perform detailed analysis");

        // progress bar
        ProgressBar progressBar = new ProgressBar("Perform cosine similarity analysis", suspiciousIdTokensMap.entrySet().size(), ProgressBarStyle.ASCII);
        progressBar.start();

        AtomicInteger progress = new AtomicInteger(0);

        // iterate the suspicious documents
        Map<String, Map<String, Double>> suspiciousIdCandidateScoresMap = suspiciousIdTokensMap.entrySet()
                .stream()
                .collect(Collectors.toMap(entry -> entry.getKey(),
                        entry -> {
                            progressBar.stepTo(progress.incrementAndGet());

                            // look in dictionary
                            return dictionary.query(entry.getValue());
                        }
                ));

        progressBar.stop();

        return suspiciousIdCandidateScoresMap;
    }


    /**
     * Ontology-enhanced cosine similarity analysis.
     *
     * @param suspiciousIdTokensMap map: suspicious id to tokens list
     * @param candidateIdTokensMap  map: candidate id to tokens list
     * @return retrieved candidates.
     */
    public Map<String, Map<String, Double>> performEnhancedCosineSimilarityAnalysis(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap) {
        Map<String, Map<String, Double>> suspiciousIdDetectedCandidateIdsMap = new HashMap<>();

        ProgressBar ontologyProgressBar = new ProgressBar("Enhancing vectors with ontology data",
                suspiciousIdTokensMap.size() + candidateIdTokensMap.size(),
                ProgressBarStyle.ASCII);
        ontologyProgressBar.start();

        Map<String, Map<String, Double>> suspiciousIdTokenCountMap = new HashMap<>();
        Map<String, Map<String, Double>> candidateIdTokenCountMap = new HashMap<>();

        for (Map.Entry<String, List<String>> suspiciousIdTokensMapEntry : suspiciousIdTokensMap.entrySet()) {
            String id = suspiciousIdTokensMapEntry.getKey();
            List<String> tokens = suspiciousIdTokensMapEntry.getValue();
            Map<String, Double> countMap = getHierarchicalCountMap(tokens);
            suspiciousIdTokenCountMap.put(id, countMap);
            ontologyProgressBar.step();
        }

        for (Map.Entry<String, List<String>> candidateIdTokensMapEntry : candidateIdTokensMap.entrySet()) {
            String id = candidateIdTokensMapEntry.getKey();
            List<String> tokens = candidateIdTokensMapEntry.getValue();
            Map<String, Double> countMap = getHierarchicalCountMap(tokens);
            candidateIdTokenCountMap.put(id, countMap);
            ontologyProgressBar.step();
        }

        ontologyProgressBar.stop();


        // perform detailed analysis
        logger.info("Perform detailed analysis");

        // progress bar
        ProgressBar progressBar = new ProgressBar("Perform cosine similarity analysis",
                suspiciousIdTokenCountMap.size() * candidateIdTokenCountMap.size(),
                ProgressBarStyle.ASCII);
        progressBar.start();

        // iterate the suspicious documents
        for (Map.Entry<String, Map<String, Double>> suspiciousEntry : suspiciousIdTokenCountMap.entrySet()) {

            Map<String, Double> candidateSimilarities = new HashMap<>();

            for (Map.Entry<String, Map<String, Double>> candidateEntry : candidateIdTokenCountMap.entrySet()) {

                double similarity = WikidataSimilarityUtil.cosineSimilarity(suspiciousEntry.getValue(), candidateEntry.getValue());

                if (similarity > 0.35) {
                    System.out.println();
                    System.out.println(suspiciousEntry.getKey());
                    System.out.println(candidateEntry.getKey());
                    System.out.println(suspiciousEntry.getValue());
                    System.out.println(candidateEntry.getValue());

                    System.out.println(similarity);
                }

                candidateSimilarities.put(candidateEntry.getKey(), similarity);
                progressBar.step();
            }

            suspiciousIdDetectedCandidateIdsMap.put(suspiciousEntry.getKey(), candidateSimilarities);
        }

        progressBar.stop();

        return suspiciousIdDetectedCandidateIdsMap;
    }


    /**
     * Ontology-enhanced cosine similarity analysis.
     *
     * @param suspiciousIdTokensMap map: suspicious id to tokens list
     * @param candidateIdTokensMap  map: candidate id to tokens list
     * @return retrieved candidates.
     */
    public Map<String, Map<String, Double>> performPropertyCosineSimilarityAnalysis(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap) {
        Map<String, Map<String, Double>> suspiciousIdDetectedCandidateIdsMap = new HashMap<>();

        ProgressBar ontologyProgressBar = new ProgressBar("Enhancing vectors with property data",
                suspiciousIdTokensMap.size() + candidateIdTokensMap.size(),
                ProgressBarStyle.ASCII);
        ontologyProgressBar.start();

        Map<String, Map<String, Double>> suspiciousIdTokenCountMap = new HashMap<>();
        Map<String, Map<String, Double>> candidateIdTokenCountMap = new HashMap<>();

        for (Map.Entry<String, List<String>> suspiciousIdTokensMapEntry : suspiciousIdTokensMap.entrySet()) {
            String id = suspiciousIdTokensMapEntry.getKey();
            List<String> tokens = suspiciousIdTokensMapEntry.getValue();
            Map<String, Double> countMap = getPropertyCountMap(tokens);
            suspiciousIdTokenCountMap.put(id, countMap);
            ontologyProgressBar.step();
        }

        for (Map.Entry<String, List<String>> candidateIdTokensMapEntry : candidateIdTokensMap.entrySet()) {
            String id = candidateIdTokensMapEntry.getKey();
            List<String> tokens = candidateIdTokensMapEntry.getValue();
            Map<String, Double> countMap = getPropertyCountMap(tokens);
            candidateIdTokenCountMap.put(id, countMap);
            ontologyProgressBar.step();
        }

        ontologyProgressBar.stop();


        // perform detailed analysis
        logger.info("Perform detailed analysis");

        // progress bar
        ProgressBar progressBar = new ProgressBar("Perform cosine similarity analysis",
                suspiciousIdTokenCountMap.size() * candidateIdTokenCountMap.size(),
                ProgressBarStyle.ASCII);
        progressBar.start();

        // iterate the suspicious documents
        for (Map.Entry<String, Map<String, Double>> suspiciousEntry : suspiciousIdTokenCountMap.entrySet()) {

            Map<String, Double> candidateSimilarities = new HashMap<>();

            for (Map.Entry<String, Map<String, Double>> candidateEntry : candidateIdTokenCountMap.entrySet()) {
                double similarity = WikidataSimilarityUtil.cosineSimilarity(suspiciousEntry.getValue(), candidateEntry.getValue());
                candidateSimilarities.put(candidateEntry.getKey(), similarity);
                progressBar.step();
            }

            suspiciousIdDetectedCandidateIdsMap.put(suspiciousEntry.getKey(), candidateSimilarities);
        }

        progressBar.stop();

        return suspiciousIdDetectedCandidateIdsMap;
    }


    /**
     * Add two levels of hierarchy, taking their inverse depth as count.
     *
     * @param tokens tokens.
     * @return tokens, with ancestors added.
     */
    private Map<String, Double> getHierarchicalCountMap(List<String> tokens) {
        Map<String, Double> tokenCountMap = new TreeMap<>();

        for (String token : tokens) {

            tokenCountMap.put(token, 1.0);

            WikidataEntity tokenEntity = WikidataDumpUtil.getEntityById(token);

            for (Map.Entry<WikidataEntity, Long> ancestorEntry : WikidataDumpUtil.getAncestorsByMaxDepth(tokenEntity, 2L).entrySet()) {
                String ancestorId = ancestorEntry.getKey().getId();

                tokenCountMap.put(ancestorId, 1.0 / Math.pow(2.0, (ancestorEntry.getValue())));

            }
        }

        return tokenCountMap;
    }

    /**
     * Add two levels of hierarchy, taking their inverse depth as count.
     *
     * @param tokens tokens.
     * @return tokens, with ancestors added.
     */
    private Map<String, Double> getPropertyCountMap(List<String> tokens) {
        Map<String, Double> tokenCountMap = new TreeMap<>();

        for (String token : tokens) {

            tokenCountMap.put(token, 1.0);

            WikidataEntity tokenEntity = WikidataDumpUtil.getEntityById(token);

            for (Map.Entry<String, List<WikidataEntity>> propertyEntry : WikidataDumpUtil.getProperties(tokenEntity).entrySet()) {
                for (WikidataEntity propertyValue : propertyEntry.getValue()) {
                    String ancestorId = propertyValue.getId();
                    tokenCountMap.put(ancestorId, 1.0 / 2.0);
                }

            }
        }

        return tokenCountMap;
    }


}
