package com.iandadesign.closa.model;

import com.iandadesign.closa.util.PAN11RankingEvaluator;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.SparseRealVector;

import java.util.*;

/**
 * Created by Johannes Stegmüller for perfomance optimizations in previous dictionary 17.02.2021
 */
public class DictionaryLong<T> {

    private static UnivariateFunction booleanWeighing = v -> {
        if (v > 0.0) return 1.0;
        return 0.0;
    };
    private static UnivariateFunction unchangedWeighting = v -> {
        return v;
    };



    private Set<T> terms;
    private Map<T, Map<Long, Integer>> dictionary;


    public DictionaryLong(Map<Long, List<T>> documentIdTokensMap) {
        terms = new HashSet<>();
        System.out.println("RAM-Creating dictionary");
        dictionary = createDictionary(documentIdTokensMap);
        System.out.println("RAM-Created dictionary");

    }

    /**
     * Cosine similarity computation for two lists of tokens.
     *
     * @param tokens1 first tokens list
     * @param tokens2 second tokens list
     * @param <T>     type parameter
     * @return similarity value.
     */
    public static <T> double cosineSimilarity(List<T> tokens1, List<T> tokens2) {
        List<SparseRealVector> vectors = createVectorsFromDocuments(tokens1, tokens2);
        return cosineSimilarity(vectors.get(0), vectors.get(1));
    }

    /**
     * Builds vectors from variable number of token lists.
     *
     * @param tokenLists variable number of token lists.
     * @param <T>        type
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
    private Map<T, Map<Long, Integer>> createDictionary(Map<Long, List<T>> documentIdTokensMap) {
        Map<T, Map<Long, Integer>> dictionary = new HashMap<>();

        documentIdTokensMap.forEach((id, tokenList) -> tokenList.forEach(token -> {
            if (dictionary.containsKey(token)) {
                if (dictionary.get(token).containsKey(id)) {
                    Integer oldCount = dictionary.get(token).get(id);
                    dictionary.get(token).put(id, oldCount + 1);
                } else {
                    dictionary.get(token).put(id, 1);
                }
            } else {
                Map<Long, Integer> documentCountMap = new HashMap<>();
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
     * @param queryTerms sendQuery
     * @return the document ids - score map.
     */
    public Map<Long, Float> query(final List<T> queryTerms, boolean absoluteScoring, boolean doStatsweighting) {
        // TF/IDF etc query terms are list of wikidata entities, calculate the corresponding weights before, get them here
        // TODO RAM is this really ok ?

        if (queryTerms.isEmpty()) {
            return new HashMap<>();
        }

        // 1 vector building
        Map<Long, SparseRealVector> docIdVectorMap = new HashMap<>();

        List<T> termList = new ArrayList<>(terms);
        termList.addAll(new ArrayList<>(new HashSet<>(queryTerms)));
        int dimension = termList.size();

        // 1.1 map term -> (docId -> freq)
        //     to docId -> term vector
        dictionary.forEach((term, value) -> {
            int conceptIndex = termList.indexOf(term);
            //TBD add the individual weighting factor here?! For simplicities sake write it to frequency
            // TODO js add custom weighting here make triggerable

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
        UnivariateFunction weighting = booleanWeighing;

        // 1.2 map sendQuery to sendQuery vector (vector has dimension of terms)
        SparseRealVector queryVector = new OpenMapRealVector(dimension);

        for (T queryTerm : queryTerms) {
            int conceptIndex = termList.indexOf(queryTerm);
            queryVector.setEntry(conceptIndex, queryVector.getEntry(conceptIndex) + 1.0);
        }


        // 2   weighing

        // 2.2   get and weigh the document frequencies
        // 2.2.1 binary frequencie
        SparseRealVector documentFrequencyVector = new OpenMapRealVector(dimension);
        for (SparseRealVector vector : docIdVectorMap.values()) {
            documentFrequencyVector = (OpenMapRealVector)
                    documentFrequencyVector.add(vector.mapToSelf(weighting));

        }

        // 2.3 weigh the document vectors
        for (Map.Entry<Long, SparseRealVector> entry : docIdVectorMap.entrySet()) {
            entry.setValue((SparseRealVector) entry.getValue()
                    .map(weighting));
        }

        // 2.4 weigh the sendQuery vector
        queryVector = (OpenMapRealVector) queryVector
                .map(weighting);


        // 3 compare
        Map<Long, Float> documentIdScoreMap = new HashMap<>();

        for (Map.Entry<Long, SparseRealVector> docIdVectorEntry : docIdVectorMap.entrySet()) {
            Long docId = docIdVectorEntry.getKey();
            SparseRealVector documentVector = docIdVectorEntry.getValue();

            // 3.1 get queryVector and documentVector length
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
            for (int i = 0; i < dimension; i++) {
               if(!absoluteScoring){
                   score += (queryVector.getEntry(i) / queryVectorLength)
                           * (documentVector.getEntry(i) / documentVectorLength);
               }else{
                   score += (queryVector.getEntry(i))
                           * (documentVector.getEntry(i));
               }
                /*
                score += (queryVector.getEntry(i))
                        * (documentVector.getEntry(i));
                */
            }

            // 3.4 put the score
            documentIdScoreMap.put(docId, (float) score);
        }

        docIdVectorMap.clear();
        return documentIdScoreMap;
    }

}
