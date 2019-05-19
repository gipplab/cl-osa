package com.fabianmarquart.closa.util;

import com.fabianmarquart.closa.model.Token;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * This class contains everything related to online machine
 * translation.
 * <p>
 * Created by Fabian Marquart on 2017/01/06.
 */
@SuppressWarnings("FieldCanBeLocal")
public class TranslationUtil {

    private static String yandexApiCall = "https://translate.yandex.net/api/v1.5/tr/translate?";

    // Yandex Key:
    // trnsl.1.1.20161215T095107Z.21e4715db6afd38f.99a817455e6c81e980aff628576c0a2fde2e610d
    private static String yandexKey1 = "trnsl.1.1.20161215T095107Z.21e4715db6afd38f.99a817455e6c81e980aff628576c0a2fde2e610d";
    private static String yandexKey2 = "trnsl.1.1.20170808T211059Z.614e9b96903bae53.3cc73e71fb5f01bbee37313819aa160fe31704d3";
    private static String yandexKey = "trnsl.1.1.20190519T185620Z.a13e2d466bfd0f48.e7fa74f26efcefd253902a5fe9bae0f0ff50aad4";
    private static String yandexText = "&text=";
    private static String yandexLang = "&lang=";

    private static HttpClient client = HttpClientBuilder.create().build();


    /**
     * Takes a list of tokens and returns a list of translated tokens.
     *
     * @param tokens         the tokens to be translated.
     * @param sourceLanguage e.g. en
     * @param targetLanguage e.g. ja
     * @return the translated tokens.
     */
    static List<Token> translateTokens(List<Token> tokens, String sourceLanguage, String targetLanguage) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();

        int size = tokens.size();

        // rejoin the last period with the word before.
        if (tokens.get(size - 1).getToken().equals(".")) {
            tokens.get(size - 2).setToken(tokens.get(size - 2).getToken() + ".");
            tokens.remove(size - 1);
        }

        // build a string separated by whitespace
        for (Token token : tokens) {
            stringBuilder.append(token).append("%20");
        }

        String translation = translate(stringBuilder.toString(), sourceLanguage, targetLanguage);

        return TokenUtil.tokenize(translation, targetLanguage);
    }


    /**
     * Use Yandex translate via HTTP Post request, and unpack xml response.
     *
     * @param text           the text to be translated
     * @param sourceLanguage e.g. en
     * @param targetLanguage e.g. ja
     * @return translation or same text if failed.
     */
    public static String translate(String text, String sourceLanguage, String targetLanguage) {
        List<String> sentences = Arrays.asList(text.split(". "));

        StringBuilder currentTranslation = new StringBuilder();
        StringBuilder currentSentence = new StringBuilder();
        for (String sentence : sentences) {
            if (currentSentence.length() + sentence.length() + ". ".length() >= 1000) {
                currentTranslation.append(translateChunk(currentSentence.toString(), sourceLanguage, targetLanguage)).append(". ");
                currentSentence = new StringBuilder();
            } else {
                currentSentence.append(sentence).append(". ");
            }
        }

        return currentTranslation.toString();
    }


    /**
     * Use Yandex translate via HTTP Post request, and unpack xml response.
     *
     * @param text           the text to be translated
     * @param sourceLanguage e.g. en
     * @param targetLanguage e.g. ja
     * @return translation or same text if failed.
     */
    private static String translateChunk(String text, String sourceLanguage, String targetLanguage) {
        try {
            URIBuilder builder = new URIBuilder("https://translate.yandex.net/api/v1.5/tr/translate");

            builder.addParameter("key", yandexKey)
                    .addParameter("lang", String.format("%s-%s", sourceLanguage, targetLanguage))
                    .addParameter("text", text);

            HttpPost post = new HttpPost(builder.build());

            HttpResponse response = client.execute(post);

            // parse post from xml
            String xml = EntityUtils.toString(response.getEntity(), UTF_8);

            if (xml.contains("Error code=\"413\"")) {
                throw new IllegalStateException("The text size exceeds the maximum.");
            }

            return getXMLElement(xml);
        } catch (URISyntaxException | SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get an XML top-level element by name.
     *
     * @param xml xml as string
     * @return the element's content.
     * @throws IOException                  input source cannot be accessed
     * @throws SAXException                 input cannot be parsed
     * @throws ParserConfigurationException document cannot be parsed
     */
    private static String getXMLElement(String xml)
            throws ParserConfigurationException, IOException, SAXException {

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource src = new InputSource();
        src.setCharacterStream(new StringReader(xml));

        Document doc = builder.parse(src);
        return doc.getElementsByTagName("text").item(0).getTextContent();
    }
}