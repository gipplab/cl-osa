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
    Map<String, Map<String, tfidfTokenInfo>> suspFiles2TermsMap;
    Map<String, Map<String, tfidfTokenInfo>> candFiles2TermsMap;
    Map<String, tfidfTokenInfo> suspOverallTermsMap;
    Map<String, tfidfTokenInfo> candOverallTermsMap;

    public void createRelevantMaps(HashMap<String, List<PAN11PlagiarismInfo>> plagiarismInformation, OntologyBasedSimilarityAnalysis osa, List<File> candidateFiles, List<File> suspiciousFiles) throws Exception {
        Map<String, List<SavedEntity>> tfSuspFragmentEntities = tfidfMapHolder.tfidfGetSavedEntities2Files(osa, suspiciousFiles, true, plagiarismInformation);
        Map<String, List<SavedEntity>> tfCandFragmentEntities = tfidfGetSavedEntities2Files(osa, candidateFiles, false, null);


        suspFiles2TermsMap = tfidfGetOccurencesByFile(tfSuspFragmentEntities);
        candFiles2TermsMap = tfidfGetOccurencesByFile(tfCandFragmentEntities);

        suspOverallTermsMap = tfidfGetOccurencesForMultipleFiles(tfSuspFragmentEntities);
        candOverallTermsMap = tfidfGetOccurencesForMultipleFiles(tfCandFragmentEntities);

        // Sort the maps by occurences (most occurences first)
        boolean SORT = true;
        if(SORT) {
            suspOverallTermsMap = suspOverallTermsMap.entrySet().stream().sorted(Comparator.comparingLong(stringtfidfTokenInfoEntry -> ((Map.Entry<String, tfidfTokenInfo>) stringtfidfTokenInfoEntry).getValue().getOccurences()).reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            candOverallTermsMap = candOverallTermsMap.entrySet().stream().sorted(Comparator.comparingLong(stringtfidfTokenInfoEntry -> ((Map.Entry<String, tfidfTokenInfo>) stringtfidfTokenInfoEntry).getValue().getOccurences()).reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            for(tfidfTokenInfo tokenInfo:candOverallTermsMap.values()){
                System.out.println(tokenInfo.getOccurences()+": "+tokenInfo.getLemmaAt(0)+"/"+tokenInfo.getLemmaAt(1));
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
                entitiesMap.put(currentFile.getName(),savedEntities);

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
                entitiesMap.put(currentFile.getName(),savedEntitiesFiltered);
            }
        }
        return entitiesMap;
    }
}
