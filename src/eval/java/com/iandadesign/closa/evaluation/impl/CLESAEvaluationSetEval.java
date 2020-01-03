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
        try {
            CLESAEvaluationSet englishSpanishPan11EvaluationSetCLESA = new CLESAEvaluationSet(
                    new File(pathPrefix + "/pan11/en"), "en",
                    new File(pathPrefix + "/pan11/es"), "es",
                    2000
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
