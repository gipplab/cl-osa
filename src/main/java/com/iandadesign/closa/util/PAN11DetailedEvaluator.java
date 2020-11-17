package com.iandadesign.closa.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Triggers PAN11 Evaluation Scripts and computes the scores.
 * Mainly for Detailed Analysis.
 * @author Johannes Stegmüller
 */
public class PAN11DetailedEvaluator {

    public static void triggerPAN11PythonEvaluation(ExtendedLogUtil logUtil, String baseResultsPath, String baseplagPath) {
        logUtil.logAndWriteStandard(true,logUtil.getDateString(),"Running evaluation tool for PAN11");
        //String pathToScript = "src/eval/resources/com/iandadesign/closa/evaluation/pan-pc11/pan09-plagiarism-detection-perfomance-measures.py";
        //TODO bring back resources and PAN11EvaluationSetEval to eval
        String pathToScript = "src/main/resources/pan-pc11/pan09-plagiarism-detection-perfomance-measures.py";
        //String pathToScript = "D:\\AA_ScienceProject\\Wikidata_vs_CharacterNGram\\PAN2011Evaluator\\pan09-plagiarism-detection-perfomance-measures.py";
        //String plagPath ="D:\\AA_ScienceProject\\Data\\pan-plagiarism-corpus-2011\\pan-plagiarism-corpus-2011\\external-detection-corpus\\suspicious-document";
        String plagPath = baseplagPath; // Path with the susp
        //String detectedPlagiarismPath="D:\\CL_OSA_caching\\preprocessed_extended\\results_comparison\\evalPAN2011All_2020_07_03_14_11_26";
        String detectedPlagiarismPath = baseResultsPath;
        try{
            ProcessBuilder builder = new ProcessBuilder();
            // builder.environment()
            logUtil.logAndWriteStandard(false,"plag-path",plagPath,"det-path",detectedPlagiarismPath);
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
}
