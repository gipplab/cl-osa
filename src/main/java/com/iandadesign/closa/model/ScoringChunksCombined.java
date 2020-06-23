package com.iandadesign.closa.model;

import com.iandadesign.closa.util.CSVUtil;
import com.iandadesign.closa.util.XmlFormatter;
import edu.stanford.nlp.util.ArrayMap;
import org.apache.http.annotation.Obsolete;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.*;

import java.io.*;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;


import static java.lang.Integer.*;

/**
 * This is a class for storing and combining
 * sliding window scores.
 *
 * @author Johannes Stegmüller on 2020/05/29/
 *
 * //TODO perfomance: remove obsolete functions and properties
 * //TODO perfomance: move functionalities to other utility classes
 */
public class ScoringChunksCombined {
    private final double adjacentTresh;
    private final double singleTresh;
    private final int slidingWindowLength;
    private final int slidingWindowIncrement;

    @Obsolete
    private Map<MapCoords, List<ScoringChunk>> allScoringChunksCombined; // This is flushed on every file combination

    private List<ScoringChunk> scoringChunksList = new ArrayList<>();
    private ScoringChunk[][] scoreMatrix; // This is flushed on every file combination
    private int matrixDimensionX;
    private int matrixDimensionY;
    // Clustering algorithm related properties.
    private int windowXsearchLength;
    private int windowYsearchLength;
    private int windowDiagsearchLength;

    private String suspiciousDocumentName;
    private String candidateDocumentName;
    private List<ResultInfo> clusteringResults;

    @Obsolete
    class MapCoords implements Cloneable{
        int candStartSentence;
        int candEndSentence;
        int suspStartSentence;
        int suspEndSentence;

        public MapCoords(int candStartSentence, int candEndSentence, int suspStartSentence, int suspEndSentence){
            this.candStartSentence = candStartSentence;
            this.candEndSentence = candEndSentence;
            this.suspStartSentence = suspStartSentence;
            this.suspEndSentence = suspEndSentence;
        }
        public boolean equals(Object o) {
            // An object is equal if it has the same end-sentence coordinates.
            MapCoords cordsIn = (MapCoords) o;
            return cordsIn.candEndSentence == candEndSentence && cordsIn.suspEndSentence == suspEndSentence;
        }
        public MapCoords getAssumedPreviousCoords() {
            MapCoords assumedPrevious = new MapCoords(candStartSentence, candEndSentence, suspStartSentence, suspEndSentence);
            //TODO adapt this for the case of an overlapping sliding window.
            // just decrement the found coordinates to get a possible previous object
            assumedPrevious.suspEndSentence--;
            assumedPrevious.candEndSentence--;
            return assumedPrevious;
        }
    }
    public ScoringChunksCombined(double adjacentTresh, double singleTresh, int slidingWindowLength, int slidingWindowIncrement){
        this.adjacentTresh = adjacentTresh;
        this.singleTresh = singleTresh;
        this.slidingWindowLength = slidingWindowLength;
        this.slidingWindowIncrement = slidingWindowIncrement;
        this.allScoringChunksCombined = new ArrayMap<MapCoords,List<ScoringChunk>>();
        this.calculateSearchLength();
    }
    private void calculateSearchLength(){
        // TODO adapt this for other cases (this is for swl 1 and swi 1)
        this.windowXsearchLength = 1; //this.slidingWindowLength-this.slidingWindowIncrement;
        this.windowYsearchLength = 1; //this.slidingWindowLength-this.slidingWindowIncrement;
        this.windowDiagsearchLength = 1; //this.slidingWindowLength-this.slidingWindowIncrement;
    }
    public void setCurrentDocuments(String suspiciousDocumentPath, String candidateDocumentPath){
        // Internally just use document names instead of paths.
        this.suspiciousDocumentName = new File(suspiciousDocumentPath).getName();
        this.candidateDocumentName = new File(candidateDocumentPath).getName();
    }
    public String getSuspiciousDocumentName(){
        return this.suspiciousDocumentName;
    }
    public String getCandidateDocumentName(){
        return this.candidateDocumentName;
    }

    public void createScoreMatrix(Integer suspiciousDocumentSentences, Integer candidateDocumentSentences){
        this.matrixDimensionX = (int) Math.floor(suspiciousDocumentSentences / (float) this.slidingWindowIncrement);
        this.matrixDimensionY = (int) Math.floor(candidateDocumentSentences / (float) this.slidingWindowIncrement);
        //List<List<ScoringChunk>> scoreMatrix = new ArrayList<>(dimensionX);
        this.scoreMatrix = new ScoringChunk[this.matrixDimensionX][this.matrixDimensionY];
    }

