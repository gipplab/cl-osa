package com.iandadesign.closa.model;


import com.google.common.base.Objects;

import java.util.HashMap;
import java.util.Map;

/**
 * Class representing an entity from Wikidata.
 *
 * @author Fabian Marquart
 */
public class WikidataEntity {

    private String id;
    private String label;

    private Map<String, String> labels;
    private Map<String, String> descriptions;
    private String originalLemma;

    /**
     * Creates a new instance of WikiDataEntity.
     *
     * @param id : WikiData ID, starting with Q (not the full URL)
     */
    public WikidataEntity(String id) {
        this.id = id;
    }

    /**
     * Creates a new instance of WikiDataEntity.
     *
     * @param id    : WikiData ID, starting with Q (not the full URL)
     * @param label : English name
     */
    public WikidataEntity(String id, String label) {
        this(id);
        this.label = label;
    }

    /**
     * Creates a new instance of WikiDataEntity.
     *
     * @param id     : WikiData ID, starting with Q (not the full URL)
     * @param label  : English name
     * @param labels : A map for languageCode - name value pairs
     */
    public WikidataEntity(String id, String label, Map<String, String> labels) {
        this(id, label);
        this.labels = labels;
    }

    /**
     * Creates a new instance of WikiDataEntity.
     *
     * @param id           : WikiData ID, starting with Q (not the full URL)
     * @param label        : English name
     * @param labels       : A map for languageCode - name value pairs
     * @param descriptions : A map for languageCode - description value pairs
     */
    public WikidataEntity(String id, String label, Map<String, String> labels, Map<String, String> descriptions) {
        this(id, label, labels);
        this.descriptions = descriptions;
    }

    public WikidataEntity(String id, String label, String description, String descriptionLanguage) {
        this(id, label);
        this.descriptions = new HashMap<>();
        this.descriptions.put(descriptionLanguage, description);
    }

    public String getOriginalLemma() {
        return originalLemma;
    }

    public void setOriginalLemma(String originalLemma) {
        this.originalLemma = originalLemma;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {

        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(Map<String, String> descriptions) {
        this.descriptions = descriptions;
    }


    @Override
    public String toString() {
        return "$q_{" + id.substring(1) + "} & " + label;
        // return "{" + id + ", " + label + ", " + originalLemma + ", " + (descriptions != null ? descriptions.getOrDefault("en", "") : "") + "}";
    }

    /**
     * Entity is equal if id is equal.
     *
     * @param o other entity
     * @return true if ids are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WikidataEntity that = (WikidataEntity) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
