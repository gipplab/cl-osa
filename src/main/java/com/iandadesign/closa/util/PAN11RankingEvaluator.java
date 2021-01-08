package com.iandadesign.closa.util;

import com.iandadesign.closa.SalvadorFragmentLevelEval;
import com.iandadesign.closa.model.PremapEntryHolder;
import com.iandadesign.closa.model.SalvadorTextFragment;
import com.iandadesign.closa.model.SavedEntity;
import edu.stanford.nlp.util.ArrayMap;
import org.apache.commons.collections.comparators.ReverseComparator;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.iandadesign.closa.SalvadorFragmentLevelEval.isEntityRelatedToPlagiarism;
import static com.iandadesign.closa.SalvadorFragmentLevelEval.isPlagiarismRelated;
import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Candidate Retrieval Evaluation for R@k for comparisons
 * @author Johannes Stegmüller
 */
public class PAN11RankingEvaluator {
    public static int sizeOfFoundPlagiarism(String candidateName, SalvadorTextFragment fragment, List<SavedEntity> savedEntities, List<PAN11PlagiarismInfo> plagiarismInfos){
        //
        int startCharacter = Integer.MAX_VALUE;
        int endCharacter = 0;
        for(SavedEntity savedEntity:savedEntities){
            startCharacter =  min(savedEntity.getToken().getStartCharacter(), startCharacter);
            endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
        }

        candidateName = candidateName.replace("candidate","source");
        int foundPlagiarism = 0;
        for(PAN11PlagiarismInfo plagiarismInfo:plagiarismInfos){
            if(!candidateName.equals(plagiarismInfo.sourceReference)){
                continue;
            }
            int plagiarismStart = plagiarismInfo.getSourceOffset();
            int plagiarismEnd = plagiarismStart + plagiarismInfo.getSourceLength();

            int foundChars =  plagiarismAreaSize(startCharacter, endCharacter, plagiarismStart, plagiarismEnd);
            foundPlagiarism +=foundChars;
        }

        return foundPlagiarism;
    }

    private static int plagiarismAreaSize(int startCharacter, int endCharacter, int plagiarismStart, int plagiarismEnd) {
        // find the overlapping area
        int findingStart = max(startCharacter, plagiarismStart);
        int findingStop = min(endCharacter, plagiarismEnd);
        // Cap finding negative score by 0
        int plagiarismArea = max(findingStop-findingStart,0);
        return plagiarismArea;
    }

    private static boolean isAreaPlagiarism(int startCharacter, int endCharacter, int plagiarismStart, int plagiarismEnd) {
        // Overlap
        if(startCharacter >= plagiarismStart && startCharacter < plagiarismEnd){
            return true;
        }
        // Overlap
        if(endCharacter > plagiarismStart && endCharacter <= plagiarismEnd){
            return true;
        }

        // entitiy is within plagiarism
        if(plagiarismStart >= startCharacter && plagiarismEnd <= endCharacter){
            return true;
        }
        return false;
    }

