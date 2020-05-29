package com.iandadesign.closa.model;

import com.mongodb.client.model.geojson.CoordinateReferenceSystem;
import edu.stanford.nlp.util.ArrayMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.max;
import static java.lang.Integer.min;

/**
 * This is a class for storing and combining
 * sliding window scores.
 *
 * @author Johannes Stegmüller on 2020/05/29/
 */
public class ScoringChunksCombined {
    private final double adjacentTresh;
    private final double singleTresh;
    private final int slidingWindowLength;
    private final int slidingWindowIncrement;
    private final Map<MapCoords, List<ScoringChunk>> allScoringChunksCombined;

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
    }

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
}
