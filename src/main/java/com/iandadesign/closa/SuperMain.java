package com.iandadesign.closa;

import com.iandadesign.closa.OntologyBasedSimilarityAnalysis;
import com.iandadesign.closa.classification.TextClassifier;
import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.TokenUtil;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SuperMain {

   public static void main(String[] args) {
       //String suspiciousPath = "~/documents/en/35157967/0.txt";
       //String candidateFolderPath = "~/documents/ja/";
       //D:\AA_ScienceProject\Wikidata_vs_CharacterNGram\closa\src\eval\resources\com\iandadesign\closa\evaluation\test-bbc\ja-t
       //Paths from test
       firstSimpleTest();
       //mainContent();
   }
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


        System.out.println("done");

    }

}
