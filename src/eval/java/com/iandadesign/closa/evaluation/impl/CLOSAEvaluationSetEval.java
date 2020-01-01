package com.iandadesign.closa.evaluation.impl;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

class CLOSAEvaluationSetEval {
    
    String pathPrefix = "/data/test";

    @Test
    void evalCLOSAAspec() {
        CLOSAEvaluationSet englishJapaneseASPECEvaluationSetCLOSA = new CLOSAEvaluationSet(
                new File(pathPrefix + "/ASPECx/ja10000"), "ja",
                new File(pathPrefix + "/ASPECx/en10000"), "en",
                2000
        );

        englishJapaneseASPECEvaluationSetCLOSA.setGraphBasedAnalysis(true);
        englishJapaneseASPECEvaluationSetCLOSA.printEvaluation();
    }

    @Test
    void evalCLOSAAspecChinese() {
        CLOSAEvaluationSet englishJapaneseASPECEvaluationSetCLOSA = new CLOSAEvaluationSet(
                new File(pathPrefix + "/ASPECxc/ja10000"), "ja",
                new File(pathPrefix + "/ASPECxc/zh10000"), "zh",
                2000
        );

        englishJapaneseASPECEvaluationSetCLOSA.setGraphBasedAnalysis(true);
        englishJapaneseASPECEvaluationSetCLOSA.printEvaluation();
    }


    @Test
    void evalCLOSAPan11Documents() {
        try {
            CLOSAEvaluationSet englishSpanishPan11EvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(pathPrefix + "/pan11/en"), "en",
                    new File(pathPrefix + "/pan11/es"), "es",
                    2000
            );

            englishSpanishPan11EvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishSpanishPan11EvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLOSAPan11Sentences() {
        try {
            CLOSAEvaluationSet englishSpanishPan11EvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(pathPrefix + "/sentences/pan11/en"), "en",
                    new File(pathPrefix + "/sentences/pan11/es"), "es",
                    2000
            );
            englishSpanishPan11EvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishSpanishPan11EvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLOSAJrcAcquisDocuments() {
        try {
            CLOSAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(pathPrefix + "/jrc/en"), "en",
                    new File(pathPrefix + "/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishFrenchJrcAcquisEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLOSAJrcAcquisSentences() {
        try {
            CLOSAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(pathPrefix + "/sentences/jrc/en"), "en",
                    new File(pathPrefix + "/sentences/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishFrenchJrcAcquisEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLOSAEuroparlDocuments() {
        try {
            CLOSAEvaluationSet englishFrenchEuroparlEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(pathPrefix + "/europarl/en"), "en",
                    new File(pathPrefix + "/europarl/fr"), "fr",
                    2000
            );
            englishFrenchEuroparlEvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishFrenchEuroparlEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLOSAEuroparlSentences() {
        try {
            CLOSAEvaluationSet englishFrenchEuroparlEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(pathPrefix + "/sentences/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/europarl/fr"), "fr",
                    2000
            );
            englishFrenchEuroparlEvaluationSetCLOSA.setGraphBasedAnalysis(true);
            englishFrenchEuroparlEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalSTS() {
        try {
            CLOSAEvaluationSet stsEvaluationSetCLOSA = new CLOSAEvaluationSet(
                    new File(pathPrefix + "/sts2016/txt"), "L", "R"
            );
            stsEvaluationSetCLOSA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void prepareASPEC() {
        File enJaFile = new File(pathPrefix + "/ASPEC/ASPEC-JE/train/train-1.txt");

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
                    File file = new File(pathPrefix + "/ASPECx/en/" + key + ".txt");
                    //noinspection UnstableApiUsage
                    Files.createParentDirs(file);
                    FileUtils.writeLines(file, value.values());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            idJapaneseDocumentMap.forEach((key, value) -> {
                try {
                    File file = new File(pathPrefix + "/ASPECx/ja/" + key + ".txt");
                    //noinspection UnstableApiUsage
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
        File zhJaFile = new File(pathPrefix + "/ASPEC/ASPEC-JC/train/train.txt");

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
                    File file = new File(pathPrefix + "/ASPECxc/zh/" + key + ".txt");
                    //noinspection UnstableApiUsage
                    Files.createParentDirs(file);
                    FileUtils.writeLines(file, value.values());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            idJapaneseDocumentMap.forEach((key, value) -> {
                try {
                    File file = new File(pathPrefix + "/ASPECxc/ja/" + key + ".txt");
                    //noinspection UnstableApiUsage
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
