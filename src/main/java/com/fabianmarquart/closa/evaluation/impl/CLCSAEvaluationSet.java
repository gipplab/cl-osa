package com.fabianmarquart.closa.evaluation.impl;

import com.fabianmarquart.closa.evaluation.EvaluationSet;
import com.fabianmarquart.closa.model.Dictionary;
import com.fabianmarquart.closa.model.Token;
import com.fabianmarquart.closa.util.ConceptUtil;
import com.fabianmarquart.closa.util.TokenUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cross-Language Conceptual Similarity Analysis, by Fabian Marquart, 2017.
 * <p>
 * Created by Fabian Marquart on 2018/01/01.
 */
public class CLCSAEvaluationSet extends EvaluationSet {

    public CLCSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int i) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, i);
    }

    public CLCSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage);
    }

    /**
     * CL-CSA analysis by querying preprocessed tokens from an inverted-index dictionary.
     */
    @Override
    protected void performAnalysis() {
        // create dictionary
        System.out.println("Create dictionary \n");
        Dictionary<String> dictionary = new Dictionary<>(candidateIdTokensMap);

        suspiciousIdDetectedCandidateIdsMap = new HashMap<>();

        // perform detailed analysis
        System.out.println("Perform detailed analysis \n");

        suspiciousIdTokensMap.forEach((suspiciousId, suspiciousConcepts) -> {

            // look in dictionary
            Map<String, Double> candidateScoreMap = dictionary.query(suspiciousConcepts);

            if (candidateScoreMap.isEmpty()) {
                System.out.println("False negative. Did not detect");
                System.out.println(suspiciousId);
                // System.out.println(suspiciousConcepts);
                System.out.println("in");
                System.out.println(suspiciousIdCandidateIdMap.get(suspiciousId));
                // System.out.println(candidateIdTokensMap.get(suspiciousIdCandidateIdMap.get(suspiciousId)));
            } else {
                String retrievedCandidateId = candidateScoreMap.entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue())
                        .get()
                        .getKey();

                if (suspiciousIdCandidateIdMap.get(suspiciousId).equals(retrievedCandidateId)) {
                    System.out.println("True positive.");
                } else {
                    System.out.println("False positive. Falsely detected ");
                    System.out.println(suspiciousId);
                    // System.out.println(suspiciousConcepts);
                    System.out.println("in");
                    System.out.println(retrievedCandidateId);
                    // System.out.println(candidateIdTokensMap.get(currentSourceId));
                }
            }

            suspiciousIdDetectedCandidateIdsMap.put(suspiciousId, candidateScoreMap);

        });
    }

    /**
     * CL-CSA preprocessing: text -> concepts -> translation -> stemming & stop-word removal
     *
     * @param documentPath     document path
     * @param documentLanguage the document's language
     * @return concepts.
     */
    @Override
    protected List<String> preProcess(String documentPath, String documentLanguage) {
        try {
            // read in the file
            String documentText = FileUtils.readFileToString(new File(documentPath), StandardCharsets.UTF_8);

            String documentTokensPath = documentPath.replace("pds", "pds/preprocessed/" + this.getClass().getSimpleName());

            // if the file has already been pre-processed
            if (Files.exists(Paths.get(documentTokensPath)) // file exists and is not empty
                    && !FileUtils.readLines(new File(documentTokensPath), StandardCharsets.UTF_8).isEmpty()) {
                System.out.println("Read preprocessed:");
                return new ArrayList<>(FileUtils.readLines(new File(documentTokensPath), StandardCharsets.UTF_8));
            }

            List<Token> documentTokens = ConceptUtil.getConceptsFromString(documentText, 4);
            documentTokens.forEach(Token::toLowerCase);
            documentTokens = ConceptUtil.translateConcepts(documentTokens, documentLanguage, "en");
            documentTokens = TokenUtil.stem(documentTokens, "en");
            documentTokens = TokenUtil.removeStopwords(documentTokens, "en");

            // FIXME : return empty list
            List<String> tokens = documentTokens.stream().map(Token::getToken).collect(Collectors.toList());
            System.out.println(tokens);
            return tokens;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
