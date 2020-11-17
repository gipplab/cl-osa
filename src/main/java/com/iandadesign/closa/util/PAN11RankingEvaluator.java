package com.iandadesign.closa.util;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.min;

/**
 * Candidate Retrieval Evaluation for R@k for comparisons
 * @author Johannes Stegmüller
 */
public class PAN11RankingEvaluator {
    
    public static double calculateRecallAtkFragmentCharacterLevel(){
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
        return 0.0;
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
