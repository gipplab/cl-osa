package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.model.Dictionary;
import com.iandadesign.closa.model.WikidataEntity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

import static com.iandadesign.closa.util.wikidata.WikidataSimilarityUtil.*;

public class WikidataSimilarityUtilTest {

    @Test
    public void testCosineSimilarity() {
        List<WikidataEntity> firstEntities = WikidataDumpUtil.getRandomEntities(20);
        List<WikidataEntity> secondEntities = new ArrayList<>(firstEntities);

        firstEntities.addAll(WikidataDumpUtil.getRandomEntities(20));
        secondEntities.addAll(WikidataDumpUtil.getRandomEntities(20));

        System.out.println(WikidataSimilarityUtil.cosineSimilarity(firstEntities, secondEntities));
        System.out.println(Dictionary.cosineSimilarity(firstEntities, secondEntities));
    }

    @Test
    public void testSimilarityWuPalmer() {
        WikidataEntity tree = new WikidataEntity("Q10884", "tree");
        WikidataEntity carnivorousPlant = new WikidataEntity("Q18240", "carnivorous plant");

        double sim = similarityWuPalmer(tree, carnivorousPlant);

        System.out.println(sim);
        Assert.assertEquals(sim, 2.0 / 3.0);

        WikidataEntity hill = new WikidataEntity("Q54050", "hill");
        WikidataEntity coast = new WikidataEntity("Q93352", "coast");

        double sim2 = similarityWuPalmer(hill, coast);

        Assert.assertTrue(sim2 == 8.0 / 13.0 || sim2 == 4.0 / 7.0); // first ist SPARQL, second is dump
    }

    @Test
    public void testSimilaritiesForRelatedConcepts() {
        /*
            Similarity Li (sum): 0.006905238518828958
            Similarity Li (max): 0.05085608839976804
         */
        String text1 = "The Rolling Stones with the participation of Roger Daltrey opened the concerts' season in Trafalgar Square";
        String text2 = "The bands headed by Mick Jagger with the leader of The Who played in London last week";

        List<WikidataEntity> entities1 = WikidataEntityExtractor.extractEntitiesFromText(text1, "en");
        List<WikidataEntity> entities2 = WikidataEntityExtractor.extractEntitiesFromText(text2, "en");

        System.out.println("Similarity Li:");
        System.out.println(getDocumentSimilarity(entities1, entities2, SimilarityFunction.LI));

        System.out.println("Enhanced cosine:");
        System.out.println(ontologyEnhancedCosineSimilarity(entities1, entities2));
    }

    @Test
    public void testComputeSimilaritiesForManuallyAnnotatedText() {
        // retrieve manually annotated pairs of roughly equivalent wikidata entity documents
        File csvFile = new File("src/test/resources/org/sciplore/pds/test-wikidata/sts-hdl2016.csv");

        Map<String, Pair<List<WikidataEntity>, List<WikidataEntity>>> documents = new HashMap<>();

        try (Reader reader = new FileReader(csvFile)) {

            CSVFormat format = CSVFormat.DEFAULT;
            List<CSVRecord> records = CSVParser.parse(reader, format).getRecords();

            for (CSVRecord record : records) {

                List<WikidataEntity> suspiciousEntities = Arrays.stream(record.get(9).split(" "))
                        .map(WikidataEntity::new)
                        .collect(Collectors.toList());

                List<WikidataEntity> candidateEntities = Arrays.stream(record.get(10).split(" "))
                        .map(WikidataEntity::new)
                        .collect(Collectors.toList());

                documents.put(record.get(3), new MutablePair<>(suspiciousEntities, candidateEntities));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, Pair<List<WikidataEntity>, List<WikidataEntity>>> document : documents.entrySet()) {
            System.out.println(getDocumentSimilarity(
                    document.getValue().getKey(),
                    document.getValue().getValue(),
                    SimilarityFunction.LI));
        }
    }

    @Test
    public void testDistanceRuntime() {
        List<WikidataEntity> testEntities = WikidataDumpUtil.getRandomEntities(1000);

        for (int k = 1; k < testEntities.size(); k++) {
            long startTime = System.currentTimeMillis();

            for (int i = 0; i <= k; i++) {
                for (int j = 0; j <= k; j++) {
                    WikidataDumpUtil.distance(testEntities.get(i), testEntities.get(j));
                }
            }

            long endTime = System.currentTimeMillis();
            long timeElapsed = endTime - startTime;

            System.out.printf("%d distances: %s seconds%n", k, timeElapsed / 1000.0);
        }
    }


    @Test
    public void secoIntrinsicInformationContent() {
        WikidataEntity hill = new WikidataEntity("Q54050", "hill");
        WikidataEntity coast = new WikidataEntity("Q93352", "coast");

        Assert.assertTrue(WikidataSimilarityUtil.secoIntrinsicInformationContent(hill) == 0.2803218544469863);
        Assert.assertTrue(WikidataSimilarityUtil.secoIntrinsicInformationContent(coast) == 0.4428415349722683);
    }

    @Test
    public void zhouIntrinsicInformationContent() {
        WikidataEntity hill = new WikidataEntity("Q54050", "hill");
        WikidataEntity coast = new WikidataEntity("Q93352", "coast");

        System.out.println(WikidataSimilarityUtil.zhouIntrinsicInformationContent(hill, 0.5));
        System.out.println(WikidataSimilarityUtil.zhouIntrinsicInformationContent(coast, 0.5));
    }
}
