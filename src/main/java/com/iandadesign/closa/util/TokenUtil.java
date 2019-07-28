package com.iandadesign.closa.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.Token;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.PropertiesUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.atilika.kuromoji.Tokenizer;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.tagging.de.GermanTagger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * This class does everything related to tokenization, stop-word removal, punctuation mark
 * stripping and language detection.
 * <p>
 * Created by Fabian Marquart on 2016/12/13.
 */
public class TokenUtil {

    private static final String properNounJa = "固有名詞";
    private static final String locationJa = "地域";
    private static final String personJa = "人名";
    private static final String organizationJa = "組織";
    private static final String symbolJa = "記号";
    private static final String verbJa = "動詞";
    private static final String fullStopJa = "句点";

    private static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final Logger stanfordNlpLogger = loggerContext.getLogger("edu.stanford.nlp");

    private static LanguageDetector languageDetector;

    static {
        stanfordNlpLogger.setLevel(Level.ERROR);
        TokenUtil.languageDetector = new LanguageDetector();
    }

    /**
     * Tokenize white-space separated text into one list of tokens.
     *
     * @param textContent the text as string.
     * @return a list of tokens.
     */
    private static List<Token> tokenize(String textContent) {
        if (textContent == null || textContent.equals("")) {
            return new ArrayList<>();
        }

        List<Token> tokenList = new ArrayList<>();

        PTBTokenizer<CoreLabel> tokenizer = new PTBTokenizer<>(new StringReader(textContent),
                new CoreLabelTokenFactory(true), "ptb3Escaping=false, untokenizable=noneDelete");
        List<Token> currentTokenList = new ArrayList<>();

        // iterate over all tokens
        int index = 0;
        while (tokenizer.hasNext()) {
            CoreLabel label = tokenizer.next();

            int startCharacter = label.beginPosition();
            int endCharacter = label.endPosition();

            // separate at period
            if (label.toString().equals(".")) {
                currentTokenList.add(new Token(label.toString(), startCharacter, endCharacter, index));
                tokenList.addAll(currentTokenList);
                currentTokenList = new ArrayList<>();
            } else {
                currentTokenList.add(new Token(label.toString(), startCharacter, endCharacter, index));
            }

            index += 1;
        }

        // if last sentence has no period
        if (currentTokenList.size() > 0) {
            tokenList.addAll(currentTokenList);
        }

        // punctuation removal
        tokenList = removePunctuation(tokenList);

        return tokenList;
    }


    private static List<List<Token>> namedEntityTokenizeSpanish(String text) {
        throw new NotImplementedException("");
        /*
        List<List<Token>> tokensBySentence = new ArrayList<>();
        List<Token> tokens = new ArrayList<>();

        POSTaggerSpanishLanguage posTaggerSpanishLanguage = new POSTaggerSpanishLanguage();

        try {
            posTaggerSpanishLanguage.tokenize(text, "", "");

        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokensBySentence;
        */
    }

