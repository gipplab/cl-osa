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

    public ResultInfo(int candStartCharIndex, int candEndCharIndex, int suspStartCharIndex, int suspEndCharIndex){
        this.candStartCharIndex = candStartCharIndex;
        this.candEndCharIndex = candEndCharIndex;
        this.suspStartCharIndex = suspStartCharIndex;
        this.suspEndCharIndex = suspEndCharIndex;
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
}
