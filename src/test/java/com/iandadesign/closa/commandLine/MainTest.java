package com.iandadesign.closa.commandLine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class MainTest {

    /**
     * Execute with timeout.
     *
     * @param task function to execute.
     */
    static void executeWithTimeOut(Callable<Object> task) {
        ExecutorService executor = Executors.newCachedThreadPool();
        Future<Object> future = executor.submit(task);
        try {
            future.get(5, TimeUnit.MINUTES);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            Assertions.fail();
            e.printStackTrace();
        } finally {
            future.cancel(true); // may or may not desire this
        }
    }

    @Test
    public void mainTest() {
        String suspiciousFilePath = "src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en/35157967/0.txt";
        String candidateFolderPath = "src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja/35157967";
        String outputPath = "src/test/resources/com/iandadesign/closa/evaluation/test-output.txt";

        MainTest.executeWithTimeOut(() -> {
            Main.main(new String[]{
                    "-s", suspiciousFilePath,
                    "-c", candidateFolderPath,
                    "-o", outputPath,
                    "-t", "neutral", "biology", "fiction",
                    "-l", "en", "ja"});
            return new Object();
        });
    }
}
