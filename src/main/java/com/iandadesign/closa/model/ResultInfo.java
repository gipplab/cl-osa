package com.iandadesign.closa.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for storing clustering algorithm results in CL-OSA extended.
 *
 * @author Johannes Stegmüller (22.06.2020)
 */
public class ResultInfo{
    private final int candStartCharIndex;
    private final int candEndCharIndex;
    private final int suspStartCharIndex;
    private final int suspEndCharIndex;
    private final int candLength;
    private final int suspLength;
    private boolean discardedByCombinationAlgo;
    private List<Integer> combinationMarkers;

    public ResultInfo(int candStartCharIndex, int candEndCharIndex, int suspStartCharIndex, int suspEndCharIndex){
        this.candStartCharIndex = candStartCharIndex;
        this.candEndCharIndex = candEndCharIndex;
        this.suspStartCharIndex = suspStartCharIndex;
        this.suspEndCharIndex = suspEndCharIndex;
        this.candLength = candEndCharIndex - candStartCharIndex;
        this.suspLength = suspEndCharIndex - suspStartCharIndex;
        this.discardedByCombinationAlgo = false;
        this.combinationMarkers = new ArrayList<>();
    }

    public void addCombinationMarker(int combinationMarker){
        this.combinationMarkers.add(combinationMarker);
    }

    /**
     * Adds a list of combination markers except value specified,
     * also doesnt add same value multiple
     * @param combinationMarkers
     * @param exceptValue
     */
    public void addCombinationMarkers(List<Integer> combinationMarkers, int exceptValue){
        for(int combomarker:combinationMarkers){
            if(combomarker!=exceptValue && !this.combinationMarkers.contains(combomarker)){
                this.combinationMarkers.add(combomarker);
            }
        }
    }
    public List<Integer> getCombinationMarkers(){
        return this.combinationMarkers;
    }

    public boolean wasDiscardedByCombinationAlgo() {
        return discardedByCombinationAlgo;
    }

    public void setDiscardedByCombinationAlgo(boolean discardedByCombinationAlgo) {
        this.discardedByCombinationAlgo = discardedByCombinationAlgo;
    }

    public int getCandStartCharIndex() {
        return candStartCharIndex;
    }

    public int getCandEndCharIndex() {
        return candEndCharIndex;
    }

    public int getSuspStartCharIndex() {
        return suspStartCharIndex;
    }

    public int getSuspEndCharIndex() {
        return suspEndCharIndex;
    }
    public int getCandLength(){
        if(this.candLength>=0){
            return this.candLength;
        }else{
            return 0;
        }
    }
    public int getSuspLength(){
        if(this.suspLength>=0){
            return this.suspLength;
        }else{
            return 0;
        }
    }
    public int getSize(){
        return this.getCandLength() * this.getSuspLength();
    }


    public boolean indexInCandArea(int candIndex){
        return candIndex >= getCandStartCharIndex() && candIndex <= getCandEndCharIndex();
    }

    public boolean indexInSuspArea(int suspIndex){
        return suspIndex >= getSuspStartCharIndex() && suspIndex <= getSuspEndCharIndex();
    }
}
