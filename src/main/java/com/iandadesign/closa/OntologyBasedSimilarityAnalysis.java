package com.iandadesign.closa;

import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.*;
import com.iandadesign.closa.model.Dictionary;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import com.iandadesign.closa.util.wikidata.WikidataEntityExtractor;
import com.iandadesign.closa.util.wikidata.WikidataSimilarityUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;


public class OntologyBasedSimilarityAnalysis {

    private final LanguageDetector languageDetector;
    private final TextClassifier textClassifier;

    private final Logger logger = LoggerFactory.getLogger(OntologyBasedSimilarityAnalysis.class);
    private static String preprocessedCachingDirectory;
    private static int lengthSublistTokens;

    static {
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read Config properties.
     *
     * @throws IOException When property file could not be loaded.
     */
    private static void loadConfig() throws IOException {
        InputStream inputStream = null;

        try {
            Properties properties = new Properties();
            String propFileName = "config.properties";
            String propFileLocalName = "config-local.properties";

            // switch to config-local if it exists
            if (WikidataDumpUtil.class.getClassLoader().getResource(propFileLocalName) != null) {
                inputStream = WikidataDumpUtil.class.getClassLoader().getResourceAsStream(propFileLocalName);

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath");
                }
            } else {
                inputStream = WikidataDumpUtil.class.getClassLoader().getResourceAsStream(propFileName);

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath");
                }
            }

            // get the property value and print it out
            preprocessedCachingDirectory = properties.getProperty("preprocessed_caching_directory");
            lengthSublistTokens = Integer.parseInt(properties.getProperty("length_sublist_tokens"));

            if(preprocessedCachingDirectory == null){
                preprocessedCachingDirectory = System.getProperty("user.home");
                System.out.println("Using the home directory for caching preprocessed files "
                        .concat(preprocessedCachingDirectory));
            }else{
                // Checking if directory exists.
                File file = new File(preprocessedCachingDirectory);
                if (!file.isDirectory() && !file.mkdirs()) {
                   throw new Exception("The preprocessed_caching_directory at path: ".concat(preprocessedCachingDirectory)
                           .concat(" cant be created, please update your config or create directory"));
                }
            }

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }


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


