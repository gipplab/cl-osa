package com.iandadesign.closa.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.collect.Lists;
import com.google.gson.*;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class is used for concept detection.
 * <p>
 * Created by Fabian Marquart on 2018/02/22.
 */
public class ConceptUtil {


    private static Logger logger = Logger.getLogger(TokenUtil.class);

    private static String wikipediaTitleQuery = ".wikipedia.org/w/api.php?action=sendQuery&titles=";
    private static String wikipediaPropsAndFormat = "&prop=pageprops&ppprop=disambiguation&format=json";

    @SuppressWarnings("FieldCanBeLocal")
    private static int wikipediaRequestLimit = 50;

    @SuppressWarnings("FieldCanBeLocal")
    private static String wikidataSiteQuery = "https://www.wikidata.org/w/api.php?action=wbgetentities&sites=";

    private static HttpClient client = HttpClientBuilder.create().build();
    private static JsonParser parser = new JsonParser();

    private static LanguageDetector languageDetector;


    static {
        ConceptUtil.languageDetector = new LanguageDetector();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger apacheHttpLogger = loggerContext.getLogger("org.apache.http");
        apacheHttpLogger.setLevel(Level.OFF);
    }

    /**
     * Takes an input text and returns a list of concepts from it.
     *
     * @param text the input text
     * @param n    the maximum length of word sequences to find
     * @return the list of concepts.
     */
    public static List<Token> getConceptsFromString(String text, int n) {
        String languageCode = languageDetector.detectLanguage(text);
        return getConceptsFromString(text, n, languageCode);
    }

    /**
     * Takes an input text and returns a list of concepts from it.
     *
     * @param text         the input text
     * @param n            the maximum length of token sequences to be considered
     * @param languageCode the language code, e.g. "en", "de", "ja", "zh".
     * @return the list of concepts.
     */
    private static List<Token> getConceptsFromString(String text, int n, String languageCode) {
        List<Token> tokens = TokenUtil.tokenize(text, true);
        return getConceptsFromTokens(tokens, n, languageCode);
    }


    /**
     * Takes input tokens end returns a list of tokens that are considered concepts by Wikipedia.
     *
     * @param tokens       the input tokens
     * @param n            the maximum token sequence length to be considered
     * @param languageCode the language code, e.g. "en", "de", "ja", "zh".
     * @return the list of concepts.
     */
    public static List<Token> getConceptsFromTokens(List<Token> tokens, int n, String languageCode) {

        // 1.1 pre-processing
        tokens = TokenUtil.removeNumbers(tokens);
        tokens = TokenUtil.removePunctuation(tokens);
        tokens.removeIf((Token token) -> token.getToken().contains("\n"));

        // 1.2 for processing large amount of requests, many are sent at once
        List<List<Token>> tokensToBeRequested = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            // make token sequences of sizes 1 to n and add them to the request list
            for (int j = 0; (j <= n) && (i + j < tokens.size()); j++) {
                tokensToBeRequested.add(tokens.subList(i, i + j));
            }
        }

        // 1.3 create title sendQuery strings from the token lists
        List<Token> titlesToBeQueried = new ArrayList<>();

        for (List<Token> tokenGroup : tokensToBeRequested) {
            StringJoiner stringJoiner = new StringJoiner("_", "", "");

            // no spaces for Chinese and Japanese
            if (languageCode.equals("ja") || languageCode.equals("zh")) {
                titlesToBeQueried.removeIf(title -> title.getToken().equals("　")
                        || title.getToken().equals(" "));
                stringJoiner = new StringJoiner("", "", "");
            }

            for (Token token : tokenGroup) {
                stringJoiner.add(token.getToken());
            }
            Token desiredToken = new Token(stringJoiner.toString());

            if (!desiredToken.getToken().equals("")) {
                titlesToBeQueried.add(desiredToken);
            }
        }


        // 1.4 partition the requests into requests of allowed length
        List<String> requests = new ArrayList<>();

        Lists.partition(titlesToBeQueried.stream().map(Token::getToken).collect(Collectors.toList()), wikipediaRequestLimit)
                .forEach(partition -> requests.add(StringUtils.join(partition, "|")));

        Map<String, String> pageTitleMap = new HashMap<>();

