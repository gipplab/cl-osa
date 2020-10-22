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
    private final int candidateStartSentence;
    private final int candidateEndSentence;
    private final int candidateCharacterStartIndex;
    private final int candidateCharacterEndIndex;

    private final int suspiciousStartSentence;
    private final int suspiciousEndSentence;
    private final int suspiciousCharacterStartIndex;
    private final int suspiciousCharacterEndIndex;

    private final StartStopInfo startStopInfo; // This is storing start stop indices of matches if used.

    private double computedCosineSimilarity;
    // Clustering algorithm related properties.
    private int suspiciousMatrixIndex;
    private int candidateMatrixIndex;
    private boolean processedByClusteringAlgo;

    public ScoringChunk(SlidingWindowInfo suspiciousWindow,
                        SlidingWindowInfo candidateWindow,
                        double computedCosineSimilarity,
                        long fragmentIndex,
                        StartStopInfo startStopInfo) {

        this.suspiciousStartSentence = suspiciousWindow.getStartSentence();
        this.suspiciousEndSentence = suspiciousWindow.getEndSentence();
        this.suspiciousCharacterStartIndex = suspiciousWindow.getCharacterStartIndex();
        this.suspiciousCharacterEndIndex = suspiciousWindow.getCharacterEndIndex();

        this.candidateStartSentence = candidateWindow.getStartSentence();
        this.candidateEndSentence = candidateWindow.getEndSentence();
        this.candidateCharacterStartIndex = candidateWindow.getCharacterStartIndex();
        this.candidateCharacterEndIndex = candidateWindow.getCharacterEndIndex();
        this.startStopInfo = startStopInfo;

        this.computedCosineSimilarity = computedCosineSimilarity;
        //this.fragmentIndex = fragmentIndex;
        this.processedByClusteringAlgo = false;
    }

    public void printMe(String printFormat, List<String> suspiciousTokens, List<String> candidateTokens){
        //System.out.printf(printFormat, "Fragment Number:", fragmentIndex);
        System.out.printf(printFormat, "Suspicious Start Sentence:", this.suspiciousStartSentence);
        System.out.printf(printFormat, "Candidate Start Sentence:", this.candidateStartSentence);
        System.out.printf(printFormat, "Suspicious Tokens:", suspiciousTokens);
        System.out.printf(printFormat, "Candidate Tokens:", candidateTokens);
        System.out.printf(printFormat, "Fragment Score:", computedCosineSimilarity);
    }

    public double getComputedCosineSimilarity() {
        return computedCosineSimilarity;
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

    public int getCandidateCharacterStartIndex() {
        return candidateCharacterStartIndex;
    }

    public int getCandidateCharacterEndIndex() {
        return candidateCharacterEndIndex;
    }

    public int getCandidateEndSentence() {
        return candidateEndSentence;
    }

    public int getCandidateStartSentence() {
        return candidateStartSentence;
    }

    public int getSuspiciousCharacterEndIndex() {
        return suspiciousCharacterEndIndex;
    }

    public int getSuspiciousCharacterStartIndex() {
        return suspiciousCharacterStartIndex;
    }

    public int getSuspiciousEndSentence() {
        return suspiciousEndSentence;
    }

    public int getSuspiciousStartSentence() {
        return suspiciousStartSentence;
    }

    public int getAverageLength(){
        return (suspiciousCharacterEndIndex - suspiciousCharacterStartIndex) + (candidateCharacterEndIndex - candidateCharacterStartIndex) / 2;
    }
    public void setComputedCosineSimilarity(double similarity){
        this.computedCosineSimilarity = similarity;
    }

    public StartStopInfo getStartStopInfo() {
        return startStopInfo;
    }
}