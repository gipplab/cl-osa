package com.iandadesign.closa.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExtendedLogUtil {

    private PrintStream errorLogStream;
    private PrintStream standardLogStream;
    private String tag;
    private boolean LOG_ERROR_TO_FILE;
    private boolean LOG_STANDARD_TO_FILE;
    private static String format = "%-40s%s%n";        // Format for key value outputs
    private String dateString;


    public ExtendedLogUtil(String errorlogPath, String logPath, String tag, boolean logErrorToFile, boolean logStandardToFile){
        try {

            this.tag = tag;
            this.LOG_ERROR_TO_FILE = logErrorToFile;
            this.LOG_STANDARD_TO_FILE = logStandardToFile;
            // Create a date string which is associated with the files in from this logger
            createCurrentDateString();
            // Create Log Files if they dont exist already
            this.errorLogStream = createLogFile(errorlogPath);
            this.standardLogStream = createLogFile(logPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getDateString() {
        return dateString;
    }

    private void createCurrentDateString(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        LocalDateTime now = LocalDateTime.now();
        dateString = dtf.format((now));
    }
    private PrintStream createLogFile(String path) throws IOException{
        if(path==null){
            return null;
        }
        if (Files.notExists(Paths.get(path))) {
            Files.createDirectories(Paths.get(path));
        }

        String fileName = tag.concat(dateString).concat(".txt");
        Path filePath = Paths.get(path, fileName);

        Path createdPath;
        if(Files.notExists(filePath)){
            createdPath =  Files.createFile(Paths.get(path, fileName));
        }else{
            createdPath = filePath;
        }
        return new PrintStream(createdPath.toFile());
    }


    /**
     * Writes a report to file titled with the current date time in the given path.
     *
     * @param message message as file content
     */
    public void writeErrorReport(boolean useFormat, Object ... args) {
        if(!this.LOG_ERROR_TO_FILE);
        if(useFormat){
            errorLogStream.printf(format, args);
        }else{
            errorLogStream.print(args.toString());
        }
    }

    public void writeStandardReport(boolean useFormat, Object ... args) {
        if(!this.LOG_STANDARD_TO_FILE) return;
        if(useFormat){
            standardLogStream.printf(format, args);
        }else{
            standardLogStream.println(args.toString());
        }
    }

    public void logAndWriteStandard(boolean useFormat, Object ... message){
        if(useFormat){
            System.out.printf(format, message);
            writeStandardReport(true, message);
        }else{
            System.out.println(message);
            writeStandardReport(false, message);
        }
    }

    public void logAndWriteError(boolean useFormat, Object ... message){
        if(useFormat){
            System.err.printf(format, message);
            writeErrorReport(true, message);
        }else{
            System.err.println(message);
            writeErrorReport(false, message);
        }
    }

    /**
     * Creates a string of dashes that is n dashes long.
     *
     * @param n The number of dashes to add to the string.
     */
    public String dashes( int n ) {
        return CharBuffer.allocate( n ).toString().replace( '\0', '-' );
    }

    public void closeStreams(){
        if(errorLogStream != null) {
             errorLogStream.close();
        }
        if(standardLogStream != null){
            standardLogStream.close();
        }
    }
}