    /**
     * Advanced tokenization of Japanese texts including named entity recognition.
     *
     * @param text: the text.
     * @return List of tokens.
     */
    private static List<List<Token>> namedEntityTokenizeJapanese(String text) {
        List<List<Token>> tokensBySentence = new ArrayList<>();
        List<Token> tokens = new ArrayList<>();

        Tokenizer japaneseTokenizer = Tokenizer.builder().build();

        // for each token
        for (org.atilika.kuromoji.Token kuromojiToken : japaneseTokenizer.tokenize(text)) {
            // get token, lemma, pos and ner
            String tokenString = kuromojiToken.getSurfaceForm();
            String lemma = kuromojiToken.getBaseForm();
            String partOfSpeech = kuromojiToken.getPartOfSpeech();

            Token.NamedEntityType namedEntityType = Token.NamedEntityType.O;

            if (partOfSpeech.contains(properNounJa)) {
                if (partOfSpeech.contains(locationJa)) {
                    namedEntityType = Token.NamedEntityType.LOCATION;
                } else if (partOfSpeech.contains(personJa)) {
                    namedEntityType = Token.NamedEntityType.PERSON;
                } else if (partOfSpeech.contains(organizationJa)) {
                    namedEntityType = Token.NamedEntityType.ORGANIZATION;
                }
            }

            // no punctuation
            if (partOfSpeech.contains(symbolJa)) {
                continue;
            }

            // join with previous token if pos and ner is equal
            if (!tokens.isEmpty()) {

                int lastTokenIndex = tokens.size() - 1;
                Token lastToken = tokens.get(lastTokenIndex);

                // exclude verbs because they tend to be composed such that 行きます　gets the lemmas 行く and ます
                // where only the first should be kept
                // O means not a named entity
                if (!partOfSpeech.contains(verbJa) && namedEntityType != Token.NamedEntityType.O && partOfSpeech.equals(lastToken.getPartOfSpeech())
                        && namedEntityType.equals(lastToken.getNamedEntityType())) {

                    tokens.set(lastTokenIndex, new Token(lastToken, new Token(tokenString, lemma), ""));
                } else {
                    tokens.add(new Token(tokenString, lemma, partOfSpeech, namedEntityType));
                }
            } else {
                tokens.add(new Token(tokenString, lemma, partOfSpeech, namedEntityType));
            }

            // split sentences at full stop
            if (kuromojiToken.getPartOfSpeech().contains(fullStopJa)) {
                // add the completed sentence
                tokensBySentence.add(new ArrayList<>(tokens));
                tokens = new ArrayList<>();
            }
        }

        // if the last sentence has no full stop
        if (!tokens.isEmpty()) {
            tokensBySentence.add(tokens);
        }

        return tokensBySentence;
    }

