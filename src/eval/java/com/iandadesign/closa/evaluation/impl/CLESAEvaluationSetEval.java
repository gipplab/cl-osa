package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class CLESAEvaluationSetEval {

    /////////////////////////////////////////////////// CL-ESA //////////////////////////////////////////////////

    @Test
    public void evalCLESAEnglishJapaneseMonolingual() {
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
                    new File("src/test/resources/com/iandadesign/closa/evaluation/preprocessed-t/test-bbc/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/preprocessed-t/test-bbc/ja"), "en"
            );

            englishJapaneseBBCEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLESAEnglishJapanese() {
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
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja"
            );

            englishJapaneseBBCEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void evalCLESAEnglishChinese() {
        /*
            Ranks 1 to 50

            Precision: [0.19646366, 0.19646366, 0.19646366, 0.19646366, 0.19646366, 0.19646366, 0.19646366]
            Recall: [0.19646366, 0.39292732, 0.589391, 0.9823183, 1.9646366, 3.9292731, 9.823183]
            F-Measure: [0.19646366, 0.26195154, 0.2946955, 0.32743946, 0.35720667, 0.37421653, 0.38522285]

            Mean reciprocal rank: 0.8839303218721857


            Aligned document similarities

            {0.0=50}

            {0.0=9.823183}
         */

        try {
            CLESAEvaluationSet englishChineseECCEEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void evalCLESAPan11Documents() {
        /*
            Ranks 1 to 50

            Precision: [0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2]
            Recall: [0.2, 0.4, 0.6, 1.0, 2.0, 4.0, 10.0]
            F-Measure: [0.2, 0.26666668, 0.3, 0.33333334, 0.36363637, 0.3809524, 0.3921569]

            Mean reciprocal rank: 0.8998410676658846


            Aligned document similarities

            {0.0=50}

            {0.0=10.0}
         */
        try {
            CLESAEvaluationSet englishSpanishPan11EvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es"
            );

            englishSpanishPan11EvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLESAJrcAcquisDocuments() {
        /*
            500

            Precision: [0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2]
            Recall: [0.2004008, 0.4008016, 0.60120237, 1.002004, 2.004008, 4.008016, 10.02004]
            F-Measure: [0.2002002, 0.2668446, 0.3001501, 0.3334445, 0.3637025, 0.3809887, 0.3921723]

            Mean reciprocal rank: 0.9016443563786423


            Aligned document similarities

            {0.0=50}

            {0.0=10.0}
         */
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
    public void evalCLESAEuroparlDocuments() {
        /*
            Precision: [0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2]
            Recall: [0.2, 0.4, 0.6, 1.0, 2.0, 4.0, 10.0]
            F-Measure: [0.2, 0.26666668, 0.3, 0.33333334, 0.36363637, 0.3809524, 0.3921569]

            Mean reciprocal rank: 0.8998410676658849


            Aligned document similarities

            {0.0=50}

            {0.0=10.0}
         */
        try {
            CLESAEvaluationSet englishFrenchEuroparlEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/fr"), "fr"
            );
            englishFrenchEuroparlEvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
