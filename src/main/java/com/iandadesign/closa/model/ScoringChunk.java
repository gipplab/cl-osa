package com.iandadesign.closa.model;

import java.util.List;

/**
 * This is an information holder for storing two
 * related SlidingWindows and their computed
 * cosineSimilarityAnalysis score.
 *
 * @author Johannes Stegmüller on 2020/05/29/
 */
public class ScoringChunk {
    public final SlidingWindowInfo suspiciousWindow;
    public final SlidingWindowInfo candidateWindow;
    public final double computedCosineSimilarity;
    public final long fragmentIndex;
    private final String printFormat;
    // Clustering algorithm related properties.
    private int suspiciousMatrixIndex;
    private int candidateMatrixIndex;
    private boolean processedByClusteringAlgo;

    public ScoringChunk(SlidingWindowInfo suspiciousWindow,
                        SlidingWindowInfo candidateWindow,
                        double computedCosineSimilarity,
                        long fragmentIndex) {

        this.suspiciousWindow = suspiciousWindow;
        this.candidateWindow = candidateWindow;
        this.computedCosineSimilarity = computedCosineSimilarity;
        this.fragmentIndex = fragmentIndex;
        this.printFormat = "%-40s%s%n";
        this.processedByClusteringAlgo = false;
    }

    public void printMe(List<String> suspiciousTokens, List<String> candidateTokens){
        System.out.printf(printFormat, "Fragment Number:", fragmentIndex);
        System.out.printf(printFormat, "Suspicious Start Sentence:", suspiciousWindow.getStartSentence());
        System.out.printf(printFormat, "Candidate Start Sentence:", candidateWindow.getStartSentence());
        System.out.printf(printFormat, "Suspicious Tokens:", suspiciousTokens);
        System.out.printf(printFormat, "Candidate Tokens:", candidateTokens);
        System.out.printf(printFormat, "Fragment Score:", computedCosineSimilarity);
    }

    public double getComputedCosineSimilarity() {
        return computedCosineSimilarity;
    }
    public long getFragmentIndex() {
        return fragmentIndex;
    }
    public int getSuspiciousMatrixIndex(){
        return suspiciousMatrixIndex;
    }
    public int getCandidateMatrixIndex(){
        return candidateMatrixIndex;
    }
    public void setSuspiciousMatrixIndex(int suspiciousMatrixIndex){
        this.suspiciousMatrixIndex = suspiciousMatrixIndex;
    }
    public void setCandidateMatrixIndex(int candidateMatrixIndex){
        this.candidateMatrixIndex = candidateMatrixIndex;
    }
    public boolean isProcessedByClusteringAlgo(){
        return processedByClusteringAlgo;
    }
    public void setProcessedByClusteringAlgo(boolean processedByClusteringAlgo){
        this.processedByClusteringAlgo = processedByClusteringAlgo;
    }

    public SlidingWindowInfo getCandidateWindow() {
        return candidateWindow;
    }
    public SlidingWindowInfo getSuspiciousWindow(){
        return suspiciousWindow;
    }

    public void deinitialize(){
        suspiciousWindow.deinitialize();
        candidateWindow.deinitialize();
    }
}