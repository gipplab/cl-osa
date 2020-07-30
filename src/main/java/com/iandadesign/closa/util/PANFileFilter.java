package com.iandadesign.closa.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Prefiltering class of files for PAN-PC 11
 *
 * @author Johannes Stegmüller (30.06.2020)
 */
public class PANFileFilter {

    public static class Languages{
        public static String English = "en";
        public static String Spanish = "es";
        public static String German = "de";
        public static List<String> allLanguages = new ArrayList<>(Arrays.asList(English, Spanish, German));
    }

    List<String> whiteListCandidates;
    List<String> whiteListSuspicious;
    List<String> whitelistLanguages;
    private final int maximumValue;


    public PANFileFilter(){
        this.maximumValue = 0; // not used
        this.whiteListCandidates = new ArrayList<>();
        this.whiteListSuspicious = new ArrayList<>();
        this.whitelistLanguages = new ArrayList<>();
    }
    public void addToWhiteListMultiple(boolean candOrSusp, int ... number){
        for (int i: number){
            addToWhiteList(i, candOrSusp);
        }
    }
    public void addToWhiteListMultiple(boolean candOrSusp, List<Integer> numbers){
        for (int i: numbers){
            addToWhiteList(i, candOrSusp);
        }
    }

    public void addLanguageToWhitelist(String ... languages) throws Exception {
        for(String language:languages){
            boolean languageIsOk = false;
            for(String comparisonLanguage:Languages.allLanguages){
                if(language.equals(comparisonLanguage)){
                    languageIsOk = true;
                    break;
                }
            }
            if(languageIsOk) {
                whitelistLanguages.add(language);
            }else{
                throw new Exception("Invalid language provided in language whitelist: "+ language);
            }
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


    public boolean checkIfLanguageWhitelisted(String language){
        return whitelistLanguages.contains(language);
    }
}
