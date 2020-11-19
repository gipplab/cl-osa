package com.iandadesign.closa.model;

import java.io.File;

public class SalvadorTextFragment {

    String fragmentID;
    int entitiesStartChar;
    int entitiesEndChar;
    int sentencesStartChar;
    int sentencesEndChar;
    int charLengthBySentences;
    int charLengthByEntities;


    public String getFragmentID() {
        return fragmentID;
    }

    public void setFragmentID(String fragmentID) {
        this.fragmentID = fragmentID;
    }

    public int getEntitiesStartChar() {
        return entitiesStartChar;
    }

    public void setEntitiesStartChar(int entitiesStartCharIn) {
        this.entitiesStartChar = entitiesStartCharIn;
    }

    public int getEntitiesEndChar() {
        return entitiesEndChar;
    }

    public void setEntitiesEndChar(int entitiesEndChar) {
        this.entitiesEndChar = entitiesEndChar;
    }

    public int getSentencesStartChar() {
        return sentencesStartChar;
    }

    public void setSentencesStartChar(int sentencesStartChar) {
        this.sentencesStartChar = sentencesStartChar;
    }

    public int getSentencesEndChar() {
        return sentencesEndChar;
    }

    public void setSentencesEndChar(int sentencesEndChar) {
        this.sentencesEndChar = sentencesEndChar;
    }

    public int getCharLengthBySentences() {
        return charLengthBySentences;
    }

    public void setCharLengthBySentences(int charLengthBySentences) {
        this.charLengthBySentences = charLengthBySentences;
    }

    public int getCharLengthByEntities() {
        return charLengthByEntities;
    }

    public void setCharLengthByEntities(int charLengthByEntities) {
        this.charLengthByEntities = charLengthByEntities;
    }

}
