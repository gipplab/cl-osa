package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.model.WikidataEntity;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class WikidataEntityExtractorTests {

    @Test
    public void testExtractEntitiesFromSingleWord() {
        String textEn = "Auschwitz";

        List<WikidataEntity> entitiesEn = WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

        System.out.println(entitiesEn);

        Assertions.assertTrue(entitiesEn.get(0).getLabel().equals("Oświęcim"));
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

        Assertions.assertTrue(foundEntities.contains(china) || foundEntities.contains(peoplesRepublicOfChina));
        Assertions.assertFalse(foundEntities.contains(chinese));
    }

    @Test
    public void testCreativeWorkFiltering() {
        WikidataEntity fiction = new WikidataEntity("Q8253");

        WikidataEntity isna = new WikidataEntity("Q1672387");

        WikidataEntity starWars = new WikidataEntity("Q462");

        WikidataEntity creativeWork = new WikidataEntity("Q17537576");

        Assertions.assertFalse(WikidataSparqlUtil.isCreativeWork(isna));
        Assertions.assertTrue(WikidataSparqlUtil.isCreativeWork(starWars));
    }


    @Test
    public void testExtractEntitiesFromComplexWord() {
        String textEn = "US Supreme Court";

        List<WikidataEntity> entitiesEn = WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

        System.out.println(entitiesEn);

        Assertions.assertTrue(entitiesEn.get(0).getLabel().equals("Supreme Court of the United States"));
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

        Assertions.assertTrue(textEn.equals(htmlText));
    }

    @Test
    public void testExtractEntitiesFromJapaneseSentence() {
        String textJa = "ドイツ人男性が、ナチスドイツの強制収容所を描いたとされるタトゥーをプールでさらしたとして、憎悪扇動の罪で起訴された。";

        List<WikidataEntity> entitiesJa = WikidataEntityExtractor.extractEntitiesFromText(textJa, "ja");
        System.out.println(entitiesJa);
    }

    @Test
    public void testExtractEntitiesFromChineseSentence() {
        String textZh = "有些事预料得到，但并不会减弱其破坏力。\n" +
                "巴拉克•奥巴马(Barack Obama)决定对中国输美轮胎开征特保关税，这似乎表明，当被迫在贸易政策上作出决断时，他一般都会采取错误的立场。\n" +
                "为什么这是一个糟糕的决定？ 论据多得很。\n" +
                "首先，它涉及一个本身不想得到保护的产业。";

        List<WikidataEntity> entitiesZh = WikidataEntityExtractor.extractEntitiesFromText(textZh, "zh");
        System.out.println(entitiesZh);
    }

    @Test
    public void testExtractEntitiesFromEnglishArticle() {
        try {
            String textEn = FileUtils.readFileToString(new File("src/test/resources/org/sciplore/pds/test-bbc/en/35157967/0.txt"), StandardCharsets.UTF_8);
            List<WikidataEntity> entitiesEn = WikidataEntityExtractor.extractEntitiesFromText(textEn, "en");

            System.out.println(entitiesEn);

            Assertions.assertTrue(entitiesEn.size() > 0);
            Assertions.assertTrue(entitiesEn.stream().noneMatch(Objects::isNull));
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

            Assertions.assertTrue(entitiesZh.size() > 0);
            Assertions.assertTrue(entitiesZh.stream().noneMatch(Objects::isNull));
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

        Assertions.assertTrue(WikidataEntityExtractor.extractEntitiesFromText(text1, "en").contains(three));
        Assertions.assertTrue(WikidataEntityExtractor.extractEntitiesFromText(text2, "en").contains(five));
    }

    @Test
    public void testExtractEntitiesWithoutCreativeWorks() {
        String text = "EU Ministers of Employment and Social policy will discuss how to boost employment, integrate social and economic policies";

        WikidataEntity theMinisters = new WikidataEntity("Q7751572", "The Ministers");

        Assertions.assertFalse(WikidataEntityExtractor.extractEntitiesFromText(text, "en").contains(theMinisters));
    }

    @Test
    public void testExtractEntitiesWithoutGenes() {
        String text = "Israeli forces detain Palestinian MP in Hebron";

        WikidataEntity mp = new WikidataEntity("Q29732832", "mp");

        Assertions.assertFalse(WikidataEntityExtractor.extractEntitiesFromText(text, "en").contains(mp));
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

        Assertions.assertTrue(result.equals(Arrays.asList(
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

        Assertions.assertTrue(result1.equals(Collections.singletonList(Collections.singletonList(list1))));

        List<Integer> list2 = Arrays.asList(1, 2);

        List<List<List<Integer>>> result2 = WikidataEntityExtractor.getSublistsOfSize(list2, 3);
        System.out.println(result2);

        Assertions.assertTrue(result2.equals(Collections.singletonList(Arrays.asList(list2, Collections.singletonList(1), Collections.singletonList(2)))));
    }


    @Test
    public void testVerbHandling() {
        String text = "Plane lands on airport.";
        Assertions.assertTrue(WikidataEntityExtractor.extractEntitiesFromText(text, "en")
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
            Assertions.fail();
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


}
