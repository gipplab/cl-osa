package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.language.LanguageDetector;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * Cross-Language Ontology-based Similarity Analysis, by Fabian Marquart, 2018.
 * <p>
 * Created by Fabian Marquart on 2018/08/06.
 */
public class CLOSAEvaluationSet extends EvaluationSet<String> {

    private OntologyBasedSimilarityAnalysis analysis;
    private boolean graphBasedAnalysis = false;
    private boolean linkedDataBasedAnalysis = false;

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

    public void setLinkedDataBasedAnalysis(boolean linkedDataBasedAnalysis) {
        this.linkedDataBasedAnalysis = linkedDataBasedAnalysis;
    }

    public void setGraphBasedAnalysis(boolean graphBasedAnalysis) {
        this.graphBasedAnalysis = graphBasedAnalysis;
    }

    /**
     * CL-OSA analysis by querying preprocessed tokens from an inverted-index dictionary.
     */
    @Override
    protected void performAnalysis() {
        if (analysis == null) {
            analysis = new OntologyBasedSimilarityAnalysis(new LanguageDetector(), new TextClassifier());
        }

        if (graphBasedAnalysis) {
            suspiciousIdCandidateScoresMap = analysis.performEnhancedCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap);
        } else if (linkedDataBasedAnalysis) {
            suspiciousIdCandidateScoresMap = analysis.performPropertyCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap);
        } else {
            suspiciousIdCandidateScoresMap = analysis.performCosineSimilarityAnalysis(suspiciousIdTokensMap, candidateIdTokensMap);
        }
    }

    /**
     * CL-OSA preprocessing: translation and entity extraction
     *
     * @param documentPath     document text
     * @param documentLanguage the document's language
     * @return concepts.
     */
    @Override
    protected List<String> preProcess(String documentPath, String documentLanguage) {
        this.analysis = new OntologyBasedSimilarityAnalysis(new LanguageDetector(), new TextClassifier());
        List<String> preProcessed = analysis.preProcess(documentPath, documentLanguage);

        return preProcessed;
    }


}
