package com.fabianmarquart.closa.util.wikidata;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fabianmarquart.closa.model.WikidataEntity;
import javafx.util.Pair;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.SparseRealVector;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.fabianmarquart.closa.util.wikidata.WikidataDumpUtil.*;


/**
 * Created by Fabian Marquart 2018/11/02.
 */
public class WikidataSimilarityUtil {

    static {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger mongoDbDriverLogger = loggerContext.getLogger("org.mongodb.driver");
        mongoDbDriverLogger.setLevel(Level.OFF);
    }


    /**
     * Detects a source document for a suspicious document and an id - document map of candidate documents.
     *
     * @param suspiciousDocument suspicious document
     * @param candidateDocuments id - document map of candidate documents.
     * @param similarityFunction enum: function for document similarity
     * @param numberOfCandidates how many candidates to retrieve.
     * @return candidate id of source.
     */
    public static List<String> retrieveCandidatesByDocumentSimilarity(
            List<WikidataEntity> suspiciousDocument,
            Map<String, List<WikidataEntity>> candidateDocuments,
            SimilarityFunction similarityFunction,
            int numberOfCandidates) {
        if (numberOfCandidates > candidateDocuments.size()) {
            throw new IllegalArgumentException("Cannot retrieve more candidates than given.");
        }

        int stepsLinear = suspiciousDocument.size() + (candidateDocuments.values().stream().mapToInt(List::size).sum());
        int stepsQuadratic = suspiciousDocument.size() * (candidateDocuments.values().stream().mapToInt(List::size).sum());

        ProgressBar progressBar;
        if (similarityFunction == SimilarityFunction.COSINE) {
            progressBar = new ProgressBar("Retrieval with " + similarityFunction.name(), stepsLinear, ProgressBarStyle.ASCII).start();
        } else {
            progressBar = new ProgressBar("Retrieval with " + similarityFunction.name(), stepsQuadratic, ProgressBarStyle.ASCII).start();
        }

        List<String> retrievedCandidateIds = candidateDocuments.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        candidateEntry -> {
                            switch (similarityFunction) {
                                case COSINE:
                                    return cosineSimilarity(suspiciousDocument, candidateEntry.getValue(), progressBar);
                                case ENHANCED_COSINE:
                                    return ontologyEnhancedCosineSimilarity(suspiciousDocument, candidateEntry.getValue(), progressBar);
                                default:
                                    return getDocumentSimilarity(suspiciousDocument, candidateEntry.getValue(), similarityFunction, progressBar);
                            }
                        }))
                .entrySet()
                .stream()
                .sorted(Comparator.comparingDouble((ToDoubleFunction<Map.Entry<String, Double>>) Map.Entry::getValue).reversed())
                .map(Map.Entry::getKey)
                .limit(numberOfCandidates)
                .collect(Collectors.toList());

        progressBar.stop();

        return retrievedCandidateIds;
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Detects a source document for a suspicious document and an id - document map of candidate documents.
     *
     * @param suspiciousDocument suspicious document
     * @param candidateDocuments id - document map of candidate documents.
     * @param similarityFunction enum: function for document similarity
     * @return candidate id of source.
     */
    public static List<String> retrieveCandidateByDocumentSimilarity(
            List<WikidataEntity> suspiciousDocument,
            Map<String, List<WikidataEntity>> candidateDocuments,
            SimilarityFunction similarityFunction) {
        return retrieveCandidatesByDocumentSimilarity(suspiciousDocument, candidateDocuments, similarityFunction, 1);
    }


    /**
     * Cosine similarity.
     * Runtime complexity: O(n)
     *
     * @param firstEntities  first entity list.
     * @param secondEntities second entity list.
     * @param progressBar    Progress bar.
     * @return overall similarity.
     */
    public static double cosineSimilarity(List<WikidataEntity> firstEntities,
                                          List<WikidataEntity> secondEntities,
                                          ProgressBar progressBar) {
        List<WikidataEntity> union = new ArrayList<>(SetUtils.union(
                new HashSet<>(firstEntities),
                new HashSet<>(secondEntities)));

        SparseRealVector firstVector = new OpenMapRealVector(union.size());
        SparseRealVector secondVector = new OpenMapRealVector(union.size());

        for (WikidataEntity firstEntity : firstEntities) {
            firstVector.addToEntry(union.indexOf(firstEntity), 1.0);
            if (progressBar != null) progressBar.step();
        }
        for (WikidataEntity secondEntity : secondEntities) {
            secondVector.addToEntry(union.indexOf(secondEntity), 1.0);
            if (progressBar != null) progressBar.step();
        }

        return firstVector.dotProduct(secondVector) / (firstVector.getNorm() * secondVector.getNorm());
    }

    /**
     * Cosine similarity.
     * Runtime complexity: O(n)
     *
     * @param firstEntities  first entity list.
     * @param secondEntities second entity list.
     * @return overall similarity.
     */
    public static double cosineSimilarity(List<WikidataEntity> firstEntities,
                                          List<WikidataEntity> secondEntities) {
        return cosineSimilarity(firstEntities, secondEntities);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Ontology-enhanced cosine similarity adds 1/(1 + distance(v,w)) to the D[v] and D'[w]
     * <p>
     * Runtime complexity: O(n^2)
     *
     * @param firstEntities  first entity list.
     * @param secondEntities second entity list.
     * @param progressBar    Progress bar.
     * @return overall similarity.
     */
    public static double ontologyEnhancedCosineSimilarity(List<WikidataEntity> firstEntities, List<WikidataEntity> secondEntities, ProgressBar progressBar) {
        List<WikidataEntity> union = new ArrayList<>(SetUtils.union(
                new HashSet<>(firstEntities),
                new HashSet<>(secondEntities)));

        SparseRealVector firstVector = new OpenMapRealVector(union.size());
        SparseRealVector secondVector = new OpenMapRealVector(union.size());

        // for all entities in first document that are not in the second
        for (WikidataEntity firstEntity : firstEntities) {
            // for all entities in second document that are not in the first
            for (WikidataEntity secondEntity : secondEntities) {
                // get their distance
                long distance = distanceWithThreshold(firstEntity, secondEntity, 3);

                // add the inverse of the shortest path to the second entity to the first vector and vice-versa
                int firstIndex = union.indexOf(firstEntity);
                int secondIndex = union.indexOf(secondEntity);

                firstVector.addToEntry(secondIndex, (1.0 / Math.pow(1.0 + distance, 2.0)));
                secondVector.addToEntry(firstIndex, (1.0 / Math.pow(1.0 + distance, 2.0)));

                if (progressBar != null) progressBar.step();
            }
        }

        return firstVector.dotProduct(secondVector) / (firstVector.getNorm() * secondVector.getNorm());
    }

    /**
     * Ontology-enhanced cosine similarity adds 1/(1 + distance(v,w)) to the D[v] and D'[w]
     * <p>
     * Runtime complexity: O(n^2)
     *
     * @param firstEntities  first entity list.
     * @param secondEntities second entity list.
     * @return overall similarity.
     */
    public static double ontologyEnhancedCosineSimilarity(List<WikidataEntity> firstEntities, List<WikidataEntity> secondEntities) {
        return ontologyEnhancedCosineSimilarity(firstEntities, secondEntities, null);
    }


    /**
     * Computes a similarity value from two entity lists.
     *
     * @param firstEntities      first entity list.
     * @param secondEntities     second entity list.
     * @param similarityFunction bi-function that calculates entity pair similarity (higher value = higher similarity)
     * @param progressBar        Progress bar.
     * @return overall similarity.
     */
    public static double getDocumentSimilarity(List<WikidataEntity> firstEntities,
                                               List<WikidataEntity> secondEntities,
                                               SimilarityFunction similarityFunction,
                                               ProgressBar progressBar) {
        return firstEntities.stream()
                .map(firstEntity -> secondEntities.stream()
                        .map(secondEntity -> {
                            switch (similarityFunction) {
                                case COSINE:
                                    throw new IllegalArgumentException("Similarity function " + similarityFunction.name() + " not supported for entity level.");
                                case ENHANCED_COSINE:
                                    throw new IllegalArgumentException("Similarity function " + similarityFunction.name() + " not supported for entity level.");
                                case C_DIST:
                                    return similarityCDist(firstEntity, secondEntity);
                                case WU_PALMER:
                                    return similarityWuPalmer(firstEntity, secondEntity);
                                case LI:
                                    return similarityLi(firstEntity, secondEntity);
                                case LEACKOCK_CHODOROW:
                                    return similarityLeacockChodorow(firstEntity, secondEntity);
                                case NGUYEN_AL_MUBAID:
                                    return similarityNguyenAlMubaid(firstEntity, secondEntity);
                                case RESNIK:
                                    return similarityResnik(firstEntity, secondEntity);
                                case LIN:
                                    return similarityLin(firstEntity, secondEntity);
                                default:
                                    throw new IllegalArgumentException("Similarity function " + similarityFunction.name() + " not supported.");
                            }
                        })
                        .peek(value -> {
                            if (progressBar != null) {
                                progressBar.step();
                            }
                        })
                        .mapToDouble(Double::doubleValue)
                        .sum() / (double) secondEntities.size())
                .mapToDouble(Double::doubleValue)
                .sum() / (double) firstEntities.size();
    }

    /**
     * Computes a similarity value from two entity lists.
     *
     * @param firstEntities      first entity list.
     * @param secondEntities     second entity list.
     * @param similarityFunction bi-function that calculates entity pair similarity (higher value = higher similarity)
     * @return overall similarity.
     */
    public static double getDocumentSimilarity(List<WikidataEntity> firstEntities,
                                               List<WikidataEntity> secondEntities,
                                               SimilarityFunction similarityFunction) {
        return getDocumentSimilarity(firstEntities, secondEntities, similarityFunction, null);
    }


    /////////////////////////////////////////////// Path-based /////////////////////////////////////////////////////

    /**
     * Rada et al.'s conceptual distance (c-dist) similarity measure.
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return similarity value between 0 and 1.
     */
    public static double similarityCDist(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        if (firstEntity.equals(secondEntity)) {
            return 1.0;
        }

        Pair<WikidataEntity, List<Long>> mostSpecificParentEntityWithDepth = getMostSpecificParentEntityWithDepth(firstEntity, secondEntity);

        double n1 = mostSpecificParentEntityWithDepth.getValue().get(0);
        double n2 = mostSpecificParentEntityWithDepth.getValue().get(1);

        return 1.0 / (n1 + n2);
    }

    /**
     * Wu & Palmer's similarity measure.
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return similarity value between 0 and 1.
     */
    public static double similarityWuPalmer(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        if (firstEntity.equals(secondEntity)) {
            return 1.0;
        }

        Pair<WikidataEntity, List<Long>> mostSpecificParentEntityWithDepth = getMostSpecificParentEntityWithDepth(firstEntity, secondEntity);

        double n1 = mostSpecificParentEntityWithDepth.getValue().get(0);
        double n2 = mostSpecificParentEntityWithDepth.getValue().get(1);

        double n3 = getRootDistance(mostSpecificParentEntityWithDepth.getKey());

        return (2.0 * n3) / (n1 + n2 + 2.0 * n3);
    }


    /**
     * Li's similarity measure.
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return similarity value between 0 and 1.
     */
    public static double similarityLi(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        if (firstEntity.equals(secondEntity)) {
            return 1.0;
        }

        Pair<WikidataEntity, List<Long>> mostSpecificParentEntityWithDepth = getMostSpecificParentEntityWithDepth(firstEntity, secondEntity);

        double n1 = mostSpecificParentEntityWithDepth.getValue().get(0);
        double n2 = mostSpecificParentEntityWithDepth.getValue().get(1);

        double L = n1 + n2;

        double H = getRootDistance(mostSpecificParentEntityWithDepth.getKey());

        double alpha = 1.0;
        double beta = 1.0;

        return Math.exp(-alpha * L) * ((Math.exp(beta * H) - Math.exp(-beta * H)) / (Math.exp(beta * H) + Math.exp(-beta * H)));
    }

    /**
     * Leacock & Chodorow's similarity measure.
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return similarity value between 0 and 1.
     */
    public static double similarityLeacockChodorow(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        if (firstEntity.equals(secondEntity)) {
            return 1.0;
        }

        Pair<WikidataEntity, List<Long>> mostSpecificParentEntityWithDepth = getMostSpecificParentEntityWithDepth(firstEntity, secondEntity);

        double n1 = mostSpecificParentEntityWithDepth.getValue().get(0);
        double n2 = mostSpecificParentEntityWithDepth.getValue().get(1);

        return -Math.log((n1 + n2) / WikidataDumpUtil.getOntologyMaxDepth());
    }

    /**
     * Nguyen & Al Mubaid's similarity measure.
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return similarity value between 0 and 1.
     */
    public static double similarityNguyenAlMubaid(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        if (firstEntity.equals(secondEntity)) {
            return 1.0;
        }

        Pair<WikidataEntity, List<Long>> mostSpecificParentEntityWithDepth = getMostSpecificParentEntityWithDepth(firstEntity, secondEntity);

        double n1 = mostSpecificParentEntityWithDepth.getValue().get(0);
        double n2 = mostSpecificParentEntityWithDepth.getValue().get(1);

        double d = getRootDistance(mostSpecificParentEntityWithDepth.getKey());

        return Math.log(2 + (n1 + n2 - 1) * (WikidataDumpUtil.getOntologyMaxDepth() - d));
    }

    ///////////////////////////////////////// Information Content-based ////////////////////////////////////////////


    /**
     * Resnik similarity measure.
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return similarity value between 0 and 1.
     */
    public static double similarityResnik(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        return -Math.log(secoIntrinsicInformationContent(getMostSpecificParentEntity(firstEntity, secondEntity)));
    }

    /**
     * Lin similarity. Dekang Lin. 1998. An Information-Theoretical Definition of Similarity. ICML
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return similarity value between 0 and 1.
     */
    public static double similarityLin(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        double tuningFactor = 0.5;

        return (2 * Math.log(zhouIntrinsicInformationContent(getMostSpecificParentEntity(firstEntity, secondEntity), tuningFactor)))
                /
                (Math.log(zhouIntrinsicInformationContent(firstEntity, tuningFactor)) + Math.log(zhouIntrinsicInformationContent(secondEntity, tuningFactor)));
    }


    /////////////////////////////////////// Intrinsic Information Content //////////////////////////////////////////


    /**
     * Intrinsic IC (information content) by Seco et al. (2004)
     *
     * @param entity entity.
     * @return entity's intrinsic information content.
     */
    public static double secoIntrinsicInformationContent(WikidataEntity entity) {
        return 1.0 - ((Math.log(getAllDescendants(entity).size() + 1.0)) / (Math.log(getOntologyEntityCount())));
    }


    /**
     * Intrinsic IC (information content) by Zhou et al. (2008 b)
     *
     * @param entity       entity.
     * @param tuningFactor tuning factor k.
     * @return entity's intrinsic information content.
     */
    public static double zhouIntrinsicInformationContent(WikidataEntity entity, double tuningFactor) {
        return tuningFactor *
                (1.0 - ((Math.log(getAllDescendants(entity).size() + 1.0)) / (Math.log(getOntologyEntityCount()))))
                + (1.0 - tuningFactor) *
                ((Math.log(getRootDistance(entity))) / (Math.log(getOntologyMaxDepth())));
    }

    /**
     * Wu formula for cotransduction frequency.
     * Wu TT. 1966. A model for three-point analysis of random general transduction.
     *
     * @param firstEntities  first entity list.
     * @param secondEntities second entity list.
     * @return overall similarity.
     */
    public static double getDocumentCotransductionFrequency(List<WikidataEntity> firstEntities, List<WikidataEntity> secondEntities) {
        // 1 edit distance

        // map each first entity to closest second entity
        List<WikidataEntity> closestSecondEntities = firstEntities.stream()
                .map(firstEntity -> secondEntities.stream()
                        .min(Comparator.comparingLong(secondEntity -> distance(firstEntity, secondEntity)))
                        .get())
                .collect(Collectors.toList());

        List<WikidataEntity> nonMappedSecondEntities = new ArrayList<>(secondEntities);
        nonMappedSecondEntities.removeAll(closestSecondEntities);

        long editDistance = IntStream.range(0, firstEntities.size())
                .mapToLong(i -> distance(firstEntities.get(i), closestSecondEntities.get(i)))
                .sum();

        editDistance += nonMappedSecondEntities.stream()
                .mapToLong(WikidataDumpUtil::getRootDistance)
                .sum();

        // 2: 1 - (d/L)^3
        // where L the size of transducing fragment
        double L = firstEntities.size() != 0 ? firstEntities.size() : 1.0;

        return 1.0 - Math.pow(editDistance / L, 3.0);
    }


    ////////////////////////////////////////////// Experimental /////////////////////////////////////////////////////

    /**
     * Modified Jaccard coefficient for usage on taxonomies.
     * Standard: J(A,B) = |A ∩ B|/|A ∪ B| = |A ∩ B|/(|A|+|B|+|A ∩ B|)
     * <p>
     * Modification: J_O(A,B) = ( ∑∑ 1/(path(A_i,B_j) + 1)^2 ) / (|A|+|B|+ ∑∑ 1/(path(A_i,B_j) + 1)^2)
     *
     * @param firstEntities  first entity list.
     * @param secondEntities second entity list.
     * @return overall similarity.
     */
    public static double similarityEnhancedJaccard(List<WikidataEntity> firstEntities, List<WikidataEntity> secondEntities) {
        // standard Jaccard modified by partial overlap of distances between entities
        double intersection = 0.0;

        for (WikidataEntity firstEntity : firstEntities) {

            for (WikidataEntity secondEntity : secondEntities) {
                intersection += 1.0 / Math.pow(distance(firstEntity, secondEntity) + 1.0, 2.0);
            }
        }

        double union = firstEntities.size() + secondEntities.size() - intersection;

        return intersection / union;
    }


    /**
     * Enum for similarity functions.
     */
    public enum SimilarityFunction {
        COSINE, ENHANCED_COSINE,
        C_DIST, WU_PALMER, LI, LEACKOCK_CHODOROW, NGUYEN_AL_MUBAID,
        RESNIK, LIN
    }

}