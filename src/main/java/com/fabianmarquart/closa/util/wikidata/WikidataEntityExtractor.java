package com.fabianmarquart.closa.util.wikidata;

import com.fabianmarquart.closa.classification.Category;
import com.fabianmarquart.closa.classification.TextClassifier;
import com.fabianmarquart.closa.model.Token;
import com.fabianmarquart.closa.model.WikidataEntity;
import com.fabianmarquart.closa.util.TokenUtil;
import com.fabianmarquart.closa.util.WordNetUtil;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.fabianmarquart.closa.util.wikidata.WikidataDumpUtil.*;

/**
 * WikidataEntityExtractor is an almost singleton class that performs entity extraction from a text.
 * <p>
 * Created by Fabian Marquart on 2018/08/03.
 */
public class WikidataEntityExtractor {

    /**
     * Extract Wikidata entities from given text, language.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER -> filter by POS -> entity extraction -> entity disambiguation
     *
     * @param text         the text
     * @param languageCode language
     * @return list of Wikidata entities found in the text
     */
    public static List<WikidataEntity> extractEntitiesFromText(String text, String languageCode) {
        return extractEntitiesFromText(text, languageCode, new TextClassifier().classifyText(text, languageCode));
    }


    /**
     * Extract Wikidata entities from given text, language and topic.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER -> filter by POS -> entity extraction -> entity disambiguation
     *
     * @param text         the text
     * @param languageCode language
     * @param category     category
     * @return list of Wikidata entities found in the text
     */
    public static List<WikidataEntity> extractEntitiesFromText(String text, String languageCode, Category category) {
        List<List<WikidataEntity>> extractedEntities = extractEntitiesFromTextWithoutDisambiguation(text, languageCode, category);
        List<WikidataEntity> disambiguatedEntities = new ArrayList<>();

        for (List<WikidataEntity> currentEntities : extractedEntities) {
            // disambiguate
            if (currentEntities.size() > 1) {
                disambiguatedEntities.add(WikidataDisambiguator.ancestorCountDisambiguate(currentEntities, text, languageCode));
            } else if (currentEntities.size() == 1) {
                disambiguatedEntities.add(currentEntities.get(0));
            }
        }

        return disambiguatedEntities;
    }


    /**
     * Extract Wikidata entities from given text, language.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER -> filter by POS -> entity extraction.
     *
     * @param text         the text
     * @param languageCode language
     * @return list of Wikidata entity candidate lists found in the text
     */
    public static List<List<WikidataEntity>> extractEntitiesFromTextWithoutDisambiguation(String text, String languageCode) {
        return extractEntitiesFromTextWithoutDisambiguation(text, languageCode, new TextClassifier().classifyText(text, languageCode));
    }

