package com.iandadesign.closa.model;

import com.iandadesign.closa.util.ExtendedLogUtil;

/**
 * Information holder for recall at k calculation.
 * @author Johannes Stegmüller
 */
public class SalvadorRatKResponse {
    public int k;
    public double recallAtK;
    public long findings;
    public long possibleFindings;

    public void refreshRatK(){
        recallAtK = 100 * findings / (double) possibleFindings;
    }
    public void logMe(ExtendedLogUtil logUtil){
        logUtil.logAndWriteStandard(false, "Recall at ", k, " is: ", recallAtK, "Findings/PossibleFindings (",findings,"/", possibleFindings,")");

    }
}
