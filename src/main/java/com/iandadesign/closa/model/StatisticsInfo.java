package com.iandadesign.closa.model;

import com.iandadesign.closa.analysis.featurama.matrix.CorrelationMatrix;

public class StatisticsInfo {
    public String candidateFilename;
    public InfoHolder infoOverall;
    public InfoHolder infoIsPlagiarism;
    public InfoHolder infoIsNoPlagiarism;

    public InfoHolder infoTruePositives;
    public InfoHolder infoTrueNegatives;
    public InfoHolder infoFalsePositives;
    public InfoHolder infoFalseNegatives;

    public CorrelationMatrix correlation;
}
