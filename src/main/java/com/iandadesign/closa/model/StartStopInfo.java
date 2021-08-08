package com.iandadesign.closa.model;

/**
 * Information holder class for storing minimum and maximum indices information.
 * @author Johannes Stegmüller
 */
public class StartStopInfo {
    int minMatchSuspIndex = -1;
    int maxMatchSuspIndex = -1;
    int minMatchCandIndex = -1;
    int maxMatchCandIndex = -1;


    public int getMaxMatchCandIndex() {
        return maxMatchCandIndex;
    }

    public int getMaxMatchSuspIndex() {
        return maxMatchSuspIndex;
    }

    public int getMinMatchCandIndex() {
        return minMatchCandIndex;
    }

    public int getMinMatchSuspIndex() {
        return minMatchSuspIndex;
    }

    public void setMaxMatchCandIndex(int maxMatchCandIndex) {
        this.maxMatchCandIndex = maxMatchCandIndex;
    }

    public void setMaxMatchSuspIndex(int maxMatchSuspIndex) {
        this.maxMatchSuspIndex = maxMatchSuspIndex;
    }

    public void setMinMatchCandIndex(int minMatchCandIndex) {
        this.minMatchCandIndex = minMatchCandIndex;
    }

    public void setMinMatchSuspIndex(int minMatchSuspIndex) {
        this.minMatchSuspIndex = minMatchSuspIndex;
    }
}
