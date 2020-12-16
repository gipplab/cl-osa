package com.iandadesign.closa.util;

import com.iandadesign.closa.model.ResultInfo;
import com.iandadesign.closa.model.SalvadorTextFragment;
import com.iandadesign.closa.model.StartStopInfo;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.Map;

public class SalvadorPAN11XMLwriter {

    public static String writeDownAllXMLResults(String tag, String dateString,
                                             String preprocessedCachingDirectory,
                                             Map<String, Map<String, Map<SalvadorTextFragment, SalvadorTextFragment>>> allResults){
        String xmlResultsFolderPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended",
                "results_comparison_salvador", tag.concat("_").concat(dateString)).toAbsolutePath().toString();
        // Do writing for each corresponding
        for(String suspiciousDocument:allResults.keySet()){
            for(String candidateDocument:allResults.get(suspiciousDocument).keySet()){
                String cosineResultsPath = Paths.get(xmlResultsFolderPath,
                        suspiciousDocument.replace(".txt",""),
                        candidateDocument.replace("candidate-","source-").replace(".txt","").concat(".xml"))
                        .toAbsolutePath().toString();
                // Writing the results to xml file
                try {
                    Map<SalvadorTextFragment, SalvadorTextFragment> currentCases = allResults.get(suspiciousDocument).get(candidateDocument);

                    System.out.println("RESULTS writing result to: "+cosineResultsPath+ " Results length: "+ currentCases.size());
                    writeResultAsXML(cosineResultsPath,suspiciousDocument, candidateDocument, currentCases);
                    prettifyXML(cosineResultsPath);
                } catch(Exception ex){
                    System.out.println("Exception during writing results: "+ex.toString());
                    ex.printStackTrace();
                }
            }



        }

        return xmlResultsFolderPath;
    }


    public static void writeResultAsXML(String resultFilePath, String suspiciousDocument, String candidateDocument, Map<SalvadorTextFragment, SalvadorTextFragment> plagiarismCases) throws Exception {

        // Creating output directory if it doesn't exist
        boolean dirCreated = new File(resultFilePath).getParentFile().mkdirs();

        // Creating an XMLOutputFactory.
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        // Creating XMLEventWriter.
        FileOutputStream fos = new FileOutputStream(resultFilePath);
        XMLEventWriter eventWriter = outputFactory
                .createXMLEventWriter(fos);
        // Creating an EventFactory.
        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent end = eventFactory.createDTD("\n");
        // Creating and writing Start Tag.
        StartDocument startDocument = eventFactory.createStartDocument();
        eventWriter.add(startDocument);

        // Writing the xml input to file.
        String candidateDocumentName = candidateDocument.replace("candidate-","source-");
        createDocumentNode(eventWriter, suspiciousDocument, candidateDocumentName, plagiarismCases); //this.suspiciousDocumentName);

        eventWriter.close();
        fos.close();
    }

    public static void prettifyXML(String path){
        try {
            File xmlFile = new File(path);
            XmlFormatter formatter = new XmlFormatter(45,2);
            String formatted =  formatter.format(xmlFile);
            FileWriter fw = new FileWriter(xmlFile);
            fw.write(formatted);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createDocumentNode(XMLEventWriter eventWriter, String suspiciousDocumentName, String candidateDocumentName, Map<SalvadorTextFragment, SalvadorTextFragment> plagiarismCases)
            throws XMLStreamException {

        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent end = eventFactory.createDTD("\n");
        XMLEvent tab = eventFactory.createDTD("\t");
        eventWriter.add(end);
        // Creating Start node.
        XMLEvent event = eventFactory.createStartElement("", "", "document");
        eventWriter.add(event);

        event = eventFactory.createAttribute
                ("reference", suspiciousDocumentName);
        eventWriter.add(event);
        // Adding all results to file.
        for (SalvadorTextFragment suspiciousResultFragment : plagiarismCases.keySet()) {
            SalvadorTextFragment candidateResultFragment = plagiarismCases.get(suspiciousResultFragment);
            createPlagiarismNode(eventWriter, candidateDocumentName,
                    candidateResultFragment.getSentencesStartChar(), candidateResultFragment.getCharLengthBySentences(),
                    suspiciousResultFragment.getSentencesStartChar(), suspiciousResultFragment.getCharLengthBySentences());
        }

        eventWriter.add(end);
        event = eventFactory.createEndElement("", "", "document");
        eventWriter.add(event);

    }

    public static void createPlagiarismNode(XMLEventWriter eventWriter, String candidateDocumentName,
                                      int candidateOffset, int candidateLength,
                                      int sourceOffset, int sourceLength) throws XMLStreamException{

        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent end = eventFactory.createDTD("\n");
        XMLEvent tab = eventFactory.createDTD("\t");
        eventWriter.add(end);
        // create Start node
        XMLEvent event = eventFactory.createStartElement("", "", "feature");
        eventWriter.add(event);
        // For some reason attributes are printed in alphabetical order. Temporarily fixed with prefixes in localName.
        /* workaround for ordering stuffs
        event = eventFactory.createAttribute("aa_name", "detected-plagiarism");
        eventWriter.add(event);
        // event = eventFactory.createCharacters("\n");
        // eventWriter.add(event);
        event = eventFactory.createAttribute("ab_this_offset", Integer.toString(sourceOffset));
        eventWriter.add(event);
        event = eventFactory.createAttribute("ac_this_length", Integer.toString(sourceLength));
        eventWriter.add(event);
        event = eventFactory.createAttribute("ba_source_reference", candidateDocumentName);
        eventWriter.add(event);
        event = eventFactory.createAttribute("bb_source_offset", Integer.toString(candidateOffset));
        eventWriter.add(event);
        event = eventFactory.createAttribute("bc_source_length", Integer.toString(candidateLength));
        eventWriter.add(event);
        event = eventFactory.createEndElement("","","feature");
        eventWriter.add(event);
        */
        event = eventFactory.createAttribute("name", "detected-plagiarism");
        eventWriter.add(event);
        // event = eventFactory.createCharacters("\n");
        // eventWriter.add(event);
        event = eventFactory.createAttribute("this_offset", Integer.toString(sourceOffset));
        eventWriter.add(event);
        event = eventFactory.createAttribute("this_length", Integer.toString(sourceLength));
        eventWriter.add(event);
        event = eventFactory.createAttribute("source_reference", candidateDocumentName);
        eventWriter.add(event);
        event = eventFactory.createAttribute("source_offset", Integer.toString(candidateOffset));
        eventWriter.add(event);
        event = eventFactory.createAttribute("source_length", Integer.toString(candidateLength));
        eventWriter.add(event);
        event = eventFactory.createEndElement("","","feature");
        eventWriter.add(event);

    }
}
