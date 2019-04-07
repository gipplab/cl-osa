package com.fabianmarquart.closa.util.wikidata;


import com.fabianmarquart.closa.model.Dictionary;
import com.fabianmarquart.closa.model.Token;
import com.fabianmarquart.closa.model.WikidataEntity;
import com.fabianmarquart.closa.util.TokenUtil;

import java.util.*;
import java.util.stream.Collectors;

import static com.fabianmarquart.closa.util.wikidata.WikidataDumpUtil.*;

/**
 * Created by Fabian Marquart on 2018/08/03.
 */
public class WikidataDisambiguator {


    /**
     * Gets the entity with the numerically smallest id.
     *
     * @param entities the entities to be disambiguated.
     * @return the disambiguated entity.
     */
    public static WikidataEntity disambiguateBySmallestId(List<WikidataEntity> entities) {
        return entities.stream()
                .min(Comparator.comparing(entity -> Integer.parseInt(entity.getId().substring(1))))
                .orElse(null);
    }

    /**
     * Disambiguates a list of entities according to the given text.
     *
     * @param entities entities to be disambiguated.
     * @param text     text for context
     * @return the disambiguated entity.
     */
    @Deprecated
    public static WikidataEntity graphDisambiguate(List<WikidataEntity> entities, String text, String languageCode) {
        if (entities.size() == 1) {
            return entities.get(0);
        }

        // replace instances in entities by their classes
        entities = entities.stream()
                .map(entity -> {
                    if (isInstance(entity)) {
                        return instanceOf(entity);
                    } else {
                        return Collections.singletonList(entity);
                    }
                }).flatMap(List::stream)
                .collect(Collectors.toList());

        // stores current iteration's subclasses and instance of the entities,
        // initialize with empty parents and classes.
        Map<WikidataEntity, Set<WikidataEntity>> currentNextLevel = entities.stream()
                .collect(Collectors.toMap(entity -> entity, entity -> new HashSet<>(Collections.singletonList(entity))));

        // stores each entity that has been disambiguated
        Set<WikidataEntity> disambiguatedEntities = new HashSet<>();


        // while the number of disambiguated entities is not exactly one
        while (disambiguatedEntities.size() != 1 && !(currentNextLevel.values().isEmpty() && disambiguatedEntities.isEmpty())) {

            // move up by one level:
            // update the parents & classes by replacing them by their own parents & classes
            currentNextLevel = currentNextLevel.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey,
                            entry -> entry.getValue().stream()
                                    .map(WikidataSparqlUtil::subclassOf)
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toSet())))
                    .entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            System.out.println(currentNextLevel.values());


            // overwrite the set of disambiguations with all current entities that are contained in the text
            disambiguatedEntities = currentNextLevel.entrySet()
                    .stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().stream()
                            // filter entries whose parent classes are not in the text
                            .filter(entity -> entity.getLabels() != null
                                    && entity.getLabels().containsKey(languageCode)
                                    && text.contains(entity.getLabels().get(languageCode))
                            ).collect(Collectors.toSet())))
                    .filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());


            System.out.println("Disambiguated entities = " + disambiguatedEntities);
        }

        if (disambiguatedEntities.size() == 1) {
            return disambiguatedEntities.stream().findFirst().get();
        } else {
            return disambiguateBySmallestId(entities);
        }
    }


    /**
     * Disambiguates to the entity whose ancestor entity labels occur most often in the text.
     *
     * @param entities     entities to disambiguate
     * @param text         the text where the entities come from
     * @param languageCode the text and entity language code
     * @return the disambiguated entity
     */
    public static WikidataEntity ancestorCountDisambiguate(List<WikidataEntity> entities, String text, String languageCode) {
        // retrieve all ancestors per entity
        Map<WikidataEntity, Set<WikidataEntity>> entitiesSubclassOf = new HashSet<>(entities).stream()
                .collect(Collectors.toMap(entity -> entity,
                        entity -> new HashSet<>(getAllAncestors(entity))));

        // count the occurrences for all ancestors in the text per entity
        Map<WikidataEntity, Integer> entityOccurrences = new HashMap<>();

        entitiesSubclassOf.forEach((key, value) -> {
            int occurrences = Math.toIntExact(value.stream()
                    .filter(entity -> entity.getLabels() != null && entity.getLabels().containsKey(languageCode))
                    .filter(entity -> text.contains(entity.getLabels().get(languageCode)))
                    .count());

            entityOccurrences.put(key, occurrences);
        });

        // the entity whose ancestors appear in the greatest number in the text wins
        long max = entityOccurrences.values()
                .stream()
                .max(Comparator.naturalOrder())
                .get();

        Set<WikidataEntity> maxKeys = entityOccurrences.entrySet()
                .stream()
                .filter(entry -> entry.getValue() == max)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (maxKeys.size() == 1) {
            return maxKeys.iterator().next();
        } else {
            return disambiguateBySmallestId(new ArrayList<>(maxKeys));
        }

    }

    /**
     * Disambiguates a list of entities according to the given text.
     *
     * @param entities entities to be disambiguated.
     * @param text     text for context
     * @return the disambiguated entity.
     */
    public static WikidataEntity disambiguateByDescription(List<WikidataEntity> entities, String text, String languageCode) {
        // TODO: disambiguate by alias

        if (entities.stream()
                .allMatch(entity -> entity.getDescriptions() == null || !entity.getDescriptions().containsKey(languageCode))) {
            throw new IllegalArgumentException("The entities need to contain a description to disambiguate from.");
        }

        List<Token> textTokens = TokenUtil.tokenize(text, languageCode);
        textTokens.forEach(Token::toLowerCase);
        textTokens = TokenUtil.removeStopwords(textTokens, languageCode);
        textTokens = TokenUtil.stem(textTokens, languageCode);

        Map<String, List<String>> idTokensMap = new HashMap<>();

        entities.stream()
                .filter(entity -> entity.getDescriptions() != null || entity.getDescriptions().containsKey(languageCode))
                .forEach(entity -> {

                    List<Token> descriptionTokens = TokenUtil.tokenize(entity.getDescriptions().get(languageCode), languageCode);
                    descriptionTokens.forEach(Token::toLowerCase);
                    descriptionTokens = TokenUtil.removeStopwords(descriptionTokens, languageCode);
                    descriptionTokens = TokenUtil.stem(descriptionTokens, languageCode);

                    idTokensMap.put(entity.getId(), descriptionTokens.stream().map(Token::getToken).collect(Collectors.toList()));
                });

        Dictionary<String> dictionary = new Dictionary<>(idTokensMap);

        List<String> queryTerms = textTokens.stream().map(Token::getToken).collect(Collectors.toList());

        String matchingId = dictionary.query(queryTerms)
                .entrySet()
                .stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .get()
                .getKey();

        return entities.stream().filter(entity -> entity.getId().equals(matchingId)).findFirst().orElse(entities.get(0));
    }


}
