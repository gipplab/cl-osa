package com.iandadesign.closa.model;

import java.util.List;
import java.util.Map;

/**
 * This is a helper class for packing return values of
 * OntologyBasedSimilarityAnalysis.getWikiEntityStringsForSlidingWindow.
 *
 * @author Johannes Stegmüller on 2020/05/29/
 */
public class SlidingWindowInfo {
    private final String fileName;
    private final Map<String, List<String>> filenameToEntities;
    private final int characterStartIndex;
    private final int characterEndIndex;
    private final int startSentence;
    private final int endSentence;

    public SlidingWindowInfo(String fileName,
                             Map <String, List<String>> filenameToEntities,
                             int characterStartIndex,
                             int characterEndIndex,
                             int startSentence,
                             int endSentence) {

        this.fileName = fileName;
        this.filenameToEntities = filenameToEntities;
        this.characterStartIndex = characterStartIndex;
        this.characterEndIndex = characterEndIndex;
        this.startSentence = startSentence;
        this.endSentence = endSentence;
    }

    public String getFileName() {
        return fileName;
    }
    public Map<String, List<String>> getFilenameToEntities() {
        return filenameToEntities;
    }
    public int getCharacterStartIndex() {
        return characterStartIndex;
    }
    public int getCharacterEndIndex() {
        return characterEndIndex;
    }
    public int getStartSentence(){ return startSentence;}
    public int getEndSentence() { return endSentence; }
}