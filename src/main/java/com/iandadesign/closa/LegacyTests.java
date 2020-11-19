package com.iandadesign.closa;

import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.ScoringChunksCombined;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.model.WikidataEntity;
import com.iandadesign.closa.util.ExtendedLogUtil;
import com.iandadesign.closa.util.TokenUtil;
import com.iandadesign.closa.util.wikidata.WikidataEntityExtractor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Some tests for detailed analysis which can be executed from main.
 * Not directly unit tests. But proof of concept functionalities.
 *
 * @author Johannes Stegmüller
 */
public class LegacyTests {
    public static void mainContent(){
        String suspiciousPath = "src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/en/35157967/0.txt";    //changed to src/eval
        String candidateFolderPath = "src/eval/resources/com/iandadesign/closa/evaluation/test-bbc/en/";             //changed to src/eval
        File candidateFile = new File(candidateFolderPath);

        List<String> candidatePaths = FileUtils.listFiles(candidateFile, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .map(File::getPath)
                .collect(Collectors.toList());

        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        Map<String, Double> candidateScoresMap = osa.executeAlgorithmAndComputeScores(suspiciousPath, candidatePaths);


        System.out.println(candidateScoresMap);
        String test = "endpoint";
        //System.out.(candidateScoresMap);
    }

    public static void testExtendedLogs() throws Exception {
        String tag = "logsTest";
        ExtendedAnalysisParameters params = new ExtendedAnalysisParameters();
        // Do all preprocessing and cache it first (if already cached this will validate preprocessed number)
        OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
        osa.initializeLogger(tag, params); // this has to be done immediately after constructor
        ExtendedLogUtil logUtil = osa.getExtendedLogUtil();

        logUtil.logAndWriteStandard(false,"test1");
        logUtil.logAndWriteStandard(false,"test2","test2");
        logUtil.logAndWriteStandard(false,"test2","test2","test3");

        logUtil.logAndWriteStandard(true,"test");
        logUtil.logAndWriteStandard(true,"test","test");
        logUtil.logAndWriteStandard(true,"test","test","test","test","test");


        logUtil.logAndWriteStandard(true,"test", params);
        logUtil.logAndWriteStandard(true,"test", params, params);
    }

    public static void firstSimpleTest() {
         String text = "Joe Smith was born in California. " +
                 "In 2017, he went to Paris, France in the summer. " +
                 "His flight left at 3:00pm on July 10th, 2017. " +
                 "After eating some escargot for the first time, Joe said, \"That was delicious!\" " +
                 "He sent a postcard to his sister Jane Smith. " +
                 "After hearing about Joe's trip, Jane decided she might go to France one day.";


         List<List<Token>> tokensBySentence = TokenUtil.namedEntityTokenize(text, "en");


         // tokenize (as it is done in WikidataEntityExtractor.buildTokenEntitiesMap)
         List<Token> tokenList = TokenUtil.namedEntityTokenize(text, "en").stream()
                 .flatMap(List::stream).collect(Collectors.toList());

         // Wrapper around tokenize, entities should contain sentence mapping, TODO result map seems to be flattened without tokens here, maybe just map sentences to WikidataEntities
         List<WikidataEntity> myEntities = WikidataEntityExtractor.extractEntitiesFromText(
                 text,
                 "en",
                 Category.neutral);


         //TODO wait for a possible testrun with db here to see the Wikidata-objects
         System.out.println("done");

     }

    public static void tinyTest() throws Exception {
         String tag = "tinyTest"; // Tag to identify results file
         String suspiciousPath = "src/main/resources/TinyTest/suspicious_smallfile.txt";
         String candidateFolderPath = "src/main/resources/TinyTest/candidates/";
         File candidateFile = new File(candidateFolderPath);

         List<File> candidateFiles = new ArrayList<>(FileUtils.listFiles(candidateFile, TrueFileFilter.TRUE, TrueFileFilter.TRUE));
         ExtendedAnalysisParameters params = new ExtendedAnalysisParameters();
         OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
         try {
             osa.executeAlgorithmAndComputeScoresExtendedInfo(suspiciousPath, candidateFiles, params, osa.getExtendedLogUtil().getDateString(), null);
         }catch(Exception ex){
             System.err.println(ex.toString());
         }

     }

    public static void pan2011Test() throws Exception {
         String tag = "PAN2011Test2"; // Tag to identify results file
         // String suspiciousPath = "src/main/resources/PAN2011Test2/suspicious-document00020.txt";
         // String candidateFolderPath = "src/main/resources/PAN2011Test2/candidates/";
         String suspiciousPath = "src/main/resources/PAN2011Test2/suspicious-document00034.txt";
         String candidateFolderPath = "src/main/resources/PAN2011Test2/candidates/";
         File candidateFile = new File(candidateFolderPath);

         List<File> candidateFiles = new ArrayList<>(FileUtils.listFiles(candidateFile, TrueFileFilter.TRUE, TrueFileFilter.TRUE));
         ExtendedAnalysisParameters params = new ExtendedAnalysisParameters();
         OntologyBasedSimilarityAnalysis osa = new OntologyBasedSimilarityAnalysis();
         try {
             osa.executeAlgorithmAndComputeScoresExtendedInfo(suspiciousPath, candidateFiles, params, osa.getExtendedLogUtil().getDateString(),null);
         }catch(Exception ex){
             System.err.println(ex.toString());
         }
     }

    public static void testSubtokenAssignment() {
         //Like it is done in WikidataEntityExtractor -create a small test here for correct assignment with subtokenListsize = 3
        /*
         List<Token> subtokens = subtokensList.get(i);

         boolean isWhitespaceSeparatedLanguage = !(languageCode.equals("zh") || languageCode.equals("ja") ||
                 languageCode.equals("ar"));

         // build token from sub-tokens TODO: JS here make the correct sentence assignment for tokens
         Token token = new Token(subtokens, isWhitespaceSeparatedLanguage ? " " : "");
         */
         // TODO add assert and add these cases to test/TokenUtil
        Token testToken = new Token("ttt");
        testToken.setSentenceNumber(1);
        Token testToken2 = new Token("ttt");
        testToken2.setSentenceNumber(3);
        Token testToken3 = new Token("ttt");
        testToken3.setSentenceNumber(3);
        List<Token> myMergeTokens = new ArrayList<>(Arrays.asList(testToken, testToken2, testToken3));
        int adjustedSentenceNumber =  TokenUtil.getAdjustedSentenceNumber(myMergeTokens, "histogramMean");
        System.out.println(adjustedSentenceNumber);

     }

    public static void testXMLResultWriter() {
        try {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
            LocalDateTime now = LocalDateTime.now();
            String dateString = dtf.format((now));
            String cosineResultsPath = Paths.get("D:\\CL_OSA_caching", "preprocessed_extended",
                    "results_comparison", "PAN2011Test".concat("_").concat(dateString).concat(".xml"))
                    .toAbsolutePath().toString();
            ScoringChunksCombined scoringChunksCombined = new ScoringChunksCombined(0.2, 0.5, 2, 2, 50, false, false, false);
            scoringChunksCombined.setCurrentDocuments("mySuspiciousDocument.txt","myCandidateDocument.txt");
            scoringChunksCombined.writeResultAsXML(cosineResultsPath);
            scoringChunksCombined.prettifyXML(cosineResultsPath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}