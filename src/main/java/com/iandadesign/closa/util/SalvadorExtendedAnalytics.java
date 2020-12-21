package com.iandadesign.closa.util;


import com.iandadesign.closa.model.InfoHolder;
import com.iandadesign.closa.model.SalvadorInfoHolder;
import com.iandadesign.closa.model.SalvadorStatisticsInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SalvadorExtendedAnalytics {
    public static SalvadorStatisticsInfo createCombinedStatistics(Map<String, Map<String, SalvadorStatisticsInfo>> allStatisticsInfosMap){
        List<SalvadorStatisticsInfo> suspCombined = new ArrayList<>();
        for (String suspiciousDocument : allStatisticsInfosMap.keySet()) {
            Map<String, SalvadorStatisticsInfo> statsForSuspDoc = allStatisticsInfosMap.get(suspiciousDocument);
            List<SalvadorStatisticsInfo> allCandidateInfos = new ArrayList<>(statsForSuspDoc.values());

            SalvadorStatisticsInfo combinedStatsInfoSusp = new SalvadorStatisticsInfo();
            combinedStatsInfoSusp.overallInfoPositives = crunchInfoHolders(allCandidateInfos.stream().map(statisticsInfo -> statisticsInfo.overallInfoPositives).collect(Collectors.toList()));
            combinedStatsInfoSusp.overallInfoNegatives = crunchInfoHolders(allCandidateInfos.stream().map(statisticsInfo -> statisticsInfo.overallInfoNegatives).collect(Collectors.toList()));
            combinedStatsInfoSusp.mergedInfoPositives = crunchInfoHolders(allCandidateInfos.stream().map(statisticsInfo -> statisticsInfo.mergedInfoPositives).collect(Collectors.toList()));
            combinedStatsInfoSusp.mergedInfoNegatives = crunchInfoHolders(allCandidateInfos.stream().map(statisticsInfo -> statisticsInfo.mergedInfoNegatives).collect(Collectors.toList()));

            suspCombined.add(combinedStatsInfoSusp);
        }
        SalvadorStatisticsInfo allCombined = new SalvadorStatisticsInfo();
        allCombined.overallInfoPositives = crunchInfoHolders(suspCombined.stream().map(statisticsInfo -> statisticsInfo.overallInfoPositives).collect(Collectors.toList()));
        allCombined.overallInfoNegatives = crunchInfoHolders(suspCombined.stream().map(statisticsInfo -> statisticsInfo.overallInfoNegatives).collect(Collectors.toList()));
        allCombined.mergedInfoPositives = crunchInfoHolders(suspCombined.stream().map(statisticsInfo -> statisticsInfo.mergedInfoPositives).collect(Collectors.toList()));
        allCombined.mergedInfoNegatives = crunchInfoHolders(suspCombined.stream().map(statisticsInfo -> statisticsInfo.mergedInfoNegatives).collect(Collectors.toList()));

        return allCombined;
    }

    public static SalvadorInfoHolder crunchInfoHolders(List<SalvadorInfoHolder> infoHolders){
        SalvadorInfoHolder finalInfoHolder = new SalvadorInfoHolder();

        int possiblePlagiarizedArea= 0;
        int numFindingsAcc = 0;
        double meanAcc = 0;
        double maxAcc = 0;
        double minAcc = 0;
        long sizeCharsAcc = 0;


        for(SalvadorInfoHolder infoHolder:infoHolders){
            if(infoHolder==null) continue;
            sizeCharsAcc += infoHolder.sizeChars;
            meanAcc += (infoHolder.mean* infoHolder.numFindings);
            maxAcc += (infoHolder.max * infoHolder.numFindings);
            minAcc += (infoHolder.min * infoHolder.numFindings);
            numFindingsAcc += infoHolder.numFindings;
            possiblePlagiarizedArea += infoHolder.possiblePlagiarizedArea;
        }
        finalInfoHolder.mean = meanAcc / ( numFindingsAcc);
        finalInfoHolder.max = maxAcc / (numFindingsAcc);
        finalInfoHolder.min = minAcc / (numFindingsAcc);
        finalInfoHolder.sizeChars = sizeCharsAcc;
        finalInfoHolder.numFindings = numFindingsAcc;
        finalInfoHolder.possiblePlagiarizedArea = possiblePlagiarizedArea;

        return finalInfoHolder;
    }


}

