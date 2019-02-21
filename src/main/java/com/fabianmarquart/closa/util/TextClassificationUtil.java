package com.fabianmarquart.closa.util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * Class that talks to the uClassify online API to perform topic classification on texts.
 *
 * @author Fabian Marquart.
 */
public class TextClassificationUtil {

    private static final String baseUrl = "https://api.uclassify.com/v1/";
    private static final String userName = "uClassify";
    private static final String classifierName = "Topics";

    private static final List<String> apiKeys = Arrays.asList("GQod5rweigdz", "pRh4lQEfx9tt", "L4YDONsJjtMw", "kxt4e1I52Pt8", "NLiolOwBVkbw");
    private static int currentApiKey = 0;

    private static HttpClient httpClient;
    private static HttpPost post;

    static {
        httpClient = HttpClientBuilder.create().build();
    }


    /**
     * Classify text into topic.
     *
     * @param textToClassify text to classify.
     * @return topic.
     */
    public static Topic classifyText(String textToClassify, String language) {
        if (!language.equals("en") && !language.equals("fr") && !language.equals("es") && !language.equals("sv")) {
            language = "en";
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
                post = new HttpPost(String.format("%s%s/%s/%s/classify", baseUrl, userName, classifierName, language));
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
     */
    public enum Topic {
        Arts, Business, Computers, Games, Health, Home, Recreation, Science, Society, Sports, None
    }


    /**
     * Inner class that is used for deserializing the JSON result.
     */
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
