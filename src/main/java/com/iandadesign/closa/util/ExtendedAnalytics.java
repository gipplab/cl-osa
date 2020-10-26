package com.iandadesign.closa.util;

import com.iandadesign.closa.model.InfoHolder;
import com.iandadesign.closa.model.ScoringChunk;
import com.iandadesign.closa.model.ScoringChunksCombined;
import com.iandadesign.closa.model.StatisticsInfo;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class provides additional analysis methods for
 * the clustering in CL-OSA extended algorithm.
 *
 * The functions are rather used for analysis than in processing.
 *
 * @author Johannes Stegmüller (02.07.2020)
 */
public  class ExtendedAnalytics {


    public static double calculateMean(ScoringChunk[][] scoreMatrix, ExtendedLogUtil logUtil, String extraInfo){
        List<Double> myValues = new ArrayList<>();

        for(ScoringChunk[] scoreRow: scoreMatrix){
            for(ScoringChunk scoreItem:scoreRow){
                if(scoreItem==null){
                    // myValues.add(0.0);
                    continue;
                }else{
                    double value = scoreItem.getComputedCosineSimilarity();
                    if(value==-1) continue;
                    myValues.add(value);
                }
            }
        }

        double[] arr = myValues.stream().mapToDouble(Double::doubleValue).toArray(); //via method reference
        Mean mean = new Mean();
        return mean.evaluate(arr);

    }

    public static double calculateMedianNew(ScoringChunk[][] scoreMatrix, ExtendedLogUtil logUtil, String extraInfo){
        List<Double> myValues = new ArrayList<>();

        for(ScoringChunk[] scoreRow: scoreMatrix){
            for(ScoringChunk scoreItem:scoreRow){
                if(scoreItem==null){
                    // myValues.add(0.0);
                    continue;
                }else{
                    double value = scoreItem.getComputedCosineSimilarity();
                    if(value==-1) continue;
                    myValues.add(value);
                }
            }
        }

        double[] arr = myValues.stream().mapToDouble(Double::doubleValue).toArray(); //via method reference
        Median median = new Median();
        return median.evaluate(arr);

    }
    public static double calculateMedian(ScoringChunk[][] scoreMatrix, ExtendedLogUtil logUtil, String extraInfo){
        //System.out.println("Calculating median");
        List<Double> myValues = new ArrayList<>();
        for(ScoringChunk[] scoreRow: scoreMatrix){
            for(ScoringChunk scoreItem:scoreRow){
                if(scoreItem==null){
                    // myValues.add(0.0);
                    continue;
                }else{
                    double value = scoreItem.getComputedCosineSimilarity();
                    if(value==-1) continue;
                    myValues.add(value);
                }
            }
        }

        Collections.sort(myValues);
        double medianValue;
        float medianPos = (myValues.size() + 1) / (float) 2;
        if((myValues.size()+1)%2 > 0){
            // Size is even-> middle value of 2
            int firstIndex = (int) Math.floor(medianPos);
            int secondIndex = (int) Math.abs(medianPos);

            medianValue = (myValues.get(firstIndex)+myValues.get(secondIndex))/2;
        }else{
            // Size is uneven - take the middle
            int index = (int) medianPos;
            medianValue = myValues.get(index);

        }
        if(logUtil!=null){
            logUtil.logAndWriteError(false, extraInfo, "Median of populated chunks is:", medianValue);
        }
        return medianValue;
    }
    private static InfoHolder calculateStatisticsPerSet(List<ScoringChunk> scoringChunks){
        InfoHolder infoHolder = new InfoHolder();
        double[] filteredScores = scoringChunks.stream().filter(v -> v.getComputedCosineSimilarity()!=-1.0).mapToDouble(ScoringChunk::getComputedCosineSimilarity).toArray();
        infoHolder.size = filteredScores.length;

        if(infoHolder.size ==0){
            infoHolder.mean = 0;
            infoHolder.median =0;
            infoHolder.max = 0;
            infoHolder.min = 0;// min is without zeros.
            return infoHolder;
        }

        infoHolder.mean = new Mean().evaluate(filteredScores);
        infoHolder.median = new Median().evaluate(filteredScores);
        try {
            infoHolder.max = Arrays.stream(filteredScores).max().getAsDouble();
        }catch(NoSuchElementException ex){
            infoHolder.max = 0;
        }
        try {
            infoHolder.min = Arrays.stream(filteredScores).filter(v -> v != 0.0).min().getAsDouble(); // min is without zeros.
        }catch(NoSuchElementException ex){
            infoHolder.min = 0;
        }

        return infoHolder;
    }
    public static StatisticsInfo createAnalyticsScores(ScoringChunksCombined scoringChunksCombined){
        StatisticsInfo statisticsInfo = new StatisticsInfo();
        List<ScoringChunk> scoringChunks = scoringChunksCombined.getScoringChunksList();
        List<ScoringChunk> isPlagiarism = scoringChunks.stream().filter(v -> v.isPlagiarism()).collect(Collectors.toList());
        List<ScoringChunk> isNoPlagiarism = scoringChunks.stream().filter(v -> !v.isPlagiarism()).collect(Collectors.toList());
        List<ScoringChunk> truePositives = scoringChunks.stream().filter(v -> v.isDetectedAsPlagiarism() && v.isPlagiarism()).collect(Collectors.toList());
        List<ScoringChunk> falsePositives = scoringChunks.stream().filter(v -> v.isDetectedAsPlagiarism() && !v.isPlagiarism()).collect(Collectors.toList());
        List<ScoringChunk> trueNegatives = scoringChunks.stream().filter(v -> !v.isDetectedAsPlagiarism() && v.isPlagiarism()).collect(Collectors.toList());
        List<ScoringChunk> falseNegatives = scoringChunks.stream().filter(v -> !v.isDetectedAsPlagiarism() && !v.isPlagiarism()).collect(Collectors.toList());
        statisticsInfo.infoOverall = calculateStatisticsPerSet(scoringChunks);
        statisticsInfo.infoIsPlagiarism = calculateStatisticsPerSet(isPlagiarism);
        statisticsInfo.infoIsNoPlagiarism = calculateStatisticsPerSet(isNoPlagiarism);
        statisticsInfo.infoTruePositives = calculateStatisticsPerSet(truePositives);
        statisticsInfo.infoFalsePositives = calculateStatisticsPerSet(falsePositives);
        statisticsInfo.infoTrueNegatives = calculateStatisticsPerSet(trueNegatives);
        statisticsInfo.infoFalseNegatives = calculateStatisticsPerSet(falseNegatives);
        return statisticsInfo;
    }

