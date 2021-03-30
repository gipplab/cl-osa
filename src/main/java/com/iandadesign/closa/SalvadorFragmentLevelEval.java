package com.iandadesign.closa;

import com.iandadesign.closa.model.*;
import com.iandadesign.closa.util.*;
import edu.stanford.nlp.util.ArrayMap;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.iandadesign.closa.PAN11EvaluationSetEval.logParams;
import static com.iandadesign.closa.model.SalvadorAnalysisParameters.*;
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
        if(args!=null && args.length >= 4){
            USE_ABSOLUTE_SCORES = Boolean.valueOf(args[0]);
            THRESH1 = Integer.valueOf(args[1]);
            THRESH2 = Double.valueOf(args[2]);
            FRAGMENT_MERGE_MODE = args[3];

        }

        evalPAN2011EnEs(null, smallTest, evaluateCandidateRetrieval, addCRResultInfo, maxMockSuspCandiates );

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
        String language = SalvadorAnalysisParameters.LANGUAGE;
        // This evaluates the specific English/Espanol-Partition from Franco Salvador
        if(languageIn!=null){
            language=languageIn;
        }
        String tag = "evalPAN2011En-"+language; // Identifier for logs ...
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
                doCREvaluationRecallFragmentsSalvador(params, tag, "CREvalRecall", resultSelectedCandidates, plagiarismInformation, language);
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
                                      HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, String language) throws Exception {
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

        if(SalvadorAnalysisParameters.SORT_SUSPICIOUS_FILES_BY_SIZE){
            logUtil.logAndWriteStandard(false, "Sorting suspicious files by size");
            // Most entitie first to create deterministic cache
            suspiciousFiles = sortByPlagiarismSizeSusp(suspiciousFiles, plagiarismInformation);
        }
        // For testing use just one basic file (and also just the corresponding results)
        // Sorted alphabetically or sorted by size (by step before)
        suspiciousFiles = filterBySuspFileLimit(plagiarismInformation, suspiciousFiles, SalvadorAnalysisParameters.DO_FILE_PREFILTERING, SalvadorAnalysisParameters.SUSP_FILE_LIMIT, SalvadorAnalysisParameters.SUSP_FILE_SELECTION_OFFSET, SalvadorAnalysisParameters.SORT_SUSPICIOUS_FILES_BY_SIZE);

        //List<File> suspiciousFilesChecking = sortByPlagiarismSizeSusp(suspiciousFiles,plagiarismInformation);

        // Filter only representative Test Files
        List<File> suspiciousFilesRepresentative = new ArrayList<>();
        if(SalvadorAnalysisParameters.SELECT_REPRESENTATIVE_TEST_FILES){
            suspiciousFilesRepresentative = PAN11FileUtil.getRepresentativeDataset(logUtil, suspiciousFiles);
            if(suspiciousFilesRepresentative==null){
                return;
            }

        }

        System.out.println("My First Suspicious-File: "+ suspiciousFiles.get(0).toString());
        System.out.println("Suspicious Files Count: "+ suspiciousFiles.size());

        // Presteps for PAN11 Evaluation remove caching directory (if there is one)
        String xmlResultsFolderPath = SalvadorPAN11XMLwriter.getXMLresultsFolderPath(tag, logUtil.getDateString(), preprocessedCachingDir);
        File cachingDir= new File(xmlResultsFolderPath +"/file_selection_cache");
        //PAN11FileUtil.removeDirectory(cachingDir);
        logUtil.logAndWriteStandard(true,"Caching dir start:", cachingDir.getPath());

        Map<String, Map<String, SalvadorStatisticsInfo>> allStatistics = new HashMap<>();




        // Filter plagiarism information by cases, obfuscation etc... if this is set.
        if(!SalvadorAnalysisParameters.PREFILTER.equals("NONE")){
            if(SalvadorAnalysisParameters.PREFILTER.equals("onlyManualTranslation")){
                plagiarismInformation = filterOnlyManualTranslation(plagiarismInformation);
            }else if(SalvadorAnalysisParameters.PREFILTER.equals("onlyAutomaticTranslation")){
                plagiarismInformation = filterOnlyAutomaticTranslation(plagiarismInformation);
            }else if(SalvadorAnalysisParameters.PREFILTER.equals("onlyShortCases")){
                plagiarismInformation = filterOnlyCaseLength(plagiarismInformation, PAN11PlagiarismInfo.CaseLength.SHORT);
            }else if(SalvadorAnalysisParameters.PREFILTER.equals("onlyMediumCases")){
                plagiarismInformation = filterOnlyCaseLength(plagiarismInformation, PAN11PlagiarismInfo.CaseLength.MEDIUM);
            }else if(SalvadorAnalysisParameters.PREFILTER.equals("onlyLongCases")){
                plagiarismInformation = filterOnlyCaseLength(plagiarismInformation, PAN11PlagiarismInfo.CaseLength.LONG);
            }else{
                logUtil.logAndWriteError(false, "No correct prefilter Value!! "+SalvadorAnalysisParameters.PREFILTER);
                return;
            }
        }
        int overallSize = 0;
        for(List<PAN11PlagiarismInfo> plagiarismInfo:plagiarismInformation.values()){
            int size = plagiarismInfo.size();
            overallSize+=size;
        }
        //TODO check cases and read salvador
        // All cases info: 2920
        // Short cases info: 737
        // Medium cases info: 1078
        // Long cases info: 1105
        // 737 + 1078 + 1105 = 2920
        // Only Manual Translation: 227
        // Only Automated Translation: 2693

        // 288 to 290
        // All cases 4
        // Short cases:
        // Medium Cases:
        // Long cases:


        // Do the actual processing
        if(!SalvadorAnalysisParameters.DO_BATCHED_PROCESSING){
            // Just calculate all files at once
            logUtil.logAndWriteStandard(true,"BATCHED_PROCESSING:", "is deactivated, just calculating all files in one step");
            doScoresMapIteration(tag, plagiarismInformation, osa, logUtil, candidateFiles, suspiciousFiles, candidateEntitiesFragment, SalvadorAnalysisParameters.SUSP_FILE_SELECTION_OFFSET, SalvadorAnalysisParameters.SUSP_FILE_LIMIT, allStatistics, language);
        }else{
            int batchCounter = 0;
            Map<Integer, SalvadorRatKResponse>  overallRecallAtK = new ArrayMap<>();
            // Do calculating in batches
            for(int batchIndex = 0; batchIndex < suspiciousFiles.size(); batchIndex+=SalvadorAnalysisParameters.BATCHED_OFFSET_INCREMENT){
                int maxBatchIndex = min(suspiciousFiles.size(), (batchIndex+SalvadorAnalysisParameters.BATCHED_OFFSET_INCREMENT));
                if(maxBatchIndex < batchIndex) break;
                int overallIndex = SalvadorAnalysisParameters.SUSP_FILE_SELECTION_OFFSET + batchIndex;
                List<File> currentSuspiciousFiles = suspiciousFiles.subList(batchIndex,maxBatchIndex);
                if(SELECT_REPRESENTATIVE_TEST_FILES){
                    // Just skipping non-representative files if filter is on, for the sake of keeping caching this is implemented that way
                    if(!suspiciousFilesRepresentative.contains(currentSuspiciousFiles.get(0))){
                        batchCounter++;
                        continue;
                    }
                }
                logUtil.logAndWriteStandard(true,"BATCHED_PROCESSING:", "Doing batch from " + overallIndex + " to " + (overallIndex+currentSuspiciousFiles.size()));

                // Actual scores calculation
                Map<Integer, SalvadorRatKResponse>  recallAtKResponses = doScoresMapIteration(tag, plagiarismInformation, osa, logUtil, candidateFiles, currentSuspiciousFiles, candidateEntitiesFragment, overallIndex, currentSuspiciousFiles.size(), allStatistics, language);
                logUtil.logAndWriteStandard(true,"Caching dir current:", cachingDir.getPath());

                // Accumulate to overall R at K responses
                accumulateRecallAtK(overallRecallAtK, recallAtKResponses);
                batchCounter++;
            }
            logUtil.logAndWriteStandard(true, "BATCHED_PROCESSING:", "Done with "+batchCounter+" batche/s." );
            logAccumulatedRecallAtK(logUtil, overallRecallAtK);
        }

        if(SalvadorAnalysisParameters.DO_ANALYSIS){
            SalvadorStatisticsInfo salvadorStatisticsInfoAllCombined = SalvadorExtendedAnalytics.createCombinedStatistics(allStatistics);
            logUtil.logAndWriteStandard(false, "All Statistics (scoring selects only candidate files plagiarism:",SalvadorAnalysisParameters.ONLY_PLAGFILES_IN_STATS,")");

            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.overallInfoPositives,"overallPositives");
            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.overallInfoNegatives,"overallNegatives");
            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.mergedInfoPositives,"mergedPositives");
            printStatisticsInfo(logUtil, salvadorStatisticsInfoAllCombined.mergedInfoNegatives,"mergedNegatives");
        }

        logUtil.logAndWriteStandard(false, "Doing PAN-PC11 Evaluation WITHOUT micro averaging...");
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath(), false, "NONE");
        logUtil.logAndWriteStandard(false, "Doing PAN-PC11 Evaluation WITH micro averaging...");
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath(), true, "NONE");
        logUtil.logAndWriteStandard(false, "Doing PAN-PC11 Evaluation WITHOUT micro averaging: only SHORT CASES");
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath(), false, "onlyShortCases");
        logUtil.logAndWriteStandard(false, "Doing PAN-PC11 Evaluation WITHOUT micro averaging: only MEDIUM CASES");
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath(), false, "onlyMediumCases");
        logUtil.logAndWriteStandard(false, "Doing PAN-PC11 Evaluation WITHOUT micro averaging: only LONG CASES");
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath(), false, "onlyLongCases");
        logUtil.logAndWriteStandard(false, "Doing PAN-PC11 Evaluation WITHOUT micro averaging: only automated translation");
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath(), false, "onlyAutomaticTranslation");
        logUtil.logAndWriteStandard(false, "Doing PAN-PC11 Evaluation WITHOUT micro averaging: only manual translation");
        PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, xmlResultsFolderPath, cachingDir.getPath(), false, "onlyManualTranslation");



        PAN11FileUtil.removeDirectory(cachingDir);

    }
    private static HashMap<String, List<PAN11PlagiarismInfo>> filterOnlyCaseLength(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, String caseLength) {
        HashMap<String, List<PAN11PlagiarismInfo>> filteredPlagiarismInformation = new HashMap<>();
        for(String fileID: plagiarismInformation.keySet()){
            List<PAN11PlagiarismInfo> filteredPlaginfos = new ArrayList<>();
            List<PAN11PlagiarismInfo> currentPlaginfos = plagiarismInformation.get(fileID);
            for(PAN11PlagiarismInfo plagiarismInfo:currentPlaginfos){
                String caseLengthSusp = plagiarismInfo.getCaseLengthThis();
                String caseLengthCand = plagiarismInfo.getCaseLengthSource();
                if(!caseLengthSusp.equals(caseLengthCand)){
                    // TODO also filter caseLength in candidates???
                    System.out.println("!caseLengthSusp.equals(caseLengthCand): "+caseLengthSusp + "/" + caseLengthCand);
                }
                if(caseLengthSusp.equals(caseLength)){
                    filteredPlaginfos.add(plagiarismInfo);
                }
            }
            filteredPlagiarismInformation.put(fileID,filteredPlaginfos);
        }
        return filteredPlagiarismInformation;
    }

    private static HashMap<String, List<PAN11PlagiarismInfo>> filterOnlyAutomaticTranslation(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation) {
        HashMap<String, List<PAN11PlagiarismInfo>> filteredPlagiarismInformation = new HashMap<>();
        for(String fileID: plagiarismInformation.keySet()){
            List<PAN11PlagiarismInfo> filteredPlaginfos = new ArrayList<>();
            List<PAN11PlagiarismInfo> currentPlaginfos = plagiarismInformation.get(fileID);
            for(PAN11PlagiarismInfo plagiarismInfo:currentPlaginfos){
                String typePlag = plagiarismInfo.getType();
                if(typePlag.equals("translation") && !plagiarismInfo.getManualObfuscation()){
                    filteredPlaginfos.add(plagiarismInfo);
                }
            }
            if(filteredPlaginfos.size()> 0){
                filteredPlagiarismInformation.put(fileID,filteredPlaginfos);
            }
        }
        return filteredPlagiarismInformation;
    }
    private static HashMap<String, List<PAN11PlagiarismInfo>> filterOnlyManualTranslation(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation) {
        HashMap<String, List<PAN11PlagiarismInfo>> filteredPlagiarismInformation = new HashMap<>();
        for(String fileID: plagiarismInformation.keySet()){
            List<PAN11PlagiarismInfo> filteredPlaginfos = new ArrayList<>();
            List<PAN11PlagiarismInfo> currentPlaginfos = plagiarismInformation.get(fileID);
            for(PAN11PlagiarismInfo plagiarismInfo:currentPlaginfos){
                String typePlag = plagiarismInfo.getType();
                if(typePlag.equals("translation") && plagiarismInfo.getManualObfuscation()){
                    filteredPlaginfos.add(plagiarismInfo);
                }
            }
            if(filteredPlaginfos.size()> 0){
                filteredPlagiarismInformation.put(fileID,filteredPlaginfos);
            }
        }
        return filteredPlagiarismInformation;
    }

    @NotNull
    private static List<File> sortByFileSize(List<File> suspiciousFiles) {
        suspiciousFiles = suspiciousFiles.stream().sorted(Comparator.comparingLong(file -> ((File)file).length()).reversed()).collect(Collectors.toList());
        return suspiciousFiles;
    }

    private static List<File> sortByEntityOccurence(OntologyBasedSimilarityAnalysis osa, ExtendedLogUtil logUtil, List<File> suspiciousFiles) {
        suspiciousFiles = suspiciousFiles.parallelStream().sorted(Comparator.comparingLong(file -> {
            try {
                return (osa.getEntityCountByFile(((File)file).getPath(), logUtil));
            } catch (Exception exception) {
                logUtil.logAndWriteError(false, "Exception during sort",exception.getMessage());
                return 0;
            }
        }).reversed()).collect(Collectors.toList());
        return suspiciousFiles;
    }

    private static void logAccumulatedRecallAtK(ExtendedLogUtil logUtil, Map<Integer, SalvadorRatKResponse> overallRecallAtK) {
        List<Integer> sortedKeysRatK = overallRecallAtK.keySet().stream().sorted().collect(Collectors.toList());
        if(sortedKeysRatK.size()>0) {
            logUtil.logAndWriteStandard(false, "Overall Recall at K:");
            // Logging the overall r@k's
            sortedKeysRatK.forEach(key -> {
                SalvadorRatKResponse myObj = overallRecallAtK.get(key);
                if (myObj != null) {
                    myObj.refreshRatK();
                    myObj.logMe(logUtil);
                }
            });
        }
    }

    private static void accumulateRecallAtK(Map<Integer, SalvadorRatKResponse> overallRecallAtK, Map<Integer, SalvadorRatKResponse> recallAtKResponses) {
        recallAtKResponses.entrySet().stream().forEach(entry -> {
              Object test = overallRecallAtK.get(entry.getKey());
              if(test!=null){
                 SalvadorRatKResponse salvadorRatKResponse = (SalvadorRatKResponse) test;
                 salvadorRatKResponse.possibleFindings+=entry.getValue().possibleFindings;
                 salvadorRatKResponse.findings+=entry.getValue().findings;
              } else {
                  overallRecallAtK.put(entry.getKey(), entry.getValue());
              }
        });
    }

    static long allfragmententitiesacc = 0;
    static long allcasesacc = 0;
    static int minCaseLength= 100000;
    static int maxCaseLength= 0;

    private static Map<Integer, SalvadorRatKResponse> doScoresMapIteration(String tag, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, OntologyBasedSimilarityAnalysis osa, ExtendedLogUtil logUtil, List<File> candidateFiles, List<File> suspiciousFiles, Map<String, List<SavedEntity>> candidateEntitiesFragment, int filesOffset, int filesNumber,         Map<String, Map<String, SalvadorStatisticsInfo>> allStatistics, String language) throws Exception {
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

        for(String fragmentID:suspiciousEntitiesFragment.keySet()){
            int fragmentEntitiesNumber = suspiciousEntitiesFragment.get(fragmentID).size();
            minCaseLength = min(minCaseLength, fragmentEntitiesNumber);
            maxCaseLength = max(maxCaseLength, fragmentEntitiesNumber);
            allfragmententitiesacc+=fragmentEntitiesNumber;
            allcasesacc+=1;

        }

        logUtil.logAndWriteStandard(true, "minCaseLength:",minCaseLength );
        logUtil.logAndWriteStandard(true, "maxCaseLength:",maxCaseLength);
        logUtil.logAndWriteStandard(true, "Num cases:", allcasesacc);
        logUtil.logAndWriteStandard(true, "Average Case length:",(allfragmententitiesacc/(double)allcasesacc));

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
            String keyPath = scoresMapCache.generateFileKey(language, preprocessedCachingDir+"/scoresmap_serialization/",SalvadorAnalysisParameters.FRAGMENT_SENTENCES,SalvadorAnalysisParameters.FRAGMENT_INCREMENT,SalvadorAnalysisParameters.USE_ABSOLUTE_SCORES, SalvadorAnalysisParameters.DO_FILE_PREFILTERING, filesNumber, SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, filesOffset, SalvadorAnalysisParameters.SORT_SUSPICIOUS_FILES_BY_SIZE, SalvadorAnalysisParameters.USE_ENHANCHED_COSINE_ANALYSIS);
            logUtil.logAndWriteStandard(true, "Caching key is:", keyPath);

            // Try to find a file
            Map<String, Map<String, Double>>  scoresMapDes = scoresMapCache.deserializeScoresMap(keyPath);
            if(scoresMapDes==null){
                logUtil.logAndWriteStandard(false, "Creating new scoresmap and serialize it.");
                if(!SalvadorAnalysisParameters.USE_ENHANCHED_COSINE_ANALYSIS){
                    scoresMap = osa.performCosineSimilarityAnalysis(simplifyEntitiesMap(suspiciousEntitiesFragment), simplifyEntitiesMap(candidateEntitiesFragment), SalvadorAnalysisParameters.USE_ABSOLUTE_SCORES, SalvadorAnalysisParameters.DO_STATISTICAL_WEIGHTING);

                }else{
                    scoresMap = osa.performEnhancedCosineSimilarityAnalysisP(simplifyEntitiesMap(suspiciousEntitiesFragment), simplifyEntitiesMap(candidateEntitiesFragment), logUtil);
                }
                scoresMapCache.serializeScoresMap(keyPath, scoresMap);
            }else{
                logUtil.logAndWriteStandard(false, "Load scoresmap from cache");
                scoresMap = scoresMapDes;
            }
        }else{
            logUtil.logAndWriteStandard(false, "SCORESMAP CACHING IS DEACTIVATED");
            if(!USE_ENHANCHED_COSINE_ANALYSIS) {
                scoresMap = osa.performCosineSimilarityAnalysis(simplifyEntitiesMap(suspiciousEntitiesFragment), simplifyEntitiesMap(candidateEntitiesFragment), SalvadorAnalysisParameters.USE_ABSOLUTE_SCORES, SalvadorAnalysisParameters.DO_STATISTICAL_WEIGHTING);
            }else{
                scoresMap = osa.performEnhancedCosineSimilarityAnalysisP(simplifyEntitiesMap(suspiciousEntitiesFragment), simplifyEntitiesMap(candidateEntitiesFragment), logUtil);
            }
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
        Map<Integer, SalvadorRatKResponse> recallAtKResponses = new ArrayMap<>();
        if(SalvadorAnalysisParameters.CALCULATE_RECALL_AT_K) {
            boolean relativeOverallScores = SalvadorAnalysisParameters.DO_RELATIVE_SCORING_R_AT_K; // Default: false
            int minsizePlagfragments = SalvadorAnalysisParameters.MIN_FRAGMENT_SIZE_R_AT_K; // for filtering irrelevant edge cases, Default: 0
            SalvadorRatKResponse recallAt1 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 1);
            recallAtKResponses.put(1, recallAt1);
            SalvadorRatKResponse recallAt5 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 5);
            recallAtKResponses.put(5, recallAt5);
            SalvadorRatKResponse recallAt10 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 10);
            recallAtKResponses.put(10, recallAt10);
            SalvadorRatKResponse recallAt20 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 20);
            recallAtKResponses.put(20, recallAt20);
            SalvadorRatKResponse recallAt50 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 50);
            recallAtKResponses.put(50, recallAt50);
            SalvadorRatKResponse recallAt100 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 100);
            recallAtKResponses.put(100, recallAt100);
            SalvadorRatKResponse recallAt200 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 200);
            recallAtKResponses.put(200, recallAt200);
            SalvadorRatKResponse recallAt25k = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 25000);
            recallAtKResponses.put(25000, recallAt25k);
            SalvadorRatKResponse recallAt10B = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(SalvadorAnalysisParameters.GET_PLAGSIZED_FRAGMENTS, relativeOverallScores, SalvadorAnalysisParameters.DISMISS_OVERLAPS_IN_R_AT_K, SalvadorAnalysisParameters.LOWER_K_MAX_PLAG_CAP_R_AT_K, minsizePlagfragments, scoresMap, suspiciousFiles, candidateEntitiesFragment, suspiciousEntitiesFragment, plagiarismInformation, logUtil, 10000000);
            recallAtKResponses.put(10000000, recallAt10B);
        }
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

        logUtil.logAndWriteStandard(false, "Doing Detailed Analysis...");

        AtomicLong allRelatedPlagiarismInfoCount = new AtomicLong();
        // Since suspDocFragmentMap is usually One
        suspDocFragmentMap.keySet().stream().forEach(suspiciousDocument -> {
            Map<String, Map<SalvadorTextFragment, SalvadorTextFragment>> supFilePlagiarism = new HashMap<>();
            Map<String, SalvadorStatisticsInfo> suspDocumentStats = new HashMap<>();
            List<PAN11PlagiarismInfo> relatedPlagiarismInfo = plagiarismInformation.get(suspiciousDocument.replace(".txt", ".xml"));

            // Calculate DA-Clustering for current file combination.
            SalvadorDetailedAnalysisResult daResult;

            // Gather fragments first and then do detailed analysis
            daResult = doDetailedAnalysisMultipleSuspCluster(
                    suspDocFragmentMap.get(suspiciousDocument),
                    candDocFragmentMap,
                    suspiciousEntitiesFragment,
                    candidateEntitiesFragment,
                    scoresMap,
                    SalvadorAnalysisParameters.THRESH1,
                    SalvadorAnalysisParameters.THRESH2,
                    SalvadorAnalysisParameters.TOPMOST,
                    SalvadorAnalysisParameters.DO_ANALYSIS,
                    relatedPlagiarismInfo,
                    logUtil
            );


            Map<String, List<Map<SalvadorTextFragment, SalvadorTextFragment>>>detailedAnalysisResultsD2D = daResult.resultMap;
            if(SalvadorAnalysisParameters.DO_ANALYSIS){
                if(daResult.salvadorStatisticsInfo!=null){
                    suspDocumentStats.put("candidateDocument", daResult.salvadorStatisticsInfo);
                }
            }

            for(String candDocument:detailedAnalysisResultsD2D.keySet()){
                List<Map<SalvadorTextFragment, SalvadorTextFragment>> findings = detailedAnalysisResultsD2D.get(candDocument);
                for(Map<SalvadorTextFragment, SalvadorTextFragment> finding:findings){
                    if(finding.size() >= 1){
                        Map<SalvadorTextFragment, SalvadorTextFragment> currentMap = supFilePlagiarism.get(candDocument);
                        if(currentMap==null) {
                            supFilePlagiarism.put(candDocument, finding);
                        }else{
                            // TBD validate if ok
                            currentMap.putAll(finding);
                        }
                        // Note results to

                    }
                }
            }




            allResults.put(suspiciousDocument, supFilePlagiarism);
            if(SalvadorAnalysisParameters.DO_ANALYSIS){
                allStatistics.put(suspiciousDocument, suspDocumentStats);
            }
        });
        System.out.println("Overall related plagiarism info: "+allRelatedPlagiarismInfoCount.get());


        // Write down all xml Results
        String xmlResultsFolderPath = SalvadorPAN11XMLwriter.writeDownAllXMLResults(tag, logUtil.getDateString(), preprocessedCachingDir, allResults);
        // Do evaluation with the current set filters
        //String baseResultsPath = "/data/CLOSA_data/preprocessed/preprocessed_extended/results_comparison/evalPAN2011Salvador"; // TODO adapt
        File cachingDir= new File(xmlResultsFolderPath +"/file_selection_cache");
        // Remove previous caching directory.
        //PAN11FileUtil.removeDirectory(cachingDir);
        List<File> suspiciousXML  =  suspiciousFiles.stream().map(file -> new File(file.getAbsolutePath().replace(".txt",".xml"))).collect(Collectors.toList()); //PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
        PAN11FileUtil.writeFileListToDirectory(suspiciousXML, cachingDir.getPath(), logUtil);

        // Perfomance: Free memory manually after a batch.
        //suspDocFragmentMap.clear();
        //candDocFragmentMap.clear();
        //suspiciousEntitiesFragment.clear();
        //candidateEntitiesFragment.clear();
        scoresMap.clear();
        System.gc();

        return recallAtKResponses;

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


    public static SalvadorDetailedAnalysisResult doDetailedAnalysisMultipleSuspCluster(List<String> suspiciousFragments,
                                                                    Map<String, List<String>> candidateFragments,
                                                                    Map<String, List<SavedEntity>> suspiciousEntitiesFragment,
                                                                    Map<String, List<SavedEntity>> candidateEntitiesFragment,
                                                                    Map<String, Map<String, Double>>  scoresMap,
                                                                    int THRESHOLD_1,
                                                                    double THRESHOLD_2,
                                                                    int TOPMOST,
                                                                    boolean DO_ANALYSIS,
                                                                    List<PAN11PlagiarismInfo> candidatePlagiarismInfos,
                                                                    ExtendedLogUtil logUtil) {
        //DIFFERENCE instead of one suspicious enitities candidates are fetched for the clusting
        // for multiple suspicious entities of one case the candidates are fetched
        double THRESH_TOPMOST = SalvadorAnalysisParameters.PRESELECTION_THRESH;

        // Get selected suspicious fragments from results
        Map<String, Map<String, Double>> scoresMapSelected = new HashMap<>(scoresMap);
        scoresMapSelected.keySet().retainAll(suspiciousFragments);
        Map<String, List<Map<SalvadorTextFragment, SalvadorTextFragment>>> fragmentInfosSelected = new ArrayMap<>();
        // Analysis related stuff
        Map<SalvadorTextFragment , Map<SalvadorTextFragment, Integer>> fragmentInfosAll = new ArrayMap<>();
        Map<SalvadorTextFragment , Map<SalvadorTextFragment, Integer>> fragmentInfosAllmerged = new ArrayMap<>();

        for(PAN11PlagiarismInfo relatedPlagiarism:candidatePlagiarismInfos){
            int suspPlagiarismStart = relatedPlagiarism.getThisOffset();
            int suspPlagiarismEnd = relatedPlagiarism.getThisLength() + relatedPlagiarism.getThisOffset();
            SalvadorTextFragment suspiciousFragmentByPlagInfo= new SalvadorTextFragment();
            suspiciousFragmentByPlagInfo.setSentencesStartChar(suspPlagiarismStart);
            suspiciousFragmentByPlagInfo.setSentencesEndChar(suspPlagiarismEnd);
            suspiciousFragmentByPlagInfo.setCharLengthBySentences(relatedPlagiarism.getThisLength());
            if(relatedPlagiarism.getTranslation()){
                suspiciousFragmentByPlagInfo.setTranslation(relatedPlagiarism.getTranslation());
                suspiciousFragmentByPlagInfo.setManualTranslation(relatedPlagiarism.getManualObfuscation());
            }

            List<SalvadorTextFragment> relatedFragments = new ArrayList<>();
            // Group up susp fragments in related plagiarism groups
            for(String suspiciousFragmentID:scoresMapSelected.keySet()) {
                // ... if ...
                SalvadorTextFragment suspiciousFragment = PAN11RankingEvaluator.createTextFragment(suspiciousEntitiesFragment.get(suspiciousFragmentID), suspiciousFragmentID, false);
                boolean plagiarismRelated = isEntityRelatedToPlagiarism(suspiciousFragment.getSentencesStartChar(), suspiciousFragment.getSentencesEndChar(), suspPlagiarismStart,suspPlagiarismEnd);
                if(plagiarismRelated){
                    relatedFragments.add(suspiciousFragment);
                }
            }
            if(relatedFragments.size()==0){
                System.out.println("WARN: Related Fragment size to plagiarism is zero!");
            }
            //if(relatedPlagiarism.getCaseLengthThis() == PAN11PlagiarismInfo.CaseLength.LONG)
            if(CLUSTERING_PARAM_BY_CASELENGTH){

                /*
                long currentCaseEntitySize = 0;
                for(String relFragmentID:relatedFragments.stream().map(SalvadorTextFragment::getFragmentID).collect(Collectors.toList())){
                    currentCaseEntitySize+=suspiciousEntitiesFragment.get(relFragmentID).size();
                }
                System.out.println("current Case size is: "+currentCaseEntitySize);
                System.out.println("current Case size is:"+caseLengthSusp);
                // Seems good 5/2 configuration
                if(currentCaseEntitySize<200){
                    TOPMOST = 5;
                    THRESHOLD_2 = 4;
                }else if(currentCaseEntitySize<450){
                    TOPMOST = 10;
                    THRESHOLD_2 = 13;
                }else{
                    TOPMOST = 15;
                    THRESHOLD_2 = 19;
                }
                */


                // Other way
                String caseLengthSusp = relatedPlagiarism.getCaseLengthThis();
                if(caseLengthSusp.equals(PAN11PlagiarismInfo.CaseLength.SHORT)){
                    TOPMOST = 5;
                    THRESHOLD_2 = 0.2;
                    THRESHOLD_1 = 1400;

                }else if(caseLengthSusp.equals(PAN11PlagiarismInfo.CaseLength.MEDIUM)){
                    TOPMOST = 5;
                    THRESHOLD_2 = 0.45;
                    THRESHOLD_1 = 1400;
                }else{
                    TOPMOST = 5;
                    THRESHOLD_2 = 0.686;
                    THRESHOLD_1 = 2400;
                }
            }

            // Get the topmost scoring candidate fragments for each plagiarism case related group
            List<String> bestCandidateFragmentIDgroup = new ArrayList<>();
            // Get the start stop coordinates for the fragments.
            Map<String, SalvadorTextFragment>  fragmentInfosSelectedCases = new ArrayMap<>();

            for(SalvadorTextFragment salvadorTextFragment:relatedFragments){
                Map<String, SalvadorTextFragment> bestCandidateFragmentInfosCase = new ArrayMap<>();

                Map<String, Double> candidateScores = new HashMap<>(scoresMapSelected.get(salvadorTextFragment.getFragmentID()));
                // Only use the corresponding candidates for the specified file
                //candidateScores.keySet().retainAll(candidateFragments);
                if(candidateScores.keySet().size()==0){
                    System.out.println("WARN: There is no candidate score for related fragment!");
                }
                // Get best scoring <RANKLIMIT> fragments
                Map<String, Double> candidateScoresMapSelected = candidateScores.entrySet().stream()
                        .filter(value -> !Double.isNaN(value.getValue()))
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .limit(TOPMOST)
                        .filter(value -> value.getValue() >= THRESH_TOPMOST)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                for(String candidateFragmentID: candidateScoresMapSelected.keySet()){
                    SalvadorTextFragment fragmentToAdd =  PAN11RankingEvaluator.createTextFragment(candidateEntitiesFragment.get(candidateFragmentID),candidateFragmentID, true);
                    fragmentToAdd.setComputedScore(candidateScoresMapSelected.get(candidateFragmentID));
                    SalvadorTextFragment alreadyInFragment = bestCandidateFragmentInfosCase.get(candidateFragmentID);
                    if(alreadyInFragment==null){
                        bestCandidateFragmentInfosCase.put(candidateFragmentID, fragmentToAdd);

                    }else{
                        fragmentToAdd.setComputedScore(Double.max(alreadyInFragment.getComputedScore(),fragmentToAdd.getComputedScore()));
                        bestCandidateFragmentInfosCase.put(candidateFragmentID, fragmentToAdd);
                    }
                }


                Map<String, SalvadorTextFragment>  fragmentInfosMergedCase = mergeFragments(THRESHOLD_1, bestCandidateFragmentInfosCase);


                // Rate the new fragment infos as plagiarism or not
                for(String clusteredFragmentID: fragmentInfosMergedCase.keySet()){
                    SalvadorTextFragment clusteredFragment = fragmentInfosMergedCase.get(clusteredFragmentID);
                    String relatedCandidateDocument = "candidate-document"+padLeftZeros(String.valueOf(clusteredFragment.getRelatedCandidateDocument()),5)+".txt";
                    //System.out.println(suspiciousFragmentID+"/"+candidateFragments.get(0)+":"+clusteredFragment.getComputedScore());
                    if(clusteredFragment.getComputedScore() > THRESHOLD_2){
                        fragmentInfosSelectedCases.put(clusteredFragmentID+salvadorTextFragment.getFragmentID(), clusteredFragment);
                    }
                }



            }

            // Merge the fragments
            Map<String, SalvadorTextFragment>  fragmentInfosMergedBest = mergeFragments(THRESHOLD_1, fragmentInfosSelectedCases);
            // Rate the new fragment infos as plagiarism or not
            for(String clusteredFragmentID: fragmentInfosMergedBest.keySet()){
                SalvadorTextFragment clusteredFragment = fragmentInfosMergedBest.get(clusteredFragmentID);
                String relatedCandidateDocument = "candidate-document"+padLeftZeros(String.valueOf(clusteredFragment.getRelatedCandidateDocument()),5)+".txt";
                //System.out.println(suspiciousFragmentID+"/"+candidateFragments.get(0)+":"+clusteredFragment.getComputedScore());
                if(clusteredFragment.getComputedScore() > THRESHOLD_2){
                    Map<SalvadorTextFragment, SalvadorTextFragment> currentFragmentMap = new ArrayMap<>();
                    currentFragmentMap.put(suspiciousFragmentByPlagInfo, clusteredFragment);
                    if(fragmentInfosSelected.get(relatedCandidateDocument) !=null){
                        fragmentInfosSelected.get(relatedCandidateDocument).add(currentFragmentMap);

                    }else{
                        List<Map<SalvadorTextFragment, SalvadorTextFragment>> myListing= new ArrayList<>();
                        myListing.add(currentFragmentMap);
                        fragmentInfosSelected.put(relatedCandidateDocument, myListing);
                    }
                }
            }


            if(DO_ANALYSIS){
                List<PAN11PlagiarismInfo> relatedPlagiarismsMocklist = new ArrayList<>();
                relatedPlagiarismsMocklist.add(relatedPlagiarism);

                for(String clusteredFragmentID: fragmentInfosMergedBest.keySet()){
                    SalvadorTextFragment fragment = fragmentInfosMergedBest.get(clusteredFragmentID);
                    int plagiarizedArea = getPlagiarismAreaAccumulated(fragment.getSentencesStartChar(), fragment.getSentencesEndChar(),relatedPlagiarismsMocklist,true);
                    // System.out.println("Area Covered: "+ plagiarizedArea+ " Plagiarism Size: "+relatedPlagiarism.getSourceLength()+" Fragment Size: "+fragment.getCharLengthBySentences());
                    Map<SalvadorTextFragment, Integer> currentMap = fragmentInfosAll.get(suspiciousFragmentByPlagInfo);
                    if(currentMap==null){
                        currentMap = new ArrayMap<>();
                        fragmentInfosAll.put(suspiciousFragmentByPlagInfo, currentMap);
                    }
                    fragmentInfosAll.get(suspiciousFragmentByPlagInfo).put(fragment, plagiarizedArea);
                }

                for(String clusteredFragmentID: fragmentInfosMergedBest.keySet()){
                    SalvadorTextFragment fragment = fragmentInfosMergedBest.get(clusteredFragmentID);
                    /*
                    if(relatedPlagiarism.getTranslation()){
                        fragment.setTranslation(true);
                        if(relatedPlagiarism.getManualObfuscation()){
                            fragment.setManualTranslation(true);
                        }
                        if(relatedPlagiarism.getAutomatedObfuscation()){
                            fragment.setAutomaticTranslation(true);
                        }
                    }
                    */
                    int plagiarizedArea =  getPlagiarismAreaAccumulated(fragment.getSentencesStartChar(), fragment.getSentencesEndChar(),relatedPlagiarismsMocklist,true);
                    Map<SalvadorTextFragment, Integer> currentMap = fragmentInfosAllmerged.get(suspiciousFragmentByPlagInfo);
                    if(currentMap==null){
                        currentMap = new ArrayMap<>();
                        fragmentInfosAllmerged.put(suspiciousFragmentByPlagInfo, currentMap);
                    }
                    fragmentInfosAllmerged.get(suspiciousFragmentByPlagInfo).put(fragment, plagiarizedArea);
                }
            }

        }
        if(fragmentInfosSelected.size()!=candidatePlagiarismInfos.size()){
            System.out.println("WARN (PLAGDET-SIZE): Found "+fragmentInfosSelected.size()+" Cases for "+candidatePlagiarismInfos.size()+" Case(s)");
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
    public static String padLeftZeros(String inputString, int length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
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
                    if(distance < THRESHOLD_1 && fragment1.getRelatedCandidateDocument() == fragment2.getRelatedCandidateDocument()){
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
        mergedFragment.setRelatedCandidateDocument(fragment1.getRelatedCandidateDocument());
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

    private static List<File> filterBySuspFileLimit(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, List<File> suspiciousFiles, boolean DO_FILE_PREFILTERING, int SUSP_FILE_LIMIT, int SUSP_FILE_SELECTION_OFFSET, boolean SORT_BY_SIZE) {
        if(DO_FILE_PREFILTERING) {
            if(SORT_BY_SIZE){
                suspiciousFiles = suspiciousFiles.stream().skip(SUSP_FILE_SELECTION_OFFSET).limit(SUSP_FILE_LIMIT).collect(Collectors.toList());// Just take one basic file.

            }else{
                suspiciousFiles = suspiciousFiles.stream().sorted().skip(SUSP_FILE_SELECTION_OFFSET).limit(SUSP_FILE_LIMIT).collect(Collectors.toList());// Just take one basic file.
            }
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
        //entityStart = max(0, entityStart-70); // 70 seems good choice, but still not 100% accurate
        //entityEnd+=70;
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

    private static List<File> sortByPlagiarismSizeSusp( List<File> inputFiles,
                                                                        HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation
                                                                        ) {
        Map<File, Long> filesByPlagiarismSize = new HashMap<>();
        long overallPlagiarismSize = 0;
        //long overallPlagiarismSizeES = 0;
        for (File currentFile: inputFiles) {
            List<PAN11PlagiarismInfo>  currentPlagiarismInfos = plagiarismInformation.get(currentFile.getName().replace(".txt",".xml"));
            long plagiarismLengthForFile = 0;
            for(PAN11PlagiarismInfo plagiarismInfo:currentPlagiarismInfos){
                //plagiarismInfo.getSourceLanguage()
                int plagiarismLength = plagiarismInfo.getThisLength();
                plagiarismLengthForFile+=plagiarismLength;
                /*
                if(plagiarismInfo.getSourceLanguage().equals("es")){
                    overallPlagiarismSizeES+=plagiarismLength;
                }
                 */
            }
            filesByPlagiarismSize.put(currentFile,plagiarismLengthForFile);
            overallPlagiarismSize+=plagiarismLengthForFile;
        }
        List<Map.Entry<File, Long>> filesByPlagiarismSizeSorted = filesByPlagiarismSize.entrySet().stream()
                .sorted(Comparator.comparingLong(fileLongEntry -> ((Map.Entry<File,Long>)fileLongEntry).getValue()).reversed())
                .collect(Collectors.toList());
        List<File> sortedFiles = filesByPlagiarismSizeSorted.stream().map(fileLongEntry -> fileLongEntry.getKey()).collect(Collectors.toList());
        return sortedFiles;
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
                    //List<PAN11PlagiarismInfo> finalCurrentPlagiarismInfos = currentPlagiarismInfos;
                    //System.out.println("size pre "+fragmentEntities.size());
                    List<SavedEntity> fragmentEntitesFiltered = new ArrayList<>();
                    for(SavedEntity fragmentEntity:fragmentEntities){

                        //TODO remove or implement fix completely
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
