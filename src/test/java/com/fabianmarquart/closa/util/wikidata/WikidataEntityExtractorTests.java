package com.fabianmarquart.closa.util.wikidata;

import com.fabianmarquart.closa.classification.Category;
import com.fabianmarquart.closa.model.WikidataEntity;
import com.google.common.collect.Sets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class WikidataEntityExtractorTests {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";


    @Test
    public void testExtractEntitiesFromSingleWord() {
        String textEn = "Auschwitz";

        List<WikidataEntity> entitiesEn = WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

        System.out.println(entitiesEn);

        Assert.assertTrue(entitiesEn.get(0).getLabel().equals("Oświęcim"));
    }

    @Test
    public void testResolveDemonyme() {
        String demonyme = "Chinese";
        List<WikidataEntity> foundEntities = WikidataEntityExtractor.extractEntitiesFromTextWithoutDisambiguation(demonyme, "en")
                .stream().flatMap(List::stream).collect(Collectors.toList());

        WikidataEntity china = new WikidataEntity("Q29520", "China");
        WikidataEntity peoplesRepublicOfChina = new WikidataEntity("Q148", "People's Republic of China");
        WikidataEntity chinese = new WikidataEntity("Q7850", "Chinese");

        System.out.println(foundEntities);

        Assert.assertTrue(foundEntities.contains(china) || foundEntities.contains(peoplesRepublicOfChina));
        Assert.assertFalse(foundEntities.contains(chinese));
    }

    @Test
    public void testCreativeWorkFiltering() {
        WikidataEntity fiction = new WikidataEntity("Q8253");

        WikidataEntity isna = new WikidataEntity("Q1672387");

        WikidataEntity starWars = new WikidataEntity("Q462");

        WikidataEntity creativeWork = new WikidataEntity("Q17537576");

        Assert.assertFalse(WikidataSparqlUtil.isCreativeWork(isna));
        Assert.assertTrue(WikidataSparqlUtil.isCreativeWork(starWars));
    }

    @Test
    public void testExtractEntitiesFromMultipleWords() {
        List<String> words = Arrays.asList(
                "recess", "appointments",
                "crisis", "debt", "treasury", "income tax", "discuss",
                "Chinese",
                "state visit",
                "Protests", "acquitted",
                "vote", "Maldives", "controversy", "president",
                "deadline", "ISNA", "19", "bombings", "10", "Central African republic", "peacekeeping", "oil price",
                "arrested", "regrets"
                // problematic:
                // "high court", "nuclear"
                // "Asian", "Iraqi"
        );

        List<WikidataEntity> entitiesToFind = new ArrayList<>();
        entitiesToFind.add(new WikidataEntity("Q176274", "recess"));

        entitiesToFind.add(new WikidataEntity("Q24238221", "rendezvous"));
        // entitiesToFind.add(new WikidataEntity("", "high court"));
        entitiesToFind.add(new WikidataEntity("Q381072", "crisis"));
        entitiesToFind.add(new WikidataEntity("Q3196867", "debt"));
        entitiesToFind.add(new WikidataEntity("Q10756188", "treasury"));
        entitiesToFind.add(new WikidataEntity("Q179222", "income tax"));
        entitiesToFind.add(new WikidataEntity("Q3030248", "discussion"));

        entitiesToFind.add(new WikidataEntity("Q29520", "China"));
        entitiesToFind.add(new WikidataEntity("Q148", "People's Republic of China"));

        entitiesToFind.add(new WikidataEntity("Q2324916", "state visit"));

        entitiesToFind.add(new WikidataEntity("Q273120", "protest"));
        entitiesToFind.add(new WikidataEntity("Q1454723", "acquittal"));

        entitiesToFind.add(new WikidataEntity("Q42904171", "vote"));
        entitiesToFind.add(new WikidataEntity("Q826", "Maldives"));
        entitiesToFind.add(new WikidataEntity("Q1255828", "controversy"));
        entitiesToFind.add(new WikidataEntity("Q30461", "president"));

        entitiesToFind.add(new WikidataEntity("Q1465133", "time limit"));
        // entitiesToFind.add(new WikidataEntity("", "nuclear"));
        entitiesToFind.add(new WikidataEntity("Q1672387", "ISNA"));
        entitiesToFind.add(new WikidataEntity("Q39850", "19"));
        entitiesToFind.add(new WikidataEntity("Q891854", "bombing"));
        entitiesToFind.add(new WikidataEntity("Q23806", "10"));
        entitiesToFind.add(new WikidataEntity("Q929", "Central African republic"));
        entitiesToFind.add(new WikidataEntity("Q10954043", "peacekeeping"));
        entitiesToFind.add(new WikidataEntity("Q48", "Asia"));
        entitiesToFind.add(new WikidataEntity("Q297279", "oil price"));

        entitiesToFind.add(new WikidataEntity("Q1403016", "arrest"));
        entitiesToFind.add(new WikidataEntity("Q796", "Iraq"));

        entitiesToFind.add(new WikidataEntity("Q4729246", "regret"));

        for (String word : words) {
            List<WikidataEntity> foundEntities = WikidataEntityExtractor.extractEntitiesFromTextWithoutDisambiguation(word, "en")
                    .stream().flatMap(List::stream).collect(Collectors.toList());
            System.out.println(foundEntities);

            Assert.assertTrue(foundEntities.stream().anyMatch(entitiesToFind::contains));
        }
    }


    @Test
    public void testExtractEntitiesFromComplexWord() {
        String textEn = "US Supreme Court";

        List<WikidataEntity> entitiesEn = WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

        System.out.println(entitiesEn);

        Assert.assertTrue(entitiesEn.get(0).getLabel().equals("Supreme Court of the United States"));
    }

    @Test
    public void testExtractEntitiesFromSentence() {
        String textEn = "A German man has been charged with incitement to hatred after he was pictured with a tattoo apparently of the Nazi death camp at Auschwitz.\n";
        List<WikidataEntity> entitiesEn = WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

        System.out.println(entitiesEn);
    }

    @Test
    public void annotateEntitiesInText() {
        String textEn = "A German man has been charged with incitement to hatred after he was pictured with a tattoo apparently of the Nazi death camp at Auschwitz.";
        String annotatedText = WikidataEntityExtractor.annotateEntitiesInText(textEn, "en", Category.neutral);

        System.out.println(annotatedText);

        String htmlText = Jsoup.parse(annotatedText).text();

        System.out.println(htmlText);

        Assert.assertTrue(textEn.equals(htmlText));
    }

    @Test
    public void testExtractEntitiesFromJapaneseSentence() {
        String textJa = "ドイツ人男性が、ナチスドイツの強制収容所を描いたとされるタトゥーをプールでさらしたとして、憎悪扇動の罪で起訴された。";

        List<WikidataEntity> entitiesJa = WikidataEntityExtractor.extractEntitiesFromText(textJa, "ja");
        System.out.println(entitiesJa);
    }

    @Test
    public void testExtractEntitiesFromEnglishArticle() {
        try {
            String textEn = FileUtils.readFileToString(new File("src/test/resources/org/sciplore/pds/test-bbc/en/35157967/0.txt"), StandardCharsets.UTF_8);
            List<WikidataEntity> entitiesEn = WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

            System.out.println(entitiesEn);

            Assert.assertTrue(entitiesEn.size() > 0);
            Assert.assertTrue(entitiesEn.stream().noneMatch(Objects::isNull));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExtractEntitiesFromChineseArticle() {
        try {
            String textZh = FileUtils.readFileToString(new File("src/test/resources/org/sciplore/pds/ECCE/zh/001052744.ZH.txt"), StandardCharsets.UTF_8);
            List<WikidataEntity> entitiesZh = WikidataEntityExtractor.extractEntitiesFromText(textZh, "zh");

            System.out.println(entitiesZh);

            Assert.assertTrue(entitiesZh.size() > 0);
            Assert.assertTrue(entitiesZh.stream().noneMatch(Objects::isNull));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExtractionRuntime() {
        // base number tokens: 222

        /*
            Parallel for extraction, querying and disambiguation only:
            2^0 length: 20.601 seconds
            2^1 length: 10.558 seconds
            2^2 length: 17.335 seconds
            2^3 length: 33.29 seconds
            2^4 length: 65.056 seconds

            Parallel for all streams:
            2^0 length: 20.351 seconds
            2^1 length: 10.339 seconds
            2^2 length: 14.84 seconds
            2^3 length: 29.323 seconds
            2^4 length: 62.042 seconds

            Single-threaded:
            2^0 length: 21.795 seconds
            2^1 length: 13.473 seconds
            2^2 length: 28.428 seconds
            2^3 length: 58.614 seconds
            2^4 length: 106.6 seconds
         */

        try {
            String textEn = FileUtils.readFileToString(new File("src/test/resources/org/sciplore/pds/test-bbc/en/35157967/0.txt"), StandardCharsets.UTF_8);

            for (int k = 0; k <= 10; k++) {
                long startTime = System.currentTimeMillis();

                WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

                long endTime = System.currentTimeMillis();
                long timeElapsed = endTime - startTime;

                System.out.printf("2^%d length: %s seconds%n", k, timeElapsed / 1000.0);

                textEn = textEn + textEn;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void compareExtractedEntitiesFromChineseEnglishPair() {
        List<String> entitiesZh = Arrays.asList("Q11471", "Q179875", "Q26132734", "Q444184", "Q795052", "Q6256",
                "Q7804", "Q629872", "Q582577", "Q1156854", "Q484605", "Q7804", "Q1162163", "Q30230067", "Q19357421",
                "Q2814650", "Q23387", "Q256817", "Q179289", "Q37172", "Q1951543", "Q21949", "Q8171", "Q25392373",
                "Q569612", "Q192193", "Q117850", "Q7804", "Q7216332", "Q605035", "Q444184", "Q25392373", "Q628523",
                "Q1498321", "Q3026787", "Q11471", "Q1951543", "Q21949", "Q1274479", "Q2132873", "Q11032", "Q93288",
                "Q6256", "Q2872553", "Q1156854", "Q503992", "Q170212", "Q2995644", "Q1971493", "Q37017", "Q21255921",
                "Q29813637", "Q1362683", "Q1156854", "Q1954125", "Q320553", "Q82794", "Q5051643", "Q7188", "Q9530",
                "Q180516", "Q1717246", "Q176982", "Q44364", "Q14565199", "Q691543", "Q41", "Q200", "Q577", "Q145",
                "Q859031", "Q21255605", "Q7685924", "Q8161", "Q211198", "Q646590", "Q1342838", "Q26186900", "Q7804",
                "Q1954125", "Q965330", "Q18907327", "Q1053266", "Q102014", "Q187021", "Q1156854", "Q189833",
                "Q1342838", "Q25392373", "Q3273086", "Q7754", "Q47035128", "Q577", "Q39911", "Q241094", "Q7804",
                "Q712212", "Q188094", "Q639907", "Q862990", "Q481609", "Q7275", "Q176982", "Q444600", "Q419793",
                "Q1156854", "Q7379580", "Q34442", "Q192630", "Q193168", "Q2132873", "Q1101080", "Q29485", "Q1954125",
                "Q7804", "Q11563", "Q859031", "Q881378", "Q11199", "Q178651", "Q183", "Q1193236", "Q7804",
                "Q4697206", "Q19033", "Q190258", "Q1951543", "Q21949", "Q881378", "Q3435731", "Q178790", "Q178790",
                "Q7257", "Q452440", "Q6468673", "Q2880529", "Q1941972", "Q591707", "Q46", "Q473750");

        WikidataDumpUtil.printEntities(entitiesZh, "en");
    }

    @Test
    public void testExtractNumbersOnlyNaturalNumbers() {
        String text1 = "3 killed, 4 injured in Los Angeles shootings";
        String text2 = "Five killed in Saudi Arabia shooting";

        WikidataEntity three = new WikidataEntity("Q201", "3");
        WikidataEntity five = new WikidataEntity("Q203", "5");

        Assert.assertTrue(WikidataEntityExtractor.extractEntitiesFromText(text1, "en").contains(three));
        Assert.assertTrue(WikidataEntityExtractor.extractEntitiesFromText(text2, "en").contains(five));
    }

    @Test
    public void testExtractEntitiesWithoutCreativeWorks() {
        String text = "EU Ministers of Employment and Social policy will discuss how to boost employment, integrate social and economic policies";

        WikidataEntity theMinisters = new WikidataEntity("Q7751572", "The Ministers");

        Assert.assertFalse(WikidataEntityExtractor.extractEntitiesFromText(text, "en").contains(theMinisters));
    }

    @Test
    public void testExtractEntitiesWithoutGenes() {
        String text = "Israeli forces detain Palestinian MP in Hebron";

        WikidataEntity mp = new WikidataEntity("Q29732832", "mp");

        Assert.assertFalse(WikidataEntityExtractor.extractEntitiesFromText(text, "en").contains(mp));
    }

    @Test
    public void testExtractEnglishEntitiesInChineseText() {
        String text = "当今年7月菲利普•法尔科(Philip Falcone)同意支付1800万美元以了";

        List<WikidataEntity> entities = WikidataEntityExtractor.extractEntitiesFromText(text, "zh");

        System.out.println(entities);
    }

    @Test
    public void testGetSublistsOfSize() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);

        List<List<List<Integer>>> result = WikidataEntityExtractor.getSublistsOfSize(list, 3);
        System.out.println(result);

        Assert.assertTrue(result.equals(Arrays.asList(
                Arrays.asList(Arrays.asList(1, 2, 3),
                        Arrays.asList(1, 2), Collections.singletonList(1)),
                Arrays.asList(Arrays.asList(2, 3, 4),
                        Arrays.asList(2, 3), Arrays.asList(3, 4), Collections.singletonList(2), Collections.singletonList(3), Collections.singletonList(4)))));
    }

    @Test
    public void testGetSublistsOfSmallList() {
        List<Integer> list1 = Collections.singletonList(1);

        List<List<List<Integer>>> result1 = WikidataEntityExtractor.getSublistsOfSize(list1, 3);
        System.out.println(result1);

        Assert.assertTrue(result1.equals(Collections.singletonList(Collections.singletonList(list1))));

        List<Integer> list2 = Arrays.asList(1, 2);

        List<List<List<Integer>>> result2 = WikidataEntityExtractor.getSublistsOfSize(list2, 3);
        System.out.println(result2);

        Assert.assertTrue(result2.equals(Collections.singletonList(Arrays.asList(list2, Collections.singletonList(1), Collections.singletonList(2)))));
    }


    @Test
    public void testVerbHandling() {
        String text = "Plane lands on airport.";
        Assert.assertTrue(WikidataEntityExtractor.extractEntitiesFromText(text, "en")
                .contains(new WikidataEntity("Q844947", "landing")));
    }

    @Test
    public void testNumberHandling() {
        String text = "9";
        System.out.println(WikidataEntityExtractor.extractEntitiesFromText(text, "en"));
    }

    @Test
    public void testExtractEntitiesFromDocument() {
        String path = "src/test/resources/org/sciplore/pds/test-bbc/en/38482208/0.txt";

        try {
            String text = FileUtils.readFileToString(new File(path), StandardCharsets.UTF_8);
            WikidataEntityExtractor.extractEntitiesFromText(text, "en");
        } catch (IOException e) {
            Assert.fail();
            e.printStackTrace();
        }
    }

    @Test
    public void testExtractEntitiesFromSpanishText() {
        String textEs = "La enmienda n.º 7 propone ciertos cambios en las referencias a los párrafos.";
        String textEn = "Amendment 7 changes in references to paragraphs.";

        List<WikidataEntity> entities = WikidataEntityExtractor.extractEntitiesFromText(textEs, "es");

        System.out.println(entities);
    }

    /**
     * DumpUtil:
     * <p>
     * Precision  = 0.6372093023255814
     * Recall     = 0.6061946902654868
     * F-Measure  = 0.6213151927437641
     * <p>
     * SparqlUtil:
     * <p>
     * Precision  = 0.6089108910891089
     * Recall     = 0.5442477876106194
     * F-Measure  = 0.5747663551401868
     */
    @Test
    public void testExtractEntitiesFromManuallyAnnotatedText() {
        File csvFile = new File("src/test/resources/org/sciplore/pds/test-wikidata/sts-hdl2016.csv");

        int relevantElements = 0;
        int selectedElements = 0;
        int truePositives = 0;
        int falsePositives = 0;

        try (Reader reader = new FileReader(csvFile)) {

            CSVFormat format = CSVFormat.DEFAULT;
            List<CSVRecord> records = CSVParser.parse(reader, format).getRecords();

            for (int i = 0; i < records.size(); i++) {
                System.out.println("////////////////// Pair " + i + " //////////////////");
                CSVRecord record = records.get(i);

                // get information from csv and from SPARQL
                String sentence1 = record.get(5);
                System.out.println("Sentence 1: " + sentence1);

                Set<WikidataEntity> manualEntities1 = Arrays.stream(record.get(9).split(" "))
                        .map(WikidataSparqlUtil::getEntityById)
                        .collect(Collectors.toSet());

                Set<WikidataEntity> extractedEntities1 = new HashSet<>(WikidataEntityExtractor.extractEntitiesFromText(sentence1, "en"));
                System.out.println("Manual:     " + Sets.intersection(manualEntities1, extractedEntities1)
                        + ANSI_BLUE + Sets.difference(manualEntities1, extractedEntities1) + ANSI_RESET);
                System.out.println("Extracted:  " + ANSI_GREEN + Sets.intersection(manualEntities1, extractedEntities1)
                        + ANSI_RED + Sets.difference(extractedEntities1, manualEntities1) + ANSI_RESET);


                String sentence2 = record.get(6);
                System.out.println("Sentence 2: " + sentence2);

                Set<WikidataEntity> manualEntities2 = Arrays.stream(record.get(10).split(" "))
                        .map(WikidataSparqlUtil::getEntityById)
                        .collect(Collectors.toSet());

                Set<WikidataEntity> extractedEntities2 = new HashSet<>(WikidataEntityExtractor.extractEntitiesFromText(sentence2, "en"));

                System.out.println("Manual:     " + Sets.intersection(manualEntities2, extractedEntities2)
                        + ANSI_BLUE + Sets.difference(manualEntities2, extractedEntities2) + ANSI_RESET);
                System.out.println("Extracted:  " + ANSI_GREEN + Sets.intersection(manualEntities2, extractedEntities2)
                        + ANSI_RED + Sets.difference(extractedEntities2, manualEntities2) + ANSI_RESET);


                // update values
                relevantElements += manualEntities1.size();
                relevantElements += manualEntities2.size();

                selectedElements += extractedEntities1.size();
                selectedElements += extractedEntities2.size();

                truePositives += Sets.intersection(manualEntities1, extractedEntities1).size();
                truePositives += Sets.intersection(manualEntities2, extractedEntities2).size();

                falsePositives += Sets.difference(extractedEntities1, manualEntities1).size();
                falsePositives += Sets.difference(extractedEntities2, manualEntities2).size();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        double precision = (double) truePositives / (selectedElements);
        double recall = (double) truePositives / (relevantElements);
        double fMeasure = 2 * (precision * recall) / (precision + recall);

        System.out.println("Precision  = " + precision);
        System.out.println("Recall     = " + recall);
        System.out.println("F-Measure  = " + fMeasure);

    }





}