        for (String request : requests) {
            pageTitleMap.putAll(getPageTitleMap(request, languageCode));
        }


        // 1.5 for English language in foreign text
        List<Token> englishTitlesToBeQueried = new ArrayList<>();

        // 2 sort out titles missing from Wikipedia
        ListIterator<Token> titleIterator = titlesToBeQueried.listIterator();

        while (titleIterator.hasNext()) {
            Token title = titleIterator.next();
            String processedTitle;

            if (languageCode.equals("ja") || languageCode.equals("zh")) {
                processedTitle = title.getToken().toLowerCase().replaceAll("_", "");
                processedTitle = processedTitle.toLowerCase().replaceAll(" ", "");
            } else {
                processedTitle = title.getToken().toLowerCase().replaceAll("_", " ");
            }

            if ((!pageTitleMap.containsKey(processedTitle)
                    || (pageTitleMap.get(processedTitle).equals("missing")))) {

                // missing in Wikipedia.
                titleIterator.remove();
                englishTitlesToBeQueried.add(title);
            } else if (!languageCode.equals("ja") && !languageCode.equals("zh")
                    && processedTitle.length() <= 2) {
                titleIterator.remove();
            }
        }

        // 3.1 remove stopwords from concepts:
        titlesToBeQueried.forEach(token -> token.setToken(token.getToken().replaceAll("_", " ")));
        titlesToBeQueried.forEach(Token::toLowerCase);
        titlesToBeQueried = TokenUtil.removeStopwords(titlesToBeQueried, languageCode);


        // 3.2 special case of English in German papers
        if (languageCode.equals("de")) {
            List<String> englishRequests = new ArrayList<>();
            Lists.partition(englishTitlesToBeQueried.stream().map(Token::getToken).collect(Collectors.toList()), wikipediaRequestLimit)
                    .forEach(partition -> englishRequests.add(StringUtils.join(partition, "|")));
            Map<String, String> englishPageTitleMap = new HashMap<>();

            for (String request : englishRequests) {
                englishPageTitleMap.putAll(getPageTitleMap(request, "en"));
            }

            // 3.2.1 sort out titles missing from Wikipedia
            ListIterator<Token> englishTitleIterator = englishTitlesToBeQueried.listIterator();
            while (englishTitleIterator.hasNext()) {
                Token title = englishTitleIterator.next();
                String processedTitle = title.getToken().toLowerCase().replaceAll("_", " ");

                if ((!englishPageTitleMap.containsKey(processedTitle)
                        || (englishPageTitleMap.get(processedTitle).equals("missing")))) {
                    // missing in English Wikipedia.
                    englishTitleIterator.remove();
                }
            }

            englishTitlesToBeQueried.forEach(token -> token.setToken(token.getToken().replaceAll("_", " ")));
            englishTitlesToBeQueried.forEach(Token::toLowerCase);
            englishTitlesToBeQueried = TokenUtil.removeStopwords(englishTitlesToBeQueried, "en");


            if (englishTitlesToBeQueried != null && titlesToBeQueried != null) {
                titlesToBeQueried.addAll(englishTitlesToBeQueried);
            }
        }

        // 3.3 return the filtered concept list OR an empty list
        Set<Token> foundConcepts;

        if (titlesToBeQueried != null) {
            return titlesToBeQueried;
        } else {
            foundConcepts = new HashSet<>();
        }