    public static List<PAN11PlagiarismInfo>  getPlagiarismCasesRelatedToSuspFragment(SalvadorTextFragment suspiciousFragment, List<SavedEntity> suspEntities, List<PAN11PlagiarismInfo> plagiarismInfos){

        List<PAN11PlagiarismInfo> relatedPlagiarismInfos = new ArrayList<>();
        int accumulatedAreaCandidate = 0;

        int fragmentSuspStart = suspiciousFragment.getSentencesStartChar();
        int fragmentSuspEnd = suspiciousFragment.getSentencesEndChar();

        // Results on File Level to Results on Segment Level
        for(PAN11PlagiarismInfo plagiarismInfo:plagiarismInfos){
            int plaginfoSuspStart = plagiarismInfo.getThisOffset();
            int plaginfoSuspEnd = plagiarismInfo.getThisOffset() + plagiarismInfo.getThisLength();

            if(isAreaPlagiarism(fragmentSuspStart, fragmentSuspEnd, plaginfoSuspStart, plaginfoSuspEnd)){
                relatedPlagiarismInfos.add(plagiarismInfo);
            }
        }

        /*
        if(relatedPlagiarismInfos.size() > 1 ){
            System.out.println("Jackpot");
        }
        */

        return relatedPlagiarismInfos;
     }
    public static String getBaseName(String fragmentID, String ending){
        if(ending!=null){
            return fragmentID.split("~")[0].concat(ending);
        }else{
            return fragmentID.split("~")[0];
        }
    }
    public static int getSuspPlagiarismAreaSize(List<PAN11PlagiarismInfo> plagiarismInfos){
        int areaSizeAcc = 0;
        for(PAN11PlagiarismInfo plagiarismInfo:plagiarismInfos){
            areaSizeAcc += plagiarismInfo.getThisLength();
        }
        return areaSizeAcc;
    }
    /**
     * Calculate R@k on character level.
     * @param suspiciousIdCandidateScoresMap
     * @param candidateEntitiesMap
     * @param suspiciousEntitiesMap
     * @param plagiarismInformation
     * @param k
     * @return
     */
    public static double calculateRecallAtkFragmentCharacterLevel(boolean plagsizeFragments,
                                                                  boolean relativeOverallScores,
                                                                  int minsizeFragments,
                                                                  Map<String, Map<String, Double>>  suspiciousIdCandidateScoresMap,
                                                                  List<File> suspiciousFiles,
                                                                  Map<String, List<SavedEntity>> candidateEntitiesMap,
                                                                  Map<String, List<SavedEntity>> suspiciousEntitiesMap,
                                                                  HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation,
                                                                  ExtendedLogUtil logUtil,
                                                                  int k){
        if(plagsizeFragments){
            return getRecallAtKForPlagsize(suspiciousIdCandidateScoresMap, minsizeFragments, suspiciousFiles, candidateEntitiesMap, suspiciousEntitiesMap, plagiarismInformation, logUtil, k);
        }else{
            return getRecallAtKForNonPlagsize(suspiciousIdCandidateScoresMap, minsizeFragments, relativeOverallScores, suspiciousFiles, candidateEntitiesMap, suspiciousEntitiesMap, plagiarismInformation, logUtil, k);
        }
    }
    private static double getRecallAtKForNonPlagsize(Map<String, Map<String, Double>> suspiciousIdCandidateScoresMap, int minsizeFragments, boolean relativeOverallScores, List<File> suspiciousFiles, Map<String, List<SavedEntity>> candidateEntitiesMap, Map<String, List<SavedEntity>> suspiciousEntitiesMap, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, ExtendedLogUtil logUtil, int k) {
        int overallPossibleFindings = 0;
        if(!relativeOverallScores){
            // This is just counting all chars interleaved which are plagiarism as overallPossible finding
            overallPossibleFindings = getOverallPossibleFindings(suspiciousFiles, candidateEntitiesMap, plagiarismInformation, minsizeFragments, true);
        }

        int overallFindings = 0;

        for(File suspiciousFile: suspiciousFiles) {

            String key = suspiciousFile.getName().replace(".txt", ".xml");
            List<PAN11PlagiarismInfo> susPlagiarismInfo = plagiarismInformation.get(key);
            for (PAN11PlagiarismInfo currentPlagiarismInfo : susPlagiarismInfo) {
                String plagSourceFilename = currentPlagiarismInfo.getSourceReference();
                int plagSuspStart = currentPlagiarismInfo.getThisOffset();
                int plagSuspEnd = currentPlagiarismInfo.getThisOffset() + currentPlagiarismInfo.getThisLength();
                int plagCandStart = currentPlagiarismInfo.getSourceOffset();
                int plagCandEnd = currentPlagiarismInfo.getSourceOffset() + currentPlagiarismInfo.getSourceLength();
                List<String> plagiarizedSuspiciousFragments = new ArrayList<>();
                // Getting all related suspicious fragments to the case
                for(Map.Entry<String, List<SavedEntity>> mapEntry: suspiciousEntitiesMap.entrySet() ) {
                    String basename = getBaseName(mapEntry.getKey(), ".txt").replace("candidate", "source");
                    if (!basename.equals(suspiciousFile.getName())) {
                        continue;
                    }
                    List<SavedEntity> suspiciousFindingEntities = mapEntry.getValue().stream()
                            .filter(savedEntity -> isEntityRelatedToPlagiarism(savedEntity.getToken().getStartCharacter(), savedEntity.getToken().getEndCharacter(), plagSuspStart, plagSuspEnd))
                            .collect(Collectors.toList());
                    if (suspiciousFindingEntities.size() >= 1) {
                        plagiarizedSuspiciousFragments.add(mapEntry.getKey());
                    }
                }

                Map<Long, Double> compoundMap = new ArrayMap<>();
                long counter = 1;
                List<PremapEntryHolder> premapEntries = new ArrayList<>();
                Map<Long, String> myKeysCompound = new ArrayMap<>(); // Perfomance fix for compound map (string concat takes long)
                for(Map.Entry<String, Map<String, Double>> suspEntitiesMapEntry:suspiciousIdCandidateScoresMap.entrySet()){
                    if(!plagiarizedSuspiciousFragments.contains(suspEntitiesMapEntry.getKey())) continue;
                    Map<String, Double> premap = suspEntitiesMapEntry.getValue().entrySet().stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                             // .filter(entry -> entry.getValue()>0.0)
                            // .limit(50000) // For perfomance reasons take the best 1000 here per suspfile (only with 50k it reaches 100%)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                    /*
                    String suspKey = suspEntitiesMapEntry.getKey();
                    Map<Object, Object> flatmap = suspiciousIdCandidateScoresMap.entrySet().stream()
                            .flatMap(PAN11RankingEvaluator::flatten)
                            .collect(Collectors.toMap(entry->entry.getKey(), entry-> entry.getValue()));
                    */
                    premapEntries.addAll(premap.entrySet().stream().map(entry-> {
                        PremapEntryHolder peh = new PremapEntryHolder();
                        peh.key = entry.getKey();
                        peh.value = entry.getValue();
                        return peh;
                    }).collect(Collectors.toList()));

                    /*
                    for(Map.Entry<String, Double> innerEntry:premap.entrySet()) {
                        // String compoundKey = suspKey + "_" + innerEntry.getKey();
                        myKeysCompound.put(counter, innerEntry.getKey());
                        compoundMap.put(counter, innerEntry.getValue());
                        counter++;
                    }
                     */
                }

                premapEntries.sort(Comparator.comparingDouble(entry->((PremapEntryHolder)entry).value).reversed());
                premapEntries = premapEntries.stream().limit(k).collect(Collectors.toList());

                // Sort and limit the compound map by k
                /*
                Map<Long, Double> compoundMapFirst = compoundMap.entrySet().stream()
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .limit(k) // For perfomance reasons take the best 1000 here per suspfile
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
                */
                List<String> foundCandkeys = new ArrayList<>();

                // Get saved entities for every entry, check if they are plagiarism and denote size

                for(PremapEntryHolder premapEntry:premapEntries){
                    String candKey = premapEntry.key;
                    String baseCandFileName = getBaseName(candKey, ".txt").replace("candidate","source");

                    if(foundCandkeys.contains(candKey)){
                        continue; // Prevent double findings, which can happen
                    }
                    if(!baseCandFileName.equals(plagSourceFilename)){
                        continue;
                    }
                    List<SavedEntity> relatedEntities = candidateEntitiesMap.get(candKey);
                    if(relativeOverallScores){
                        int startCharacter = Integer.MAX_VALUE;
                        int endCharacter = 0;
                        for(SavedEntity savedEntity:relatedEntities){
                            startCharacter = min(savedEntity.getToken().getStartCharacter(), startCharacter);
                            endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
                        }

                        // Get the plagiarized area
                        int findingSize = max(0, endCharacter - startCharacter);
                        overallPossibleFindings += findingSize;
                    }

                    List<SavedEntity>  candidateFindingEntities = relatedEntities.stream()
                            .filter(savedEntity -> isEntityRelatedToPlagiarism(savedEntity.getToken().getStartCharacter(),savedEntity.getToken().getEndCharacter(),plagCandStart,plagCandEnd))
                            .collect(Collectors.toList());
                    int startCharacter = Integer.MAX_VALUE;
                    int endCharacter = 0;
                    for(SavedEntity savedEntity:candidateFindingEntities){
                        startCharacter = min(savedEntity.getToken().getStartCharacter(), startCharacter);
                        endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
                    }

                    // Get the plagiarized area
                    int findingSize = max(0, endCharacter - startCharacter);
                    if(findingSize<minsizeFragments){
                        continue;
                    }
                    if(findingSize>0){
                        foundCandkeys.add(candKey);
                    }
                    overallFindings+=findingSize;
                }
                logUtil.writeErrorReport(false,"asd");

            }
        }


        double recallAtK = (double) overallFindings / overallPossibleFindings * 100;
        logUtil.logAndWriteStandard(false, "Recall at ", k, " is: ", recallAtK, "Findings/PossibleFindings (",overallFindings,"/", overallPossibleFindings,")");
        return recallAtK;
    }

