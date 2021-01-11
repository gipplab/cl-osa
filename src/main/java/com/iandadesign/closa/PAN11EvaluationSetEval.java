package com.iandadesign.closa;

import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.SavedEntity;
import com.iandadesign.closa.model.StatisticsInfo;
import com.iandadesign.closa.util.*;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;


import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Complete Evaluation for PAN11 implementation (not extended from abstract class etc)
 * Since tests not work cause of local dependency missing, the placement of the class here
 * is a workaround to make evaluations executable.
 * @author Johannes Stegmüller (26.06.2020)
 */
public class PAN11EvaluationSetEval {
    public static String pathPrefix = "/data/pan-plagiarism-corpus-2011/external-detection-corpus";

    public static void main(String[] args) {
        Boolean smallTest = false;                  // Just select few suspicious files for the complete process
        Boolean evaluateCandidateRetrieval = false; // This triggers only the CR evaluation.
        Boolean mockCRResults = true;              // This will test detailed analysis with mocked CR results
        Integer maxMockSuspCandiates = 10;          // This is a delimeter for the maximum of suspicious files locked in mockCR Evaluation, set over 304 to check all susp files.


        //evalPAN2011All();

        if(args!=null && args.length >= 1){
            evalPAN2011EnEs(args[0], smallTest, evaluateCandidateRetrieval, mockCRResults, maxMockSuspCandiates );
        }else{
            evalPAN2011EnEs(null, smallTest, evaluateCandidateRetrieval, mockCRResults, maxMockSuspCandiates );
        }
    }


    static void doCREvaluationRecall(ExtendedAnalysisParameters params, String tag, String comment,
                                                       HashMap<String, List<String>> resultSelectedCandidates){
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory;

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);

        logUtil.writeStandardReport(false, "Assuming the preprpocessing has been done here. ");
        params.MAX_NUM_CANDIDATES_SELECTED = 5000;
        //params.CANDIDATE_SELECTION_TRESH = 0;
        logParams(logUtil, tag, params, osa);


        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");


