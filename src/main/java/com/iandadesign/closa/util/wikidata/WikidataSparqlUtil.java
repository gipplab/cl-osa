package com.iandadesign.closa.util.wikidata;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.model.WikidataEntity;
import com.iandadesign.closa.util.ExtendedLogUtil;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The WikidataSparqlUtil talks to the Wikidata SPARQL endpoint via HTTP.
 * <p>
 * Several types of queries are supported:
 * - entity extraction by label
 * - entity extraction by id
 * - similarity measures (Wu and Palmer, Jaccard)
 * - distance measures (pairwise, to root, and to specified ancestor)
 * - sub class and parent class (and ancestor class) extraction
 * - instance class and instantiating class extraction
 * - property extraction
 * - checking of various properties related to hierarchy: location, human, organization, number, natural number,
 * human language, creative work, gene, instance class, instantiating class.
 * - general SPARQL sendQuery
 * <p>
 * Created by Fabian Marquart on 2018/08/03.
 */
public class WikidataSparqlUtil {

    private static String wikidataSparqlEndpoint;
    private static boolean logSparqlRequests;
    private static String sparqlErrorLogPath;

    private static final String[] wikidataPrefixes = {"PREFIX wd: <http://www.wikidata.org/entity/>",
            "PREFIX wds: <http://www.wikidata.org/entity/statement/>",
            "PREFIX wdv: <http://www.wikidata.org/value/>",
            "PREFIX wdt: <http://www.wikidata.org/prop/direct/>",
            "PREFIX wikibase: <http://wikiba.se/ontology#>",
            "PREFIX p: <http://www.wikidata.org/prop/>",
            "PREFIX ps: <http://www.wikidata.org/prop/statement/>",
            "PREFIX pq: <http://www.wikidata.org/prop/qualifier/>",
            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>",
            "PREFIX bd: <http://www.bigdata.com/rdf#>"};

    // entity is not exactly the root of Wikidata's taxonomy, but there seems no better candidate.
    // at the time of this writing (2018/10/29):
    //
    // instanceOf(entity) = variable-order metaclass,
    // subclassOf(variable-order metaclass) = metaclass,
    // instanceOf(metaclass) = concept,
    // subclassOf(concept) = abstract object,
    // instanceOf(abstract object) = variable-order metaclass
    private static final WikidataEntity rootEntity = new WikidataEntity("Q35120", "entity");

    private static final WikidataEntity location = new WikidataEntity("Q17334923", "location");
    private static final WikidataEntity human = new WikidataEntity("Q5", "human");
    private static final WikidataEntity organization = new WikidataEntity("Q43229", "organization");

    private static final WikidataEntity number = new WikidataEntity("Q11563", "number");
    private static final WikidataEntity naturalNumber = new WikidataEntity("Q21S199", "natural number");

    private static final WikidataEntity humanLanguage = new WikidataEntity("Q20162172", "human language");

    private static final WikidataEntity creativeWork = new WikidataEntity("Q17537576", "creative work");
    private static final WikidataEntity gene = new WikidataEntity("Q7187", "gene");

    private static final String countryProperty = "P17";

