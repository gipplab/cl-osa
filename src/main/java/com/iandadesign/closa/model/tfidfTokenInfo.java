package com.iandadesign.closa.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class tfidfTokenInfo {
    public long occurences = 0;
    public List<String>  lemmas = new ArrayList<>();

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
