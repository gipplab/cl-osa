package com.iandadesign.closa.evaluation.impl;

import org.junit.jupiter.api.Test;

import java.io.File;

class CLASAEvaluationSetEval {

    String pathPrefix = "/home/marquart/eval_data/test";

    @Test
    void evalCLASAAspec() {
        CLASAEvaluationSet englishJapaneseASPECEvaluationSetCLASA = new CLASAEvaluationSet(
                new File(pathPrefix + "/ASPECx/en10000"), "en",
                new File(pathPrefix + "/ASPECx/ja10000"), "ja",
                2000
        );

        englishJapaneseASPECEvaluationSetCLASA.printEvaluation();
    }

    @Test
    void evalCLASAAspecChinese() {
        CLASAEvaluationSet englishJapaneseASPECEvaluationSetCLASA = new CLASAEvaluationSet(
                new File(pathPrefix + "/ASPECxc/ja10000"), "en",
                new File(pathPrefix + "/ASPECxc/zh10000"), "ja",
                2000
        );

        englishJapaneseASPECEvaluationSetCLASA.printEvaluation();
    }



    @Test
    void evalCLASAPan11Documents() {
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/pan11/en"), "en",
                    new File(pathPrefix + "/pan11/es"), "es",
                    2000
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAPan11Sentences() {
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/pan11/en"), "en",
                    new File(pathPrefix + "/sentences/pan11/es"), "es",
                    2000
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLASAJrcAcquisDocuments() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/jrc/en"), "en",
                    new File(pathPrefix + "/jrc/fr"), "fr",
                    1989
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAJrcAcquisSentences() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/jrc/en"), "en",
                    new File(pathPrefix + "/sentences/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAEuroparlDocuments() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAEuroparlSentences() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/europarl/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