    @Obsolete
    public boolean storeAndAddToPreviousChunk(ScoringChunk chunkToStore){
        MapCoords chunkStoreCoords = getMapCoordsOfChunk(chunkToStore);
        MapCoords assumedPreviousCoords = chunkStoreCoords.getAssumedPreviousCoords();

        List<ScoringChunk> previousScoringChunks = this.allScoringChunksCombined.get(chunkStoreCoords);

        // Searching the previous coordinate and create a merged new coordinate
        MapCoords mergedMapCoord = null;
        // TODO make more efficient iteration here
        for(MapCoords keyCurrent: this.allScoringChunksCombined.keySet()){
            if(keyCurrent.equals(assumedPreviousCoords)){
                mergedMapCoord = mergeMapCoords(keyCurrent, chunkStoreCoords);
                break;
            }
        }
        if(mergedMapCoord == null){
            return false;
        }
        // Getting the Array list of scoring chunks and add the entry
        List<ScoringChunk> scoringChunks = this.allScoringChunksCombined.get(assumedPreviousCoords);
        scoringChunks.add(chunkToStore);
        // Deleting the old entry TODO maybe there is a more efficient method like update here
        this.allScoringChunksCombined.remove(assumedPreviousCoords);
        // Put the updated array with new coordinates
        this.allScoringChunksCombined.put(mergedMapCoord, scoringChunks);
        return true;
    }

    public MapCoords mergeMapCoords(MapCoords coordPrev, MapCoords coordNew) {
        int newSuspStartSentence = min(coordPrev.suspStartSentence, coordNew.suspStartSentence);
        int newSuspEndSentence = max(coordPrev.suspEndSentence, coordNew.suspEndSentence);
        int newCandStartSentence = min(coordPrev.candStartSentence, coordNew.candStartSentence);
        int newCandEndSentence = max(coordPrev.candEndSentence, coordNew.candEndSentence);
        return new MapCoords(newCandStartSentence, newCandEndSentence, newSuspStartSentence, newSuspEndSentence);
    }
    @Obsolete
    public void storeScoringChunkToScoringMatrix(ScoringChunk chunkToStore,
                                                 int suspiciousSlidingWindowIndex,
                                                 int candidateSlidingWindowIndex){
        // Does this duplicate object or ref ?
        chunkToStore.setCandidateMatrixIndex(candidateSlidingWindowIndex);
        chunkToStore.setSuspiciousMatrixIndex(suspiciousSlidingWindowIndex);
        this.scoreMatrix[suspiciousSlidingWindowIndex][candidateSlidingWindowIndex] = chunkToStore;
        this.scoringChunksList.add(chunkToStore);

    }
    @Obsolete
    public void storeScoringChunk(ScoringChunk chunkToStore){
        MapCoords chunkStoreCoords = getMapCoordsOfChunk(chunkToStore);
        List<ScoringChunk> scoringChunks = this.allScoringChunksCombined.get(chunkStoreCoords);
        if(scoringChunks == null){
            List<ScoringChunk>  scoringChunksToPut = new ArrayList<>();
            scoringChunksToPut.add(chunkToStore);
            this.allScoringChunksCombined.put(chunkStoreCoords, scoringChunksToPut);
        }else{
            // TODO does this work?
            scoringChunks.add(chunkToStore);
        }
    }
    @Obsolete
    private MapCoords getMapCoordsOfChunk(ScoringChunk chunk){
        int candStartSentenceIndex = chunk.getCandidateWindow().getStartSentence();
        int suspStartSentenceIndex = chunk.getSuspiciousWindow().getStartSentence();
        int candEndSentenceIndex = chunk.getCandidateWindow().getEndSentence();
        int suspEndSentenceIndex = chunk.getSuspiciousWindow().getEndSentence();

        MapCoords coords = new MapCoords(candStartSentenceIndex,
                candEndSentenceIndex,
                suspStartSentenceIndex,
                suspEndSentenceIndex);
        return coords;
    }

