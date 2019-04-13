package com.fabianmarquart.closa;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OntologyBasedSimilarityAnalysisTest {

    @Test
    public void executeAlgorithmAndComputeScoresTest() {
        String suspiciousPath = "/Users/fabian/citeplag-dev-backend/pds-backend-core/src/test/resources/org/sciplore/pds/test-bbc/en/35157967/0.txt";

        String candidateFolderPath = "/Users/fabian/citeplag-dev-backend/pds-backend-core/src/test/resources/org/sciplore/pds/test-bbc/en/";

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
}