        return new ArrayList<>(foundConcepts);
    }

    /**
     * This method sends a title request of multiple titles to any language wikipedia and
     * retrieves a map with titles and corresponding ids.
     *
     * @param title        the titles
     * @param languageCode language code
     * @return a map with titles and ids.
     */
    public static Map<String, String> getPageTitleMap(String title, String languageCode) {
        String formattedString = title.replaceAll("(\n|\r|\t|\\s)*", "");
        if (formattedString.startsWith("|")) {
            formattedString = formattedString.substring(1);
        }
        formattedString = formattedString.replaceAll("\\|+", "|");

        String strURL = "https://" + languageCode + wikipediaTitleQuery
                + formattedString + wikipediaPropsAndFormat;

        String response = sendRequest(strURL);
        return jsonGetTitleMap(response);
    }

    /**
     * Checks whether the given title exists in English Wikipedia.
     *
     * @param title the queried title
     * @return true if it exists, false otherwise
     */

    public static boolean titleExists(String title) {
        return titleExists(title, "en");
    }

    /**
     * Checks whether the given title exists in the Wikipedia of the given language.
     *
     * @param title    the queried title
     * @param language the langauge code, e.g. "en", "de", "ja"
     * @return true if it exists, false otherwise
     */
    private static boolean titleExists(String title, String language) {
        String formattedString = title.replaceAll(" ", "_");

        String strURL = "https://" + language + wikipediaTitleQuery + formattedString + wikipediaPropsAndFormat;

        String response = sendRequest(strURL);
        Map<String, String> pageIdTitleMap = jsonGetTitleMap(response);

        // read json response
        return pageIdTitleMap.get(title) == null || !pageIdTitleMap.get(title).equals("missing");
    }


    /**
     * Finds the value for a given jsonFile string and the requested memberName.
     *
     * @param jsonFile the json file as a string.
     * @return the contents of the memberName field.
     */
    private static Map<String, String> jsonGetTitleMap(String jsonFile) {
        try {
            // create a new JSON parser and get the object pages
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(jsonFile);
            JsonObject object = element.getAsJsonObject();
            JsonObject pages = object.getAsJsonObject("sendQuery").getAsJsonObject("pages");

            // pull all ids from the pages object into a map
            Map<String, String> intermediateMap = new HashMap<>();
            Map<String, String> pageTitleMap = new HashMap<>();
            intermediateMap = new Gson().fromJson(pages, intermediateMap.getClass());

            for (String currentKey : intermediateMap.keySet()) {

                JsonObject currentPage = pages.getAsJsonObject(currentKey);

                String title = currentPage.getAsJsonPrimitive("title").getAsString().toLowerCase();

                if (currentPage.has("missing")
                        || currentPage.has("invalidreason")) {
                    pageTitleMap.put(title, "missing");
                } else if (currentPage.has("pageprops")) {
                    pageTitleMap.put(title, "disambiguation");
                } else {
                    pageTitleMap.put(title, "present");
                }
            }

            return pageTitleMap;


        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.trace("Malformed json file: " + jsonFile);
        } catch (ClassCastException | NullPointerException e) {
            logger.trace("Could not process json file: " + jsonFile);
        }
        return new HashMap<>();
    }


    /**
     * Find the translation of a Wikipedia concept in a different language.
     *
     * @param title          the Wikipedia page title ("concept").
     * @param sourceLanguage the source language's code (e.g. "en").
     * @param targetLanguage the target language's code (e.g. "ja").
     * @return the title in the target language.
     */
    private static String translateTitle(String title, String sourceLanguage, String targetLanguage) {
        String formattedString = title.substring(0, 1).toUpperCase() + title.substring(1);
        formattedString = formattedString.replaceAll(" ", "%20");

        String strURL = wikidataSiteQuery + sourceLanguage + "wiki&" +
                "titles=" + formattedString + "&languages=" + targetLanguage + "&utf8=&format=json";

        String response = sendRequest(strURL);

        return jsonProcessEntities(response, targetLanguage);
    }

    /**
     * Translates a list of concepts by using Wikipedia and Yandex translate
     * as fallback.
     *
     * @param concepts       the concepts
     * @param sourceLanguage source language code
     * @param targetLanguage target language code
     * @return the translated concepts
     */
    public static List<Token> translateConcepts(List<Token> concepts, String sourceLanguage, String targetLanguage) {
        if (sourceLanguage.equals(targetLanguage)) {
            return concepts;
        }

        List<Token> translatedConcepts = new ArrayList<>();

        for (Token concept : concepts) {
            String translatedConcept = translateTitle(concept.getToken(), sourceLanguage, targetLanguage);

            // if Wikipedia has no translation
            if (translatedConcept == null
                    || translatedConcept.equals("missing") || translatedConcept.equals("disambiguation")) {
                // use Yandex
                try {
                    concept.setToken(TranslationUtil.translate(concept.getToken(), sourceLanguage, targetLanguage));
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
                translatedConcepts.add(concept);
            } else {
                // otherwise use Wikipedia translation
                concept.setToken(translatedConcept);
                translatedConcepts.add(concept);
            }

            // if translation is a compound
            if (concept.getToken() != null && concept.getToken().contains(" ")) {
                String[] separatedTokens = concept.getToken().split(" ");
                if (separatedTokens.length > 1) {
                    for (String separatedToken : separatedTokens) {
                        translatedConcepts.add(new Token(separatedToken));
                    }
                }
            }
        }

        return translatedConcepts;
    }


    /**
     * Map Wikipedia page id to page id from original language into target language. If non-existant, null is returned.
     *
     * @param pageId           page id in original language
     * @param originalLanguage original language code
     * @param targetLanguage   target language code
     * @return page id in given language.
     */
    public static String getPageIdInLanguage(String pageId, String originalLanguage, String targetLanguage) {
        // no sendQuery if the languages are the same
        if (originalLanguage.equals(targetLanguage)) {
            return pageId;
        }

        String wikipediaPageTitleInLanguage = null;

        // get wikipedia page title in language
        try {
            URIBuilder builder = new URIBuilder("https://" + originalLanguage + ".wikipedia.org/w/api.php");
            builder.addParameter("action", "sendQuery")
                    .addParameter("pageids", pageId)
                    .addParameter("prop", "langlinks")
                    .addParameter("lllang", targetLanguage)
                    .addParameter("format", "json");

            HttpGet get = new HttpGet(builder.build());
            HttpResponse response = client.execute(get);

            JsonObject object = parser.parse(EntityUtils.toString(response.getEntity(), UTF_8)).getAsJsonObject();
            JsonObject page = object.getAsJsonObject("sendQuery")
                    .getAsJsonObject("pages")
                    .getAsJsonObject(pageId);

            if (!page.has("langlinks")) {
                return null;
            }

            wikipediaPageTitleInLanguage = page.getAsJsonArray("langlinks").get(0).getAsJsonObject().get("*").getAsString();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return getPageIdByTitle(wikipediaPageTitleInLanguage, targetLanguage);
    }

    public static boolean hasPageIdLanguage(String pageId, String originalLanguage, String targetLanguage) {

        return originalLanguage.equals(targetLanguage) || getPageIdInLanguage(pageId, originalLanguage, targetLanguage) != null;

    }


    /**
     * Map Wikipedia page title to page id in language.
     *
     * @param pageTitle page title
     * @param language  language
     * @return id.
     */
    public static String getPageIdByTitle(String pageTitle, String language) {
        // get id in language
        try {
            URIBuilder builder = new URIBuilder(String.format("https://%s.wikipedia.org/w/api.php", language));
            builder.addParameter("action", "sendQuery")
                    .addParameter("titles", pageTitle)
                    .addParameter("format", "json");

            HttpGet get = new HttpGet(builder.build());
            HttpResponse response = client.execute(get);

            JsonObject object = parser.parse(EntityUtils.toString(response.getEntity(), UTF_8)).getAsJsonObject();

            if (!object.has("sendQuery")) {
                return null;
            }

            JsonObject query = object.getAsJsonObject("sendQuery");

            if (!query.has("pages")) {
                return null;
            }

            Set<Map.Entry<String, JsonElement>> pages = query
                    .getAsJsonObject("pages")
                    .entrySet();

            if (!pages.iterator().hasNext()) {
                return null;
            }

            return pages.iterator().next().getKey();

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Map Wikipedia page title to Wikidata id in language.
     *
     * @param pageTitle page title
     * @param language  language
     * @return Wikidata id.
     */
    public static String getWikidataIdByTitle(String pageTitle, String language) {
        try {
            URIBuilder builder = new URIBuilder(String.format("https://%s.wikipedia.org/w/api.php", language));
            builder.addParameter("action", "sendQuery")
                    .addParameter("prop", "pageprops")
                    .addParameter("titles", pageTitle)
                    .addParameter("format", "json");

            HttpGet get = new HttpGet(builder.build());
            HttpResponse response = client.execute(get);

            JsonObject object = parser.parse(EntityUtils.toString(response.getEntity(), UTF_8)).getAsJsonObject();

            if (!object.has("sendQuery")) {
                return null;
            }

            JsonObject query = object.getAsJsonObject("sendQuery");

            if (!query.has("pages")) {
                return null;
            }

            Set<Map.Entry<String, JsonElement>> pages = query
                    .getAsJsonObject("pages")
                    .entrySet();

            if (!pages.iterator().hasNext()) {
                return null;
            }

            JsonObject pageEntry = pages.iterator().next()
                    .getValue()
                    .getAsJsonObject();

            JsonObject pageprops = pageEntry
                    .getAsJsonObject("pageprops");

            if (pageprops == null || !pageprops.has("wikibase_item")) {
                return null;
            }

            return pageprops.get("wikibase_item").getAsString();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Get Wikipedia page summmary by title and language code.
     *
     * @param pageId   page id
     * @param language language code
     * @return page summary.
     */
    public static String getPageContent(String pageId, String language) {
        try {
            URIBuilder builder = new URIBuilder(String.format("https://%s.wikipedia.org/w/api.php", language));
            builder.addParameter("action", "sendQuery")
                    .addParameter("prop", "extracts")
                    .addParameter("redirects", "1")
                    .addParameter("pageids", pageId)
                    .addParameter("format", "json");

            HttpGet get = new HttpGet(builder.build());
            HttpResponse response = client.execute(get);

            JsonObject object = parser.parse(EntityUtils.toString(response.getEntity(), UTF_8)).getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> pages = object
                    .getAsJsonObject("sendQuery")
                    .getAsJsonObject("pages")
                    .entrySet();

            if (!pages.iterator().hasNext()) {
                return null;
            }

            String extract = pages.iterator().next().getValue().getAsJsonObject()
                    .get("extract").getAsString();

            return Jsoup.parse(extract).text();
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * sends a URL GET request to the API.
     *
     * @param strURL the URL as a string.
     * @return the response string in json format.
     */
    private static String sendRequest(String strURL) {
        boolean requestSuccessful = false;
        while (!requestSuccessful) {
            try {
                URL url = new URL(strURL);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(); // exception: URL is not valid
                // exception: URL connection cannot be opened

                // add request header
                connection.setRequestMethod("GET"); // exception: GET unknown

                // Send post request
                connection.setDoOutput(true);
                DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                outputStream.flush();
                outputStream.close();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = bufferedReader.readLine()) != null) {
                    response.append(inputLine);
                }
                bufferedReader.close();

                requestSuccessful = true;
                return response.toString();
            } catch (MalformedURLException e) {
                logger.trace("Malformed URL: " + strURL);
                return "missing";
            } catch (UnknownHostException e) {
                logger.trace("Unknown host. Trying again…");
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            } catch (IOException e) {
                logger.trace("HTTP response 503. Trying again: " + strURL);
                e.printStackTrace();
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        return "missing";
    }


    /**
     * Finds the title in the target language for a given json file and the targetLanguage code.
     *
     * @param jsonFile       the json file.
     * @param targetLanguage the target language code.
     * @return the title in the target language.
     */
    private static String jsonProcessEntities(String jsonFile, String targetLanguage) {
        try {
            // create a new JSON parser and get the object pages
            JsonParser parser = new JsonParser();
            JsonElement element = parser.parse(jsonFile);
            JsonObject object = element.getAsJsonObject();
            JsonObject entities = object.getAsJsonObject("entities");

            // pull all ids from the pages object into a map
            Map<String, Integer> result = new HashMap<>();
            result = new Gson().fromJson(entities, result.getClass());

            // get the title source language - title target language mappings
            Iterator<String> keyIterator = result.keySet().iterator();
            List<String> translations = new ArrayList<>();

            while (keyIterator.hasNext()) {
                String currentKey = keyIterator.next();
                if (currentKey.equals("-1")) {
                    translations.add("missing");
                } else if (entities.getAsJsonObject(currentKey)
                        .getAsJsonObject("descriptions")
                        .getAsJsonObject(targetLanguage)
                        .getAsJsonPrimitive("value")
                        .toString().contains("disambiguation")) {
                    translations.add("disambiguation");
                } else {
                    translations.add(entities.getAsJsonObject(currentKey)
                            .getAsJsonObject("labels")
                            .getAsJsonObject(targetLanguage)
                            .getAsJsonPrimitive("value")
                            .toString().replaceAll("\"", ""));
                }
            }
            return translations.get(0);
        } catch (ClassCastException | NullPointerException e) {

            // else return null
            logger.trace("Could not process json file: " + jsonFile);
            return null;
        }
    }
}
