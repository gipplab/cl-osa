package com.iandadesign.closa.util.wikidata;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.iandadesign.closa.classification.Category;
import com.iandadesign.closa.model.Token;
import com.iandadesign.closa.model.WikidataEntity;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The WikidataDumpUtil class comprises all methods needed to work with a local Wikidata dump in NoSQL format (MongoDB),
 * i.e. existance queries, entity-to-label mapping, and entity extraction from a single token.
 * <p>
 * Created by Fabian Marquart on 2018/07/19.
 */
public class WikidataDumpUtil {

    private final static List<String> languages = Arrays.asList("en", "de", "fr", "es", "ja", "zh", "hi", "it", "ru");

    // entities and properties used for querying
    private static final String subclassOfProperty = "P279";
    private static final String instanceOfProperty = "P31";
    private static final String countryProperty = "P17";
    private static final WikidataEntity rootEntity = new WikidataEntity("Q35120", "entity");
    private static final long ontologyMaxDepth = 29;

    // classes for classification
    private static final WikidataEntity number = new WikidataEntity("Q11563", "number");
    private static final WikidataEntity naturalNumber = new WikidataEntity("Q21199", "natural number");
    private static final WikidataEntity humanLanguage = new WikidataEntity("Q20162172", "human language");
    private static final WikidataEntity creativeWork = new WikidataEntity("Q17537576", "creative work");
    private static final WikidataEntity gene = new WikidataEntity("Q7187", "gene");
    private static final WikidataEntity location = new WikidataEntity("Q17334923", "location");
    private static final WikidataEntity human = new WikidataEntity("Q5", "human");
    private static final WikidataEntity organization = new WikidataEntity("Q43229", "organization");

    private static final String mongoDatabaseName = "wikidata";
    private static final String entitiesCollectionName = "entities";
    private static final String entitiesHierarchyCollectionName = "entitiesHierarchyPersistent";

    // database
    private static ServerAddress serverAddress;
    private static MongoCredential mongoCredential;
    private static MongoClient mongoClient;
    private static MongoCollection<Document> entitiesCollection;
    private static MongoCollection<Document> entitiesHierarchyCollection;

