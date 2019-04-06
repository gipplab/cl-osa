package com.fabianmarquart.closa.language;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class LanguageDetectorTest {


    @Test
    public void testLangDetect() {
        LanguageDetector languageDetector = new LanguageDetector();

        Assert.assertEquals(languageDetector.detectLanguage("Beispiel"), "de");
        Assert.assertEquals(languageDetector.detectLanguage("a text should have sufficient length in order to" +
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
