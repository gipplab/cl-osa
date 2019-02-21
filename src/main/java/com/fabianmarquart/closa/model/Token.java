package com.fabianmarquart.closa.model;

import edu.stanford.nlp.ling.HasWord;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A class representing a word token.
 * Extends Stanford's HasWord to be compatible with its POS-tagger libraries.
 *
 * Created by Fabian Marquart on 2016/12/12.
 */
public class Token implements HasWord {

    private String token;
    private int startCharacter;
    private int endCharacter;

    private int startCharacterCandidate;
    private int endCharacterCandidate;

    private int index;

    private String lemma;
    private String partOfSpeech;
    private boolean isNamedEntity;
    private NamedEntityType namedEntityType;

    public enum NamedEntityType {
        // core
        PERSON, LOCATION, ORGANIZATION, MISC,
        MONEY, NUMBER, ORDINAL, PERCENT,
        DATE, TIME, DURATION, SET,
        O,
        // chinese
        GPE, CITY, FACILITY, DEMONYM
    }

    /**
     * Initialize with token string and indices.
     * @param token token string
     * @param startCharacter index of start character in text
     * @param endCharacter index of end character in text
     * @param index index of token in token list.
     */
    public Token(String token, int startCharacter, int endCharacter, int index) {
        this.token = token;
        this.startCharacter = startCharacter;
        this.endCharacter = endCharacter;
        this.index = index;
    }

    /**
     * Initialize with two tokens that should be joined into one.
     * @param token1 first token
     * @param token2 second token
     */
    public Token(Token token1, Token token2, String separator) {
        this.token = token1.getToken() + separator + token2.getToken();
        this.startCharacter = token1.getStartCharacter();
        this.endCharacter = token2.getEndCharacter();
        this.index = token1.getIndex();

        this.lemma = token1.getLemma() + separator + token2.getLemma();
        this.partOfSpeech = token1.getPartOfSpeech();
        this.namedEntityType = token1.getNamedEntityType();
        this.isNamedEntity = token1.isNamedEntity();
    }

    /**
     * Initialize with many tokens that should be joined into one.
     * @param tokens    token list
     * @param separator the separator to join the tokens with
     */
    public Token(List<Token> tokens, String separator) {
        this.token = StringUtils.join(tokens.stream().map(Token::getToken).collect(Collectors.toList()), separator);
        this.lemma = StringUtils.join(tokens.stream().map(Token::getLemma).collect(Collectors.toList()), separator);
        this.partOfSpeech = tokens.get(0).getPartOfSpeech();
        this.namedEntityType = tokens.get(0).getNamedEntityType();
        this.isNamedEntity = tokens.get(0).isNamedEntity;
        this.startCharacter = tokens.get(0).getStartCharacter();
        this.endCharacter = tokens.get(tokens.size() - 1).getEndCharacter();
        this.index = tokens.get(0).getIndex();
    }

    /**
     * Simple initialization with token only.
     * @param token token string.
     */
    public Token(String token) {
        this.token = token;
    }

    /**
     * Initialization with token and lemma.
     * @param token token string.
     * @param lemma lemma string.
     */
    public Token(String token, String lemma) {
        this.token = token;
        this.lemma = lemma;
    }

    /**
     * For copying a token.
     * @param token the token.
     */
    public Token(Token token) {
        this.token = token.getToken();
        this.startCharacter = token.getStartCharacter();
        this.endCharacter = token.getEndCharacter();
        this.index = token.getIndex();
    }

    /**
     * Initalizes a token with part of speech tag and named entity type.
     * @param token token
     * @param lemma lemma
     * @param partOfSpeech part of speech
     * @param namedEntityType named entity type.
     */
    public Token(String token, String lemma, String partOfSpeech, NamedEntityType namedEntityType) {
        this.token = token;
        this.lemma = lemma;
        this.partOfSpeech = partOfSpeech;
        this.namedEntityType = namedEntityType;

        if (!namedEntityType.equals(NamedEntityType.O)) {
            isNamedEntity = true;
        }
    }

    public String getLemma() {
        return lemma;
    }

    public void setLemma(String lemma) {
        this.lemma = lemma;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public boolean isNamedEntity() {
        return isNamedEntity;
    }

    public void setNamedEntity(boolean namedEntity) {
        isNamedEntity = namedEntity;
    }

    public NamedEntityType getNamedEntityType() {
        return namedEntityType;
    }

    public boolean isPerson() {
        return namedEntityType == NamedEntityType.PERSON;
    }

    public void setPerson() {
        this.namedEntityType = NamedEntityType.PERSON;
    }

    public boolean isLocation() {
        return namedEntityType == NamedEntityType.LOCATION;
    }

    public void setLocation() {
        this.namedEntityType = NamedEntityType.LOCATION;
    }

    public boolean isOrganization() {
        return namedEntityType == NamedEntityType.ORGANIZATION;
    }

    public void setOrganization() {
        this.namedEntityType = NamedEntityType.ORGANIZATION;
    }

    public void setNamedEntityType(NamedEntityType namedEntityType) {
        this.namedEntityType = namedEntityType;
    }

    /**
     * Lower-cases the token.
     */
    public void toLowerCase() {
        if (token != null) token = token.toLowerCase();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getStartCharacter() {
        return startCharacter;
    }

    public void setStartCharacter(int startCharacter) {
        this.startCharacter = startCharacter;
    }

    public int getEndCharacter() {
        return endCharacter;
    }

    public void setEndCharacter(int endCharacter) {
        this.endCharacter = endCharacter;
    }

    public int getStartCharacterCandidate() {
        return startCharacterCandidate;
    }

    public void setStartCharacterCandidate(int startCharacterCandidate) {
        this.startCharacterCandidate = startCharacterCandidate;
    }

    public int getEndCharacterCandidate() {
        return endCharacterCandidate;
    }

    public void setEndCharacterCandidate(int endCharacterCandidate) {
        this.endCharacterCandidate = endCharacterCandidate;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "{" +
                " " + token +
                ", " + lemma +
                ", " + partOfSpeech  +
                ", " + namedEntityType +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token1 = (Token) o;

        return token != null ? token.equals(token1.token) : token1.token == null;

    }

    @Override
    public int hashCode() {
        return token != null ? token.hashCode() : 0;
    }

    @Override
    public String word() {
        return token;
    }

    @Override
    public void setWord(String s) {
        setToken(s);
    }
}
