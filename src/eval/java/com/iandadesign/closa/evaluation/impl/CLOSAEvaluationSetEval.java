package com.iandadesign.closa.evaluation.impl;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class CLOSAEvaluationSetEval {


    ///////////////////////////////////////////////// New Stuff /////////////////////////////////////////////////

    // Translation and Tokenization with Named Entity Recognition and POS-Tagging
    // disambiguate using properties of wikidata entities (e.g. throw away named entities)
    // Hypothesis: • if something is instanceOf something, it is a NE
    //             • if something is subclassOf something, it is not a NE


    ////////////////////////////////////////////////// CL-OSA ///////////////////////////////////////////////////

    /**
     * Tests T+OSA with VroniPlag, core documents only.
     * <p>
     * Granularity: full documents.
     */
    @Test
    void evalCLOSAVroniPlag() {
        /*
            Cross-language analysis. With cosine similarity.

            True positives: 219
            Relevant elements: 309
            Irrelevant elements: 0
            Collection size: 309
            Selected elements: 315
            False positives: 90
            False negatives: 90


            Precision: 0.6952381
            Recall: 0.70873785
            F-Measure: 0.701923063860253


                #Server 2019/02/23:

            True positives: 183
            Relevant elements: 309
            Irrelevant elements: 0
            Collection size: 309
            Selected elements: 306
            False positives: 123
            False negatives: 126

            Ranks 1 to 1

            Precision: [0.5980392]
            Recall: [0.592233]
            F-Measure: [0.5951219]


                #MacBook Pro 2019/02/24:

            Values for ranks 1 to 1:

            True positives: 214
            Relevant elements: 309
            Irrelevant elements: 0
            Collection size: 309
            Selected elements: 307
            False positives: 93
            False negatives: 95

            Ranks 1 to 1

            Precision: [0.6970684]
            Recall: [0.6925566]
            F-Measure: [0.69480515]
        */

        try {
            CLOSAEvaluationSet vroniPlagEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/test-vroniplag/fragments-text"),
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/test-vroniplag/sources-text")
            );

            vroniPlagEvaluationSetCLOSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tests CL-OSA with BBC English-Japanese, core documents only.
     * <p>
     * Granularity: full documents.
     */
    @Test
    void evalCLOSAEnglishJapaneseCosineSimilarity() {
        /*
            Cross-language analysis. With cosine similarity (graph-based analysis true).

            True positives: 90
            Relevant elements: 94
            Irrelevant elements: 0
            Collection size: 94
            Selected elements: 97
            False positives: 4
            False negatives: 4


            Precision: 0.92783505
            Recall: 0.9574468
            F-Measure: 0.942408388550284
         */
        try {
            CLOSAEvaluationSet englishJapaneseBBCEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja"
            );

            englishJapaneseBBCEvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishJapaneseBBCEvaluationSetCLOSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Tests CL-OSA with ECCE, core documents only.
     * <p>
     * Granularity: full documents.
     */
    @Test
    void evalCLOSAEnglishChineseCosineSimilarity() {
        /*
            Cross-language analysis. With cosine similarity.

            True positives: 505
            Relevant elements: 509
            Irrelevant elements: 0
            Collection size: 509
            Selected elements: 512
            False positives: 4
            False negatives: 4

            Precision: 0.9863281
            Recall: 0.9921414
            F-Measure: 0.9892262309534853
        */

        try {
            CLOSAEvaluationSet englishChineseECCEEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File("src/eval/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishChineseECCEEvaluationSetCLOSA.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLOSAPan11Documents() {
        /*
            True positives: 2794
            Relevant elements: 2920
            Irrelevant elements: 0
            Collection size: 2920
            Selected elements: 2920
            False positives: 126
            False negatives: 126

            Ranks 1 to 1

            Precision: [0.95684934]
            Recall: [0.95684934]
            F-Measure: [0.95684934]
         */

        try {
            CLOSAEvaluationSet englishSpanishPan11EvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es"
            );
            // englishSpanishPan11EvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishSpanishPan11EvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLOSAPan11Chunks() {
        /*
            True positives: 214
            Relevant elements: 692
            Irrelevant elements: 0
            Collection size: 692
            Selected elements: 436
            False positives: 222
            False negatives: 478

            Ranks 1 to 1

            Precision: [0.49082568]
            Recall: [0.30924857]
            F-Measure: [0.37943262]
         */
        try {
            CLOSAEvaluationSet englishSpanishPan11EvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/es"), "es"
            );
            englishSpanishPan11EvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLOSAPan11Sentences() {
        /*
            True positives: 2381
            Relevant elements: 2669
            Irrelevant elements: 0
            Collection size: 2669
            Selected elements: 2657
            False positives: 276
            False negatives: 288

            Ranks 1 to 1

            Precision: [0.89612347]
            Recall: [0.89209443]
            F-Measure: [0.89410436]
         */

        try {
            CLOSAEvaluationSet englishSpanishPan11EvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/es"), "es"
            );
            englishSpanishPan11EvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLOSAJrcAcquisDocuments() {
        try {
            CLOSAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"), "fr"
            );
            englishFrenchJrcAcquisEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLOSAEuroparlDocuments() {
        /*
            True positives: 4317
            Relevant elements: 9428
            Irrelevant elements: 0
            Collection size: 9428
            Selected elements: 8937
            False positives: 4620
            False negatives: 5111

            Ranks 1 to 1

            Precision: [0.483048]
            Recall: [0.45789137]
            F-Measure: [0.4701334]
         */
        try {
            CLOSAEvaluationSet englishFrenchEuroparlEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/fr"), "fr"
            );
            englishFrenchEuroparlEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void evalCLOSAConferencePapersDocuments() {
        /*
            True positives: 459
            Relevant elements: 616
            Irrelevant elements: 0
            Collection size: 616
            Selected elements: 616
            False positives: 157
            False negatives: 157

            Ranks 1 to 1

            Precision: [0.7451299]
            Recall: [0.7451299]
            F-Measure: [0.7451298]

            TODO: this result is bad because the document pairs of this corpus are not aligned correctly
         */
        try {
            CLOSAEvaluationSet englishFrenchConferencePapersEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Conference_papers/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Conference_papers/fr"), "fr"
            );
            englishFrenchConferencePapersEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalSTS() {
        try {
            CLOSAEvaluationSet stsEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/sts2016/txt"), "L", "R"
            );
            stsEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
