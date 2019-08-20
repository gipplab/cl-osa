package com.iandadesign.closa;

import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.TokenUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OntologyBasedSimilarityAnalysisTest {

    @Test
    void executeAlgorithmAndComputeScores() {
        String suspiciousPath = "src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en/35157967/0.txt";

        String candidateFolderPath = "src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en/";

        List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .map(File::getPath)
                .limit(3)
                .collect(Collectors.toList());

        Map<String, Double> candidateScoreMap = new OntologyBasedSimilarityAnalysis()
                .executeAlgorithmAndComputeScores(suspiciousPath, candidatePaths);

        System.out.println(candidateScoreMap);
    }

    @Test
    void executeOntologyEnhancedAlgorithmAndComputeScores() {
        String suspiciousPath = "src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en/35157967/0.txt";

        String candidateFolderPath = "src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en/";

        List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .map(File::getPath)
                .limit(3)
                .collect(Collectors.toList());

        Map<String, Double> candidateScoreMap = new OntologyBasedSimilarityAnalysis()
                .executeOntologyEnhancedAlgorithmAndComputeScores(suspiciousPath, candidatePaths);

        System.out.println(candidateScoreMap);
    }

    @Test
    void preProcess() {
        Assertions.fail();
    }

    @Test
    void performCosineSimilarityAnalysis() {
        Assertions.fail();
    }

    @Test
    void performEnhancedCosineSimilarityAnalysis() {
        Assertions.fail();
    }
}
