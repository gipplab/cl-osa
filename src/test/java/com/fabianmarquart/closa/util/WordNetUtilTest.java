package com.fabianmarquart.closa.util;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.dictionary.Dictionary;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WordNetUtilTest {

    @Test
    public void getMorphosemanticLinkTest() {

        try {
            Dictionary dictionary = Dictionary.getDefaultResourceInstance();

            Map<String, String> verbToNounMap = new HashMap<>();
            verbToNounMap.put("discover", "discoverer");
            verbToNounMap.put("discuss", "discussion");
            verbToNounMap.put("land", "lander");
            verbToNounMap.put("arrest", "arrest");

            for (Map.Entry<String, String> entry : verbToNounMap.entrySet()) {
                IndexWord indexWord = dictionary.lookupIndexWord(POS.VERB, entry.getKey());

                Assert.assertNotNull(indexWord);

                List<String> links = WordNetUtil.getMorphosemanticLink(indexWord);

                System.out.print(entry.getKey() + ": ");
                System.out.println(links);

                Assert.assertTrue(links.contains(entry.getValue()));
            }

        } catch (JWNLException e) {
            e.printStackTrace();
        }

    }
}