    public static StatisticsInfo createCombinedStatistics(Map<String, List<StatisticsInfo>> allStatisticsInfosMap){
        List<StatisticsInfo> allStatsInfos = new ArrayList<>();
        for (Map.Entry<String, List<StatisticsInfo>> entry : allStatisticsInfosMap.entrySet()) {
            // Here more in depth analysis (for each suspicious file and its candidates) can be done if necessary.
             allStatsInfos.addAll(entry.getValue());
        }
        StatisticsInfo combinedStatisticsInfo = new StatisticsInfo();
        combinedStatisticsInfo.infoOverall = crunchInfoHolders(allStatsInfos.stream().map(statisticsInfo -> statisticsInfo.infoOverall).collect(Collectors.toList()));
        combinedStatisticsInfo.infoIsPlagiarism = crunchInfoHolders(allStatsInfos.stream().map(statisticsInfo -> statisticsInfo.infoIsPlagiarism).collect(Collectors.toList()));
        combinedStatisticsInfo.infoIsNoPlagiarism = crunchInfoHolders(allStatsInfos.stream().map(statisticsInfo -> statisticsInfo.infoIsNoPlagiarism).collect(Collectors.toList()));
        combinedStatisticsInfo.infoTruePositives = crunchInfoHolders(allStatsInfos.stream().map(statisticsInfo -> statisticsInfo.infoTruePositives).collect(Collectors.toList()));
        combinedStatisticsInfo.infoFalsePositives = crunchInfoHolders(allStatsInfos.stream().map(statisticsInfo -> statisticsInfo.infoFalsePositives).collect(Collectors.toList()));
        combinedStatisticsInfo.infoTrueNegatives = crunchInfoHolders(allStatsInfos.stream().map(statisticsInfo -> statisticsInfo.infoTrueNegatives).collect(Collectors.toList()));
        combinedStatisticsInfo.infoFalseNegatives = crunchInfoHolders(allStatsInfos.stream().map(statisticsInfo -> statisticsInfo.infoFalseNegatives).collect(Collectors.toList()));

        return combinedStatisticsInfo;
    }

    public static InfoHolder crunchInfoHolders(List<InfoHolder> infoHolders){
        InfoHolder finalInfoHilder = new InfoHolder();
        long overallSize= 0;
        double meanAcc = 0;
        double medianAcc = 0;
        double maxAcc = 0;
        double minAcc = 0;

        for(InfoHolder infoHolder:infoHolders){
            overallSize+=infoHolder.size;
            meanAcc += infoHolder.mean * infoHolder.size;
            medianAcc += infoHolder.median * infoHolder.size;
            maxAcc += infoHolder.max * infoHolder.size;
            minAcc += infoHolder.min * infoHolder.size;
        }
        finalInfoHilder.mean = meanAcc / overallSize;
        finalInfoHilder.max = maxAcc / overallSize;
        finalInfoHilder.min = minAcc / overallSize;
        finalInfoHilder.median = medianAcc / overallSize;
        finalInfoHilder.size = overallSize;

        return finalInfoHilder;
    }

    public static void printStatisticsInfo(StatisticsInfo statisticsInfo, ExtendedLogUtil logUtil ){
        logUtil.logAndWriteStandard(false, "Overall Statistics:-----------------------");
        printInfoHolder(statisticsInfo.infoOverall, logUtil);
        logUtil.logAndWriteStandard(false, "Plagiarism Statistics:");
        printInfoHolder(statisticsInfo.infoIsPlagiarism, logUtil);
        logUtil.logAndWriteStandard(false, "Non-Plagiarism Statistics:");
        printInfoHolder(statisticsInfo.infoIsNoPlagiarism, logUtil);
        logUtil.logAndWriteStandard(false, "------------------------------------------");

    }

    public static void printInfoHolder(InfoHolder infoHolder, ExtendedLogUtil logUtil){
        logUtil.logAndWriteStandard(true, "Maximum: ", infoHolder.max);
        logUtil.logAndWriteStandard(true, "Minimum: ", infoHolder.min);
        logUtil.logAndWriteStandard(true, "Size: ", infoHolder.size);
        logUtil.logAndWriteStandard(true, "Median: ", infoHolder.median);
        logUtil.logAndWriteStandard(true, "Mean: ", infoHolder.mean);
    }
}
