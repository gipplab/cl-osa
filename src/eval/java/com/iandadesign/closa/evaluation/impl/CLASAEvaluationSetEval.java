package com.iandadesign.closa.evaluation.impl;

import org.junit.jupiter.api.Test;

import java.io.File;

class CLASAEvaluationSetEval {

    @Test
    void evalCLASAPan11Documents() {
        /*
         * Ranks 1 to 50
         *
         * Precision: [10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0]
         * Recall: [10.0, 20.0, 30.000002, 50.0, 100.0, 100.0, 100.0]
         * F-Measure: [10.000001, 13.333334, 15.000001, 16.666666, 18.181818, 18.181818, 18.181818]
         *
         * Mean reciprocal rank: 29.289682539682545
         *
         *
         * Aligned document similarities
         *
         * {0.0=10}
         *
         * {0.0=100.0}
         */
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es",
                    500
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
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/es"), "es",
                    500
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAJrcAcquis() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"), "fr",
                    500
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAEuroparl() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/fr"), "fr",
                    500
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