    static {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger("org.mongodb.driver");
        rootLogger.setLevel(Level.OFF);

        try {
            loadDatabaseFromConfig();
            MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
            MongoClientOptions options = builder.connectionsPerHost(1000).build();

            if (mongoCredential.getUserName().equals("") || mongoCredential.getPassword().length == 0) {
                mongoClient = new MongoClient(serverAddress, options);
            } else {
                mongoClient = new MongoClient(serverAddress, Collections.singletonList(mongoCredential), options);
            }

            MongoDatabase database = mongoClient.getDatabase(mongoDatabaseName);
            entitiesCollection = database.getCollection(entitiesCollectionName);
            entitiesHierarchyCollection = database.getCollection(entitiesHierarchyCollectionName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * Read MongoDB properties.
     *
     * @throws IOException When property file could not be loaded.
     */
    private static void loadDatabaseFromConfig() throws IOException {
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
            String mongoHost = properties.getProperty("mongo_host");
            int mongoPort = Integer.parseInt(properties.getProperty("mongo_port"));
            String mongoUsername = properties.getProperty("mongo_username");
            String mongoPassword = properties.getProperty("mongo_password");

            mongoCredential = MongoCredential.createCredential(mongoUsername, mongoDatabaseName, mongoPassword.toCharArray());
            serverAddress = new ServerAddress(mongoHost, mongoPort);
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public static long getOntologyMaxDepth() {
        return ontologyMaxDepth;
    }

    public static long getOntologyEntityCount() {
        return entitiesCollection.count();
    }

    public static WikidataEntity getRootEntity() {
        return rootEntity;
    }

    /**
     * Checks the MongoDB if the given entity exists.
     *
     * @param entity the given entity.
     * @return true if it exists, false otherwise.
     */
    public static boolean exists(WikidataEntity entity) {
        return entitiesCollection.find(new Document("id", entity.getId())).first() != null;
    }

    /**
     * Checks the MongoDB if the given label exists.
     *
     * @param label        the given label.
     * @param languageCode : the label language.
     * @return true if it exists, false otherwise.
     */
    public static boolean exists(String languageCode, String label) {
        return entitiesCollection.find(createLabelQuery(languageCode, label)).first() != null;
    }

    /**
     * Retrieves the entity matching the id from MongoDB.
     *
     * @param id : the id to find
     * @return the result as Wikidata entity.
     */
    public static WikidataEntity getEntityById(String id) {
        // sendQuery
        Document query = new Document("id", id);

        Document document = entitiesCollection.find(query)
                .first();

        if (document == null) {
            if (id.contains("Q")) {
                return new WikidataEntity(id);
            }
            throw new NoSuchElementException(String.format("No entity for id %s", id));
        }

        // read data
        String jsonId = document.get("id").toString();

        Map<String, String> labels = new HashMap<>();
        Map<String, String> descriptions = new HashMap<>();

        languages.forEach(language -> {
            if (document.get("labels", Document.class).containsKey(language)) {
                labels.put(language, document.get("labels", Document.class)
                        .get(language, Document.class).getString("value"));
            }

            if (document.get("descriptions", Document.class).containsKey(language)) {
                descriptions.put(language, document.get("descriptions", Document.class)
                        .get(language, Document.class).getString("value"));
            }
        });

        return new WikidataEntity(jsonId, labels.get("en"), labels, descriptions);
    }

    /**
     * Get sample entities.
     *
     * @param sampleSize number of entities to randomly take.
     * @return a list of random entities of size sample size.
     */
    public static List<WikidataEntity> getRandomEntities(int sampleSize) {
        AggregateIterable<Document> result = entitiesCollection.aggregate(Collections.singletonList(
                new Document("$sample", new Document("size", sampleSize))
        ));

        List<WikidataEntity> sample = new ArrayList<>();

        for (Document document : result) {
            // read data
            String jsonId = document.get("id").toString();

            Map<String, String> labels = new HashMap<>();
            Map<String, String> descriptions = new HashMap<>();

            languages.forEach(language -> {
                if (document.get("labels", Document.class).containsKey(language)) {
                    labels.put(language, document.get("labels", Document.class)
                            .get(language, Document.class).getString("value"));
                }

                if (document.get("descriptions", Document.class).containsKey(language)) {
                    descriptions.put(language, document.get("descriptions", Document.class)
                            .get(language, Document.class).getString("value"));
                }
            });

            sample.add(new WikidataEntity(jsonId, labels.get("en"), labels, descriptions));
        }

        return sample;
    }

    /**
     * Retrieves the entity matching the label from MongoDB.
     *
     * @param label        : the label to find
     * @param languageCode : the label language.
     * @return the results as Wikidata entities.
     */
    public static List<WikidataEntity> getEntitiesByLabel(String label, String languageCode) {
        if (!languages.contains(languageCode)) {
            throw new IllegalArgumentException(String.format("Language code %s is not supported", languageCode));
        }
        List<WikidataEntity> entities = new ArrayList<>();

        for (Document nextDocument : entitiesCollection
                .find(createLabelQuery(label, languageCode))) {
            String jsonId = nextDocument.get("id").toString();
            String jsonLabel = nextDocument.get("labels", Document.class).containsKey(languageCode)
                    ? nextDocument.get("labels", Document.class).get(languageCode, Document.class)
                    .getString("value") : "NULL";

            Map<String, String> labels = new HashMap<>();
            Map<String, String> descriptions = new HashMap<>();

            languages.forEach(language -> {
                if (nextDocument.get("labels", Document.class).containsKey(language)) {
                    labels.put(language, nextDocument.get("labels", Document.class)
                            .get(language, Document.class).getString("value"));
                }

                if (nextDocument.get("descriptions", Document.class).containsKey(language)) {
                    descriptions.put(language, nextDocument.get("descriptions", Document.class)
                            .get(language, Document.class).getString("value"));
                }

            });

            WikidataEntity entity = new WikidataEntity(jsonId, jsonLabel, labels, descriptions);
            entities.add(entity);
        }

        return entities;
    }

    /**
     * Retrieves the entity matching the token from MongoDB:
     * - tokens that are named entities only retrieve instances from Wikidata
     * - tokens that are not named entities only retrieve classes from Wikidata
     *
     * @param token        : the token to find, by lemma.
     * @param languageCode : the label language.
     * @return the results as Wikidata entities.
     */
    public static List<WikidataEntity> getEntitiesByToken(Token token, String languageCode) {
        if (!languages.contains(languageCode)) {
            throw new IllegalArgumentException(String.format("Language code %s is not supported", languageCode));
        }
        return getEntitiesByToken(token, languageCode, Category.neutral);
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
        if (!languages.contains(languageCode)) {
            throw new IllegalArgumentException(String.format("Language code %s is not supported", languageCode));
        }
        if (token.getLemma() == null) {
            throw new IllegalArgumentException("The token lemma is null");
        } else if (token.getLemma().equals("")) {
            return new ArrayList<>();
        }

        List<WikidataEntity> entities;

        String queryLemma = token.getLemma();

        // count results
        List<Document> results = new ArrayList<>();

        Document query = createLabelQuery(queryLemma, languageCode);

        // 1 decide between lemma or token
        for (int i = 0; i < 2; i++) {
            // collect the results
            for (Document currentDocument : entitiesCollection.find(query)) {
                results.add(currentDocument);

                if (token.getPartOfSpeech().equals("CD")) {
                    if (isNaturalNumber(new WikidataEntity(currentDocument.get("id").toString()))) {
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

            // if no results are found, sendQuery aliases instead
            // sendQuery = new Document("aliases." + languageCode + ".value", capitalizeIfFirstLetterIsUppercase(queryLemma));
            // if no results are found for lemma, assume wrong lemmatization and sendQuery token instead
            query = createLabelQuery(token.getToken(), languageCode);
        }

        // 2 consider the sendQuery result itself
        entities = results.stream()
                .map(document -> {
                    String jsonId = document.get("id").toString();
                    String jsonLabel = document.get("labels", Document.class).containsKey(languageCode)
                            ? document.get("labels", Document.class).get(languageCode, Document.class).getString("value")
                            : "NULL";
                    return new WikidataEntity(jsonId, jsonLabel);
                })
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
                                || entity.getDescriptions().getOrDefault("en", "").matches(".*protein.*")
                                || entity.getDescriptions().getOrDefault("en", "").matches(".*gene.*"))
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

    public static Map<String, List<WikidataEntity>> getProperties(WikidataEntity entity) {
        Map<String, List<WikidataEntity>> propertyValues = new HashMap<>();

        Document query = new Document("id", entity.getId());

        Document document = entitiesCollection.find(query).first();

        // read data
        Document claims = document.get("claims", Document.class);

        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            List<Document> propertyDocuments = (ArrayList) entry.getValue();
            String property = entry.getKey();

            for (Document propertyDocument : propertyDocuments) {
                Document mainsnak = propertyDocument.get("mainsnak", Document.class);

                String dataType = mainsnak.getString("datatype");

                if (dataType.equals("wikibase-item") && mainsnak.containsKey("datavalue")) {
                    String entityId = mainsnak.get("datavalue", Document.class)
                            .get("value", Document.class).getString("id");

                    if (!propertyValues.containsKey(property)) {
                        propertyValues.put(property, new ArrayList<>());
                    }
                    propertyValues.get(property).add(new WikidataEntity(entityId));
                }
            }
        }

        return propertyValues;
    }


    /**
     * Gets a list of the property matches in the Wikidata ontology.
     *
     * @param entity     entity
     * @param propertyId property id
     * @return the list of matched entities.
     */
    public static List<WikidataEntity> getProperty(WikidataEntity entity, String propertyId) {
        if (!propertyId.contains("P")) {
            throw new IllegalArgumentException("The entity has to be a property.");
        }

        List<WikidataEntity> propertyMatches = new ArrayList<>();

        Bson query = Filters.and(new Document("id", entity.getId()),
                new Document("claims." + propertyId + ".mainsnak.property", propertyId));


        for (Document nextDocument : entitiesCollection.find(query)) {
            Document claims = (Document) nextDocument.get("claims");

            if (claims.containsKey(propertyId)) {
                List<Document> properties = (List<Document>) claims.get(propertyId, ArrayList.class);

                for (Document currentProperty : properties) {
                    Document mainsnak = currentProperty.get("mainsnak", Document.class);

                    if (mainsnak.containsKey("datavalue")) {
                        Document datavalue = mainsnak.get("datavalue", Document.class);
                        Document value = datavalue.get("value", Document.class);
                        String id = value.getString("id");

                        WikidataEntity propertyMatch = new WikidataEntity(id);
                        propertyMatches.add(propertyMatch);
                    }
                }
            }
        }

        return propertyMatches;
    }


    /**
     * Gets a list of the parents in the Wikidata ontology.
     * All instances of these items are instances of those items; this item is a class (subset) of that item.
     * Not to be confused with P31 (instance of)
     *
     * @param entity entity
     * @return the list of parent entities.
     */
    public static List<WikidataEntity> subclassOf(WikidataEntity entity) {
        return getProperty(entity, subclassOfProperty);
    }

    /**
     * Gets a list of the direct children in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of child entities.
     */
    public static List<WikidataEntity> getSubclasses(WikidataEntity entity) {
        List<WikidataEntity> subclasses = new ArrayList<>();

        for (Document nextDocument :
                entitiesCollection.find(new Document("claims." + subclassOfProperty + ".mainsnak.datavalue.value.id",
                        entity.getId()))) {
            subclasses.add(new WikidataEntity(nextDocument.getString("id")));
        }

        return subclasses;
    }


    /**
     * Gets a list of the instantiating classes in the Wikidata ontology.
     * Not to be confused with P279 (subclass of)
     *
     * @param entity entity
     * @return the list of class entities.
     */
    public static List<WikidataEntity> instanceOf(WikidataEntity entity) {
        return getProperty(entity, instanceOfProperty);
    }

    /**
     * Gets a list of instances in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of instance entities.
     */
    public static List<WikidataEntity> getInstances(WikidataEntity entity) {
        List<WikidataEntity> instances = new ArrayList<>();

        for (Document nextDocument :
                entitiesCollection.find(new Document("claims." + instanceOfProperty + ".mainsnak.datavalue.value.id",
                        entity.getId()))) {
            instances.add(new WikidataEntity(nextDocument.getString("id")));
        }

        return instances;
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
     * Checks if there is an ancestor relationship.
     *
     * @param entity   given entity
     * @param ancestor possible ancestor of given entity
     * @return true if there is an ancestor relationship.
     */
    public static boolean isAncestor(WikidataEntity entity, WikidataEntity ancestor) {
        return entitiesHierarchyCollection.find(new Document("id", entity.getId())
                .append("hierarchy.id", ancestor.getId()))
                .first() != null;
    }


    /**
     * Gets the depth between the given entity and the given entity's ancestor.
     * If there exist multiple paths, the shortest is used.
     *
     * @param entity   specified entity
     * @param ancestor specified entity's ancestor
     * @return number of levels to the taxonomy root
     */
    public static long getDistanceToAncestor(WikidataEntity entity, WikidataEntity ancestor) {
        Document result = entitiesHierarchyCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("id", entity.getId())),
                new Document("$unwind", "$hierarchy"),
                new Document("$group",
                        new Document("_id", new Document("hierarchyId", "$hierarchy.id"))
                                .append("depth", new Document("$first", new Document("$add", Arrays.asList("$hierarchy.depth", 1L))))),
                new Document("$match", new Document("_id.hierarchyId", ancestor.getId()))
        )).first();

        if (result == null) {
            return ontologyMaxDepth;
        }

        return result.getLong("depth");
    }

    /**
     * Gets the depth between the given entity and the taxonomy root. If there exist multiple paths,
     * the shortest is used.
     *
     * @param entity specified entity
     * @return number of levels to the taxonomy root
     */
    public static long getRootDistance(WikidataEntity entity) {
        if (entity.equals(rootEntity)) {
            return 0;
        }

        return getDistanceToAncestor(entity, rootEntity);
    }


    /**
     * Gets a list of all ancestors in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of parent / ancestor entities.
     */
    public static List<WikidataEntity> getAllAncestors(WikidataEntity entity) {
        AggregateIterable<Document> queryResult = entitiesHierarchyCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("id", entity.getId())),
                new Document("$unwind", "$hierarchy"),
                new Document("$group",
                        new Document("_id", new Document("hierarchyId", "$hierarchy.id"))
                                .append("depth", new Document("$first", new Document("$add", Arrays.asList("$hierarchy.depth", 1L)))))
        ));

        List<WikidataEntity> ancestors = new ArrayList<>();

        for (Document document : queryResult) {
            ancestors.add(new WikidataEntity(document.get("_id", Document.class).getString("hierarchyId")));
        }

        return ancestors;
    }


    /**
     * Gets a list of ancestors limited by depth in the Wikidata ontology.
     *
     * @param entity   entity
     * @param maxDepth maximum depth for traversal.
     * @return the map of ancestor entities with distance.
     */
    public static Map<WikidataEntity, Long> getAncestorsByMaxDepth(WikidataEntity entity, Long maxDepth) {
        AggregateIterable<Document> queryResult = entitiesHierarchyCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("id", entity.getId())),
                new Document("$unwind", "$hierarchy"),
                new Document("$match", new Document("hierarchy.depth", new Document("$lte", maxDepth)))
        ));

        Map<WikidataEntity, Long> ancestors = new HashMap<>();

        for (Document document : queryResult) {
            ancestors.put(new WikidataEntity(document.get("hierarchy", Document.class).getString("id")),
                    document.get("hierarchy", Document.class).getLong("depth") + 1L);
        }

        return ancestors;
    }

    /**
     * Gets a list of all descendants in the Wikidata ontology.
     *
     * @param entity entity
     * @return the list of child / descendant entities.
     */
    public static List<WikidataEntity> getAllDescendants(WikidataEntity entity) {
        AggregateIterable<Document> queryResult = entitiesHierarchyCollection.aggregate(Collections.singletonList(
                new Document("$match", new Document("hierarchy.id", entity.getId()))
        ));

        List<WikidataEntity> descendants = new ArrayList<>();

        for (Document document : queryResult) {
            descendants.add(new WikidataEntity(document.getString("id")));
        }

        return descendants;
    }


    /**
     * What is or are the lowest common ancestor(s) of two entities in the Wikidata ontology and their distance?
     *
     * @param firstEntity  first entity
     * @param secondEntity second entity
     * @return the most specific parent entity with distances from first and second entity.
     */
    public static Pair<WikidataEntity, List<Long>> getMostSpecificParentEntityWithDepth(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        Document result = entitiesHierarchyCollection.aggregate(Arrays.asList(
                new Document("$match",
                        new Document("$and",
                                Arrays.asList(new Document("$or",
                                                Arrays.asList(new Document("id", firstEntity.getId()),
                                                        new Document("id", secondEntity.getId()))),
                                        new Document("hierarchy", new Document("$exists", true)
                                                .append("$not", new Document("$size", 0)))))),
                new Document("$project", new Document("id", 1)
                        .append("label", 1)
                        .append("hierarchy", new Document("$concatArrays",
                                Arrays.asList(new Document("$ifNull",
                                                Arrays.asList("$hierarchy", Collections.emptyList())),
                                        Collections.singletonList(new Document("id", "$id")
                                                .append("label", "$label")
                                                .append("depth", -1L)))))),
                new Document("$unwind", "$hierarchy"),
                new Document("$group", new Document("_id", new Document("hierarchyId", "$hierarchy.id"))
                        .append("count", new Document("$sum", 1))
                        .append("depth", new Document("$push", new Document("$add", Arrays.asList("$hierarchy.depth", 1L))))
                        .append("maxDepth", new Document("$max", new Document("$add", Arrays.asList("$hierarchy.depth", 1L))))),
                new Document("$match", new Document("count", 2)),
                new Document("$sort", new Document("maxDepth", 1)),
                new Document("$limit", 1)
        )).first();

        if (result == null) {
            return new MutablePair<>(rootEntity, Arrays.asList(ontologyMaxDepth, ontologyMaxDepth));
        }

        WikidataEntity mostSpecificParentEntity = new WikidataEntity(result.get("_id", Document.class).getString("hierarchyId"));
        List<Long> depths = (ArrayList<Long>) result.get("depth", ArrayList.class);

        return new MutablePair<>(mostSpecificParentEntity, depths);
    }


    /**
     * What is or are the lowest common ancestor(s) of two entities in the Wikidata ontology?
     *
     * @param firstEntity  first entity
     * @param secondEntity second entity
     * @return the most specific parent entity.
     */
    public static WikidataEntity getMostSpecificParentEntity(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        return getMostSpecificParentEntityWithDepth(firstEntity, secondEntity).getKey();
    }

    /**
     * Shortest path length between two entities in Wikidata.
     *
     * @param firstEntity  first entity.
     * @param secondEntity second entity.
     * @return Shortest path length between both.
     */
    public static long distance(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        if (firstEntity.equals(secondEntity)) {
            return 0L;
        }

        return getMostSpecificParentEntityWithDepth(firstEntity, secondEntity).getValue().stream().mapToLong(l -> l).sum();
    }

    /**
     * Shortest path length between two entities in Wikidata, with cut-off distance threshold.
     *
     * @param firstEntity       first entity.
     * @param secondEntity      second entity.
     * @param distanceThreshold cut-off distance threshold
     * @return Shortest path length between both if below threshold, else ontology depth * 2.
     */
    public static long distanceWithThreshold(WikidataEntity firstEntity, WikidataEntity secondEntity, long distanceThreshold) {
        if (firstEntity.equals(secondEntity)) {
            return 0L;
        }

        Document result = entitiesHierarchyCollection.aggregate(Arrays.asList(
                new Document("$match",
                        new Document("$and",
                                Arrays.asList(new Document("$or",
                                                Arrays.asList(new Document("id", firstEntity.getId()),
                                                        new Document("id", secondEntity.getId()))),
                                        new Document("hierarchy", new Document("$exists", true)
                                                .append("$not", new Document("$size", 0)))))),
                new Document("$project", new Document("id", 1)
                        .append("label", 1)
                        .append("hierarchy", new Document("$concatArrays",
                                Arrays.asList(new Document("$ifNull",
                                                Arrays.asList("$hierarchy", Collections.emptyList())),
                                        Collections.singletonList(new Document("id", "$id")
                                                .append("label", "$label")
                                                .append("depth", -1L)))))),
                new Document("$unwind", "$hierarchy"),
                new Document("$group", new Document("_id", new Document("hierarchyId", "$hierarchy.id"))
                        .append("count", new Document("$sum", 1))
                        .append("depth", new Document("$push", new Document("$add", Arrays.asList("$hierarchy.depth", 1L))))
                        .append("maxDepth", new Document("$max", new Document("$add", Arrays.asList("$hierarchy.depth", 1L))))),
                new Document("$match", new Document("count", 2).append("maxDepth", new Document("$lt", distanceThreshold))),
                new Document("$sort", new Document("maxDepth", 1)),
                new Document("$limit", 1)
        )).first();

        if (result == null) {
            return ontologyMaxDepth * 2;
        }

        List<Long> depths = (ArrayList<Long>) result.get("depth", ArrayList.class);
        return depths.stream()
                .mapToLong(l -> l)
                .sum();
    }

    public static long distanceByProperties(WikidataEntity firstEntity, WikidataEntity secondEntity) {
        throw new NotImplementedException("");
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
     * Creates a basic or sendQuery looking for the sendQuery string in the labels or in the aliases of
     * entities.
     *
     * @param languageCode two letter language code.
     * @param queryString  the string to be queried.
     * @return a sendQuery document.
     */
    private static Document createLabelQuery(String queryString, String languageCode) {
        if (!languages.contains(languageCode)) {
            throw new IllegalArgumentException(String.format("Language code %s is not supported", languageCode));
        }

        return new Document("$or", Arrays.asList(
                new Document("labels." + languageCode + ".value", capitalizeIfFirstLetterIsUppercase(queryString)),
                new Document("aliases." + languageCode + ".value", capitalizeIfFirstLetterIsUppercase(queryString))));
    }


    /**
     * Wikipedia site link.
     *
     * @param englishSiteLink
     * @param language
     * @return
     */
    public static String getSiteLinkInLanguage(String englishSiteLink, String language) {
        Document entityEntry = entitiesCollection.find(new Document("sitelinks.enwiki.title", englishSiteLink)).first();

        return entityEntry.get("sitelinks", Document.class)
                .get(language + "wiki", Document.class)
                .getString("title");
    }

    /**
     * Wikipedia site link.
     *
     * @param siteLink
     * @param language
     * @return
     */
    public static String getSiteLinkInEnglish(String siteLink, String language) {
        if (language.equals("en")) {
            return siteLink;
        }

        Document entityEntry = entitiesCollection.find(new Document("sitelinks." + language + "wiki.title", siteLink)).first();

        if (entityEntry == null) {
            return null;
        }

        Document siteLinks = entityEntry.get("sitelinks", Document.class);

        if (siteLinks == null) {
            return null;
        }

        Document enWiki = siteLinks.get("enwiki", Document.class);

        if (enWiki == null) {
            return null;
        }

        return enWiki.getString("title");
    }


    /**
     * If a string's first letter is uppercase but contains multiple whitespace-separated words,
     * those are also capitalized.
     *
     * @param string a string.
     * @return fully capitalized if first letter was uppercase.
     */
    static String capitalizeIfFirstLetterIsUppercase(String string) {
        if (string.length() > 0) {
            String firstLetter = string.substring(0, 1);
            if (string.contains(" ") && firstLetter.equals(firstLetter.toUpperCase())) {
                return String.join(" ", Arrays.stream(string.split(" "))
                        .map(StringUtils::capitalize)
                        .collect(Collectors.toList()));
            }
        }

        return string;
    }


    /**
     * Printing function for entity list.
     *
     * @param entities     entities to print.
     * @param languageCode language code.
     */
    public static void printEntities(List<String> entities, String languageCode) {
        if (!languages.contains(languageCode)) {
            throw new IllegalArgumentException(String.format("Language code %s is not supported", languageCode));
        }
        System.out.println(getEntitiesForPrinting(entities, languageCode));
    }

    /**
     * Printing helper function to pretty print entities.
     *
     * @param entities     entities to print.
     * @param languageCode language code.
     * @return list of printable entity strings.
     */
    private static String getEntitiesForPrinting(List<String> entities, String languageCode) {
        if (!languages.contains(languageCode)) {
            throw new IllegalArgumentException(String.format("Language code %s is not supported", languageCode));
        }

        return String.join(", \n", entities.stream()
                .map(WikidataDumpUtil::getEntityById)
                .map(entity -> "{ " + entity.getId() + ", "
                        + (entity.getLabels() != null ? entity.getLabels().getOrDefault(languageCode, "") : "") + ", "
                        + (entity.getDescriptions() != null ? entity.getDescriptions().getOrDefault(languageCode, "") : "") + " }")
                .collect(Collectors.toList()));
    }


}