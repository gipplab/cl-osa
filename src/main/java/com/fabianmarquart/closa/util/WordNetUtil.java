package com.fabianmarquart.closa.util;


import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.data.Word;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import net.sf.extjwnl.dictionary.Dictionary;
import com.fabianmarquart.closa.model.Token;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * The WordNetUtil class uses WordNet locally to retrieve morphosemantic links to an IndexWord.
 * A file released by WordNet called morphosemantic-links.csv is required to perform this mapping.
 * <p>
 * Created by Fabian Marquart on 2018/10/28.
 */
public class WordNetUtil {

    private static final String morphosemanticLinksPath = "/wordnet/morphosemantic-links.csv";
    private static Map<String, List<String>> morphosemanticMap;
    private static Dictionary dictionary;

    /*
      Initializes with the records given by the csv file.
     */
    static {
        try {
            dictionary = Dictionary.getDefaultResourceInstance();

            InputStream inputStream = WordNetUtil.class.getResourceAsStream(morphosemanticLinksPath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Iterable<CSVRecord> records = CSVFormat.RFC4180.withDelimiter(';').withFirstRecordAsHeader().parse(reader);

            morphosemanticMap = new HashMap<>();

            for (CSVRecord record : records) {
                String arg1 = record.get("arg1 sensekey");
                String arg2 = record.get("arg2 sensekey");

                if (morphosemanticMap.containsKey(arg1)) {
                    morphosemanticMap.put(arg1, Stream.concat(morphosemanticMap.get(arg1).stream(), Stream.of(arg2))
                            .collect(Collectors.toList()));
                } else {
                    morphosemanticMap.put(arg1, Collections.singletonList(arg2));
                }
            }

        } catch (IOException | JWNLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Maps a verb token to a noun token using WordNet.
     *
     * @param token original token, has to be a verb.
     * @return morphosemantically related noun token.
     */
    public static Token mapVerbToNoun(Token token) {

        // WordNet instantiation
        try {
            IndexWord indexWord = dictionary.lookupIndexWord(POS.VERB, token.getLemma());

            // no mapping if word is not in the dictionary
            if (indexWord == null) {
                return token;
            }

            List<String> links = WordNetUtil.getMorphosemanticLink(indexWord);

            if (links.size() > 0) {
                // if a noun lemma is identical to the verb lemma, use it
                if (!links.contains(token.getLemma())) {
                    // else, use a different one
                    token.setLemma(links.get(0));
                }
            }

            return token;
        } catch (JWNLException e) {
            e.printStackTrace();
        }

        // no modification
        return token;
    }


    /**
     * Gets related morphosemantically related words from WordNet.
     *
     * @param indexWord the word to query.
     * @return morphosemantically related words.
     */
    public static List<String> getMorphosemanticLink(IndexWord indexWord) {
        List<String> morphosemanticallyRelatedLemmas = new ArrayList<>();

        try {
            // System.out.println("indexWord = " + indexWord);

            for (Synset sense : indexWord.getSenses()) {
                // System.out.println("synset = " + sense);

                for (Word word : sense.getWords()) {
                    if (word.getSenseKey().contains(indexWord.getLemma())) {

                        List<String> morphosemanticLinks = morphosemanticMap.get(word.getSenseKey());

                        if (morphosemanticLinks != null) {
                            // System.out.println("\t word = " + word);
                            // System.out.println("\t word senseKey = " + word.getSenseKey());
                            // System.out.println("\t link = " + morphosemanticMap.get(word.getSenseKey()));
                            morphosemanticallyRelatedLemmas.addAll(morphosemanticLinks);
                        }

                    }
                }
            }
        } catch (JWNLException e) {
            e.printStackTrace();
        }

        morphosemanticallyRelatedLemmas = morphosemanticallyRelatedLemmas
                .stream()
                .map(string -> string.split("%")[0])
                .collect(Collectors.toList());

        return morphosemanticallyRelatedLemmas;
    }

    public static Dictionary getDictionary() {
        return dictionary;
    }
}
