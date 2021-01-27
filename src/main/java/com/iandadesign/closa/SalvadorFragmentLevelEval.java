package com.iandadesign.closa;

import com.iandadesign.closa.model.*;
import com.iandadesign.closa.util.*;
import edu.stanford.nlp.util.ArrayMap;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.iandadesign.closa.PAN11EvaluationSetEval.logParams;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

public class SalvadorFragmentLevelEval {

    public static String pathPrefix = "/data/pan-plagiarism-corpus-2011/external-detection-corpus";
    public static String preprocessedCachingDir = "/data/CLOSA_data/preprocessed";

    public static void main(String[] args) {
        Boolean smallTest = false;                  // Just select few suspicious files for the complete process
        Boolean evaluateCandidateRetrieval = true; // This triggers only the CR evaluation.
        Boolean addCRResultInfo = true;              // This will test detailed analysis with mocked CR results
        Integer maxMockSuspCandiates = 5000;          // This is a delimeter for the maximum of suspicious files locked in mockCR Evaluation, set over 304 to check all susp files.

        //evalPAN2011All();

        if(args!=null && args.length >= 1){
            evalPAN2011EnEs(args[0], smallTest, evaluateCandidateRetrieval, addCRResultInfo, maxMockSuspCandiates );
        }else{
            evalPAN2011EnEs(null, smallTest, evaluateCandidateRetrieval, addCRResultInfo, maxMockSuspCandiates );
        }
    }


