package com.iandadesign.closa.util;

import com.iandadesign.closa.model.Token;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WordNetUtilTest {

    @Test
    void mapVerbToNoun() {
        Assertions.assertEquals(
                WordNetUtil.mapVerbToNoun(new Token("discuss", "discuss")).getLemma(),
                "discussion"
        );
    }

    @Test
    void getMorphosemanticLink() {
        try {
            Dictionary dictionary = Dictionary.getDefaultResourceInstance();

            Map<String, String> verbToNounMap = new HashMap<>();
            verbToNounMap.put("discover", "discoverer");
            verbToNounMap.put("discuss", "discussion");
            verbToNounMap.put("land", "lander");
            verbToNounMap.put("arrest", "arrest");

            for (Map.Entry<String, String> entry : verbToNounMap.entrySet()) {
                IndexWord indexWord = dictionary.lookupIndexWord(POS.VERB, entry.getKey());

                Assertions.assertNotNull(indexWord);

                List<String> links = WordNetUtil.getMorphosemanticLink(indexWord);

                Assertions.assertTrue(links.contains(entry.getValue()));
            }

        } catch (JWNLException e) {
            e.printStackTrace();
        }
    }

    @Test
    void getDictionary() {
        Assertions.assertNotNull(WordNetUtil.getDictionary());
    }
}
