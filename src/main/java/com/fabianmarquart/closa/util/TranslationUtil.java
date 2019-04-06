package com.fabianmarquart.closa.util;

import com.fabianmarquart.closa.model.Token;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

/**
 * This class contains everything related to online machine
 * translation.
 *
 * Created by Fabian Marquart on 2017/01/06.
 */
public class TranslationUtil {

    private static String yandexApiCall = "https://translate.yandex.net/api/v1.5/tr/translate?";

    // Yandex Key:
    // trnsl.1.1.20161215T095107Z.21e4715db6afd38f.99a817455e6c81e980aff628576c0a2fde2e610d
    private static String yandexKey = "key=trnsl.1.1.20161215T095107Z.21e4715db6afd38f.99a817455e6c81e980aff628576c0a2fde2e610d";
    private static String yandexKey2 = "key=trnsl.1.1.20170808T211059Z.614e9b96903bae53.3cc73e71fb5f01bbee37313819aa160fe31704d3";
    private static String yandexText = "&text=";
    private static String yandexLang = "&lang=";


    /**
     * Takes a list of tokens and returns a list of translated tokens.
     * @param tokens the tokens to be translated.
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
     * @param text the text to be translated
     * @param sourceLanguage e.g. en
     * @param targetLanguage e.g. ja
     * @return translation or same text if failed.
     */
    public static String translate(String text, String sourceLanguage, String targetLanguage) throws Exception {

        String strURL = yandexApiCall + yandexKey + yandexLang
                + sourceLanguage + "-" + targetLanguage + yandexText + text;

        try {
            URL url = new URL(strURL.replaceAll("\\s+","%20"));
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            // add request header
            connection.setRequestMethod("POST");

            // parse post from xml
            String xml = null;
            try {
                xml = IOUtils.toString(connection.getInputStream(), "UTF8");
            } catch (IOException e) {
                if (e.getMessage().contains("403 for URL")) {
                    System.out.println("Daily limit exceeded");
                    throw new Exception("Daily limit exceeded");
                }
                e.printStackTrace();
            }

            return getXMLElement(xml);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            e.printStackTrace();
            return text;
        }
    }


    /**
     * Get an XML top-level element by name.
     * @param xml xml as string
     * @return the element's content.
     * @throws IOException input source cannot be accessed
     * @throws SAXException input cannot be parsed
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