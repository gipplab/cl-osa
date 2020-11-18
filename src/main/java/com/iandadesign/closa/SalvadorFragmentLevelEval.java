package com.iandadesign.closa;

import com.iandadesign.closa.model.*;
import com.iandadesign.closa.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.iandadesign.closa.PAN11EvaluationSetEval.logParams;

public class SalvadorFragmentLevelEval {

    public static String pathPrefix = "/data/pan-plagiarism-corpus-2011/external-detection-corpus";


    public static void main(String[] args) {
        Boolean smallTest = false;                  // Just select few suspicious files for the complete process
        Boolean evaluateCandidateRetrieval = true; // This triggers only the CR evaluation.
        Boolean addCRResultInfo = true;              // This will test detailed analysis with mocked CR results
        Integer maxMockSuspCandiates = 30;          // This is a delimeter for the maximum of suspicious files locked in mockCR Evaluation, set over 304 to check all susp files.

        //evalPAN2011All();

        if(args!=null && args.length >= 1){
            evalPAN2011EnEs(args[0], smallTest, evaluateCandidateRetrieval, addCRResultInfo, 30 );
        }else{
            evalPAN2011EnEs(null, smallTest, evaluateCandidateRetrieval, addCRResultInfo, 30 );
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
        Map<String, List<SavedEntity>> candidateEntitiesFragment = getFragments(osa, candidateFiles, FRAGMENT_SENTENCES, FRAGMENT_INCREMENT, false, null, true);


         // Create a list of suspicious fragments (only plagiarism involved fragments)
        Map<String, List<SavedEntity>> suspiciousEntitiesFragment = getFragments(osa, suspiciousFiles, FRAGMENT_SENTENCES, FRAGMENT_INCREMENT, true, plagiarismInformation, false);

        // For testing take a smaller suspicious map
        suspiciousEntitiesFragment = suspiciousEntitiesFragment.entrySet().stream().limit(2).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Do the comparison
        Map<String, Map<String, Double>>  scoresMap = osa.performCosineSimilarityAnalysis(simplifyEntitiesMap(suspiciousEntitiesFragment), simplifyEntitiesMap(candidateEntitiesFragment));

        // Calculate the recall for the scores map (character based)
        Double recallAt20 = PAN11RankingEvaluator.calculateRecallAtkFragmentCharacterLevel(scoresMap, candidateEntitiesFragment, suspiciousEntitiesFragment, 20);

    }


    private static boolean isEntityRelatedToPlagiarism(int entityStart, int entityEnd, int plagiarismStart, int plagiarismEnd){
        // similar to isWindowRelatedToPlagiarism in OntologyBasedSimilarityAnalysis
        // TODO maybe require minimum overlap otherwise prob

        // Overlap
        if(entityStart >= plagiarismStart && entityStart < plagiarismEnd ){
            return true;
        }
        // Overlap
        if(entityEnd > plagiarismStart && entityEnd <= plagiarismEnd ){
            return true;
        }

        // entitiy is within plagiarism (seems redundant)
        if(plagiarismStart  >= entityStart && plagiarismEnd <= entityEnd){
            return true;
        }

        return false;
    }
    private static boolean isPlagiarismRelated(SavedEntity savedEntity, List<PAN11PlagiarismInfo> currentPlagiarismInfos, Boolean candOrSusp){
        // Assuming Suspicious File here.
        int startCharacterEntity = savedEntity.getToken().getStartCharacter();
        int endCharacterEntity = savedEntity.getToken().getEndCharacter();
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
            for(int currentSentencePosition=0; currentSentencePosition < maximumPosition; currentSentencePosition+= FRAGMENT_INCREMENT){
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
                        if(isPlagiarismRelated(fragmentEntity,currentPlagiarismInfos,candOrSusp)){
                            fragmentEntitesFiltered.add(fragmentEntity);
                        }
                    }
                    //System.out.println("size post "+fragmentEntitesFiltered.size());

                    fragmentEntities = fragmentEntitesFiltered;

                    // Only take fragmentEntities which are results.
                    //System.out.println("------------");
                }
                if(fragmentEntities.size() > 0 ){
                    //List<String> fragmentEntitiesS = fragmentEntities.stream().map(SavedEntity::getWikidataEntityId).collect(Collectors.toList());
                    String fragmentName = getFragmentName(currentFile, index, candOrSusp);
                    entitiesMap.put(fragmentName, fragmentEntities);
                }
                index++;
            }
        }
        return entitiesMap;
    }

    @NotNull
    private static String getFragmentName(File candidateFile, int index, boolean candOrSusp) {
        int sourceIdOfFile = PAN11FileUtil.getSourceIdOfFile(candidateFile);
        if(candOrSusp){
            return "candidate-"+sourceIdOfFile+"-"+ index;
        }else{
            return "suspicious-"+sourceIdOfFile+"-"+ index;
        }
    }
}
