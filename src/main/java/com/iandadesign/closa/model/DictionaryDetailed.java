package com.iandadesign.closa.model;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SparseRealVector;

import java.util.*;

/**
 *  This is the same like dictionary, but modified for detailed analysis with some additional features.
 *  created modified by Johannes Stegmüller 21.10.2020
 *
 */
public class DictionaryDetailed<T> {

    private static UnivariateFunction booleanWeighing = v -> {
        if (v > 0.0) return 1.0;
        return 0.0;
    };

    private Set<T> terms;
    private Map<T, Map<String, Integer>> dictionary;


    public DictionaryDetailed(Map<String, List<String>> documentIdTokensMap) {
        terms = new HashSet<>();
        dictionary = createDictionary(documentIdTokensMap);
    }

    /**
     * Cosine similarity computation for two lists of tokens.
     *
     * @param tokens1 first tokens list
     * @param tokens2 second tokens list
     * @param <T>     type parameter
     * @return similarity value.
     */
    public static <T> double cosineSimilarity(List<String> tokens1, List<String> tokens2) {
        List allTokensList = new ArrayList<>();

        List<SparseRealVector> vectors = createVectorsFromDocuments(allTokensList, tokens1, tokens2);
        return cosineSimilarity(vectors.get(0), vectors.get(1));
    }


    public static double dotProduct(List matchingIndices, RealVector vector1, RealVector vector2) {

        double d = 0.0D;
        int n = vector1.getDimension();

        for(int i = 0; i < n; ++i) {
            double increment = vector1.getEntry(i) * vector2.getEntry(i);
            if(increment > 0){
                matchingIndices.add(i);
            }
            d += increment;
        }

        return d;
    }

    public static Map<Double, List<String>> getMatchesCount(List<String> tokens1, List<String> tokens2, boolean getAccurateStartStop){
        List allTokensList = new ArrayList<>(); // this is passed by ref for remapping
        List matchingIndices = new ArrayList();

        List<SparseRealVector> vectors = createVectorsFromDocuments(allTokensList, tokens1, tokens2);
        SparseRealVector vector1 = vectors.get(0);
        SparseRealVector vector2 = vectors.get(1);
        if (vector1.getDimension() != vector2.getDimension()) {
            throw new IllegalArgumentException("Vector dimensions need to agree.");
        }

        double matches = dotProduct(matchingIndices, vector1,vector2);

        List matchingTokens = new ArrayList();
        matchingIndices.stream().forEach(index -> {
            matchingTokens.add(allTokensList.get((Integer) index));
        });

        Map<Double, List<String>> retMap = new HashMap<>();
        retMap.put(matches, matchingTokens);
        return retMap;

    }
    /**
     * Builds vectors from variable number of token lists.
     *
     * @param tokenLists variable number of token lists.
     * @param <T>        type
     * @return vectors
     */
    @SafeVarargs
    public static <T> List<SparseRealVector> createVectorsFromDocuments(List<T> allTokensList, List<String>... tokenLists) {
        Set<String> allTokens = new HashSet<>();
        Arrays.stream(tokenLists).forEach(allTokens::addAll);
        int dimension = allTokens.size();
        List allTokensListIn = new ArrayList<>(allTokens);

        List<SparseRealVector> vectors = new ArrayList<>();

        Arrays.stream(tokenLists).forEach(tokenList -> {
            SparseRealVector vector = new OpenMapRealVector(dimension);

            tokenList.forEach(token -> {
                int index = allTokensListIn.indexOf(token);
                vector.addToEntry(index, 1.0);
            });

            vectors.add(vector);
        });
        allTokensList.addAll(allTokensListIn);
        return vectors;
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
        double matches = vector1.dotProduct(vector2);
        return vector1.dotProduct(vector2) / (vector1.getNorm() * vector2.getNorm());
    }


    /**
     * Creates an inverted index dictionary.
     *
     * @param documentIdTokensMap document ids mapped to list of tokens.
     * @return the dictionary.
     */
    private Map<T, Map<String, Integer>> createDictionary(Map<String, List<String>> documentIdTokensMap) {
        Map<T, Map<String, Integer>> dictionary = new HashMap<>();

        documentIdTokensMap.forEach((id, tokenList) -> tokenList.forEach(token -> {
            //String wikidataId = token.getWikidataEntityId();
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
                dictionary.put((T) token, documentCountMap);
                terms.add((T) token);
            }
        }));

        return dictionary;
    }

    /**
     * Queries the inverted index dictionary.
     *
     * @param queryTerms sendQuery
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
            int conceptIndex = termList.indexOf(term);
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

        // 1.2 map sendQuery to sendQuery vector (vector has dimension of terms)
        SparseRealVector queryVector = new OpenMapRealVector(dimension);

        for (T queryTerm : queryTerms) {
            int conceptIndex = termList.indexOf(queryTerm);
            queryVector.setEntry(conceptIndex, queryVector.getEntry(conceptIndex) + 1.0);
        }


        // 2   weighing

        // 2.2   get and weigh the document frequencies
        // 2.2.1 binary frequencies TODO: can the frequency of a concept be used to determine its importance?
        SparseRealVector documentFrequencyVector = new OpenMapRealVector(dimension);
        for (SparseRealVector vector : docIdVectorMap.values()) {
            documentFrequencyVector = (OpenMapRealVector)
                    documentFrequencyVector.add(vector.mapToSelf(booleanWeighing));

        }

        // 2.3 weigh the document vectors
        for (Map.Entry<String, SparseRealVector> entry : docIdVectorMap.entrySet()) {
            entry.setValue((SparseRealVector) entry.getValue()
                    .map(booleanWeighing));
        }

        // 2.4 weigh the sendQuery vector
        queryVector = (OpenMapRealVector) queryVector
                .map(booleanWeighing);


        // 3 compare
        Map<String, Double> documentIdScoreMap = new HashMap<>();

        for (Map.Entry<String, SparseRealVector> docIdVectorEntry : docIdVectorMap.entrySet()) {
            String docId = docIdVectorEntry.getKey();
            SparseRealVector documentVector = docIdVectorEntry.getValue();

            // 3.1 get queryVector and documentVector length (Explanation: vectorlengths represent scaling to 1 each score is counting less in 100 compared to 10 entites)
            double queryVectorLength = 0.0;
            double documentVectorLength = 0.0;
            for (int i = 0; i < dimension; i++) {
                queryVectorLength += Math.pow(queryVector.getEntry(i), 2);
                documentVectorLength += Math.pow(documentVector.getEntry(i), 2);
            }
            queryVectorLength = Math.sqrt(queryVectorLength);
            documentVectorLength = Math.sqrt(documentVectorLength);

            // 3.3 calculate the score
            double score = 0.0;
            int matches = 0;
            for (int i = 0; i < dimension; i++) {
                double queryEntry = queryVector.getEntry(i);
                double vectorEntry = documentVector.getEntry(i);

                if(queryEntry*vectorEntry >=1.0){
                    // It is a match,get corresponding dictionary entries.
                    matches++;
                }
                score += (queryEntry/ queryVectorLength)
                        * (vectorEntry/ documentVectorLength);
                //TODO: for index adaptions here would be the match. Reverse get entity here. Get SavedEntity from entity. Set min/max index.
            }

            // 3.4 put the score
            documentIdScoreMap.put(docId, score);
        }

        return documentIdScoreMap;
    }

}
