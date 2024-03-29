package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.SavedEntity;
import com.iandadesign.closa.util.*;
import net.sf.extjwnl.data.Exc;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.XMLFormatter;
import java.util.stream.Collectors;

/**
 * Complete Evaluation for PAN11 implementation (not extended from abstract class etc)
 *
 * @author Johannes Stegmüller (26.06.2020)
 */
public class PAN11EvaluationSetEval {

    public static void main(String[] args) {
        //JS: since tests not work cause of local dependency missing, heres a workaround to make evaluations executable
        //evalPAN2011All();
        //verifyNumberNonEnglishSusp();
        if(args!=null && args.length>=1){
            evalPAN2011EnEs(args[0]);
        }else{
            evalPAN2011EnEs(null);
        }
    }

    //static String pathPrefix = "D:\\AA_ScienceProject\\Data\\pan-plagiarism-corpus-2011\\pan-plagiarism-corpus-2011\\external-detection-corpus";
    //static String pathPrefix = "/media/johannes/Elements SE/CLOSA/pan-plagiarism-corpus-2011/external-detection-corpus";
    static String pathPrefix = "/data/pan-plagiarism-corpus-2011/external-detection-corpus";

    static void evalPAN2011EnEs(String languageIn){
        // This evaluates the specific English/Espanol-Partition from Franco Salvador
        String tag = "evalPAN2011En-DeEs"; // Identifier for logs ...
        String language = "es"; //state "es" or "de" here
        if(languageIn!=null){
            language=languageIn;
        }
        List<String> allowedCaseLengths = new ArrayList<>();
        allowedCaseLengths.add(PAN11PlagiarismInfo.CaseLength.LONG);
        allowedCaseLengths.add(PAN11PlagiarismInfo.CaseLength.MEDIUM);
        allowedCaseLengths.add(PAN11PlagiarismInfo.CaseLength.SHORT);

        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");
        ExtendedAnalysisParameters params;
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }
        // Get the filenumbers for susp and candidate files ...
        List<File> suspiciousFilesLangXML = getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
        List<File> candidateFilesLangXML = getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".xml");
        PAN11XMLParser pan11XMLParser = new PAN11XMLParser();

        List<Integer> usedCandidates = new ArrayList<>();
        List<Integer> usedSuspicious = new ArrayList<>();

        for(File suspFileXML: suspiciousFilesLangXML){
            boolean hasValidLanguagePair = false;
            // Read XML File
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);

            for(PAN11PlagiarismInfo plaginfo:xmlInfo.plagiarismInfos) {

                if (plaginfo.getSourceLanguage().equals(language)) {//|| plaginfo.getSourceLanguage().equals("de")){ //"es" //"de""
                    if(!allowedCaseLengths.contains(plaginfo.getCaseLengthThis())) {
                        continue; // Filter non-matching case-lengths
                    }
                    if (!hasValidLanguagePair) {
                        hasValidLanguagePair = true;
                    }
                    if(language.equals("de")) {
                        Integer sourceId = Integer.parseInt(plaginfo.getSourceReference().replaceAll("\\D+", ""));
                        if (!usedCandidates.contains(sourceId)) {
                            usedCandidates.add(sourceId);
                        }
                    }
                }
            }
            if(hasValidLanguagePair){
                Integer suspId = Integer.parseInt(suspFileXML.getName().replaceAll("\\D+",""));
                usedSuspicious.add(suspId);
            }
        }

        // Salvador differentiates in mechanism of selection on de/es
        // In words: in spanish all source documents are used, in german only the source documents which are related to plagiarism
        if(language.equals("es")) {
            // Getting the corresponding candidates
            for (File candFileXML : candidateFilesLangXML) {
                PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(candFileXML);
                if (xmlInfo.language.equals(language)) {
                    Integer sourceId = Integer.parseInt(candFileXML.getName().replaceAll("\\D+", ""));
                    if (!usedCandidates.contains(sourceId)) {
                        usedCandidates.add(sourceId);
                    }
                }
            }
        }
        // Checking if selection is ok (these are the numbers stated in Salador2016, only if no caselength filtering)
        if(allowedCaseLengths.size()==3 && language.equals("es")){
            if(usedCandidates.size()!=202 || usedSuspicious.size()!=304){
                System.err.println("Wrong file numbers");
                return;
            }
        }else if(allowedCaseLengths.size()==3 && language.equals("de")){
            if(usedCandidates.size()!=348 || usedSuspicious.size()!=251){
                System.err.println("Wrong file numbers");
                return;
            }
        }



        // Overwrite the file filter
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }

        params.USE_FILE_FILTER = true;
        params.USE_LANGUAGE_WHITELISTING = false; // This is done in the step above
        PANFileFilter panFileFilter = new PANFileFilter();
        panFileFilter.addToWhiteListMultiple(true, usedCandidates);
        panFileFilter.addToWhiteListMultiple(false, usedSuspicious);

        // Overwrite the filter with new filtering
        params.panFileFilter = panFileFilter;

        // Just process as usual
        evalPAN2011All(params, tag, "Parsing En-" + language + "\n"+
                "used Candidates Files: " +usedCandidates.size() +
                "\nUsed Suspicious Files: " +usedSuspicious.size()
        );

    }

    //@Test
    static void evalPAN2011All(ExtendedAnalysisParameters params, String tag, String comment)  {
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory;

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);
        // ..


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
        if(!params.USE_FILE_FILTER && !params.USE_LANGUAGE_WHITELISTING && numfiles!=(11093*2)){
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
                osa.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString(), null);
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
        final LanguageDetector langdet;
        if(params.USE_LANGUAGE_WHITELISTING){
            langdet = new LanguageDetector();
        }else{
            langdet = null;
        }

        if(!params.USE_FILE_FILTER){

            // Get all files to preprocess
            List<File> myFilesFiltered = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(filetype)) // Filter .xml files and others only take txt.
                    //.map(File::getPath)
                    .filter(file -> getDocumentLanguageAndCheckIfWhitelisted(langdet, file,params, candOrSusp))
                    .collect(Collectors.toList());
            return myFilesFiltered;
        }else{


            // Apply the local filter in preprocessing
            List<File> myFilesFiltered = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(filetype)) // Filter .xml files and others only take txt.
                    .filter(file -> params.panFileFilter.checkIfFilenameWhitelisted(file.getName(), candOrSusp))
                    //.map(File::getPath)
                    .filter(file -> getDocumentLanguageAndCheckIfWhitelisted(langdet, file,params, candOrSusp))
                    .collect(Collectors.toList());
            return myFilesFiltered;
        }

    }
    public static boolean getDocumentLanguageAndCheckIfWhitelisted(LanguageDetector langdet, File fileToCheck, ExtendedAnalysisParameters params, boolean candOrSusp){
        if(!params.USE_LANGUAGE_WHITELISTING){
            return true;
        }
        if(!candOrSusp){
            return true; // dont filter suispicious files
        }
        // TODO eventually make this less redundant and log to fileoutput
        try{
            String language = langdet.detectLanguage(FileUtils.readFileToString(fileToCheck, StandardCharsets.UTF_8));
            return params.panFileFilter.checkIfLanguageWhitelisted(language);
        }catch(Exception ex){
            System.err.println("Exception during processing file "+ fileToCheck+ " " + ex.toString());
            return false;
        }


    }
    private static int doAllPreprocessing(String topFolderPath, OntologyBasedSimilarityAnalysis osa, String language, ExtendedAnalysisParameters params, boolean candOrSusp) {
        List<File> myFiles = getTextFilesFromTopLevelDir(topFolderPath, params, candOrSusp, ".txt");
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(true,"doAllPreprocessing", topFolderPath + ". with length: " + myFiles.size());
        List<String> panLanguages = new ArrayList<>(); //J4T
        panLanguages.add("en");

        int processedCounter=0;
        int germanLanguageDocs=0;
        int spanishLanguageDocs=0;
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
                    // System.out.println("does this happen? If not just set en without classifier"); //susp part21/10420 de,  before 01500 (1 german), source 00008 german 00011 es, 13 es  .....
                    if(!panLanguages.contains(detectedLanguage)){
                        panLanguages.add(detectedLanguage);
                    }
                    if(detectedLanguage.equals("de")){
                        germanLanguageDocs++;
                    }
                    if(detectedLanguage.equals("es")){
                        spanishLanguageDocs++;
                    }
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
        String pathToScript = "src/eval/resources/com/iandadesign/closa/evaluation/pan-pc11/pan09-plagiarism-detection-perfomance-measures-modified.py";

        //String pathToScript = "D:\\AA_ScienceProject\\Wikidata_vs_CharacterNGram\\PAN2011Evaluator\\pan09-plagiarism-detection-perfomance-measures-modified.py";
        //String plagPath ="D:\\AA_ScienceProject\\Data\\pan-plagiarism-corpus-2011\\pan-plagiarism-corpus-2011\\external-detection-corpus\\suspicious-document";
        String plagPath = baseplagPath; // Path with the susp
        //String detectedPlagiarismPath="D:\\CL_OSA_caching\\preprocessed_extended\\results_comparison\\evalPAN2011All_2020_07_03_14_11_26";
        String detectedPlagiarismPath = baseResultsPath;
        try{
            ProcessBuilder builder = new ProcessBuilder();
            // builder.environment()
            builder.command("python", pathToScript, "--plag-path",plagPath, "--det-path",detectedPlagiarismPath);
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


    public static void verifyNumberNonEnglishDocs(){
        String tag = "evalPAN2011All"; // Identifier for logs ...
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory
        ExtendedAnalysisParameters params;
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();

        // ..
        List<File> candidateFilesLangCount = getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".xml");
        PAN11XMLParser pan11XMLParser = new PAN11XMLParser();
        List<String> usedLanguages = new ArrayList<>();
        int espanolDocs = 0;
        int germanDocs = 0;
        for(File suspFileXML: candidateFilesLangCount){
            // Read XML File
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);
            if(xmlInfo.language.equals("es")){
                espanolDocs++;
            }
            if(xmlInfo.language.equals("de")){
                germanDocs++;
            }
            System.out.println("read");
            if(xmlInfo!=null && !usedLanguages.contains(xmlInfo.language)){
                usedLanguages.add(xmlInfo.language);
            }
        }
        // From xml its:
        // its 202 espanol docs
        // its 471 german docs
        // By langdet its:
        // its 471 german docs
        // its 202 spanish docs
        // ..
    }


    public static void verifyNumberNonEnglishSusp(){
        String tag = "PAN11verifySalvadorPartitions"; // Identifier for logs ...

        String lpathPrefix = "D:\\AA_ScienceProject\\Data\\pan-plagiarism-corpus-2011\\pan-plagiarism-corpus-2011\\";

        String toplevelPathSuspicious = lpathPrefix.concat("external-detection-corpus\\").concat("/suspicious-document/");
        String toplevelPathCandidates = lpathPrefix.concat("external-detection-corpus\\").concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory
        ExtendedAnalysisParameters params;
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();

        // ..
        List<File> suspiciousFilesLangCount = getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
        List<File> candidateFilesLangCount = getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".xml");

        PAN11XMLParser pan11XMLParser = new PAN11XMLParser();
        List<String> usedLanguages = new ArrayList<>();
        int englishDocsCorresponding = 0;
        int shortCases= 0;
        int mediumCases = 0;
        int longCases = 0;
        int overallCases = 0 ;
        int overallXMLOk=0;

        int translationTypeCount=0;
        int manualObfuscation=0;
        int automaticObfuscation=0;
        int othercount = 0;
        long allCasesOfPlagiarism=0;

        for(File suspFileXML: suspiciousFilesLangCount){
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);
            if(xmlInfo.language!=null){
                overallXMLOk++;
            }
            for(PAN11PlagiarismInfo plaginfo:xmlInfo.plagiarismInfos) {
                if(plaginfo.getSourceLanguage().equals("es") || plaginfo.getSourceLanguage().equals("de")) { //"es" //"de""

                    if (plaginfo.getType().equals("translation")) {
                        translationTypeCount++;
                    } else {
                        othercount++;
                    }

                }
                if(plaginfo.getManualObfuscation()!=null && plaginfo.getManualObfuscation()){
                    manualObfuscation++;
                }
                if(plaginfo.getManualObfuscation()!=null && !plaginfo.getManualObfuscation()){
                    automaticObfuscation++;
                }
                allCasesOfPlagiarism++;
            }
        }

        //61 064 all cases of plagiarism -> it is only 49261 (+11443 for intrinsic) = 60704 (difference of 360 cases)
        // The dataset has 49261 cases by searching 'plagiarism' in xml files in notepad++
        // 26 939 documents supposed -> 50/50 -> 11093 are there (its less because of intrinsic corpus missing)
        // Salvador says its 5164 +

        //Checklist:
        // Filecount (xml): 26939 supposed, actual: 11093  * 2 + 4753 (int,susp)  = 26939 -> files are ok
        // Plagiarism (based on suspicious) 61 064 supposed, actual: 11443 intrinsic + 49261 external = 60704 (==> 360 cases missing)
        List<String> espEnSusp = new ArrayList<>();
        List<String> espEnSource = new ArrayList<>();

        for(File suspFileXML: suspiciousFilesLangCount){
            boolean hasValidLanguagePair = false;
            // Read XML File
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);
            if(xmlInfo.language.equals("en")){

                for(PAN11PlagiarismInfo plaginfo:xmlInfo.plagiarismInfos){

                    if(plaginfo.getSourceLanguage().equals("es")){ //|| plaginfo.getSourceLanguage().equals("de")){ //"es" //"de""
                        if(!espEnSource.contains(plaginfo.getSourceReference())) {
                            // This seems correct by logic
                            espEnSource.add(plaginfo.getSourceReference());
                        }
                        if(!hasValidLanguagePair){
                            // This gives the correct number for salvador
                            espEnSusp.add(suspFileXML.getName());

                            englishDocsCorresponding++;
                            hasValidLanguagePair=true;
                        }

                        overallCases ++;
                        String caseLength = plaginfo.getCaseLengthSource();
                        if(caseLength.equals(PAN11PlagiarismInfo.CaseLength.LONG)){
                            longCases++;
                        }else if(caseLength.equals(PAN11PlagiarismInfo.CaseLength.MEDIUM)){
                            mediumCases++;
                        }else if(caseLength.equals(PAN11PlagiarismInfo.CaseLength.SHORT)){
                            shortCases++;
                        }else{
                            System.out.println("error case length!");
                        }
                    }
                }
            }else {
                System.out.println("asd");
            }
        }
        // The selected (de+es) file counts are ok here, so the basic filter is working.
        // Case lengths Salvador:     s1506    m2118     l1951 = SUM -> 5575
        //                             0,27    0,379     0,3499         (1506/5575etc)
        // Case lengths with source:  s1136    m2105     l1870 = SUM -> 5142
        //                              0,22   0,409      0,36
        // Case lengths with this:    s1377    m1936     l1829 = SUM -> 5142
        //                              0,2677  0,376      0,355
        // 433 cases missing
        // This is the number of Translated manual obfuscation 433
        // Most likely its with 'this'cases


        int germanDocsCand = 0;
        int spanishDocsCand = 0;
        for(File candFileXML: candidateFilesLangCount){
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(candFileXML);

            if(xmlInfo.language.equals("es")){
                spanishDocsCand++;
            }
            if(xmlInfo.language.equals("de")){
                germanDocsCand++;
            }

        }
        System.out.println("done");
        //partition en-de: en: 251/251 de: 348/348 -> ok
        //partition en-es: en:  /304  es: /202
        //partition en-es: en:  304/304  es: 199/202 --> 2 candidates are not mapped

    }
}