    /**
     * Advanced tokenization including named entity recognition.
     *
     * @param text         the text.
     * @param languageCode the language code.
     * @return tokenized text with named entity annotations on tokens.
     */
    public static List<List<Token>> namedEntityTokenize(String text, String languageCode) {
        List<List<Token>> tokensBySentence = new ArrayList<>();

        StanfordCoreNLP pipeline;

        switch (languageCode) {
            case "en":
                pipeline = new StanfordCoreNLP(
                        PropertiesUtils.asProperties(
                                "annotators", "tokenize, ssplit, pos, lemma, ner", //, parse",
                                "ssplit.isOneSentence", "false",
                                "parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz",
                                "tokenize.language", "en",
                                "untokenizable", "noneDelete",
                                "ner.useSUTime", "false"));
                break;
            case "zh":
                pipeline = new StanfordCoreNLP(
                        PropertiesUtils.asProperties(
                                "annotators", "tokenize, ssplit, pos, lemma, ner", //, parse",
                                "ssplit.isOneSentence", "false",
                                // "parse.model", "edu/stanford/nlp/models/srparser/chineseSR.ser.gz",
                                // segment
                                "tokenize.language", "zh",
                                "segment.model", "edu/stanford/nlp/models/segmenter/chinese/ctb.gz",
                                "segment.sighanCorporaDict", "edu/stanford/nlp/models/segmenter/chinese",
                                "segment.serDictionary", "edu/stanford/nlp/models/segmenter/chinese/dict-chris6.ser.gz",
                                "segment.sighanPostProcessing", "true",
                                // sentence split
                                "ssplit.boundaryTokenRegex", "[.。]|[!?！？]+",
                                // pos model
                                "pos.model", "edu/stanford/nlp/models/pos-tagger/chinese-distsim/chinese-distsim.tagger",
                                // ner
                                "ner.language", "chinese",
                                "ner.model", "edu/stanford/nlp/models/ner/chinese.misc.distsim.crf.ser.gz",
                                "ner.applyNumericClassifiers", "true",
                                "ner.useSUTime", "false",
                                // regexner
                                "regexner.mapping", "edu/stanford/nlp/models/kbp/cn_regexner_mapping.tab",
                                "regexner.validpospattern", "^(NR|NN|JJ).*",
                                "regexner.ignorecase", "true",
                                "regexner.noDefaultOverwriteLabels", "CITY"
                        ));
                break;
            case "de":
                pipeline = new StanfordCoreNLP(
                        PropertiesUtils.asProperties(
                                "annotators", "tokenize, ssplit, pos, lemma, ner", //, parse",
                                "ssplit.isOneSentence", "false",
                                "parse.model", "edu/stanford/nlp/models/srparser/germanSR.ser.gz",
                                "tokenize.language", "de",
                                "ner.useSUTime", "false"));
                break;
            case "fr":
                pipeline = new StanfordCoreNLP(
                        PropertiesUtils.asProperties(
                                "annotators", "tokenize, ssplit, pos, lemma, ner", //, parse",
                                "ssplit.isOneSentence", "false",
                                "parse.model", "edu/stanford/nlp/models/srparser/frenchSR.beam.ser.gz",
                                "tokenize.language", "fr",
                                "ner.useSUTime", "false",
                                "untokenizable", "noneDelete"));
                break;
            case "es":
                // TODO: lemmatize
                // return namedEntityTokenizeSpanish(text);

                pipeline = new StanfordCoreNLP(
                        PropertiesUtils.asProperties(
                                "annotators", "tokenize, ssplit, pos, lemma, ner", //, parse",
                                "ssplit.isOneSentence", "false",
                                "parse.model", "edu/stanford/nlp/models/srparser/spanishSR.ser.gz",
                                "tokenize.language", "es",
                                "ner.useSUTime", "false",
                                "untokenizable", "noneDelete"));
                break;
            case "ja":
                return namedEntityTokenizeJapanese(text);
            default:
                throw new IllegalArgumentException("Language code '" + languageCode + "' not supported");
        }

        Annotation document = new Annotation(text);

        // run all Annotators on this text
        pipeline.annotate(document);

        // these are all the sentences in this document
        // a CoreMap is essentially a Map that uses class objects as keys and has values with custom types
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

        for (CoreMap sentence : sentences) {
            List<Token> tokens = new ArrayList<>();

            // traversing the words in the current sentence
            // a CoreLabel is a CoreMap with additional token-specific methods
            for (CoreLabel coreLabel : sentence.get(TokensAnnotation.class)) {
                // get token, lemma, pos and ner
                String tokenString = coreLabel.get(CoreAnnotations.TextAnnotation.class);
                String lemma = coreLabel.get(CoreAnnotations.LemmaAnnotation.class);
                String partOfSpeech = coreLabel.get(CoreAnnotations.PartOfSpeechAnnotation.class);
                Token.NamedEntityType namedEntityType = Token.NamedEntityType.valueOf(
                        coreLabel.get(CoreAnnotations.NamedEntityTagAnnotation.class));

                Token currentToken = new Token(tokenString, lemma, partOfSpeech, namedEntityType);
                currentToken.setStartCharacter(coreLabel.beginPosition());
                currentToken.setEndCharacter(coreLabel.endPosition());

                // no punctuation
                if (partOfSpeech.equals(".") || getPunctuation().contains(tokenString)) {
                    continue;
                }

                // join with previous token if POS and NE type is equal
                if (!tokens.isEmpty()) {
                    int lastTokenIndex = tokens.size() - 1;
                    Token lastToken = tokens.get(lastTokenIndex);

                    // O means not a named entity
                    if ((namedEntityType != Token.NamedEntityType.O || languageCode.equals("zh"))
                            && partOfSpeech.equals(lastToken.getPartOfSpeech())
                            && namedEntityType.equals(lastToken.getNamedEntityType())) {

                        String separator;
                        if (languageCode.equals("zh")) {
                            if (isLatinAlphabet(tokenString) && isLatinAlphabet(tokens.get(lastTokenIndex))) {
                                separator = " ";
                            } else {
                                separator = "";
                            }
                        } else {
                            separator = " ";
                        }

                        Token mergeToken = new Token(tokenString, lemma);
                        mergeToken.setStartCharacter(coreLabel.beginPosition());
                        mergeToken.setEndCharacter(coreLabel.endPosition());

                        tokens.set(lastTokenIndex, new Token(lastToken, mergeToken, separator));
                    } else {
                        tokens.add(currentToken);
                    }
                } else {
                    tokens.add(currentToken);
                }

            }

            // add the completed sentence
            tokensBySentence.add(tokens);
        }

        return tokensBySentence;
    }

