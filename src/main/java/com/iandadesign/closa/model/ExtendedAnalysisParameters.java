package com.iandadesign.closa.model;

import com.iandadesign.closa.util.PANFileFilter;

public class ExtendedAnalysisParameters {

    //public final int LENGTH_SUBLIST_TOKENS;
    public final int NUM_SENTENCES_IN_SLIDING_WINDOW;
    public final int NUM_SENTENCE_INCREMENT_SLIDINGW;
    public final double ADJACENT_THRESH;
    public final double SINGLE_THRESH;
    public final boolean USE_ADAPTIVE_CLUSTERING_TRESH;     // Creates a clustering thresh based on the median of 2D-Matrix scores (!Takes longer because median calculation!)
    public final double ADAPTIVE_FORM_FACTOR;               // Form factor for adaptive clustering (Thresh = Median * FormFactor)
    public final int CLIPPING_MARGING;                      // Number of characters in squashing clusters when a cluster still is considered clipping
    public int MAX_NUM_CANDIDATES_SELECTED;           // The number of candidates per suspicious file which get selected for detailed comparison
    public final int CR_PRINT_LIMIT;                        // Candidate number which is results are printed to files and log.
    public double CANDIDATE_SELECTION_TRESH;          // Only if scoring above thresh a similar candidate gets selected (0 for off)
    public final boolean LOG_TO_CSV;                        // Log scoring map to csv
    public final boolean LOG_PREPROCESSING_RESULTS;         // Log the sliding window comparisons to a .txt file
    public final boolean LOG_STANDARD_TO_FILE;              // Log relevant standard output to .txt file
    public final boolean LOG_ERROR_TO_FILE;                 // Log error output to .txt file
    public final boolean LOG_VERBOSE;                       // Log more file outputs
    public boolean USE_FILE_FILTER;                         // Pre-Filter the used files like defined in panFileFilter
    public boolean USE_LANGUAGE_WHITELISTING;               // Only finds plagiarism in whitelisted languages.
    public boolean RUN_EVALUATION_AFTER_PROCESSING;   // Run evaluation python script after processing the plagiarism files
    public int PARALLELISM_THREAD_DIF;
    public final boolean USE_ENHANCHED_COSINE_ANALYSIS; // if enhanched taxomony
    public PANFileFilter panFileFilter;


    public ExtendedAnalysisParameters() throws Exception{
        // Token forming before making Wikidata query
        //LENGTH_SUBLIST_TOKENS = 3; // This is not used atm, but the parameter in config.properties dung refactoring reasons
        // Sliding window parameters (atm only possible increment == num_sentences)
        NUM_SENTENCES_IN_SLIDING_WINDOW = 20; //2
        NUM_SENTENCE_INCREMENT_SLIDINGW = 10; //1
        // Sliding window comparison thresholds
        ADJACENT_THRESH = 0.09;   //0,3; 0,1
        SINGLE_THRESH = 0.125;// 0,7; 0.8; //0.45act; //0.7;     //0,6
        USE_ADAPTIVE_CLUSTERING_TRESH = true; // false
        ADAPTIVE_FORM_FACTOR = 5.2; // 6 still false positive, rec ok
        CLIPPING_MARGING = 3000;
        USE_ENHANCHED_COSINE_ANALYSIS  = true;
        // Candidate retrieval settings
        MAX_NUM_CANDIDATES_SELECTED = 20;
        CR_PRINT_LIMIT = 10;
        CANDIDATE_SELECTION_TRESH = 0; //0.2;
        //Settings for Logging etc
        LOG_TO_CSV= true;
        LOG_PREPROCESSING_RESULTS= true;
        LOG_STANDARD_TO_FILE= false;
        LOG_ERROR_TO_FILE=false;
        LOG_VERBOSE=false;
        // Evaluation settings
        RUN_EVALUATION_AFTER_PROCESSING = true;
        // Parallelism Settings
        PARALLELISM_THREAD_DIF = 20; // difference from available processors

        // File Filter Options
        USE_FILE_FILTER=false;
        USE_LANGUAGE_WHITELISTING=false;
        // Add a file filter (only used if USE_FILE_FILTER is true)
        panFileFilter = new PANFileFilter();
        // Add Candidate Whitelisting
        panFileFilter.addToWhiteListMultiple(true, 1, 14, 3164, 4001, 71, 76, 3317);
        // Add Suspicious Whitelisting
        panFileFilter.addToWhiteListMultiple(false, 2, 45, 20, 34);
        //panFileFilter.addToWhiteListMultiple(true, 3164);
        //panFileFilter.addToWhiteListMultiple(false, 20);
        // cand has only: "en", "de", "es"
        // susp has only: "en"
        panFileFilter.addLanguageToWhitelist("de", "es");

    }

    public boolean checkIfInLanguageWhitelist(String language){
        if(!this.USE_LANGUAGE_WHITELISTING){
            return true;
        }
        return panFileFilter.checkIfLanguageWhitelisted(language);
    }
}
