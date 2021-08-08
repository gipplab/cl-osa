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
import java.nio.file.Files;
import java.nio.file.Paths;

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


            PAN11XMLInfo pan11XMLInfo =  new PAN11XMLInfo();
            NodeList mainNode = document.getChildNodes();
            NodeList allNodes = mainNode.item(0).getChildNodes();
            if(mainNode.getLength()>1){
                System.out.println("check whats going on here");
            }
            for(int i=0; i < allNodes.getLength(); i++){
                Node currentNode = allNodes.item(i);
                NamedNodeMap attributes = currentNode.getAttributes();
                if(attributes==null){
                    continue;
                }
                String name = attributes.getNamedItem("name").getNodeValue();
                if(name.equals("about")) {
                    pan11XMLInfo.setLanguage(attributes.getNamedItem("lang").getNodeValue());
                }else if(name.equals("md5Hash")) {
                    continue;
                }else if(name.equals("plagiarism")) {
                    PAN11PlagiarismInfo plagInfo = new PAN11PlagiarismInfo();
                    plagInfo.type = attributes.getNamedItem("type").getNodeValue();
                    /*
                    if(!plagInfo.type.equals("translation") && !plagInfo.type.equals("artificial")
                            && !plagInfo.type.equals("simulated")){
                        System.out.println("asd");
                    }
                    */

                    if(plagInfo.type.equals("translation")){
                        plagInfo.translation = true;
                    }
                    Node obfItem = attributes.getNamedItem("obfuscation");
                    if(obfItem!=null){
                        // Obfuscation doesn't always exist (in type='simulated' it apparently doesn't)
                        plagInfo.obfuscation = attributes.getNamedItem("obfuscation").getNodeValue();
                    }
                    Node manObfItem = attributes.getNamedItem("manual_obfuscation");
                    if(manObfItem!=null){
                        // Usually for 'translation' type plagiarism
                        plagInfo.manualObfuscation = Boolean.valueOf(manObfItem.getNodeValue());  // Boolean.valueOf
                    }
                    /*
                    Node manObfAutItem = attributes.getNamedItem("automated_obfuscation");
                    if(manObfAutItem!=null){
                        // Usually for 'translation' type plagiarism
                        plagInfo.automatedObfuscation = Boolean.valueOf(manObfAutItem.getNodeValue());  // Boolean.valueOf
                    }
                    */

                    plagInfo.thisLanguage = attributes.getNamedItem("this_language").getNodeValue();
                    plagInfo.thisLength = Integer.parseInt(attributes.getNamedItem("this_length").getNodeValue());
                    plagInfo.thisOffset = Integer.parseInt(attributes.getNamedItem("this_offset").getNodeValue());

                    Node sourceLanguage = attributes.getNamedItem("source_language");
                    if(sourceLanguage!=null) {
                        plagInfo.sourceLanguage = attributes.getNamedItem("source_language").getNodeValue();
                        plagInfo.sourceLength = Integer.parseInt((attributes.getNamedItem("source_length").getNodeValue()));
                        plagInfo.sourceOffset = Integer.parseInt(((attributes.getNamedItem("source_offset").getNodeValue())));
                        plagInfo.sourceReference = attributes.getNamedItem("source_reference").getNodeValue();
                    }
                    plagInfo.calculateCaseLength();
                    pan11XMLInfo.addPlagiarismInfo(plagInfo);
                } else {
                    System.out.println("strange case");
                }
            }


            //String myFileString = file.toString();
            /* Comment in for verifying numbers
            String myFileString =  new String ( Files.readAllBytes(Paths.get(file.getPath())));
            int occurences = countOccurences(myFileString,"plagiarism");
            if(pan11XMLInfo.plagiarismInfos.size()!=occurences){
                System.out.println("asd");
            }
            */
            return pan11XMLInfo;

        } catch (Exception ex) {
            System.err.println(ex.toString());
            return null;
        }
    }

    int countOccurences(String data, String searchWord){
        int count = 0;
        while (data.indexOf(searchWord)>-1){
            data = data.replaceFirst(searchWord, "");
            count++;
        }
        return count ;
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