    private static Stream<Map.Entry<String, ?>> flatten(Map.Entry<String, ?> entry) {
        if (entry.getValue() instanceof Map) {
            return ((Map<String,?>) entry.getValue()).entrySet().stream().flatMap(PAN11RankingEvaluator::flatten);
        }
        return Stream.of(entry);
    }

    private static double getRecallAtKForPlagsize(Map<String, Map<String, Double>> suspiciousIdCandidateScoresMap, int minsizeFragments, List<File> suspiciousFiles, Map<String, List<SavedEntity>> candidateEntitiesMap, Map<String, List<SavedEntity>> suspiciousEntitiesMap, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, ExtendedLogUtil logUtil, int k) {
        // Found character counts.

        int maxK = candidateEntitiesMap.size();
        long overallFindings = getFindingsAtKForPlagsize(suspiciousIdCandidateScoresMap, minsizeFragments, suspiciousFiles, candidateEntitiesMap, suspiciousEntitiesMap, plagiarismInformation, k);
        long overallPossibleFindings = getFindingsAtKForPlagsize(suspiciousIdCandidateScoresMap, minsizeFragments, suspiciousFiles, candidateEntitiesMap, suspiciousEntitiesMap, plagiarismInformation,maxK);
        /* atm as pseudocode, supposedly how F.Salvador Evaluates Recall
        k=1
        overallFindings = 0;
        overallPossibleFindings=0;

        for each suspFragment in PlagiarizedSuspFragments:
            numCurrentFindings = 0;
            numPossibleFindings =0;
            List my_ranked_fragments = getRankedFragments(suspFragment).sort(by Score).getFirst(k);
            overall_char_count_possible = my_ranked_fragments.getAll.getSize();
            overall_char_count_plagiarism = results.getPossiblePlagiarismCount();

            for each fragment in my_ranked_fragments:
                charcount_detected_plagiarism = getCharcountPlagiarism(fragment);
                numCurrentFindings += charcount_detected_plagiarism
                overallFindings+=numCurrentFindings
            overallPossibleFindings+=min(overall_char_count_possible, overall_char_count_plagiarism)


        R@k = overallFindings/overallPossibleFindings * 100;
        */
        /*
        List<String> notInFoundCandKeysCompare = new ArrayList<>();
        for(String key:foundCandkeys){
            if(!foundKeysCompare.contains(key)){
                notInFoundCandKeysCompare.add(key);
            }
        }
        List<String> notInFoundCandKeys = new ArrayList<>();
        for(String key:foundKeysCompare){
            if(!foundCandkeys.contains(key)){
                notInFoundCandKeys.add(key);
            }
        }
        */
        //double recallAtKS = (double) overallFindings / overallPossibleFindingsSimple * 100;
        //logUtil.logAndWriteStandard(false, "RecallS at ", k, " is: ", recallAtKS, "Findings/PossibleFindings (",overallFindings,"/", overallPossibleFindingsSimple,")");

        double recallAtK = (double) overallFindings / overallPossibleFindings * 100;
        logUtil.logAndWriteStandard(false, "Recall at ", k, " is: ", recallAtK, "Findings/PossibleFindings (",overallFindings,"/", overallPossibleFindings,")");
        return recallAtK;
    }