    public void calculateMatrixClusters(){
        List<ResultInfo> clusteringResults = new ArrayList<>();
        // Sort the list to get the highest score chunks.
        this.scoringChunksList.sort(Comparator.comparing(ScoringChunk::getComputedCosineSimilarity).reversed());
        for(ScoringChunk currentScoringChunk:this.scoringChunksList) {
            if(!currentScoringChunk.isProcessedByClusteringAlgo()
               && currentScoringChunk.getComputedCosineSimilarity() >= this.singleTresh){
                List<ScoringChunk> clusterChunks = new ArrayList<>();
                currentScoringChunk.setProcessedByClusteringAlgo(true);
                clusterChunks.add(currentScoringChunk);
                clusterChunks = processMatrixHV(currentScoringChunk, clusterChunks);
                clusteringResults.add(getClusterEdgeCoordinates(clusterChunks));
            }
        }
        this.clusteringResults = clusteringResults;
    }
    public ResultInfo getClusterEdgeCoordinates(List<ScoringChunk> clusterChunks){
        // Get the relevant edge indices in one iteration
        int suspStartChar = MAX_VALUE;
        int suspEndChar = -1;
        int candStartChar = MAX_VALUE;
        int candEndChar = -1;
        for(ScoringChunk clusterChunk:clusterChunks){

            int cCandStart = clusterChunk.getCandidateWindow().getCharacterStartIndex();
            if(cCandStart<candStartChar){
                candStartChar = cCandStart;
            }
            int cSuspStart = clusterChunk.getSuspiciousWindow().getCharacterStartIndex();
            if(cSuspStart<suspStartChar){
                suspStartChar = cSuspStart;
            }
            int cCandEnd = clusterChunk.getCandidateWindow().getCharacterEndIndex();
            if(cCandEnd>candEndChar){
                candEndChar = cCandEnd;
            }
            int cSuspEnd = clusterChunk.getSuspiciousWindow().getCharacterEndIndex();
            if(cSuspEnd>suspEndChar){
                suspEndChar = cSuspEnd;
            }
        }
        return new ResultInfo(candStartChar, candEndChar, suspStartChar, suspEndChar);
    }
    public List<ScoringChunk> processMatrixHV(ScoringChunk currentHVScoringChunk, List<ScoringChunk> clusterChunks){
        // Get the adjacent neighbors
        int yIndex = currentHVScoringChunk.getSuspiciousMatrixIndex();
        int xIndex = currentHVScoringChunk.getCandidateMatrixIndex();
        // Get Adjacent chunks
        List<ScoringChunk> adjacentChunks = getAdjacentChunks(yIndex, xIndex);
        for(ScoringChunk currentAdjacentChunk:adjacentChunks){
            if(currentAdjacentChunk!= null
                && currentAdjacentChunk.getComputedCosineSimilarity() >= this.adjacentTresh
                && !currentAdjacentChunk.isProcessedByClusteringAlgo()){

                currentAdjacentChunk.setProcessedByClusteringAlgo(true);
                clusterChunks.add(currentAdjacentChunk);
                clusterChunks = processMatrixHV(currentAdjacentChunk, clusterChunks);
            }
        }
        return clusterChunks;
    }

    private List<ScoringChunk> getAdjacentChunks(final int yIndex, final int xIndex){
        List<ScoringChunk> adjacentChunks = new ArrayList<>();
        // For simplicities sake just use a square matrix with width==windowXlength and height==windowYlength
        // TODO adapt this if necessary with the windowDiagLength
        for(int yIndexP=yIndex-this.windowYsearchLength; yIndexP<=(yIndex+this.windowYsearchLength); yIndexP++){
            // Get all horizontally adjacent chunks
            for(int xIndexP=xIndex-this.windowXsearchLength; xIndexP<=(xIndex+this.windowXsearchLength); xIndexP++){
                if(xIndexP==xIndex && yIndexP==yIndex){
                    continue;
                }
                adjacentChunks.add(getChunkOrNull(yIndexP, xIndexP));
            }
        }

        return adjacentChunks;
    }
    private ScoringChunk getChunkOrNull(int yIndex, int xIndex){
        if(yIndex < 0 || xIndex < 0){
            return null;
        }else if(yIndex > this.scoreMatrix.length){
            return null;
        }else if(xIndex > this.scoreMatrix[yIndex].length){
            return null;
        }
        return this.scoreMatrix[yIndex][xIndex];
    }

    public void flushInternalCombinedChunks(){
        this.allScoringChunksCombined = new ArrayMap<>(); //Obsolete
        this.scoringChunksList = new ArrayList<>();
        this.scoreMatrix = null;
        this.clusteringResults = new ArrayList<>();
    }

