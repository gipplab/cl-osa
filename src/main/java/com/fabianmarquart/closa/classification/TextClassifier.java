package com.fabianmarquart.closa.classification;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.fabianmarquart.closa.model.Token;
import com.fabianmarquart.closa.util.TokenUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.FilterBuilder;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
    private final List<String> apiKeys = Arrays.asList(
            "c3LtarI0DK1B",
            "XRAkPoML5FH7", "XE4tzJVY85dj", "3Iehl2rAZdX0", "0SRglD0zYbWT", "4EGZdjVR0ogY",
            "DR9Ix6mulbmP", "GQod5rweigdz", "pRh4lQEfx9tt", "L4YDONsJjtMw", "kxt4e1I52Pt8", "NLiolOwBVkbw"
    );
    private int currentApiKey = 0;
    private HttpClient httpClient;
    // classifier map
    private Map<String, Classifier<String, String>> classifierMap;


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
        // initialize http client only once
        httpClient = HttpClientBuilder.create().build();

        // initialize classifier map
        classifierMap = new HashMap<>();

        supportedLanguages.forEach(language -> {
            Classifier<String, String> bayesClassifier = new BayesClassifier<>();

            supportedCategories.forEach(category -> {

                Reflections reflections = new Reflections(
                        String.format("com.fabianmarquart.closa.classification.%s.%s", language, category),
                        new ResourcesScanner());

                Set<String> resources = reflections.getResources(new FilterBuilder().include(".*\\.txt"))
                        .stream()
                        .map(resource -> resource.replace("com/fabianmarquart/closa/classification/", ""))
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

    /**
     * Getter for classifier map.
     *
     * @return Classifier map.
     */
    public Map<String, Classifier<String, String>> getClassifierMap() {
        return classifierMap;
    }

    /**
     * Classify text into topic, using uClassify API.
     *
     * @param textToClassify text to classify.
     * @return topic.
     * @deprecated relies on external classifier.
     */
    @Deprecated
    public Topic uClassifyText(String textToClassify, String language) {
        if (!supportedLanguages.contains(language)) {
            throw new IllegalArgumentException("Language " + language + " is not supported.");
        }

        for (int i = 0; i < apiKeys.size(); i++) {
            PrintStream originalStream = System.out;
            PrintStream dummyStream = new PrintStream(new OutputStream() {
                public void write(int b) {
                    // NO-OP
                }
            });
            System.setOut(dummyStream);

            String responseBody = "";

            try {
                String baseUrl = "https://api.uclassify.com/v1/";
                String userName = "uClassify";
                String classifierName = "Topics";
                HttpPost post = new HttpPost(String.format("%s%s/%s/%s/classify", baseUrl, userName, classifierName, language));
                post.addHeader("Content-Type", "application/json");
                post.addHeader("Authorization", "Token " + apiKeys.get(currentApiKey));

                JsonObject json = new JsonObject();
                JsonArray textsArray = new JsonArray();
                textsArray.add(textToClassify);
                json.add("texts", textsArray);

                StringEntity requestBody = new StringEntity(json.toString());

                post.setEntity(requestBody);

                HttpResponse response = httpClient.execute(post);

                responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

                ClassificationReport[] reports = new Gson().fromJson(responseBody, ClassificationReport[].class);

                Topic topic = Topic.valueOf(reports[0].getClassification().stream()
                        .max(Comparator.comparingDouble(ClassificationReport.Classification::getP))
                        .get()
                        .getClassName());

                System.setOut(originalStream);

                return topic;
            } catch (JsonSyntaxException e) {
                System.setOut(originalStream);
                e.printStackTrace();
                currentApiKey = (currentApiKey + 1) % apiKeys.size();
            } catch (IOException e) {
                System.setOut(originalStream);
                e.printStackTrace();
                System.out.println(responseBody);
            }
        }

        throw new IllegalStateException();
    }


}
