package com.fabianmarquart.closa.evaluation;

import com.fabianmarquart.closa.evaluation.impl.TOSAEvaluationSet;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

public class EvaluationSetTest {


    @Test
    public void testFileHandling() {
        EvaluationSet englishJapaneseBBCEvaluationSetCLCSA = new TOSAEvaluationSet(
                new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/en"), "en",
                new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/ja"), "ja",
                0
        );

        englishJapaneseBBCEvaluationSetCLCSA.saveDocumentTokensToFile(
                "src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/en/35157967/0.txt",
                Collections.singletonList("test"));

    }
}
