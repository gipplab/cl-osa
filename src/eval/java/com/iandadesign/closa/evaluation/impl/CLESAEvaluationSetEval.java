package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class CLESAEvaluationSetEval {

    String pathPrefix = "/home/marquart/eval_data/test";

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
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/fr"), "fr",
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
