package com.iandadesign.closa.model;

import com.iandadesign.closa.util.PANFileFilter;

public class ExtendedAnalysisParameters {

    //public final int LENGTH_SUBLIST_TOKENS;
    public final int NUM_SENTENCES_IN_SLIDING_WINDOW;
    public final int NUM_SENTENCE_INCREMENT_SLIDINGW;
    public final double ADJACENT_THRESH;
    public final double SINGLE_THRESH;
    public final int MAX_NUM_CANDIDATES_SELECTED;   // The number of candidates per suspicious file which get selected for detailed comparison
    public final int CANDIDATE_SELECTION_TRESH;     // Only if scoring above thresh a similar candidate gets selected (0 for off)
    public final boolean LOG_TO_CSV;                // Log scoring map to csv
    public final boolean LOG_PREPROCESSING_RESULTS; // Log the sliding window comparisons to a .txt file
    public final boolean LOG_STANDARD_TO_FILE;      // Log relevant standard output to .txt file
    public final boolean LOG_ERROR_TO_FILE;         // Log error output to .txt file
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
        SINGLE_THRESH = 0.7;     //0,6
        // Candidate retrieval settings
        MAX_NUM_CANDIDATES_SELECTED = 2;
        CANDIDATE_SELECTION_TRESH = 0;
        //Settings for Logging etc
        LOG_TO_CSV= true;
        LOG_PREPROCESSING_RESULTS= true;
        LOG_STANDARD_TO_FILE= true;
        LOG_ERROR_TO_FILE=true;

        // File Filter Options
        USE_FILE_FILTER=true;
        // Add a file filter (only used if USE_FILE_FILTER is true)
        panFileFilter = new PANFileFilter(11093);
        // Add Candidate Whitelisting
        panFileFilter.addToWhiteListMultiple(true, 1, 14, 2000, 55);
        // Add Suspicious Whitelisting
        panFileFilter.addToWhiteListMultiple(false, 2, 45, 222, 110);
    }
}
