package com.fabianmarquart.closa.evaluation;

import com.fabianmarquart.closa.evaluation.impl.TOSAEvaluationSet;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

public class EvaluationSetTest {


    /////////////////////////////////////////////////// files ///////////////////////////////////////////////////

    @Test
    public void testFileHandling() {
        EvaluationSet englishJapaneseBBCEvaluationSetCLCSA = new TOSAEvaluationSet(
                new File("src/test/resources/org/sciplore/pds/test-bbc/en"), "en",
                new File("src/test/resources/org/sciplore/pds/test-bbc/ja"), "ja",
                0
        );

        englishJapaneseBBCEvaluationSetCLCSA.saveDocumentTokensToFile(
                "src/test/resources/org/sciplore/pds/test-bbc/en/35157967/0.txt",
                Collections.singletonList("test"));

    }
}
