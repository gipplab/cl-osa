package com.iandadesign.closa.evaluation.impl;

import org.junit.jupiter.api.Test;

import java.io.File;

class GenericVectorEvaluationSetEval {

    String pathPrefix = "/Users/fabian/data/vectors";

    @Test
    /*
        Ranks 1 to 50

        Precision: [71.7, 40.075, 27.883333, 17.41, 9.035, 4.6675, 1.9239999]
        Recall: [71.7, 80.15, 83.65, 87.05, 90.35, 93.35, 96.200005]
        F-Measure: [71.7, 53.433334, 41.825, 29.016665, 16.427275, 8.890475, 3.772549]

        Mean reciprocal rank: 78.67065665492112

        Aligned document similarities

        {60.0=216, 80.0=355, 40.0=18, 30.0=2, 90.0=1, 50.0=67, 70.0=1341}

        {60.0=10.8, 80.0=17.75, 40.0=0.9, 30.0=0.1, 90.0=0.05, 50.0=3.35, 70.0=67.05}
     */
    void evalVectorsPan11Documents() {
        try {
            GenericVectorEvaluationSet evaluationSet = new GenericVectorEvaluationSet(
                    new File(pathPrefix + "/vectors_doc/en/conceptnet/pan"), "en",
                    new File(pathPrefix + "/vectors_doc/es/conceptnet/pan"), "es",
                    2000
            );

            evaluationSet.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /*
        Ranks 1 to 50

        Precision: [27.35, 18.35, 14.366667, 10.13, 6.205, 3.6474998, 1.689]
        Recall: [27.35, 36.7, 43.1, 50.65, 62.050003, 72.95, 84.45]
        F-Measure: [27.35, 24.466665, 21.55, 16.883333, 11.281818, 6.9476185, 3.3117647]

        Mean reciprocal rank: 38.734454682602426

        Aligned document similarities

        {60.0=912, 20.0=5, 30.0=5, 80.0=29, 40.0=54, 50.0=938, 70.0=57}

        {60.0=45.6, 20.0=0.25, 30.0=0.25, 80.0=1.45, 40.0=2.7, 50.0=46.9, 70.0=2.85}
     */
    void evalVectorsJrcDocuments() {
        try {
            GenericVectorEvaluationSet evaluationSet = new GenericVectorEvaluationSet(
                    new File(pathPrefix + "/vectors_doc/en/conceptnet/jrc"), "en",
                    new File(pathPrefix + "/vectors_doc/fr/conceptnet/jrc"), "fr",
                    2000
            );

            evaluationSet.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /*
        Ranks 1 to 50

        Precision: [32.274334, 19.692385, 14.657925, 10.025214, 5.8144226, 3.361069, 1.5632879]
        Recall: [32.274334, 39.38477, 43.973778, 50.12607, 58.144222, 67.22138, 78.1644]
        F-Measure: [32.274334, 26.256514, 21.986889, 16.70869, 10.571677, 6.4020357, 3.0652707]

        Mean reciprocal rank: 40.99152344814153


        Aligned document similarities

        {80.0=43, 10.0=31, 30.0=27, 40.0=283, 60.0=439, 20.0=29, 0.0=2, -10.0=1, 90.0=9, 50.0=861, 70.0=258}

        {80.0=2.1684318, 10.0=1.563288, 30.0=1.3615733, 40.0=14.271306, 60.0=22.138174, 20.0=1.4624307, 0.0=0.10085729, -10.0=0.050428644, 90.0=0.45385778, 50.0=43.419064, 70.0=13.01059}
     */
    void evalVectorsEuroparlDocuments() {
        try {
            GenericVectorEvaluationSet evaluationSet = new GenericVectorEvaluationSet(
                    new File(pathPrefix + "/vectors_doc/en/conceptnet/europarl"), "en",
                    new File(pathPrefix + "/vectors_doc/fr/conceptnet/europarl"), "fr",
                    2000
            );

            evaluationSet.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /*
        Ranks 1 to 50

        Precision: [22.949999, 15.325, 12.133333, 8.65, 5.29, 3.1800003, 1.5860001]
        Recall: [22.949999, 30.65, 36.399998, 43.25, 52.899998, 63.6, 79.299995]
        F-Measure: [22.949999, 20.43333, 18.199999, 14.416666, 9.618182, 6.0571437, 3.109804]

        Mean reciprocal rank: 33.03074771803877


        Aligned document similarities

        {40.0=938, 30.0=863, 20.0=79, 60.0=4, 10.0=2, 50.0=113, 70.0=1}

        {40.0=46.9, 30.0=43.15, 20.0=3.95, 60.0=0.2, 10.0=0.1, 50.0=5.65, 70.0=0.05}
     */
    void evalVectorsAspecEnJa() {
        try {
            GenericVectorEvaluationSet evaluationSet = new GenericVectorEvaluationSet(
                    new File(pathPrefix + "/vectors_doc/en/conceptnet/aspcx_en"), "en",
                    new File(pathPrefix + "/vectors_doc/ja/conceptnet/aspcx_ja"), "ja",
                    2000
            );

            evaluationSet.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /*
        Ranks 1 to 50

        Precision: [9.849999, 6.6749997, 5.3166666, 3.8899999, 2.49, 1.6125, 0.896]
        Recall: [9.849999, 13.349999, 15.950001, 19.45, 24.9, 32.25, 44.800003]
        F-Measure: [9.849999, 8.9, 7.9750004, 6.4833326, 4.527273, 3.0714285, 1.7568628]

        Mean reciprocal rank: 15.21729019468116

        Aligned document similarities

        {40.0=633, 60.0=45, 30.0=20, 50.0=1302}

        {40.0=31.65, 60.0=2.25, 30.0=1.0, 50.0=65.1}
     */
    void evalVectorsAspecJaZh() {
        try {
            GenericVectorEvaluationSet evaluationSet = new GenericVectorEvaluationSet(
                    new File(pathPrefix + "/vectors_doc/ja/conceptnet/aspcxc_ja"), "ja",
                    new File(pathPrefix + "/vectors_doc/zh/conceptnet/aspcxc_zh"), "zh",
                    2000
            );

            evaluationSet.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    /*
        Values for ranks 1 to 50:

        True positives: 1969
        Relevant elements: 1996
        Irrelevant elements: 0
        Collection size: 1996
        Selected elements: 99800
        False positives: 27
        False negatives: 27

        Ranks 1 to 50

        Precision: [88.82766, 46.242485, 31.212425, 18.987976, 9.634268, 4.8872747, 1.9729459]
        Recall: [88.82766, 92.48497, 93.637276, 94.93988, 96.34268, 97.74549, 98.64729]
        F-Measure: [88.82766, 61.656647, 46.818638, 31.64663, 17.516851, 9.309095, 3.8685215]

        Mean reciprocal rank: 91.68678687919179
     */
    void evalVectorsPan11Paragraphs() {
        try {
            GenericVectorEvaluationSet evaluationSet = new GenericVectorEvaluationSet(
                    new File(pathPrefix + "/vectors_par/en/conceptnet/pan"), "en",
                    new File(pathPrefix + "/vectors_par/es/conceptnet/pan"), "es",
                    2000
            );

            evaluationSet.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
