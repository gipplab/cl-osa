package com.iandadesign.closa.language;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LanguageDetectorTest {

    @Test
    public void testLangDetectConstructor() {
        LanguageDetector languageDetector = new LanguageDetector(Arrays.asList("en", "ja"));
        Assertions.assertEquals(languageDetector.detectLanguage("Beispiel"), "en");
    }

    @Test
    public void testLangDetectConstructorChinese() {
        LanguageDetector languageDetector = new LanguageDetector(Collections.singletonList("zh"));
        Assertions.assertEquals(languageDetector.detectLanguage("Beispiel"), "zh");
    }

    @Test
    public void testLangDetect() {
        LanguageDetector languageDetector = new LanguageDetector();

        Assertions.assertEquals(languageDetector.detectLanguage("Beispiel"), "de");
        Assertions.assertEquals(languageDetector.detectLanguage("a text should have sufficient length in order to" +
                "be detected correctly"), "en");
    }

    @Test
    public void testLangDetectFiles() {
        LanguageDetector languageDetector = new LanguageDetector();

        File folder = new File(System.getProperty("user.home") + "/sts2016/txt/");

        List<String> languages = FileUtils.listFiles(folder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .map(File::getPath)
                .map(path -> {
                    try {
                        return FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return "";
                    }
                })
                .map(languageDetector::detectLanguage)
                .collect(Collectors.toList());

        System.out.println(languages);
    }
}
