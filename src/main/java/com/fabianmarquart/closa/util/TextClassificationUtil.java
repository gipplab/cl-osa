package com.fabianmarquart.closa.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fabianmarquart.closa.model.Token;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Class that talks to the uClassify online API to perform topic classification on texts.
 *
 * @author Fabian Marquart.
 */
public class TextClassificationUtil {


    // uClassify
    private static final String baseUrl = "https://api.uclassify.com/v1/";
    private static final String userName = "uClassify";
    private static final String classifierName = "Topics";
    private static final List<String> supportedLanguages = Arrays.asList("en", "fr", "es", "zh", "ja", "de");
    private static final List<String> supportedCategories = Arrays.asList("biology", "fiction", "neutral");
    private static final List<String> apiKeys = Arrays.asList(
            "c3LtarI0DK1B",
            "XRAkPoML5FH7", "XE4tzJVY85dj", "3Iehl2rAZdX0", "0SRglD0zYbWT", "4EGZdjVR0ogY",
            "DR9Ix6mulbmP", "GQod5rweigdz", "pRh4lQEfx9tt", "L4YDONsJjtMw", "kxt4e1I52Pt8", "NLiolOwBVkbw"
    );
    private static int currentApiKey = 0;
    private static HttpClient httpClient;

    // classifier map
    private static Map<String, Classifier<String, String>> classifierMap;

    static {
        // logger
        Logger logger = (Logger) LoggerFactory.getLogger(Reflections.class);
        logger.setLevel(Level.ERROR);

        // initialize http client only once
        httpClient = HttpClientBuilder.create().build();

        // initialize classifier map
        classifierMap = new HashMap<>();

        supportedLanguages.forEach(language -> {
            Classifier<String, String> bayesClassifier = new BayesClassifier<>();

            supportedCategories.forEach(category -> {
                Reflections reflections = new Reflections(String.format("corpus/categorization/%s/%s/", language, category),
                        new ResourcesScanner());
                Set<String> resourceList = reflections.getResources(Pattern.compile(".*\\.txt"));

                // System.out.println("resourceList = " + resourceList);

                resourceList.stream()
                        .map(resource ->  {
                            InputStream inputStream = TextClassificationUtil.class.getClassLoader().getResourceAsStream(resource);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                            return reader.lines();
                        })
                        .map(stringStream -> stringStream.collect(Collectors.toList()))
                        .forEach(tokenList -> {
                            bayesClassifier.setMemoryCapacity(Integer.MAX_VALUE);
                            bayesClassifier.learn(category, tokenList);
                        });

            });

            classifierMap.put(language, bayesClassifier);
        });
    }

    public static Map<String, Classifier<String, String>> getClassifierMap() {
        return classifierMap;
    }

    public static void setClassifierMap(Map<String, Classifier<String, String>> classifierMap) {
        TextClassificationUtil.classifierMap = classifierMap;
    }

    public static List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    /**
     * Classify text into topic.
     *
     * @param textToClassify text to classify.
     * @return topic.
     */
    public static Category classifyText(String textToClassify, String language) {
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
     * Classify text into topic, using uClassify API.
     *
     * @param textToClassify text to classify.
     * @return topic.
     * @deprecated relies on external classifier.
     */
    @Deprecated
    public static Topic uClassifyText(String textToClassify, String language) {
        if (!supportedLanguages.contains(language)) {

            language = "en";
            //throw new IllegalArgumentException("Language " + language + " is not supported.");
        }

        for (int i = 0; i < apiKeys.size(); i++) {
            PrintStream originalStream = System.out;
            PrintStream dummyStream = new PrintStream(new OutputStream() {
                public void write(int b) {
                    // NO-OP
                }
            });
            System.setOut(dummyStream);

            try {
                HttpPost post = new HttpPost(String.format("%s%s/%s/%s/classify", baseUrl, userName, classifierName, language));
                post.addHeader("Content-Type", "application/json");
                post.addHeader("Authorization", "Token " + apiKeys.get(currentApiKey));


                JSONObject json = new JSONObject();
                JSONArray textsArray = new JSONArray();
                textsArray.put(textToClassify);
                json.put("texts", textsArray);

                StringEntity requestBody = new StringEntity(json.toString());

                post.setEntity(requestBody);

                HttpResponse response = httpClient.execute(post);

                String responseBody = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);

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
            }
        }

        throw new IllegalStateException();
    }

    /**
     * Enum with all topics that are output by the API, with a null topic 'None' added.
     *
     * @deprecated used for external classifier.
     */
    @Deprecated
    public enum Topic {
        Arts, Business, Computers, Games, Health, Home, Recreation, Science, Society, Sports, None
    }


    /**
     * Enum with all categories.
     */
    public enum Category {
        biology, fiction, neutral
    }


    /**
     * Inner class that is used for deserializing the JSON result.
     *
     * @deprecated used for external classifier.
     */
    @Deprecated
    private class ClassificationReport {
        private double textCoverage;
        private List<Classification> classification;

        public ClassificationReport(double textCoverage) {
            this.textCoverage = textCoverage;
        }

        @Override
        public String toString() {
            return "ClassificationReport{" +
                    "textCoverage=" + textCoverage +
                    ", classification=" + classification +
                    '}';
        }

        public double getTextCoverage() {

            return textCoverage;
        }

        public void setTextCoverage(double textCoverage) {
            this.textCoverage = textCoverage;
        }

        public List<Classification> getClassification() {
            return classification;
        }

        public void setClassification(List<Classification> classification) {
            this.classification = classification;
        }

        private class Classification {
            private String className;
            private double p;

            public Classification(String className) {
                this.className = className;
            }

            public String getClassName() {

                return className;
            }

            public void setClassName(String className) {
                this.className = className;
            }

            @Override
            public String toString() {
                return "Classification{" +
                        "className='" + className + '\'' +
                        ", p=" + p +
                        '}';
            }

            public double getP() {
                return p;
            }

            public void setP(double p) {
                this.p = p;
            }
        }
    }

}