    private static List<SavedEntity> preProcessExtendedInfo(String documentId, String documentText, String documentLanguage, Category documentCategory){
        return WikidataEntityExtractor.extractSavedEntitiesFromText(documentText, documentLanguage, documentCategory);
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
            String suspiciousDocumentStr = FileUtils.readFileToString(new File(suspiciousDocumentPath), StandardCharsets.UTF_8);
            String lang = languageDetector.detectLanguage(suspiciousDocumentStr);
            List<String> preprocessed = preProcess(suspiciousDocumentPath,lang);
            suspiciousIdTokensMap.put(suspiciousDocumentPath, preprocessed);

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
     * Creates a string of dashes that is n dashes long.
     *
     * @param n The number of dashes to add to the string.
     */
    public String dashes( int n ) {
        return CharBuffer.allocate( n ).toString().replace( '\0', '-' );
    }

    public Map<String, Double> executeAlgorithmAndComputeScoresExtendedInfo(String suspiciousDocumentPath,
                                                                            List<String> candidateDocumentPaths,
                                                                            String tag) {
        final int NUM_SENTENCES_IN_SLIDING_WINDOW = 1;
        final int NUM_SENTENCE_INCREMENT_SLIDINGW = 1;

        // Maps used for detailed comparison
        Map<String, List<SavedEntity>> suspiciousIdTokensMapExt = new HashMap<>();
        Map<String, List<SavedEntity>> candidateIdTokensMapExt = new HashMap<>();
        // Maps used for candidate retrieval
        Map<String, List<String>> suspiciousIdTokensMap = new HashMap<>();
        Map<String, List<String>> candidateIdTokensMap = new HashMap<>();
        try {
            String suspiciousDocumentStr = FileUtils.readFileToString(new File(suspiciousDocumentPath), StandardCharsets.UTF_8);
            String lang = languageDetector.detectLanguage(suspiciousDocumentStr);
            List<SavedEntity> preprocessedExt = preProcessExtendedInfo(suspiciousDocumentPath, lang);
            List<String> preprocessed = preprocessedExt.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());

            suspiciousIdTokensMapExt.put(suspiciousDocumentPath, preprocessedExt);
            suspiciousIdTokensMap.put(suspiciousDocumentPath, preprocessed);

            for (String candidateDocumentPath : candidateDocumentPaths) {
                List<SavedEntity> preprocessedCandExt = preProcessExtendedInfo(candidateDocumentPath,
                        languageDetector.detectLanguage(FileUtils.readFileToString(new File(candidateDocumentPath), StandardCharsets.UTF_8)));
                List<String> preprocessedCand = preprocessedCandExt.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());

                candidateIdTokensMap.put(candidateDocumentPath, preprocessedCand);
                candidateIdTokensMapExt.put(candidateDocumentPath, preprocessedCandExt);
            }

            // Creating a results file
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            LocalDateTime now = LocalDateTime.now();
            String dateString = dtf.format((now));
            String documentResultsPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended",
                    "results_preprocessing", tag.concat("_").concat(dateString).concat(".txt"))
                    .toAbsolutePath().toString();
            String format = "%-40s%s%n";        // Format for key value outputs
            File resultsDocument = new File(documentResultsPath);
            boolean dirsCreated = resultsDocument.getParentFile().mkdirs();
            PrintStream resultsStream = new PrintStream(resultsDocument);
            resultsStream.println("Extended Algorithm results"+dashes(74));
            resultsStream.printf(format, "TAG:", tag);
            resultsStream.printf(format, "Time:", dateString);
            resultsStream.printf(format, "Sliding Window Length:", NUM_SENTENCES_IN_SLIDING_WINDOW);
            resultsStream.printf(format, "Sliding Window Increment:", NUM_SENTENCE_INCREMENT_SLIDINGW);
            resultsStream.printf(format, "Sublist Token Length:", lengthSublistTokens);
            resultsStream.println(dashes(100));

