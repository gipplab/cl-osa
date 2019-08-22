package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class CLESAEvaluationSetEval {


    @Test
    void buildDatabase() {
        CLESAEvaluationSet.extractWikipediaArticlesAndStore();
    }


    /////////////////////////////////////////////////// CL-ESA //////////////////////////////////////////////////


    @Test
    void evalCLESAEnglishJapanese() {
        /*
           
            Concept space dimensionality 10000.
            Ranks 1 to 50

            Precision: [20.0, 15.000001, 11.851851, 8.888889, 5.555556, 3.1111112, 1.6222222]
            Recall: [20.0, 30.000002, 35.555557, 44.444447, 55.555557, 62.222225, 81.11111]
            F-Measure: [20.000002, 20.0, 17.777779, 14.814815, 10.10101, 5.9259257, 3.1808279]

            Mean reciprocal rank: 31.698998916244182


            Aligned document similarities

            {80.0=28, 0.0=1, 60.0=10, 50.0=2, 70.0=49}

            {80.0=31.11111, 0.0=1.1111112, 60.0=11.111111, 50.0=2.2222223, 70.0=54.444443}
         */
        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja",
            );

            englishJapaneseBBCEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAAspec() {
        CLESAEvaluationSet englishJapaneseASPECEvaluationSetCLOSA = new CLESAEvaluationSet(
                new File(System.getProperty("user.home") + "/ASPECx/ja"), "ja",
                new File(System.getProperty("user.home") + "/ASPECx/en"), "en",
                10000
        );

        englishJapaneseASPECEvaluationSetCLOSA.printEvaluation();
    }

    @Test
    void evalCLESAAspecChinese() {
        CLESAEvaluationSet chineseJapaneseASPECEvaluationSetCLOSA = new CLESAEvaluationSet(
                new File(System.getProperty("user.home") + "/ASPECxc/ja"), "ja",
                new File(System.getProperty("user.home") + "/ASPECxc/zh"), "zh",
                10000
        );

        chineseJapaneseASPECEvaluationSetCLOSA.printEvaluation();
    }

    @Test
    void evalCLESAEnglishChinese() {
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
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLESAPan11Documents() {
        /*
            Ranks 1 to 50

            Precision: [1.4000001, 0.9, 0.6666667, 0.48000002, 0.52, 0.37, 0.352]
            Recall: [1.4000001, 1.8, 2.0, 2.4, 5.2000003, 7.4, 17.6]
            F-Measure: [1.4000001, 1.1999999, 1.0, 0.8, 0.94545454, 0.7047618, 0.69019616]

            Mean reciprocal rank: 3.071126406984371


            Aligned document similarities

            {80.0=264, 60.0=54, 30.0=4, 40.0=5, 90.0=76, 50.0=23, 70.0=74}

            {80.0=52.8, 60.0=10.8, 30.0=0.8, 40.0=1.0, 90.0=15.2, 50.0=4.6, 70.0=14.8}
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
    void evalCLESAPan11Sentences() {
        /*
            Ranks 1 to 50

            Precision: [1.0, 0.9, 0.6666667, 0.6, 0.56, 0.49, 0.37199998]
            Recall: [1.0, 1.8, 2.0, 3.0, 5.6000004, 9.8, 18.6]
            F-Measure: [1.0, 1.1999999, 1.0, 1.0000001, 1.0181818, 0.9333333, 0.7294117]

            Mean reciprocal rank: 3.062407209149037


            Aligned document similarities

            {80.0=208, 40.0=21, 60.0=60, 30.0=11, 20.0=4, 10.0=1, 0.0=1, 90.0=73, 50.0=44, 70.0=77}

            {80.0=41.6, 40.0=4.2, 60.0=12.0, 30.0=2.2, 20.0=0.8, 10.0=0.2, 0.0=0.2, 90.0=14.6, 50.0=8.8, 70.0=15.4}

         */
        try {
            CLESAEvaluationSet englishSpanishPan11EvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/es"), "es"
            );

            englishSpanishPan11EvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAPan11Chunks() {
        /*

         */
        try {
            CLESAEvaluationSet englishSpanishPan11EvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/es"), "es"
            );

            englishSpanishPan11EvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAJrcAcquisDocuments() {
        /*

            Ranks 1 to 50

            Precision: [3.539823, 4.8672566, 4.424779, 3.3628318, 2.6548672, 2.1238937, 1.5221239]
            Recall: [3.539823, 9.734513, 13.274336, 16.81416, 26.548672, 42.477875, 76.10619]
            F-Measure: [3.539823, 6.4896765, 6.637168, 5.6047196, 4.827031, 4.045512, 2.9845567]

            Mean reciprocal rank: 12.339210435032864


            Aligned document similarities

            {60.0=53, 40.0=3, 80.0=5, 50.0=10, 70.0=42}

            {60.0=46.902657, 40.0=2.6548672, 80.0=4.424779, 50.0=8.849558, 70.0=37.16814}

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
    void evalCLESAEuroparlDocuments() {
        /*
            Ranks 1 to 50

            Precision: [13.432837, 9.701492, 6.9651737, 5.671642, 4.1791043, 2.7611942, 1.7014927]
            Recall: [13.432837, 19.402985, 20.895523, 28.35821, 41.791046, 55.22388, 85.07463]
            F-Measure: [13.432837, 12.935324, 10.447762, 9.452736, 7.598371, 5.2594175, 3.33626]

            Mean reciprocal rank: 22.380372176260945


            Aligned document similarities

            {40.0=16, 60.0=4, 0.0=3, 20.0=7, 30.0=11, 10.0=2, 80.0=2, 50.0=6, 70.0=16}

            {40.0=23.880596, 60.0=5.970149, 0.0=4.477612, 20.0=10.447762, 30.0=16.41791, 10.0=2.9850745, 80.0=2.9850745, 50.0=8.955224, 70.0=23.880596}
         */
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
