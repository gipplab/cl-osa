package com.iandadesign.closa.util;

import com.iandadesign.closa.model.SavedEntity;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * Candidate Retrieval Evaluation for R@k for comparisons
 * @author Johannes Stegmüller
 */
public class PAN11RankingEvaluator {
    public static boolean isCandidateInPlagiarismArea(String candidateName, List<SavedEntity> savedEntities, List<PAN11PlagiarismInfo> plagiarismInfos){
        //
        int startCharacter = Integer.MAX_VALUE;
        int endCharacter = 0;
        for(SavedEntity savedEntity:savedEntities){
            startCharacter =  min(savedEntity.getToken().getStartCharacter(), startCharacter);
            endCharacter = max(savedEntity.getToken().getEndCharacter(), endCharacter);
        }

        for(PAN11PlagiarismInfo plagiarismInfo:plagiarismInfos){
            if(!candidateName.equals(plagiarismInfo.sourceReference)){
                continue;
            }
            int plagiarismStart = plagiarismInfo.getSourceOffset();
            int plagiarismEnd= plagiarismStart + plagiarismInfo.getSourceLength();

            // Overlap
            if(startCharacter >= plagiarismStart && startCharacter < plagiarismEnd ){
                return true;
            }
            // Overlap
            if(endCharacter > plagiarismStart && endCharacter <= plagiarismEnd ){
                return true;
            }

            // entitiy is within plagiarism
            if(plagiarismStart  >= startCharacter && plagiarismEnd <= endCharacter){
                return true;
            }
        }



        return false;
    }

    public static int getPlagiarismAreaAccumulatedCandidate(List<SavedEntity> savedEntities, List<PAN11PlagiarismInfo> plagiarismInfos){
        int accumulatedAreaCandidate = 0;
        for(PAN11PlagiarismInfo plagiarismInfo:plagiarismInfos){
            accumulatedAreaCandidate+= plagiarismInfo.getSourceLength();
        }
        return accumulatedAreaCandidate;
     }
    public static String getBaseName(String fragmentID, String ending){
        if(ending!=null){
            return fragmentID.split("~")[0].concat(ending);
        }else{
            return fragmentID.split("~")[0];
        }
    }
    public static double calculateRecallAtkFragmentCharacterLevel(Map<String, Map<String, Double>>  suspiciousIdCandidateScoresMap,
                                                                  Map<String, List<SavedEntity>> candidateEntitiesMap,
                                                                  Map<String, List<SavedEntity>> suspiciousEntitiesMap,
                                                                  HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation,
                                                                  int k){

        int overallFindings = 0;
        int overallPossibleFindings=0;

        for(String suspiciousFragmentID:suspiciousIdCandidateScoresMap.keySet()) {
            List<SavedEntity> suspiciousEntities = suspiciousEntitiesMap.get(suspiciousFragmentID);

            String baseSuspFileName = getBaseName(suspiciousFragmentID, ".xml");
            int maxCandPlagiarismCount = getPlagiarismAreaAccumulatedCandidate(suspiciousEntities, plagiarismInformation.get(baseSuspFileName));
            int currentFindings = 0;
            // Getting the first k candidates in the ranking
            Map<String, Double> selectedCandidates = suspiciousIdCandidateScoresMap.get(suspiciousFragmentID);
            Map<String, Double> candidateScoresMapSorted = selectedCandidates
                    .entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(k)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            for(String candidateFragmentID:candidateScoresMapSorted.keySet()){
                List<SavedEntity> candidateEntites = candidateEntitiesMap.get(candidateFragmentID);
                String baseCandFileName = getBaseName(candidateFragmentID, ".txt");

                boolean isPlagiarism = isCandidateInPlagiarismArea(baseCandFileName, candidateEntites, plagiarismInformation.get(baseSuspFileName));
                if(isPlagiarism) currentFindings++;
                System.out.println("test");
            }
            overallFindings += currentFindings;
            overallPossibleFindings += k;
        }
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

        double recallAtK = (double) overallFindings / overallPossibleFindings * 100;
        return recallAtK;
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

}
