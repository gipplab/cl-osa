package com.iandadesign.closa.util;

import org.w3c.dom.*;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;

/**
 * Class for parsing the *xml info files in PAN-PC 11 dataset.
 *
 * @author Johannes Stegmüller (06.07.2020)
 */
public class PAN11XMLParser {

    public PAN11XMLInfo parseXMLfile(File file) {
        try {
            // Load
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);

            // Validate (which fails)
            /*
            Schema schema = null;
            try {
                String language = XMLConstants.W3C_XML_SCHEMA_NS_URI;
                SchemaFactory factoryV = SchemaFactory.newInstance(language);
                schema = factoryV.newSchema(file);
            } catch (Exception e) {
                System.err.println(e);
            }
            */
            //Validator validator = schema.newValidator();
            // validator.validate(new DOMSource(document));


            // Parse TODO probably can be made more elegant
            NamedNodeMap overallDescription  = document.getFirstChild().getFirstChild().getNextSibling().getAttributes();
            String langValue = overallDescription.getNamedItem("lang").getNodeValue();
            return new PAN11XMLInfo(langValue);

        } catch (Exception ex) {
            System.err.println(ex.toString());
            return null;
        }
    }
}


/**
 *<?xml version="1.0" encoding="UTF-8"?>
 * <document reference="suspicious-document01003.txt">
 *   <feature name="about" authors="Byrne, Desmond" title="Australian Writers" lang="en" />
 *   <feature name="md5Hash" value="8cece2f85000118f132f1cb1e8bdb705" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="55879" this_length="3461" source_reference="source-document10626.txt" source_language="en" source_offset="8681" source_length="3560" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="133469" this_length="3478" source_reference="source-document06163.txt" source_language="en" source_offset="4290" source_length="3440" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="157647" this_length="3825" source_reference="source-document08827.txt" source_language="en" source_offset="1290" source_length="3903" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="263644" this_length="2308" source_reference="source-document07288.txt" source_language="en" source_offset="32997" source_length="2349" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="274804" this_length="250" source_reference="source-document06163.txt" source_language="en" source_offset="2671" source_length="256" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="280237" this_length="560" source_reference="source-document10626.txt" source_language="en" source_offset="7329" source_length="545" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="288905" this_length="762" source_reference="source-document06163.txt" source_language="en" source_offset="2970" source_length="753" />
 *   <feature name="plagiarism" type="artificial" obfuscation="low" this_language="en" this_offset="294024" this_length="281" source_reference="source-document08827.txt" source_language="en" source_offset="159" source_length="274" />
 * </document>
 *
 *
 *
 *
 **/