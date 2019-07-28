package com.iandadesign.closa.util;

import com.iandadesign.closa.model.Token;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/**
 * Created by Fabian Marquart on 2017/01/06.
 */
public class TranslationUtilTest {

    @Test
    void translateTokens() {
        List<Token> tokensEnglish = TokenUtil.tokenize("Cross-language plagiarism detection", "en");
        List<Token> tokensJapanese = TokenUtil.tokenize("クロス言語剽窃検知", "ja");

        try {
            List<Token> translatedTokens = TranslationUtil.translateTokens(tokensEnglish, "en", "ja");

            Assertions.assertEquals(translatedTokens, tokensJapanese);
        } catch (Exception e) {
            Assertions.fail();
        }
    }

    @Test
    void translate() {
        String textJapanese = "クロス言語剽窃検知";
        String textEnglish = "Cross-language plagiarism detection";

        try {
            String yandexTranslatedText = TranslationUtil.translate(textJapanese, "ja", "en");
            String yandexTranslatedText2 = TranslationUtil.translate(textEnglish, "en", "ja");
            Assertions.assertEquals(yandexTranslatedText, textEnglish);
            Assertions.assertEquals(yandexTranslatedText2, textJapanese);
        } catch (Exception e) {
            Assertions.fail();
        }
    }
}