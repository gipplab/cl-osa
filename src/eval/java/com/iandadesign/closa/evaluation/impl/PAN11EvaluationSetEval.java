package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.model.SavedEntity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag); // this has to be done immediatly after constructor
        int suspiciousPreprocessedFiles = doAllPreprocessing(toplevelPathSuspicious, osa);
        int candidatePreprocessedFiles = doAllPreprocessing(toplevelPathCandidates, osa);

        if((suspiciousPreprocessedFiles+candidatePreprocessedFiles)!=(11093*2)){
            return;
        }

        // Do the file comparisons



        // <String, Double> candidateScoresMap = osa.executeAlgorithmAndComputeScoresExtendedInfo(suspiciousPath, candidatePaths, tag);

        System.out.println("");

    }

    private static int doAllPreprocessing(String topFolderPath, OntologyBasedSimilarityAnalysis osa) {
        File myFiles = new File(topFolderPath);

        // Get all files to preprocess
        List<String> myPaths = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .filter(file -> file.getName().endsWith(".txt")) // Filter .xml files and others only take txt.
                .map(File::getPath)
                .collect(Collectors.toList());

        osa.getLogUtil().logAndWriteStandard("doAllPreprocessing for " + topFolderPath + ". with length: " + myPaths.size());

        int processedCounter=0;
        // Preprocess one file after other (TODO eventually add parallelism)
        for (String documentPath : myPaths) {
            try {
                System.out.println("doAllPreprocessing -> starting: "+documentPath);
                String detectedLanguage = osa
                        .getLanguageDetector()
                        .detectLanguage(FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8));
                if(!detectedLanguage.equals("en")){
                    System.out.println("does this happen? If not just set en without classifier");
                }
                List<SavedEntity> preprocessedCandExt = osa.preProcessExtendedInfo(documentPath, detectedLanguage);
                System.out.println("doAllPreprocessing -> done: "+documentPath+ " with ");
                processedCounter++;
            }catch(Exception ex){
                // Save exception to error log
                String message = "doAllPreprocessing Error at "+documentPath+":"+ex;
                osa.getLogUtil().logAndWriteStandard(message);
            }
        }
        osa.getLogUtil().logAndWriteStandard("doAllPreprocessing finished processing "+processedCounter+" of "+myPaths.size());
        return processedCounter;
    }
    @Test
    void evalCLASAAspecChinese() {
        CLASAEvaluationSet englishJapaneseASPECEvaluationSetCLASA = new CLASAEvaluationSet(
                new File(pathPrefix + "/ASPECxc/ja10000"), "en",
                new File(pathPrefix + "/ASPECxc/zh10000"), "ja",
                2000
        );

        englishJapaneseASPECEvaluationSetCLASA.printEvaluation();
    }



    @Test
    void evalCLASAPan11Documents() {
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/pan11/en"), "en",
                    new File(pathPrefix + "/pan11/es"), "es",
                    2000
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAPan11Sentences() {
        try {
            CLASAEvaluationSet englishSpanishPan11EvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/pan11/en"), "en",
                    new File(pathPrefix + "/sentences/pan11/es"), "es",
                    2000
            );
            englishSpanishPan11EvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    void evalCLASAJrcAcquisDocuments() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/jrc/en"), "en",
                    new File(pathPrefix + "/jrc/fr"), "fr",
                    1989
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAJrcAcquisSentences() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/jrc/en"), "en",
                    new File(pathPrefix + "/sentences/jrc/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAEuroparlDocuments() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/sentence_split_whole_paragraphs/europarl/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void evalCLASAEuroparlSentences() {
        try {
            CLASAEvaluationSet englishFrenchJrcAcquisEvaluationSetCLASA = new CLASAEvaluationSet(
                    new File(pathPrefix + "/sentences/europarl/en"), "en",
                    new File(pathPrefix + "/sentences/europarl/fr"), "fr",
                    2000
            );
            englishFrenchJrcAcquisEvaluationSetCLASA.printEvaluation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
