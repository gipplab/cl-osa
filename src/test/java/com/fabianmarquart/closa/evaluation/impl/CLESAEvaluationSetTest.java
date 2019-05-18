package com.fabianmarquart.closa.evaluation.impl;

import com.fabianmarquart.closa.evaluation.EvaluationSet;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CLESAEvaluationSetTest {

    /////////////////////////////////////////////////// CL-ESA //////////////////////////////////////////////////

    @Test
    public void testCLESAEnglishJapaneseMonolingual() {
        /*
            Top 10 ranked retrieval, concept space dimensionality 5000. Total time: 3 d 05:36 h

            Potthast 2008:
            "If high retrieval speed or a high multilinguality is desired, documents should be represented as 1000-dimensional concept vectors.
            At a lower dimension the retrieval quality deteriorates significantly.
            A reasonable trade-off between retrieval quality and runtime is achieved for a concept space dimensionality between 1 000 and 10 000."

            True positives: 10
            Relevant elements: 94
            Irrelevant elements: 0
            Collection size: 94
            Selected elements: 940
            False positives: 84
            False negatives: 84

            Precision: 0.010638298
            Recall: 0.10638298
            F-Measure: 0.019342358701873702
        */

        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/preprocessed-t/test-bbc/en"), "en",
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/preprocessed-t/test-bbc/ja"), "en"
            );

            englishJapaneseBBCEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCLESAEnglishJapanese() {
        /*
            - Concept space dimensionality 5000.

            Ranks 1 to 50

            Precision: [0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298]
            Recall: [0.010638298, 0.021276595, 0.031914894, 0.04255319, 0.05319149, 0.06382979, 0.07446808, 0.08510638, 0.095744684, 0.10638298, 0.11702128, 0.12765957, 0.13829787, 0.14893617, 0.15957446, 0.17021276, 0.18085106, 0.19148937, 0.20212767, 0.21276596, 0.22340426, 0.23404256, 0.24468085, 0.25531915, 0.26595744, 0.27659574, 0.28723404, 0.29787233, 0.30851063, 0.31914893, 0.32978722, 0.34042552, 0.35106382, 0.3617021, 0.3723404, 0.38297874, 0.39361703, 0.40425533, 0.41489363, 0.42553192, 0.43617022, 0.44680852, 0.4574468, 0.4680851, 0.4787234, 0.4893617, 0.5, 0.5106383, 0.5212766, 0.5319149]
            F-Measure: [0.010638298, 0.014184396, 0.015957447, 0.017021276, 0.017730495, 0.018237082, 0.01861702, 0.01891253, 0.019148936, 0.01934236, 0.019503545, 0.019639933, 0.019756839, 0.019858155, 0.019946808, 0.02002503, 0.020094562, 0.020156775, 0.020212764, 0.020263424, 0.020309476, 0.020351525, 0.020390071, 0.020425532, 0.020458264, 0.020488573, 0.020516718, 0.02054292, 0.020567374, 0.020590253, 0.020611702, 0.02063185, 0.020650813, 0.020668693, 0.02068558, 0.020701554, 0.020716686, 0.020731041, 0.02074468, 0.020757653, 0.02077001, 0.02078179, 0.020793036, 0.020803781, 0.020814061, 0.020823902, 0.020833332, 0.020842379, 0.020851063, 0.020859407]


            - Concept space dimensionality 10000.

            Ranks 1 to 50

            Precision: [0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298, 0.010638298]
            Recall: [0.010638298, 0.021276595, 0.031914894, 0.04255319, 0.05319149, 0.06382979, 0.07446808, 0.08510638, 0.095744684, 0.10638298, 0.11702128, 0.12765957, 0.13829787, 0.14893617, 0.15957446, 0.17021276, 0.18085106, 0.19148937, 0.20212767, 0.21276596, 0.22340426, 0.23404256, 0.24468085, 0.25531915, 0.26595744, 0.27659574, 0.28723404, 0.29787233, 0.30851063, 0.31914893, 0.32978722, 0.34042552, 0.35106382, 0.3617021, 0.3723404, 0.38297874, 0.39361703, 0.40425533, 0.41489363, 0.42553192, 0.43617022, 0.44680852, 0.4574468, 0.4680851, 0.4787234, 0.4893617, 0.5, 0.5106383, 0.5212766, 0.5319149]
            F-Measure: [0.010638298, 0.014184396, 0.015957447, 0.017021276, 0.017730495, 0.018237082, 0.01861702, 0.01891253, 0.019148936, 0.01934236, 0.019503545, 0.019639933, 0.019756839, 0.019858155, 0.019946808, 0.02002503, 0.020094562, 0.020156775, 0.020212764, 0.020263424, 0.020309476, 0.020351525, 0.020390071, 0.020425532, 0.020458264, 0.020488573, 0.020516718, 0.02054292, 0.020567374, 0.020590253, 0.020611702, 0.02063185, 0.020650813, 0.020668693, 0.02068558, 0.020701554, 0.020716686, 0.020731041, 0.02074468, 0.020757653, 0.02077001, 0.02078179, 0.020793036, 0.020803781, 0.020814061, 0.020823902, 0.020833332, 0.020842379, 0.020851063, 0.020859407]
         */
        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/ja"), "ja"
            );

            englishJapaneseBBCEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCLESAPan11Documents() {
        /*
            True positives: 2794
            Relevant elements: 2920
            Irrelevant elements: 0
            Collection size: 2920
            Selected elements: 2920
            False positives: 126
            False negatives: 126

            Ranks 1 to 1

            Precision: [0.95684934]
            Recall: [0.95684934]
            F-Measure: [0.95684934]
         */

        try {
            CLESAEvaluationSet englishSpanishPan11EvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es",
                    500
            );

            englishSpanishPan11EvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCLESAJrcAcquisDocuments() {
        try {
            CLESAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"), "fr",
                    500
            );
            englishFrenchJrcAcquisEvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testCLESAEuroparlDocuments() {

        try {
            CLESAEvaluationSet englishFrenchEuroparlEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/fr"), "fr",
                    500
            );
            englishFrenchEuroparlEvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
