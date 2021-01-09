package com.iandadesign.closa.model;

import static java.lang.Integer.max;

public class SalvadorFinding {

    int candStartPosition;
    int candStopPosition;
    boolean isMerged;

    public SalvadorFinding(int candStartPos, int candStopPos){
        candStartPosition = candStartPos;
        candStopPosition = candStopPos;
        isMerged = false;
    }

    public boolean isMerged() {
        return isMerged;
    }

    public void setMerged(boolean merged) {
        isMerged = merged;
    }

    public int getCandStartPosition(){
        return candStartPosition;
    }
    public int getCandStopPosition(){
        return candStopPosition;
    }
    public int getSize(){
       return max(0,candStopPosition-candStartPosition);
    }
}
