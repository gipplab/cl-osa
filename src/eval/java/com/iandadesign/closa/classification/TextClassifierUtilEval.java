package com.iandadesign.closa.classification;

import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.ConceptUtil;
import com.iandadesign.closa.util.TokenUtil;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import me.tongfei.progressbar.ProgressBar;Ø
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TextClassifierUtilEval {


    @Test
    public void testConstructor() {
        TextClassifier classifier = new TextClassifier(Arrays.asList("fiction", "neutral"));
    }

    @Test
    public void getClassificationCorpusFromWikipedia() {
        TextClassifier textClassifier = new TextClassifier();

        List<String> languages = Arrays.asList("zh", "en", "fr", "es", "ja", "de");

        Map<String, String> categoriesByTitle = new HashMap<>();

        // Wikipedia training articles
        Arrays.asList(
                "WHI3", "CHON", "Anesthesia", "Peptide", "Alpha helix", "Triosephosphate isomerase",
                "Triosephosphate isomerase", "Hepatitis B", "Immunoglobulin G", "Epileptic seizure",
                "Saccharomyces cerevisiae", "Carboxylic acid", "Papillary muscle", "Antibiotic",
                "Adenosine triphosphate"
        ).forEach(title -> categoriesByTitle.put(title, "biology"));

        Arrays.asList(
                "Hollywood", "Film industry", "Television show", "Cinematography", "The Rolling Stones", "Comedy"
        ).forEach(title -> categoriesByTitle.put(title, "fiction"));

        Arrays.asList(
                "World Trade Organization", "Investment management", "Finance", "United States presidential election",
                "United States Constitution"
        ).forEach(title -> categoriesByTitle.put(title, "neutral"));


        // for different languages
        for (String language : languages) {
            // Create a new bayes classifier with string categories and string features.
            Classifier<String, String> bayesClassifier = new BayesClassifier<>();
            bayesClassifier.setMemoryCapacity(Integer.MAX_VALUE);

            // for all titles per category
            for (Map.Entry<String, String> entry : categoriesByTitle.entrySet()) {
                String title = entry.getKey();
                String category = entry.getValue();

                String pageId = ConceptUtil.getPageIdByTitle(title, "en");
                String pageIdInLanguage = ConceptUtil.getPageIdInLanguage(pageId, "en", language);

                if (pageIdInLanguage == null) {
                    continue;
                }

                String text = ConceptUtil.getPageContent(pageIdInLanguage, language);
                List<String> tokens = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(text, language)
                        .stream()
                        .map(Token::getToken)
                        .collect(Collectors.toList());

                try {
                    String path = String.format("src/main/resources/corpus/categorization/%s/%s/%s.txt", language, category, title);
                    FileUtils.writeLines(new File(path), tokens);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                bayesClassifier.learn(category, tokens);
            }

            textClassifier.getClassifierMap().put(language, bayesClassifier);
        }

        evaluateClassifier();
    }


    /**
     * Evaluate the bayesian classifier against uClassify.
     */
    @Test
    public void evaluateClassifier() {
        /*
            Classification for language en:
            Correct rate = 0.9591623
            Incorrect rate = 0.040837698
            Sample size = 955.0

            Classification for language zh:
            Correct rate = 0.99372387
            Incorrect rate = 0.0062761507
            Sample size = 478.0

            Classification for language ja:
            Correct rate = 0.88297874
            Incorrect rate = 0.11702128
            Sample size = 94.0

            Classification for language de:
            Correct rate = 0.8553459
            Incorrect rate = 0.1446541
            Sample size = 318.0

            Classification for language fr:
            Correct rate = 0.9878296
            Incorrect rate = 0.012170386
            Sample size = 493.0

            Classification for language es: (STS only)
            Correct rate = 0.93939394
            Incorrect rate = 0.060606062
            Sample size = 99.0
         */

        TextClassifier textClassifier = new TextClassifier();
        LanguageDetector languageDetector = new LanguageDetector();

        // test files
        List<File> files = new ArrayList<>();

        files.addAll(FileUtils.listFiles(new File("src/test/resources/com/iandadesign/closa/evaluation/test-vroniplag"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/en"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File("src/test/resources/com/iandadesign/closa/evaluation/test-bbc/ja"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File("src/test/resources/com/iandadesign/closa/evaluation/ECCE"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"),
                TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File(System.getProperty("user.home") + "/sts2016/txt/"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));

        List<String> languages = Arrays.asList("en", "fr", "es", "ja", "de", "zh");

        for (String language : languages) {
            AtomicInteger correct = new AtomicInteger();
            AtomicInteger incorrect = new AtomicInteger();

            Map<String, String> pathContentMap = files.stream()
                    .sorted()
                    .filter(file -> !file.getName().equals(".DS_Store"))
                    .limit(500)
                    .collect(Collectors.toMap(File::getPath,
                            file -> {
                                try {
                                    return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                return "";
                            }));

            ProgressBar progressBar = new ProgressBar("Evaluate categorizer for language " + language, pathContentMap.size(), ProgressBarStyle.ASCII).start();

            pathContentMap.entrySet()
                    .stream()
                    .filter(entry -> language.equals(languageDetector.detectLanguage(entry.getValue())))
                    .forEach(entry -> {
                        File topicFile;

                        if (entry.getKey().contains("pds")) {
                            topicFile = new File(entry.getKey().replace("pds", "pds/topic"));
                        } else {
                            topicFile = new File(entry.getKey().replace(System.getProperty("user.home"), String.format("%s/topic", System.getProperty("user.home"))));
                        }

                        Topic topic1 = null;

                        if (Files.exists(topicFile.toPath())) {
                            try {
                                topic1 = Topic.valueOf(FileUtils.readFileToString(topicFile, StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            topic1 = textClassifier.uClassifyText(entry.getValue(), language);
                            try {
                                FileUtils.writeStringToFile(topicFile, topic1.name(), Charset.forName("UTF-8"));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Category topic2 = textClassifier.classifyText(entry.getValue(), language);

                        assert topic1 != null;
                        if (topic1.equals(Topic.Health)) {
                            if (topic2.equals(Category.biology)) {
                                correct.getAndIncrement();
                            } else {
                                incorrect.getAndIncrement();
                                System.out.printf("Incorrect: Classified %s but should be %s%n", topic2, topic1);
                                System.out.println("File name: " + entry.getKey());
                            }
                        } else if ((topic1.equals(Topic.Arts) || topic1.equals(Topic.Games))) {
                            if (topic2.equals(Category.fiction)) {
                                correct.getAndIncrement();
                            } else {
                                incorrect.getAndIncrement();
                                System.out.printf("Incorrect: Classified %s but should be %s%n", topic2, topic1);
                                System.out.println("File name: " + entry.getKey());
                            }
                        } else {
                            correct.getAndIncrement();
                        }

                        progressBar.step();
                    });

            progressBar.stop();

            System.out.println("Classification for language " + language + ":");

            float sampleSize = correct.get() + incorrect.get();

            System.out.println("Correct rate = " + correct.get() / sampleSize);
            System.out.println("Incorrect rate = " + incorrect.get() / sampleSize);
            System.out.println("Sample size = " + sampleSize);
            System.out.println();
        }

    }
}
