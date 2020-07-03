package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.SavedEntity;
import com.iandadesign.closa.util.ExtendedLogUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Complete Evaluation for PAN11 implementation (not extended from abstract class etc)
 *
 * @author Johannes Stegmüller (26.06.2020)
 */
class PAN11EvaluationSetEval {

    public static void main(String[] args) {
        //JS: since tests not work cause of local dependency missing, heres a workaround to make evaluations executable
        evalPAN2011All();
    }

    static String pathPrefix = "D:\\AA_ScienceProject\\Data\\pan-plagiarism-corpus-2011\\pan-plagiarism-corpus-2011\\external-detection-corpus";

    //@Test
    static void evalPAN2011All()  {
        // TODO load a centralized config or class with all params
        // Route the complete output to a logfile here.
        String tag = "evalPAN2011All"; // Identifier for logs ...
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory
        ExtendedAnalysisParameters params = new ExtendedAnalysisParameters();

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();

        // Starting log
        logUtil.logAndWriteStandard(false,logUtil.dashes(100));
        logUtil.logAndWriteStandard(true,"Starting PAN2011 evaluation");
        logUtil.logAndWriteStandard(true,"TAG:", tag);
        logUtil.logAndWriteStandard(true,"Time:", DateTime.now());
        logUtil.logAndWriteStandard(true,"Results and Caching directory:", osa.getPreprocessedCachingDirectory());
        logUtil.logAndWriteStandard(true,"Standard-Logfiles Path:", osa.getStandardlogPath());
        logUtil.logAndWriteStandard(true,"Error-Logfiles Path:", osa.getErrorlogPath());
        logUtil.logAndWriteStandard(true,"Logging Standard.out to file activated:", params.LOG_STANDARD_TO_FILE);
        logUtil.logAndWriteStandard(true,"Logging Error.out to file activated:", params.LOG_ERROR_TO_FILE);
        logUtil.logAndWriteStandard(true,"Saving 2D-Matrix to .csv:", params.LOG_TO_CSV);
        logUtil.logAndWriteStandard(true,"Logging verbosely activated:", params.LOG_VERBOSE);
        logUtil.logAndWriteStandard(true,"Pre-Filter the complete dataset:", params.USE_FILE_FILTER);
        logUtil.logAndWriteStandard(true,"Parallelism fetching Wikidata entries:", osa.getDoParallelRequests());
        logUtil.logAndWriteStandard(true,"Sliding Window Length:", params.NUM_SENTENCES_IN_SLIDING_WINDOW);
        logUtil.logAndWriteStandard(true,"Sliding Window Increment:", params.NUM_SENTENCE_INCREMENT_SLIDINGW);
        logUtil.logAndWriteStandard(true,"Clustering Adjacent Threshold:", params.ADJACENT_THRESH);
        logUtil.logAndWriteStandard(true,"Clustering Single Threshold:", params.SINGLE_THRESH);
        logUtil.logAndWriteStandard(true,"Clustering Use adaptive Threshold (by median):", params.USE_ADAPTIVE_CLUSTERING_TRESH);
        logUtil.logAndWriteStandard(true,"Adaptive Threshold form factor:", params.ADAPTIVE_FORM_FACTOR);
        logUtil.logAndWriteStandard(true,"Clipping Margin Characters:", params.CLIPPING_MARGING);
        logUtil.logAndWriteStandard(true,"Maximum selected candidates:", params.MAX_NUM_CANDIDATES_SELECTED);
        logUtil.logAndWriteStandard(true,"Candidate Selection Threshold:", params.CANDIDATE_SELECTION_TRESH);
        logUtil.logAndWriteStandard(true,"Sublist Token Length:", osa.getLenSublistTokens());
        logUtil.logAndWriteStandard(true,"Run evaluation after processing:", params.RUN_EVALUATION_AFTER_PROCESSING);
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));


        logUtil.logAndWriteStandard(false, "Preprocessing all candidate files...");
        int candidatePreprocessedFiles = doAllPreprocessing(toplevelPathCandidates, osa, null, params, true);  // candidate docs can be en,de,es
        logUtil.logAndWriteStandard(false, "Preprocessing all suspicious files...");
        int suspiciousPreprocessedFiles = doAllPreprocessing(toplevelPathSuspicious, osa, "en", params, false); // all suspicious docs are english

        int numfiles = suspiciousPreprocessedFiles+candidatePreprocessedFiles;
        logUtil.logAndWriteStandard(false, "Number of preprocessed files is: "+numfiles);
        if(!params.USE_FILE_FILTER && numfiles!=(11093*2)){
            logUtil.logAndWriteError(false, "Aborting: Processed files not 2*11093=22186 but "+numfiles);
            return;
        }
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));

        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");

        // Do the file comparisons
        int parsedFiles=0;
        int parsedErrors=0;
        String baseResultsPath="";
        for(int index=0; index<suspiciousFiles.size(); index++) {
            String suspPath = suspiciousFiles.get(index).getPath();
            String suspFileName = suspiciousFiles.get(index).getName();
            try {
                logUtil.logAndWriteStandard(true, logUtil.getDateString(), "Parsing Suspicious file ", index+1, "/", suspiciousFiles.size(),"Filename:", suspFileName," and its",candidateFiles.size() ,"candidates");
                osa.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString());
                baseResultsPath = osa.getExtendedXmlResultsPath();
                parsedFiles++;
            }catch(Exception ex){
                parsedErrors++;
                logUtil.logAndWriteError(false, "Exception during parse of suspicious file with Filename", suspFileName, "Exception:", ex);
            }
        }
        logUtil.logAndWriteStandard(true,logUtil.getDateString(), "done processing PAN11,- parsed files:", parsedFiles, "errors:", parsedErrors);
        if(parsedErrors>=1 || parsedFiles==0){
            return;
        }
        // Evaluation related stuff ...
        if(params.RUN_EVALUATION_AFTER_PROCESSING){
            if(!params.USE_FILE_FILTER){
                // No filter-> just do the regular evaluation with all files
                triggerPAN11PythonEvaluation(logUtil, baseResultsPath, toplevelPathSuspicious);
            }else{
                // Filter used, only compare with relevant files
                File cachingDir= new File(baseResultsPath +"\\file_selection_cache");
                removeDirectory(cachingDir);
                List<File> suspiciousXML  = getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
                writeFileListToDirectory(suspiciousXML, cachingDir.getPath(), logUtil);
                triggerPAN11PythonEvaluation(logUtil, baseResultsPath, cachingDir.getPath());
                removeDirectory(cachingDir);
            }
            logUtil.logAndWriteStandard(true,logUtil.getDateString(), "done doing evaluation for PAN11");
        }
        logUtil.closeStreams();
    }

    private static boolean removeDirectory(File directoryToBeDeleted){
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                removeDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private static void writeFileListToDirectory(List<File> filesToWrite, String directoryPath, ExtendedLogUtil logUtil){
        try {
            File dir = new File(directoryPath);
            if (!dir.exists() || !dir.isDirectory()) {
                //if the file is present then it will show the msg
                dir.mkdirs();
            }

            for (File file : filesToWrite) {
                File destinationFile = new File(directoryPath + "\\"+ file.getName());
                FileUtils.copyFile(file, destinationFile);
            }
        }catch(Exception e){
            logUtil.logAndWriteError(false, "exception copying files", e.toString());

        }
    }

    private static List<File> getTextFilesFromTopLevelDir(String topFolderPath, ExtendedAnalysisParameters params, boolean candOrSusp, String filetype){
        File myFiles = new File(topFolderPath);
        if(!params.USE_FILE_FILTER){
            // Get all files to preprocess
            List<File> myFilesFiltered = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(filetype)) // Filter .xml files and others only take txt.
                    //.map(File::getPath)
                    .collect(Collectors.toList());
            return myFilesFiltered;
        }else{
            // Apply the local filter in preprocessing
            List<File> myFilesFiltered = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(filetype)) // Filter .xml files and others only take txt.
                    .filter(file -> params.panFileFilter.checkIfFilenameWhitelisted(file.getName(), candOrSusp))
                    //.map(File::getPath)
                    .collect(Collectors.toList());
            return myFilesFiltered;
        }

    }
    private static int doAllPreprocessing(String topFolderPath, OntologyBasedSimilarityAnalysis osa, String language, ExtendedAnalysisParameters params, boolean candOrSusp) {
        List<File> myFiles = getTextFilesFromTopLevelDir(topFolderPath, params, candOrSusp, ".txt");
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(true,"doAllPreprocessing", topFolderPath + ". with length: " + myFiles.size());

        int processedCounter=0;
        // Preprocess one file after other (TODO eventually add parallelism: mind that requests to dbs can already be parallelised)
        for (File documentFile : myFiles) {
            String documentPath = documentFile.getPath();

            try {
                logUtil.logAndWriteStandard(true,"doAllPreprocessing", "Starting preprocessing filename: "+documentFile.getName());
                String detectedLanguage;
                if(language==null) {
                    // If language not defined detect it
                     detectedLanguage = osa
                            .getLanguageDetector()
                            .detectLanguage(FileUtils.readFileToString(documentFile, StandardCharsets.UTF_8));
                }else{
                     detectedLanguage = language;
                }
                /*
                if(!detectedLanguage.equals("en")){ // susp part 21 10262
                    System.out.println("does this happen? If not just set en without classifier"); //susp part21/10420 de,  before 01500 (1 german), source 00008 german 00011 es, 13 es  .....
                }
                */
                List<SavedEntity> preprocessedCandExt = osa.preProcessExtendedInfo(documentPath, detectedLanguage);
                logUtil.logAndWriteStandard(true,"doAllPreprocessing -> done", "Processed filename: "+documentFile.getName()+ " with entities: "+preprocessedCandExt.size());
                processedCounter++;
            }catch(Exception ex){
                // Save exception to error log
                logUtil.logAndWriteError(false, "doAllPreprocessing Error processing filename:"+documentFile.getName()+":"+ex);
            }
        }
        logUtil.logAndWriteStandard(true,"doAllPreprocessing finished processing ", processedCounter+" of "+myFiles.size());
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));
        return processedCounter;
    }

    private static void triggerPAN11PythonEvaluation(ExtendedLogUtil logUtil, String baseResultsPath, String baseplagPath) {
        logUtil.logAndWriteStandard(true,logUtil.getDateString(),"Running evaluation tool for PAN11");
        String pathToScript = "src/eval/resources/com/iandadesign/closa/evaluation/pan-pc11/pan09-plagiarism-detection-perfomance-measures.py";

        //String pathToScript = "D:\\AA_ScienceProject\\Wikidata_vs_CharacterNGram\\PAN2011Evaluator\\pan09-plagiarism-detection-perfomance-measures.py";
        //String plagPath ="D:\\AA_ScienceProject\\Data\\pan-plagiarism-corpus-2011\\pan-plagiarism-corpus-2011\\external-detection-corpus\\suspicious-document";
        String plagPath = baseplagPath; // Path with the susp
        //String detectedPlagiarismPath="D:\\CL_OSA_caching\\preprocessed_extended\\results_comparison\\evalPAN2011All_2020_07_03_14_11_26";
        String detectedPlagiarismPath = baseResultsPath;
        try{
            ProcessBuilder builder = new ProcessBuilder();
            // builder.environment()
            builder.command(        "python", pathToScript,"--plag-path",plagPath,"--det-path",detectedPlagiarismPath);
            //builder.directory(new File(homeDir));
            //builder.command("dir");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while ((line = reader.readLine()) != null) {
                logUtil.logAndWriteStandard(false, line);
            }
        }catch(Exception ex){
            logUtil.logAndWriteError(false,"Exception during");
        }

    }


    @Test
    void evalCLASAAspecChinese() {
        // Left in as example
        CLASAEvaluationSet englishJapaneseASPECEvaluationSetCLASA = new CLASAEvaluationSet(
                new File(pathPrefix + "/ASPECxc/ja10000"), "en",
                new File(pathPrefix + "/ASPECxc/zh10000"), "ja",
                2000
        );

        englishJapaneseASPECEvaluationSetCLASA.printEvaluation();
    }

}