    /**
     * Preselection for PAN-PC11 Evaluation. Language Partitioning like F.Salvador.
     * Writes preselection to params. Calls evalPAN2011All.
     * @param languageIn de or es (en-de or en-es dataset)
     * @param smallTest just select a small amount of data
     * @param addResultInfoForCR parse result info for CR
     * @param testCandidateRetrieval in subsequent function do an evaluation for candidate retrieval
     */
    static void evalPAN2011EnEs(String languageIn, Boolean smallTest, Boolean testCandidateRetrieval, Boolean addResultInfoForCR, int maxMockSuspCandidates){
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
                    if(testCandidateRetrieval){
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
                if(addResultInfoForCR) {
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
            try {
                doCREvaluationRecallFragmentsSalvador(params, tag, "CREvalRecall", resultSelectedCandidates, plagiarismInformation);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return;
        }
    }

    static Map<String, List<String>> simplifyEntitiesMap(Map<String, List<SavedEntity>> extendedEntitiesMap )   {
        return extendedEntitiesMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList())));
    }

    static Map<String, List<String>> simplifyEntitiesMapSalvador(Map<SalvadorTextFragment, List<SavedEntity>> extendedEntitiesMap )   {
        return extendedEntitiesMap.entrySet().stream()
                .collect(Collectors.toMap(entry-> entry.getKey().getFragmentID() , entry -> entry.getValue().stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList())));
    }

    static void doCREvaluationRecallFragmentsSalvador(ExtendedAnalysisParameters params, String tag, String comment,
                                     HashMap<String, List<String>> resultSelectedCandidates,
                                      HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation) throws Exception {
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory;

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);

        logUtil.logAndWriteStandard(false, "Assuming the preprpocessing has been done here. ");
        params.MAX_NUM_CANDIDATES_SELECTED = 50000;
        //params.USE_LANGUAGE_WHITELISTING = true;
        //params.USE_FILE_FILTER = false;
        //params.panFileFilter.addLanguageToWhitelist("en", "es");

        //params.CANDIDATE_SELECTION_TRESH = 0;
        logParams(logUtil, tag, params, osa);

        // Print the current parametrization for Salvador
        SalvadorAnalysisParameters.printSalvadorMembers(logUtil);

        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");



        // Create a list of candidate fragments (all)
        Map<String, List<SavedEntity>> candidateEntitiesFragment = getFragments(osa, candidateFiles, SalvadorAnalysisParameters.FRAGMENT_SENTENCES, SalvadorAnalysisParameters.FRAGMENT_INCREMENT, false, null, true);

        // For testing use just one basic file (and also just the corresponding results)

        suspiciousFiles = filterBySuspFileLimit(plagiarismInformation, suspiciousFiles, SalvadorAnalysisParameters.DO_FILE_PREFILTERING, SalvadorAnalysisParameters.SUSP_FILE_LIMIT, SalvadorAnalysisParameters.SUSP_FILE_SELECTION_OFFSET);

        System.out.println("My First SuspFile: "+ suspiciousFiles.get(0).toString());
        System.out.println("Suspfile Count: "+ suspiciousFiles.size());
        if(!SalvadorAnalysisParameters.DO_BATCHED_PROCESSING){
            // Just calculate all files at once
            logUtil.logAndWriteStandard(true,"BATCHED_PROCESSING:", "is deactivated, just calculating all files in one step");
            doScoresMapIteration(tag, plagiarismInformation, osa, logUtil, candidateFiles, suspiciousFiles, candidateEntitiesFragment, SalvadorAnalysisParameters.SUSP_FILE_SELECTION_OFFSET, SalvadorAnalysisParameters.SUSP_FILE_LIMIT);
        }else{
            int batchCounter = 0;
            // Do calculating in batches
            for(int batchIndex = 0; batchIndex < suspiciousFiles.size(); batchIndex+=SalvadorAnalysisParameters.BATCHED_OFFSET_INCREMENT){
                int maxBatchIndex = min(suspiciousFiles.size(), (batchIndex+SalvadorAnalysisParameters.BATCHED_OFFSET_INCREMENT));
                if(maxBatchIndex < batchIndex) break;
                int overallIndex = SalvadorAnalysisParameters.SUSP_FILE_SELECTION_OFFSET + batchIndex;
                List<File> currentSuspiciousFiles = suspiciousFiles.subList(batchIndex,maxBatchIndex);
                logUtil.logAndWriteStandard(true,"BATCHED_PROCESSING:", "Doing batch from " + batchIndex + " to " + maxBatchIndex);

                doScoresMapIteration(tag, plagiarismInformation, osa, logUtil, candidateFiles, currentSuspiciousFiles, candidateEntitiesFragment, overallIndex, currentSuspiciousFiles.size());
                batchCounter++;
            }
            logUtil.logAndWriteStandard(true, "BATCHED_PROCESSING:", "Done with "+batchCounter+" batche/s." );
        }

    }

    private static void doScoresMapIteration(String tag, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, OntologyBasedSimilarityAnalysis osa, ExtendedLogUtil logUtil, List<File> candidateFiles, List<File> suspiciousFiles, Map<String, List<SavedEntity>> candidateEntitiesFragment, int filesOffset, int filesNumber) throws Exception {
        Map<String, List<SavedEntity>> suspiciousEntitiesFragment;
        // Create a list of suspicious fragments (only plagiarism involved fragments)

        if(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS){
            // Get fragments exactly the plagiarism size
            suspiciousEntitiesFragment = getPlagsizedFragments(osa, suspiciousFiles, plagiarismInformation, false);

        }else{
            // Get fragments with plagiarism involved, size of FRAGMENT_SENTENCES
            suspiciousEntitiesFragment = getFragments(osa, suspiciousFiles, SalvadorAnalysisParameters.FRAGMENT_SENTENCES, SalvadorAnalysisParameters.FRAGMENT_INCREMENT, true, plagiarismInformation, false);
        }

        System.out.println("Susp-enties count: "+ suspiciousEntitiesFragment.size());


        if(SalvadorAnalysisParameters.DO_STATISTICAL_WEIGHTING){
            // Required:
            // Corpus (Susp / Candidate) Level occurences per entitiy
            // Document Level Occurences

            // Definition TF/IDF
            // TF(t) = (Number of times term t appears in a document) / (Total number of terms in the document).
            // IDF(t) = log_e(Total number of documents / Number of documents with term t in it).
            // Getting fragments, but without overlap TBD: mind susp pre-selection somehow.
            tfidfMapHolder.createRelevantMaps(plagiarismInformation, osa, candidateFiles, suspiciousFiles);
            tfidfMapHolder.calculateWeightingScores();

            // combining
            /* tbd sort by occurences
            Map<String, tfidfTokenInfo> sortedTermsByOccurence =
                    tfCandFragmentEntities.entrySet().stream()
                            .sorted(Map.Entry::comparingByValue::(tfidfTokenInfo h1, tfidfTokenInfo h2) -> !(h1.getOccurences() <= h2.getOccurences()))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            */
            System.out.println("reacher");
        }
        // For testing take a smaller suspicious map and the corresponding results.
        // boolean DO_OBSOLETE_PREFILTERING = false; // usually file filter above is more practical
        // suspiciousEntitiesFragment = obsoletePreselectionFilter(plagiarismInformation, suspiciousEntitiesFragment, DO_OBSOLETE_PREFILTERING, 2);


        // Get the scoring (wrapped by caching block)
        final Map<String, Map<String, Double>>  scoresMap;
        if(SalvadorAnalysisParameters.DO_SCORES_MAP_CACHING){
            ScoresMapCache scoresMapCache = new ScoresMapCache();
            // Generate key on base of used parameters
            String keyPath = scoresMapCache.generateFileKey(preprocessedCachingDir+"/scoresmap_serialization/",SalvadorAnalysisParameters.FRAGMENT_SENTENCES,SalvadorAnalysisParameters.FRAGMENT_INCREMENT,SalvadorAnalysisParameters.USE_ABSOLUTE_SCORES, SalvadorAnalysisParameters.DO_FILE_PREFILTERING, filesNumber, SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, filesOffset);
            // Try to find a file
            Map<String, Map<String, Double>>  scoresMapDes = scoresMapCache.deserializeScoresMap(keyPath);
            if(scoresMapDes==null){
                scoresMap = osa.performCosineSimilarityAnalysis(simplifyEntitiesMap(suspiciousEntitiesFragment), simplifyEntitiesMap(candidateEntitiesFragment), SalvadorAnalysisParameters.USE_ABSOLUTE_SCORES, SalvadorAnalysisParameters.DO_STATISTICAL_WEIGHTING);
                scoresMapCache.serializeScoresMap(keyPath, scoresMap);
            }else{
                scoresMap = scoresMapDes;
            }
        }else{
            scoresMap = osa.performCosineSimilarityAnalysis(simplifyEntitiesMap(suspiciousEntitiesFragment), simplifyEntitiesMap(candidateEntitiesFragment), SalvadorAnalysisParameters.USE_ABSOLUTE_SCORES, SalvadorAnalysisParameters.DO_STATISTICAL_WEIGHTING);
        }

        if(SalvadorAnalysisParameters.DO_REGRESSION_ANALYSIS){
            // scoresMap -> entities -> results
            // suspicious is always plagiarism
            // my candidate fragment is 200/2000 characters plagiasm
            // linearer score für plagiarism
            // linearized , feature1 , feature2, ....
            // Oberservartion(lineerize, scoreAbsolute, scoreNormalized, scores)
        }


        // Calculate the recall for the scores map (character based)
        // Experimental parameters
        boolean relativeOverallScores = SalvadorAnalysisParameters.DO_RELATIVE_SCORING_R_AT_K; // Default: false
        int minsizePlagfragments = SalvadorAnalysisParameters.MIN_FRAGMENT_SIZE_R_AT_K; // for filtering irrelevant edge cases, Default: 0
        Double recallAt1 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS,relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,  minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,1);
        Double recallAt5 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS,relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,5);
        Double recallAt10 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS,relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,10);
        Double recallAt20 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,20);
        Double recallAt50 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,50);
        Double recallAt100 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS,relativeOverallScores,SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,100);
        Double recallAt200 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores,SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments,  scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,200);
        Double recallAt25k = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,25000);
        Double recallAt10B = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K,  SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K,minsizePlagfragments,  scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil,10000000);

        // DA implementation:

        // Create a document fragment map for (SuspFragments/CandFragments)
        Map<String, List<String>> suspDocFragmentMap = getDocumentFragmentMap(suspiciousEntitiesFragment);
        Map<String, List<String>> candDocFragmentMap = getDocumentFragmentMap(candidateEntitiesFragment);


        // Detailed Comparison ...

        // TBD: Do document preselection (not clear atm, which documents)
        // Variations:
        //- all documents
        //- documents containing the k-next candidates
        //- documents containing plagiarism
        // Compare each preselected suspicious document to candidate document.
        Map<String, Map<String, Map<SalvadorTextFragment, SalvadorTextFragment>>> allResults = new HashMap<>();
        Map<String, Map<String, SalvadorStatisticsInfo>> allStatistics = new HashMap<>();


        suspDocFragmentMap.keySet().parallelStream().forEach(suspiciousDocument -> {
            Map<String, Map<SalvadorTextFragment, SalvadorTextFragment>> supFilePlagiarism = new HashMap<>();
            Map<String, SalvadorStatisticsInfo> suspDocumentStats = new HashMap<>();
            List<PAN11PlagiarismInfo> relatedPlagiarismInfo = null;
            if(SalvadorAnalysisParameters.DO_ANALYSIS) {
                relatedPlagiarismInfo = plagiarismInformation.get(suspiciousDocument.replace(".txt", ".xml"));
            }
            for(String candidateDocument:candDocFragmentMap.keySet()){
                List<PAN11PlagiarismInfo> relatedPlagiarismInfoCandFiltered = null;
                if(SalvadorAnalysisParameters.DO_ANALYSIS){
                    relatedPlagiarismInfoCandFiltered = relatedPlagiarismInfo.stream().filter( value -> {
                        return value.getSourceReference().equals(candidateDocument.replace("candidate-", "source-"));
                    }).collect(Collectors.toList());
                }

                // Calculate DA-Clustering for current file combination.
                SalvadorDetailedAnalysisResult daResult = doDetailedAnalysis(
                        suspDocFragmentMap.get(suspiciousDocument),
                        candDocFragmentMap.get(candidateDocument),
                        suspiciousEntitiesFragment,
                        candidateEntitiesFragment,
                        scoresMap,
                        SalvadorAnalysisParameters.THRESH1,
                        SalvadorAnalysisParameters.THRESH2,
                        SalvadorAnalysisParameters.TOPMOST,
                        SalvadorAnalysisParameters.DO_ANALYSIS,
                        relatedPlagiarismInfoCandFiltered,
                        logUtil
                );
                Map<SalvadorTextFragment, SalvadorTextFragment> detailedAnalysisResultsD2D = daResult.resultMap;
                if(SalvadorAnalysisParameters.DO_ANALYSIS){
                    if(daResult.salvadorStatisticsInfo!=null){
                        suspDocumentStats.put(candidateDocument, daResult.salvadorStatisticsInfo);
                    }
                }
                if(detailedAnalysisResultsD2D.size() >= 1){
                    Map<SalvadorTextFragment, SalvadorTextFragment> currentMap = supFilePlagiarism.get(candidateDocument);
                    if(currentMap==null) {
                        supFilePlagiarism.put(candidateDocument, detailedAnalysisResultsD2D);
                    }else{
                        // TBD validate if ok
                        currentMap.putAll(detailedAnalysisResultsD2D);
                    }
                    // Note results to

                }
            }

            allResults.put(suspiciousDocument, supFilePlagiarism);
            if(SalvadorAnalysisParameters.DO_ANALYSIS){
                allStatistics.put(suspiciousDocument, suspDocumentStats);
            }
        });

        if(SalvadorAnalysisParameters.DO_ANALYSIS){
            SalvadorStatisticsInfo salvadorStatisticsInfoAllCombined = SalvadorExtendedAnalytics.createCombinedStatistics(allStatistics);
            logUtil.logAndWriteStandard(false, "All Statistics (scoring selects only candidate files plagiarism:",SalvadorAnalysisParameters.ONLY_PLAGFILES_IN_STATS,")");

            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.overallInfoPositives,"overallPositives");
            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.overallInfoNegatives,"overallNegatives");
            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.mergedInfoPositives,"mergedPositives");
            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.mergedInfoNegatives,"mergedNegatives");
        }

        // Write down all xml Results
        String xmlResultsFolderPath = SalvadorPAN11XMLwriter.writeDownAllXMLResults(tag, logUtil.getDateString(), preprocessedCachingDir,allResults);
        // Do evaluation with the current set filters
        //String baseResultsPath = "/data/CLOSA_data/preprocessed/preprocessed_extended/results_comparison/evalPAN2011Salvador"; // TODO adapt
        File cachingDir= new File(xmlResultsFolderPath +"/file_selection_cache");
        // Remove previous caching directory.
        PAN11FileUtil.removeDirectory(cachingDir);
        List<File> suspiciousXML  =  suspiciousFiles.stream().map(file -> new File(file.getAbsolutePath().replace(".txt",".xml"))).collect(Collectors.toList()); //PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
        PAN11FileUtil.writeFileListToDirectory(suspiciousXML, cachingDir.getPath(), logUtil);
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath());
        PAN11FileUtil.removeDirectory(cachingDir);
    }


    public static void printStatisticsInfo(ExtendedLogUtil logUtil, SalvadorInfoHolder salvadorInfoHolder, String name) {
        logUtil.logAndWriteStandard(false, "Printing Statistics Infos for", name+"----------------");
        logUtil.logAndWriteStandard(true, "plagiarizedAreaPossible:", salvadorInfoHolder.possiblePlagiarizedArea);
        logUtil.logAndWriteStandard(true, "numFindings:", salvadorInfoHolder.numFindings);
        logUtil.logAndWriteStandard(true, "max(average):", salvadorInfoHolder.max);
        logUtil.logAndWriteStandard(true, "mean:", salvadorInfoHolder.mean);
        logUtil.logAndWriteStandard(true,"min(average):", salvadorInfoHolder.min);
        logUtil.logAndWriteStandard(true, "---");

    }

    public static SalvadorDetailedAnalysisResult doDetailedAnalysis(List<String> suspiciousFragments,
                                                                                      List<String> candidateFragments,
                                          Map<String, List<SavedEntity>> suspiciousEntitiesFragment,
                                          Map<String, List<SavedEntity>> candidateEntitiesFragment,
                                          Map<String, Map<String, Double>>  scoresMap,
                                          int THRESHOLD_1,
                                          double THRESHOLD_2,
                                          int RANKLIMIT,
                                          boolean DO_ANALYSIS,
                                          List<PAN11PlagiarismInfo> candidatePlagiarismInfos,
                                          ExtendedLogUtil logUtil) {

        double THRESH_TOPMOST = SalvadorAnalysisParameters.PRESELECTION_THRESH;

        // Get selected suspicious fragments from results
        Map<String, Map<String, Double>> scoresMapSelected = new HashMap<>(scoresMap);
        scoresMapSelected.keySet().retainAll(suspiciousFragments);
        Map<SalvadorTextFragment, SalvadorTextFragment> fragmentInfosSelected = new ArrayMap<>();
        // Analysis related stuff
        Map<SalvadorTextFragment , Map<SalvadorTextFragment, Integer>> fragmentInfosAll = new ArrayMap<>();
        Map<SalvadorTextFragment , Map<SalvadorTextFragment, Integer>> fragmentInfosAllmerged = new ArrayMap<>();




        for(String suspiciousFragmentID:scoresMapSelected.keySet()){
            SalvadorTextFragment suspiciousFragment = PAN11RankingEvaluator.createTextFragment(suspiciousEntitiesFragment.get(suspiciousFragmentID), suspiciousFragmentID);
            // Get selected candidate fragments from results
           Map<String, Double> candidateScores = new HashMap<>(scoresMapSelected.get(suspiciousFragmentID));
            candidateScores.keySet().retainAll(candidateFragments);

           // Get best scoring <RANKLIMIT> fragments
            Map<String, Double> candidateScoresMapSelected = candidateScores.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(RANKLIMIT)
                    .filter(value -> value.getValue() >= THRESH_TOPMOST)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            // Get the start stop coordinates for the fragments.
            Map<String, SalvadorTextFragment> fragmentInfos = new ArrayMap<>();
            for(String candidateFragmentID: candidateScoresMapSelected.keySet()){
                SalvadorTextFragment fragmentToAdd =  PAN11RankingEvaluator.createTextFragment(candidateEntitiesFragment.get(candidateFragmentID),candidateFragmentID);
                fragmentToAdd.setComputedScore(candidateScoresMapSelected.get(candidateFragmentID));

                fragmentInfos.put(candidateFragmentID, fragmentToAdd);
            }

            // Merge the fragments
            Map<String, SalvadorTextFragment>  fragmentInfosMerged = mergeFragments(THRESHOLD_1, fragmentInfos);


            // Rate the new fragment infos as plagiarism or not
            for(String clusteredFragmentID: fragmentInfosMerged.keySet()){
                SalvadorTextFragment clusteredFragment = fragmentInfosMerged.get(clusteredFragmentID);
                //System.out.println(suspiciousFragmentID+"/"+candidateFragments.get(0)+":"+clusteredFragment.getComputedScore());
                if(clusteredFragment.getComputedScore() > THRESHOLD_2){
                    fragmentInfosSelected.put(suspiciousFragment, clusteredFragment);
                }
            }

            // Additional optional analysis steps
            if(DO_ANALYSIS){
                // Get related plagiarismInfo to the current node
                List<PAN11PlagiarismInfo> nodePlagiarismInfos = new ArrayList<>();
                for(PAN11PlagiarismInfo plagiarismInfo:candidatePlagiarismInfos){
                    int plagiarizedArea = getPlagiarizedArea(suspiciousFragment.getSentencesStartChar(),suspiciousFragment.getSentencesEndChar(),plagiarismInfo.getThisOffset(),plagiarismInfo.getThisOffset()+plagiarismInfo.getThisLength());
                    if(plagiarizedArea > 0){
                        nodePlagiarismInfos.add(plagiarismInfo);
                    }
                }
                for(String clusteredFragmentID: fragmentInfos.keySet()){
                    SalvadorTextFragment fragment = fragmentInfos.get(clusteredFragmentID);
                    int plagiarizedArea = getPlagiarismAreaAccumulated(fragment.getSentencesStartChar(), fragment.getSentencesEndChar(),nodePlagiarismInfos,true);
                    Map<SalvadorTextFragment, Integer> currentMap = fragmentInfosAll.get(suspiciousFragment);
                    if(currentMap==null){
                        currentMap = new ArrayMap<>();
                        fragmentInfosAll.put(suspiciousFragment, currentMap);
                    }
                    fragmentInfosAll.get(suspiciousFragment).put(fragment, plagiarizedArea);
                }
                for(String clusteredFragmentID: fragmentInfosMerged.keySet()){
                    SalvadorTextFragment fragment = fragmentInfosMerged.get(clusteredFragmentID);
                    int plagiarizedArea =  getPlagiarismAreaAccumulated(fragment.getSentencesStartChar(), fragment.getSentencesEndChar(),nodePlagiarismInfos,true);

                    Map<SalvadorTextFragment, Integer> currentMap = fragmentInfosAllmerged.get(suspiciousFragment);
                    if(currentMap==null){
                        currentMap = new ArrayMap<>();
                        fragmentInfosAllmerged.put(suspiciousFragment, currentMap);
                    }
                    fragmentInfosAllmerged.get(suspiciousFragment).put(fragment, plagiarizedArea);
                }
            }

        }
        SalvadorDetailedAnalysisResult myResult = new SalvadorDetailedAnalysisResult();
        myResult.resultMap = fragmentInfosSelected;
        // Holder for analysis stuff
        if(DO_ANALYSIS){
            if(!SalvadorAnalysisParameters.ONLY_PLAGFILES_IN_STATS || candidatePlagiarismInfos.size()>0){
                // myStatisticsD2D is filled by reference by both functions
                SalvadorStatisticsInfo myStatisticsD2D = new SalvadorStatisticsInfo();
                getStats(fragmentInfosAll, true, myStatisticsD2D, "Overall", logUtil);
                getStats(fragmentInfosAllmerged, false, myStatisticsD2D, "Merged", logUtil);
                myResult.salvadorStatisticsInfo = myStatisticsD2D;
            }
        }
        return myResult;
    }
    private static void getStats(Map<SalvadorTextFragment , Map<SalvadorTextFragment, Integer>> currentMap, boolean overallOrMerge, SalvadorStatisticsInfo statisticsInfo, String name, ExtendedLogUtil logUtil){
        List<Double> scoresPositve = new ArrayList<>();
        List<Double> scoresNegative = new ArrayList<>();
        long overallCharLengthPositive = 0;
        long overallCharLengthNegative = 0;
        int possiblePlagiarizedArea = 0;
        for(SalvadorTextFragment suspiciousTextFragment: currentMap.keySet()){
            Map<SalvadorTextFragment, Integer> relatedDetections = currentMap.get(suspiciousTextFragment);
            for(SalvadorTextFragment candidateTextFragment:relatedDetections.keySet()){
                int plagiarizedArea = relatedDetections.get(candidateTextFragment);
                double relevance = plagiarizedArea / (double)candidateTextFragment.getCharLengthBySentences();
                double score = candidateTextFragment.getComputedScore();
                if(relevance > SalvadorAnalysisParameters.ANALYSIS_RELEVANCE_THRESH){
                    possiblePlagiarizedArea += plagiarizedArea;
                    scoresPositve.add(score);
                    overallCharLengthPositive += candidateTextFragment.getCharLengthBySentences();

                }else{
                    scoresNegative.add(score);
                    overallCharLengthNegative += candidateTextFragment.getCharLengthBySentences();
                }
            }
        }
        OptionalDouble meanPositiveOpt = scoresPositve.stream().mapToDouble(a -> a).average();
        double meanPositive  = -1.0;
        if(meanPositiveOpt.isPresent()){
            meanPositive = meanPositiveOpt.getAsDouble();
        }
        OptionalDouble maxPositiveOpt = scoresPositve.stream().mapToDouble(Double::doubleValue).max();
        double maxPositive = -1.0;
        if(maxPositiveOpt.isPresent()){
            maxPositive = maxPositiveOpt.getAsDouble();
        }



        double minPositive =  -1.0;
        OptionalDouble minPositiveOpt = scoresPositve.stream().mapToDouble(Double::doubleValue).min();
        if(minPositiveOpt.isPresent()){
            minPositive = minPositiveOpt.getAsDouble();
        }
        // Probably use the InfoHolders to create overall stats

        OptionalDouble meanNegativeOpt = scoresNegative.stream().mapToDouble(a -> a).average();
        double meanNegative  = -1.0;
        if(meanNegativeOpt.isPresent()){
            meanNegative = meanNegativeOpt.getAsDouble();
        }

        OptionalDouble maxNegativeOpt = scoresNegative.stream().mapToDouble(Double::doubleValue).max();
        double maxNegative = -1.0;
        if(maxNegativeOpt.isPresent()){
            maxNegative = maxNegativeOpt.getAsDouble();
        }
        double minNegative = -1.0;
        OptionalDouble minNegativeOpt = scoresNegative.stream().mapToDouble(Double::doubleValue).min();
        if(minNegativeOpt.isPresent()){
            minNegative = minNegativeOpt.getAsDouble();
        }
        boolean printMe = false;

        // Add stuffs to info holder
        SalvadorInfoHolder salvadorInfoHolderPositives = new SalvadorInfoHolder();
        SalvadorInfoHolder salvadorInfoHolderNegatives = new SalvadorInfoHolder();
        salvadorInfoHolderPositives.possiblePlagiarizedArea = possiblePlagiarizedArea;
        salvadorInfoHolderPositives.numFindings = scoresPositve.size();
        salvadorInfoHolderPositives.max = maxPositive;
        salvadorInfoHolderPositives.min = minPositive;
        salvadorInfoHolderPositives.mean = meanPositive;
        salvadorInfoHolderPositives.sizeChars = overallCharLengthPositive;

        salvadorInfoHolderNegatives.numFindings = scoresNegative.size();
        salvadorInfoHolderNegatives.max = maxNegative;
        salvadorInfoHolderNegatives.min = minNegative;
        salvadorInfoHolderNegatives.mean = meanNegative;
        salvadorInfoHolderPositives.sizeChars = overallCharLengthNegative;
        if(printMe) {
            printStatisticsInfo(logUtil,salvadorInfoHolderPositives, name+"Positives");
            printStatisticsInfo(logUtil,salvadorInfoHolderNegatives, name+"Negatives");

        }
        if(overallOrMerge){
            statisticsInfo.overallInfoPositives = salvadorInfoHolderPositives;
            statisticsInfo.overallInfoNegatives = salvadorInfoHolderNegatives;

        }else{
            statisticsInfo.mergedInfoPositives = salvadorInfoHolderPositives;
            statisticsInfo.mergedInfoNegatives = salvadorInfoHolderNegatives;
        }

    }
    @NotNull
    private static Map<String, SalvadorTextFragment> mergeFragments(int THRESHOLD_1, Map<String, SalvadorTextFragment> fragmentInfos) {
        // Merge the 5 fragments with each other if they are near in distance (THRESHOLD_1)
        boolean convergent = false;
        int convergenceIndex=0;
        while (!convergent){
            boolean mergeHappened = false;
            Map<String, SalvadorTextFragment> newFragmentInfos = new ArrayMap<>(fragmentInfos);


            int outerIndex = 0;
            for(String fragmentIDSelected1: fragmentInfos.keySet()){
                SalvadorTextFragment fragment1 = fragmentInfos.get(fragmentIDSelected1);

                int innerIndex = 0;
                for(String fragmentIDSelected2: fragmentInfos.keySet()){
                    // Only compare same IDs once
                    if(innerIndex<=outerIndex){
                        innerIndex++;
                        continue;
                    }

                    SalvadorTextFragment fragment2 = fragmentInfos.get(fragmentIDSelected2);
                    double distance = calculateDistanceFragments(fragment1, fragment2);
                    if(distance < THRESHOLD_1){
                        //System.out.println("Merge fragments");
                        String mergeFragmentID = "merge"+outerIndex+"_"+innerIndex+"_"+convergenceIndex;
                        SalvadorTextFragment mergedFragment = mergeTextFragments(fragment1, fragment2, mergeFragmentID);
                        // Remove IDs from unmerged List
                        newFragmentInfos.remove(fragmentIDSelected1);
                        newFragmentInfos.remove(fragmentIDSelected2);
                        newFragmentInfos.put(mergeFragmentID, mergedFragment);
                        mergeHappened = true;
                        if(mergeHappened) break;
                    }
                    if(mergeHappened)break;
                    innerIndex++;
                }
                outerIndex++;
            }

            fragmentInfos = newFragmentInfos;
            if(!mergeHappened) {
                convergent = true;
            }
            convergenceIndex++;
        }
        return fragmentInfos;
    }

    public static SalvadorTextFragment mergeTextFragments(SalvadorTextFragment fragment1, SalvadorTextFragment fragment2, String mergeFragmentID){

        SalvadorTextFragment mergedFragment = new SalvadorTextFragment();
        mergedFragment.setMerged(true);
        mergedFragment.setSentencesStartChar(min(fragment1.getSentencesStartChar(),fragment2.getSentencesStartChar()));
        mergedFragment.setSentencesEndChar(max(fragment1.getSentencesEndChar(),fragment2.getSentencesEndChar()));
        mergedFragment.setCharLengthBySentences(mergedFragment.getSentencesEndChar()-mergedFragment.getSentencesStartChar());
        mergedFragment.setFragmentID(mergeFragmentID);

        // Merge Score (TBD: Score eval)
        String MODE = SalvadorAnalysisParameters.FRAGMENT_MERGE_MODE; //"weightedAdd";
        boolean someModeFound = false;
        if(MODE.equals("weightedAdd")) {
            // Weighting the scores by character lengths
            double lengthAdded = fragment1.getCharLengthBySentences() + fragment2.getCharLengthBySentences();
            double weightingConstant = SalvadorAnalysisParameters.WEIGHTED_ADD_CONSTANT;
            int padding = 10000; // This padding shall prevent below 1 multiplication
            double factor1 = weightingConstant + (fragment1.getCharLengthBySentences() / lengthAdded);
            double factor2 = weightingConstant + (fragment2.getCharLengthBySentences() / lengthAdded);
            double preMergedScore = (factor1 * (fragment1.getComputedScore() * padding)) + (factor2 * (fragment2.getComputedScore() * padding));
            Double mergedScore = preMergedScore / padding;
            mergedFragment.setComputedScore(mergedScore);
            someModeFound = true;
        }
        if(MODE.equals("simpleAdd")){
            // Just adding up the scores
            Double fragmentScore = fragment1.getComputedScore() + fragment2.getComputedScore();
            mergedFragment.setComputedScore(fragmentScore);
            someModeFound = true;
        }
        if(MODE.equals("weightedAverage")){
            // Weighting the scores by character lengths
            double lengthAdded = fragment1.getCharLengthBySentences() + fragment2.getCharLengthBySentences();

            int padding = 10000; // This padding shall prevent below 1 multiplication
            double factor1 = (fragment1.getCharLengthBySentences() / lengthAdded);
            double factor2 = (fragment2.getCharLengthBySentences() / lengthAdded);
            double preMergedScore = (factor1 * (fragment1.getComputedScore() * padding)) + (factor2 * (fragment2.getComputedScore() * padding));
            Double mergedScore = preMergedScore / padding;
            mergedFragment.setComputedScore(mergedScore);
            someModeFound = true;

        }
        if(MODE.equals("keepingMax")){
            // Keeping the maximum of merged fragments as a score
            mergedFragment.setComputedScore(Double.max(fragment1.getComputedScore(),fragment2.getComputedScore()));
            someModeFound = true;
        }
        if(!someModeFound){
            System.err.println("INCORRECT FRAGMENT_MERGE_MODE IN CLUSTERING SETTINGS: "+MODE);
        }
        // Add additional info
        /*
        List<String> fragmentIDs = new ArrayList<>();
        if(fragment1.getMergedIDs()!=null){
            fragmentIDs.addAll(fragment1.getMergedIDs());
        }
        if(fragment2.getMergedIDs()!=null){
            fragmentIDs.addAll(fragment2.getMergedIDs());
        }
        if(!fragmentIDs.contains(fragment1.getFragmentID())){
            fragmentIDs.add(fragment1.getFragmentID());
        }
        if(!fragmentIDs.contains(fragment2.getFragmentID())){
            fragmentIDs.add(fragment2.getFragmentID());
        }
        mergedFragment.setMergedIDs(fragmentIDs);
        */
        return mergedFragment;
    }

    public static int calculateDistanceFragments(SalvadorTextFragment fragment1, SalvadorTextFragment fragment2){
        int f1start = fragment1.getSentencesStartChar();
        int f1end = fragment1.getSentencesEndChar();
        int f2start = fragment2.getSentencesStartChar();
        int f2end = fragment2.getSentencesEndChar();
        int distance = 0;
        if(f1start < f2start){
            // Fragment one is first
            distance = f2start - f1end;
        }else{
            // Fragment two is first
            distance = f1start - f2end;
        }
         // TODO fix and check this
        //int d1 = fragment2.getSentencesStartChar()-fragment1.getSentencesEndChar();
        //int d2 = fragment1.getSentencesStartChar()-fragment2.getSentencesEndChar();
        //int distance = min(d1,d2);
        return distance;
    }
    /**
     * Suspicious or Candidate entityFragment Map,
     * fragmentIDs get mapped to corresponding document IDs
     * @param fragmentEntityMap
     * @return
     */
    private static Map<String, List<String>>  getDocumentFragmentMap(Map<String, List<SavedEntity>> fragmentEntityMap) {
        Map<String, List<String>> documentFragmentsMap = new ArrayMap<>();
        for(String fragmentID: fragmentEntityMap.keySet()){
            String baseName = PAN11RankingEvaluator.getBaseName(fragmentID,".txt");
            List<String> fragmentIDs = documentFragmentsMap.get(baseName);
            if(fragmentIDs == null){
                fragmentIDs = new ArrayList<>();
                fragmentIDs.add(fragmentID);
                documentFragmentsMap.put(baseName, fragmentIDs);
                continue;
            }
            fragmentIDs.add(fragmentID);

        }
        return documentFragmentsMap;
    }

    private static List<File> filterBySuspFileLimit(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, List<File> suspiciousFiles, boolean DO_FILE_PREFILTERING, int SUSP_FILE_LIMIT, int SUSP_FILE_SELECTION_OFFSET) {
        if(DO_FILE_PREFILTERING) {
            suspiciousFiles = suspiciousFiles.stream().sorted().skip(SUSP_FILE_SELECTION_OFFSET).limit(SUSP_FILE_LIMIT).collect(Collectors.toList());// Just take one basic file.
            List<String> usedPlagiarismInfos  = suspiciousFiles.stream().map(entry->entry.getName().replace(".txt",".xml")).collect(Collectors.toList());
            plagiarismInformation.keySet().retainAll(usedPlagiarismInfos);
        }
        return suspiciousFiles;
    }

    private static Map<String, List<SavedEntity>> obsoletePreselectionFilter(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, Map<String, List<SavedEntity>> suspiciousEntitiesFragment, boolean DO_PREFILTERING, int limitCorpus) {
        if(DO_PREFILTERING) {
            suspiciousEntitiesFragment = suspiciousEntitiesFragment.entrySet().stream().limit(limitCorpus).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            List<String> usedPlagiarismInfos = new ArrayList<>();
            for (String suspFragmentID : suspiciousEntitiesFragment.keySet()) {
                String relatedInfoName = PAN11RankingEvaluator.getBaseName(suspFragmentID, ".xml");
                usedPlagiarismInfos.add(relatedInfoName);
            }
            List<String> usedPlagiarismInfosDedup = new ArrayList<String>(new HashSet<String>(usedPlagiarismInfos));
            plagiarismInformation.keySet().retainAll(usedPlagiarismInfosDedup);
        }
        return suspiciousEntitiesFragment;
    }

    public static boolean isEntityRelatedToPlagiarism(int entityStart, int entityEnd, int plagiarismStart, int plagiarismEnd){
        int plagiarizedArea = getPlagiarizedArea(entityStart, entityEnd, plagiarismStart, plagiarismEnd);
        if(plagiarizedArea > 0){
            return true;
        }
        return false;
    }
    public static int getPlagiarizedArea(int entityStart, int entityEnd, int plagiarismStart, int plagiarismEnd){
        // similar to isWindowRelatedToPlagiarism in OntologyBasedSimilarityAnalysis
        // TODO maybe require minimum overlap otherwise prob

        // Overlap entity starts before plagiarism
        if(entityStart >= plagiarismStart && entityStart < plagiarismEnd ){
            return min(plagiarismEnd, entityEnd)-entityStart;
        }
        // Overlap entity starts before plagiarism
        if(entityEnd > plagiarismStart && entityEnd <= plagiarismEnd ){
            return entityEnd-max(entityStart, plagiarismStart);
        }

        // plagiarism is within entity
        if(plagiarismStart  >= entityStart && plagiarismEnd <= entityEnd){
            return plagiarismEnd-plagiarismStart;
        }

        return 0;
    }
    public static boolean isPlagiarismRelated(int startCharacterEntity, int endCharacterEntity, List<PAN11PlagiarismInfo> currentPlagiarismInfos, Boolean candOrSusp){
        // Assuming Suspicious File here.
        for(PAN11PlagiarismInfo currentPlagiarismInfo:currentPlagiarismInfos){
            boolean isPlagiarism = false;
            if(candOrSusp){
                // for candidate files
                int startCharacterPlagiarism = currentPlagiarismInfo.getSourceOffset();
                int endCharacterPlagiarism = startCharacterPlagiarism + currentPlagiarismInfo.getSourceLength();
                isPlagiarism = isEntityRelatedToPlagiarism(startCharacterEntity, endCharacterEntity, startCharacterPlagiarism, endCharacterPlagiarism);
            }else{
                // for suspicious files
                int startCharacterPlagiarism = currentPlagiarismInfo.getThisOffset();
                int endCharacterPlagiarism = startCharacterPlagiarism + currentPlagiarismInfo.getThisLength();
                isPlagiarism = isEntityRelatedToPlagiarism(startCharacterEntity,endCharacterEntity, startCharacterPlagiarism, endCharacterPlagiarism);

            }
            if(isPlagiarism){
                return true;
            }
        }
        return false;
    }

    private static int getPlagiarismAreaAccumulated(int startCharacterEntity, int endCharacterEntity, List<PAN11PlagiarismInfo> currentPlagiarismInfos, Boolean candOrSusp){
        int completePlagiarizedArea = 0;
        // Assuming Suspicious File here.
        for(PAN11PlagiarismInfo currentPlagiarismInfo:currentPlagiarismInfos){
            boolean isPlagiarism = false;
            if(candOrSusp){
                // for candidate files
                int startCharacterPlagiarism = currentPlagiarismInfo.getSourceOffset();
                int endCharacterPlagiarism = startCharacterPlagiarism + currentPlagiarismInfo.getSourceLength();
                int plagiarizedArea = getPlagiarizedArea(startCharacterEntity, endCharacterEntity, startCharacterPlagiarism, endCharacterPlagiarism);
                completePlagiarizedArea += plagiarizedArea;
            }else{
                // for suspicious files
                int startCharacterPlagiarism = currentPlagiarismInfo.getThisOffset();
                int endCharacterPlagiarism = startCharacterPlagiarism + currentPlagiarismInfo.getThisLength();
                int plagiarizedArea = getPlagiarizedArea(startCharacterEntity, endCharacterEntity, startCharacterPlagiarism, endCharacterPlagiarism);
                completePlagiarizedArea += plagiarizedArea;
            }
        }
        return completePlagiarizedArea;
    }

    private static Map<String, List<SavedEntity>> getPlagsizedFragments(OntologyBasedSimilarityAnalysis osa, List<File> inputFiles,
                                                                HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation,
                                                                 boolean candOrSusp) throws Exception {
        Map<String, List<SavedEntity>> entitiesMap = new HashMap<>();
        for (File currentFile: inputFiles) {
            List<SavedEntity> savedEntities = osa.preProcessExtendedInfo(currentFile.getPath(),null );
            Optional<Integer> maxpos = savedEntities.stream().map(SavedEntity::getToken).max(Comparator.comparing(Token::getSentenceNumber)).map(Token::getSentenceNumber);
            List<PAN11PlagiarismInfo>  currentPlagiarismInfos = plagiarismInformation.get(currentFile.getName().replace(".txt",".xml"));
            int index = 0;
            for(PAN11PlagiarismInfo plagiarismInfo:currentPlagiarismInfos){
                int plagSuspStart = candOrSusp ? plagiarismInfo.getSourceOffset() : plagiarismInfo.getThisOffset();
                int plagSuspEnd = candOrSusp ? (plagiarismInfo.getSourceOffset() + plagiarismInfo.getSourceLength()) : (plagiarismInfo.getThisOffset() + plagiarismInfo.getThisLength());

                List<SavedEntity>  plagiarismInfoEntities = savedEntities.stream().filter(savedEntity -> isEntityRelatedToPlagiarism(savedEntity.getToken().getStartCharacter(),savedEntity.getToken().getEndCharacter(),plagSuspStart,plagSuspEnd)).collect(Collectors.toList());
                String fragmentName = getFragmentName(currentFile, index, candOrSusp);
                entitiesMap.put(fragmentName,plagiarismInfoEntities);
                index++;
            }
        }
        return entitiesMap;
    }

    private static Map<String, List<SavedEntity>> getFragments(OntologyBasedSimilarityAnalysis osa, List<File> inputFiles,
                                                                                                         int FRAGMENT_SENTENCES, int FRAGMENT_INCREMENT, boolean filterByResults, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, boolean candOrSusp) throws Exception {
        Map<String, List<SavedEntity>> entitiesMap = new HashMap<>();
        for (File currentFile: inputFiles) {
            List<SavedEntity> candidateSavedEntities = osa.preProcessExtendedInfo(currentFile.getPath(),null );
            Optional<Integer> maxpos = candidateSavedEntities.stream().map(SavedEntity::getToken).max(Comparator.comparing(Token::getSentenceNumber)).map(Token::getSentenceNumber);
            int maximumPosition = (maxpos.get()- FRAGMENT_SENTENCES);
            int index = 0;
            List<PAN11PlagiarismInfo> currentPlagiarismInfos = null;
            if(filterByResults){
                String key = currentFile.getName().replace(".txt",".xml");
                currentPlagiarismInfos = plagiarismInformation.get(key);
            }
            for(int currentSentencePosition=0; currentSentencePosition < maximumPosition; currentSentencePosition += FRAGMENT_INCREMENT){

                int finalCurrentSentencePosition = currentSentencePosition;
                List<SavedEntity> fragmentEntities = candidateSavedEntities.stream()
                        .filter(currentEntity ->
                                currentEntity.getToken().getSentenceNumber() >= finalCurrentSentencePosition
                                && currentEntity.getToken().getSentenceNumber() < (finalCurrentSentencePosition + FRAGMENT_SENTENCES))
                        .collect(Collectors.toList());
                if(filterByResults){
                    List<PAN11PlagiarismInfo> finalCurrentPlagiarismInfos = currentPlagiarismInfos;
                    //System.out.println("size pre "+fragmentEntities.size());
                    List<SavedEntity> fragmentEntitesFiltered = new ArrayList<>();
                    for(SavedEntity fragmentEntity:fragmentEntities){
                        if(isPlagiarismRelated(fragmentEntity.getToken().getStartCharacter(), fragmentEntity.getToken().getEndCharacter(),currentPlagiarismInfos,candOrSusp)){
                            fragmentEntitesFiltered.add(fragmentEntity);
                        }
                    }
                    //System.out.println("size post "+fragmentEntitesFiltered.size());

                    fragmentEntities = fragmentEntitesFiltered;

                    // Only take fragmentEntities which are results.
                    //System.out.println("------------");
                }
                //if(fragmentEntities.size() > 0 ){
                // For evaluation reasons also zero entity fragments have to be added.
                String fragmentName = getFragmentName(currentFile, index, candOrSusp);
                //SalvadorTextFragment fragment = createTextFragment(fragmentEntities, fragmentName);

                entitiesMap.put(fragmentName,fragmentEntities);
                //}
                index++;
            }
        }
        return entitiesMap;
    }


    @NotNull
    private static String getFragmentName(File candidateFile, int index, boolean candOrSusp) {
        int sourceIdOfFile = PAN11FileUtil.getSourceIdOfFile(candidateFile);
        if(candOrSusp){
            return "candidate-document"+String.format("%05d", sourceIdOfFile)+"~"+ index;

        }else{

            return "suspicious-document"+String.format("%05d", sourceIdOfFile)+"~"+ index;
        }
    }
}
