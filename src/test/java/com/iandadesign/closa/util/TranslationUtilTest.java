package com.iandadesign.closa.util;

import com.iandadesign.closa.model.Token;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;


/**
 * Created by Fabian Marquart on 2017/01/06.
 */
public class TranslationUtilTest {


    @Test
    public void testSimpleTranslation() {
        String textJapanese = "クロス言語剽窃検知";
        String textEnglish = "Cross-language plagiarism detection";

        // Yandex:
        try {
            String yandexTranslatedText = TranslationUtil.translate(textJapanese, "ja", "en");
            String yandexTranslatedText2 = TranslationUtil.translate(textEnglish, "en", "ja");
            Assert.assertTrue(yandexTranslatedText.equals(textEnglish));
            Assert.assertTrue(yandexTranslatedText2.equals(textJapanese));
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testTokenTranslation() {
        List<Token> tokensEnglish = TokenUtil.tokenize("Cross-language plagiarism detection", "en");
        List<Token> tokensJapanese = TokenUtil.tokenize("クロス言語剽窃検知", "ja");

        try {
            List<Token> translatedTokens = TranslationUtil.translateTokens(tokensEnglish, "en", "ja");

            Assert.assertTrue(translatedTokens.equals(tokensJapanese));
        } catch (Exception e) {
            Assert.fail();
        }
    }

}