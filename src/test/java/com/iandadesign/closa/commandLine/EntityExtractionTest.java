package com.iandadesign.closa.commandLine;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.*;

public class EntityExtractionTest {

    @Test
    public void mainTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";

        this.executeWithTimeOut(() -> {
            WikidataEntityExtraction.main(new String[]{"-i", inputPath, "-o", outputPath});
            return new Object();
        });
    }

    @Test
    public void mainParametersTest() {
        String inputPath = "src/test/resources/com/iandadesign/closa/test.txt";
        String outputPath = "src/test/resources/com/iandadesign/closa/test-output.txt";

        this.executeWithTimeOut(() -> {
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

        this.executeWithTimeOut(() -> {
            WikidataEntityExtraction.main(new String[]{
                    "-i", inputPath,
                    "-o", outputPath,
                    "-t", "neutral", "biology", "fiction",
                    "-l", "en", "es", "fr",
                    "-a"});
            return new Object();
        });
    }


    private void executeWithTimeOut(Callable<Object> task) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Future<Object> future = executor.submit(task);
        try {
            future.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            Assert.fail();
            e.printStackTrace();
        } finally {
            future.cancel(true); // may or may not desire this
        }
    }
}
