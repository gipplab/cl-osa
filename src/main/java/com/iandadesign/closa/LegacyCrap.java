package com.iandadesign.closa;

import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.SavedEntity;
import com.iandadesign.closa.model.StatisticsInfo;
import com.iandadesign.closa.util.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LegacyCrap {
    static String pathPrefix = "/data/pan-plagiarism-corpus-2011/external-detection-corpus";




    static void doCREvaluation(ExtendedAnalysisParameters params, String tag, String comment, HashMap<String, List<String>> resultSelectedCandidates){
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory;

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);

        logUtil.writeStandardReport(false, "Assuming the preprpocessing has been done here. ");
        params.MAX_NUM_CANDIDATES_SELECTED = 202;
        params.CANDIDATE_SELECTION_TRESH = 0;

        PAN11EvaluationSetEval.logParams(logUtil, tag, params, osa);


        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");


        AtomicInteger overallPlagiariasmFiles = new AtomicInteger();
        AtomicInteger overallMatches = new AtomicInteger();
        AtomicInteger maxPos = new AtomicInteger();
        AtomicInteger averagePos = new AtomicInteger();

        suspiciousFiles.forEach((suspiciousFile) -> {
            String suspPath = suspiciousFile.getPath();
            String suspFileName = suspiciousFile.getName();
            try {
                logUtil.logAndWriteStandard(true, logUtil.getDateString(), "Parsing Suspicious file Size:" , suspiciousFiles.size(), "Filename:", suspFileName, " and its", candidateFiles.size(), "candidates");
                //OntologyBasedSimilarityAnalysis osaT = new OntologyBasedSimilarityAnalysis(null, null);
                //osaT.setLogger(osa.getExtendedLogUtil(), osa.getTag()); // this has to be done immediately after constructor
                //osaT.initializeLogger(tag, params);
                WeakHashMap<String, List<SavedEntity>> suspiciousIdTokensMapExt = new WeakHashMap<>();
                Map<String, Double> selectedCandidates = osa.doCandidateRetrievalExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString(), suspiciousIdTokensMapExt);


                suspiciousIdTokensMapExt.clear();
                // Get the corresponding result candidates
                List<String> actualCandidates = resultSelectedCandidates.get(suspFileName.replace(".txt",".xml"));
                overallPlagiariasmFiles.addAndGet(actualCandidates.size());
                // Do a comparison here quickly

                int posCounter = 0;
                int matchCandidates = 0;
                for(String selectedCandidate:selectedCandidates.keySet()) {
                    File filename = new File(selectedCandidate);

                    if(actualCandidates.contains(filename.getName())){
                        logUtil.logAndWriteStandard(false, "Found Candidate at pos: "+posCounter+"\t score: "+selectedCandidates.get(selectedCandidate));
                        if(posCounter > maxPos.get()){
                            maxPos.set(posCounter);
                        }
                        matchCandidates++;
                    }
                    posCounter++;

                }
                logUtil.logAndWriteStandard(false, "Matched candidates: " + matchCandidates+ "/"+actualCandidates.size());
                overallMatches.addAndGet(matchCandidates);
                // Save score for complete comparison.
                logUtil.logAndWriteStandard(false, "Overall Matched candidates: " + overallMatches.get()+ "/"+ overallPlagiariasmFiles.get() + " max pos: " + maxPos.get());

                System.gc(); // Explicit call to garbage collector.
            } catch (Exception ex) {
                logUtil.logAndWriteError(false, "Exception during parse of suspicious file with Filename", suspFileName, "Exception:");
                ex.printStackTrace();
            }
        });
        logUtil.logAndWriteStandard(false, "Overall Matched candidates: " + overallMatches.get()+ "/"+ overallPlagiariasmFiles.get()+ " max pos: " + maxPos.get());

    }

    /**
     * THis is the old method for evalPAN2011All from PAN11EvaluationSetEval
     * @param params
     * @param tag
     * @param comment
     */
    static void evalPAN2011All(ExtendedAnalysisParameters params, String tag, String comment)  {
        // Route the complete output to a logfile here.
        String toplevelPathSuspicious = PAN11EvaluationSetEval.pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = PAN11EvaluationSetEval.pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory;

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();
        logUtil.logAndWriteStandard(false, comment);
        // ..

        PAN11EvaluationSetEval.logParams(logUtil, tag, params, osa);


        logUtil.logAndWriteStandard(false, "Preprocessing all candidate files...");
        int candidatePreprocessedFiles = PAN11EvaluationSetEval.doAllPreprocessing(toplevelPathCandidates, osa, null, params, true);  // candidate docs can be en,de,es
        logUtil.logAndWriteStandard(false, "Preprocessing all suspicious files...");
        int suspiciousPreprocessedFiles = PAN11EvaluationSetEval.doAllPreprocessing(toplevelPathSuspicious, osa, "en", params, false); // all suspicious docs are english
        int numfiles = suspiciousPreprocessedFiles+candidatePreprocessedFiles;
        logUtil.logAndWriteStandard(false, "Number of preprocessed files is: "+numfiles);
        if(!params.USE_FILE_FILTER && !params.USE_LANGUAGE_WHITELISTING && numfiles!=(11093*2)){
            logUtil.logAndWriteError(false, "Aborting: Processed files not 2*11093=22186 but "+numfiles);
            return;
        }
        logUtil.logAndWriteStandard(false, logUtil.dashes(100));

        logUtil.logAndWriteStandard(false, "Starting file comparisons...");
        List<File> candidateFiles = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".txt");
        List<File> suspiciousFiles  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".txt");


        // MEMORY MARK 2: free memory here
        // memory usage OK until here

        int parsedFiles = 0;
        int parsedErrors = 0;
        String baseResultsPath = "";

        // Do the file comparisons
        if(!osa.getDoParallelRequests()) {
            // Single Thread Execution
            for (int index = 0; index < suspiciousFiles.size(); index++) {
                String suspPath = suspiciousFiles.get(index).getPath();
                String suspFileName = suspiciousFiles.get(index).getName();
                try {
                    logUtil.logAndWriteStandard(true, logUtil.getDateString(), "Parsing Suspicious file ", index + 1, "/", suspiciousFiles.size(), "Filename:", suspFileName, " and its", candidateFiles.size(), "candidates");
                    OntologyBasedSimilarityAnalysis osaT = new OntologyBasedSimilarityAnalysis();
                    osaT.initializeLogger(tag, params); // this has to be done immediately after constructor
                    osaT.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString(), null);
                    baseResultsPath = osaT.getExtendedXmlResultsPath();
                    parsedFiles++;
                } catch (Exception ex) {
                    parsedErrors++;
                    logUtil.logAndWriteError(false, "Exception during parse of suspicious file with Filename", suspFileName, "Exception:", ex);
                }
            }
            logUtil.logAndWriteStandard(true, logUtil.getDateString(), "done processing PAN11,- parsed files:", parsedFiles, "errors:", parsedErrors);
        } else {
            // Multi Thread Execution

            // Limit: https://www.codementor.io/@nitinpuri/controlling-parallelism-of-java-8-collection-streams-umex0qbt1
            AtomicInteger indexP = new AtomicInteger(0);
            AtomicInteger parsedFilesP = new AtomicInteger(0);
            AtomicInteger parsedErrorsP = new AtomicInteger(0);
            logUtil.logAndWriteStandard(true, logUtil.getDateString(), " Using parallelism, counter (x/y) is only a vague indicator");

            int parallelism = Runtime.getRuntime().availableProcessors() - params.PARALLELISM_THREAD_DIF;
            if(parallelism < 1){
                logUtil.writeStandardReport(false, "Starting Parallel Processing Dif in settings too big");
                parallelism = 1;
            }
            logUtil.writeStandardReport(false, "Starting Parallel Processing with Num CPUs: "+ parallelism);
            ForkJoinPool forkJoinPool = null;

            try {

                forkJoinPool = new ForkJoinPool(parallelism);
                forkJoinPool.submit(() -> suspiciousFiles.parallelStream().forEach((suspiciousFile) -> {
                    String suspPath = suspiciousFile.getPath();
                    String suspFileName = suspiciousFile.getName();
                    try {
                        logUtil.logAndWriteStandard(true, logUtil.getDateString(), "Parsing Suspicious file ", indexP.get() + 1, "/", suspiciousFiles.size(), "Filename:", suspFileName, " and its", candidateFiles.size(), "candidates");
                        parsedFilesP.getAndIncrement();
                        indexP.getAndIncrement();
                        OntologyBasedSimilarityAnalysis osaT = new OntologyBasedSimilarityAnalysis(null, null);
                        osaT.setLogger(osa.getExtendedLogUtil(), osa.getTag()); // this has to be done immediately after constructor
                        osaT.executeAlgorithmAndComputeScoresExtendedInfo(suspPath, candidateFiles, params, logUtil.getDateString(), null);
                        osaT = null;
                        System.gc(); // Excplicit call to garbage collector.
                    } catch (Exception ex) {
                        parsedErrorsP.getAndIncrement();
                        indexP.getAndIncrement();
                        logUtil.logAndWriteError(false, "Exception during parse of suspicious file with Filename", suspFileName, "Exception:");
                        ex.printStackTrace();
                    }
                })).get();

            }catch(Exception e) {
                logUtil.logAndWriteError(false, "Exception with with thread execution:", e);
            } finally {
                if (forkJoinPool != null) {
                    forkJoinPool.shutdown(); //always remember to shutdown the pool
                }
            }

            // Generating xmlResultsfolderPath as it is done in each thread atm.
            String xmlResultsFolderPath = Paths.get(osa.getPreprocessedCachingDirectory(), "preprocessed_extended",
                    "results_comparison", tag.concat("_").concat(logUtil.getDateString())).toAbsolutePath().toString();
            baseResultsPath = xmlResultsFolderPath;
            parsedFiles = parsedFilesP.get();
            parsedErrors = parsedErrorsP.get();
        }


        if(parsedErrors>=1 || parsedFiles==0){
            return;
        }


        //TODO MEMORY FREE MEMORY HERE ? nO


        // Evaluation related stuff ...
        if(params.RUN_EVALUATION_AFTER_PROCESSING){
            if(!params.USE_FILE_FILTER){
                // No filter-> just do the regular evaluation with all files
                PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, baseResultsPath, toplevelPathSuspicious);
            }else{
                // Filter used, only compare with relevant files
                File cachingDir= new File(baseResultsPath +"/file_selection_cache");
                PAN11FileUtil.removeDirectory(cachingDir);
                List<File> suspiciousXML  = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
                PAN11FileUtil.writeFileListToDirectory(suspiciousXML, cachingDir.getPath(), logUtil);
                PAN11DetailedEvaluator.triggerPAN11PythonEvaluation(logUtil, baseResultsPath, cachingDir.getPath());
                PAN11FileUtil.removeDirectory(cachingDir);
            }
            logUtil.logAndWriteStandard(true,logUtil.getDateString(), "done doing evaluation for PAN11");
        }
        logUtil.closeStreams();
    }
}
