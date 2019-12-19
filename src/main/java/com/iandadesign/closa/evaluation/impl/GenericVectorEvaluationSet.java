package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.model.Dictionary;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.SparseRealVector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenericVectorEvaluationSet extends EvaluationSet<Double> {

    public GenericVectorEvaluationSet(File suspiciousFolder, File candidateFolder) throws IOException {
        super(suspiciousFolder, candidateFolder);
    }

    public GenericVectorEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage);
    }

    public GenericVectorEvaluationSet(File suspiciousFolder, File candidateFolder, int fileCountLimit) {
        super(suspiciousFolder, candidateFolder, fileCountLimit);
    }

    public GenericVectorEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int fileCountLimit) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, fileCountLimit);
    }

    public GenericVectorEvaluationSet(File folder, String suspiciousSuffix, String candidateSuffix) {
        super(folder, suspiciousSuffix, candidateSuffix);
    }

    public GenericVectorEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, File extraCandidateFolder, int fileCountLimit) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, extraCandidateFolder, fileCountLimit);
    }

    public GenericVectorEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, File extraCandidateFolder) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, extraCandidateFolder);
    }

    @Override
    protected List<Double> preProcess(String documentPath, String documentLanguage) {
        try {
            List<Double> vector = FileUtils.readLines(new File(documentPath), StandardCharsets.UTF_8)
                    .stream()
                    .map(Double::parseDouble)
                    .collect(Collectors.toList());

            System.out.println("Vector of " + documentPath + " : " + vector);
            return vector;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void performAnalysis() {
        suspiciousIdCandidateScoresMap = new HashMap<>();

        // perform detailed analysis
        System.out.println("Perform detailed analysis \n");

        suspiciousIdTokensMap.forEach((String suspiciousId, List<Double> suspiciousVector) -> {

            Map<String, Double> candidateScoreMap = candidateIdTokensMap.entrySet()
                    .stream()
                    .collect(
                            Collectors.toMap(Map.Entry::getKey,
                                    entry -> {

                                        return Dictionary.cosineSimilarity(
                                                listToVector(entry.getValue()),
                                                listToVector(suspiciousVector));
                                    }
                            )
                    );

            System.out.println("candidateScoreMap = " + candidateScoreMap);

            if (candidateScoreMap.isEmpty()) {
                System.out.println("False negative. Did not detect");
                System.out.println(suspiciousId);
                System.out.println("in");
                System.out.println(suspiciousIdCandidateIdMap.get(suspiciousId));
            } else {
                String retrievedCandidateId = candidateScoreMap.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .get()
                        .getKey();

                if (suspiciousIdCandidateIdMap.get(suspiciousId).equals(retrievedCandidateId)) {
                    System.out.println("True positive.");
                } else {
                    System.out.println("False positive. Falsely detected ");
                    System.out.println(suspiciousId);
                    System.out.println("in");
                    System.out.println(retrievedCandidateId);
                }
            }

            suspiciousIdCandidateScoresMap.put(suspiciousId, candidateScoreMap);
        });
    }


    private SparseRealVector listToVector(List<Double> doubles) {
        SparseRealVector vector = new OpenMapRealVector(doubles.size());

        for (int i = 0; i < doubles.size(); i++) {
            vector.setEntry(i, doubles.get(i));
        }

        return vector;
    }


}
