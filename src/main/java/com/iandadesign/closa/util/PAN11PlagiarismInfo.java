package com.iandadesign.closa.util;


/**
 * Plagiarism information obtained from xml-file for each txt-document.
 *
 * Information is parsed in PAN11XMLParser
 *
 * @author Johannes Stegmüller (30.07.2020)
 */
public class PAN11PlagiarismInfo {
    String type;
    String obfuscation;
    Boolean manualObfuscation;
    String thisLanguage;
    int thisOffset;
    int thisLength;
    String sourceReference;
    String sourceLanguage;
    int sourceOffset;
    int sourceLength;
    String caseLengthThis;
    String caseLengthSource;

    public String getType(){
        return type;
    }
    public Boolean getManualObfuscation() { return manualObfuscation;}
    public String getSourceLanguage(){
        return sourceLanguage;
    }
    public String getSourceReference(){
        return sourceReference;
    }
    public void calculateCaseLength(){
        if(thisLength < 700) {
            caseLengthThis = CaseLength.SHORT;
        } else if(thisLength < 5000) {
            caseLengthThis = CaseLength.MEDIUM;
        } else {
            caseLengthThis = CaseLength.LONG;
        }

        if(sourceLength < 700) {
            caseLengthSource = CaseLength.SHORT;
        } else if(sourceLength < 5000){
            caseLengthSource = CaseLength.MEDIUM;
        } else {
            caseLengthSource = CaseLength.LONG;
        }
    }
    public String getCaseLengthThis(){
        return caseLengthThis;
    }
    public String getCaseLengthSource(){
        return caseLengthSource;
    }

    public static class CaseLength{
        // F. Salvador 2016-2 p7. footnote:
        // We followed the PAN-PC-11 setup and considered as short cases those with less than 700 characters.
        // Long cases are those larger than 5000 characters (not clear if source or candidate was taken for length
        // classification, assumed source)
        public static String LONG = "LONG";
        public static String MEDIUM = "MEDIUM";
        public static String SHORT = "SHORT";
    }

    public int getThisLength() {
        return thisLength;
    }

    public int getThisOffset() {
        return thisOffset;
    }

    public int getSourceLength() {
        return sourceLength;
    }

    public int getSourceOffset() {
        return sourceOffset;
    }
}

/**
 * <feature name="plagiarism" type="artificial" obfuscation="low"
 * this_language="en" this_offset="55879" this_length="3461"
 * source_reference="source-document10626.txt" source_language="en" source_offset="8681" source_length="3560" />
**/