    /**
     * Extract Wikidata entities from given text, language and topic.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER -> filter by POS -> entity extraction.
     *
     * @param text         the text
     * @param languageCode language
     * @param category     category
     * @return list of Wikidata entity candidate lists found in the text
     */
    public static List<List<WikidataEntity>> extractEntitiesFromTextWithoutDisambiguation(
            String text,
            String languageCode,
            Category category) {
        // tokenize
        List<Token> tokenList = TokenUtil.namedEntityTokenize(text, languageCode).stream()
                .flatMap(List::stream).collect(Collectors.toList());

        // optional per-language stop-word removal
        if (languageCode.equals("ja")) {
            tokenList = TokenUtil.removeStopwords(tokenList, languageCode);
        }

        List<List<List<Token>>> subtokensLists = getSublistsOfSize(tokenList, 3);

        // filter
        List<String> forbiddenPartOfSpeechTags = Arrays.asList(",", ":", ".", "SYM", "TO", "IN", "PP", "PP$", "SENT");

        List<List<WikidataEntity>> extractedEntities = new ArrayList<>();

        ProgressBar progressBar = new ProgressBar(String.format("Extract entities from %s %s text.", languageCode, category), subtokensLists.size(), ProgressBarStyle.ASCII);
        progressBar.start();

        // TODO: use Java 8 streams
        for (List<List<Token>> subtokensList : subtokensLists) {

            for (int i = 0; i < subtokensList.size(); i++) {

                List<Token> subtokens = subtokensList.get(i);

                boolean isWhitespaceSeparatedLanguage = !(languageCode.equals("zh") || languageCode.equals("ja") ||
                        languageCode.equals("ar"));

                // build token from sub-tokens
                Token token = new Token(subtokens, isWhitespaceSeparatedLanguage ? " " : "");

                // filter
                if (Objects.requireNonNull(TokenUtil.getStopwords(languageCode)).contains(token.getLemma())) {
                    continue;
                }
                if (forbiddenPartOfSpeechTags.contains(token.getPartOfSpeech())) {
                    continue;
                }

                // verb to noun mapping
                if (token.getPartOfSpeech().contains("V")) {
                    if (token.getToken().contains("ing") || token.getToken().contains("tion")) {
                        token.setLemma(token.getToken());
                    } else {
                        token = WordNetUtil.mapVerbToNoun(token);
                    }
                }

                // extract entities
                // System.out.print(token + " => ");
                List<WikidataEntity> currentEntities = getEntitiesByToken(token,
                        // if english text bits are contained in a chinese text
                        languageCode.equals("zh") && TokenUtil.isLatinAlphabet(token)
                                ? "en"
                                : languageCode, category)
                        .stream()
                        // no CJK character pages
                        .filter(entity ->
                                (!languageCode.equals("zh") && !languageCode.equals("ja"))
                                        || !entity.getDescriptions().getOrDefault("en", "").contains("CJK character (hanzi/kanji/hanja)"))
                        // no Wikimedia disambiguation pages
                        .filter(entity -> !entity.getDescriptions().getOrDefault(languageCode, "").contains("Wikimedia "))
                        // if text is not about fiction, remove pages about music, movies or tv shows
                        .filter(entity -> category.equals(Category.fiction) || !isCreativeWork(entity))
                        // if text is not about biology, remove pages about genes
                        .filter(entity -> category.equals(Category.biology) || !isGene(entity))
                        // TODO: if category is not linguistics, remove entities like "the (definite article)"
                        // only keep pages about numbers when the token is numeric
                        .filter(entity -> !StringUtils.isNumeric(entity.getOriginalLemma())
                                || (StringUtils.isNumeric(entity.getOriginalLemma()) && isNaturalNumber(entity)))
                        .collect(Collectors.toList());

                // System.out.println(currentEntities);

                extractedEntities.add(currentEntities);

                // if biggest group has result, don't consider smaller token groups anymore
                if (currentEntities.size() > 0) {
                    if (i + 1 < subtokensList.size() && subtokens.size() > subtokensList.get(i + 1).size()) {
                        // progressBarSubtokens.stop();
                        break;
                    }
                }
            }
            progressBar.step();
        }
        progressBar.stop();

        return extractedEntities;
    }


    /**
     * Extract Wikidata entities from given text and language.
     * <p>
     * Tokenization / POS-tagging / lemmatization / NER -> filter by POS -> entity extraction.
     *
     * @param text         the text
     * @param languageCode language
     * @return list of Wikidata entity candidate lists found in the text
     */
    public static List<List<WikidataEntity>> extractEntitiesFromTextWithoutDisambiguation(String text, String languageCode, TextClassifier textClassifier) {
        return extractEntitiesFromTextWithoutDisambiguation(text, languageCode, textClassifier.classifyText(text, languageCode));
    }


    /**
     * Gets a list of T sublists of sizes 1 to n, e.g. {1, 2, 3, 4} with n = 3 becomes
     * {
     * {{1,2,3},{1,2},{2,3},{1},{2},{3}},
     * {{2,3,4},{2,3},{3,4},{2},{3},{4}}
     * }
     *
     * @param list input list.
     * @param n    target sublist size maximum.
     * @param <T>  type parameter.
     * @return List of T sublists of sizes 1 to n.
     */
    static <T> List<List<List<T>>> getSublistsOfSize(List<T> list, int n) {
        if (list.size() == 1) {
            return Collections.singletonList(Collections.singletonList(list));
        }

        List<List<List<T>>> sublists = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {

            List<List<T>> sublist = new ArrayList<>();

            List<T> currentSublist = new ArrayList<>();

            for (int j = 0; j < n && i + j < list.size(); j++) {
                currentSublist.add(list.get(i + j));
                sublist.add(new ArrayList<>(currentSublist));
            }

            sublists.add(sublist);
        }

        // move last n-2 lists into preceeding list
        int initialSublistsSize = sublists.size();

        for (int i = initialSublistsSize - 1; i > 0; i--) {
            if (sublists.get(i).stream().anyMatch(currentList -> currentList.size() == n)) {
                break;
            }

            sublists.get(i - 1).addAll(sublists.get(i));
            sublists.remove(i);
        }

        // sort by list length
        sublists.forEach(sublist -> sublist.sort(Comparator.comparing(List::size, Comparator.reverseOrder())));

        return sublists;
    }


}


