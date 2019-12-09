package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.TokenUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CLASAEvaluationSetTest {

    @Test
    void extractTranslationProbabilitiesAndStore() {
        CLASAEvaluationSet.extractTranslationProbabilitiesAndStore();
    }

    @Test
    void preprocessTedJapaneseChinese() {
        File file = new File("/data/corpus/Multilingual_Parallel_Corpus/Multi_lingual_Parallel_corpus_2.txt");

        File japaneseOut = new File("/data/corpus/Multilingual_Parallel_Corpus/MPC.JA.txt");
        File chineseOut = new File("/data/corpus/Multilingual_Parallel_Corpus/MPC.ZH.txt");

        try {
            ProgressBar progressBar = new ProgressBar("preprocessTedJapaneseChinese: ",
                    Files.lines(file.toPath(), StandardCharsets.UTF_8).count() / 2, ProgressBarStyle.ASCII);
            progressBar.start();

            if (Files.exists(japaneseOut.toPath())) {
                Files.delete(japaneseOut.toPath());
            }
            if (Files.exists(chineseOut.toPath())) {
                Files.delete(chineseOut.toPath());
            }
            Files.createFile(japaneseOut.toPath());
            Files.createFile(chineseOut.toPath());

            Writer japaneseWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(japaneseOut, true), StandardCharsets.UTF_8));
            Writer chineseWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(chineseOut, true), StandardCharsets.UTF_8));

            FileUtils.readLines(file, StandardCharsets.UTF_8)
                    .stream()
                    .filter((String line) -> line.contains(":ja:") || line.contains(":zh-cn:"))
                    .forEach((String line) -> {
                        if (line.contains(":ja:")) {
                            String tokenized = StringUtils.join(TokenUtil.tokenize(line.split(":")[2], "ja")
                                    .stream()
                                    .map(Token::getToken)
                                    .collect(Collectors.toList()), " ");

                            try {
                                japaneseWriter.append(tokenized).append("\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            progressBar.step();
                        } else if (line.contains(":zh-cn:")) {
                            String tokenized = StringUtils.join(TokenUtil.tokenize(line.split(":")[2], "zh")
                                    .stream()
                                    .map(Token::getToken)
                                    .collect(Collectors.toList()), " ");

                            try {
                                chineseWriter.append(tokenized).append("\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            progressBar.step();
                        }
                    });

            progressBar.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}