            // Perform similarity analysis for candidate retrieval.
            Map<String, Double> candidateScoresMap = performCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap).get(suspiciousDocumentPath);
            System.out.println("Scores for candidate retrieval:");
            System.out.println(candidateScoresMap);
            // Select most similar candidates for detailed analysis.
            System.out.println("Continue with detailed analysis:");
            final int NUM_CANDIDATES_SELECTED = 1;
            Map<String, Double> candidatesForDetailedComparison = candidateScoresMap
                    .entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(NUM_CANDIDATES_SELECTED)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // By having the most similar candidates a detailed analysis is performed.
            Set<String> selectedCandidateKeys = candidatesForDetailedComparison.keySet();
            // Create a copy of the original candidates map and reduce it to selected candidates.
            Map<String, List<SavedEntity>> selectedCandidateIdTokensMapExt = new HashMap<> (candidateIdTokensMapExt);
            selectedCandidateIdTokensMapExt.keySet().retainAll(selectedCandidateKeys);

            // Window Comparsison Tresholds
            Double ADJACENT_TRESH = 0.1;
            Double SINGLE_TRESH = 0.4;
            // Storage for combined window entities.
            ScoringChunksCombined scoringChunksCombined = new ScoringChunksCombined(ADJACENT_TRESH, SINGLE_TRESH, NUM_SENTENCES_IN_SLIDING_WINDOW,NUM_SENTENCE_INCREMENT_SLIDINGW);
            long fragmentIndex=0; // Just a running index for adressing fragments.
            for (Map.Entry<String, List<SavedEntity>> suspiciousIdTokenExt : suspiciousIdTokensMapExt.entrySet()) {
                Integer numSentencesSusp = getMaxSentenceNumber(suspiciousIdTokenExt)+1;
                System.out.printf(format, "Detailed Analysis selected Suspicious File:", suspiciousIdTokenExt.getKey());
                System.out.printf(format, "Suspicious file sentences:", numSentencesSusp);

                for (Map.Entry<String, List<SavedEntity>> candidateIdTokenExt : selectedCandidateIdTokensMapExt.entrySet()) {

                    Integer numSentencesCand = getMaxSentenceNumber(candidateIdTokenExt)+1; //TODO fix redundant Operation
                    System.out.printf(format, "Detailed Analysis selected Candidate File:", candidateIdTokenExt.getKey());
                    System.out.printf(format, "Candidate file sentences:", numSentencesCand);
                    resultsStream.println("Comparing Files");
                    resultsStream.printf(format, "Suspicious File:", suspiciousIdTokenExt.getKey());
                    resultsStream.printf(format, "Candidate File:", candidateIdTokenExt.getKey());
                    scoringChunksCombined.setCurrentDocuments(suspiciousIdTokenExt.getKey(), candidateIdTokenExt.getKey());
                    // Documents have been specified here->start to slide the window.
                    for(int currentSuspWindowStartSentence=0;currentSuspWindowStartSentence<numSentencesSusp;currentSuspWindowStartSentence+=NUM_SENTENCE_INCREMENT_SLIDINGW){
                        SlidingWindowInfo swiSuspicious= getWikiEntityStringsForSlidingWindow(
                                suspiciousIdTokenExt,
                                currentSuspWindowStartSentence,
                                NUM_SENTENCES_IN_SLIDING_WINDOW,
                                suspiciousIdTokenExt.getKey());
                        Map<String, List<String>> currentSuspiciousIdTokensMap = swiSuspicious.getFilenameToEntities();

                        for(int currentCandWindowStartSentence=0;currentCandWindowStartSentence<numSentencesCand;currentCandWindowStartSentence+=NUM_SENTENCE_INCREMENT_SLIDINGW){
                             SlidingWindowInfo swiCandidate = getWikiEntityStringsForSlidingWindow(
                                    candidateIdTokenExt,
                                    currentCandWindowStartSentence,
                                    NUM_SENTENCES_IN_SLIDING_WINDOW,
                                    candidateIdTokenExt.getKey());
                            Map<String, List<String>> currentCandidateIdTokensMap = swiCandidate.getFilenameToEntities();

                            Map<String, Double> fragmentScoresMap = performCosineSimilarityAnalysis(currentSuspiciousIdTokensMap,
                                    currentCandidateIdTokensMap).get(suspiciousIdTokenExt.getKey());

                            Double fragmentScore = fragmentScoresMap.get(candidateIdTokenExt.getKey());
                            // TODO if using a window-bordersize buffering remove this later
                            if(fragmentScore==null || fragmentScore<=0.0) {
                                fragmentIndex++; // Just increase the fragment index for absolute indexing.
                                continue;
                            }
                            ScoringChunk currentScoringChunk = new ScoringChunk(swiSuspicious,
                                                                                swiCandidate,
                                                                                fragmentScore,
                                                                                fragmentIndex);

                            resultsStream.println(dashes(50));
                            resultsStream.printf(format, "Fragment Number:", fragmentIndex);
                            resultsStream.printf(format, "Suspicious Start Sentence:", currentSuspWindowStartSentence);
                            resultsStream.printf(format, "Candidate Start Sentence:", currentCandWindowStartSentence);
                            resultsStream.printf(format, "Suspicious Start Character Index:", swiSuspicious.getCharacterStartIndex());
                            resultsStream.printf(format, "Candidate Start Character Index:", swiCandidate.getCharacterStartIndex());
                            resultsStream.printf(format, "Suspicious End Character Index:", swiSuspicious.getCharacterEndIndex());
                            resultsStream.printf(format, "Candidate End Character Index:", swiCandidate.getCharacterEndIndex());
                            resultsStream.printf(format, "Suspicious Tokens:", currentSuspiciousIdTokensMap.get(suspiciousIdTokenExt.getKey()));
                            resultsStream.printf(format, "Candidate Tokens:", currentCandidateIdTokensMap.get(candidateIdTokenExt.getKey()));
                            resultsStream.printf(format, "Fragment Score:", fragmentScore);
                            System.out.println(dashes(50));
                            currentScoringChunk.printMe(
                                    currentSuspiciousIdTokensMap.get(suspiciousIdTokenExt.getKey()),
                                    currentCandidateIdTokensMap.get(candidateIdTokenExt.getKey())
                            );

                            // Do combination with previously stored windows.
                            boolean scoringAdded = false;
                            // Checking if an the window can be added in combination with adjacent previous window.
                            if(currentScoringChunk.getComputedCosineSimilarity() >= ADJACENT_TRESH) {
                                scoringAdded = scoringChunksCombined.storeAndAddToPreviousChunk(currentScoringChunk);
                            }
                            // Checking if a new storage can be done if there was no adjacent window.
                            if(!scoringAdded && currentScoringChunk.getComputedCosineSimilarity() >= SINGLE_TRESH){
                                scoringChunksCombined.storeScoringChunk(currentScoringChunk);
                            }
                        }
                    }
                    // After each candidate and suspicious file combination
                    // ... write down results
                    writeDownXMLResults(tag, dateString, scoringChunksCombined);
                    // ... free memory
                    scoringChunksCombined.flushInternalCombinedChunks();
                }
            }
            resultsStream.close();
            System.out.println(candidatesForDetailedComparison);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return new HashMap<>();
    }

    void writeDownXMLResults(String tag, String dateString, ScoringChunksCombined scoringChunksCombined){
        String cosineResultsPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended",
                "results_comparison", tag.concat("_").concat(dateString),
                scoringChunksCombined.getSuspiciousDocumentName().concat(".xml"))
                .toAbsolutePath().toString();
        // Writing the results to xml file
        try {
            scoringChunksCombined.writeResultAsXML(cosineResultsPath);
            scoringChunksCombined.prettifyXML(cosineResultsPath);
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }
    SlidingWindowInfo getWikiEntityStringsForSlidingWindow(
            Map.Entry<String, List<SavedEntity>> idTokenExt, int startSentenceIndex, int windowSize, String filename){
        // Obtaining the entities which are within the window
        int endSentenceIndex = startSentenceIndex + windowSize;
        List<SavedEntity> windowEntitysSusp = idTokenExt
                .getValue().stream()
                .filter(currentEntity ->
                        currentEntity.getToken().getSentenceNumber() >= startSentenceIndex
                                && currentEntity.getToken().getSentenceNumber() < endSentenceIndex)
                .collect(Collectors.toList());

        // Getting relative start index as the character start coordinate of the first entity
        List<Integer> startChars = windowEntitysSusp.stream()
                .map(SavedEntity::getToken)
                .map(Token::getStartCharacter)
                .collect(Collectors.toList());
        int firstStartChar = 0;
        if(startChars.size()>=1) {
            firstStartChar = Collections.min(startChars);
        }
        // Getting relative stop index as the character end coordinate of the last entity
        List<Integer> endChars = windowEntitysSusp.stream()
                .map(SavedEntity::getToken)
                .map(Token::getEndCharacter)
                .collect(Collectors.toList());
        int lastEndChar = 0;
        if(endChars.size()>=1){
            lastEndChar = Collections.max(endChars);
        }

        // Casting the entities for performing the cosine analysis
        List<String> entityIdsForWindow = windowEntitysSusp
                .stream()
                .map(SavedEntity::getWikidataEntityId)
                .collect(Collectors.toList());
        Map <String, List<String>> filenameToEntities = new HashMap<>();
        filenameToEntities.put(filename, entityIdsForWindow);

        // Return everything in a compound object
        return new SlidingWindowInfo(filename, filenameToEntities, firstStartChar, lastEndChar,
                startSentenceIndex, endSentenceIndex);
    }

    Integer getMaxSentenceNumber(Map.Entry<String, List<SavedEntity>> inputMap){
        return Collections.max(inputMap
                .getValue().stream()
                .map(SavedEntity::getToken)
                .map(Token::getSentenceNumber)
                .collect(Collectors.toList()));
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


            documentEntitiesPath = Paths.get(preprocessedCachingDirectory, "preprocessed",
                    documentPath.replace(preprocessedCachingDirectory, ""))
                    .toAbsolutePath().toString();

            List<String> documentEntities;

            // document entities
            if (Files.exists(Paths.get(documentEntitiesPath)) && !FileUtils.readFileToString(new File(documentEntitiesPath), StandardCharsets.UTF_8).isEmpty()) {
                // if the file has already been pre-processed
                documentEntities = new ArrayList<>(FileUtils.readLines(new File(documentEntitiesPath), StandardCharsets.UTF_8));
            } else {
                Category documentCategory = textClassifier.classifyText(documentText, documentLanguage);
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
     * Preprocess for returning serializable objects instead of lists of string.
     * @param documentPath     document path
     * @param documentLanguage the document's language

     * @return concepts with additional information.
     */
    public List<SavedEntity> preProcessExtendedInfo(String documentPath, String documentLanguage) {
        try {
            // read in the file
            String documentText = FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8);

            String documentEntitiesPath;


            documentEntitiesPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended",
                    documentPath.replace(preprocessedCachingDirectory, "").concat(".ser"))
                    .toAbsolutePath().toString();

            List<SavedEntity> documentEntities = new ArrayList<>();

            // document entities
            if (Files.exists(Paths.get(documentEntitiesPath)) && !FileUtils.readFileToString(new File(documentEntitiesPath), StandardCharsets.UTF_8).isEmpty()) {
                // if the file has already been pre-processed deserialize corresponding info
                FileInputStream fileIn = new FileInputStream(documentEntitiesPath);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                List<?> myIncomingObjects = (List<?>)in.readObject();
                for(Object desObject:myIncomingObjects){
                    documentEntities.add((SavedEntity)desObject);
                }
                in.close();
                fileIn.close();
            } else {
                Category documentCategory = textClassifier.classifyText(documentText, documentLanguage);
                // pre-process the file
                documentEntities = preProcessExtendedInfo(documentPath, documentText, documentLanguage, documentCategory);

                if (documentEntities.size() == 0 && !Pattern.compile("(\\s)+").matcher(documentText).find()) {
                    // throw new IllegalStateException("Empty preprocessing.");
                }
                // Serialize to the list of SavedEntites to defined path.
                boolean dirsCreated = new File(documentEntitiesPath).getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(documentEntitiesPath);
                SavedEntitiesObjectOutputStream oos = new SavedEntitiesObjectOutputStream(fos);
                oos.writeObject(documentEntities);
                oos.close();
                fos.close();
            }

            return documentEntities;
        } catch (Exception e) {
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
     * Cosine similarity analysis.
     *
     * @param suspiciousIdTokensMap map: suspicious id to tokens list
     * @param candidateIdTokensMap  map: candidate id to tokens list
     * @return retrieved candidates.
     */
    public Map<String, Map<String, Double>> performCosineSimilarityAnalysisExtendedInfo(
            Map<String, List<SavedEntity>> suspiciousIdTokensMap,
            Map<String, List<SavedEntity>> candidateIdTokensMap
    ) {
        // create dictionary
        logger.info("Create dictionary");
        Dictionary<SavedEntity> dictionary = new Dictionary<SavedEntity>(candidateIdTokensMap);

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
                            List<SavedEntity> linkedEntities = entry.getValue();
                            Map<String, Double> queryResult =  dictionary.query(entry.getValue());
                            // look in dictionary
                            return queryResult;
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

                candidateSimilarities.put(candidateEntry.getKey(), similarity);
                progressBar.step();
            }

            suspiciousIdDetectedCandidateIdsMap.put(suspiciousEntry.getKey(), candidateSimilarities);
            /* System.out.println("suspiciousIdDetectedCandidateIdsMap.put");
            System.out.println(suspiciousEntry.getKey() + "=" + candidateSimilarities
                    .entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(1)
                    .collect(Collectors.toList())
                    .get(0));
             */
        }

        progressBar.stop();

        return suspiciousIdDetectedCandidateIdsMap;
    }


    /**
     * Property-enhanced cosine similarity analysis.
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
