package com.fabianmarquart.closa.model;

import com.google.common.collect.Ordering;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.SparseRealVector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Fabian Marquart on 2017/07/15/
 */
public class Dictionary<T> {

    private static UnivariateFunction booleanWeighing = v -> {
        if (v > 0.0) return 1.0;
        return 0.0;
    };
    private static UnivariateFunction logNormalization = v -> {
        if (v == 0.0) return 0.0;
        return 1.0 + Math.log10(v);
    };
    private static UnivariateFunction augmentedWeighingAddition = v -> {
        if (v == 0.0) return 0.0;
        return 0.6 + v;
    };
    private Double numberOfDocuments;
    private Set<T> terms;
    private Map<T, Map<String, Integer>> dictionary;


    public Dictionary(Map<String, List<T>> documentIdTokensMap) {
        numberOfDocuments = 0.0;
        terms = new HashSet<>();
        dictionary = createDictionary(documentIdTokensMap);
    }

    /**
     * Cosine similarity computation for two lists of tokens.
     *
     * @param tokens1 first tokens list
     * @param tokens2 second tokens list
     * @return similarity value.
     */
    public static <T> double cosineSimilarity(List<T> tokens1, List<T> tokens2) {
        List<SparseRealVector> vectors = createVectorsFromDocuments(tokens1, tokens2);
        return cosineSimilarity(vectors.get(0), vectors.get(1));
    }

    /**
     * Builds vectors from variable number of token lists.
     * @param tokenLists variable number of token lists.
     * @param <T> type
     * @return vectors
     */
    @SafeVarargs
    public static <T> List<SparseRealVector> createVectorsFromDocuments(List<T>... tokenLists) {
        Set<T> allTokens = new HashSet<>();
        Arrays.stream(tokenLists).forEach(allTokens::addAll);
        int dimension = allTokens.size();
        List<T> allTokensList = new ArrayList<>(allTokens);

        List<SparseRealVector> vectors = new ArrayList<>();

        Arrays.stream(tokenLists).forEach(tokenList -> {
            SparseRealVector vector = new OpenMapRealVector(dimension);

            tokenList.forEach(token -> {
                int index = allTokensList.indexOf(token);
                vector.addToEntry(index, 1.0);
            });

            vectors.add(vector);
        });

        return vectors;
    }


    /**
     * Performs boolean weighing on a vector.
     * @param vector givne vector.
     * @return return booleanly weighted vector.
     */
    public static SparseRealVector booleanWeighing(SparseRealVector vector) {
        SparseRealVector booleanVector = new OpenMapRealVector(vector.getDimension());
        booleanVector = (OpenMapRealVector)
                booleanVector.add(vector.mapToSelf(booleanWeighing));

        return booleanVector;
    }


    /**
     * Cosine similarity computation for two vectors.
     *
     * @param vector1 first vector
     * @param vector2 second vector
     * @return similarity value.
     */
    public static double cosineSimilarity(SparseRealVector vector1, SparseRealVector vector2) {
        if (vector1.getDimension() != vector2.getDimension()) {
            throw new IllegalArgumentException("Vector dimensions need to agree.");
        }

        return vector1.dotProduct(vector2) / (vector1.getNorm() * vector2.getNorm());
    }

    /**
     * Creates an inverted index dictionary.
     *
     * @param documentIdTokensMap document ids mapped to list of tokens.
     * @return the dictionary.
     */
    private Map<T, Map<String, Integer>> createDictionary(Map<String, List<T>> documentIdTokensMap) {
        Map<T, Map<String, Integer>> dictionary = new HashMap<>();
        numberOfDocuments = (double) documentIdTokensMap.size();

        documentIdTokensMap.forEach((id, tokenList) -> tokenList.forEach(token -> {
            if (dictionary.containsKey(token)) {
                if (dictionary.get(token).containsKey(id)) {
                    Integer oldCount = dictionary.get(token).get(id);
                    dictionary.get(token).put(id, oldCount + 1);
                } else {
                    dictionary.get(token).put(id, 1);
                }
            } else {
                Map<String, Integer> documentCountMap = new HashMap<>();
                documentCountMap.put(id, 1);
                dictionary.put(token, documentCountMap);
                terms.add(token);
            }
        }));

        return dictionary;
    }

