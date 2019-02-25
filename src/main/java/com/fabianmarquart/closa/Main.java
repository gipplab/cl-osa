package com.fabianmarquart.closa;

import com.fabianmarquart.closa.model.WikidataEntity;
import com.fabianmarquart.closa.util.TextClassificationUtil;
import com.fabianmarquart.closa.util.TokenUtil;
import com.fabianmarquart.closa.util.wikidata.WikidataEntityExtractor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Need two parameters.");
        }

        String inputPath = args[0];
        String outputPath = args[1];

        try {
            String inputText = FileUtils.readFileToString(new File(inputPath), StandardCharsets.UTF_8);
            List<WikidataEntity> extractedEntities = WikidataEntityExtractor.extractEntitiesFromText(inputText, TokenUtil.detectLanguage(inputText));

            FileUtils.writeLines(new File(outputPath), extractedEntities.stream().map(WikidataEntity::getId).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
