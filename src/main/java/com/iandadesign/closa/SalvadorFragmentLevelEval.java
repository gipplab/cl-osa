package com.iandadesign.closa;

import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.SavedEntity;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.iandadesign.closa.PAN11EvaluationSetEval.logParams;

public class SalvadorFragmentLevelEval {

    public static String pathPrefix = "/data/pan-plagiarism-corpus-2011/external-detection-corpus";


    public static void main(String[] args) {
        Boolean smallTest = true;                  // Just select few suspicious files for the complete process
        Boolean evaluateCandidateRetrieval = true; // This triggers only the CR evaluation.
        Boolean addCRResultInfo = true;              // This will test detailed analysis with mocked CR results
        Integer maxMockSuspCandiates = 30;          // This is a delimeter for the maximum of suspicious files locked in mockCR Evaluation, set over 304 to check all susp files.

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

        logUtil.writeStandardReport(false, "Assuming the preprpocessing has been done here. ");
        params.MAX_NUM_CANDIDATES_SELECTED = 5000;
        //params.CANDIDATE_SELECTION_TRESH = 0;
        logParams(logUtil, tag, params, osa);


        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");

        int THRESH1 = 1500;
        int THRESH2 = 2;
        int FRAGMENT_SENTENCES = 5; // In Sentences
        int FRAGMENT_INCREMENT = 2; // In Sentences

        // Create a list of candidate fragments (all)
        Map<String, List<String>> candidateEntitiesFragment = getFragments(osa, candidateFiles, FRAGMENT_SENTENCES, FRAGMENT_INCREMENT, false, null);

        // Create a list of suspicious fragments (only plagiarism involved fragments)
        Map<String, List<String>> suspiciousEntitiesFragment = getFragments(osa, suspiciousFiles, FRAGMENT_SENTENCES, FRAGMENT_INCREMENT, true, plagiarismInformation);

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

    private static Map<String, List<String>> getFragments(OntologyBasedSimilarityAnalysis osa, List<File> inputFiles,
                                     int FRAGMENT_SENTENCES, int FRAGMENT_INCREMENT, boolean filterByResults, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation) throws Exception {
        Map<String, List<String>> entitiesMap = new HashMap<>();
        for (File currentFile: inputFiles) {
            List<SavedEntity> candidateSavedEntities = osa.preProcessExtendedInfo(currentFile.getPath(),null );
            Optional<Integer> maxpos = candidateSavedEntities.stream().map(SavedEntity::getToken).max(Comparator.comparing(Token::getSentenceNumber)).map(Token::getSentenceNumber);
            int maximumPosition = (maxpos.get()- FRAGMENT_SENTENCES);
            int index = 0;
            for(int currentSentencePosition=0; currentSentencePosition < maximumPosition; currentSentencePosition+= FRAGMENT_INCREMENT){
                int finalCurrentSentencePosition = currentSentencePosition;
                List<SavedEntity> fragmentEntities = candidateSavedEntities.stream()
                        .filter(currentEntity ->
                                currentEntity.getToken().getSentenceNumber() >= finalCurrentSentencePosition
                                && currentEntity.getToken().getSentenceNumber() < (finalCurrentSentencePosition + FRAGMENT_SENTENCES))
                        .collect(Collectors.toList());
                if(filterByResults){
                    // Only take fragmentEntities which are results.
                    // TODO JS
                }
                List<String> candidateFragmentEntitiesS = fragmentEntities.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());
                String fragmentName = getFragmentName(currentFile, index);
                entitiesMap.put(fragmentName, candidateFragmentEntitiesS);
                index++;
            }
        }
        return entitiesMap;
    }

    @NotNull
    private static String getFragmentName(File candidateFile, int index) {
        int sourceIdOfFile = PAN11FileUtil.getSourceIdOfFile(candidateFile);
        String fragmentName = "candidate-"+sourceIdOfFile+"-"+ index;
        return fragmentName;
    }
}