    /**
     * Queries the inverted index dictionary.
     *
     * @param queryTerms query
     * @return the document ids - score map.
     */
    public Map<String, Double> query(final List<T> queryTerms) {

        if (queryTerms.isEmpty()) {
            return new HashMap<>();
        }

        // 1 vector building
        Map<String, SparseRealVector> docIdVectorMap = new HashMap<>();

        List<T> termList = new ArrayList<>(terms);
        termList.addAll(new ArrayList<>(new HashSet<>(queryTerms)));
        int dimension = termList.size();

        // 1.1 map term -> (docId -> freq)
        //     to docId -> term vector
        dictionary.forEach((term, value) -> {
            Integer conceptIndex = termList.indexOf(term);
            value.forEach((docId, freq) -> {
                if (docIdVectorMap.containsKey(docId)) {
                    docIdVectorMap.get(docId).addToEntry(conceptIndex, freq);
                } else {
                    SparseRealVector documentVector = new OpenMapRealVector(dimension);
                    documentVector.setEntry(conceptIndex, freq);
                    docIdVectorMap.put(docId, documentVector);
                }
            });
        });

        // 1.2 map query to query vector
        SparseRealVector queryVector = new OpenMapRealVector(dimension);

        for (T queryTerm : queryTerms) {
            Integer conceptIndex = termList.indexOf(queryTerm);
            queryVector.setEntry(conceptIndex, queryVector.getEntry(conceptIndex) + 1.0);
        }


        // 2   weighing

        // 2.2   get and weigh the document frequencies
        // 2.2.1 binary frequencies
        SparseRealVector documentFrequencyVector = new OpenMapRealVector(dimension);
        for (SparseRealVector vector : docIdVectorMap.values()) {
            documentFrequencyVector = (OpenMapRealVector)
                    documentFrequencyVector.add(vector.mapToSelf(booleanWeighing));

        }

        // 2.2.2 idf: every value
        SparseRealVector inverseDocumentFrequencyVector = (SparseRealVector) documentFrequencyVector
                .mapDivideToSelf(numberOfDocuments)
                .mapAdd(1.0)
                .mapToSelf(Math::log10);

        double a = 0.6;

        // 2.3 weigh the document vectors
        for (Map.Entry<String, SparseRealVector> entry : docIdVectorMap.entrySet()) {
            int documentLength = 0;
            double maxTermFrequency = entry.getValue().getMaxValue();
            for (int i = 0; i < dimension; i++) {
                if (entry.getValue().getEntry(i) > 0) {
                    documentLength += 1;
                }
            }

            entry.setValue((SparseRealVector) entry.getValue()
                    .map(booleanWeighing));
            // .mapMultiply(1 - a)
            // .mapDivide(maxTermFrequency)
            // .map(augmentedWeightingAddition)
            //.mapDivide(documentLength)
            // .mapToSelf(logNormalization)
            //.ebeMultiply(inverseDocumentFrequencyVector));
        }

        // 2.4 weigh the query vector
        double maxQueryTermFrequency = queryVector.getMaxValue();

        queryVector = (OpenMapRealVector) queryVector
                .map(booleanWeighing);
        // .mapMultiply(1 - a)
        //.mapDivide(maxQueryTermFrequency)
        //.map(augmentedWeightingAddition)
        //.mapDivide(queryTerms.size())
        //.mapToSelf(logNormalization)
        //.ebeMultiply(inverseDocumentFrequencyVector);


        // 3 compare
        Map<String, Double> documentIdScoreMap = new HashMap<>();

        for (Map.Entry<String, SparseRealVector> docIdVectorEntry : docIdVectorMap.entrySet()) {
            String docId = docIdVectorEntry.getKey();
            SparseRealVector documentVector = docIdVectorEntry.getValue();

            // 3.1 get queryVector and documentVector length
            Double queryVectorLength = 0.0;
            Double documentVectorLength = 0.0;
            for (int i = 0; i < dimension; i++) {
                queryVectorLength += Math.pow(queryVector.getEntry(i), 2);
                documentVectorLength += Math.pow(documentVector.getEntry(i), 2);
            }
            queryVectorLength = Math.sqrt(queryVectorLength);
            documentVectorLength = Math.sqrt(documentVectorLength);

            // 3.2 apply cosine normalization
            // documentVector.mapDivide(documentVectorLength);
            // queryVector.mapDivide(queryVectorLength);

            // 3.3 calculate the score
            Double score = 0.0;
            for (int i = 0; i < dimension; i++) {
                score += (queryVector.getEntry(i) / queryVectorLength)
                        * (documentVector.getEntry(i) / documentVectorLength);
            }

            // 3.4 put the score
            documentIdScoreMap.put(docId, score);
            // System.out.println(docId + ", " + score);
        }

        List<Double> highestScores = Ordering.natural().greatestOf(documentIdScoreMap.values(), 1);


        return documentIdScoreMap;
    }


    /**
     * Boolean query to the inverted index dictionary.
     *
     * @param queryTerms and query
     * @return the matching document ids.
     */
    public List<String> booleanQuery(final List<Token> queryTerms, double threshold) {

        if (queryTerms.isEmpty()) {
            return new ArrayList<>();
        }

        // map term -> docId -> freq
        return dictionary.entrySet().stream()
                .filter(entry -> queryTerms.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap( // to docId -> query term frequency sum
                        Map.Entry::getKey,   // docId
                        Map.Entry::getValue,
                        Integer::sum))
                .entrySet() // to list
                .stream()
                .filter(entry -> entry.getValue() > threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

}
