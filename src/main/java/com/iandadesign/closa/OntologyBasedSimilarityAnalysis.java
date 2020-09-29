package com.iandadesign.closa;

import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.*;
import com.iandadesign.closa.model.Dictionary;
import com.iandadesign.closa.util.ExtendedAnalytics;
import com.iandadesign.closa.util.ExtendedLogUtil;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import com.iandadesign.closa.util.wikidata.WikidataEntityExtractor;
import com.iandadesign.closa.util.wikidata.WikidataSimilarityUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private  String tag;
    private ExtendedLogUtil logUtil;
    private String extendedXmlResultsPath;

    private final Logger logger = LoggerFactory.getLogger(OntologyBasedSimilarityAnalysis.class);
    private static String preprocessedCachingDirectory;
    private static String errorlogPath;
    private static String standardlogPath;
    private static int lenSublistTokens;
    private static boolean doParallelRequests;

    static {
        try {
            loadConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public LanguageDetector getLanguageDetector() {
        return languageDetector;
    }

    public ExtendedLogUtil getExtendedLogUtil() {
        return logUtil;
    }

    public int getLenSublistTokens(){ return lenSublistTokens;}
    public String getErrorlogPath(){ return errorlogPath;}
    public String getStandardlogPath(){ return standardlogPath;}
    public String getPreprocessedCachingDirectory(){return preprocessedCachingDirectory;}
    public Boolean getDoParallelRequests(){return doParallelRequests;}
    public String getExtendedXmlResultsPath(){return extendedXmlResultsPath;}

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
            errorlogPath = properties.getProperty("errorlog_path");
            standardlogPath = properties.getProperty("standardlog_path");
            lenSublistTokens = Integer.parseInt(properties.getProperty("length_sublist_tokens"));
            doParallelRequests = Boolean.parseBoolean(properties.getProperty("do_parallel_requests"));

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
    public void initializeLogger(String tag, ExtendedAnalysisParameters params){
        this.tag = tag;
        this.logUtil = new ExtendedLogUtil(errorlogPath, standardlogPath, tag,
                       params.LOG_ERROR_TO_FILE, params.LOG_STANDARD_TO_FILE);
    }

    public void setLogger(ExtendedLogUtil extendedLogUtil, String tag){
        this.logUtil = extendedLogUtil;
        this.tag = tag;
    }

    public String getTag(){
        return this.tag;
    }

    public TextClassifier getTextClassifier() {
        return textClassifier;
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
        WeakHashMap<String, List<String>> suspiciousIdTokensMap = new WeakHashMap<>();
        WeakHashMap<String, List<String>> candidateIdTokensMap = new WeakHashMap<>();

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

    private void printCandidateRetrievalResults(ExtendedLogUtil logUtil, Map<String, Double> candidateScoresMap, ExtendedAnalysisParameters params){
        int counter=0;

        for(String key: candidateScoresMap.keySet()){
            if(counter>=params.CR_PRINT_LIMIT) break;
            Double value = candidateScoresMap.get(key);
            if(value<=0){
                counter++;
                continue;
            }
            logUtil.logAndWriteStandard(true,"CR-Result "+new File(key).getName(),"has score: "+value);
            counter++;
        }
    }

    /**
     *
     * @param suspiciousDocumentPath
     * @param candidateDocumentFiles
     * @param params
     * @param initialDateString
     * @param suspiciousIdTokensMapExt this is filled by reference
     * @throws Exception
     */
    public Map<String, Double> doCandidateRetrievalExtendedInfo(String suspiciousDocumentPath,
                                                 List<File> candidateDocumentFiles,
                                                 ExtendedAnalysisParameters params,
                                                 String initialDateString,
                                                 Map<String, List<SavedEntity>>  suspiciousIdTokensMapExt) throws Exception{
        // Comparing one suspicious document to  a list of candidate documents


        // Maps used for detailed comparison
        WeakHashMap<String, List<SavedEntity>> candidateIdTokensMapExt = new WeakHashMap<>();
        // Maps used for candidate retrieval
        WeakHashMap<String, List<String>> suspiciousIdTokensMap = new WeakHashMap<>();
        WeakHashMap<String, List<String>> candidateIdTokensMap = new WeakHashMap<>();

        String suspiciousDocumentStr = FileUtils.readFileToString(new File(suspiciousDocumentPath), StandardCharsets.UTF_8);
        List<SavedEntity> preprocessedExt = preProcessExtendedInfo(suspiciousDocumentPath, null);
        List<String> preprocessed = preprocessedExt.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());

        suspiciousIdTokensMapExt.put(suspiciousDocumentPath, preprocessedExt);
        suspiciousIdTokensMap.put(suspiciousDocumentPath, preprocessed);

        for (File candidateDocumentFile : candidateDocumentFiles) {
            List<SavedEntity> preprocessedCandExt = preProcessExtendedInfo(candidateDocumentFile.getPath(),null );
            List<String> preprocessedCand = preprocessedCandExt.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());
            candidateIdTokensMap.put(candidateDocumentFile.getPath(), preprocessedCand);
            candidateIdTokensMapExt.put(candidateDocumentFile.getPath(), preprocessedCandExt);
        }

        // Perform similarity analysis for candidate retrieval.
        Map<String, Double> candidateScoresMap = performCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap).get(suspiciousDocumentPath);
        //Map<String, Double> candidateScoresMap = performEnhancedCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap).get(suspiciousDocumentPath);


        Map<String, Double> candidateScoresMapS = candidateScoresMap.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        logUtil.logAndWriteStandard(false,"Scores for candidate retrieval:");
        // Print a representative selection of the scores
        printCandidateRetrievalResults(logUtil, candidateScoresMapS, params);
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));



        // CANDIDATE SELECTION: Select most similar candidates for detailed analysis.
        logUtil.logAndWriteStandard(false, "Selecting most similar candidates...");
        Map<String, Double> candidatesForDetailedComparison = candidateScoresMap
                .entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .filter(entry-> entry.getValue() >= params.CANDIDATE_SELECTION_TRESH)
                .limit(params.MAX_NUM_CANDIDATES_SELECTED)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        logUtil.logAndWriteStandard(false,"Scores of selected candidates:");

        Map<String, Double> candidatesForDetailedComparisonS = candidatesForDetailedComparison.entrySet().stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // Print a representative selection of the scores
        printCandidateRetrievalResults(logUtil, candidatesForDetailedComparisonS, params);

        //TODO MEMORY MARK 3
        //candidateIdTokensMap.clear();
        //suspiciousIdTokensMap.clear(); // this deletes suspicious id tokens ext
        // preprocessedExt.clear(); // this deletes suspciious id tokens ext saved entities
        //preprocessed.clear();


        // By having the most similar candidates a detailed analysis is performed.
        Set<String> selectedCandidateKeys = candidatesForDetailedComparisonS.keySet();
        // Create a copy of the original candidates map and reduce it to selected candidates.
        WeakHashMap<String, List<SavedEntity>> selectedCandidateIdTokensMapExt = new WeakHashMap<> (candidateIdTokensMapExt);
        selectedCandidateIdTokensMapExt.keySet().retainAll(selectedCandidateKeys);

        logUtil.logAndWriteStandard(false, selectedCandidateKeys.size()+" of "+candidateScoresMap.size() + " candidates have been selected");

        if(selectedCandidateKeys.size()<=0){
            logUtil.logAndWriteStandard(false, "no candidates have been selected, returning");
            return null;
        }
        //selectedCandidateKeys.clear();
        //candidateScoresMapS.clear();
        candidateIdTokensMapExt.clear();
        candidateIdTokensMap.clear();
        candidateScoresMap.clear();
        preprocessed.clear();
        suspiciousIdTokensMap.clear();
        candidatesForDetailedComparison = null;
        selectedCandidateIdTokensMapExt.clear();
        preprocessedExt = new ArrayList<>(); // Just new ref here, because entries still used.
        return candidatesForDetailedComparisonS;

    }


    public Map<String,Map <String, Double>>  doCandidateRetrievalExtendedInfo2(List<File>  suspiciousDocumentFiles,
                                                                List<File> candidateDocumentFiles,
                                                                ExtendedAnalysisParameters params,
                                                                String initialDateString,
                                                                Map<String, List<SavedEntity>>  suspiciousIdTokensMapExt) throws Exception{
        // Comparing one suspicious document to  a list of candidate documents


        // Maps used for detailed comparison
        Map<String, List<SavedEntity>> candidateIdTokensMapExt = new HashMap<>();
        // Maps used for candidate retrieval
        Map<String, List<String>> suspiciousIdTokensMap = new HashMap<>();
        Map<String, List<String>> candidateIdTokensMap = new HashMap<>();


        for (File suspiciousDocumentFile : suspiciousDocumentFiles) {
            List<SavedEntity> preprocessedSuspExt = preProcessExtendedInfo(suspiciousDocumentFile.getPath(),null );
            List<String> preprocessedSusp = preprocessedSuspExt.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());
            suspiciousIdTokensMap.put(suspiciousDocumentFile.getPath(), preprocessedSusp);
            suspiciousIdTokensMapExt.put(suspiciousDocumentFile.getPath(), preprocessedSuspExt);
        }


        for (File candidateDocumentFile : candidateDocumentFiles) {
            List<SavedEntity> preprocessedCandExt = preProcessExtendedInfo(candidateDocumentFile.getPath(),null );
            List<String> preprocessedCand = preprocessedCandExt.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());
            candidateIdTokensMap.put(candidateDocumentFile.getPath(), preprocessedCand);
            candidateIdTokensMapExt.put(candidateDocumentFile.getPath(), preprocessedCandExt);
        }

        // Perform similarity analysis for candidate retrieval.

        Map<String,Map <String, Double>> suspiciousIdCandidateScoresMap = performCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap);
        //Map<String,Map <String, Double>> suspiciousIdCandidateScoresMap = performEnhancedCosineSimilarityAnalysisP(suspiciousIdTokensMap, candidateIdTokensMap);

        //Map<String, Double> candidateScoresMap = performEnhancedCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap).get(suspiciousDocumentPath);





        //todo finish
        return suspiciousIdCandidateScoresMap;

    }

    public void executeAlgorithmForAllfiles(List<File> suspiciousDocumentFiles,
                                                             List<File> candidateDocumentFiles,
                                                             ExtendedAnalysisParameters params,
                                                             String initialDateString)
            throws Exception{

        // This takes around 500 MB
        List<SlidingWindowInfo> slidingWindowInfosSusp = new ArrayList<>();
        List<SlidingWindowInfo> slidingWindowInfosCand = new ArrayList<>();
        Map<String, List<String>> suspiciousEntitiesAll = new HashMap<>();
        Map<String, List<String>> candidateEntitiesAll = new HashMap<>();

        logUtil.writeStandardReport(false, "Getting the entities for the suspicious files");
        // Getting the entities for the sliding window
        for(File fileS:suspiciousDocumentFiles) {
            List<SavedEntity> suspicousEntities = preProcessExtendedInfo(fileS.getPath(), null);
            int numSentencesSusp = getMaxSentenceNumber(suspicousEntities) + 1;

            for (int currentSuspWindowStartSentence = 0; currentSuspWindowStartSentence < numSentencesSusp; currentSuspWindowStartSentence += params.NUM_SENTENCE_INCREMENT_SLIDINGW) {
                SlidingWindowInfo swiSuspicious = getWikiEntityStringsForSlidingWindow(
                        suspicousEntities,
                        currentSuspWindowStartSentence,
                        params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                       fileS.getName());
                slidingWindowInfosSusp.add(swiSuspicious);

                suspiciousEntitiesAll.put(fileS.getName()+currentSuspWindowStartSentence, swiSuspicious.getFilenameToEntities().get(fileS.getName()));
            }
        }
        logUtil.writeStandardReport(false, "Getting the entities for the candidate files");
        for(File fileC:candidateDocumentFiles) {
            List<SavedEntity> candEntities = preProcessExtendedInfo(fileC.getPath(),null);
            int numSentencesCand= getMaxSentenceNumber(candEntities) + 1;

            for (int currentCandWindowStartSentence = 0; currentCandWindowStartSentence < numSentencesCand; currentCandWindowStartSentence += params.NUM_SENTENCE_INCREMENT_SLIDINGW) {
                SlidingWindowInfo swiCand = getWikiEntityStringsForSlidingWindow(
                        candEntities,
                        currentCandWindowStartSentence,
                        params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                        fileC.getName());
                slidingWindowInfosCand.add(swiCand);

                candidateEntitiesAll.put(fileC.getName()+currentCandWindowStartSentence, swiCand.getFilenameToEntities().get(fileC.getName()));
            }

        }

        Map<String, Map<String, Double>> fragmentScoresMapN = performCosineSimilarityAnalysis(suspiciousEntitiesAll,
                candidateEntitiesAll);
        // Perform the analysis

        // Create Scoring Chunk Matrix

        // Calculate Scores
        if(true){
            return;
        }

        String suspiciousDocumentPath="";
        // This hashmap is populated by candidateRetrieval.
        WeakHashMap<String, List<SavedEntity>> suspiciousIdTokensMapExt = new WeakHashMap<>();
        Set<String> selectedCandidateKeys = doCandidateRetrievalExtendedInfo(suspiciousDocumentPath,
                candidateDocumentFiles, params, initialDateString, suspiciousIdTokensMapExt).keySet();
        if(selectedCandidateKeys==null){
            logUtil.writeStandardReport(false, "No candidates selected, continuing");
            return;
        }

        logUtil.logAndWriteStandard(false, logUtil.dashes(100));
        logUtil.logAndWriteStandard(false, "Starting with detailed analysis ...");
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));


        long fragmentIndex=0; // Just a running index for adressing fragments.
        for (Map.Entry<String, List<SavedEntity>> suspiciousIdTokenExt : suspiciousIdTokensMapExt.entrySet()) {
            // MEMORY: This usually is just one suspicious candidate, therefore the complete SavedEntities are imported
            int numSentencesSusp = getMaxSentenceNumber(suspiciousIdTokenExt)+1;
            String suspFilename = new File(suspiciousIdTokenExt.getKey()).getName();
            logUtil.logAndWriteStandard(true, "DA selected Susp-File:",suspFilename);
            logUtil.logAndWriteStandard(true,"Suspicious file sentences:", numSentencesSusp);



            for(String selectedCandidatePath: selectedCandidateKeys) {

                // Storage for combined window entities.
                ScoringChunksCombined scoringChunksCombined = new ScoringChunksCombined(
                        params.ADJACENT_THRESH,
                        params.SINGLE_THRESH,
                        params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                        params.NUM_SENTENCE_INCREMENT_SLIDINGW,
                        params.CLIPPING_MARGING);
                try {

                    // MEMORY: Getting the Saved entities for the current candidate.
                    List<SavedEntity> candidateEntities = preProcessExtendedInfo(selectedCandidatePath,null);

                    int numSentencesCand = getMaxSentenceNumber(candidateEntities) + 1; //TODO fix redundant Operation
                    String candFilename = new File(selectedCandidatePath).getName();
                    logUtil.logAndWriteStandard(true, "DA selected Cand-File:", candFilename);
                    logUtil.logAndWriteStandard(true, "Candidate file sentences:", numSentencesCand);
                    scoringChunksCombined.setCurrentDocuments(suspiciousIdTokenExt.getKey(), selectedCandidatePath);
                    scoringChunksCombined.createScoreMatrix(numSentencesSusp, numSentencesCand);
                    int suspiciousSlidingWindowY = 0; // specific index for 2D Matrix positioning

                    // Documents have been specified here->start to slide the window.
                    for (int currentSuspWindowStartSentence = 0; currentSuspWindowStartSentence < numSentencesSusp; currentSuspWindowStartSentence += params.NUM_SENTENCE_INCREMENT_SLIDINGW) {
                        // Content in this loop is causing the memory problem ....
                        SlidingWindowInfo swiSuspicious = getWikiEntityStringsForSlidingWindow(
                                suspiciousIdTokenExt.getValue(),
                                currentSuspWindowStartSentence,
                                params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                                suspiciousIdTokenExt.getKey());

                        WeakHashMap<String, List<String>> currentSuspiciousIdTokensMap = swiSuspicious.getFilenameToEntities();
                        int candSlidingWindowX = 0; // specific index for 2D Matrix positioning
                        for (int currentCandWindowStartSentence = 0; currentCandWindowStartSentence < numSentencesCand; currentCandWindowStartSentence += params.NUM_SENTENCE_INCREMENT_SLIDINGW) {
                            SlidingWindowInfo swiCandidate = getWikiEntityStringsForSlidingWindow(
                                    candidateEntities,
                                    currentCandWindowStartSentence,
                                    params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                                    selectedCandidatePath);

                            WeakHashMap<String, List<String>> currentCandidateIdTokensMap = swiCandidate.getFilenameToEntities();

                            // logUtil.logAndWriteStandard(false,"Susp Sentence: "+suspiciousIdTokenExt.getKey());
                            // logUtil.logAndWriteStandard(false,"Cand Sentence: "+candidateIdTokenExt.getKey());

                            // Create a specific mock entry if there is an empty row or column item in matrix.
                            if (swiSuspicious.isNoEntitiesInWindow() || swiCandidate.isNoEntitiesInWindow()) {
                                ScoringChunk mockScoringChunk = new ScoringChunk(swiSuspicious,
                                        swiCandidate,
                                        -1, // mock entry value
                                        fragmentIndex);
                                scoringChunksCombined.storeScoringChunkToScoringMatrix(mockScoringChunk,
                                        suspiciousSlidingWindowY,
                                        candSlidingWindowX);
                                fragmentIndex++; // Just increase the fragment index for absolute indexing.
                                candSlidingWindowX++;
                                swiCandidate.deinitialize();
                                continue;  // Skip without increasing 2D indices (all window comparisons would be 0 score)
                            }

                            Map<String, Double> fragmentScoresMap = performCosineSimilarityAnalysis(currentSuspiciousIdTokensMap,
                                    currentCandidateIdTokensMap).get(suspiciousIdTokenExt.getKey());

                            Double fragmentScore = fragmentScoresMap.get(selectedCandidatePath);
                            // TODO if using a window-bordersize buffering remove this later
                            if (fragmentScore == null || fragmentScore <= 0.0) {
                                fragmentIndex++; // Just increase the fragment index for absolute indexing.
                                candSlidingWindowX++;
                                continue;
                            }

                            ScoringChunk currentScoringChunk = new ScoringChunk(swiSuspicious,
                                    swiCandidate,
                                    fragmentScore,
                                    fragmentIndex);
                            swiCandidate.deinitialize();

                            if (params.LOG_VERBOSE) {
                                logUtil.logAndWriteStandard(false, logUtil.dashes(50));
                                logUtil.logAndWriteStandard(true, "Fragment Number:", fragmentIndex);
                                logUtil.logAndWriteStandard(true, "Suspicious Start Sentence:", currentSuspWindowStartSentence);
                                logUtil.logAndWriteStandard(true, "Candidate Start Sentence:", currentCandWindowStartSentence);
                                logUtil.logAndWriteStandard(true, "Suspicious Start Character Index:", swiSuspicious.getCharacterStartIndex());
                                logUtil.logAndWriteStandard(true, "Candidate Start Character Index:", swiCandidate.getCharacterStartIndex());
                                logUtil.logAndWriteStandard(true, "Suspicious End Character Index:", swiSuspicious.getCharacterEndIndex());
                                logUtil.logAndWriteStandard(true, "Candidate End Character Index:", swiCandidate.getCharacterEndIndex());
                                logUtil.logAndWriteStandard(true, "Suspicious Tokens:", currentSuspiciousIdTokensMap.get(suspiciousIdTokenExt.getKey()));
                                logUtil.logAndWriteStandard(true, "Candidate Tokens:", currentCandidateIdTokensMap.get(selectedCandidatePath));
                                logUtil.logAndWriteStandard(true, "Fragment Score:", fragmentScore);
                                logUtil.logAndWriteStandard(false, logUtil.dashes(50));
                                currentScoringChunk.printMe(
                                        "%-40s%s%n",
                                        currentSuspiciousIdTokensMap.get(suspiciousIdTokenExt.getKey()),
                                        currentCandidateIdTokensMap.get(selectedCandidatePath)
                                );

                            }
                            //TODO performance mark fileprints as optional
                            // Adding scoring chunk with coordinates to matrix.
                            scoringChunksCombined.storeScoringChunkToScoringMatrix(currentScoringChunk, suspiciousSlidingWindowY, candSlidingWindowX);

                            candSlidingWindowX++;

                            // Clear memory (ok?)
                            currentCandidateIdTokensMap.clear();
                            fragmentScoresMap.clear();
                        }
                        swiSuspicious.deinitialize();
                        suspiciousSlidingWindowY++;
                        currentSuspiciousIdTokensMap.clear();

                    }
                    // After each candidate and suspicious file combination
                    // ... calculate the plagiarism sections from windows
                    scoringChunksCombined.calculateMatrixClusters(params.USE_ADAPTIVE_CLUSTERING_TRESH, params.ADAPTIVE_FORM_FACTOR);
                    // ... write down results
                    // TODO solve this in multithreading context
                    this.extendedXmlResultsPath = scoringChunksCombined.writeDownXMLResults(tag, initialDateString, preprocessedCachingDirectory);
                    if (params.LOG_TO_CSV) {
                        scoringChunksCombined.writeScoresMapAsCSV(tag, initialDateString, preprocessedCachingDirectory);
                    }
                    logUtil.logAndWriteStandard(true, "done processing file combination", suspFilename, "with", candFilename);
                    logUtil.logAndWriteStandard(false, logUtil.dashes(100));

                    // MEMORY: Clear candidate entities from memory.
                    candidateEntities.clear();
                } catch (Exception ex) {
                    logUtil.logAndWriteError(true, "Exception processing file combination",
                            suspiciousIdTokenExt.getKey(), selectedCandidatePath);
                    ex.printStackTrace();
                } finally {
                    // ... free memory
                    //TODO perfomance: Check if memory is released here properly
                    scoringChunksCombined.flushInternalCombinedChunks();
                    System.gc();
                }
            }

        }
        selectedCandidateKeys.clear();
        suspiciousIdTokensMapExt.clear();
        logUtil.writeStandardReport(false, "Whats going on here?");
    }



    public void executeAlgorithmAndComputeScoresExtendedInfo(String suspiciousDocumentPath,
                                                                            List<File> candidateDocumentFiles,
                                                                            ExtendedAnalysisParameters params,
                                                                            String initialDateString)
                                                                            throws Exception{

        // This hashmap is populated by candidateRetrieval.
        WeakHashMap<String, List<SavedEntity>> suspiciousIdTokensMapExt = new WeakHashMap<>();
        Set<String> selectedCandidateKeys = doCandidateRetrievalExtendedInfo(suspiciousDocumentPath,
                candidateDocumentFiles, params, initialDateString, suspiciousIdTokensMapExt).keySet();
        if(selectedCandidateKeys==null){
            logUtil.writeStandardReport(false, "No candidates selected, continuing");
            return;
        }

        logUtil.logAndWriteStandard(false, logUtil.dashes(100));
        logUtil.logAndWriteStandard(false, "Starting with detailed analysis ...");
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));


        long fragmentIndex=0; // Just a running index for adressing fragments.
        for (Map.Entry<String, List<SavedEntity>> suspiciousIdTokenExt : suspiciousIdTokensMapExt.entrySet()) {
            // MEMORY: This usually is just one suspicious candidate, therefore the complete SavedEntities are imported
            int numSentencesSusp = getMaxSentenceNumber(suspiciousIdTokenExt)+1;
            String suspFilename = new File(suspiciousIdTokenExt.getKey()).getName();
            logUtil.logAndWriteStandard(true, "DA selected Susp-File:",suspFilename);
            logUtil.logAndWriteStandard(true,"Suspicious file sentences:", numSentencesSusp);



            for(String selectedCandidatePath: selectedCandidateKeys) {

                // Storage for combined window entities.
                ScoringChunksCombined scoringChunksCombined = new ScoringChunksCombined(
                        params.ADJACENT_THRESH,
                        params.SINGLE_THRESH,
                        params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                        params.NUM_SENTENCE_INCREMENT_SLIDINGW,
                        params.CLIPPING_MARGING);
                try {

                    // MEMORY: Getting the Saved entities for the current candidate.
                    List<SavedEntity> candidateEntities = preProcessExtendedInfo(selectedCandidatePath,null);

                    int numSentencesCand = getMaxSentenceNumber(candidateEntities) + 1; //TODO fix redundant Operation
                    String candFilename = new File(selectedCandidatePath).getName();
                    logUtil.logAndWriteStandard(true, "DA selected Cand-File:", candFilename);
                    logUtil.logAndWriteStandard(true, "Candidate file sentences:", numSentencesCand);
                    scoringChunksCombined.setCurrentDocuments(suspiciousIdTokenExt.getKey(), selectedCandidatePath);
                    scoringChunksCombined.createScoreMatrix(numSentencesSusp, numSentencesCand);
                    int suspiciousSlidingWindowY = 0; // specific index for 2D Matrix positioning

                    // Documents have been specified here->start to slide the window.
                    for (int currentSuspWindowStartSentence = 0; currentSuspWindowStartSentence < numSentencesSusp; currentSuspWindowStartSentence += params.NUM_SENTENCE_INCREMENT_SLIDINGW) {
                        // Content in this loop is causing the memory problem ....
                        SlidingWindowInfo swiSuspicious = getWikiEntityStringsForSlidingWindow(
                                suspiciousIdTokenExt.getValue(),
                                currentSuspWindowStartSentence,
                                params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                                suspiciousIdTokenExt.getKey());

                        WeakHashMap<String, List<String>> currentSuspiciousIdTokensMap = swiSuspicious.getFilenameToEntities();
                        int candSlidingWindowX = 0; // specific index for 2D Matrix positioning
                        for (int currentCandWindowStartSentence = 0; currentCandWindowStartSentence < numSentencesCand; currentCandWindowStartSentence += params.NUM_SENTENCE_INCREMENT_SLIDINGW) {
                            SlidingWindowInfo swiCandidate = getWikiEntityStringsForSlidingWindow(
                                    candidateEntities,
                                    currentCandWindowStartSentence,
                                    params.NUM_SENTENCES_IN_SLIDING_WINDOW,
                                    selectedCandidatePath);

                            WeakHashMap<String, List<String>> currentCandidateIdTokensMap = swiCandidate.getFilenameToEntities();

                            // logUtil.logAndWriteStandard(false,"Susp Sentence: "+suspiciousIdTokenExt.getKey());
                            // logUtil.logAndWriteStandard(false,"Cand Sentence: "+candidateIdTokenExt.getKey());

                            // Create a specific mock entry if there is an empty row or column item in matrix.
                            if (swiSuspicious.isNoEntitiesInWindow() || swiCandidate.isNoEntitiesInWindow()) {
                                ScoringChunk mockScoringChunk = new ScoringChunk(swiSuspicious,
                                        swiCandidate,
                                        -1, // mock entry value
                                        fragmentIndex);
                                scoringChunksCombined.storeScoringChunkToScoringMatrix(mockScoringChunk,
                                        suspiciousSlidingWindowY,
                                        candSlidingWindowX);
                                fragmentIndex++; // Just increase the fragment index for absolute indexing.
                                candSlidingWindowX++;
                                swiCandidate.deinitialize();
                                continue;  // Skip without increasing 2D indices (all window comparisons would be 0 score)
                            }

                            Map<String, Double> fragmentScoresMap = performCosineSimilarityAnalysis(currentSuspiciousIdTokensMap,
                                    currentCandidateIdTokensMap).get(suspiciousIdTokenExt.getKey());

                            Double fragmentScore = fragmentScoresMap.get(selectedCandidatePath);
                            // TODO if using a window-bordersize buffering remove this later
                            if (fragmentScore == null || fragmentScore <= 0.0) {
                                fragmentIndex++; // Just increase the fragment index for absolute indexing.
                                candSlidingWindowX++;
                                continue;
                            }

                            ScoringChunk currentScoringChunk = new ScoringChunk(swiSuspicious,
                                    swiCandidate,
                                    fragmentScore,
                                    fragmentIndex);
                            swiCandidate.deinitialize();

                            if (params.LOG_VERBOSE) {
                                logUtil.logAndWriteStandard(false, logUtil.dashes(50));
                                logUtil.logAndWriteStandard(true, "Fragment Number:", fragmentIndex);
                                logUtil.logAndWriteStandard(true, "Suspicious Start Sentence:", currentSuspWindowStartSentence);
                                logUtil.logAndWriteStandard(true, "Candidate Start Sentence:", currentCandWindowStartSentence);
                                logUtil.logAndWriteStandard(true, "Suspicious Start Character Index:", swiSuspicious.getCharacterStartIndex());
                                logUtil.logAndWriteStandard(true, "Candidate Start Character Index:", swiCandidate.getCharacterStartIndex());
                                logUtil.logAndWriteStandard(true, "Suspicious End Character Index:", swiSuspicious.getCharacterEndIndex());
                                logUtil.logAndWriteStandard(true, "Candidate End Character Index:", swiCandidate.getCharacterEndIndex());
                                logUtil.logAndWriteStandard(true, "Suspicious Tokens:", currentSuspiciousIdTokensMap.get(suspiciousIdTokenExt.getKey()));
                                logUtil.logAndWriteStandard(true, "Candidate Tokens:", currentCandidateIdTokensMap.get(selectedCandidatePath));
                                logUtil.logAndWriteStandard(true, "Fragment Score:", fragmentScore);
                                logUtil.logAndWriteStandard(false, logUtil.dashes(50));
                                currentScoringChunk.printMe(
                                        "%-40s%s%n",
                                        currentSuspiciousIdTokensMap.get(suspiciousIdTokenExt.getKey()),
                                        currentCandidateIdTokensMap.get(selectedCandidatePath)
                                );

                            }
                            //TODO performance mark fileprints as optional
                            // Adding scoring chunk with coordinates to matrix.
                            scoringChunksCombined.storeScoringChunkToScoringMatrix(currentScoringChunk, suspiciousSlidingWindowY, candSlidingWindowX);

                            candSlidingWindowX++;

                            // Clear memory (ok?)
                            currentCandidateIdTokensMap.clear();
                            fragmentScoresMap.clear();
                        }
                        swiSuspicious.deinitialize();
                        suspiciousSlidingWindowY++;
                        currentSuspiciousIdTokensMap.clear();

                    }
                    // After each candidate and suspicious file combination
                    // ... calculate the plagiarism sections from windows
                    scoringChunksCombined.calculateMatrixClusters(params.USE_ADAPTIVE_CLUSTERING_TRESH, params.ADAPTIVE_FORM_FACTOR);
                    // ... write down results
                    // TODO solve this in multithreading context
                    this.extendedXmlResultsPath = scoringChunksCombined.writeDownXMLResults(tag, initialDateString, preprocessedCachingDirectory);
                    if (params.LOG_TO_CSV) {
                        scoringChunksCombined.writeScoresMapAsCSV(tag, initialDateString, preprocessedCachingDirectory);
                    }
                    logUtil.logAndWriteStandard(true, "done processing file combination", suspFilename, "with", candFilename);
                    logUtil.logAndWriteStandard(false, logUtil.dashes(100));

                    // MEMORY: Clear candidate entities from memory.
                    candidateEntities.clear();
                } catch (Exception ex) {
                    logUtil.logAndWriteError(true, "Exception processing file combination",
                            suspiciousIdTokenExt.getKey(), selectedCandidatePath);
                    ex.printStackTrace();
                } finally {
                    // ... free memory
                    //TODO perfomance: Check if memory is released here properly
                    scoringChunksCombined.flushInternalCombinedChunks();
                    System.gc();
                }
            }

        }
        selectedCandidateKeys.clear();
        suspiciousIdTokensMapExt.clear();
        logUtil.writeStandardReport(false, "Whats going on here?");
    }

    SlidingWindowInfo getWikiEntityStringsForSlidingWindow(
            List<SavedEntity> savedEntities, int startSentenceIndex, int windowSize, String filename){
        // Obtaining the entities which are within the window
        int endSentenceIndex = startSentenceIndex + windowSize;
        List<SavedEntity> windowEntitysSusp = savedEntities.stream()
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
        startChars.clear();
        // Getting relative stop index as the character end coordinate of the last entity
        List<Integer> endChars = windowEntitysSusp.stream()
                .map(SavedEntity::getToken)
                .map(Token::getEndCharacter)
                .collect(Collectors.toList());
        int lastEndChar = 0;
        if(endChars.size()>=1){
            lastEndChar = Collections.max(endChars);
        }
        endChars.clear();

        // Casting the entities for performing the cosine analysis
        List<String> entityIdsForWindow = windowEntitysSusp
                .stream()
                .map(SavedEntity::getWikidataEntityId)
                .collect(Collectors.toList());
        windowEntitysSusp.clear();
        // Return everything in a compound object
        return new SlidingWindowInfo(filename, entityIdsForWindow, firstStartChar, lastEndChar,
                startSentenceIndex, endSentenceIndex);
    }

    Integer getMaxSentenceNumber(Map.Entry<String, List<SavedEntity>> inputMap){
        return Collections.max(inputMap
                .getValue().stream()
                .map(SavedEntity::getToken)
                .map(Token::getSentenceNumber)
                .collect(Collectors.toList()));
    }

    Integer getMaxSentenceNumber(List<SavedEntity> inputEntities){
        return Collections.max(
                inputEntities.stream()
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
     * @throws Exception on error in preprocessing
     */
    public List<SavedEntity> preProcessExtendedInfo(String documentPath, String documentLanguage) throws Exception {

        // read in the file
        File documentFile = new File(documentPath);
        String documentText = FileUtils.readFileToString(documentFile, StandardCharsets.UTF_8);

        String documentEntitiesPath;

        documentEntitiesPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended", "serialized_entities",
                documentFile.getName().concat(".ser"))
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
        final boolean showProgress = false;
        // create dictionary
        //logger.info("Create dictionary");
        Dictionary<String> dictionary = new Dictionary<>(candidateIdTokensMap);

        // perform detailed analysis
        //logger.info("Perform detailed analysis");

        // progress bar
        AtomicInteger progress = new AtomicInteger(0);

        ProgressBar progressBar = null;
        if(showProgress) {
            progressBar = new ProgressBar("Perform cosine similarity analysis", suspiciousIdTokensMap.entrySet().size(), ProgressBarStyle.ASCII);

            progressBar.start();
         }

        // iterate the suspicious documents
        ProgressBar finalProgressBar = progressBar;
        Map<String, Map<String, Double>> suspiciousIdCandidateScoresMap = suspiciousIdTokensMap.entrySet()
                .parallelStream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> {
                            if (showProgress){
                                finalProgressBar.stepTo(progress.incrementAndGet());
                            }
                            // look in dictionary
                            return dictionary.query(entry.getValue());
                        }
                ));
        if(showProgress) {
            progressBar.stop();
        }
        return  suspiciousIdCandidateScoresMap;
    }

    /**
     * Cosine similarity analysis.
     * TODO is this used anywhere ?
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

    public Map<String, Map<String, Double>> performEnhancedCosineSimilarityAnalysisP(
            Map<String, List<String>> suspiciousIdTokensMap,
            Map<String, List<String>> candidateIdTokensMap) {
        Map<String, Map<String, Double>> suspiciousIdDetectedCandidateIdsMap = new HashMap<>();

        ProgressBar ontologyProgressBar = new ProgressBar("Enhancing vectors with ontology data",
                suspiciousIdTokensMap.size() + candidateIdTokensMap.size(),
                ProgressBarStyle.ASCII);
        ontologyProgressBar.start();

        Map<String, Map<String, Double>> suspiciousIdTokenCountMap = new HashMap<>();
        Map<String, Map<String, Double>> candidateIdTokenCountMap = new HashMap<>();


        suspiciousIdTokensMap.entrySet().parallelStream().forEach((suspiciousIdTokensMapEntry) -> {
            String id = suspiciousIdTokensMapEntry.getKey();
            List<String> tokens = suspiciousIdTokensMapEntry.getValue();
            Map<String, Double> countMap = getHierarchicalCountMap(tokens);
            suspiciousIdTokenCountMap.put(id, countMap);
            ontologyProgressBar.step();
        });




        candidateIdTokensMap.entrySet().parallelStream().forEach((candidateIdTokensMapEntry) -> {
            String id = candidateIdTokensMapEntry.getKey();
            List<String> tokens = candidateIdTokensMapEntry.getValue();
            Map<String, Double> countMap = getHierarchicalCountMap(tokens);
            candidateIdTokenCountMap.put(id, countMap);
            ontologyProgressBar.step();
        });


        ontologyProgressBar.stop();


        // perform detailed analysis
        logger.info("Perform detailed analysis");

        // progress bar
        ProgressBar progressBar = new ProgressBar("Perform cosine similarity analysis",
                suspiciousIdTokenCountMap.size() * candidateIdTokenCountMap.size(),
                ProgressBarStyle.ASCII);
        progressBar.start();

        // iterate the suspicious documents

        suspiciousIdTokenCountMap.entrySet().parallelStream().forEach(((suspiciousEntry)-> {
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
        }));




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
