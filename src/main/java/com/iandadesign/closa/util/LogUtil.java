package com.iandadesign.closa.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

public class LogUtil {

    private Path errorLogPath;
    private Path standardLogPath;
    private String tag;

    public LogUtil(String errorlogPath, String logPath, String tag){
        try {
            // Create Log Files if they dont exist already
            this.errorLogPath = createLogFile(errorlogPath);
            this.standardLogPath = createLogFile(logPath);
            this.tag = tag;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private Path createLogFile(String path) throws IOException{
        if(path==null){
            return null;
        }
        if (Files.notExists(Paths.get(path))) {
            Files.createDirectories(Paths.get(path));
        }
        String fileName = tag + ".txt";
        Path filePath = Paths.get(path,fileName);
        if(Files.notExists(filePath)){
            return Files.createFile(Paths.get(path, fileName));
        }else{
            return filePath;
        }
    }


    /**
     * Writes a report to file titled with the current date time in the given path.
     *
     * @param message message as file content
     */
    public void writeErrorReport(String message) {
        try {
            FileUtils.writeStringToFile(this.errorLogPath.toFile(), message, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeStandardReport(String message) {
        try {
            FileUtils.writeStringToFile(this.standardLogPath.toFile(), message, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logAndWriteStandard(String message){
        System.out.println(message);
        writeStandardReport(message);
    }
    public void logAndWriteError(String message){
        System.err.println(message);
        writeErrorReport(message);
    }

}
