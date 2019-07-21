package com.iandadesign.closa.commandLine;

import org.junit.Test;

public class EntityExtractionTest {

    @Test
    public void mainTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";
        WikidataEntityExtraction.main(new String[]{"-i", inputPath, "-o", outputPath});
    }

    @Test
    public void mainParametersTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";
        WikidataEntityExtraction.main(new String[]{
                "-i", inputPath,
                "-o", outputPath,
                "-t", "neutral", "biology", "fiction",
                "-l", "en", "es", "fr"});
    }


    @Test
    public void mainAnnotationTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";
        WikidataEntityExtraction.main(new String[]{
                "-i", inputPath,
                "-o", outputPath,
                "-t", "neutral", "biology", "fiction",
                "-l", "en", "es", "fr",
                "-a"});
    }
}
