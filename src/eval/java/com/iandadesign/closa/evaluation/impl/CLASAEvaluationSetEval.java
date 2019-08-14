package com.iandadesign.closa.evaluation.impl;

import org.junit.jupiter.api.Test;

import java.io.File;

class CLASAEvaluationSetEval {

    @Test
    void evalCLASAEnglishChinese() {
        /*
         */

        CLASAEvaluationSet englishChineseECCEEvaluationSetCLASA = new CLASAEvaluationSet(
                new File("src/eval/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                new File("src/eval/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh",
                500
        );
        englishChineseECCEEvaluationSetCLASA.printEvaluation();
    }


    @Test
    void evalCLASAPan11Documents() {
        /*
           Ranks 1 to 50

            Precision: [64.4, 34.5, 23.800001, 14.96, 8.02, 4.3, 1.8479999]
            Recall: [64.4, 69.0, 71.4, 74.8, 80.2, 86.0, 92.4]
            F-Measure: [64.4, 46.0, 35.7, 24.933334, 14.581819, 8.190477, 3.6235292]

            Mean reciprocal rank: 69.69919188569743


            Aligned document similarities

            {0.0=203, 20.0=30, 40.0=46, 10.0=113, 30.0=37, 60.0=23, 80.0=2, 50.0=37, 70.0=9}

            {0.0=40.6, 20.0=6.0, 40.0=9.2, 10.0=22.6, 30.0=7.4, 60.0=4.6, 80.0=0.4, 50.0=7.4, 70.0=1.8}
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
        /*
           Ranks 1 to 50

            Precision: [63.6, 34.5, 23.933332, 14.839999, 7.9000006, 4.23, 1.8479999]
            Recall: [63.6, 69.0, 71.8, 74.2, 79.0, 84.6, 92.4]
            F-Measure: [63.6, 46.0, 35.899998, 24.733332, 14.363638, 8.057143, 3.6235292]

            Mean reciprocal rank: 69.18055259272658


            Aligned document similarities

            {0.0=245, 40.0=32, 10.0=96, 30.0=36, 20.0=23, 60.0=17, 80.0=6, 90.0=1, 50.0=27, 100.0=1, 70.0=16}

            {0.0=49.0, 40.0=6.4, 10.0=19.2, 30.0=7.2, 20.0=4.6, 60.0=3.4, 80.0=1.2, 90.0=0.2, 50.0=5.4, 100.0=0.2, 70.0=3.2}

         */
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
        /*
            Ranks 1 to 50

            Precision: [56.199997, 30.4, 21.533333, 13.719999, 7.48, 4.2599998, 1.916]
            Recall: [56.199997, 60.8, 64.600006, 68.6, 74.8, 85.2, 95.8]
            F-Measure: [56.199997, 40.533333, 32.3, 22.866667, 13.6, 8.114285, 3.7568629]

            Mean reciprocal rank: 62.67319319666734


            Aligned document similarities

            {0.0=224, 10.0=224, 20.0=31, 30.0=13, 40.0=2, 60.0=2, 80.0=1, 50.0=2, 70.0=1}

            {0.0=44.8, 10.0=44.8, 20.0=6.2, 30.0=2.6, 40.0=0.4, 60.0=0.4, 80.0=0.2, 50.0=0.4, 70.0=0.2}   */
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
