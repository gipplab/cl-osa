package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class CLESAEvaluationSetEval {

    String pathPrefix = "/data/test";

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
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja"
            );

            englishJapaneseBBCEvaluationSetCLESA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAAspec() {
        CLESAEvaluationSet englishJapaneseASPECEvaluationSetCLOSA = new CLESAEvaluationSet(
                new File(pathPrefix + "/ASPECx/ja10000"), "ja",
                new File(pathPrefix + "/ASPECx/en10000"), "en",
                2000
        );

        englishJapaneseASPECEvaluationSetCLOSA.printEvaluation();
    }

    @Test
    void evalCLESAAspecChinese() {
        CLESAEvaluationSet chineseJapaneseASPECEvaluationSetCLOSA = new CLESAEvaluationSet(
                new File(pathPrefix + "/ASPECxc/ja10000"), "ja",
                new File(pathPrefix + "/ASPECxc/zh10000"), "zh",
                2000
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
                    new File(pathPrefix + "/pan11/en"), "en",
                    new File(pathPrefix + "/pan11/es"), "es",
                    1000
            );

            englishSpanishPan11EvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLESAPan11Sentences() {
        try {
            CLESAEvaluationSet englishSpanishPan11EvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(pathPrefix + "/sentences/pan11/en"), "en",
                    new File(pathPrefix + "/sentences/pan11/es"), "es",
                    2000
            );

            englishSpanishPan11EvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAJrcAcquisDocuments() {
        try {
            CLESAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(pathPrefix + "/jrc/en"), "en",
                    new File(pathPrefix + "/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAJrcAcquisSentences() {
        try {
            CLESAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(pathPrefix + "/sentences/jrc/en"), "en",
                    new File(pathPrefix + "/sentences/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAEuroparlDocuments() {
        try {
            CLESAEvaluationSet englishFrenchEuroparlEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(pathPrefix + "/europarl/en"), "en",
                    new File(pathPrefix + "/europarl/fr"), "fr",
                    2000
            );
            englishFrenchEuroparlEvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLESAEuroparlSentences() {
        try {
            CLESAEvaluationSet englishFrenchEuroparlEvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(pathPrefix + "/sentences/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/europarl/fr"), "fr",
                    2000
            );
            englishFrenchEuroparlEvaluationSetCLESA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
