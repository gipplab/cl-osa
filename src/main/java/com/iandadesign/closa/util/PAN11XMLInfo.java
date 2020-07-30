package com.iandadesign.closa.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Information obtained from xml-file for each txt-document.
 *
 * Information is parsed in PAN11XMLParser
 *
 * @author Johannes Stegmüller (06.07.2020)
 */
public class PAN11XMLInfo {
    public String language;
    public List<PAN11PlagiarismInfo> plagiarismInfos;

    public PAN11XMLInfo(){
        plagiarismInfos = new ArrayList<PAN11PlagiarismInfo>();
    }

    public void setLanguage(String language){
        this.language = language;
    }

    public void addPlagiarismInfo(PAN11PlagiarismInfo plagiarismInfo){
        plagiarismInfos.add(plagiarismInfo);
    }

}
