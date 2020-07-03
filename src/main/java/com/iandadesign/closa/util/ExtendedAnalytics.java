package com.iandadesign.closa.util;

import com.iandadesign.closa.model.ScoringChunk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class provides additional analysis methods for
 * the clustering in CL-OSA extended algorithm.
 *
 * The functions are rather used for analysis than in processing.
 *
 * @author Johannes Stegmüller (02.07.2020)
 */
public  class ExtendedAnalytics {
    public static double calculateMedian(ScoringChunk[][] scoreMatrix, ExtendedLogUtil logUtil, String extraInfo){
        //System.out.println("Calculating median");
        List<Double> myValues = new ArrayList<>();
        for(ScoringChunk[] scoreRow: scoreMatrix){
            for(ScoringChunk scoreItem:scoreRow){
                if(scoreItem==null){
                    // myValues.add(0.0);
                    continue;
                }else{
                    double value = scoreItem.getComputedCosineSimilarity();
                    if(value==-1) continue;
                    myValues.add(value);
                }
            }
        }
        Collections.sort(myValues);
        double medianValue;
        float medianPos = (myValues.size() + 1) / (float) 2;
        if((myValues.size()+1)%2 > 0){
            // Size is even-> middle value of 2
            int firstIndex = (int) Math.floor(medianPos);
            int secondIndex = (int) Math.abs(medianPos);

            medianValue = (myValues.get(firstIndex)+myValues.get(secondIndex))/2;
        }else{
            // Size is uneven - take the middle
            int index = (int) medianPos;
            medianValue = myValues.get(index);

        }
        if(logUtil!=null){
            logUtil.logAndWriteError(false, extraInfo, "Median of populated chunks is:", medianValue);
        }
        return medianValue;
    }
}
