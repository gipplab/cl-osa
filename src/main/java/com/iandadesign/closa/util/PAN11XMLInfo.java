package com.iandadesign.closa.util;

/**
 * Information obtained from xml-file for each txt-document.
 *
 * Information is parsed in PAN11XMLParser
 *
 * @author Johannes Stegmüller (06.07.2020)
 */
public class PAN11XMLInfo {
    public final String language;

    public PAN11XMLInfo(String lang){
        language = lang;
    }
}
