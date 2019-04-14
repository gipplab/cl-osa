package com.fabianmarquart.closa.language;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LanguageDetector {

    private static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    private static final Logger optimaizeLogger = loggerContext.getLogger("com.optimaize.langdetect");
    private static com.optimaize.langdetect.LanguageDetector languageDetector;


    /**
     * Constructor for all supported languages.
     */
    public LanguageDetector() {
        optimaizeLogger.setLevel(Level.ERROR);

        try {
            List<LdLocale> languages = Stream.of("en", "de", "fr", "es", "ja", "zh-CN", "zh-TW", "hi", "it", "ru")
                    .map(LdLocale::fromString)
                    .collect(Collectors.toList());

            // load all languages
            List<LanguageProfile> languageProfiles = new LanguageProfileReader()
                    .readBuiltIn(languages);

            // build language detector
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor for a subset of supported languages.
     *
     * @param languageCodes languages to consider.
     */
    public LanguageDetector(List<String> languageCodes) {
        optimaizeLogger.setLevel(Level.ERROR);

        try {
            List<LdLocale> languages = languageCodes.stream()
                    .map(languageCode -> languageCode.equals("zh") ? "zh-CN" : languageCode)
                    .map(LdLocale::fromString)
                    .collect(Collectors.toList());

            // load all languages
            List<LanguageProfile> languageProfiles = new LanguageProfileReader()
                    .readBuiltIn(languages);

            // build language detector
            languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                    .withProfiles(languageProfiles)
                    .build();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Detects a text's language.
     *
     * @param text the text.
     * @return language code string.
     */
    public String detectLanguage(String text) {
        // start
        Optional<LdLocale> ldLocaleOptional;

        // create a text object factory
        TextObjectFactory textObjectFactory;

        if (text.length() <= 50) {
            textObjectFactory = CommonTextObjectFactories.forDetectingShortCleanText();
        } else {
            textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText();
        }

        // detect the suspicious text's language, (translate) and tokenize it
        ldLocaleOptional = languageDetector.detect(textObjectFactory.forText(text));

        if (ldLocaleOptional != null && ldLocaleOptional.isPresent()) {
            return ldLocaleOptional.get().getLanguage();
        }

        // default
        return "en";
    }
}
