package com.iandadesign.closa.evaluation.impl;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class CLASAEvaluationSetEval {

    String pathPrefix = "/data/test";

    @Test
    void evalCLASAEnglishJapanese() {
        /*
            Ranks 1 to 50

            Precision: [77.77778, 42.22222, 28.88889, 18.0, 9.444445, 4.833333, 1.9777778]
            Recall: [77.77778, 84.44444, 86.666664, 90.0, 94.44444, 96.666664, 98.888885]
            F-Measure: [77.77777, 56.296295, 43.333332, 30.000002, 17.171719, 9.206348, 3.877996]

            Mean reciprocal rank: 83.46916977001935


            Aligned document similarities

            {60.0=8, 40.0=19, 30.0=15, 80.0=7, 20.0=12, 10.0=9, 0.0=2, 90.0=1, 50.0=13, 100.0=1, 70.0=3}

            {60.0=8.888889, 40.0=21.11111, 30.0=16.666666, 80.0=7.7777777, 20.0=13.333333, 10.0=10.0, 0.0=2.2222223, 90.0=1.1111112, 50.0=14.444445, 100.0=1.1111112, 70.0=3.3333333}

         */
        try {
            CLASAEvaluationSet englishJapaneseBBCEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/closa/src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File(pathPrefix + "/closa/src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja"
            );
            englishJapaneseBBCEvaluationSetCLASA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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
    void evalCLASAEnglishChinese() {
        /*
            Ranks 1 to 50

            Precision: [70.0, 36.6, 24.666668, 14.96, 7.5400004, 3.81, 1.544]
            Recall: [70.0, 73.2, 74.0, 74.8, 75.4, 76.200005, 77.200005]
            F-Measure: [70.0, 48.8, 37.0, 24.933334, 13.709092, 7.2571425, 3.027451]

            Mean reciprocal rank: 72.3303432121858


            Aligned document similarities

            {20.0=148, 0.0=120, 30.0=134, 60.0=6, 40.0=57, 10.0=16, 50.0=17, 100.0=1, 70.0=1}

            {20.0=29.6, 0.0=24.0, 30.0=26.8, 60.0=1.2, 40.0=11.4, 10.0=3.2, 50.0=3.4, 100.0=0.2, 70.0=0.2}
         */

        try {
            CLASAEvaluationSet englishChineseECCEEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/closa/src/eval/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File(pathPrefix + "/closa/src/eval/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLASA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    new File(pathPrefix + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/en"), "en",
                    new File(pathPrefix + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/es"), "es"
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAPan11Chunks() {
        /*
         */
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/en"), "en",
                    new File(pathPrefix + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/es"), "es"
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
                    new File(pathPrefix + "/jrc/en"), "en",
                    new File(pathPrefix + "/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAEuroparl() {
        /*
            Ranks 1 to 50

            Precision: [20.4, 12.700001, 9.533333, 7.0, 4.5, 3.06, 1.672]
            Recall: [20.4, 25.400002, 28.600002, 35.0, 45.0, 61.199997, 83.600006]
            F-Measure: [20.400002, 16.933332, 14.300001, 11.666667, 8.181818, 5.8285713, 3.2784314]

            Mean reciprocal rank: 28.75150835340408


            Aligned document similarities

            {0.0=499, 10.0=1}

            {0.0=99.8, 10.0=0.2}

         */
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/europarl/en"), "en",
                    new File(pathPrefix + "/europarl/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
