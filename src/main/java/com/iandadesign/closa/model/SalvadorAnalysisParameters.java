package com.iandadesign.closa.model;

import com.iandadesign.closa.util.ExtendedLogUtil;

import java.lang.reflect.Field;

// Dataset settings
public class SalvadorAnalysisParameters {
    // Dataset settings
    public static boolean DO_FILE_PREFILTERING = true;            // Only take a limited amount of suspicious files
    public static int SUSP_FILE_LIMIT = 1;                        // Only take XX supicicious files with all candidates

    // Basic score calculation
    public static boolean USE_ABSOLUTE_SCORES = false;            // If false: use default normalized cosine-score for each

    // Fragmentation
    public static int FRAGMENT_SENTENCES = 14; //5;               // In Sentences
    public static int FRAGMENT_INCREMENT = 7; //2;                // In Sentences
    public static boolean GET_PLAGSIZED_FRAGMENTS = true;         // Get fragments exactly the plagiarism size

    // Clustering
    public static int THRESH1 = 1500;                             // Fragment distance merging thresh
    public static double THRESH2 = 0.33; //0.686;      //0.086;   // Merged fragment selection thresh 0,1 too much (25) below too much 0.13
    public static int TOPMOST = 5;                                // topmost fetched suspicious for one plagiarism node
    // Clustering - Fragment Merge
    public static String FRAGMENT_MERGE_MODE = "weightedAverage"; // "weightedAdd", "weightedAverage", "simpleAdd"
    public static double WEIGHTED_ADD_CONSTANT = 0.3;             // The more, the higher scores have weightedAdd merged fragments

    // Analysis & MISC Parameters
    public static boolean DO_ANALYSIS = true;                     // Do additional analysis steps (deactivate for perfomance)


    public static void printSalvadorMembers(ExtendedLogUtil logUtil) throws IllegalAccessException {
        logUtil.logAndWriteStandard(false, "Settings for Salvador Evaluation:---------------------");
        for(Field f : SalvadorAnalysisParameters.class.getDeclaredFields()) {
            Class type = f.getType();
            String key = f.getName();
            Object value = f.get(type);
            logUtil.logAndWriteStandard(true, key+":",value.toString());
        }
        logUtil.logAndWriteStandard(false, "------------------------------------------------------");
    }
}