    /**
     * Simple tokenization of texts in different languages.
     *
     * @param textContent    the text.
     * @param detectLanguage whether language should be detected.
     * @return the tokenized text.
     */
    public static List<Token> tokenize(String textContent, boolean detectLanguage) {
        if (detectLanguage) {
            String language = languageDetector.detectLanguage(textContent);
            return tokenize(textContent, language);
        } else {
            // white-space separated language
            return tokenize(textContent);
        }
    }

    /**
     * Full pipeline for simple tokenization.
     *
     * @param textContent the text.
     * @param language    language code.
     * @return a list of lowercased, stemmed, punctuation and stopword-free tokens.
     */
    public static List<Token> tokenizeLowercaseStemAndRemoveStopwords(String textContent, String language) {
        List<Token> result = tokenize(textContent, language);
        result.forEach(Token::toLowerCase);
        result = stem(result, language);
        result = removeStopwords(result, language);
        result = removePunctuation(result);
        return result;
    }

    /**
     * Simple tokenization of texts in different languages.
     *
     * @param textContent the text.
     * @param language    the language code.
     * @return the tokenized text.
     */
    public static List<Token> tokenize(String textContent, String language) {
        List<Token> tokenList = new ArrayList<>();

        switch (language) {
            case "ja":
                // Japanese
                Tokenizer japaneseTokenizer = Tokenizer.builder().build();

                int startCharacter = 0;
                int index = 0;

                for (org.atilika.kuromoji.Token kuromojiToken : japaneseTokenizer.tokenize(textContent)) {
                    String surfaceForm = kuromojiToken.getSurfaceForm();
                    Token token = new Token(surfaceForm,
                            startCharacter, startCharacter + surfaceForm.length() - 1,
                            index);
                    tokenList.add(token);

                    startCharacter += surfaceForm.length();
                    index += 1;
                }
                return tokenList;
            case "zh":
                // Chinese
                return namedEntityTokenize(textContent, "zh")
                        .stream()
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
            default:
                // white-space separated language
                return tokenize(textContent, false);
        }

    }


