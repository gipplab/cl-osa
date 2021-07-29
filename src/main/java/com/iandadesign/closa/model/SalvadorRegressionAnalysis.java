package com.iandadesign.closa.model;

import com.iandadesign.closa.SalvadorFragmentLevelEval;
import com.iandadesign.closa.analysis.featurama.matrix.CorrelationMatrix;
import com.iandadesign.closa.analysis.featurama.matrix.Matrix;
import com.iandadesign.closa.analysis.featurama.observation.Observation;
import com.iandadesign.closa.analysis.featurama.observation.ObservationHolder;
import com.iandadesign.closa.util.PAN11PlagiarismInfo;
import com.iandadesign.closa.util.*;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SalvadorRegressionAnalysis {
    ExtendedLogUtil logUtil;
    static SalvadorFragmentLevelEval evaluation;

    public SalvadorRegressionAnalysis(ExtendedLogUtil _logUtil){
        logUtil = _logUtil;
    }

    /**
     * Save all collected observations into CSV file
     * @param observationList containing all collected data
     */
    public void saveObservationsAsCsv(ObservationHolder observationList){
        Matrix data = new Matrix(observationList);
        CorrelationMatrix corr = new CorrelationMatrix(data);
                corr.display();
                try {
            LocalDateTime myDateObj = LocalDateTime.now();
            System.out.println("Before formatting: " + myDateObj);
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss");

            String formattedDate = myDateObj.format(myFormatObj);
            data.saveMatrixToFile(SalvadorAnalysisParameters.SAVE_CSV_DIR,  formattedDate + "dataMatrix");
            corr.saveMatrixToFile(SalvadorAnalysisParameters.SAVE_CSV_DIR,  formattedDate + "correlationMatrix");
            logUtil.logAndWriteStandard(false, "Saved data and correlation values.");
        } catch (
        IOException e) {
            logUtil.logAndWriteError(false,"Error saving the correlation matrix!");
            logUtil.logAndWriteError(false,e.getMessage());
        }
    }

    /**
     * Save current status of observations into CSV file
     * @param observationList containing all collected data
     * @param suspiciousDocument name of last suspicious document
     */
    public void saveCurrentObservationsAsCsv(ObservationHolder observationList, String suspiciousDocument){
        Matrix data = new Matrix(observationList);
        CorrelationMatrix corr = new CorrelationMatrix(data);
        corr.display();
        try {
            data.saveMatrixToFile(SalvadorAnalysisParameters.SAVE_CSV_DIR, FilenameUtils.removeExtension(suspiciousDocument) + "dataMatrix");
            corr.saveMatrixToFile(SalvadorAnalysisParameters.SAVE_CSV_DIR, FilenameUtils.removeExtension(suspiciousDocument) + "correlationMatrix");
            logUtil.logAndWriteStandard(false, "Saved data and correlation values.");
        } catch (IOException e) {
            logUtil.logAndWriteError(false, "Error saving the correlation matrix!");
            logUtil.logAndWriteError(false, e.getMessage());
        }
    }

    /**
     * Calculate Fragments based on different merge modes. If the modes return
     * the same amount of fragments store them in observation list
     * @param observationList here the observations are stored
     * @param relatedPlagiarism Plagiarism information
     * @param bestCandidateFragmentInfo list of best candidates to be merged
     * @param THRESHOLD_1 threshold for merging
     */
    public void storeInformationInObsevations(ObservationHolder observationList,
                                              PAN11PlagiarismInfo relatedPlagiarism,
                                              Map<String, SalvadorTextFragment> bestCandidateFragmentInfo,
                                              int THRESHOLD_1){
        // Do not use bestCandidateFragmentInfo, but all candidate info by setting TOPMOST to maximum
        // TODO Kay: collect all possible scores by observations matrix
        // relative / absolute in 2 different implementations

        SalvadorAnalysisParameters.FRAGMENT_MERGE_MODE = "simpleAdd";
        Map<String, SalvadorTextFragment> fragmentInfoMergedSimpleAdd = evaluation.mergeFragments(THRESHOLD_1, bestCandidateFragmentInfo);
        SalvadorAnalysisParameters.FRAGMENT_MERGE_MODE = "keepingMax";
        Map<String, SalvadorTextFragment>  fragmentInfoMergedKeepingMax = evaluation.mergeFragments(THRESHOLD_1, bestCandidateFragmentInfo);
        SalvadorAnalysisParameters.FRAGMENT_MERGE_MODE = "weightedAdd";
        Map<String, SalvadorTextFragment>  fragmentInfoMergedWeightedAdd = evaluation.mergeFragments(THRESHOLD_1, bestCandidateFragmentInfo);
        SalvadorAnalysisParameters.FRAGMENT_MERGE_MODE = "weightedAverage";
        Map<String, SalvadorTextFragment>  fragmentInfoMergedWeightedAverage = evaluation.mergeFragments(THRESHOLD_1, bestCandidateFragmentInfo);

        // only add observation if feature length are the same
        // TBD: a system when the length are not the same
        if(fragmentInfoMergedSimpleAdd.size() != fragmentInfoMergedKeepingMax.size() ||
                fragmentInfoMergedSimpleAdd.size() != fragmentInfoMergedWeightedAdd.size() ||
                fragmentInfoMergedSimpleAdd.size() != fragmentInfoMergedWeightedAverage.size())
        {
            logUtil.logAndWriteStandard(false, "Different sizes in merge. Not adding as observation!");
            logUtil.logAndWriteStandard(false, fragmentInfoMergedSimpleAdd.size());
            logUtil.logAndWriteStandard(false, fragmentInfoMergedKeepingMax.size());
            logUtil.logAndWriteStandard(false, fragmentInfoMergedWeightedAdd.size());
            logUtil.logAndWriteStandard(false, fragmentInfoMergedWeightedAverage.size());
        }
        else {
            saveFeaturesToList(observationList,
                    relatedPlagiarism,
                    fragmentInfoMergedSimpleAdd,
                    fragmentInfoMergedKeepingMax,
                    fragmentInfoMergedWeightedAdd,
                    fragmentInfoMergedWeightedAverage);
        }
    }

    /**
     * The merge modes returned the same amount of fragments, which are stored to the observation holder
     */
    private void saveFeaturesToList(ObservationHolder observationList,
                                    PAN11PlagiarismInfo relatedPlagiarism,
                                    Map<String, SalvadorTextFragment> fragmentInfoMergedSimpleAdd,
                                    Map<String, SalvadorTextFragment> fragmentInfoMergedKeepingMax,
                                    Map<String, SalvadorTextFragment> fragmentInfoMergedWeightedAdd,
                                    Map<String, SalvadorTextFragment> fragmentInfoMergedWeightedAverage) {
        // TODO: features.put("isPlagiarismPercentage", candidatePlagiarismInfos.get(i).);

        List< List<SalvadorTextFragment> > sortedTextFragmentsByMergeType = new ArrayList<>();
        sortedTextFragmentsByMergeType.add(returnSortedTextFragments(fragmentInfoMergedSimpleAdd));
        sortedTextFragmentsByMergeType.add(returnSortedTextFragments(fragmentInfoMergedKeepingMax));
        sortedTextFragmentsByMergeType.add(returnSortedTextFragments(fragmentInfoMergedWeightedAdd));
        sortedTextFragmentsByMergeType.add(returnSortedTextFragments(fragmentInfoMergedWeightedAverage));


        for(int i = 0; i < fragmentInfoMergedSimpleAdd.size(); i++) {
            try {
            LinkedHashMap<String, Object> features = new LinkedHashMap<>();

            int area = returnPlagiarismValue(relatedPlagiarism,
                    sortedTextFragmentsByMergeType.get(0).get(i));

            features.put("isPlagiarism", area);

            features.put("isPlagiarismBool", area > 0 ? 1 : 0);

            features.put("isPlagiarismBig", area > 650 ? 1 : 0);

            features.put("isPlagiarismMedium", area > 450 ? 1 : 0);

            features.put("isPlagiarismSmall", area > 0 ? 1 : 0);

            features.put("simpleAdd",
                    (sortedTextFragmentsByMergeType.get(0).get(i)).getComputedScore());

            /*features.put("isPlagiarismKeepingMax", returnPlagiarismValue(relatedPlagiarism,
                    sortedTextFragmentsByMergeType.get(1).get(i)));*/
            features.put("keepingMax",
                    (sortedTextFragmentsByMergeType.get(1).get(i)).getComputedScore());

            /*features.put("isPlagiarismWeightedAdd", returnPlagiarismValue(relatedPlagiarism,
                    sortedTextFragmentsByMergeType.get(2).get(i)));*/
            features.put("weightedAdd",
                    (sortedTextFragmentsByMergeType.get(2).get(i)).getComputedScore());

            /*features.put("isPlagiarismWeightedAverage", returnPlagiarismValue(relatedPlagiarism,
                    sortedTextFragmentsByMergeType.get(3).get(i)));*/
            features.put("weightedAverage",
                    (sortedTextFragmentsByMergeType.get(3).get(i)).getComputedScore());

            Observation obs = new Observation(features);
            observationList.add(obs);
            logUtil.logAndWriteStandard(false, "Added Observation of merge function to plagiarism");
            }
            catch(Exception e) {
                logUtil.logAndWriteError(false,"Error saving observation!");
                logUtil.logAndWriteError(false,e.getMessage());
            }
        }
    }

    private List<SalvadorTextFragment> returnSortedTextFragments(Map<String, SalvadorTextFragment> fragmentInfo){
        List<Map.Entry<String, SalvadorTextFragment>> sortedEntries = new ArrayList<>(fragmentInfo.entrySet());
        Collections.sort(sortedEntries,
                Map.Entry.comparingByValue(
                        Comparator.comparingLong(SalvadorTextFragment::getSentencesStartChar)));

        return sortedEntries.stream().map(fileLongEntry -> fileLongEntry.getValue()).collect(Collectors.toList());

    }

    public static Integer returnPlagiarismValue(PAN11PlagiarismInfo relatedPlagiarism, SalvadorTextFragment fragmentInfoMerged)
    {
        List<PAN11PlagiarismInfo> relatedPlagiarismsMocklist = new ArrayList<>();
        relatedPlagiarismsMocklist.add(relatedPlagiarism);
        SalvadorTextFragment fragment = fragmentInfoMerged;
        return evaluation.getPlagiarismAreaAccumulated(
                fragment.getSentencesStartChar(),
                fragment.getSentencesEndChar(),
                relatedPlagiarismsMocklist,
                true);
    }

}