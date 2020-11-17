package com.iandadesign.closa.util;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.PAN11EvaluationSetEval;
import com.iandadesign.closa.model.ExtendedAnalysisParameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Verification functions for corpus length for PAN11-Corpus.
 * @author Johannes Stegmüller
 */
public class PAN11CorpusVerification {
    public static void verifyNumberNonEnglishDocs(){
        String tag = "evalPAN2011All"; // Identifier for logs ...
        String toplevelPathSuspicious = PAN11EvaluationSetEval.pathPrefix.concat("/suspicious-document/");
        String toplevelPathCandidates = PAN11EvaluationSetEval.pathPrefix.concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory
        ExtendedAnalysisParameters params;
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();

        // ..
        List<File> candidateFilesLangCount = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".xml");
        PAN11XMLParser pan11XMLParser = new PAN11XMLParser();
        List<String> usedLanguages = new ArrayList<>();
        int espanolDocs = 0;
        int germanDocs = 0;
        for(File suspFileXML: candidateFilesLangCount){
            // Read XML File
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);
            if(xmlInfo.language.equals("es")){
                espanolDocs++;
            }
            if(xmlInfo.language.equals("de")){
                germanDocs++;
            }
            System.out.println("read");
            if(xmlInfo!=null && !usedLanguages.contains(xmlInfo.language)){
                usedLanguages.add(xmlInfo.language);
            }
        }
        // From xml its:
        // its 202 espanol docs
        // its 471 german docs
        // By langdet its:
        // its 471 german docs
        // its 202 spanish docs
        // ..
    }

    public static void verifyNumberNonEnglishSusp(){
        String tag = "PAN11verifySalvadorPartitions"; // Identifier for logs ...

        String lpathPrefix = "D:\\AA_ScienceProject\\Data\\pan-plagiarism-corpus-2011\\pan-plagiarism-corpus-2011\\";

        String toplevelPathSuspicious = lpathPrefix.concat("external-detection-corpus\\").concat("/suspicious-document/");
        String toplevelPathCandidates = lpathPrefix.concat("external-detection-corpus\\").concat("/source-document/");


        //  (26939 - (9506/2)) / 2 = 11093 is the number of files in each directory
        ExtendedAnalysisParameters params;
        try{
            params = new ExtendedAnalysisParameters();
        }catch(Exception ex){
            System.err.println("Problem initializing params: "+ex);
            return;
        }

        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();

        // ..
        List<File> suspiciousFilesLangCount = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathSuspicious, params, false, ".xml");
        List<File> candidateFilesLangCount = PAN11FileUtil.getTextFilesFromTopLevelDir(toplevelPathCandidates, params, true, ".xml");

        PAN11XMLParser pan11XMLParser = new PAN11XMLParser();
        List<String> usedLanguages = new ArrayList<>();
        int englishDocsCorresponding = 0;
        int shortCases= 0;
        int mediumCases = 0;
        int longCases = 0;
        int overallCases = 0 ;
        int overallXMLOk=0;

        int translationTypeCount=0;
        int manualObfuscation=0;
        int automaticObfuscation=0;
        int othercount = 0;
        long allCasesOfPlagiarism=0;

        for(File suspFileXML: suspiciousFilesLangCount){
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);
            if(xmlInfo.language!=null){
                overallXMLOk++;
            }
            for(PAN11PlagiarismInfo plaginfo:xmlInfo.plagiarismInfos) {
                if(plaginfo.getSourceLanguage().equals("es") || plaginfo.getSourceLanguage().equals("de")) { //"es" //"de""

                    if (plaginfo.getType().equals("translation")) {
                        translationTypeCount++;
                    } else {
                        othercount++;
                    }

                }
                if(plaginfo.getManualObfuscation()!=null && plaginfo.getManualObfuscation()){
                    manualObfuscation++;
                }
                if(plaginfo.getManualObfuscation()!=null && !plaginfo.getManualObfuscation()){
                    automaticObfuscation++;
                }
                allCasesOfPlagiarism++;
            }
        }

        //61 064 all cases of plagiarism -> it is only 49261 (+11443 for intrinsic) = 60704 (difference of 360 cases)
        // The dataset has 49261 cases by searching 'plagiarism' in xml files in notepad++
        // 26 939 documents supposed -> 50/50 -> 11093 are there (its less because of intrinsic corpus missing)
        // Salvador says its 5164 +

        //Checklist:
        // Filecount (xml): 26939 supposed, actual: 11093  * 2 + 4753 (int,susp)  = 26939 -> files are ok
        // Plagiarism (based on suspicious) 61 064 supposed, actual: 11443 intrinsic + 49261 external = 60704 (==> 360 cases missing)
        List<String> espEnSusp = new ArrayList<>();
        List<String> espEnSource = new ArrayList<>();

        for(File suspFileXML: suspiciousFilesLangCount){
            boolean hasValidLanguagePair = false;
            // Read XML File
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(suspFileXML);
            if(xmlInfo.language.equals("en")){

                for(PAN11PlagiarismInfo plaginfo:xmlInfo.plagiarismInfos){

                    if(plaginfo.getSourceLanguage().equals("es")){ //|| plaginfo.getSourceLanguage().equals("de")){ //"es" //"de""
                        if(!espEnSource.contains(plaginfo.getSourceReference())) {
                            // This seems correct by logic
                            espEnSource.add(plaginfo.getSourceReference());
                        }
                        if(!hasValidLanguagePair){
                            // This gives the correct number for salvador
                            espEnSusp.add(suspFileXML.getName());

                            englishDocsCorresponding++;
                            hasValidLanguagePair=true;
                        }

                        overallCases ++;
                        String caseLength = plaginfo.getCaseLengthSource();
                        if(caseLength.equals(PAN11PlagiarismInfo.CaseLength.LONG)){
                            longCases++;
                        }else if(caseLength.equals(PAN11PlagiarismInfo.CaseLength.MEDIUM)){
                            mediumCases++;
                        }else if(caseLength.equals(PAN11PlagiarismInfo.CaseLength.SHORT)){
                            shortCases++;
                        }else{
                            System.out.println("error case length!");
                        }
                    }
                }
            }else {
                System.out.println("asd");
            }
        }
        // The selected (de+es) file counts are ok here, so the basic filter is working.
        // Case lengths Salvador:     s1506    m2118     l1951 = SUM -> 5575
        //                             0,27    0,379     0,3499         (1506/5575etc)
        // Case lengths with source:  s1136    m2105     l1870 = SUM -> 5142
        //                              0,22   0,409      0,36
        // Case lengths with this:    s1377    m1936     l1829 = SUM -> 5142
        //                              0,2677  0,376      0,355
        // 433 cases missing
        // This is the number of Translated manual obfuscation 433
        // Most likely its with 'this'cases


        int germanDocsCand = 0;
        int spanishDocsCand = 0;
        for(File candFileXML: candidateFilesLangCount){
            PAN11XMLInfo xmlInfo = pan11XMLParser.parseXMLfile(candFileXML);

            if(xmlInfo.language.equals("es")){
                spanishDocsCand++;
            }
            if(xmlInfo.language.equals("de")){
                germanDocsCand++;
            }

        }
        System.out.println("done");
        //partition en-de: en: 251/251 de: 348/348 -> ok
        //partition en-es: en:  /304  es: /202
        //partition en-es: en:  304/304  es: 199/202 --> 2 candidates are not mapped

    }
}
