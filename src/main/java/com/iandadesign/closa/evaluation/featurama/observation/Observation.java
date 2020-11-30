package com.iandadesign.closa.evaluation.featurama.observation;

import com.iandadesign.closa.model.SlidingWindowInfo;
import java.util.*;

public class Observation {

    public boolean isPlagiarism;
    public double fragmentScore;
    //public WeakHashMap<String, List<String>> currentSuspiciousIdTokensMap;
    //public WeakHashMap<String, List<String>> currentCandidateIdTokensMap;
    //public SlidingWindowInfo swiSuspicious;
    //public SlidingWindowInfo swiCandidate;

    public Observation(
            boolean isPlagiarism,
            double fragmentScore)
    {
        this.isPlagiarism = isPlagiarism;
        this.fragmentScore = fragmentScore;
    }

    /* TODO
    public Observation(
            boolean isPlagiarism,
            Double fragmentScore,
            WeakHashMap<String, List<String>> currentSuspiciousIdTokensMap,
            WeakHashMap<String, List<String>> currentCandidateIdTokensMap,
            SlidingWindowInfo swiSuspicious,
            SlidingWindowInfo swiCandidate)
    {
        this.isPlagiarism = isPlagiarism;
        this.fragmentScore = fragmentScore;
        this.currentCandidateIdTokensMap = currentCandidateIdTokensMap;
        this.currentSuspiciousIdTokensMap = currentSuspiciousIdTokensMap;
        this.swiSuspicious = swiSuspicious;
        this.swiCandidate = swiCandidate;
    }
    */

    public int returnObservationDim()
    {
        return 2;
    }

    public double[] returnObservationData()
    {
        double[] returnArray = {this.isPlagiarism ? 1d : 0d, this.fragmentScore};
        return returnArray;
    }
}
