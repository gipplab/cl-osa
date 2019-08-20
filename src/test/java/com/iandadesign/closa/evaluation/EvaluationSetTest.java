package com.iandadesign.closa.evaluation;

import com.iandadesign.closa.evaluation.impl.CLOSAEvaluationSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;

class EvaluationSetTest {

    @Test
    void testFileHandling() {
        EvaluationSet<String> englishJapaneseBBCEvaluationSetCLCSA = new CLOSAEvaluationSet(
                new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja",
                0
        );

        englishJapaneseBBCEvaluationSetCLCSA.saveDocumentTokensToFile(
                "src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en/35157967/0.txt",
                Collections.singletonList("test"));

    }
}
