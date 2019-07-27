package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class CLCNGEvaluationSetEval {

    @Test
    public void evalCLCNGEnglishJapanese() {
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
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja");

            englishJapaneseBBCEvaluationSetCLCNG.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCNGEnglishChinese() {
        /*
            Precision: [98.23183, 49.50884, 33.005894, 19.842829, 9.921414, 4.960707, 1.984283]
            Recall: [98.23183, 99.01768, 99.01768, 99.21414, 99.21414, 99.21414, 99.21414]
            F-Measure: [98.23183, 66.01179, 49.508842, 33.07138, 18.038937, 9.448966, 3.890751]

            Mean reciprocal rank: 98.67567758325117


            Aligned document similarities

            {60.0=346, 40.0=3, 20.0=1, 10.0=1, 50.0=35, 70.0=123}

            {10.0=0.19646366, 20.0=0.19646366, 40.0=0.58939093, 60.0=67.976425, 50.0=6.876228, 70.0=24.16503}
         */
        try {
            EvaluationSet englishChineseECCEEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLCNG.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCNGPan11Documents() {
        /*
            Ranks 1 to 50

            Precision: [35.0, 19.8, 13.733334, 9.12, 4.9, 2.6100001, 1.076]
            Recall: [35.0, 39.6, 41.2, 45.6, 49.0, 52.2, 53.8]
            F-Measure: [35.0, 26.400003, 20.6, 15.2, 8.909091, 4.971429, 2.109804]

            Mean reciprocal rank: 39.67356806548


            Aligned document similarities

            {40.0=67, 60.0=158, 30.0=32, 80.0=5, 50.0=87, 70.0=19}

            {80.0=1.0, 30.0=6.4, 60.0=31.6, 40.0=13.4, 50.0=17.4, 70.0=3.8}
         */
        try {
            CLCNGEvaluationSet englishSpanishPan11EvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es",
                    500
            );
            englishSpanishPan11EvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCNGJrcAcquisDocuments() {
        /*
            Ranks 1 to 50

            Precision: [92.0, 46.9, 31.866667, 19.28, 9.74, 4.89, 1.956]
            Recall: [92.0, 93.8, 95.6, 96.4, 97.399994, 97.799995, 97.799995]
            F-Measure: [92.0, 62.533337, 47.8, 32.13333, 17.709093, 9.314287, 3.8352945]

            Mean reciprocal rank: 93.85399839437963


            Aligned document similarities

            {40.0=169, 60.0=79, 30.0=12, 80.0=1, 50.0=225, 70.0=6}

            {80.0=0.2, 30.0=2.4, 60.0=15.8, 40.0=33.8, 50.0=45.0, 70.0=1.2}

         */
        try {
            CLCNGEvaluationSet englishFrenchJrcAcquisEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"), "fr",
                    500
            );
            englishFrenchJrcAcquisEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void evalCLCNGEuroparlDocuments() {
        /*
            Ranks 1 to 50

            Precision: [23.991936, 12.298388, 8.333334, 5.040323, 2.560484, 1.2903225, 0.5241936]
            Recall: [23.800001, 24.4, 24.8, 25.0, 25.400002, 25.6, 26.0]
            F-Measure: [23.895582, 16.353886, 12.474849, 8.389261, 4.6520143, 2.4568138, 1.027668]

            Mean reciprocal rank: 24.44609418197027


            Aligned document similarities

            {0.0=65, 60.0=77, 40.0=19, 30.0=12, 50.0=12, 70.0=15}

            {30.0=2.4, 40.0=3.8, 60.0=15.4, 0.0=13.0, 50.0=2.4, 70.0=3.0}
         */

        try {
            CLCNGEvaluationSet englishFrenchEuroparlEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/fr"), "fr",
                    500
            );
            englishFrenchEuroparlEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
