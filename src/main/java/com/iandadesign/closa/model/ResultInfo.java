package com.iandadesign.closa.model;

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

    public ResultInfo(int candStartCharIndex, int candEndCharIndex, int suspStartCharIndex, int suspEndCharIndex){
        this.candStartCharIndex = candStartCharIndex;
        this.candEndCharIndex = candEndCharIndex;
        this.suspStartCharIndex = suspStartCharIndex;
        this.suspEndCharIndex = suspEndCharIndex;
        this.candLength = candEndCharIndex - candStartCharIndex;
        this.suspLength = suspEndCharIndex - suspStartCharIndex;
        this.discardedByCombinationAlgo = false;
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
}
