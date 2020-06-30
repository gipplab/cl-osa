package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.SavedEntity;
import com.iandadesign.closa.util.ExtendedLogUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Complete Evaluation for PAN11 implementation (not founded on abstract class etc)
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
        String toplevelPathSuspicious = pathPrefix + "/suspicious-document/";
        String toplevelPathCandidates = pathPrefix + "/source-document/";


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory
        ExtendedAnalysisParameters params= new ExtendedAnalysisParameters();
        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        List<String> myPathsC = getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true);
        List<String> myPathsS = getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false);

        int candidatePreprocessedFiles = doAllPreprocessing(toplevelPathCandidates, osa, null, params, true);  // candidate docs can be en,de,es
        int suspiciousPreprocessedFiles = doAllPreprocessing(toplevelPathSuspicious, osa, "en", params, false); // all suspicious docs are english

        int numfiles = suspiciousPreprocessedFiles+candidatePreprocessedFiles;
        if(params.USE_FILE_FILTER && numfiles!=(11093*2)){
            logUtil.logAndWriteError(false, "Aborting: Processed files not 2*11093=22186 but "+numfiles);
            return;
        }

        List<String> candidatePaths = getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true);
        List<String> suspiciousPaths  = getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false);


        // Do the file comparisons
        int parsedFiles=0;
        int parsedErrors=0;
        for(int index=0; index<suspiciousPaths.size();index++){
            String suspPath = suspiciousPaths.get(index);
            try {
                logUtil.logAndWriteStandard(true,"Parsing Suspicious file ", index, "/", suspiciousPaths.size()," path:", suspPath," and its candidates");
                osa.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidatePaths, tag, params);
                parsedFiles++;
            }catch(Exception ex){
                parsedErrors++;
                logUtil.logAndWriteError(false, "Exception during parse of suspicious file ", suspPath, "Exception:", ex);
            }
        }

        logUtil.logAndWriteStandard(false, "completely done PAN11 parsed files:", parsedFiles, "errors:", parsedErrors);

    }
    private static List<String> getTextFilesFromTopLevelDir(String topFolderPath, ExtendedAnalysisParameters params, boolean candOrSusp){
        File myFiles = new File(topFolderPath);
        if(!params.USE_FILE_FILTER){
            // Get all files to preprocess
            List<String> myPaths = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(".txt")) // Filter .xml files and others only take txt.
                    .map(File::getPath)
                    .collect(Collectors.toList());
            return myPaths;
        }else{

            // Apply the local filter in preprocessing
            List<String> myPaths = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(".txt")) // Filter .xml files and others only take txt.
                    .filter(file -> params.panFileFilter.checkIfFilenameWhitelisted(file.getName(), candOrSusp))
                    .map(File::getPath)
                    .collect(Collectors.toList());
            return myPaths;
        }

    }
    private static int doAllPreprocessing(String topFolderPath, OntologyBasedSimilarityAnalysis osa, String language, ExtendedAnalysisParameters params, boolean candOrSusp) {
        List<String> myPaths = getTextFilesFromTopLevelDir(topFolderPath, params, candOrSusp);

        osa.getExtendedLogUtil().logAndWriteStandard(true,"doAllPreprocessing", topFolderPath + ". with length: " + myPaths.size());

        int processedCounter=0;
        // Preprocess one file after other (TODO eventually add parallelism: mind that requests to dbs can already be parallelised)
        for (String documentPath : myPaths) {
            try {
                osa.getExtendedLogUtil().logAndWriteStandard(true,"doAllPreprocessing", "starting: "+documentPath);
                String detectedLanguage;
                if(language==null) {
                    // If language not defined detect it
                     detectedLanguage = osa
                            .getLanguageDetector()
                            .detectLanguage(FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8));
                }else{
                     detectedLanguage = language;
                }
                /*
                if(!detectedLanguage.equals("en")){ // susp part 21 10262
                    System.out.println("does this happen? If not just set en without classifier"); //susp part21/10420 de,  before 01500 (1 german), source 00008 german 00011 es, 13 es  .....
                }
                */
                List<SavedEntity> preprocessedCandExt = osa.preProcessExtendedInfo(documentPath, detectedLanguage);
                System.out.println("doAllPreprocessing -> done: "+documentPath+ " with ");
                processedCounter++;
            }catch(Exception ex){
                // Save exception to error log
                osa.getExtendedLogUtil().logAndWriteError(false, "doAllPreprocessing Error at "+documentPath+":"+ex);
            }
        }
        osa.getExtendedLogUtil().logAndWriteStandard(true,"doAllPreprocessing finished processing ", processedCounter+" of "+myPaths.size());
        return processedCounter;
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
