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
    public final String fileName;
    public final Map<String, List<String>> filenameToEntities;
    public final int characterStartIndex;
    public final int characterEndIndex;

    public SlidingWindowInfo(String fileName,
                             Map <String, List<String>> filenameToEntities,
                             int characterStartIndex,
                             int characterEndIndex) {

        this.fileName = fileName;
        this.filenameToEntities = filenameToEntities;
        this.characterStartIndex = characterStartIndex;
        this.characterEndIndex = characterEndIndex;
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

}