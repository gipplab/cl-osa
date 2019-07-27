package com.iandadesign.closa.classification;

import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.util.ConceptUtil;
import com.iandadesign.closa.util.TokenUtil;
import de.daslaboratorium.machinelearning.classifier.Classifier;
import de.daslaboratorium.machinelearning.classifier.bayes.BayesClassifier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TextClassifierUtilEval {


    @Test
    public void testConstructor() {
        TextClassifier classifier = new TextClassifier(Arrays.asList("fiction", "neutral"));
    }

    @Test
    public void getClassificationCorpusFromWikipedia() {
        TextClassifier textClassifier = new TextClassifier();

        List<String> languages = Arrays.asList("zh", "en", "fr", "es", "ja", "de");

        Map<String, String> categoriesByTitle = new HashMap<>();

        // Wikipedia training articles
        Arrays.asList(
                "WHI3", "CHON", "Anesthesia", "Peptide", "Alpha helix", "Triosephosphate isomerase",
                "Triosephosphate isomerase", "Hepatitis B", "Immunoglobulin G", "Epileptic seizure",
                "Saccharomyces cerevisiae", "Carboxylic acid", "Papillary muscle", "Antibiotic",
                "Adenosine triphosphate"
        ).forEach(title -> categoriesByTitle.put(title, "biology"));

        Arrays.asList(
                "Hollywood", "Film industry", "Television show", "Cinematography", "The Rolling Stones", "Comedy"
        ).forEach(title -> categoriesByTitle.put(title, "fiction"));

        Arrays.asList(
                "World Trade Organization", "Investment management", "Finance", "United States presidential election",
                "United States Constitution"
        ).forEach(title -> categoriesByTitle.put(title, "neutral"));


        // for different languages
        for (String language : languages) {
            // Create a new bayes classifier with string categories and string features.
            Classifier<String, String> bayesClassifier = new BayesClassifier<>();
            bayesClassifier.setMemoryCapacity(Integer.MAX_VALUE);

            // for all titles per category
            for (Map.Entry<String, String> entry : categoriesByTitle.entrySet()) {
                String title = entry.getKey();
                String category = entry.getValue();

                String pageId = ConceptUtil.getPageIdByTitle(title, "en");
                String pageIdInLanguage = ConceptUtil.getPageIdInLanguage(pageId, "en", language);

                if (pageIdInLanguage == null) {
                    continue;
                }

                String text = ConceptUtil.getPageContent(pageIdInLanguage, language);
                List<String> tokens = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(text, language)
                        .stream()
                        .map(Token::getToken)
                        .collect(Collectors.toList());

                try {
                    String path = String.format("src/main/resources/corpus/categorization/%s/%s/%s.txt", language, category, title);
                    FileUtils.writeLines(new File(path), tokens);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                bayesClassifier.learn(category, tokens);
            }

            textClassifier.getClassifierMap().put(language, bayesClassifier);
        }
    }


}
