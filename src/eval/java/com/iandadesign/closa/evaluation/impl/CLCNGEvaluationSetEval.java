package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.TokenUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class CLCNGEvaluationSetEval {

    @Test
    public void evalCLCNGEnglishJapanese() {
        /*
            Ranks 1 to 50

            Precision: [78.49463, 42.473118, 28.315413, 18.064516, 9.247312, 4.784946, 1.9784946]
            Recall: [77.65958, 84.04256, 84.04256, 89.3617, 91.489365, 94.680855, 97.87234]
            F-Measure: [78.07487, 56.428574, 42.35925, 30.053669, 16.796875, 9.109519, 3.8785834]

            Mean reciprocal rank: 82.56386671215749

            Aligned document similarities

            {40.0=27, 60.0=3, 30.0=3, 50.0=60}

            {30.0=3.1914895, 60.0=3.1914895, 40.0=28.723404, 50.0=63.82979}
        */
        try {
            EvaluationSet englishJapaneseBBCEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), "ja");

            englishJapaneseBBCEvaluationSetCLCNG.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLCNGAspec() {
        CLCNGEvaluationSet englishJapaneseASPECEvaluationSetCLCNG = new CLCNGEvaluationSet(
                new File(System.getProperty("user.home") + "/ASPECx/ja"), "ja",
                new File(System.getProperty("user.home") + "/ASPECx/en"), "en",
                500
        );

        englishJapaneseASPECEvaluationSetCLCNG.printEvaluation();
    }

    @Test
    void evalCLCNGAspecChinese() {
        CLCNGEvaluationSet chineseJapaneseASPECEvaluationSetCLCNG = new CLCNGEvaluationSet(
                new File(System.getProperty("user.home") + "/ASPECxc/ja"), "ja",
                new File(System.getProperty("user.home") + "/ASPECxc/zh"), "zh",
                10000
        );

        chineseJapaneseASPECEvaluationSetCLCNG.printEvaluation();
    }

    @Test
    public void evalCLCNGEnglishChinese() {
        /*
            Precision: [98.23183, 49.50884, 33.005894, 19.842829, 9.921414, 4.960707, 1.984283]
            Recall: [98.23183, 99.01768, 99.01768, 99.21414, 99.21414, 99.21414, 99.21414]
            F-Measure: [98.23183, 66.01179, 49.508842, 33.07138, 18.038937, 9.448966, 3.890751]

            Mean reciprocal rank: 98.67567758325117


            Aligned document similarities

            {60.0=346, 40.0=3, 20.0=1, 10.0=1, 50.0=35, 70.0=123}

            {10.0=0.19646366, 20.0=0.19646366, 40.0=0.58939093, 60.0=67.976425, 50.0=6.876228, 70.0=24.16503}
         */
        try {
            EvaluationSet englishChineseECCEEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/en"), "en",
                    new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE/zh"), "zh"
            );
            englishChineseECCEEvaluationSetCLCNG.printEvaluation();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCNGPan11Documents() {
        /*
            Ranks 1 to 50

            Precision: [35.0, 19.8, 13.733334, 9.12, 4.9, 2.6100001, 1.076]
            Recall: [35.0, 39.6, 41.2, 45.6, 49.0, 52.2, 53.8]
            F-Measure: [35.0, 26.400003, 20.6, 15.2, 8.909091, 4.971429, 2.109804]

            Mean reciprocal rank: 39.67356806548


            Aligned document similarities

            {40.0=67, 60.0=158, 30.0=32, 80.0=5, 50.0=87, 70.0=19}

            {80.0=1.0, 30.0=6.4, 60.0=31.6, 40.0=13.4, 50.0=17.4, 70.0=3.8}
         */
        try {
            CLCNGEvaluationSet englishSpanishPan11EvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/PAN11/es"), "es"
            );
            englishSpanishPan11EvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCNGPan11Sentences() {
        /*
            Ranks 1 to 50

            Precision: [40.6, 21.1, 14.266666, 8.76, 4.58, 2.3600001, 0.984]
            Recall: [40.6, 42.2, 42.8, 43.8, 45.8, 47.2, 49.2]
            F-Measure: [40.6, 28.13333, 21.4, 14.599999, 8.327271, 4.4952383, 1.9294119]

            Mean reciprocal rank: 42.36612540375742


            Aligned document similarities

            {60.0=142, 40.0=68, 30.0=37, 50.0=54}

            {60.0=28.4, 40.0=13.6, 30.0=7.4, 50.0=10.8}
         */

        try {
            CLCNGEvaluationSet englishSpanishPan11EvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/sentences/PAN11/es"), "es"
            );
            englishSpanishPan11EvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCNGPan11Chunks() {
        /*

         */

        try {
            CLCNGEvaluationSet englishSpanishPan11EvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/chunks/PAN11/es"), "es"
            );
            englishSpanishPan11EvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void evalCLCNGJrcAcquisDocuments() {
        /*
            Ranks 1 to 50

            Precision: [92.0, 46.9, 31.866667, 19.28, 9.74, 4.89, 1.956]
            Recall: [92.0, 93.8, 95.6, 96.4, 97.399994, 97.799995, 97.799995]
            F-Measure: [92.0, 62.533337, 47.8, 32.13333, 17.709093, 9.314287, 3.8352945]

            Mean reciprocal rank: 93.85399839437963


            Aligned document similarities

            {40.0=169, 60.0=79, 30.0=12, 80.0=1, 50.0=225, 70.0=6}

            {80.0=0.2, 30.0=2.4, 60.0=15.8, 40.0=33.8, 50.0=45.0, 70.0=1.2}

         */
        try {
            CLCNGEvaluationSet englishFrenchJrcAcquisEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"), "fr"
            );
            englishFrenchJrcAcquisEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void evalCLCNGEuroparlDocuments() {
        /*
            Ranks 1 to 50

            Precision: [23.991936, 12.298388, 8.333334, 5.040323, 2.560484, 1.2903225, 0.5241936]
            Recall: [23.800001, 24.4, 24.8, 25.0, 25.400002, 25.6, 26.0]
            F-Measure: [23.895582, 16.353886, 12.474849, 8.389261, 4.6520143, 2.4568138, 1.027668]

            Mean reciprocal rank: 24.44609418197027


            Aligned document similarities

            {0.0=65, 60.0=77, 40.0=19, 30.0=12, 50.0=12, 70.0=15}

            {30.0=2.4, 40.0=3.8, 60.0=15.4, 0.0=13.0, 50.0=2.4, 70.0=3.0}
         */

        try {
            CLCNGEvaluationSet englishFrenchEuroparlEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/en"), "en",
                    new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/Europarl/fr"), "fr"
            );
            englishFrenchEuroparlEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void copy10000files() {
        File directory = new File(System.getProperty("user.home") + "/ASPECxc/zh");
        String destinationDirectory = System.getProperty("user.home") + "/ASPECxc/zh10000/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(10000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        FileUtils.copyFile(file, new File(destinationDirectory + fileName));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void tokenizeChineseFiles() {
        File directory = new File(System.getProperty("user.home") + "/ASPECxc/zh10000");
        String destinationDirectory = System.getProperty("user.home") + "/ASPECxc/zh10000-tokenized/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(10000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        String tokenized = StringUtils.join(TokenUtil.chineseTokenize(text, "zh").stream()
                                .map(Token::getToken)
                                .collect(Collectors.toList()), " ");

                        FileUtils.writeStringToFile(new File(destinationDirectory + fileName), tokenized, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
    @Test
    void tokenizeJapaneseXFiles() {
        File directory = new File(System.getProperty("user.home") + "/ASPECx/ja10000");
        String destinationDirectory = System.getProperty("user.home") + "/ASPECx/ja10000-tokenized/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(10000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        String tokenized = StringUtils.join(TokenUtil.tokenize(text, "ja").stream()
                                .map(Token::getToken)
                                .collect(Collectors.toList()), " ");

                        FileUtils.writeStringToFile(new File(destinationDirectory + fileName), tokenized, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
