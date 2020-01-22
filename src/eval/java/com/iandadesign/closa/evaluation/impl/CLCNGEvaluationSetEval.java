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
import java.util.List;
import java.util.stream.Collectors;

public class CLCNGEvaluationSetEval {

    String pathPrefix = "/data/test";

    @Test
    void evalCLCNGAspec() {
        CLCNGEvaluationSet englishJapaneseASPECEvaluationSetCLCNG = new CLCNGEvaluationSet(
                new File(pathPrefix + "/ASPECx/en10000"), "en",
                new File(pathPrefix + "/ASPECx/ja10000"), "ja",
                2000
        );
        englishJapaneseASPECEvaluationSetCLCNG.printEvaluation();
    }

    @Test
    void evalCLCNGAspecChinese() {
        CLCNGEvaluationSet englishJapaneseASPECEvaluationSetCLCNG = new CLCNGEvaluationSet(
                new File(pathPrefix + "/ASPECxc/ja10000"), "en",
                new File(pathPrefix + "/ASPECxc/zh10000"), "ja",
                2000
        );
        englishJapaneseASPECEvaluationSetCLCNG.printEvaluation();
    }


    @Test
    void evalCLCNGPan11Documents() {
        try {
            CLCNGEvaluationSet englishSpanishPan11EvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(pathPrefix + "/pan11/en"), "en",
                    new File(pathPrefix + "/pan11/es"), "es",
                    2000
            );
            englishSpanishPan11EvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLCNGPan11Sentences() {
        try {
            CLCNGEvaluationSet englishSpanishPan11EvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(pathPrefix + "/sentences/pan11/en"), "en",
                    new File(pathPrefix + "/sentences/pan11/es"), "es",
                    2000
            );
            englishSpanishPan11EvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLCNGJrcAcquisDocuments() {
        try {
            CLCNGEvaluationSet englishFrenchJrcAcquisEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(pathPrefix + "/jrc/en"), "en",
                    new File(pathPrefix + "/jrc/fr"), "fr",
                    1989
            );
            englishFrenchJrcAcquisEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLCNGJrcAcquisSentences() {
        try {
            CLCNGEvaluationSet englishFrenchJrcAcquisEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(pathPrefix + "/sentences/jrc/en"), "en",
                    new File(pathPrefix + "/sentences/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLCNGEuroparlDocuments() {
        try {
            CLCNGEvaluationSet englishFrenchJrcAcquisEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLCNGEuroparlSentences() {
        try {
            CLCNGEvaluationSet englishFrenchJrcAcquisEvaluationSetCLCNG = new CLCNGEvaluationSet(
                    new File(pathPrefix + "/sentences/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/europarl/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLCNG.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    void copy10000files() {
        File directory = new File(pathPrefix + "/ASPECxc/zh");
        String destinationDirectory = pathPrefix + "/ASPECxc/zh10000/";

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
        File directory = new File(pathPrefix + "/ASPECxc/zh10000");
        String destinationDirectory = pathPrefix + "/ASPECxc/zh10000-tokenized-prepr/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(10000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                        List<Token> tokens = TokenUtil.chineseTokenize(text, "zh");
                        tokens = TokenUtil.removePunctuation(tokens);
                        tokens = TokenUtil.removeStopwords(tokens, "zh");

                        String tokenized = StringUtils.join(tokens.stream()
                                .map(Token::getToken)
                                .collect(Collectors.toList()), " ");

                        FileUtils.writeStringToFile(new File(destinationDirectory + fileName), tokenized, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void tokenizeSentencesChineseFiles() {
        File directory = new File(pathPrefix + "/ASPECxc/zh10000");
        String destinationDirectory = pathPrefix + "/ASPECxc/zh200-sents-tokenized-prepr/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(200)
                .forEach(file -> {
                    String fileName = file.getName();
                    System.out.println(fileName);

                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                        String[] sentences = text.split("。");

                        for (int i = 0; i < sentences.length; i++) {
                            sentences[i] = sentences[i].replaceAll("\n", "");

                            List<Token> tokens = TokenUtil.tokenize(sentences[i], "zh");
                            tokens = TokenUtil.removeStopwords(tokens, "zh");
                            tokens = TokenUtil.removePunctuation(tokens);

                            String tokenized = StringUtils.join(
                                    tokens.stream()
                                            .map(Token::getToken)
                                            .collect(Collectors.toList()), " ");

                            if (tokens.size() > 0) {
                                FileUtils.writeStringToFile(
                                        new File(destinationDirectory + fileName.replace(".txt", "-" + (i + 1) + ".txt")),
                                        tokenized,
                                        StandardCharsets.UTF_8);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void preprocessEnglishFiles() {
        File directory = new File(pathPrefix + "/ASPECx/en10000");
        String destinationDirectory = pathPrefix + "/ASPECx/en10000-prepr/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(10000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        List<Token> tokenized = TokenUtil.tokenize(text, "en");

                        tokenized = TokenUtil.removeStopwords(tokenized, "en");
                        tokenized = TokenUtil.removePunctuation(tokenized);

                        String joined = StringUtils.join(tokenized.stream().map(Token::getToken).collect(Collectors.toList()), " ");

                        FileUtils.writeStringToFile(
                                new File(destinationDirectory + fileName),
                                joined,
                                StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void tokenizeSentencesEnglishFiles() {
        File directory = new File(pathPrefix + "/ASPECx/en10000");
        String destinationDirectory = pathPrefix + "/ASPECx/en2000-sents-prepr/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(2000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                        List<List<Token>> sentences = TokenUtil.namedEntityTokenize(text, "en");

                        int i = 0;

                        for (List<Token> sentence : sentences) {
                            sentence = TokenUtil.removeStopwords(sentence, "en");
                            sentence = TokenUtil.removePunctuation(sentence);

                            String joinedSentence = StringUtils.join(sentence.stream().map(Token::getToken).collect(Collectors.toList()), " ");

                            FileUtils.writeStringToFile(
                                    new File(destinationDirectory + fileName.replace(".txt", "-" + (i + 1) + ".txt")),
                                    joinedSentence,
                                    StandardCharsets.UTF_8);
                            i++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void tokenizeJapaneseXFiles() {
        File directory = new File(pathPrefix + "/ASPECxc/ja10000");
        String destinationDirectory = pathPrefix + "/ASPECxc/ja10000-tokenized-prepr/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(10000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                        List<Token> tokens = TokenUtil.tokenize(text, "ja");
                        tokens = TokenUtil.removePunctuation(tokens);
                        tokens = TokenUtil.removeStopwords(tokens, "ja");

                        String tokenized = StringUtils.join(
                                tokens.stream()
                                        .map(Token::getToken)
                                        .collect(Collectors.toList()), " ");

                        FileUtils.writeStringToFile(new File(destinationDirectory + fileName), tokenized, StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    void tokenizeSentencesJapaneseXFiles() {
        File directory = new File(pathPrefix + "/ASPECx/ja10000");
        String destinationDirectory = pathPrefix + "/ASPECx/ja2000-sents-tokenized-prepr/";

        FileUtils.listFiles(directory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .limit(2000)
                .forEach(file -> {
                    String fileName = file.getName();
                    try {
                        String text = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

                        String[] sentences = text.split("。");

                        for (int i = 0; i < sentences.length; i++) {
                            sentences[i] = sentences[i].replaceAll("\n", "");

                            List<Token> tokens = TokenUtil.tokenize(sentences[i], "ja");
                            tokens = TokenUtil.removeStopwords(tokens, "ja");
                            tokens = TokenUtil.removePunctuation(tokens);

                            String tokenized = StringUtils.join(
                                    tokens.stream()
                                            .map(Token::getToken)
                                            .collect(Collectors.toList()),
                                    " ");

                            if (tokens.size() != 0) {
                                FileUtils.writeStringToFile(
                                        new File(destinationDirectory + fileName.replace(".txt", "-" + (i + 1) + ".txt")),
                                        tokenized,
                                        StandardCharsets.UTF_8);
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }
}
