package com.fabianmarquart.closa;

import org.junit.Test;

public class MainTests {

    @Test
    public void mainTest() {
        String inputPath = "src/test/resources/com/fabianmarquart/closa/test.txt";
        String outputPath = "src/test/resources/com/fabianmarquart/closa/test-output.txt";
        Main.main(new String[]{"-i", inputPath, "-o", outputPath});
    }

    @Test
    public void mainParametersTest() {
        String inputPath = "src/test/resources/com/fabianmarquart/closa/test.txt";
        String outputPath = "src/test/resources/com/fabianmarquart/closa/test-output.txt";
        Main.main(new String[]{
                "-i", inputPath,
                "-o", outputPath,
                "-t", "neutral", "biology", "fiction",
                "-l", "en", "es", "fr"});
    }


    @Test
    public void mainAnnotationTest() {
        String inputPath = "src/test/resources/com/fabianmarquart/closa/test.txt";
        String outputPath = "src/test/resources/com/fabianmarquart/closa/test-output.txt";
        Main.main(new String[]{
                "-i", inputPath,
                "-o", outputPath,
                "-t", "neutral", "biology", "fiction",
                "-l", "en", "es", "fr",
                "-a"});
    }
}
