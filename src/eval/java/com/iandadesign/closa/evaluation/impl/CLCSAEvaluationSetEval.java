package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CLCSAEvaluationSetTest {

    // Old stuff.
    // Don't forget: Results for T+CSA were better, but we're only comparing CL-Algorithms.

    @Test
    public void evalCLCSAEvaluationSetVroniPlag() {
        /*
            True positives: 218
            Relevant elements: 309
            Irrelevant elements: 0
            Collection size: 309
            Selected elements: 307
            False positives: 89
            False negatives: 91

            Ranks 1 to 1

            Precision: [0.71009773]
            Recall: [0.7055016]
            F-Measure: [0.70779216]
         */
        try {
            EvaluationSet vroniPlagEvaluationSetCLCSA = new CLOSAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-vroniplag/fragments-text"),
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-vroniplag/sources-text")
            );

            vroniPlagEvaluationSetCLCSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void evalCLCSAEvaluationSetEnglishJapanese() {
         /*
             True positives: 90
             Relevant elements: 94
             Irrelevant elements: 0
             Collection size: 94
             Selected elements: 93
             False positives: 3
             False negatives: 4

             Ranks 1 to 1

             Precision: [0.9677419]
             Recall: [0.9574468]
             F-Measure: [0.9625668]
        */
        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLCSA = new CLCSAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja");

            englishJapaneseBBCEvaluationSetCLCSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCSAEvaluationSetEnglishChinese() {
        /*
            True positives: 426
            Relevant elements: 509
            Irrelevant elements: 0
            Collection size: 509
            Selected elements: 509
            False positives: 83
            False negatives: 83

            Ranks 1 to 1

            Precision: [0.83693516]
            Recall: [0.83693516]
            F-Measure: [0.8369352]
         */
        try {
            EvaluationSet englishChineseECCEEvaluationSetCLCSA = new CLCSAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh");

            englishChineseECCEEvaluationSetCLCSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
