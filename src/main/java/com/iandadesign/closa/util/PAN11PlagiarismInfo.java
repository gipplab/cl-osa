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
    String thisLanguage;
    int thisOffset;
    int thisLength;
    String sourceReference;
    String sourceLanguage;
    int sourceOffset;
    int sourceLength;
}

/**
 * <feature name="plagiarism" type="artificial" obfuscation="low"
 * this_language="en" this_offset="55879" this_length="3461"
 * source_reference="source-document10626.txt" source_language="en" source_offset="8681" source_length="3560" />
**/
