package com.iandadesign.closa.model;

import com.iandadesign.closa.util.ExtendedLogUtil;

import java.lang.reflect.Field;

// Dataset settings
public class SalvadorAnalysisParameters {
    // Dataset settings
    public static boolean DO_FILE_PREFILTERING = true;            // Only take a limited amount of suspicious files
    public static int SUSP_FILE_LIMIT = 2;                        // Only take XX supicicious files with all candidates
    public static int SUSP_FILE_SELECTION_OFFSET = 0;             // Offset selection, default 0

    // Basic score calculation
    public static boolean USE_ABSOLUTE_SCORES = true;            // If false: use default normalized cosine-score for each

    // Fragmentation
    public static int FRAGMENT_SENTENCES = 14; //5;               // In Sentences
    public static int FRAGMENT_INCREMENT = 7; //2;                // In Sentences
    public static boolean GET_PLAGSIZED_FRAGMENTS = false;         // Get fragments exactly the plagiarism size

    // Clustering
    public static int THRESH1 = 800;                             // Fragment distance merging thresh
    public static double THRESH2 = 0.125; //0.686;      //0.086;    // Merged fragment selection thresh 0,1 too much (25) below too much 0.13
    public static double PRESELECTION_THRESH = 0.0;               // From the topmost candidates only the ones above this thresh get considered for merge
    public static int TOPMOST = 5;                                // topmost fetched suspicious for one plagiarism node
    // Clustering - Fragment Merge
    public static String FRAGMENT_MERGE_MODE = "keepingMax";        // "weightedAdd", "weightedAverage", "simpleAdd", "keepingMax"
    public static double WEIGHTED_ADD_CONSTANT = 0.3;             // The more, the higher scores have weightedAdd merged fragments

    // Analysis & MISC Parameters
    public static boolean DO_ANALYSIS = true;                     // Do additional analysis steps (deactivate for perfomance)
    public static double ANALYSIS_RELEVANCE_THRESH = 0.1;         // Fragments have to have at least XX percent overlap with plagiarism to go as positivies
    public static boolean DO_REGRESSION_ANALYSIS = false;
    public static boolean ONLY_PLAGFILES_IN_STATS = false;        // only consider candidate files which actually contain plagiarism for statistics calculation.

    // Experimental Features (Features in Development)
    public static boolean DO_STATISTICAL_WEIGHTING = false;     // weighting all found entities by tf/idf
    public static int WEIGHTING_SCHEME = 1;                     // See Wikipedia "Recommended Weighting Schemes: https://en.wikipedia.org/wiki/Tf%E2%80%93idf
    public static boolean DO_SCORES_MAP_CACHING = true;         // Cache scoresmap on base of parameters
    public static boolean DO_RELATIVE_SCORING_R_AT_K = false;   // default: false, relative scoring (how many characters of the found set are plagiarism, compared to the fetched chars)
    public static int MIN_FRAGMENT_SIZE_R_AT_K = 0;             // default: 0, minimum plagiarism size in chars, that a fragment counts
    public static boolean DISMISS_OVERLAPS_IN_R_AT_K = true;   // default: true, though much hardware, calculates r@k for actual detected plagsize and not overlapping area.
    public static boolean LOWER_K_MAX_PLAG_CAP_R_AT_K = false;  // if for lower k's complete cases can't be fetched, cap plagiarism there

    // Perfomance
    public static boolean DO_BATCHED_PROCESSING = false;         // if false calculates scoresmap and pan11 for all files at once, if not sequential
    public static int BATCHED_OFFSET_INCREMENT = 1;         // If DO_SEQUENTIAL_CALCULATAION is true, do batches of maximum size 20 suspicious files


    public static void printSalvadorMembers(ExtendedLogUtil logUtil) throws IllegalAccessException {
        logUtil.logAndWriteStandard(false, "Settings for Salvador Evaluation:---------------------");
        for(Field f : SalvadorAnalysisParameters.class.getDeclaredFields()) {
            Class type = f.getType();
            String key = f.getName();
            Object value = f.get(type);
            logUtil.logAndWriteStandard(true, key+":", value.toString());
        }
        logUtil.logAndWriteStandard(false, "------------------------------------------------------");
    }
}
