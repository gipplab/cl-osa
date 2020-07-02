package com.iandadesign.closa.model;

import com.iandadesign.closa.util.PANFileFilter;

public class ExtendedAnalysisParameters {

    //public final int LENGTH_SUBLIST_TOKENS;
    public final int NUM_SENTENCES_IN_SLIDING_WINDOW;
    public final int NUM_SENTENCE_INCREMENT_SLIDINGW;
    public final double ADJACENT_THRESH;
    public final double SINGLE_THRESH;
    public final int CLIPPING_MARGING;              // Number of characters in squashing clusters when a cluster still is considered clipping
    public final int MAX_NUM_CANDIDATES_SELECTED;   // The number of candidates per suspicious file which get selected for detailed comparison
    public final int CR_PRINT_LIMIT;                // Candidate number which is results are printed to files and log.
    public final double CANDIDATE_SELECTION_TRESH;     // Only if scoring above thresh a similar candidate gets selected (0 for off)
    public final boolean LOG_TO_CSV;                // Log scoring map to csv
    public final boolean LOG_PREPROCESSING_RESULTS; // Log the sliding window comparisons to a .txt file
    public final boolean LOG_STANDARD_TO_FILE;      // Log relevant standard output to .txt file
    public final boolean LOG_ERROR_TO_FILE;         // Log error output to .txt file
    public final boolean LOG_VERBOSE;               // Log more file outputs
    public final boolean USE_FILE_FILTER;           // Pre-Filter the used files like defined in panFileFilter
    public final PANFileFilter panFileFilter;


    public ExtendedAnalysisParameters(){
        // Token forming before making Wikidata query
        //LENGTH_SUBLIST_TOKENS = 3; // This is not used atm, but the parameter in config.properties dung refactoring reasons
        // Sliding window parameters (atm only possible increment == num_sentences)
        NUM_SENTENCES_IN_SLIDING_WINDOW = 2;
        NUM_SENTENCE_INCREMENT_SLIDINGW = 1;
        // Sliding window comparison thresholds
        ADJACENT_THRESH = 0.3;   //0,1
        SINGLE_THRESH = 0.5; //0.7;     //0,6
        CLIPPING_MARGING = 100;
        // Candidate retrieval settings
        MAX_NUM_CANDIDATES_SELECTED = 2;
        CR_PRINT_LIMIT = 10;
        CANDIDATE_SELECTION_TRESH = 0.2;
        //Settings for Logging etc
        LOG_TO_CSV= true;
        LOG_PREPROCESSING_RESULTS= true;
        LOG_STANDARD_TO_FILE= true;
        LOG_ERROR_TO_FILE=true;
        LOG_VERBOSE=false;

        // File Filter Options
        USE_FILE_FILTER=true;
        // Add a file filter (only used if USE_FILE_FILTER is true)
        panFileFilter = new PANFileFilter(11093);
        // Add Candidate Whitelisting
        //panFileFilter.addToWhiteListMultiple(true, 1, 14, 3164, 4001, 71, 76, 3317);
        // Add Suspicious Whitelisting
        //panFileFilter.addToWhiteListMultiple(false, 2, 45, 20, 34);
        panFileFilter.addToWhiteListMultiple(true, 3164);
        panFileFilter.addToWhiteListMultiple(false, 20);

    }
}
