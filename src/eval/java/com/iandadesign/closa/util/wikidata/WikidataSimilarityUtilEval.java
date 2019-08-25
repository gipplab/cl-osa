package com.iandadesign.closa.util.wikidata;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.WikidataEntity;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

import static com.iandadesign.closa.util.wikidata.WikidataSimilarityUtil.SimilarityFunction;

public class WikidataSimilarityUtilEval {


    @Test
    public void ontologyEnhancedCosineEval() {
        // retrieve manually annotated pairs of roughly equivalent wikidata entity documents
        File csvFile = new File("src/eval/resources/com/iandadesign/closa/util/wikidata/sts-hdl2016.csv");

        Map<String, List<String>> suspiciousDocuments = new HashMap<>();
        Map<String, List<String>> candidateDocuments = new HashMap<>();

        try (Reader reader = new FileReader(csvFile)) {

            CSVFormat format = CSVFormat.DEFAULT;
            List<CSVRecord> records = CSVParser.parse(reader, format).getRecords();

            for (CSVRecord record : records) {
                String id = record.get(3).trim();
                if (!id.equals("603")) {
                    continue;
                }

                List<String> suspiciousEntities = Arrays.stream(record.get(9).split(" "))
                        .collect(Collectors.toList());

                suspiciousDocuments.put(record.get(3), suspiciousEntities);

                List<String> candidateEntities = Arrays.stream(record.get(10).split(" "))
                        .collect(Collectors.toList());

                candidateDocuments.put(record.get(3), candidateEntities);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // initialize retrieval measures
        int relevantElements = suspiciousDocuments.size();
        int selectedElements = 0;
        int truePositives = 0;
        int falsePositives = 0;


        Map<String, Map<String, Double>> candidateScoreMap = new OntologyBasedSimilarityAnalysis(new LanguageDetector(), new TextClassifier())
                .performEnhancedCosineSimilarityAnalysis(suspiciousDocuments, candidateDocuments);

        for (Map.Entry<String, Map<String, Double>> candidateScoreEntry : candidateScoreMap.entrySet()) {
            String suspiciousId = candidateScoreEntry.getKey();
            String candidateId = candidateScoreEntry.getValue().entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .get()
                    .getKey();

            if (suspiciousId.equals(candidateId)) {
                truePositives += 1;
            } else {
                falsePositives += 1;
            }
            selectedElements += 1;
        }


        double precision = (double) truePositives / (selectedElements);
        double recall = (double) truePositives / (relevantElements);
        double fMeasure = 2 * (precision * recall) / (precision + recall);

        System.out.println("Function Ontology-enhanced cosine");
        System.out.println();
        System.out.println("Precision  = " + precision);
        System.out.println("Recall     = " + recall);
        System.out.println("F-Measure  = " + fMeasure);
    }

    @Test
    public void RetrievalForManuallyAnnotatedTextEval() {
        /*
            C-Dist:
                Precision  = 0.6818181818181818
                Recall     = 0.6818181818181818
                F-Measure  = 0.6818181818181818

            Wu and Palmer:
                Precision  = 0.4090909090909091
                Recall     = 0.4090909090909091
                F-Measure  = 0.4090909090909091

            Li et al.:
                Precision  = 0.9545454545454546
                Recall     = 0.9545454545454546
                F-Measure  = 0.9545454545454546

            Leacock and Chodorow:
                Precision  = 0.09090909090909091
                Recall     = 0.09090909090909091
                F-Measure  = 0.09090909090909091

            Ngyuen and Al Mubaid:
                Precision  = 0.5909090909090909
                Recall     = 0.5909090909090909
                F-Measure  = 0.5909090909090909

            Ontology-Enhanced cosine (1/((1+dist(v,w))^2))
                Precision  = 0.9545454545454546
                Recall     = 0.9545454545454546
                F-Measure  = 0.9545454545454546
        */

        // retrieve manually annotated pairs of roughly equivalent wikidata entity documents
        File csvFile = new File("src/eval/resources/com/iandadesign/closa/util/wikidata/sts-hdl2016.csv");

        Map<String, List<WikidataEntity>> suspiciousDocuments = new HashMap<>();
        Map<String, List<WikidataEntity>> candidateDocuments = new HashMap<>();

        try (Reader reader = new FileReader(csvFile)) {
            CSVFormat format = CSVFormat.DEFAULT;
            List<CSVRecord> records = CSVParser.parse(reader, format).getRecords();

            for (CSVRecord record : records) {
                List<WikidataEntity> suspiciousEntities = Arrays.stream(record.get(9).split(" "))
                        .map(WikidataDumpUtil::getEntityById)
                        .collect(Collectors.toList());
                suspiciousDocuments.put(record.get(3), suspiciousEntities);

                List<WikidataEntity> candidateEntities = Arrays.stream(record.get(10).split(" "))
                        .map(WikidataDumpUtil::getEntityById)
                        .collect(Collectors.toList());
                candidateDocuments.put(record.get(3), candidateEntities);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // evaluate all functions
        List<SimilarityFunction> similarityFunctions = new ArrayList<>();

        similarityFunctions.add(SimilarityFunction.COSINE);
        similarityFunctions.add(SimilarityFunction.ENHANCED_COSINE);

        similarityFunctions.add(SimilarityFunction.C_DIST);
        similarityFunctions.add(SimilarityFunction.WU_PALMER);
        similarityFunctions.add(SimilarityFunction.LI);
        similarityFunctions.add(SimilarityFunction.LEACKOCK_CHODOROW);
        similarityFunctions.add(SimilarityFunction.NGUYEN_AL_MUBAID);

        similarityFunctions.add(SimilarityFunction.RESNIK);
        similarityFunctions.add(SimilarityFunction.LIN);

    }

}
