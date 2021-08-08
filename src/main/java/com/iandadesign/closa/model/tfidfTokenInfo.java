package com.iandadesign.closa.model;

import java.util.ArrayList;
import java.util.List;

/**
 * TF/IDF information holder class. Rather experimental stage.
 * @author Johannes Stegmüller
 */
public class tfidfTokenInfo {
    public long occurences = 0;
    public List<String>  lemmas = new ArrayList<>();
    // For idf
    public int N = 0;
    public int n_t = 0;
    public double idf = 0;


    public long max_t = 0;
    public double tf = 0;
    public double tfidf = 0;

    public long getOccurences() {
        return occurences;
    }
    public String getLemmaAt(int position){
        if(position<lemmas.size()){
            return lemmas.get(position);
        }
        return "";
    }
}
