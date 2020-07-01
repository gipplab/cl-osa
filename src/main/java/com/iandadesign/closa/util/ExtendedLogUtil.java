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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
            // Create Log Files if they do not exist already
            if(logErrorToFile){
                this.errorLogStream = createLogFile(errorlogPath, false);
            }
            if(logStandardToFile){
                this.standardLogStream = createLogFile(logPath, true);
            }
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
    private PrintStream createLogFile(String path, boolean standardLogOrErrorLog) throws IOException{
        if(path==null){
            return null;
        }
        if (Files.notExists(Paths.get(path))) {
            Files.createDirectories(Paths.get(path));
        }

        String fileName;
        if(standardLogOrErrorLog){
            fileName = tag.concat(dateString).concat("standard_log").concat(".txt");
        }else{
            fileName = tag.concat(dateString).concat("error_log").concat(".txt");
        }
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
     */
    public void writeErrorReport(boolean useFormat, Object ... args) {
        if(!this.LOG_ERROR_TO_FILE) return;
        if(args == null){
            return;
        }
        varargsToOutput(useFormat, System.err, args);

    }

    public void writeStandardReport(boolean useFormat, Object ... args) {
        if(!this.LOG_STANDARD_TO_FILE) return;
        if(args == null){
            return;
        }
        varargsToOutput(useFormat, System.out, args);

    }

    public void logAndWriteStandard(boolean useFormat, Object ... message){
        if(message == null){
            return;
        }
        // Log message to system out
        varargsToOutput(useFormat, System.out, message);
        // Log message to filesystem
        varargsToOutput(useFormat, standardLogStream, message);
    }

    public void logAndWriteError(boolean useFormat, Object ... message){
        if(message == null){
            return;
        }
        // Log message to system out
        varargsToOutput(useFormat, System.err, message);
        // Log message to filesystem
        varargsToOutput(useFormat, errorLogStream, message);

    }
    private void varargsToOutput(boolean useFormat, PrintStream stream, Object ... message){
        if(useFormat){
            switch(message.length){
                case 1:
                    stream.println(message[0].toString());
                    break;
                case 2:
                    stream.printf(format, message[0].toString(), message[1].toString());
                    break;
                default:
                    StringBuilder concatString= new StringBuilder();
                    boolean first = true;
                    for(Object obj:message){
                        if(first){
                            first= false;
                            continue;
                        }
                        concatString.append(obj.toString()).append(" ");
                    }
                    stream.printf(format, message[0].toString(), concatString.toString());
                    break;
            }
        }else{
            for (Object i: message) {
                stream.print(i + " ");
            }
            stream.println();
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
    public String getCurrentTime(){
        return new SimpleDateFormat().format( new Date() );
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
