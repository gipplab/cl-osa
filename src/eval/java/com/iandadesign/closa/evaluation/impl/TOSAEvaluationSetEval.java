package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class TOSAEvaluationSetEval {

    /////////////////////////////////////////////////// T+OSA ///////////////////////////////////////////////////


    /**
     * Tests T+OSA with VroniPlag, core documents only.
     * <p>
     * Granularity: full documents.
     */
    @Test
    public void evalTOSAVroniPlag() {
        /*
            True positives: 306
            Relevant elements: 324
            Irrelevant elements: 0
            Collection size: 324
            Selected elements: 326
            False positives: 18
            False negatives: 18


            Precision: 0.9386503
            Recall: 0.9444444
            F-Measure: 0.9415384149849486
        */
        try {
            TOSAEvaluationSet vroniPlagEvaluationSetTOSA = new TOSAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-vroniplag/fragments-text"),
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-vroniplag/sources-text")
            );

            vroniPlagEvaluationSetTOSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests T+OSA with BBC English-Japanese, core documents only.
     * <p>
     * Granularity: full documents.
     */
    @Test
    public void evalTOSAEnglishJapaneseCosineSimilarity() {
        /*
            Translation + monolingual analysis. With cosine similarity.

            True positives: 89
            Relevant elements: 90
            Irrelevant elements: 0
            Collection size: 90
            Selected elements: 93
            False positives: 4
            False negatives: 1

            Precision: 0.9569892
            Recall: 0.98888886
            F-Measure: 0.9726776133710971
         */

        try {
            EvaluationSet englishJapaneseBBCEvaluationSetTOSA = new TOSAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja"
            );

            englishJapaneseBBCEvaluationSetTOSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Tests T+OSA with BBC English-Japanese, core documents plus extra candidate documents without
     * a suspicious file mapped to them.
     * <p>
     * Granularity: full documents.
     */
    @Test
    public void evalTOSAEnglishJapaneseWithExtraDocumentsCosineSimilarity() {
        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLOSA = new TOSAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en-extra")
            );

            englishJapaneseBBCEvaluationSetCLOSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests T+OSA with ECCE, core documents only.
     * <p>
     * Granularity: full documents.
     */
    @Test
    public void evalTOSAEnglishChineseCosineSimilarity() {
        /*
            Translation + monolingual analysis. With cosine similarity.

            True positives: 506
            Relevant elements: 509
            Irrelevant elements: 0
            Collection size: 509
            Selected elements: 511
            False positives: 3
            False negatives: 3

            Precision: 0.99021524
            Recall: 0.9941061
            F-Measure: 0.992156890544786
         */
        try {
            TOSAEvaluationSet englishChineseECCEEvaluationSetTOSA = new TOSAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh");

            englishChineseECCEEvaluationSetTOSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
