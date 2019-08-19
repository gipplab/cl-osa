package com.iandadesign.closa.evaluation.impl;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

    @Test
    void evalCLOSAAspec() {
            CLOSAEvaluationSet englishJapaneseASPECEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/ASPECx/en"), "en",
                    new File(System.getProperty("user.home") + "/ASPECx/ja"), "ja",
                    10000
            );

        englishJapaneseASPECEvaluationSetCLOSA.setGraphBasedAnalysis(true);
        englishJapaneseASPECEvaluationSetCLOSA.printEvaluation();
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
                    new File(System.getProperty("user.home")
                            + "/closa/src/eval/resources/com/iandadesign/closa/evaluation/ECCE/en"),
                    "en",
                    new File(System.getProperty("user.home")
                            + "/closa/src/eval/resources/com/iandadesign/closa/evaluation/ECCE/zh"),
                    "zh"
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
            Ranks 1 to 50

            Precision: [95.41096, 48.647263, 32.591324, 19.643835, 9.866438, 4.950342, 1.9869862]
            Recall: [95.41096, 97.294525, 97.77397, 98.21918, 98.66438, 99.00685, 99.34931]
            F-Measure: [95.41096, 64.863014, 48.88699, 32.739727, 17.938978, 9.429224, 3.8960516]

            Mean reciprocal rank: 96.71354945366922


            Aligned document similarities

            {30.0=1895, 40.0=315, 20.0=568, 10.0=117, 0.0=14, 50.0=11}

            {30.0=64.89726, 40.0=10.787671, 20.0=19.452055, 10.0=4.0068493, 0.0=0.47945204, 50.0=0.37671232}
         */

        try {
            CLOSAEvaluationSet englishSpanishPan11EvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es"
            );

            englishSpanishPan11EvaluationSetCLOSA.setGraphBasedAnalysis(true);
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
            Ranks 1 to 50

            Precision: [93.987976, 47.895794, 32.06413, 19.318638, 9.699399, 4.8597193, 1.9519038]
            Recall: [93.8, 95.6, 96.0, 96.4, 96.8, 97.0, 97.399994]
            F-Measure: [93.89389, 63.818424, 48.0721, 32.186977, 17.63206, 9.255725, 3.8271117]

            Mean reciprocal rank: 95.00687178457385


            Aligned document similarities

            {30.0=287, 10.0=30, 20.0=111, 40.0=54, 0.0=12, 50.0=4, 70.0=1}

            {30.0=57.4, 10.0=6.0, 20.0=22.2, 40.0=10.8, 0.0=2.4, 50.0=0.8, 70.0=0.2}
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
        /*
            Ranks 1 to 50

            Precision: [98.4, 49.399998, 33.0, 19.92, 10.0, 5.0, 2.0]
            Recall: [98.4, 98.799995, 99.0, 99.6, 100.0, 100.0, 100.0]
            F-Measure: [98.4, 65.86666, 49.499996, 33.2, 18.181818, 9.52381, 3.9215689]

            Mean reciprocal rank: 98.86333333333333


            Aligned document similarities

            {40.0=191, 30.0=254, 20.0=33, 60.0=1, 50.0=20, 70.0=1}

            {40.0=38.2, 30.0=50.8, 20.0=6.6, 60.0=0.2, 50.0=4.0, 70.0=0.2}



         */
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

    @Test
    void prepareASPEC() {
        File enJaFile = new File(System.getProperty("user.home") + "/ASPEC/ASPEC-JE/train/train-1.txt");

        Map<String, Map<Integer, String>> idEnglishDocumentMap = new HashMap<>();
        Map<String, Map<Integer, String>> idJapaneseDocumentMap = new HashMap<>();

        try {
            FileUtils.readLines(enJaFile, StandardCharsets.UTF_8)
                    .forEach((String line) -> {
                        String[] parts = line.split("\\|\\|\\|");
                        String id = parts[1].trim();
                        int sentenceNumber = Integer.parseInt(parts[2].trim());
                        String japanese = parts[3].trim();
                        String english = parts[4].trim();

                        if (!idEnglishDocumentMap.containsKey(id)) {
                            idEnglishDocumentMap.put(id, new HashMap<>());
                        }

                        idEnglishDocumentMap.get(id).put(sentenceNumber, english);

                        if (!idJapaneseDocumentMap.containsKey(id)) {
                            idJapaneseDocumentMap.put(id, new HashMap<>());
                        }

                        idJapaneseDocumentMap.get(id).put(sentenceNumber, japanese);
                    });

            idEnglishDocumentMap.forEach((key, value) -> {
                try {
                    File file = new File(System.getProperty("user.home") + "/ASPECx/en/" + key + ".txt");
                    Files.createParentDirs(file);
                    FileUtils.writeLines(file, value.values());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            idJapaneseDocumentMap.forEach((key, value) -> {
                try {
                    File file = new File(System.getProperty("user.home") + "/ASPECx/ja/" + key + ".txt");
                    Files.createParentDirs(file);
                    FileUtils.writeLines(file, value.values());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Test
    void prepareASPECChinese() {
        File zhJaFile = new File(System.getProperty("user.home") + "/ASPEC/ASPEC-JC/train/train.txt");

        Map<String, Map<String, String>> idChineseDocumentMap = new HashMap<>();
        Map<String, Map<String, String>> idJapaneseDocumentMap = new HashMap<>();

        try {
            FileUtils.readLines(zhJaFile, StandardCharsets.UTF_8)
                    .forEach((String line) -> {
                        String[] parts = line.split("\\|\\|\\|");
                        String id = parts[0].trim();

                        String[] idParts = id.split("-");
                        String sentenceNumber = idParts[idParts.length - 2] + "-" + idParts[idParts.length - 1];
                        idParts[idParts.length - 1] = "";
                        idParts[idParts.length - 2] = "";

                        id = String.join("-", idParts);

                        String japanese = parts[1].trim();
                        String chinese = parts[2].trim();

                        if (!idChineseDocumentMap.containsKey(id)) {
                            idChineseDocumentMap.put(id, new HashMap<>());
                        }

                        idChineseDocumentMap.get(id).put(sentenceNumber, chinese);

                        if (!idJapaneseDocumentMap.containsKey(id)) {
                            idJapaneseDocumentMap.put(id, new HashMap<>());
                        }

                        idJapaneseDocumentMap.get(id).put(sentenceNumber, japanese);
                    });

            idChineseDocumentMap.forEach((key, value) -> {
                try {
                    File file = new File(System.getProperty("user.home") + "/ASPECxc/zh/" + key + ".txt");
                    Files.createParentDirs(file);
                    FileUtils.writeLines(file, value.values());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            idJapaneseDocumentMap.forEach((key, value) -> {
                try {
                    File file = new File(System.getProperty("user.home") + "/ASPECxc/ja/" + key + ".txt");
                    Files.createParentDirs(file);
                    FileUtils.writeLines(file, value.values());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
