package com.fabianmarquart.closa.classification;

import com.fabianmarquart.closa.language.LanguageDetector;
import com.fabianmarquart.closa.model.Token;
import com.fabianmarquart.closa.util.ConceptUtil;
import com.fabianmarquart.closa.util.TokenUtil;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import me.tongfei.progressbar.ProgressBar;
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

public class TextClassifierUtilTest {

    /**
     * Simple test for the bayesian classifier.
     */
    @Test
    public void testClassify() {
        TextClassifier textClassifier = new TextClassifier();

        String englishText =
                "2.1.4 virulence factors virulence factors are defined as bacterial products that cause direct " +
                        "damage to the host cells. Virulence-associated factors enable the growth or Survival of the pathogen " +
                        "in the host and in this way contribute to infection and illness (MAHAN et al. 1996";

        String germanText =
                "SD kann im empfindlichen Bereich durch eine einzelne Entladung eines epileptischen Fokus " +
                        "ausgelöst werden (spike triggered SD). In der Regel werden epileptiforme Feldpotentiale während der SD " +
                        "unterdrückt und erscheinen in wenigen Minuten wieder.";

        String japaneseText =
                "ドイツ西部ケルンで大みそかに若者の集団が性的暴行や窃盗に及んだ事件を受けて5日、女性を中心に数百人が" +
                        "ケルンで抗議デモを行った。独国民に衝撃を与えたこの事件についてメルケル独首相は「胸が悪くなるような襲撃」と怒りを表明し、" +
                        "犯人を捕らえるためあらゆる手を尽くす必要があると強調した。\n" +
                        "目撃者や警察は、事件を起こした若者たちはアラブ系か北アフリカ出身のように見えたと証言している。\n" +
                        "ドイツでは、昨年に記録的な数で流入した移民や難民の受け入れをめぐり激しい議論が交わされている。移民の多くはシリア内戦" +
                        "を逃れてきた人々だ。\n" +
                        "ケルンのヘンリエッテ・レーカー市長は、犯人について早急な結論に飛びつくべきでないと訴えた。現時点では逮捕者は出ていない。";

        String chineseText =
                "二十年前的今天，当人们推倒柏林墙时，在欢庆自由的混乱人群中，很少有人能预见到这件事的后果。\n" +
                        "但抓住了这一时刻，他们便创造了历史。\n" +
                        "和那座混凝土的砖石建筑一样，德国及欧洲始于1945年的分裂状态也从此消失，由此打开了通向冷战结束、民主和自由市场进入东欧、以及欧盟(EU)壮大的道路。\n" +
                        "二十年过去了，很明显，世界从共产主义的倒台中获得了巨大的政治与经济利益。\n" +
                        "尽管今天还存在全球恐怖主义、中东问题和经济危机这样的难题，但超级大国对立局面的结束，让世界变得更加安全、更加自由、也更加富裕。\n" +
                        "德国与东欧经济一体化的完成依然存在困难，上述地区的生活水平仍然低于西方。";

        String spanishText =
                "Nada puede ser tan interesante para los pueblos del Nuevo Mundo como el estudio social de España.\n" +
                        "Las sociedades de una y otra region son muy homogéneas en sus condiciones esenciales,--en su\n" +
                        "educacion sobre todo,--y conviene compararlas para que se vea que donde quiera las mismas causas,\n" +
                        "es decir, las mismas instituciones, producen resultados análogos, habida consideracion á las\n" +
                        "diferencias geográficas. El rápido viaje de tres meses que pude hacer en España es, sin duda\n" +
                        "ninguna, muy insuficiente para formular apreciaciones bastante sólidas, tanto mas cuanto que\n" +
                        "España es uno de los pueblos que tienen ménos unidad etnográfica y social entre los mas notables\n" +
                        "de Europa.";

        String frenchText =
                "Madame la Présidente, permettez-moi de rappeler que demain aura lieu le deuxième anniversaire de la tragédie du Cermès.\n" +
                        "        En effet, il y a deux ans, à Cavalese en Italie, un avion américain venant de la base " +
                        "de l'OTAN situé à Aviano, trancha, lors d'un exercice à basse altitude - en deçà des limites de " +
                        "sécurité -, les câbles d'un téléphérique, causant la mort de plus de vingt citoyens européens.";

        Assert.assertTrue(textClassifier.classifyText(englishText, "en").equals(Category.biology));
        Assert.assertTrue(textClassifier.classifyText(germanText, "de").equals(Category.biology));
        System.out.println(textClassifier.classifyText(japaneseText, "en"));
        System.out.println(textClassifier.classifyText(chineseText, "en"));
        System.out.println(textClassifier.classifyText(spanishText, "en"));
        System.out.println(textClassifier.classifyText(frenchText, "en"));
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
        /*
        files.addAll(FileUtils.listFiles(new File("src/test/resources/org/sciplore/pds/test-vroniplag"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File("src/test/resources/org/sciplore/pds/test-bbc/en"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File("src/test/resources/org/sciplore/pds/test-bbc/ja"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File("src/test/resources/org/sciplore/pds/ECCE"), TrueFileFilter.TRUE, TrueFileFilter.TRUE));
        files.addAll(FileUtils.listFiles(new File(System.getProperty("user.home") + "/Cross-Language-Dataset-master/dataset/documents/JRC_acquis/fr"),
                TrueFileFilter.TRUE, TrueFileFilter.TRUE));*/
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
                                // System.out.printf("Incorrect: Classified %s but should be %s%n", topic2, topic1);
                                // System.out.println("File name: " + entry.getKey());
                            }
                        } else if ((topic1.equals(Topic.Arts) || topic1.equals(Topic.Games))) {
                            if (topic2.equals(Category.fiction)) {
                                correct.getAndIncrement();
                            } else {
                                incorrect.getAndIncrement();
                                // System.out.printf("Incorrect: Classified %s but should be %s%n", topic2, topic1);
                                // System.out.println("File name: " + entry.getKey());
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
     * Get random series from list.
     *
     * @param givenList          given list
     * @param randomSeriesLength number of elements to take from list.
     * @return random series from list.
     */
    private List<String> getRandomSeriesFromList(List<String> givenList, int randomSeriesLength) {
        Collections.shuffle(givenList);
        return givenList.subList(0, randomSeriesLength);
    }


    /**
     * Preliminary tests for the bayes classifier.
     */
    @Test
    public void testSimpleBayes() {
        // Create a new bayes classifier with string categories and string features.
        Classifier<String, String> bayes = new BayesClassifier<>();
        bayes.setMemoryCapacity(Integer.MAX_VALUE);

        // Two examples to learn from.
        List<String> positiveText = TokenUtil.nGramPartition("I love sunny days", 3);
        List<String> neutralText = TokenUtil.nGramPartition("Maybe it's cloudy", 3);
        List<String> negativeText = TokenUtil.nGramPartition("I hate rain", 3);

        // Learn by classifying examples.
        // New categories can be added on the fly, when they are first used.
        // A classification consists of a category and a list of features
        // that resulted in the classification in that category.
        bayes.learn("positive", positiveText);
        bayes.learn("neutral", neutralText);
        bayes.learn("negative", negativeText);

        // Here are two unknown sentences to classify.
        List<String> unknownText1 = TokenUtil.nGramPartition("today is a sunny day", 3);
        List<String> unknownText2 = TokenUtil.nGramPartition("there will be rain", 3);
        List<String> unknownText3 = TokenUtil.nGramPartition("it will be cloudy", 3);

        System.out.println( // will output "positive"
                bayes.classify(unknownText1).getCategory());
        System.out.println( // will output "negative"
                bayes.classify(unknownText2).getCategory());
        System.out.println( // will output "neutral"
                bayes.classify(unknownText3).getCategory());

        // Get more detailed classification result.
        ((BayesClassifier<String, String>) bayes).classifyDetailed(unknownText1);
    }

    @Test
    public void testTextClassificationFromFiles() {
        TextClassifier textClassifier = new TextClassifier();
        LanguageDetector languageDetector = new LanguageDetector();

        File folder = new File(System.getProperty("user.home") + "/sts2016/txt/");

        Map<String, String> pathTextMap = new HashMap<>();

        FileUtils.listFiles(folder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .map(File::getPath)
                .forEach(path -> {
                    try {
                        pathTextMap.put(path, FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

        pathTextMap.forEach((path, text) -> {

            String topicPath = path.replace(System.getProperty("user.home"), System.getProperty("user.home") + "/topic");
            String topic = textClassifier.uClassifyText(text, languageDetector.detectLanguage(text)).name();

            try {
                FileUtils.writeStringToFile(new File(topicPath), topic, Charset.forName("UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }


    /**
     * Test uClassify classifier.
     */
    @Test
    public void testUClassifyText() {
        TextClassifier textClassifier = new TextClassifier();

        String text = "2.1.4 virulence factors virulence factors are defined as bacterial products that cause direct " +
                "damage to the host cells. Virulence-associated factors enable the growth or Survival of the pathogen " +
                "in the host and in this way contribute to infection and illness (MAHAN et al. 1996";
        Topic topic = textClassifier.uClassifyText(text, "en");
        System.out.println(topic);

        Assert.assertTrue(topic.equals(Topic.Health));

        text = "SD kann im empfindlichen Bereich durch eine einzelne Entladung eines epileptischen Fokus ausgelöst " +
                "werden (spike triggered SD). In der Regel werden epileptiforme Feldpotentiale während der SD unterdrückt " +
                "und erscheinen in wenigen Minuten wieder.";
        topic = textClassifier.uClassifyText(text, "en");
        System.out.println(topic);
    }
}
