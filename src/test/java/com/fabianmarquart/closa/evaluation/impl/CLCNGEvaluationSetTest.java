package com.fabianmarquart.closa.evaluation.impl;

import com.fabianmarquart.closa.evaluation.EvaluationSet;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CLCNGEvaluationSetTest {

    @Test
    public void testCLCNGEvaluationSetEnglishJapanese() {
        /*
            True positives: 7
            Relevant elements: 94
            Irrelevant elements: 0
            Collection size: 94
            Selected elements: 93
            False positives: 86
            False negatives: 87

            Ranks 1 to 1

            Precision: [0.07526882]
            Recall: [0.07446808]
            F-Measure: [0.07486631]
        */
        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/ja"), "ja");

            englishJapaneseBBCEvaluationSetCLCNG.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
