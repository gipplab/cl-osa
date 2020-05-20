package com.iandadesign.closa.model;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Class which holds the stored information of
 * preprocessed entities in the filesystem.
 *
 * @author Johannes Stegmüller
 */
public class SavedEntity implements Serializable {
    private Token token;
    private String wikidataEntityId;

    public void setToken(Token token) {
        this.token = token;
    }

    public Token getToken() {
        return token;
    }

    public String getWikidataEntityId() {
        return wikidataEntityId;
    }

    public void setWikidataEntityId(String wikidataEntityId) {
        this.wikidataEntityId = wikidataEntityId;
    }
}

