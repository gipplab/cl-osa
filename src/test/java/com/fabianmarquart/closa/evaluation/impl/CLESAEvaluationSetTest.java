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
            Ranks 1 to 50

            Precision: [1.0638298, 1.0638298, 1.0638298, 1.0638298, 1.0638298, 1.0638298, 1.0638298]
            Recall: [1.0638298, 2.1276596, 3.1914895, 5.319149, 10.638298, 21.276596, 53.19149]
            F-Measure: [1.0638298, 1.4184396, 1.5957447, 1.7730495, 1.9342359, 2.0263424, 2.0859408]

            Mean reciprocal rank: 4.78638865779726

            Aligned document similarities

            {0.0=50}

            {0.0=53.19149}
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
    public void testCLESAEnglishChinese() {

        try {
            CLESAEvaluationSet englishChineseECCEEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/ECCE/en"), "en",
                    new File("src/test/resources/com/fabianmarquart/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testCLESAPan11Documents() {
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
