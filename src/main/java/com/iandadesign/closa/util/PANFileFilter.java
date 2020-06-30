package com.iandadesign.closa.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Prefiltering class of files for PAN-PC 11
 *
 * @author Johannes Stegmüller (30.06.2020)
 */
public class PANFileFilter {
    List<String> whiteListCandidates;
    List<String> whiteListSuspicious;
    private final int maximumValue;

    public PANFileFilter(int maximumValue){
        this.maximumValue = maximumValue;
        this.whiteListCandidates = new ArrayList<>();
        this.whiteListSuspicious = new ArrayList<>();
    }
    public void addToWhiteListMultiple(boolean candOrSusp, int ... number){
        for (int i: number){
            addToWhiteList(i, candOrSusp);
        }
    }
    public void addRangeToWhiteList(int firstNumber, int lastNumber, boolean candOrSusp){
        for(int i=firstNumber;i<lastNumber;i++){
            addToWhiteList(i, candOrSusp);
        }
    }
    public void addToWhiteList(int number, boolean candOrSusp){
        DecimalFormat df = new DecimalFormat("00000");
        String numberAsString = df.format(number);
        if(candOrSusp){
            whiteListCandidates.add(numberAsString);
        }else{
            whiteListSuspicious.add(numberAsString);
        }
    }
    public List<String> getWhiteListCandidates() {
        return whiteListCandidates;
    }
    public List<String> getWhiteListSuspicious() {
        return whiteListSuspicious;
    }
    public boolean checkIfFilenameWhitelisted(String fileName, boolean candOrSusp){
        if(candOrSusp){
           return whiteListCandidates.contains(getNumberFromFileName(fileName));
        }else{
           return whiteListSuspicious.contains(getNumberFromFileName(fileName));
        }
    }
    private String getNumberFromFileName(String fileName){
        return fileName.replaceAll("\\D+","");
    }
}
