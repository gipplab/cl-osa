package com.iandadesign.closa.model;

import com.iandadesign.closa.util.CSVUtil;
import com.iandadesign.closa.util.ExtendedAnalytics;
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
import java.util.stream.Collectors;


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
    private final int clippingMarging;


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

    public ScoringChunksCombined(double adjacentTresh, double singleTresh, int slidingWindowLength, int slidingWindowIncrement, int clippingMargin){
        this.adjacentTresh = adjacentTresh;
        this.singleTresh = singleTresh;
        this.slidingWindowLength = slidingWindowLength;
        this.slidingWindowIncrement = slidingWindowIncrement;
        this.clippingMarging = clippingMargin;
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

    public ScoringChunk[][] getScoreMatrix() {
        return scoreMatrix;
    }

    public void createScoreMatrix(Integer suspiciousDocumentSentences, Integer candidateDocumentSentences){
        this.matrixDimensionX = (int) Math.floor(suspiciousDocumentSentences / (float) this.slidingWindowIncrement);
        this.matrixDimensionY = (int) Math.floor(candidateDocumentSentences / (float) this.slidingWindowIncrement);
        //List<List<ScoringChunk>> scoreMatrix = new ArrayList<>(dimensionX);
        this.scoreMatrix = new ScoringChunk[this.matrixDimensionX][this.matrixDimensionY];
    }

    public void storeScoringChunkToScoringMatrix(ScoringChunk chunkToStore,
                                                 int suspiciousSlidingWindowIndex,
                                                 int candidateSlidingWindowIndex){
        // Does this duplicate object or ref ?
        chunkToStore.setCandidateMatrixIndex(candidateSlidingWindowIndex);
        chunkToStore.setSuspiciousMatrixIndex(suspiciousSlidingWindowIndex);
        this.scoreMatrix[suspiciousSlidingWindowIndex][candidateSlidingWindowIndex] = chunkToStore;
        this.scoringChunksList.add(chunkToStore);

    }


    public void calculateMatrixClusters(boolean useAdaptiveThresh, double adaptiveFormFactor){

        double usedSingleThresh;
        if(!useAdaptiveThresh){
            // default mode, just use the single Thresh set
            usedSingleThresh = this.singleTresh;
        }else{
            double medianValue = ExtendedAnalytics.calculateMedian(scoreMatrix, null, null );
            usedSingleThresh = adaptiveFormFactor * medianValue;
            if(usedSingleThresh<=this.singleTresh){
                // In this mode single thresh is the absolute minimum thresh
                usedSingleThresh = this.singleTresh;
            }
            System.out.println("Used Single Thresh is: " + usedSingleThresh);
        }

        List<ResultInfo> clusteringResults = new ArrayList<>();
        // Sort the list to get the highest score chunks.
        this.scoringChunksList.sort(Comparator.comparing(ScoringChunk::getComputedCosineSimilarity).reversed());
        for(ScoringChunk currentScoringChunk:this.scoringChunksList) {
            if(!currentScoringChunk.isProcessedByClusteringAlgo()
               && currentScoringChunk.getComputedCosineSimilarity() >= usedSingleThresh){
                List<ScoringChunk> clusterChunks = new ArrayList<>();
                currentScoringChunk.setProcessedByClusteringAlgo(true);
                clusterChunks.add(currentScoringChunk);
                clusterChunks = processMatrixHV(currentScoringChunk, clusterChunks);
                clusteringResults.add(getClusterEdgeCoordinates(clusterChunks));
            }
        }

        // Combine results which cover the same areas (or are within the same areas)
        this.clusteringResults = combineClusteringResults(clusteringResults);
    }

    public List<ResultInfo> combineClusteringResults(List<ResultInfo> uncombinedClusteringResults){
        // Sort after area size
        uncombinedClusteringResults.sort(Comparator.comparing(ResultInfo::getSize).reversed());
        int markerIndex = 0;

        for(ResultInfo resultInfo:uncombinedClusteringResults){
            if(!resultInfo.wasDiscardedByCombinationAlgo()){
                // Search for other chunks which fit in area
                markerIndex = markOtherResultsInResultArea(resultInfo, uncombinedClusteringResults, markerIndex);
            }
        }
        // TODO not used for testing
        /*
        List<ResultInfo> combinedResultInfo = uncombinedClusteringResults
                                .stream()
                                .filter(res -> !res.wasDiscardedByCombinationAlgo())
                                .collect(Collectors.toList());
        */
        //System.out.println("COMBINE_RESULTS: Number before combination: " + uncombinedClusteringResults.size()+" after: "+combinedResultInfo.size());


        // TODO combine clipping results here
        List<ResultInfo> finalResultInfos = uncombinedClusteringResults;


        // Iterate through all combination markers
        for(int index=0;index<markerIndex;index++){
            List<ResultInfo> itemsWithTheMarker = getItemsWithSameMarker(index, finalResultInfos);
            if(itemsWithTheMarker.size()!=2){
                // System.out.println("Combination error this should not happen");
                continue;
            }
            // Combine (which removes the marker from combined item, but not further ones)
            ResultInfo mergedResults = combineResultInfo(itemsWithTheMarker.get(0), itemsWithTheMarker.get(1), index);
            // Add the combined item to finalResultInfos
            finalResultInfos.add(mergedResults);
            // Remove the items which are the parts of combined item from finalResultInfos
            finalResultInfos.remove(itemsWithTheMarker.get(0));
            finalResultInfos.remove(itemsWithTheMarker.get(1));
        }


        return finalResultInfos;
    }
    public ResultInfo combineResultInfo(ResultInfo resultInfo1, ResultInfo resultInfo2, int markerForCombo){
        int candStartCharIndex = min(resultInfo1.getCandStartCharIndex(), resultInfo2.getCandStartCharIndex());
        int candEndCharIndex = max(resultInfo1.getCandEndCharIndex(), resultInfo2.getCandEndCharIndex());
        int suspStartCharIndex = min(resultInfo1.getSuspStartCharIndex(), resultInfo2.getSuspStartCharIndex());
        int suspEndCharIndex = max(resultInfo1.getSuspEndCharIndex(),resultInfo2.getSuspEndCharIndex());

        // Add combination markers except the processed one, prevent combination marker redundancy in combined item
        ResultInfo combinedResult = new ResultInfo(candStartCharIndex, candEndCharIndex, suspStartCharIndex, suspEndCharIndex);
        combinedResult.addCombinationMarkers(resultInfo1.getCombinationMarkers(), markerForCombo);
        combinedResult.addCombinationMarkers(resultInfo2.getCombinationMarkers(), markerForCombo);
        return combinedResult;
    }

    public List<ResultInfo> getItemsWithSameMarker(int marker, List<ResultInfo> myResults){
        List<ResultInfo> itemsWithMarkers = new ArrayList<>();
        for(ResultInfo resultInfo:myResults){
            if(resultInfo.getCombinationMarkers().size()>=1){
                if(resultInfo.getCombinationMarkers().contains(marker)){
                    itemsWithMarkers.add(resultInfo);
                }
            }
        }
        return itemsWithMarkers;
    }

    public int markOtherResultsInResultArea(ResultInfo currentResultInfo, List<ResultInfo> clusteringResults, int combinationMarkerIndex){
        for(ResultInfo comparisonResultInfo:clusteringResults){
            if(!comparisonResultInfo.wasDiscardedByCombinationAlgo()){
                if(comparisonResultInfo.equals(currentResultInfo)){
                    continue;   // Do not compare the same object with itself
                }
                // Check if result is in area of cluster
                boolean resultIsInArea = resultIsInAreaOfResult(currentResultInfo, comparisonResultInfo);
                // If it is in other area mark it, its not relevant for the final result
                if(resultIsInArea){
                    comparisonResultInfo.setDiscardedByCombinationAlgo(true);
                    continue;
                }
                // Check if result is clipping
                boolean resultIsClipping = resultIsClipping(currentResultInfo, comparisonResultInfo);
                if(resultIsClipping){
                    // Add a combination marker to both results
                    currentResultInfo.addCombinationMarker(combinationMarkerIndex);
                    comparisonResultInfo.addCombinationMarker(combinationMarkerIndex);
                    combinationMarkerIndex++;
                }
            }
        }
        return combinationMarkerIndex;
    }
    public boolean resultIsClipping(ResultInfo bigResultArea, ResultInfo smallResultArea){
        final int CLIPPING_MARGIN = this.clippingMarging;   // was 50 before as default
        boolean activateMargin = true;

        if(bigResultArea.indexInSuspArea(smallResultArea.getSuspStartCharIndex(), activateMargin, CLIPPING_MARGIN) ||
           bigResultArea.indexInSuspArea(smallResultArea.getSuspEndCharIndex(), activateMargin, CLIPPING_MARGIN)){
            if(bigResultArea.indexInCandArea(smallResultArea.getCandStartCharIndex(), activateMargin, CLIPPING_MARGIN)||
                bigResultArea.indexInCandArea(smallResultArea.getCandEndCharIndex(), activateMargin, CLIPPING_MARGIN)){
                return true;
            }
        }
        // This might be redundant
        if(smallResultArea.indexInSuspArea(bigResultArea.getSuspStartCharIndex(), activateMargin, CLIPPING_MARGIN) ||
                smallResultArea.indexInSuspArea(bigResultArea.getSuspEndCharIndex(), activateMargin, CLIPPING_MARGIN)){
            if(smallResultArea.indexInCandArea(bigResultArea.getCandStartCharIndex(), activateMargin, CLIPPING_MARGIN)||
                    smallResultArea.indexInCandArea(bigResultArea.getCandEndCharIndex(), activateMargin, CLIPPING_MARGIN)){
                return true;
            }
        }

        return false;
    }


    public boolean resultIsInAreaOfResult(ResultInfo bigResultArea, ResultInfo smallResultArea) {
        if (smallResultArea.getCandStartCharIndex() < bigResultArea.getCandStartCharIndex()) {
            return false;
        }
        if (smallResultArea.getCandEndCharIndex() > bigResultArea.getCandEndCharIndex()) {
            return false;
        }
        if (smallResultArea.getSuspStartCharIndex() < bigResultArea.getSuspStartCharIndex()) {
            return false;
        }
        if (smallResultArea.getSuspEndCharIndex() > bigResultArea.getSuspEndCharIndex()) {
            return false;
        }

        return true;
    }

    public ResultInfo getClusterEdgeCoordinates(List<ScoringChunk> clusterChunks){
        // Get the relevant edge indices in one iteration
        int suspStartChar = MAX_VALUE;
        int suspEndChar = -1;
        int candStartChar = MAX_VALUE;
        int candEndChar = -1;
        for(ScoringChunk clusterChunk:clusterChunks){

            int cCandStart = clusterChunk.getCandidateCharacterStartIndex();
            if(cCandStart<candStartChar){
                candStartChar = cCandStart;
            }
            int cSuspStart = clusterChunk.getSuspiciousCharacterStartIndex();
            if(cSuspStart<suspStartChar){
                suspStartChar = cSuspStart;
            }
            int cCandEnd = clusterChunk.getCandidateCharacterEndIndex();
            if(cCandEnd>candEndChar){
                candEndChar = cCandEnd;
            }
            int cSuspEnd = clusterChunk.getSuspiciousCharacterEndIndex();
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

    private boolean isADiscardedColumnOrRowItem(final int yIndex, final int xIndex){
        ScoringChunk relatedChunk = getChunkOrNull(yIndex, xIndex);
        if(relatedChunk==null){
            return false;
        }
        return relatedChunk.getComputedCosineSimilarity() == -1;
    }
    private int getAdjustedIndexVertical(int xIndex, int yIndex, int currentSearchIndex, boolean verticalOrhorizontal){
        boolean isDiscard1 = false;
        boolean isDiscard2  = false;
        if(verticalOrhorizontal){
             isDiscard1 = isADiscardedColumnOrRowItem(yIndex+currentSearchIndex,xIndex);
             isDiscard2 = isADiscardedColumnOrRowItem(yIndex-currentSearchIndex, xIndex);
        }else{
            isDiscard1 = isADiscardedColumnOrRowItem(yIndex,xIndex+currentSearchIndex);
            isDiscard2 = isADiscardedColumnOrRowItem(yIndex, xIndex-currentSearchIndex);

        }

        if(isDiscard1 || isDiscard2){
            currentSearchIndex = getAdjustedIndexVertical(xIndex,yIndex+currentSearchIndex,currentSearchIndex+1, verticalOrhorizontal);
        }
        return currentSearchIndex;
    }

    private List<ScoringChunk> getAdjacentChunks(final int yIndex, final int xIndex){
        int currentWindowYsearchLength = this.windowYsearchLength;
        int currentWindowXsearchLength = this.windowXsearchLength;


        List<ScoringChunk> adjacentChunks = new ArrayList<>();
        // Recursively check if there are columns or rows which are completely unpopulated (-1), create bigger window in this cases
        currentWindowYsearchLength = getAdjustedIndexVertical(xIndex, yIndex, currentWindowYsearchLength, true);
        currentWindowXsearchLength = getAdjustedIndexVertical(xIndex, yIndex, currentWindowYsearchLength, false);

        // For simplicities sake just use a square matrix with width==windowXlength and height==windowYlength
        // TODO adapt this if necessary with the windowDiagLength
        for(int yIndexP=yIndex-currentWindowYsearchLength; yIndexP<=(yIndex+currentWindowYsearchLength); yIndexP++){
            // Get all horizontally adjacent chunks
            for(int xIndexP=xIndex-currentWindowXsearchLength; xIndexP<=(xIndex+currentWindowXsearchLength); xIndexP++){
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
        }else if(yIndex >= this.scoreMatrix.length){
            return null;
        }else if(xIndex >= this.scoreMatrix[yIndex].length){
            return null;
        }
        return this.scoreMatrix[yIndex][xIndex];
    }

    public void flushInternalCombinedChunks(){
        //this.allScoringChunksCombined = new ArrayMap<>(); //Obsolete
        //this.scoringChunksList = new ArrayList<>();
        this.scoringChunksList.clear();

        //this.scoringChunksList.removeAll(this.scoringChunksList);
        //this.scoreMatrix = null;

        for (int row=0; row < this.scoreMatrix.length; row++)
        {
            if(this.scoreMatrix[row]==null){
                continue;
            }
            for (int col=0; col < this.scoreMatrix[row].length; col++)
            {
                if(this.scoreMatrix[row][col]==null){
                    continue;
                }
                this.scoreMatrix[row][col] = null;
            }
        }
        this.scoreMatrix = null;
        //this.clusteringResults = new ArrayList<>();
        if(this.clusteringResults !=null) {
            this.clusteringResults.clear();
        }
    }

    public String writeDownXMLResults(String tag, String dateString, String preprocessedCachingDirectory){
        String xmlResultsFolderPath = Paths.get(preprocessedCachingDirectory, "preprocessed_extended",
                "results_comparison", tag.concat("_").concat(dateString)).toAbsolutePath().toString();

        String cosineResultsPath = Paths.get(xmlResultsFolderPath,
                this.getSuspiciousDocumentName().replace(".txt",""),
                this.getCandidateDocumentName().concat(".xml"))
                .toAbsolutePath().toString();
        // Writing the results to xml file
        try {
            System.out.println("RESULTS writing result to: "+cosineResultsPath+ "Results length: "+this.clusteringResults.size());
            this.writeResultAsXML(cosineResultsPath);
            this.prettifyXML(cosineResultsPath);
        } catch(Exception ex){
            System.out.println("Exception during printiing results: "+ex.toString());
            ex.printStackTrace();
        }
        return xmlResultsFolderPath;
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
                                formatted+="||cs"+item.getCandidateCharacterStartIndex();
                                formatted+="||ss"+item.getSuspiciousCharacterStartIndex();
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

                    } finally {
                        pw.close();
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
        createDocumentNode(eventWriter, this.suspiciousDocumentName);

        eventWriter.close();
        fos.close();
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
