package com.fabianmarquart.closa.evaluation.impl;

import com.fabianmarquart.closa.evaluation.EvaluationSet;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CLCNGEvaluationSetTest {

    @Test
    public void testCLCNGEnglishJapanese() {
        /*
            Ranks 1 to 50

            Precision: [78.49463, 42.473118, 28.315413, 18.064516, 9.247312, 4.784946, 1.9784946]
            Recall: [77.65958, 84.04256, 84.04256, 89.3617, 91.489365, 94.680855, 97.87234]
            F-Measure: [78.07487, 56.428574, 42.35925, 30.053669, 16.796875, 9.109519, 3.8785834]

            Mean reciprocal rank: 82.56386671215749


            Aligned document similarities

            {40.0=27, 60.0=3, 30.0=3, 50.0=60}

            {30.0=3.1914895, 60.0=3.1914895, 40.0=28.723404, 50.0=63.82979}
        */
        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/ja-t"), "en");

            englishJapaneseBBCEvaluationSetCLCNG.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCLCNGEnglishChinese() {

        try {
            EvaluationSet englishChineseECCEEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/ECCE/en"), "en",
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLCNG.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
