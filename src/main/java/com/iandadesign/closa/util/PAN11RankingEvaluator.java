package com.iandadesign.closa.util;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.min;

public class PAN11RankingEvaluator {

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
