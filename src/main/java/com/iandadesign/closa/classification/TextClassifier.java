package com.iandadesign.closa.classification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.TokenUtil;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.FilterBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Class that talks to the uClassify online API to perform topic classification on texts.
 *
 * @author Fabian Marquart.
 */
public class TextClassifier {

    private static final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    private final List<String> supportedLanguages = Arrays.asList("en", "fr", "es", "zh", "ja", "de");
    private final List<String> supportedCategories;

    // classifier map
    private Map<String, Classifier<String, String>> classifierMap;


    /**
     * Getter for classifier map.
     *
     * @return Classifier map.
     */
    public Map<String, Classifier<String, String>> getClassifierMap() {
        return classifierMap;
    }


    /**
     * Constructor.
     */
    public TextClassifier() {
        Logger reflectionsLogger = loggerContext.getLogger("org.reflections");
        reflectionsLogger.setLevel(Level.OFF);

        this.supportedCategories = Arrays.asList("biology", "fiction", "neutral");
        this.trainClassifier();
    }

    /**
     * Constructor with supported categories.
     *
     * @param supportedCategories list of categories to consider.
     */
    public TextClassifier(List<String> supportedCategories) {
        Logger reflectionsLogger = loggerContext.getLogger("org.reflections");
        reflectionsLogger.setLevel(Level.OFF);

        this.supportedCategories = supportedCategories;
        this.trainClassifier();
    }

    /**
     * Use languages and categories to train classifier.
     */
    private void trainClassifier() {
        // initialize classifier map
        classifierMap = new HashMap<>();

        supportedLanguages.forEach(language -> {
            Classifier<String, String> bayesClassifier = new BayesClassifier<>();

            supportedCategories.forEach(category -> {

                Reflections reflections = new Reflections(
                        String.format("com.iandadesign.closa.classification.%s.%s", language, category),
                        new ResourcesScanner());

                Set<String> resources = reflections.getResources(new FilterBuilder().include(".*\\.txt"))
                        .stream()
                        .map(resource -> resource.replace("com/iandadesign/closa/classification/", ""))
                        .collect(Collectors.toSet());

                resources.stream()
                        .map((String resource) -> this.getClass().getResourceAsStream(resource))
                        .map((InputStream stream) -> {
                            try {
                                return IOUtils.toString(stream, StandardCharsets.UTF_8.name());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return "";
                        })
                        .map((String fileContent) -> Arrays.asList(fileContent.split("\\r?\\n")))
                        .forEach((List<String> tokenList) -> {
                            bayesClassifier.setMemoryCapacity(Integer.MAX_VALUE);
                            bayesClassifier.learn(category, tokenList);
                        });
            });

            classifierMap.put(language, bayesClassifier);
        });
    }


    /**
     * Classify text into topic.
     *
     * @param textToClassify text to classify.
     * @param language language code.
     * @return topic.
     */
    public Category classifyText(String textToClassify, String language) {
        if (!supportedLanguages.contains(language)) {
            throw new IllegalArgumentException("Language " + language + " is not supported.");
        }

        List<String> tokensToClassify = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(textToClassify, language)
                .stream()
                .map(Token::getToken)
                .collect(Collectors.toList());

        return Category.valueOf(classifierMap.get(language).classify(tokensToClassify).getCategory());
    }


}