    private static long getFindingsAtKForPlagsize(Map<String, Map<String, Double>> suspiciousIdCandidateScoresMap, int minsizeFragments, List<File> suspiciousFiles, Map<String, List<SavedEntity>> candidateEntitiesMap, Map<String, List<SavedEntity>> suspiciousEntitiesMap, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, int k) {
        long findingsAtK = 0;
        for(File suspiciousFile: suspiciousFiles) {
            String key = suspiciousFile.getName();
            Map<String, List<SavedEntity>> suspiciousEntitiesForFile = suspiciousEntitiesMap.entrySet().stream().filter(stringListEntry -> getBaseName(stringListEntry.getKey(),".txt").equals(key)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            // List<PAN11PlagiarismInfo> relatedPlagiarism = plagiarismInformation.get(key.replace(".txt",".xml"));


            for (String suspiciousFragmentID : suspiciousEntitiesForFile.keySet()) {
                List<SavedEntity> suspiciousEntities = suspiciousEntitiesMap.get(suspiciousFragmentID);
                SalvadorTextFragment currentSuspFragment = createTextFragment(suspiciousEntities, suspiciousFragmentID);
                String baseSuspFileName = getBaseName(suspiciousFragmentID, ".xml");
                List<PAN11PlagiarismInfo> relatedPlagiarism = getPlagiarismCasesRelatedToSuspFragment(currentSuspFragment, suspiciousEntities, plagiarismInformation.get(baseSuspFileName));


                // Getting the first k candidates in the ranking
                Map<String, Double> selectedCandidates = suspiciousIdCandidateScoresMap.get(suspiciousFragmentID);
                Map<String, Double> candidateScoresMapSorted = selectedCandidates
                        .entrySet().stream()
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .limit(k)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

                for (String candidateFragmentID : candidateScoresMapSorted.keySet()) {
                    String baseCandFileName = getBaseName(candidateFragmentID, ".txt").replace("candidate", "source");
                    // perfomance: if the no current plagiarism points to candidate this can be skipped (or filter plagiarism again by candidates here!)
                    List<SavedEntity> candidateEntites = candidateEntitiesMap.get(candidateFragmentID);

                    for (PAN11PlagiarismInfo relatedPlagcase : relatedPlagiarism) {
                        if (!baseCandFileName.equals(relatedPlagcase.getSourceReference())) {
                            continue;
                        }

                        int plagCandStart = relatedPlagcase.getSourceOffset();
                        int plagCandEnd = relatedPlagcase.getSourceOffset() + relatedPlagcase.getSourceLength();

                        List<SavedEntity> candidateFindingEntities = candidateEntites.stream()
                                .filter(savedEntity -> isEntityRelatedToPlagiarism(savedEntity.getToken().getStartCharacter(), savedEntity.getToken().getEndCharacter(), plagCandStart, plagCandEnd))
                                .collect(Collectors.toList());

                        int startCharacter = Integer.MAX_VALUE;
                        int endCharacter = 0;
                        for (SavedEntity savedEntity : candidateFindingEntities) {
                            startCharacter = min(savedEntity.getToken().getStartCharacter(), startCharacter);
                            endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
                        }
                        // Get the area the fragments cover
                        int findingSize = max(0, endCharacter - startCharacter);
                        if (findingSize < minsizeFragments) {
                            continue;
                        }
                        if (findingSize > 0) {
                            findingsAtK += findingSize;

                        }

                    }
                }

            }
        }
        return findingsAtK;
    }

    private static int getOverallPossibleFindings(List<File> suspiciousFiles, Map<String, List<SavedEntity>> candidateEntitiesMap, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, int minsizeFragments, boolean noDoubleCounts) {
        int overallPossibleFindings = 0;
        int findingCount =0;
        List<String> foundCandkeys = new ArrayList<>();

        // Calculate overall possible findings
        for(File suspiciousFile: suspiciousFiles){
            String key = suspiciousFile.getName().replace(".txt",".xml");
            List<PAN11PlagiarismInfo> susPlagiarismInfo = plagiarismInformation.get(key);


            for(PAN11PlagiarismInfo currentPlagiarismInfo:susPlagiarismInfo){

                //overallPossibleFindingsSimple += currentPlagiarismInfo.getSourceLength();

                //With overlaps: get the related fragment for each candidate plagiarism area
                String sourceFilename = currentPlagiarismInfo.getSourceReference();
                int plagCandStart = currentPlagiarismInfo.getSourceOffset();
                int plagCandEnd = currentPlagiarismInfo.getSourceOffset() + currentPlagiarismInfo.getSourceLength();
                List<SavedEntity> selectedSourceEntities = new ArrayList<>();
                for(Map.Entry<String, List<SavedEntity>> mapEntry: candidateEntitiesMap.entrySet() ){
                    String basename = getBaseName(mapEntry.getKey(), ".txt").replace("candidate","source");
                    if(!basename.equals(sourceFilename)){
                        continue;
                    }
                    if(noDoubleCounts && foundCandkeys.contains(mapEntry.getKey())){
                        continue;
                    }
                    //selectedSourceEntities.addAll(mapEntry.getValue());
                    // TODO continue here Probably no double entries
                    List<SavedEntity>  candidateFindingEntities = mapEntry.getValue().stream()
                            .filter(savedEntity -> isEntityRelatedToPlagiarism(savedEntity.getToken().getStartCharacter(),savedEntity.getToken().getEndCharacter(),plagCandStart,plagCandEnd))
                            .collect(Collectors.toList());

                    int startCharacter = Integer.MAX_VALUE;
                    int endCharacter = 0;
                    for(SavedEntity savedEntity:candidateFindingEntities){
                        startCharacter =  min(savedEntity.getToken().getStartCharacter(), startCharacter);
                        endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
                    }
                    // Get the area the fragments cover
                    int findingSize = max(0, endCharacter - startCharacter);
                    if(findingSize<minsizeFragments){
                        continue;
                    }

                    if(findingSize > 0){
                        overallPossibleFindings +=findingSize;
                        findingCount+=1;
                        if(noDoubleCounts){
                            foundCandkeys.add(mapEntry.getKey());
                        }
                    }


                }
            }
        }
        return overallPossibleFindings;
    }
    private static List<String> getOverallPossibleFindingsDebugFoundKeys(List<File> suspiciousFiles, Map<String, List<SavedEntity>> candidateEntitiesMap, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, int minsizeFragments, boolean noDoubleCounts) {
        int overallPossibleFindings = 0;
        int findingCount =0;
        List<String> foundCandkeys = new ArrayList<>();

        // Calculate overall possible findings
        for(File suspiciousFile: suspiciousFiles){
            String key = suspiciousFile.getName().replace(".txt",".xml");
            List<PAN11PlagiarismInfo> susPlagiarismInfo = plagiarismInformation.get(key);


            for(PAN11PlagiarismInfo currentPlagiarismInfo:susPlagiarismInfo){

                //overallPossibleFindingsSimple += currentPlagiarismInfo.getSourceLength();

                //With overlaps: get the related fragment for each candidate plagiarism area
                String sourceFilename = currentPlagiarismInfo.getSourceReference();
                int plagCandStart = currentPlagiarismInfo.getSourceOffset();
                int plagCandEnd = currentPlagiarismInfo.getSourceOffset() + currentPlagiarismInfo.getSourceLength();
                List<SavedEntity> selectedSourceEntities = new ArrayList<>();
                for(Map.Entry<String, List<SavedEntity>> mapEntry: candidateEntitiesMap.entrySet() ){
                    String basename = getBaseName(mapEntry.getKey(), ".txt").replace("candidate","source");
                    if(!basename.equals(sourceFilename)){
                        continue;
                    }
                    if(noDoubleCounts && foundCandkeys.contains(mapEntry.getKey())){
                        continue;
                    }
                    //selectedSourceEntities.addAll(mapEntry.getValue());
                    // TODO continue here Probably no double entries
                    List<SavedEntity>  candidateFindingEntities = mapEntry.getValue().stream()
                            .filter(savedEntity -> isEntityRelatedToPlagiarism(savedEntity.getToken().getStartCharacter(),savedEntity.getToken().getEndCharacter(),plagCandStart,plagCandEnd))
                            .collect(Collectors.toList());

                    int startCharacter = Integer.MAX_VALUE;
                    int endCharacter = 0;
                    for(SavedEntity savedEntity:candidateFindingEntities){
                        startCharacter =  min(savedEntity.getToken().getStartCharacter(), startCharacter);
                        endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
                    }
                    // Get the area the fragments cover
                    int findingSize = max(0, endCharacter - startCharacter);
                    if(findingSize<minsizeFragments){
                        continue;
                    }

                    if(findingSize > 0){
                        if(noDoubleCounts){
                            foundCandkeys.add(mapEntry.getKey());
                        }
                        findingCount+=1;
                    }
                    overallPossibleFindings +=findingSize;

                }
            }
        }
        return foundCandkeys;
    }

    public static double calculateRecallAtK(Map<String, Map <String, Double>> suspiciousIdCandidateScoresMap,
                                            HashMap<String, List<String>> resultSelectedCandidates,
                                            int k, ExtendedLogUtil logUtil){
        int overallFindings = 0;
        int overallPossibleFindings=0;

        for(String suspiciousFilePath:suspiciousIdCandidateScoresMap.keySet()){
            String suspPath = new File(suspiciousFilePath).getPath();
            String suspFileName = new File(suspiciousFilePath).getName();

            // Getting the first k candidates in the ranking
            Map<String, Double> selectedCandidates = suspiciousIdCandidateScoresMap.get(suspiciousFilePath);
            Map<String, Double> candidateScoresMapSorted = selectedCandidates
                    .entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(k)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            List<String> actualCandidates = resultSelectedCandidates.get(suspFileName.replace(".txt",".xml"));


            // Checking how many of the k candidates are actual plagiarism candidates
            int numCurrentFindings = 0;
            int numPossibleFindings = min(actualCandidates.size(), k);

            for(String selectedCandidate:candidateScoresMapSorted.keySet()) {
                File filename = new File(selectedCandidate);

                if(actualCandidates.contains(filename.getName())){
                    // This is a match
                    numCurrentFindings++;
                }
            }
            //double localRecall = (double) numCurrentFindings / numPossibleFindings * 100;

            overallFindings += numCurrentFindings;
            overallPossibleFindings += numPossibleFindings;
        }
        double recallAtK = (double) overallFindings / overallPossibleFindings * 100;

        logUtil.logAndWriteStandard(false, "Recall at ", k, " is: ", recallAtK, "Findings/PossibleFindings (",overallFindings,"/", overallPossibleFindings,")");
        return recallAtK;
    }


    public static double calculateRecallAtKStandard(Map<String, Map <String, Double>> suspiciousIdCandidateScoresMap,
                                            HashMap<String, List<String>> resultSelectedCandidates,
                                            int k, ExtendedLogUtil logUtil){
        int overallFindings = 0;
        int overallPossibleFindings=0;

        for(String suspiciousFilePath:suspiciousIdCandidateScoresMap.keySet()){
            String suspPath = new File(suspiciousFilePath).getPath();
            String suspFileName = new File(suspiciousFilePath).getName();

            // Getting the first k candidates in the ranking
            Map<String, Double> selectedCandidates = suspiciousIdCandidateScoresMap.get(suspiciousFilePath);
            Map<String, Double> candidateScoresMapSorted = selectedCandidates
                    .entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(k)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            List<String> actualCandidates = resultSelectedCandidates.get(suspFileName.replace(".txt",".xml"));


            // Checking how many of the k candidates are actual plagiarism candidates
            int numCurrentFindings = 0;

            for(String selectedCandidate:candidateScoresMapSorted.keySet()) {
                File filename = new File(selectedCandidate);

                if(actualCandidates.contains(filename.getName())){
                    // This is a match
                    numCurrentFindings++;
                }
            }
            //double localRecall = (double) numCurrentFindings / numPossibleFindings * 100;

            overallFindings += numCurrentFindings;
            overallPossibleFindings += k;
        }
        double recallAtK = (double) overallFindings / overallPossibleFindings * 100;

        logUtil.logAndWriteStandard(false, "Standard Recall at ", k, " is: ", recallAtK, "Findings/PossibleFindings (",overallFindings,"/", overallPossibleFindings,")");
        return recallAtK;
    }
    public static SalvadorTextFragment createTextFragment(List<SavedEntity> savedEntities, String fragmentName){

        SalvadorTextFragment currentFragment = new SalvadorTextFragment();
        currentFragment.setFragmentID(fragmentName);
        int startCharacter = Integer.MAX_VALUE;
        int endCharacter = 0;
        int startCharEntity = Integer.MAX_VALUE;
        int endCharEntitiy  =0;
        for(SavedEntity savedEntity:savedEntities){
            startCharacter =  min(savedEntity.getToken().getStartCharacter(), startCharacter);
            endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
            startCharEntity =  min(savedEntity.getToken().getStartCharacterCandidate(), startCharEntity);
            endCharEntitiy = max(savedEntity.getToken().getEndCharacterCandidate(), endCharEntitiy);
        }


        currentFragment.setSentencesStartChar(startCharacter);
        currentFragment.setSentencesEndChar(endCharacter);
        currentFragment.setCharLengthBySentences(endCharacter-startCharacter);

        currentFragment.setEntitiesStartChar(startCharEntity);
        currentFragment.setEntitiesEndChar(endCharEntitiy);
        currentFragment.setCharLengthByEntities(endCharEntitiy-startCharEntity);
        return currentFragment;
    }

}
