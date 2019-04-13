package com.fabianmarquart.closa.commandLine;

import org.junit.Test;

public class MainTest {

    @Test
    public void mainTest() {
        String suspiciousFilePath = "src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/en/35157967/0.txt";
        String candidateFolderPath = "src/test/resources/com/fabianmarquart/closa/evaluation/test-bbc/ja/35157967";
        String outputPath = "src/test/resources/com/fabianmarquart/closa/evaluation/test-output.txt";
        Main.main(new String[]{
                "-s", suspiciousFilePath,
                "-c", candidateFolderPath,
                "-o", outputPath,
                "-t", "neutral", "biology", "fiction",
                "-l", "en", "ja"});
    }
}
