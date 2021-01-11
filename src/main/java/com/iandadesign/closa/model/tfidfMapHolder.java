package com.iandadesign.closa.model;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.SalvadorFragmentLevelEval;
import com.iandadesign.closa.util.PAN11PlagiarismInfo;
import edu.stanford.nlp.util.ArrayMap;
import org.apache.commons.collections4.comparators.ReverseComparator;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class tfidfMapHolder {
    static Map<String, Map<String, tfidfTokenInfo>> suspFiles2TermsMap;
    static Map<String, Map<String, tfidfTokenInfo>> candFiles2TermsMap;
    static Map<String, tfidfTokenInfo> suspOverallTermsMap;
    static Map<String, tfidfTokenInfo> candOverallTermsMap;

    public static double getWeightFor(String wikidataID, String candFile){
        //System.out.println("TODO implement complete with fragment / candfile");

        double tfidf = candFiles2TermsMap.get(candFile).get(wikidataID).tfidf;
        return tfidf;
    }

    public static void calculateWeightingScores(){
        //System.out.println("TODO implement");



        // Store idf scores for each entity to the candidate terms map
        calculateIDFs2map(candOverallTermsMap, candFiles2TermsMap);
        // calculateIDFs2map(suspOverallTermsMap, suspFiles2TermsMap);

        // Mind that this only works if idf have been calculated before
        calculateTFsAndTFIDFs2Map(candOverallTermsMap, candFiles2TermsMap);


    }

    private static void calculateTFsAndTFIDFs2Map(Map<String, tfidfTokenInfo> overallTermsMap, Map<String, Map<String, tfidfTokenInfo>> files2TermsMap) {
        // Term frequency calculation:
        // tf(t,d) = ((0,5 + 0,5 * ft,q / (maxt * ftq )) * log(N/nt)
        // ft_q = occurences of the entity in document
        // max_t  =  occurences of most frequent entity in document

        for(String filename: files2TermsMap.keySet()){
            Map<String, tfidfTokenInfo> terms4file =  files2TermsMap.get(filename);
            long max_t = 0;
            boolean firstEntry = true;
            for(String wikidataID:terms4file.keySet()){
                tfidfTokenInfo tokenInfo = terms4file.get(wikidataID);

                if(firstEntry == true){
                    max_t  = tokenInfo.getOccurences();
                    firstEntry = false;
                }
                tokenInfo.max_t  = max_t;
                tfidfTokenInfo overallInfo = overallTermsMap.get(wikidataID);
                long n_t =overallInfo.n_t;
                long N = overallInfo.N;
                double idf = overallInfo.idf;
                long ft_q = tokenInfo.getOccurences();
                if(SalvadorAnalysisParameters.WEIGHTING_SCHEME==1){
                    double division = ((double) ft_q) / (max_t * ft_q);
                    double division2 = ((double) N) / n_t;
                    tokenInfo.tf = (0.5 + (0.5 *  division)) * Math.log(division2);
                    tokenInfo.tfidf = idf * tokenInfo.tf;
                    tokenInfo.idf = overallInfo.idf; // Just duplicating the idf here for plausibiltiy checks
                }
            }
        }
    }

    private static void calculateIDFs2map(Map<String, tfidfTokenInfo> myMap,Map<String, Map<String, tfidfTokenInfo>> files2termsMap ) {
        // Calculating params for IDF
        // idf(t,D) = f_td * log (N / n_t)
        // N: total number of documents in the corpus
        // f_td: occurences of enittiy <xyz> in the corpus
        // n_t: number of documents where the entity <xyz> occurs

        for(String wikidataID: myMap.keySet()){
            tfidfTokenInfo currentInfo = myMap.get(wikidataID);
            long f_td = currentInfo.occurences;
            int N = files2termsMap.size();
            int n_t = 0;

            // TBD Step is taking very long, can be done more elegant probably in assignment parsing
            for(String candidateFile:candFiles2TermsMap.keySet()){
                tfidfTokenInfo tokenInfo = files2termsMap.get(candidateFile).get(wikidataID);
                if(tokenInfo!=null) n_t+=1;
            }
            currentInfo.N = N;
            currentInfo.n_t = n_t;
            if(SalvadorAnalysisParameters.WEIGHTING_SCHEME==1){
                double divisionResult = ((double) N) / n_t;
                currentInfo.idf = f_td * Math.log(divisionResult);
            }
            //System.out.println(wikidataID+" f_td:"+f_td+" N:"+N+" n_t:"+n_t);
        }
    }

    public static void createRelevantMaps(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, OntologyBasedSimilarityAnalysis osa, List<File> candidateFiles, List<File> suspiciousFiles) throws Exception {
        Map<String, List<SavedEntity>> tfSuspFragmentEntities = tfidfMapHolder.tfidfGetSavedEntities2Files(osa, suspiciousFiles, true, plagiarismInformation);
        Map<String, List<SavedEntity>> tfCandFragmentEntities = tfidfGetSavedEntities2Files(osa, candidateFiles, false, null);


        suspFiles2TermsMap = tfidfGetOccurencesByFile(tfSuspFragmentEntities);
        candFiles2TermsMap = tfidfGetOccurencesByFile(tfCandFragmentEntities);

        // Renaming to 'candidate' for later matching in Dictionary
        /*
        Map<String, Map<String, tfidfTokenInfo>> newMap = new ArrayMap<>();
        for(Map.Entry<String, Map<String, tfidfTokenInfo>> entry:candFiles2TermsMap.entrySet()){
            newMap.put(entry.getKey().replace("source","candidate"), entry.getValue());
        }

        candFiles2TermsMap = newMap;
        */

        suspOverallTermsMap = tfidfGetOccurencesForMultipleFiles(tfSuspFragmentEntities);
        candOverallTermsMap = tfidfGetOccurencesForMultipleFiles(tfCandFragmentEntities);

        // Sort the maps by occurences (most occurences first)
        boolean SORT = true;
        if(SORT) {
            suspOverallTermsMap = suspOverallTermsMap.entrySet().stream().sorted(Comparator.comparingLong(stringtfidfTokenInfoEntry -> ((Map.Entry<String, tfidfTokenInfo>) stringtfidfTokenInfoEntry).getValue().getOccurences()).reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            candOverallTermsMap = candOverallTermsMap.entrySet().stream().sorted(Comparator.comparingLong(stringtfidfTokenInfoEntry -> ((Map.Entry<String, tfidfTokenInfo>) stringtfidfTokenInfoEntry).getValue().getOccurences()).reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            for(String wikidataID:candOverallTermsMap.keySet().stream().limit(10).collect(Collectors.toList())){
                tfidfTokenInfo tokenInfo = candOverallTermsMap.get(wikidataID);
                System.out.println(wikidataID+"   "+tokenInfo.getOccurences()+": "+tokenInfo.getLemmaAt(0)+"/"+tokenInfo.getLemmaAt(1));
            }
        }
    }

    private static Map<String, Map<String,tfidfTokenInfo>> tfidfGetOccurencesByFile(Map<String, List<SavedEntity>> tfidFragmentEntities){
        Map<String, Map<String, tfidfTokenInfo>> files2termsMap = new HashMap<>();
        for(String currentKey: tfidFragmentEntities.keySet()) {
            List<SavedEntity> currentTerms = tfidFragmentEntities.get(currentKey);
            Map<String, tfidfTokenInfo> currentFileTermsMap =  files2termsMap.get(currentKey);
            if(currentFileTermsMap==null){
                currentFileTermsMap = new ArrayMap<>();
                files2termsMap.put(currentKey, currentFileTermsMap);
            }
            for(SavedEntity term:currentTerms){
                String wikidataEntity = term.getWikidataEntityId();
                tfidfTokenInfo tokenInfo = currentFileTermsMap.get(wikidataEntity);
                if(tokenInfo == null){
                    tfidfTokenInfo myNewTokenInfo = new tfidfTokenInfo();
                    myNewTokenInfo.occurences = 1;
                    myNewTokenInfo.lemmas.add(term.getToken().getLemma());
                    currentFileTermsMap.put(wikidataEntity, myNewTokenInfo);
                }else{
                    tokenInfo.occurences += 1;
                    if(!tokenInfo.lemmas.contains(term.getToken().getLemma())){
                        tokenInfo.lemmas.add(term.getToken().getLemma());
                    }
                    currentFileTermsMap.put(wikidataEntity,tokenInfo); //necessary?
                }
            }
            // Sort by occurences
            currentFileTermsMap = currentFileTermsMap.entrySet().stream().sorted(Comparator.comparingLong(stringtfidfTokenInfoEntry -> ((Map.Entry<String, tfidfTokenInfo>) stringtfidfTokenInfoEntry).getValue().getOccurences()).reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            files2termsMap.put(currentKey, currentFileTermsMap); // necessary ?
        }
        return files2termsMap;
    }
    private static Map<String, tfidfTokenInfo> tfidfGetOccurencesForMultipleFiles(Map<String, List<SavedEntity>> tfidFragmentEntities) {
        Map<String, tfidfTokenInfo> termsMap = new HashMap<>();
        for(String currentKey: tfidFragmentEntities.keySet()){
            List<SavedEntity> currentTerms = tfidFragmentEntities.get(currentKey);
            for(SavedEntity term:currentTerms){
                String wikidataEntity = term.getWikidataEntityId();
                tfidfTokenInfo tokenInfo = termsMap.get(wikidataEntity);
                if(tokenInfo == null){
                    tfidfTokenInfo myNewTokenInfo = new tfidfTokenInfo();
                    myNewTokenInfo.occurences = 1;
                    myNewTokenInfo.lemmas.add(term.getToken().getLemma());
                    termsMap.put(wikidataEntity, myNewTokenInfo);
                }else{
                    tokenInfo.occurences += 1;
                    if(!tokenInfo.lemmas.contains(term.getToken().getLemma())){
                        tokenInfo.lemmas.add(term.getToken().getLemma());
                    }
                    termsMap.put(wikidataEntity,tokenInfo); //necessary?
                }
            }
        }
        return termsMap;
    }


    private static Map<String, List<SavedEntity>> tfidfGetSavedEntities2Files(OntologyBasedSimilarityAnalysis osa, List<File> inputFiles, boolean onlyPlagiarismRelated, HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation) throws Exception{
        Map<String, List<SavedEntity>> entitiesMap = new HashMap<>();
        for(File currentFile: inputFiles){

            List<SavedEntity> savedEntities = osa.preProcessExtendedInfo(currentFile.getPath(),null );
            if(!onlyPlagiarismRelated){
                entitiesMap.put(currentFile.getName().replace("source","candidate"),savedEntities);

            }else{
                // Mind that this only works for susp files currently
                String key = currentFile.getName().replace(".txt",".xml");
                List<PAN11PlagiarismInfo> currentPlagiarismInfos = plagiarismInformation.get(key);

                List<SavedEntity> savedEntitiesFiltered = new ArrayList<>();
                for(SavedEntity currentEntity:savedEntities){
                    if(SalvadorFragmentLevelEval.isPlagiarismRelated(currentEntity.getToken().getStartCharacter(), currentEntity.getToken().getEndCharacter(),currentPlagiarismInfos, false)){
                        savedEntitiesFiltered.add(currentEntity);
                    }
                }
                entitiesMap.put(currentFile.getName().replace("source","candidate"),savedEntitiesFiltered);
            }
        }
        return entitiesMap;
    }
}