        WeakHashMap<String, List<SavedEntity>> suspiciousIdTokensMapExt = new WeakHashMap<>();
        try {
            Map<String, Map <String, Double>> suspiciousIdCandidateScoresMap =  osa.doCandidateRetrievalExtendedInfo2(suspiciousFiles, candidateFiles, params, logUtil.getDateString(), suspiciousIdTokensMapExt);
            // Evaluate results

            Double recallAt1 = PAN11RankingEvaluator.calculateRecallAtK(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 1, logUtil);
            Double recallAt5 = PAN11RankingEvaluator.calculateRecallAtK(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 5, logUtil);
            Double recallAt10 = PAN11RankingEvaluator.calculateRecallAtK(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 10, logUtil);
            Double recallAt20 = PAN11RankingEvaluator.calculateRecallAtK(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 20, logUtil);
            Double recallAt50 = PAN11RankingEvaluator.calculateRecallAtK(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 50, logUtil);
            Double recallAt100 = PAN11RankingEvaluator.calculateRecallAtK(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 100, logUtil);


            Double recallAt1S = PAN11RankingEvaluator.calculateRecallAtKStandard(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 1, logUtil);
            Double recallAt5S = PAN11RankingEvaluator.calculateRecallAtKStandard(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 5, logUtil);
            Double recallAt10S = PAN11RankingEvaluator.calculateRecallAtKStandard(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 10, logUtil);
            Double recallAt20S = PAN11RankingEvaluator.calculateRecallAtKStandard(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 20, logUtil);
            Double recallAt50S = PAN11RankingEvaluator.calculateRecallAtKStandard(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 50, logUtil);
            Double recallAt100S = PAN11RankingEvaluator.calculateRecallAtKStandard(suspiciousIdCandidateScoresMap, resultSelectedCandidates, 100, logUtil);




            logUtil.logAndWriteStandard(false, "Recall calculation done");

        } catch (Exception ex){
            logUtil.logAndWriteError(false, "Exception during parse of suspicious files ");
            ex.printStackTrace();
        }
    }


    /**
     * Preselection for PAN-PC11 Evaluation. Language Partitioning like F.Salvador.
     * Writes preselection to params. Calls evalPAN2011All.
     * @param languageIn de or es (en-de or en-es dataset)
     * @param smallTest just select a small amount of data
     * @param mockCRResults instead of using a real CR, just select a list of mocked candidates
     * @param testCandidateRetrieval in subsequent function do an evaluation for candidate retrieval
     */
    static void evalPAN2011EnEs(String languageIn, Boolean smallTest, Boolean testCandidateRetrieval, Boolean mockCRResults, int maxMockSuspCandidates){
        // This evaluates the specific English/Espanol-Partition from Franco Salvador
        String tag = "evalPAN2011En-DeEs"; // Identifier for logs ...
        String language = "es"; //state "es" or "de" here
        if(languageIn!=null){
            language=languageIn;
        }
        List<String> allowedCaseLengths = new ArrayList<>();
        allowedCaseLengths.add(PAN11PlagiarismInfo.CaseLength.LONG);
        allowedCaseLengths.add(PAN11PlagiarismInfo.CaseLength.MEDIUM);
        allowedCaseLengths.add(PAN11PlagiarismInfo.CaseLength.SHORT);

        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");
        ExtendedAnalysisParameters params;
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }
        // Get the filenumbers for susp and candidate files ...
        List<File> suspiciousFilesLangXML = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
        List<File> candidateFilesLangXML = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".xml");
        PAN11XMLParser pan11XMLParser = new PAN11XMLParser();

        List<Integer> usedCandidates = new ArrayList<>();
        List<Integer> usedSuspicious = new ArrayList<>();

        HashMap<String, List<String>> resultSelectedCandidates = new HashMap<>();
        HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation = new HashMap<>();

        int ctr = 0;
        for(File suspFileXML: suspiciousFilesLangXML){
            boolean hasValidLanguagePair = false;
            // Read XML File
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);
            List <String> selectedCandidateForFile = new ArrayList<>();
            List<PAN11PlagiarismInfo> plaginfosCurrent = new ArrayList<>();
            for(PAN11PlagiarismInfo plaginfo:xmlInfo.plagiarismInfos) {
                plaginfosCurrent.add(plaginfo);
                if (plaginfo.getSourceLanguage().equals(language)) {//|| plaginfo.getSourceLanguage().equals("de")){ //"es" //"de""
                    if(!allowedCaseLengths.contains(plaginfo.getCaseLengthThis())) {
                        continue; // Filter non-matching case-lengths
                    }
                    if (!hasValidLanguagePair) {
                        hasValidLanguagePair = true;
                    }
                    // Fill hashmap with for testcandidate evaluation
                    if(mockCRResults){
                        if(!selectedCandidateForFile.contains(plaginfo.getSourceReference())){
                            selectedCandidateForFile.add(plaginfo.getSourceReference());

                        }
                    }
                    if(language.equals("de")) {
                        Integer sourceId = Integer.parseInt(plaginfo.getSourceReference().replaceAll("\\D+", ""));
                        if (!usedCandidates.contains(sourceId)) {
                            usedCandidates.add(sourceId);
                        }
                    }
                }

            }

            if(hasValidLanguagePair){
                Integer suspId = Integer.parseInt(suspFileXML.getName().replaceAll("\\D+",""));
                usedSuspicious.add(suspId);
                if(mockCRResults) {
                    plagiarismInformation.put(suspFileXML.getName(), plaginfosCurrent);

                    resultSelectedCandidates.put(suspFileXML.getName(), selectedCandidateForFile);
                    ctr++;
                    if(ctr>=maxMockSuspCandidates){
                      break;
                    }
                }
            }
        }

        // Salvador differentiates in mechanism of selection on de/es
        // In words: in spanish all source documents are used, in german only the source documents which are related to plagiarism
        if(language.equals("es")) {
            // Getting the corresponding candidates
            for (File candFileXML : candidateFilesLangXML) {
                PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(candFileXML);
                if (xmlInfo.language.equals(language)) {
                    Integer sourceId = Integer.parseInt(candFileXML.getName().replaceAll("\\D+", ""));
                    if (!usedCandidates.contains(sourceId)) {
                        usedCandidates.add(sourceId);
                    }
                }
            }
        }
        // Checking if selection is ok (these are the numbers stated in Salador2016, only if no caselength filtering)
        boolean CHECKSELECTION = false;
        if(CHECKSELECTION) {
            if (allowedCaseLengths.size() == 3 && language.equals("es")) {
                if (usedCandidates.size() != 202 || usedSuspicious.size() != 304) {
                    System.err.println("Wrong file numbers");
                    return;
                }
            } else if (allowedCaseLengths.size() == 3 && language.equals("de")) {
                if (usedCandidates.size() != 348 || usedSuspicious.size() != 251) {
                    System.err.println("Wrong file numbers");
                    return;
                }
            }
        }


        // Overwrite the file filter
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }
        if(smallTest){
            //usedCandidates = usedCandidates.subList(0,1);
            usedSuspicious = usedSuspicious.subList(0,2);
        }

        params.USE_FILE_FILTER = true;
        params.USE_LANGUAGE_WHITELISTING = false; // This is done in the step above
        PANFileFilter panFileFilter = new PANFileFilter();
        panFileFilter.addToWhiteListMultiple(true, usedCandidates);
        panFileFilter.addToWhiteListMultiple(false, usedSuspicious);

        // Overwrite the filter with new filtering
        params.panFileFilter = panFileFilter;

        // Free memory TODO MEMORY MARK1: is clear effective: it is but it can delete the fundamental data if there are multirefs
         suspiciousFilesLangXML.clear();
         candidateFilesLangXML.clear();

        if(testCandidateRetrieval){
            doCREvaluationRecall(params, tag, "CREvalRecall", resultSelectedCandidates);
            return;
        }
        if(mockCRResults){
            Map<String, List<String>> resultsMockCR = getMockCRResults(params, tag, "CREval", resultSelectedCandidates);
            evalPAN2011MockCandidates(params, tag,"Parsing En-" + language + " with mock preselected CR candidates", resultsMockCR, plagiarismInformation);
            return;
        }


        // Just process as usual
        evalPAN2011AllNew(params, tag, "Parsing En-" + language + "\n" +
                "used Candidates Files: " + usedCandidates.size() +
                "\nUsed Suspicious Files: " + usedSuspicious.size(),
                false
        );

        // }else {
        // }
    }
    static Map<String, List<String>> getMockCRResults(ExtendedAnalysisParameters params, String tag, String comment,
                                                       HashMap<String, List<String>> resultSelectedCandidates){
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory;

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);

        logUtil.writeStandardReport(false, "Assuming the preprpocessing has been done here. ");
        params.MAX_NUM_CANDIDATES_SELECTED = 202;
        params.CANDIDATE_SELECTION_TRESH = 0;
        int MOCK_CANDIDATES_PER_FILE = 20;
        PAN11EvaluationSetEval.logParams(logUtil, tag, params, osa);


        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");


        AtomicInteger overallPlagiariasmFiles = new AtomicInteger();
        AtomicInteger overallMatches = new AtomicInteger();
        AtomicInteger accPos = new AtomicInteger();
        AtomicInteger maxPos = new AtomicInteger();
        AtomicInteger averagePos = new AtomicInteger();

        Map<String, List<String>> mockCandidates = new HashMap<>();
        WeakHashMap<String, List<SavedEntity>> suspiciousIdTokensMapExt = new WeakHashMap<>();
        try {
            Map<String, Map <String, Double>> suspiciousIdCandidateScoresMap =  osa.doCandidateRetrievalExtendedInfo2(suspiciousFiles, candidateFiles, params, logUtil.getDateString(), suspiciousIdTokensMapExt);
            // Evaluate results
            suspiciousIdCandidateScoresMap.keySet().forEach((suspiciousFilePath) -> {

                String suspPath = new File(suspiciousFilePath).getPath();
                String suspFileName =new File(suspiciousFilePath).getName();

                Map<String, Double> selectedCandidates = suspiciousIdCandidateScoresMap.get(suspiciousFilePath);
                Map<String, Double> candidateScoresMapS = selectedCandidates.entrySet().stream()
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                suspiciousIdTokensMapExt.clear();
                // Get the corresponding result candidates
                List<String> actualCandidates = resultSelectedCandidates.get(suspFileName.replace(".txt",".xml"));
                overallPlagiariasmFiles.addAndGet(actualCandidates.size());
                // Do a comparison here quickly
                long accPositions = 0;
                int posCounter = 0;
                int matchCandidates = 0;


                logUtil.logAndWriteStandard(false, "Evaluating Suspicious " + suspFileName+ " --------------------");

                List<String> mockMatchesForThisFile = new ArrayList<>();
                List<String> mockFalsePositivesForThisFile = new ArrayList<>();
                for(String selectedCandidate:candidateScoresMapS.keySet()) {
                    File filename = new File(selectedCandidate);

                    if(actualCandidates.contains(filename.getName())){
                        logUtil.logAndWriteStandard(false, "Found Candidate " + filename.getName()+ " at pos: " + posCounter + "\t score: " + candidateScoresMapS.get(selectedCandidate));
                        if(posCounter > maxPos.get()){
                            maxPos.set(posCounter);
                        }
                        accPos.addAndGet( posCounter);
                        matchCandidates++;
                        mockMatchesForThisFile.add(selectedCandidate);
                    }else {
                        if(posCounter<=MOCK_CANDIDATES_PER_FILE){
                            mockFalsePositivesForThisFile.add(selectedCandidate);

                        }
                    }
                    posCounter++;
                }
                // Create realistic selection for this susp file
                List<String> usedMockCandidates = new ArrayList<>();
                usedMockCandidates.addAll(mockMatchesForThisFile);
                // The first candidates are always the real ones, the other ones are realistic false positives
                // TODO turn on False positives gain
                //usedMockCandidates.addAll(mockFalsePositivesForThisFile.subList(mockMatchesForThisFile.size(),MOCK_CANDIDATES_PER_FILE));

                mockCandidates.put(suspiciousFilePath, usedMockCandidates);

                // done creating selection
                overallMatches.addAndGet(matchCandidates);
                // Save score for complete comparison.
                logUtil.logAndWriteStandard(false, "Matched candidates: " + matchCandidates+ "/"+actualCandidates.size());
                logUtil.logAndWriteStandard(false, "Overall Matched candidates: " + overallMatches.get()+ "/"+ overallPlagiariasmFiles.get() + " max pos: " + maxPos.get());

            });

            float meanPos = (float) accPos.get() / overallMatches.get();
            logUtil.logAndWriteStandard(false, "Mean Position: " + meanPos);

        } catch (Exception ex){
            logUtil.logAndWriteError(false, "Exception during parse of suspicious files ");
            ex.printStackTrace();

        }
        logUtil.logAndWriteStandard(false, "Overall Matched candidates: " + overallMatches.get()+ "/"+ overallPlagiariasmFiles.get()+ " max pos: " + maxPos.get());
        return mockCandidates;
    }

    /**
     * Logging the settings
     * @param logUtil
     * @param tag
     * @param params
     * @param osa
     */
    public static void logParams(ExtendedLogUtil logUtil, String tag, ExtendedAnalysisParameters params, OntologyBasedSimilarityAnalysis osa){
        // Starting log
        logUtil.logAndWriteStandard(false,logUtil.dashes(100));
        logUtil.logAndWriteStandard(true,"Starting PAN2011 evaluation");
        logUtil.logAndWriteStandard(true,"TAG:", tag);
        logUtil.logAndWriteStandard(true,"Time:", DateTime.now());
        logUtil.logAndWriteStandard(true,"Results and Caching directory:", osa.getPreprocessedCachingDirectory());
        logUtil.logAndWriteStandard(true,"Standard-Logfiles Path:", osa.getStandardlogPath());
        logUtil.logAndWriteStandard(true,"Error-Logfiles Path:", osa.getErrorlogPath());
        logUtil.logAndWriteStandard(true,"Logging Standard.out to file activated:", params.LOG_STANDARD_TO_FILE);
        logUtil.logAndWriteStandard(true,"Logging Error.out to file activated:", params.LOG_ERROR_TO_FILE);
        logUtil.logAndWriteStandard(true,"Saving 2D-Matrix to .csv:", params.LOG_TO_CSV);
        logUtil.logAndWriteStandard(true,"Logging verbosely activated:", params.LOG_VERBOSE);
        logUtil.logAndWriteStandard(true,"Pre-Filter the complete dataset:", params.USE_FILE_FILTER);
        logUtil.logAndWriteStandard(true,"Parallelism fetching Wikidata entries:", osa.getDoParallelRequests());
        logUtil.logAndWriteStandard(true,"Sliding Window Length:", params.NUM_SENTENCES_IN_SLIDING_WINDOW);
        logUtil.logAndWriteStandard(true,"Sliding Window Increment:", params.NUM_SENTENCE_INCREMENT_SLIDINGW);
        logUtil.logAndWriteStandard(true,"Clustering Adjacent Threshold:", params.ADJACENT_THRESH);
        logUtil.logAndWriteStandard(true,"Clustering Single Threshold:", params.SINGLE_THRESH);
        logUtil.logAndWriteStandard(true,"Clustering Use adaptive Threshold (by median):", params.USE_ADAPTIVE_CLUSTERING_TRESH);
        logUtil.logAndWriteStandard(true,"Adaptive Threshold form factor:", params.ADAPTIVE_FORM_FACTOR);
        logUtil.logAndWriteStandard(true,"Clipping Margin Characters:", params.CLIPPING_MARGING);
        logUtil.logAndWriteStandard(true,"Maximum selected candidates:", params.MAX_NUM_CANDIDATES_SELECTED);
        logUtil.logAndWriteStandard(true,"Candidate Selection Threshold:", params.CANDIDATE_SELECTION_TRESH);
        logUtil.logAndWriteStandard(true,"Sublist Token Length:", osa.getLenSublistTokens());
        logUtil.logAndWriteStandard(true,"Run evaluation after processing:", params.RUN_EVALUATION_AFTER_PROCESSING);
        logUtil.logAndWriteStandard(true,"Parallelism Thread Difference:", params.PARALLELISM_THREAD_DIF);

        logUtil.logAndWriteStandard(false, logUtil.dashes(100));


    }




    static void evalPAN2011MockCandidates(ExtendedAnalysisParameters params, String tag, String comment,
                                          Map<String, List<String>> mockSuspToSelectedCandidates, Map<String, List<PAN11PlagiarismInfo>> plagiarismInfo)  {
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);
        // ..

        PAN11EvaluationSetEval.logParams(logUtil, tag, params, osa);

        logUtil.logAndWriteStandard(false, logUtil.dashes(100));

        logUtil.logAndWriteStandard(false, "Starting file comparisons...");


        int parsedFiles = 0;
        int parsedErrors = 0;
        String baseResultsPath = "";

        // Multi Thread Execution

        // Limit: https://www.codementor.io/@nitinpuri/controlling-parallelism-of-java-8-collection-streams-umex0qbt1
        AtomicInteger indexP = new AtomicInteger(0);
        AtomicInteger parsedFilesP = new AtomicInteger(0);
        AtomicInteger parsedErrorsP = new AtomicInteger(0);
        logUtil.logAndWriteStandard(true, logUtil.getDateString(), " Using parallelism, counter (x/y) is only a vague indicator");

        int parallelism = Runtime.getRuntime().availableProcessors() - params.PARALLELISM_THREAD_DIF;
        if(parallelism < 1){
            logUtil.writeStandardReport(false, "Starting Parallel Processing Dif in settings too big");
            parallelism = 1;
        }
        logUtil.writeStandardReport(false, "Starting Parallel Processing with Num CPUs: "+ parallelism);
        ForkJoinPool forkJoinPool = null;

        try {
            //osa.createOverallDictionary(params, mockSuspToSelectedCandidates);
            forkJoinPool = new ForkJoinPool(parallelism);
            Map<String, List<String>> finalMockSuspToSelectedCandidates = mockSuspToSelectedCandidates;
            Map<String, List<StatisticsInfo>> allStatisticsInfos = new HashMap<>();

            forkJoinPool.submit(() -> finalMockSuspToSelectedCandidates.keySet().parallelStream().forEach((suspiciousFilePath) -> {
                String suspPath = suspiciousFilePath;
                String suspFileName = new File(suspiciousFilePath).getName();
                List<File> candidateFiles = new ArrayList<>();
                for(String candidateEntry: finalMockSuspToSelectedCandidates.get(suspiciousFilePath)){
                    candidateFiles.add(new File(candidateEntry));
                }
                try {
                    logUtil.logAndWriteStandard(true, logUtil.getDateString(), "Parsing Suspicious file ", indexP.get() + 1, "/", finalMockSuspToSelectedCandidates.keySet().size(), "Filename:", suspFileName, " and its", candidateFiles.size(), "candidates");
                    parsedFilesP.getAndIncrement();
                    indexP.getAndIncrement();
                    List<PAN11PlagiarismInfo>  currentPlaginfoList = plagiarismInfo.get(suspFileName.replace(".txt",".xml"));
                    OntologyBasedSimilarityAnalysis osaT = new OntologyBasedSimilarityAnalysis(null, null);
                    osaT.setLogger(osa.getExtendedLogUtil(), osa.getTag()); // this has to be done immediately after constructor
                    List<StatisticsInfo> statisticsInfos = osaT.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString(), currentPlaginfoList);
                    allStatisticsInfos.put(suspFileName, statisticsInfos); // ok in multithreading?
                    osaT = null;
                    System.gc(); // Excplicit call to garbage collector.
                } catch (Exception ex) {
                    parsedErrorsP.getAndIncrement();
                    indexP.getAndIncrement();
                    logUtil.logAndWriteError(false, "Exception during parse of suspicious file with Filename", suspFileName, "Exception:");
                    ex.printStackTrace();
                }
            })).get();

            // Combining statistics information
            if(params.DO_RESULTS_ANALYSIS){
                StatisticsInfo combinedInfo = ExtendedAnalytics.createCombinedStatistics(allStatisticsInfos);
                ExtendedAnalytics.printStatisticsInfo(combinedInfo, logUtil);
            }
        }catch(Exception e) {//SecurityException | RejectedExecutionException e){
            logUtil.logAndWriteError(false, "Exception with with thread execution:", e);
            e.printStackTrace();
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown(); //always remember to shutdown the pool
            }
        }

        // Generating xmlResultsfolderPath as it is done in each thread atm.
        String xmlResultsFolderPath = Paths.get(osa.getPreprocessedCachingDirectory(), "preprocessed_extended",
                "results_comparison", tag.concat("_").concat(logUtil.getDateString())).toAbsolutePath().toString();
        baseResultsPath = xmlResultsFolderPath;
        parsedFiles = parsedFilesP.get();
        parsedErrors = parsedErrorsP.get();



        if(parsedErrors>=1 || parsedFiles==0){
            logUtil.writeErrorReport(false, "There have been problems during detecting plagiarism: ");
            logUtil.writeErrorReport(false, "Parsing errors:  ", parsedErrors);
            logUtil.writeErrorReport(false, "Parsed files:  ", parsedFiles);
        }


        //TODO MEMORY FREE MEMORY HERE ? nO


        // Evaluation related stuff ...
        if(params.RUN_EVALUATION_AFTER_PROCESSING){
            if(!params.USE_FILE_FILTER){
                // No filter-> just do the regular evaluation with all files
                PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, baseResultsPath, toplevelPathSuspicious);
            }else{
                // Filter used, only compare with relevant files
                File cachingDir= new File(baseResultsPath +"/file_selection_cache");
                PAN11FileUtil.removeDirectory(cachingDir);
                List<File> suspiciousXML  =  PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
                PAN11FileUtil.writeFileListToDirectory(suspiciousXML, cachingDir.getPath(), logUtil);
                PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, baseResultsPath, cachingDir.getPath());
                PAN11FileUtil.removeDirectory(cachingDir);
            }
            logUtil.logAndWriteStandard(true,logUtil.getDateString(), "done doing evaluation for PAN11");
        }
        logUtil.closeStreams();
    }
    /**
     *
     * @param params parameters which might have been adapted for preselection
     * @param tag
     * @param comment
     */
    public static void evalPAN2011AllNew(ExtendedAnalysisParameters params, String tag, String comment, boolean doPreprocessing)  {
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory;

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);
        // ..

        logParams(logUtil, tag, params, osa);

        if(doPreprocessing) {
            logUtil.logAndWriteStandard(false, "Preprocessing all candidate files...");
            int candidatePreprocessedFiles = doAllPreprocessing(toplevelPathCandidates, osa, null, params, true);  // candidate docs can be en,de,es
            logUtil.logAndWriteStandard(false, "Preprocessing all suspicious files...");
            int suspiciousPreprocessedFiles = doAllPreprocessing(toplevelPathSuspicious, osa, "en", params, false); // all suspicious docs are english
            int numfiles = suspiciousPreprocessedFiles + candidatePreprocessedFiles;
            logUtil.logAndWriteStandard(false, "Number of preprocessed files is: " + numfiles);
            if (!params.USE_FILE_FILTER && !params.USE_LANGUAGE_WHITELISTING && numfiles != (11093 * 2)) {
                logUtil.logAndWriteError(false, "Aborting: Processed files not 2*11093=22186 but " + numfiles);
                return;
            }
        }
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));

        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");
        /*
        try {
            osa.executeAlgorithmForAllfiles(suspiciousFiles, candidateFiles, params, logUtil.getDateString());
        } catch (Exception ex) {
            logUtil.logAndWriteError(false, "Exception during parse of all files Exception:", ex);
            ex.printStackTrace();
        }
        */

        int parsedFiles = 0;
        int parsedErrors = 0;
        String baseResultsPath = "";

        // Do the file comparisons
        if(!osa.getDoParallelRequests()) {
            // Single Thread Execution
            for (int index = 0; index < suspiciousFiles.size(); index++) {
                String suspPath = suspiciousFiles.get(index).getPath();
                String suspFileName = suspiciousFiles.get(index).getName();
                try {
                    logUtil.logAndWriteStandard(true, logUtil.getDateString(), "Parsing Suspicious file ", index + 1, "/", suspiciousFiles.size(), "Filename:", suspFileName, " and its", candidateFiles.size(), "candidates");
                    OntologyBasedSimilarityAnalysis osaT = new OntologyBasedSimilarityAnalysis();
                    osaT.initializeLogger(tag, params); // this has to be done immediately after constructor
                    osaT.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString(),null);
                    baseResultsPath = osaT.getExtendedXmlResultsPath();
                    parsedFiles++;
                } catch (Exception ex) {
                    parsedErrors++;
                    logUtil.logAndWriteError(false, "Exception during parse of suspicious file with Filename", suspFileName, "Exception:", ex);
                }
            }
            logUtil.logAndWriteStandard(true, logUtil.getDateString(), "done processing PAN11,- parsed files:", parsedFiles, "errors:", parsedErrors);
        } else {
            // Multi Thread Execution

            // Limit: https://www.codementor.io/@nitinpuri/controlling-parallelism-of-java-8-collection-streams-umex0qbt1
            AtomicInteger indexP = new AtomicInteger(0);
            AtomicInteger parsedFilesP = new AtomicInteger(0);
            AtomicInteger parsedErrorsP = new AtomicInteger(0);
            logUtil.logAndWriteStandard(true, logUtil.getDateString(), " Using parallelism, counter (x/y) is only a vague indicator");

            int parallelism = Runtime.getRuntime().availableProcessors() - params.PARALLELISM_THREAD_DIF;
            if(parallelism < 1){
                logUtil.writeStandardReport(false, "Starting Parallel Processing Dif in settings too big");
                parallelism = 1;
            }
            logUtil.writeStandardReport(false, "Starting Parallel Processing with Num CPUs: "+ parallelism);
            ForkJoinPool forkJoinPool = null;

            try {

                forkJoinPool = new ForkJoinPool(parallelism);
                forkJoinPool.submit(() -> suspiciousFiles.parallelStream().forEach((suspiciousFile) -> {
                    String suspPath = suspiciousFile.getPath();
                    String suspFileName = suspiciousFile.getName();
                    try {
                        logUtil.logAndWriteStandard(true, logUtil.getDateString(), "Parsing Suspicious file ", indexP.get() + 1, "/", suspiciousFiles.size(), "Filename:", suspFileName, " and its", candidateFiles.size(), "candidates");
                        parsedFilesP.getAndIncrement();
                        indexP.getAndIncrement();
                        OntologyBasedSimilarityAnalysis osaT = new OntologyBasedSimilarityAnalysis(null, null);
                        osaT.setLogger(osa.getExtendedLogUtil(), osa.getTag()); // this has to be done immediately after constructor
                        osaT.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString(),null);
                        osaT = null;
                        System.gc(); // Excplicit call to garbage collector.
                    } catch (Exception ex) {
                        parsedErrorsP.getAndIncrement();
                        indexP.getAndIncrement();
                        logUtil.logAndWriteError(false, "Exception during parse of suspicious file with Filename", suspFileName, "Exception:");
                        ex.printStackTrace();
                    }
                })).get();

            } catch(Exception e) {//SecurityException | RejectedExecutionException e){
                logUtil.logAndWriteError(false, "Exception with with thread execution:", e);
            } finally {
                if (forkJoinPool != null) {
                    forkJoinPool.shutdown(); //always remember to shutdown the pool
                }
            }

            // Generating xmlResultsfolderPath as it is done in each thread atm.
            String xmlResultsFolderPath = Paths.get(osa.getPreprocessedCachingDirectory(), "preprocessed_extended",
                    "results_comparison", tag.concat("_").concat(logUtil.getDateString())).toAbsolutePath().toString();
            baseResultsPath = xmlResultsFolderPath;
            parsedFiles = parsedFilesP.get();
            parsedErrors = parsedErrorsP.get();
        }


        if(parsedErrors>=1 || parsedFiles==0){
            return;
        }


        //TODO MEMORY FREE MEMORY HERE ? nO


        // Evaluation related stuff ...
        if(params.RUN_EVALUATION_AFTER_PROCESSING){
            if(!params.USE_FILE_FILTER){
                // No filter-> just do the regular evaluation with all files
                PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, baseResultsPath, toplevelPathSuspicious);
            }else{
                // Filter used, only compare with relevant files
                File cachingDir= new File(baseResultsPath +"/file_selection_cache");
                PAN11FileUtil.removeDirectory(cachingDir);
                List<File> suspiciousXML  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
                PAN11FileUtil.writeFileListToDirectory(suspiciousXML, cachingDir.getPath(), logUtil);
                PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, baseResultsPath, cachingDir.getPath());
                PAN11FileUtil.removeDirectory(cachingDir);
            }
            logUtil.logAndWriteStandard(true,logUtil.getDateString(), "done doing evaluation for PAN11");
        }
        logUtil.closeStreams();
    }

    public static int doAllPreprocessing(String topFolderPath, OntologyBasedSimilarityAnalysis osa, String language, ExtendedAnalysisParameters params, boolean candOrSusp) {
        List<File> myFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(topFolderPath, params, candOrSusp, ".txt");
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(true,"doAllPreprocessing", topFolderPath + ". with length: " + myFiles.size());
        List<String> panLanguages = new ArrayList<>(); //J4T
        panLanguages.add("en");

        int processedCounter=0;
        int germanLanguageDocs=0;
        int spanishLanguageDocs=0;
        // Preprocess one file after other (TODO eventually add parallelism: mind that requests to dbs can already be parallelised)
        for (File documentFile : myFiles) {
            String documentPath = documentFile.getPath();

            try {
                logUtil.logAndWriteStandard(true,"doAllPreprocessing", "Starting preprocessing filename: "+documentFile.getName());
                String detectedLanguage;
                if(language==null) {
                    // If language not defined detect it
                     detectedLanguage = osa
                            .getLanguageDetector()
                            .detectLanguage(FileUtils.readFileToString(documentFile, StandardCharsets.UTF_8));
                }else{
                     detectedLanguage = language;
                }

                /*
                if(!detectedLanguage.equals("en")){ // susp part 21 10262
                    // System.out.println("does this happen? If not just set en without classifier"); //susp part21/10420 de,  before 01500 (1 german), source 00008 german 00011 es, 13 es  .....
                    if(!panLanguages.contains(detectedLanguage)){
                        panLanguages.add(detectedLanguage);
                    }
                    if(detectedLanguage.equals("de")){
                        germanLanguageDocs++;
                    }
                    if(detectedLanguage.equals("es")){
                        spanishLanguageDocs++;
                    }
                }
                */

                List<SavedEntity> preprocessedCandExt = osa.preProcessExtendedInfo(documentPath, detectedLanguage);
                logUtil.logAndWriteStandard(true,"doAllPreprocessing -> done", "Processed filename: "+documentFile.getName()+ " with entities: "+preprocessedCandExt.size());
                processedCounter++;
            }catch(Exception ex){
                // Save exception to error log
                logUtil.logAndWriteError(false, "doAllPreprocessing Error processing filename:"+documentFile.getName()+":"+ex);
            }
        }
        logUtil.logAndWriteStandard(true,"doAllPreprocessing finished processing ", processedCounter+" of "+myFiles.size());
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));
        return processedCounter;
    }

}












