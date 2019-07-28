package com.iandadesign.closa.commandLine;

import org.junit.jupiter.api.Test;


public class WikidataEntityExtractionTest {

    @Test
    public void mainTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";

        MainTest.executeWithTimeOut(() -> {
            WikidataEntityExtraction.main(new String[]{"-i", inputPath, "-o", outputPath});
            return new Object();
        });
    }

    @Test
    public void mainParametersTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";

        MainTest.executeWithTimeOut(() -> {
            WikidataEntityExtraction.main(new String[]{
                    "-i", inputPath,
                    "-o", outputPath,
                    "-t", "neutral", "biology", "fiction",
                    "-l", "en", "es", "fr"});
            return new Object();
        });
    }


    @Test
    public void mainAnnotationTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";

        MainTest.executeWithTimeOut(() -> {
            WikidataEntityExtraction.main(new String[]{
                    "-i", inputPath,
                    "-o", outputPath,
                    "-t", "neutral", "biology", "fiction",
                    "-l", "en", "es", "fr",
                    "-a"});
            return new Object();
        });
    }


}