    public void writeDownXMLResults(String tag, String dateString, String preprocessedCachingDirectory){
        String cosineResultsPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended",
                "results_comparison", tag.concat("_").concat(dateString),
                this.getSuspiciousDocumentName().replace(".txt",""),
                this.getCandidateDocumentName().concat(".xml"))
                .toAbsolutePath().toString();
        // Writing the results to xml file
        try {
            this.writeResultAsXML(cosineResultsPath);
            this.prettifyXML(cosineResultsPath);
        } catch(Exception ex){
            ex.printStackTrace();
        }
    }

    public void writeScoresMapAsCSV(String tag, String dateString, String preprocessedCachingDirectory){
        String resultPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended",
                "scores_maps", tag.concat("_").concat(dateString),
                this.getSuspiciousDocumentName().replace(".txt",""),
                this.getCandidateDocumentName().concat(".csv")).toString();


        // Creating output directory if it doesn't exist
        boolean dirCreated = new File(resultPath).getParentFile().mkdirs();

        try {
            File csvOutputFile = new File(resultPath.toString());
            try (PrintWriter pw = new PrintWriter(csvOutputFile)) {

                // Create Headline on X-Axis:
                String[] firstRow = new String[this.matrixDimensionY+1];
                int counterX = -1;
                for(int index=0;index<firstRow.length;index++){
                    firstRow[index] = "X"+Integer.toString(counterX);
                    counterX++;
                }
                firstRow[0] = "SuspSentenceNumber\\CandSentenceNumber";
                pw.println(CSVUtil.convertToCSV(firstRow));



                int counterY = 0;
                for (ScoringChunk[] items : this.scoreMatrix) {
                    try {
                        if (items == null) {
                            // If a row doesn't have values, just fill with empty strings
                            String[] emptyStrings = new String[this.matrixDimensionX + 1];
                            Arrays.fill(emptyStrings, "");
                            emptyStrings[0] = "Y" + Integer.toString(counterY);
                            pw.println(CSVUtil.convertToCSV(emptyStrings));
                            counterY++;
                            continue;
                        }
                        List<String> values = new ArrayList<>();
                        values.add("Y" + Integer.toString(counterY));
                        for (ScoringChunk item : items) {
                            if (item == null) {
                                values.add("");
                            } else {
                                DecimalFormat df = new DecimalFormat("#.####");
                                String formatted = df.format(item.getComputedCosineSimilarity());
                                values.add(formatted);
                            }
                        }
                    /*
                    Double[] values = (Double[]) Arrays.stream(items).map(ScoringChunk::getComputedCosineSimilarity).toArray();
                    String[] strings = (String[]) Arrays.stream(values).map(String::valueOf).toArray();

                    String[] strings2 = Arrays.stream(items)
                            .map(ScoringChunk::getComputedCosineSimilarity)
                            .map(String::valueOf)
                            .toArray(String[]::new);
                    */

                        String returnVal = CSVUtil.convertToCSV(values.toArray(new String[0]));
                        pw.println(returnVal);
                        counterY++;
                    }catch(NullPointerException nex){
                        System.out.println("Exception during writing data to csv:"+ nex);

                    }
                }
            }
        } catch(Exception ex){
            System.out.println("Exception during writing data to csv:"+ ex);
        }
    }

    public void writeResultAsXML(String resultFilePath) throws Exception {

        // Creating output directory if it doesn't exist
        boolean dirCreated = new File(resultFilePath).getParentFile().mkdirs();

        // Creating an XMLOutputFactory.
        XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
        // Creating XMLEventWriter.
        XMLEventWriter eventWriter = outputFactory
                .createXMLEventWriter(new FileOutputStream(resultFilePath));
        // Creating an EventFactory.
        XMLEventFactory eventFactory = XMLEventFactory.newInstance();
        XMLEvent end = eventFactory.createDTD("\n");
        // Creating and writing Start Tag.
        StartDocument startDocument = eventFactory.createStartDocument();
        eventWriter.add(startDocument);

        // Writing the xml input to file.
        createDocumentNode(eventWriter, this.suspiciousDocumentName);

        eventWriter.close();
    }

    public void prettifyXML(String path){
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

    private void createDocumentNode(XMLEventWriter eventWriter, String suspiciousDocumentName)
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
        for (ResultInfo plagiarismCluster : this.clusteringResults) {
            int suspiciousLength = plagiarismCluster.getSuspEndCharIndex() - plagiarismCluster.getSuspStartCharIndex();
            int candidateLength =  plagiarismCluster.getCandEndCharIndex() - plagiarismCluster.getCandStartCharIndex();
            createPlagiarismNode(eventWriter, candidateDocumentName,
                                plagiarismCluster.getCandStartCharIndex(), candidateLength,
                                plagiarismCluster.getSuspStartCharIndex(), suspiciousLength);
        }

        eventWriter.add(end);
        event = eventFactory.createEndElement("", "", "document");
        eventWriter.add(event);

    }
    private void createPlagiarismNode(XMLEventWriter eventWriter, String candidateDocumentName,
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
    }


    /**
     * <document reference="suspicious-documentXYZ.txt">
     *   <feature name="detected-plagiarism"
     *            this_offset="5"
     *            this_length="1000"
     *            source_reference="source-documentABC.txt"
     *            source_offset="100"
     *            source_length="1000"
     *   />
     *   ...
     * </document>
     */
}
