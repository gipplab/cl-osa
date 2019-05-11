package com.fabianmarquart.closa.evaluation.impl;

import com.fabianmarquart.closa.OntologyBasedSimilarityAnalysis;
import com.fabianmarquart.closa.classification.TextClassifier;
import com.fabianmarquart.closa.evaluation.EvaluationSet;
import com.fabianmarquart.closa.language.LanguageDetector;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Cross-Language Ontology-based Similarity Analysis, by Fabian Marquart, 2018.
 * <p>
 * Created by Fabian Marquart on 2018/08/06.
 */
public class CLOSAEvaluationSet extends EvaluationSet {

    private final OntologyBasedSimilarityAnalysis analysis = new OntologyBasedSimilarityAnalysis(new LanguageDetector(), new TextClassifier());
    private boolean graphBasedAnalysis = false;

    public CLOSAEvaluationSet(File folder, String suspiciousSuffix, String candidateSuffix) {
        super(folder, suspiciousSuffix, candidateSuffix);
    }

    public CLOSAEvaluationSet(File suspiciousFolder, File candidateFolder) throws IOException {
        super(suspiciousFolder, candidateFolder);
    }

    public CLOSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage);
    }

    public CLOSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int fileCountLimit) {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, fileCountLimit);
    }

    public CLOSAEvaluationSet(File suspiciousFolder, String suspiciousLanguage,
                              File candidateFolder, String candidateLanguage,
                              File extraCandidateFolder) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, extraCandidateFolder);
    }

    public CLOSAEvaluationSet(File suspiciousFolder, File candidateFolder, int fileCountLimit) {
        super(suspiciousFolder, candidateFolder, fileCountLimit);
    }

    public boolean isGraphBasedAnalysis() {
        return graphBasedAnalysis;
    }

    public void setGraphBasedAnalysis(boolean graphBasedAnalysis) {
        this.graphBasedAnalysis = graphBasedAnalysis;
    }

    /**
     * CL-OSA analysis by querying preprocessed tokens from an inverted-index dictionary.
     */
    @Override
    protected void performAnalysis() {
        if (!graphBasedAnalysis) {
            suspiciousIdDetectedCandidateIdsMap = analysis.performCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap);
        } else {
            suspiciousIdDetectedCandidateIdsMap = analysis.performEnhancedCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap);
        }
    }

    /**
     * CL-OSA preprocessing: translation -> entity extraction
     *
     * @param documentPath     document text
     * @param documentLanguage the document's language
     * @return concepts.
     */
    @Override
    protected List<String> preProcess(String documentPath, String documentLanguage) {
        System.out.println("documentPath = " + documentPath);
        return analysis.preProcess(documentPath, documentLanguage);
    }


}