    /**
     * Take a German-language token list and lemmatize the nouns and adjectives.
     *
     * @param tokens input tokens
     * @return lemmatized token list
     */
    static List<Token> germanLemmatize(List<Token> tokens) {
        org.languagetool.tagging.de.GermanTagger tagger = new GermanTagger();

        List<String> stringTokens = tokens.stream().map(Token::getToken).collect(Collectors.toList());

        try {
            String lemmatizedText = "";
            List<AnalyzedTokenReadings> readings = tagger.tag(stringTokens, true);

            // for every adjective: set it into singular and same case as the next noun
            // while ignoring the determiner
            for (int i = 0; i < readings.size(); i++) {
                String token = readings.get(i).getToken();
                String posTag = readings.get(i).getReadings().get(0).getPOSTag();
                String lemma = readings.get(i).getReadings().get(0).getLemma();

                if (lemma != null && lemma.equals("hoch")) {
                    lemma = "hoh";
                }

                // if we are at an adjective or attribute
                if (posTag != null && (posTag.contains("ADJ") || posTag.contains("PA2"))) {

                    // look for the next noun
                    for (int j = i; (j < i + 3 && j < readings.size()); j++) {
                        String nextPosTag = readings.get(j).getReadings().get(0).getPOSTag();

                        if (nextPosTag != null && nextPosTag.contains("SUB")) {
                            // determine the nouns grammatical gender
                            // String noun = readings.get(j).getToken();
                            String gender = nextPosTag.substring(12, 15);

                            // if the noun has a gender
                            if (lemma != null) {
                                // add the corresponding morphological suffix to the adjective's lemma (stem)
                                switch (gender) {
                                    case "FEM":
                                        token = lemma.concat("e");
                                        break;
                                    case "MAS":
                                        token = lemma.concat("er");
                                        break;
                                    case "NEU":
                                        token = lemma.concat("es");
                                        break;
                                }
                            }
                            break;
                        }
                    }

                    lemmatizedText = lemmatizedText.concat(token).concat(" ");

                } else if (posTag != null && posTag.contains("SUB") && lemma != null) {
                    // noun: change to its lemma (stem)
                    lemmatizedText = lemmatizedText.concat(lemma).concat(" ");

                } else {
                    // no adjective: no change
                    lemmatizedText = lemmatizedText.concat(token).concat(" ");
                }
            }
            return tokenize(lemmatizedText.replaceAll("-", " "));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokens;
    }


    /**
     * Takes a list of tokens and returns a stemmed version.
     *
     * @param tokens   the list of tokens
     * @param language language code
     * @return the stemmed tokens
     */
    public static List<Token> stem(List<Token> tokens, String language) {
        if (tokens != null && tokens.size() == 0) {
            return tokens;
        }

        SnowballProgram stemmer = null;

        switch (language) {
            case "de":
                stemmer = new GermanStemmer();
                break;
            case "en":
                stemmer = new EnglishStemmer();
                break;
            case "ru":
                stemmer = new RussianStemmer();
                break;
            case "es":
                stemmer = new SpanishStemmer();
                break;
            case "fr":
                stemmer = new FrenchStemmer();
                break;
        }

        if (stemmer == null) {
            return tokens;
        }

        List<Token> stemmedTokens = new ArrayList<>();

        if (tokens != null) {
            for (Token token : tokens) {
                // copy token
                Token stemmedToken = new Token(token);

                // change its string
                stemmer.setCurrent(token.getToken());
                stemmer.stem();
                stemmedToken.setToken(stemmer.getCurrent());

                // normalize decades 50s, 60s to 1950s, 1960s etc.
                if (stemmedToken.getToken().matches("^[0-9]0s$")) {
                    stemmedToken.setToken("19" + stemmedToken.getToken());
                }

                // add to list
                stemmedTokens.add(stemmedToken);
            }
        }

        return stemmedTokens;
    }


    /**
     * Takes a token list and returns a token list containing only noun phrases.
     *
     * @param rawTokens input tokens.
     * @param language  language code, e.g. "en", "de".
     * @return token list of noun phraes.
     */
    static List<Token> keepNounPhrases(List<Token> rawTokens, String language) {
        List<Token> onlyNounPhrases = new ArrayList<>();

        String englishTaggerPath = "edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger";
        String germanTaggerPath = "edu/stanford/nlp/models/pos-tagger/german/german-fast.tagger";

        MaxentTagger tagger;

        switch (language) {
            case "en":
                tagger = new MaxentTagger(englishTaggerPath);
                break;
            case "de":
                tagger = new MaxentTagger(germanTaggerPath);
                break;
            default:
                throw new IllegalArgumentException("Language not supported.");
        }

        List<TaggedWord> taggedWords = tagger.tagSentence(rawTokens);

        for (int i = 0; i < taggedWords.size(); i++) {
            if (taggedWords.get(i).tag().equals("ADJA")
                    && (i + 1) < taggedWords.size()
                    && taggedWords.get(i + 1).tag().equals("NN")) {
                onlyNounPhrases.add(new Token(rawTokens.get(i), rawTokens.get(i + 1), " "));
                i++;
            } else if (taggedWords.get(i).tag().equals("NN")) {
                onlyNounPhrases.add(rawTokens.get(i));
            }
        }

        if (language.equals("de")) {
            onlyNounPhrases = germanLemmatize(onlyNounPhrases);
        }

        return onlyNounPhrases;
    }


    /**
     * Removes numbers.
     *
     * @param tokens the tokens
     * @return tokens without any numbers.
     */
    public static List<Token> removeNumbers(List<Token> tokens) {
        if (tokens != null) {
            return tokens.stream()
                    .filter(Objects::nonNull)
                    .filter(token -> !StringUtils.isNumeric(token.getToken())
                            && !token.getToken().contains("http://")
                            && !token.getToken().equals("*"))
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }


    /**
     * Returns a list of punctuation symbols.
     *
     * @return list of punctuation symbols.
     */
    static List<String> getPunctuation() {
        InputStream inputStream = WordNetUtil.class.getResourceAsStream("/corpus/punctuation/punctuation.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        // create list of punctuation symbols
        return reader.lines().collect(Collectors.toList());
    }

    /**
     * Takes a list of tokens and returns a version stripped of punctuation symbols.
     *
     * @param tokens the list of tokens
     * @return list of tokens minus the symbols
     */
    public static List<Token> removePunctuation(List<Token> tokens) {
        List<Token> tokensWithoutPunctuation;
        List<String> punctuationSymbols = getPunctuation();

        String regex = "a-zA-Z\\.";

        // compare whether it is included
        tokensWithoutPunctuation = tokens.stream()
                .filter(token -> !punctuationSymbols.contains(token.getToken())
                        && !token.getToken().matches(regex))
                .collect(Collectors.toList());

        return tokensWithoutPunctuation;
    }


    /**
     * Gets a list of stop words.
     *
     * @param language language code.
     * @return list of stop words.
     */
    public static List<String> getStopwords(String language) {
        InputStream inputStream = WordNetUtil.class.getResourceAsStream(String.format("/corpus/stopwords/stopwords_%s.txt", language));
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return reader.lines().collect(Collectors.toList());
    }

    /**
     * Removes stopwords from a text.
     *
     * @param tokens       the tokens to be manipulated.
     * @param languageCode the language.
     * @return the tokens without stopwords.
     */
    public static List<Token> removeStopwords(final List<Token> tokens, String languageCode) {
        List<Token> stopwordlessTokens = new ArrayList<>();

        // read in stopwords to ArrayList
        List<String> stopwords = getStopwords(languageCode);
        // compare whether it is included
        tokens.forEach(token -> {
            if (!stopwords.contains(token.getToken())) {
                stopwordlessTokens.add(token);
            }
        });

        return stopwordlessTokens;
    }

    /**
     * Repartition tokens into n-grams.
     *
     * @param originalTokens input
     * @param n              n-gram length
     * @return repartitioned tokens
     */
    public static List<Token> nGramPartition(List<Token> originalTokens, int n) {
        if (originalTokens == null || originalTokens.isEmpty()) {
            return new ArrayList<>();
        }

        List<Token> nGrams = new ArrayList<>();

        // split into uniGrams
        List<Token> uniGrams = new ArrayList<>();

        for (Token token : originalTokens) {
            for (int i = 0; i < token.getToken().length(); i++) {
                uniGrams.add(new Token(token.getToken().substring(i, i + 1),
                        token.getStartCharacter() + i,
                        token.getStartCharacter() + i + 1,
                        token.getIndex()));
            }
        }

        if (n == 1) {
            return uniGrams;
        }

        // build nGrams from unigrams
        for (int i = 0; i < uniGrams.size() - (n - 1); i++) {
            List<Token> currentUniGrams = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                currentUniGrams.add(uniGrams.get(i + j));
            }
            nGrams.add(new Token(currentUniGrams, ""));
        }

        return nGrams;
    }


    /**
     * Partition text into n-grams.
     *
     * @param text input
     * @param n    n-gram length
     * @return partitioned n-grams.
     */
    public static List<String> nGramPartition(String text, int n) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> nGrams = new ArrayList<>();

        // split into uniGrams
        List<String> uniGrams = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            uniGrams.add(text.substring(i, i + 1));
        }

        if (n == 1) {
            return uniGrams;
        }

        // build nGrams from unigrams
        for (int i = 0; i < uniGrams.size() - (n - 1); i++) {
            StringBuilder currentUniGrams = new StringBuilder();
            for (int j = 0; j < n; j++) {
                currentUniGrams.append(uniGrams.get(i + j));
            }
            nGrams.add(currentUniGrams.toString());
        }

        return nGrams;
    }


    /**
     * Returns true if the token contains only latin characters.
     *
     * @param token token.
     * @return true if the token contains only latin characters.
     */
    public static boolean isLatinAlphabet(Token token) {
        return isLatinAlphabet(token.getToken());
    }


    /**
     * Returns true if the string contains only latin characters.
     *
     * @param string string.
     * @return true if the string contains only latin characters.
     */
    public static boolean isLatinAlphabet(String string) {
        return Charset.forName("US-ASCII").newEncoder().canEncode(string);
    }


}