    static {
        try {
            loadEndpointFromConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read Wikidata SPARQL properties.
     *
     * @throws IOException When property file could not be loaded.
     */
    private static void loadEndpointFromConfig() throws IOException {
        InputStream inputStream = null;

        try {
            Properties properties = new Properties();
            String propFileName = "config.properties";
            String propFileLocalName = "config-local.properties";

            // switch to config-local if it exists
            if (WikidataDumpUtil.class.getClassLoader().getResource(propFileLocalName) != null) {
                inputStream = WikidataDumpUtil.class.getClassLoader().getResourceAsStream(propFileLocalName);

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath");
                }
            } else {
                inputStream = WikidataDumpUtil.class.getClassLoader().getResourceAsStream(propFileName);

                if (inputStream != null) {
                    properties.load(inputStream);
                } else {
                    throw new FileNotFoundException("Property file '" + propFileName + "' not found in the classpath");
                }
            }

            // get the property value and print it out
            wikidataSparqlEndpoint = properties.getProperty("wikidata_sparql_endpoint");
            logSparqlRequests = Boolean.parseBoolean(properties.getProperty("log_sparql_requests"));
            sparqlErrorLogPath = properties.getProperty("errorlog_path");

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * Just a simple logger for logging SPARQL communication.
     * Can be toggled by corresponding configuration property.
     * @param message message to be logged.
     */
    private static void logSimple(String message){
        if(logSparqlRequests) {
            System.out.println(message);
        }
    }

    /**
     * Retrieves the entity matching the id from the Wikidata SPARQL endpoint.
     *
     * @param id : the id to find
     * @return the result as Wikidata entity.
     */
    public static WikidataEntity getEntityById(String id) {
        String queryString = String.format("SELECT DISTINCT ?label ?description WHERE \n" +
                        "{\n" +
                        "   wd:%1$s rdfs:label ?label . \n" +
                        "   wd:%1$s schema:description ?description . \n" +
                        "\n" +
                        "   SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],en\". }\n" +
                        "   FILTER (langMatches( lang(?label), \"EN\" ))\n" +
                        "   FILTER (langMatches( lang(?description), \"EN\" ))\n" +
                        "}\n" +
                        "LIMIT 1",
                id);

        Map<String, List<String>> queryResult = sendQuery(queryString);

        if (queryResult.isEmpty()) {
            return new WikidataEntity(id);
        }

        // read data
        Map<String, String> labels = new HashMap<>();
        Map<String, String> descriptions = new HashMap<>();

        if (!queryResult.get("label").isEmpty()) {
            labels.put("en", queryResult.get("label").get(0));
        }

        if (!queryResult.get("description").isEmpty()) {
            descriptions.put("en", queryResult.get("description").get(0));
        }

        return new WikidataEntity(id, labels.get("en"), labels, descriptions);
    }

    /**
     * Retrieves the label matching the entity from the Wikidata SPARQL endpoint.
     *
     * @param entityId     : the entityId to sendQuery.
     * @param languageCode : the label language.
     * @return the label.
     */
    public static String getEntityLabelById(String entityId, String languageCode) {
        String queryString = String.format("SELECT DISTINCT * WHERE {\n" +
                        "  wd:%1$s rdfs:label ?label . \n" +
                        "  FILTER (langMatches( lang(?label), \"%2$s\" ) )\n" +
                        "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%2$s\". }    \n" +
                        "}\n" +
                        "LIMIT 1",
                entityId,
                languageCode);

        return sendQuery(queryString).get("label").get(0);
    }


    /**
     * Retrieves the entity matching the label from the Wikidata SPARQL endpoint.
     *
     * @param label        : the label to find
     * @param languageCode : the label language.
     * @return the results as Wikidata entities.
     */
    public static List<WikidataEntity> getEntitiesByLabel(String label, String languageCode) {
        String queryString = String.format("PREFIX schema: <http://schema.org/>\n" +
                        "\n" +
                        "SELECT DISTINCT ?item ?itemDescription WHERE {\n" +
                        "  ?item ?label \"%1$s\"@%2$s.\n" +
                        "  ?article schema:about ?item.\n" +
                        "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"%2$s\". }\n" +
                        "}",
                label,
                languageCode);

        Map<String, List<String>> queryResult = sendQuery(queryString);

        return queryResult.containsKey("item") ?
                queryResult.get("item")
                        .stream()
                        .map(itemString -> new WikidataEntity(itemString.split("entity/")[1]))
                        .collect(Collectors.toList()) :
                Collections.emptyList();
    }


    /**
     * Retrieves the entity matching the token from MongoDB:
     * - tokens that are named entities only retrieve instances from Wikidata
     * - tokens that are not named entities only retrieve classes from Wikidata
     *
     * @param token        : the token to find, by lemma.
     * @param languageCode : the label language.
     * @param category     : the text's category from which the token was taken.
     * @return the results as Wikidata entities.
     */
    public static List<WikidataEntity> getEntitiesByToken(Token token, String languageCode, Category category) {
        if (token.getLemma() == null) {
            throw new IllegalArgumentException("The token lemma is null");
        } else if (token.getLemma().equals("")) {
            return new ArrayList<>();
        }

        List<WikidataEntity> entities;

        String queryLemma = token.getLemma();

        // count results
        List<WikidataEntity> results = new ArrayList<>();

        List<WikidataEntity> queriedEntities = getEntitiesByLabel(queryLemma, languageCode);

        // 1 decide between lemma or token
        for (int i = 0; i < 2; i++) {
            // collect the results
            for (WikidataEntity currentEntity : queriedEntities) {
                results.add(currentEntity);

                if (token.getPartOfSpeech().equals("CD")) {
                    if (isNaturalNumber(new WikidataEntity(currentEntity.getId()))) {
                        break;
                    }
                }
            }

            if (results.size() > 250) {
                // too many results means the search was too broad, use exact match
                return new ArrayList<>();
            }

            // if the lemma sendQuery is successful, exit the loop
            if (results.size() > 0) {
                break;
            }

            if (results.size() == 0) {
                // if no results are found for lemma, assume wrong lemmatization and sendQuery token instead
                if(!token.getToken().toLowerCase().equals(token.getLemma().toLowerCase())){
                    // only do another request if token and lemma differ to prevent redundant requests
                    queriedEntities = getEntitiesByLabel(token.getToken(), languageCode);
                }else{
                    //System.out.println("same lemma and token, continue with no result");
                }
            }
        }

        // 2 consider the sendQuery result itself
        entities = results.stream()
                .filter(entity -> entity.getId().contains("Q"))
                .map(entity -> {
                    entity = getEntityById(entity.getId());
                    entity.setOriginalLemma(token.getLemma());
                    return entity;
                })
                // filter if no label and or no description
                .filter(entity ->
                        entity.getLabels() != null && !entity.getLabels().isEmpty()
                                && entity.getDescriptions() != null && !entity.getDescriptions().isEmpty())
                .filter(entity ->
                        !category.equals(Category.biology)
                                || results.size() <= 50
                                || entity.getDescriptions().getOrDefault("en", "").contains("protein")
                                || entity.getDescriptions().getOrDefault("en", "").contains("gene"))
                .filter(entity ->
                        results.size() <= 1 // no filter if only one result
                                || !token.isNamedEntity()
                                || (isInstance(entity)
                                || (token.isLocation() && isLocation(entity))
                                || (token.isPerson() && isHuman(entity))
                                || (token.isOrganization() && isOrganization(entity))
                        ))
                .filter(entity ->
                        results.size() <= 1 // no filter if only one result
                                || !token.getPartOfSpeech().equals("CD") || isNaturalNumber(entity))
                .filter(entity -> !isCreativeWork(entity))
                .map(entity -> {
                    // map languages to their respective country
                    if (isHumanLanguage(entity)) {
                        List<WikidataEntity> propertyValues = getProperty(entity, countryProperty);
                        if (!propertyValues.isEmpty()) {
                            entity.setId(propertyValues.get(0).getId());
                            entity.setLabel(propertyValues.get(0).getLabel());
                            return entity;
                        }
                    }
                    return entity;
                })
                .collect(Collectors.toList());

        return entities;
    }


    /**
     * Get property value for a given entity and property.
     *
     * @param entity     entity
     * @param propertyId property id
     * @return Property value for a given entity and property..
     */
    public static List<WikidataEntity> getProperty(WikidataEntity entity, String propertyId) {
        if (!propertyId.contains("P")) {
            throw new IllegalArgumentException("The entity has to be a property.");
        }

        String queryString = String.format("SELECT ?prop_id ?prop_label ?prop_val_label ?entity_item WHERE {\n" +
                "  VALUES (?entity) {\n" +
                "    (wd:%1$s)\n" +
                "  }\n" +
                "  ?entity wdt:%2$s ?entity_item.\n" +
                "  ?wd wikibase:directClaim wdt:%2$s.\n" +
                "  ?wd rdfs:label ?prop_label.\n" +
                "  OPTIONAL {\n" +
                "    ?entity_item rdfs:label ?prop_val.\n" +
                "    FILTER((LANG(?prop_val)) = \"en\")\n" +
                "  }\n" +
                "  BIND(COALESCE(?prop_val, ?entityItem) AS ?prop_val_label)\n" +
                "  FILTER((LANG(?prop_label)) = \"en\")\n" +
                "}", entity.getId(), propertyId);

        Map<String, List<String>> resultMap = sendQuery(queryString);

        if (resultMap.containsKey("entity_item")) {
            return resultMap.get("entity_item").stream()
                    .map(propertyValueId -> {
                        if (propertyValueId.contains("prop/direct/")) {
                            propertyValueId = propertyValueId.split("prop/direct/")[1];
                        } else if (propertyValueId.contains("entity/")) {
                            propertyValueId = propertyValueId.split("entity/")[1];
                        }
                        return new WikidataEntity(propertyValueId);
                    }).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * Gets a list of the direct ancestors in the Wikidata ontology.
     * All instances of these items are instances of those items; this item is a class (subset) of that item.
     * Not to be confused with P31 (instance of)
     *
     * @param entity entity
     * @return the list of parent entities.
     */
    public static List<WikidataEntity> subclassOf(WikidataEntity entity) {

        String queryString = String.format("SELECT ?parent ?parentLabel WHERE {\n" +
                        "   ?parent ^wdt:P279 wd:%1$s .\n" +
                        "   SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . }\n" +
                        "}",
                entity.getId());

        Map<String, List<String>> queryResult = sendQuery(queryString);

        return queryResult.containsKey("parent")
                ? queryResult.get("parent")
                .stream()
                .map(parentString -> new WikidataEntity(parentString.split("entity/")[1]))
                .collect(Collectors.toList())
                : Collections.emptyList();
    }

    /**
     * Gets a list of the direct children in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of child entities.
     */
    public static List<WikidataEntity> getSubclasses(WikidataEntity entity) {
        String queryString = String.format("SELECT ?subclass ?subclassLabel WHERE {\n" +
                        "   wd:%1$s ^wdt:P279 ?subclass .\n" +
                        "   SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . }\n" +
                        "}",
                entity.getId());

        return sendQuery(queryString).get("subclass")
                .stream()
                .map(subclassString -> new WikidataEntity(subclassString.split("entity/")[1]))
                .collect(Collectors.toList());
    }


    /**
     * Gets a list of the classes of which this subject is a particular example and member.
     * (Subject typically an individual member with Proper Name label.) Different from P279 (subclass of)
     *
     * @param entity the given entity.
     * @return the list of class entities.
     */
    public static List<WikidataEntity> instanceOf(WikidataEntity entity) {
        String queryString = String.format("SELECT ?class ?classLabel WHERE {\n" +
                        "   ?class ^wdt:P31 wd:%1$s .\n" +
                        "   SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . }\n" +
                        "}",
                entity.getId());

        List<WikidataEntity> instanceOf = new ArrayList<>();
        Map<String, List<String>> queryResult = sendQuery(queryString);

        if (queryResult.containsKey("class")) {
            for (int i = 0; i < queryResult.get("class").size(); i++) {
                String classString = queryResult.get("class").get(i);
                String classLabelString = queryResult.get("classLabel").get(i);
                instanceOf.add(new WikidataEntity(classString.split("entity/")[1], classLabelString));
            }
        }

        return instanceOf;
    }


    /**
     * Gets a list of the instances in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of instance entities.
     */
    public static List<WikidataEntity> getInstances(WikidataEntity entity) {
        String queryString = String.format("SELECT ?instance ?instanceLabel WHERE {\n" +
                        "   wd:%1$s ^wdt:P31 ?instance .\n" +
                        "   SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . }\n" +
                        "}",
                entity.getId());

        return sendQuery(queryString).get("instance")
                .stream()
                .map(instanceString -> new WikidataEntity(instanceString.split("entity/")[1]))
                .collect(Collectors.toList());
    }


    /**
     * Returns true if the entity is instance of a subclass of the second entity.
     *
     * @param firstEntity  first entity
     * @param secondEntity second entity
     * @return true if the entity is instance of a subclass of the second entity.
     */
    public static boolean isTransitiveInstanceOf(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        return (instanceOf(firstEntity).contains(secondEntity)
                || instanceOf(firstEntity).stream().anyMatch(classEntity -> getAllAncestors(classEntity).contains(secondEntity)));
    }

    /**
     * Gets the depth between the given entity and the given entity's ancestor.
     * If there exist multiple paths, the shortest is used.
     *
     * @param entity   specified entity
     * @param ancestor specified entity's ancestor
     * @return number of levels to the ancestor.
     */
    public static long getDistanceToAncestor(WikidataEntity entity, WikidataEntity ancestor) {

        String queryString = String.format("PREFIX gas: <http://www.bigdata.com/rdf/gas#>\n" +
                        "\n" +
                        "SELECT ?parent ?parentLabel ?depth WHERE {\n" +
                        "SERVICE gas:service {\n" +
                        "     gas:program gas:gasClass \"com.bigdata.rdf.graph.analytics.BFS\" .\n" +
                        "     gas:program gas:linkType wdt:P279 .\n" +
                        "     gas:program gas:traversalDirection \"Forward\" .\n" +
                        "     gas:program gas:in wd:%1$s . # one or more times, specifies the initial frontier.\n" +
                        "     gas:program gas:out ?parent . # exactly once - will be bound to the visited vertices.\n" +
                        "     gas:program gas:out1 ?depth . # exactly once - will be bound to the depth of the visited vertices.\n" +
                        "     gas:program gas:maxIterations 8 . # optional limit on breadth first expansion.\n" +
                        "  }\n" +
                        "SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . } \n" +
                        "FILTER(?parent = wd:%2$s).\n" +
                        "}\n" +
                        "ORDER BY ASC(?depth)",
                entity.getId(),
                ancestor.getId());

        Map<String, List<String>> queryResult = sendQuery(queryString);

        if (queryResult.containsKey("depth")) {
            return queryResult.get("depth")
                    .stream()
                    .mapToLong(Integer::parseInt)
                    .findFirst()
                    .orElse(WikidataDumpUtil.getOntologyMaxDepth());
        }

        return WikidataDumpUtil.getOntologyMaxDepth();
    }


    /**
     * Gets the depth between the given entity and the taxonomy root. If there exist multiple paths,
     * the shortest is used.
     *
     * @param entity specified entity
     * @return number of levels to the taxonomy root
     */
    public static long getRootDistance(WikidataEntity entity) {
        return getDistanceToAncestor(entity, rootEntity);
    }

    /**
     * Gets a list of all ancestors in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of parent / ancestor entities.
     */
    public static List<WikidataEntity> getAllAncestors(WikidataEntity entity) {
        String queryString = String.format("PREFIX gas: <http://www.bigdata.com/rdf/gas#>\n" +
                        "\n" +
                        "SELECT ?parent ?parentLabel ?depth WHERE {\n" +
                        "SERVICE gas:service {\n" +
                        "     gas:program gas:gasClass \"com.bigdata.rdf.graph.analytics.BFS\" .\n" +
                        "     gas:program gas:linkType wdt:P279 .\n" +
                        "     gas:program gas:traversalDirection \"Forward\" .\n" +
                        "     gas:program gas:in wd:%1$s . # one or more times, specifies the initial frontier.\n" +
                        "     gas:program gas:out ?parent . # exactly once - will be bound to the visited vertices.\n" +
                        "     gas:program gas:out1 ?depth . # exactly once - will be bound to the depth of the visited vertices.\n" +
                        "  }\n" +
                        "SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . } \n" +
                        "}\n" +
                        "ORDER BY ASC(?depth)",
                entity.getId());

        return sendQuery(queryString).get("parent")
                .stream()
                .map(parentString -> new WikidataEntity(parentString.split("entity/")[1]))
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of all descendants in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of child / descendant entities.
     */
    public static List<WikidataEntity> getAllDescendants(WikidataEntity entity) {
        String queryString = String.format("PREFIX gas: <http://www.bigdata.com/rdf/gas#>\n" +
                        "\n" +
                        "SELECT ?parent ?parentLabel ?depth WHERE {\n" +
                        "SERVICE gas:service {\n" +
                        "     gas:program gas:gasClass \"com.bigdata.rdf.graph.analytics.BFS\" .\n" +
                        "     gas:program gas:linkType wdt:P279 .\n" +
                        "     gas:program gas:traversalDirection \"Reverse\" .\n" +
                        "     gas:program gas:in wd:%1$s . # one or more times, specifies the initial frontier.\n" +
                        "     gas:program gas:out ?parent . # exactly once - will be bound to the visited vertices.\n" +
                        "     gas:program gas:out1 ?depth . # exactly once - will be bound to the depth of the visited vertices.\n" +
                        "  }\n" +
                        "SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . } \n" +
                        "}\n" +
                        "ORDER BY ASC(?depth)",
                entity.getId());

        return sendQuery(queryString).get("parent")
                .stream()
                .map(parentString -> new WikidataEntity(parentString.split("entity/")[1]))
                .collect(Collectors.toList());
    }

    /**
     * What is or are the lowest common ancestor(s) of two entities in the Wikidata ontology ?
     *
     * @param firstEntity  first entity
     * @param secondEntity second entity
     * @return the most specific parent entity.
     */
    public static WikidataEntity getMostSpecificParentEntity(WikidataEntity firstEntity, WikidataEntity secondEntity) {

        String queryString = String.format("SELECT ?lcs ?lcsLabel WHERE {\n" +
                        "    ?lcs ^wdt:P279* wd:%1$s, wd:%2$s .\n" +
                        "    filter not exists {\n" +
                        "    ?sublcs ^wdt:P279* wd:%1$s, wd:%2$s ;\n" +
                        "          wdt:P279 ?lcs . }\n" +
                        "    SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE], en\" . }\n" +
                        "  }",
                firstEntity.getId(),
                secondEntity.getId());

        return sendQuery(queryString).get("lcs")
                .stream()
                .map(lcsString -> new WikidataEntity(lcsString.split("entity/")[1]))
                .findFirst().orElse(rootEntity);
    }


    /**
     * Gets the number of edges between two given entities. If there exist multiple paths,
     * the shortest is used.
     *
     * @param firstEntity  first entity
     * @param secondEntity second entity
     * @return the number of edges between both.
     */
    public static long distance(WikidataEntity firstEntity, WikidataEntity secondEntity) {

        WikidataEntity mostSpecificParentEntity = getMostSpecificParentEntity(firstEntity, secondEntity);

        long firstDistance = getDistanceToAncestor(firstEntity, mostSpecificParentEntity);
        long secondDistance = getDistanceToAncestor(secondEntity, mostSpecificParentEntity);

        return firstDistance + secondDistance;
    }


    /**
     * Returns true if the entity is instance of a subclass of number.
     *
     * @param entity entity
     * @return true if the entity is instance of a subclass of number.
     */
    public static boolean isNumber(WikidataEntity entity) {
        return instanceOf(entity).stream().anyMatch(classEntity -> classEntity.getLabel().contains("number") || getAllAncestors(classEntity).contains(number));
    }

    /**
     * Returns true if the entity is instance of natural number.
     *
     * @param entity entity
     * @return true if the entity is instance of natural number.
     */
    public static boolean isNaturalNumber(WikidataEntity entity) {
        return instanceOf(entity).contains(naturalNumber);
    }

    /**
     * Returns true if the entity is instance of a subclass of creative work.
     *
     * @param entity entity
     * @return true if the entity is instance of a subclass of creative work.
     */
    public static boolean isCreativeWork(WikidataEntity entity) {
        return (isTransitiveInstanceOf(entity, creativeWork) || isTransitiveInstanceOf(entity, new WikidataEntity("Q196600", "media franchise")))
                && !instanceOf(entity).contains(new WikidataEntity("Q35127", "website"));
    }


    /**
     * Returns true if the entity is instance of gene.
     *
     * @param entity entity
     * @return true if the entity is instance of gene.
     */
    public static boolean isGene(WikidataEntity entity) {
        return isTransitiveInstanceOf(entity, gene);
    }

    /**
     * Returns true if the entity is instance of human.
     *
     * @param entity entity
     * @return true if the entity is instance of human.
     */
    public static boolean isHuman(WikidataEntity entity) {
        return instanceOf(entity).contains(human);
    }

    /**
     * Returns true if the entity is instance of location.
     *
     * @param entity entity
     * @return true if the entity is instance of location.
     */
    public static boolean isLocation(WikidataEntity entity) {
        return isTransitiveInstanceOf(entity, location);
    }

    /**
     * Returns true if the entity is instance of organization.
     *
     * @param entity entity
     * @return true if the entity is instance of organization.
     */
    public static boolean isOrganization(WikidataEntity entity) {
        return isTransitiveInstanceOf(entity, organization);
    }


    /**
     * Returns true if the entity is instance of human language.
     *
     * @param entity entity
     * @return true if the entity is instance of human language.
     */
    public static boolean isHumanLanguage(WikidataEntity entity) {
        return (subclassOf(entity).contains(humanLanguage)
                || getAllAncestors(entity).contains(humanLanguage));
    }


    /**
     * Returns true if the given entity is an instance.
     *
     * @param entity entity
     * @return true if it is an instance.
     */
    public static boolean isInstance(WikidataEntity entity) {
        return instanceOf(entity).size() > 0;
    }


    /**
     * Returns true if the given entity is a class.
     *
     * @param entity entity
     * @return true if it is a class.
     */
    public static boolean isClass(WikidataEntity entity) {
        return getInstances(entity).size() > 0;
    }


    /**
     * Performs a SPARQL sendQuery to WikiData.
     *
     * @param queryString : A SPARQL sendQuery string.
     * @return : the result set returned by the server.
     */
    public static Map<String, List<String>> sendQuery(String queryString) {

        Map<String, List<String>> bindings = new HashMap<>();
        String urlString;

        // build url string and perform get request
        try {
            //In case of Wikidata changes, reformat query according to: https://www.wikidata.org/wiki/Wikidata:SPARQL_query_service/de
            urlString = wikidataSparqlEndpoint +
                    "?query=" +
                    URLEncoder.encode(String.join("\n", wikidataPrefixes) + "\n" + queryString, "UTF-8");

            logSimple("WikidataSparqlUtil: Sending query: "+queryString);
            String jsonString = httpGetJson(urlString);

            // parse json
            JsonElement jsonElement = new JsonParser().parse(jsonString);
            JsonArray jsonArray = jsonElement.getAsJsonObject()
                    .getAsJsonObject("results")
                    .getAsJsonArray("bindings");
            //System.out.println("WikidataSparqlUtil: Got JSON response" + jsonArray.toString()); // Too long
            long size = jsonArray.size();
            logSimple("WikidataSparqlUtil: Got JSON response Arraylength: "+ size);
            // read property bindings
            final long[] counterBindings = {0};
            jsonArray.forEach(binding -> {
                //System.out.println("WikidataSparqlUtil: processing binding "+ counterBindings[0] + "/"+size);
                counterBindings[0] = counterBindings[0] +1;
                Set<Map.Entry<String, JsonElement>> entrySet = binding.getAsJsonObject().entrySet();

                entrySet.forEach((Map.Entry<String, JsonElement> entry) -> {
                    String value = entry.getValue().getAsJsonObject().get("value").getAsString();

                    if (bindings.containsKey(entry.getKey())) {
                        // add binding to existing key value pair
                        List<String> values = new ArrayList<>(bindings.get(entry.getKey()));
                        values.add(value);
                        //System.out.println("WikidataSparqlUtil: Putting key to bindings"+entry.getKey().toString() + ",value: "+value.toString());
                        bindings.put(entry.getKey(), values);
                    } else {
                        // create new key value pair
                        //System.out.println("WikidataSparqlUtil: Putting key to bindings"+entry.getKey().toString()+ ",value: "+value.toString());
                        bindings.put(entry.getKey(),
                                Collections.singletonList(value));
                    }
                });
            });
            logSimple("WikidataSparqlUtil: done processing bindings");
            return bindings;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IllegalStateException("Wikidata SPARL communication failed because UTF-8 sendQuery could not be built.");
        }
    }

    /**
     * Performs an HTTP GET request and returns the response as string.
     *
     * @param urlString url string
     * @return response string
     */
    private static synchronized String httpGetJson(String urlString) {
        int numberOfTries = 0;

        while (true) {
            try {
                URL url = new URL(urlString);

                URLConnection conn = url.openConnection();
                conn.setRequestProperty("Accept", "application/sparql-results+json");
                InputStream inputStream = conn.getInputStream();

                StringWriter writer = new StringWriter();
                IOUtils.copy(inputStream, writer, "UTF8");

                return writer.toString();
            } catch (IOException e) {
                try {
                    numberOfTries += 1;

                    // create file for error report.
                    String path = sparqlErrorLogPath;

                    // Custom message
                    String message = e.getMessage().contains("response code: 500") ? "Internal server error." : e.getMessage();
                    message = e.getMessage().contains("response code: 429") ? "Too many requests." : message;
                    message = message + "\n\n" + urlString + "\n\n" + Arrays.toString(e.getStackTrace());
                    ExtendedLogUtil extendedLogUtil = new ExtendedLogUtil(path,null,"SparQLHandler",true,false);
                    extendedLogUtil.writeErrorReport(false, message);

                    int sleepTime = 1000 * 20 + 1000 * 10 * numberOfTries;

                    System.out.println(message + " Error report written. Wait for " + sleepTime / 1000 + " seconds.");

                    // for 20 seconds + 10 seconds * number of tries
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }




}
