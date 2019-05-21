package com.fabianmarquart.closa.evaluation.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.fabianmarquart.closa.evaluation.EvaluationSet;
import com.fabianmarquart.closa.model.Dictionary;
import com.fabianmarquart.closa.model.Token;
import com.fabianmarquart.closa.util.ConceptUtil;
import com.fabianmarquart.closa.util.TokenUtil;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Projections;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.SparseRealVector;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Cross-Language Explicit Semantic Analysis, by Potthast, Stein & Anderka (2008).
 * <p>
 * Created by Fabian Marquart on 2018/11/11.
 */
public class CLESAEvaluationSet extends EvaluationSet {

    private static final String host = "localhost:27017";
    private static final String databaseName = "wikipedia-multi";

    private final static List<String> supportedLanguages = Arrays.asList("en", "de", "fr", "es", "ja", "zh", "hi", "it", "ru");

    // tokens-{en|ja|zh} stores documents with format:
    //
    // {
    //   id: "12",
    //   url: "https://en.wikipedia.org/wiki?curid=12",
    //   title: "Anarchism",
    //   ids: { "en": "12", "ja": .... },
    //   tokens: [ "token1", "token2", ... ]
    // }
    private static final String tokensCollectionNameSuffix = "Tokens";

    // similarities-{en|ja|zh} stores documents with format:
    //
    // {
    //   documentId: "35157967/0.txt",
    //   similarities: [
    //      {
    //          id: "12",
    //          url: "https://en.wikipedia.org/wiki?curid=12",
    //          title: "Anarchism",
    //          value: 0.0042489
    //      },
    //      ...
    //   ]
    // }
    private static final String similaritiesCollectionNameSuffix = "Similarities";

    // max number of wikipedia articles (recommended by Potthast 2008)
    // "If high retrieval speed or a high multilinguality is desired, documents should be represented as 1000- dimensional concept vectors.
    // At a lower dimension the retrieval quality deteriorates significantly.
    // A reasonable trade-off between retrieval quality and runtime is achieved for a concept space dimensionality between 1 000 and 10 000."
    private static final int wikipediaArticleLimit = 5000;
    private static final int retrievalCountLimit = 50;

    private static MongoDatabase database;

    static {
        // mongo --dbpath /Users/fabian/data/db
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger mongoDbDriverLogger = loggerContext.getLogger("org.mongodb.driver");
        mongoDbDriverLogger.setLevel(Level.OFF);

        database = new MongoClient(host).getDatabase(databaseName);
    }


    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     */
    public CLESAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage);
    }

    /**
     * Initializes the evaluationSet with max file count. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @param maxFileCount       maximum file count.
     */
    public CLESAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int maxFileCount) throws IOException {
        super(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, maxFileCount);
    }

    /**
     * Gets a MongoDB collection.
     *
     * @return the Collection object.
     */
    private static MongoCollection<Document> getMongoCollection(String collectionNamePrefix, String collectionNameSuffix) {
        return database.getCollection(collectionNamePrefix + collectionNameSuffix);
    }


    /**
     * Get Wikipedia articles (in document language) from Wikipedia dump that has been preprocessed
     * by WikiExtractor.py, tokenize them and store them into the mongo collection {{documentLanguage}}Tokens.
     *
     * @param documentLanguage document's language
     */
    private void extractWikipediaTokensAndStore(String documentLanguage, List<Bson> languageFilters) {
        try {
            // check wikipedia dump files
            if (Files.notExists(Paths.get(System.getProperty("user.home") + "/wikipedia/output_" + documentLanguage + "/"))) {
                throw new FileNotFoundException("The WikiExtractor.py output file is missing or not named according " +
                        "to the format \"output-{LANGUAGE_CODE}\".");
            }

            Path wikipediaExtractedDumpPath = Paths.get(System.getProperty("user.home") + "/wikipedia/output_" + documentLanguage + "/");


            // 2.1 Walk the Wikipedia dump files
            //     only if not all have been processed into the MongoDB collection
            MongoCollection<Document> wikipediaTokensCollection = getMongoCollection(documentLanguage, tokensCollectionNameSuffix);
            wikipediaTokensCollection.createIndex(new Document("id", 1), new IndexOptions().unique(true));

            if (wikipediaTokensCollection.count() < wikipediaArticleLimit) {
                long missingEntriesCount = wikipediaArticleLimit - wikipediaTokensCollection.count();

                System.out.println("Preprocess Wikipedia dump. Need " + wikipediaArticleLimit + " entries in" +
                        " collection " + documentLanguage + tokensCollectionNameSuffix + "," +
                        " only " + wikipediaTokensCollection.count() + " present.");

                ProgressBar progressBarDumpInitial = new ProgressBar("Find relevant Wikipedia dump files:", missingEntriesCount, ProgressBarStyle.ASCII).start();


                // get the articles in the Wikipedia dump
                Files.walk(wikipediaExtractedDumpPath)
                        .parallel()
                        .filter(Files::isRegularFile)
                        .filter(path -> !path.endsWith(".DS_Store"))
                        .sorted()
                        .map(path -> {
                            // split the file into the documents it contains
                            try {
                                org.jsoup.nodes.Document parsedFile = Jsoup.parse(FileUtils.readFileToString(new File(path.toUri()), UTF_8));
                                return parsedFile.select("doc");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return new Elements();
                        })
                        .flatMap(Collection::stream)
                        .filter((Element document) -> {
                            String id = document.attr("id");
                            boolean articlePresentInAllLanguages = true;
                            for (String language : documentLanguages) {
                                articlePresentInAllLanguages =
                                        articlePresentInAllLanguages && ConceptUtil.hasPageIdLanguage(id, documentLanguage, language);
                            }
                            return articlePresentInAllLanguages;
                        })
                        .limit(wikipediaArticleLimit)
                        // 2.1 check if Wikipedia file has already been preprocessed to tokens
                        .filter((Element document) -> {
                            String id = document.attr("id");
                            MongoCollection<Document> tokensCollection = getMongoCollection(documentLanguage, tokensCollectionNameSuffix);
                            return tokensCollection.find(Filters.and(
                                    new Document("id", id),
                                    Filters.and(languageFilters)))
                                    .first() == null;
                        })
                        .forEach((Element document) -> {
                            String id = document.attr("id");
                            String url = document.attr("url");
                            String title = document.attr("title");
                            String text = document.text();

                            List<String> wikipediaArticleTokens;

                            // 2.2 preprocess to tokens and save to MongoDB collection
                            wikipediaArticleTokens = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(text, documentLanguage)
                                    .stream().map(Token::getToken).collect(Collectors.toList());

                            storeWikipediaTokenDocument(id, url, title, wikipediaArticleTokens, documentLanguage);
                            progressBarDumpInitial.step();
                        });

                progressBarDumpInitial.stop();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> preProcess(String documentPath, String documentLanguage) {
        // use a MongoDB database to store intermediate results
        // 1 document's tokens
        // 2 wikipedia articles' tokens
        // 3 the vectors

        try {
            // 1 get the document's tokens and attributes
            String documentTokensPath = documentPath.replace("pds", "pds/preprocessed/" + this.getClass().getSimpleName() + "/tokens");
            List<String> documentTokens;
            Map<String, Double> documentSimilarities = new HashMap<>();

            if (Files.exists(Paths.get(documentTokensPath))
                    && !FileUtils.readLines(new File(documentTokensPath), UTF_8).isEmpty()) {
                // if file exists and is not empty
                documentTokens = new ArrayList<>(FileUtils.readLines(new File(documentTokensPath), UTF_8));
            } else {
                String documentText = FileUtils.readFileToString(new File(documentPath), UTF_8);
                documentTokens = TokenUtil.tokenizeLowercaseStemAndRemoveStopwords(documentText, documentLanguage)
                        .stream().map(Token::getToken).collect(Collectors.toList());
            }

            // 2 get the Wikipedia articles' tokens
            List<Bson> languageFilters = documentLanguages.stream()
                    .map(lang -> new Document("ids." + lang, new Document("$exists", true)))
                    .collect(Collectors.toList());

            extractWikipediaTokensAndStore(documentLanguage, languageFilters);

            // 3 create similarity vector of document and wikipedia articles

            // get list of {wikipediaArticleLimit} wikipedia IDs
            List<String> wikipediaIds = new ArrayList<>();
            MongoCollection<Document> wikipediaTokensCollection = getMongoCollection(documentLanguage, tokensCollectionNameSuffix);

            for (Document currentWikipediaId :
                    wikipediaTokensCollection.find(Filters.and(languageFilters))
                            .projection(Projections.fields(Projections.include("id")))
                            .limit(wikipediaArticleLimit)) {

                wikipediaIds.add(currentWikipediaId.getString("id"));
            }

            System.out.println("wikipediaIds.size() = " + wikipediaIds.size());
            // System.out.println("wikipediaIds = " + wikipediaIds);

            // directly retrieve vector of {wikipediaArticleLimit} similarities from the document
            // db.enSimilarities.find( { $and: [ { "similarities.id": { $all:  [ "621" ] } }, { "documentId": "src/test/resources/org/sciplore/pds/test-bbc/en/40546743/0.txt" } ] } )
            MongoCollection<Document> wikipediaSimilaritiesCollection = getMongoCollection(documentLanguage, similaritiesCollectionNameSuffix);

            for (Document documents : wikipediaSimilaritiesCollection.find(Filters.and(
                    new Document("documentId", documentPath),
                    Filters.in("similarities.id", wikipediaIds)))
                    .projection(new Document("similarities", "1"))) {

                ArrayList<Document> similarities = documents.get("similarities", ArrayList.class);

                for (Document similarity : similarities) {
                    documentSimilarities.put(similarity.getString("id"), similarity.getDouble("value"));
                }
            }


            // get similarities missing from the database
            if (documentSimilarities.entrySet().size() < wikipediaArticleLimit) {

                System.out.println("Only " + documentSimilarities.entrySet().size() + ", less than " + wikipediaArticleLimit + " similarities stored.");

                // wikipedia ids with missing similarity value
                List<String> missingWikipediaIds = new ArrayList<>(wikipediaIds);
                missingWikipediaIds.removeAll(documentSimilarities.keySet());

                // iterate the preprocessed Wikipedia MongoDB documents
                ProgressBar progressBarMongoTokens = new ProgressBar("Walk Mongo tokens collection:", missingWikipediaIds.size(), ProgressBarStyle.ASCII).start();


                for (Document currentWikipediaTokensDocument : wikipediaTokensCollection.find(Filters.in("id", missingWikipediaIds))) {

                    String id = currentWikipediaTokensDocument.getString("id");

                    String url = currentWikipediaTokensDocument.getString("url");
                    String title = currentWikipediaTokensDocument.getString("title");

                    // read tokens from MongoDB collection into memory
                    List<String> wikipediaArticleTokens = (List<String>) currentWikipediaTokensDocument.get("tokens");

                    double similarityValue;

                    MongoCollection<Document> similaritiesCollection = getMongoCollection(documentLanguage, similaritiesCollectionNameSuffix);

                    // check if there is already a similarity document for current document
                    if (similaritiesCollection.find(new Document("documentId", documentPath)).first() == null) {

                        // 3.1 create new similarity document for the given suspicious/candidate document
                        List<SparseRealVector> documentVectors = Dictionary.createVectorsFromDocuments(documentTokens, wikipediaArticleTokens);

                        similarityValue = Dictionary.cosineSimilarity(documentVectors.get(0), documentVectors.get(1));
                        System.out.println("similarityValue = " + similarityValue);

                        Document similarityDocument = createSimilarityDocument(documentPath, id, url, title, similarityValue);

                        // insert into collection
                        similaritiesCollection.insertOne(similarityDocument);
                    } else {
                        // 3.2 retrieve existing similarity document for given suspicious/candidate document
                        Document similarityDocument = similaritiesCollection.find(new Document("documentId", documentPath)).first();

                        List<Document> similarities = (List<Document>) similarityDocument.get("similarities");
                        Document similarityDocumentThatContainsWikipediaSimilarity = similaritiesCollection.find(Filters.and(
                                new Document("documentId", documentPath),
                                new Document("documentId.similarities.id", id))).first();

                        // 3.2.2 the document has a complete similarities list and there is a similarity value for
                        //       every Wikipedia article.
                        // Some articles have no similarity associated.
                        if (similarityDocumentThatContainsWikipediaSimilarity == null) {
                            // 3.2.1 the document has yet no similarity to given Wikipedia article associated.
                            //       Add to the similarity sub document.

                            similarityValue = Dictionary.cosineSimilarity(documentTokens, wikipediaArticleTokens);

                            Document similaritySubDocument = createSimilaritySubDocument(id, url, title, similarityValue);

                            // create the similarities list inside the similarity document
                            similarities.add(similaritySubDocument);

                            similaritiesCollection.updateOne(similaritiesCollection.find(new Document("documentId", documentPath)).first(),
                                    new Document("$set", similarityDocument));
                        } else {
                            // case that is never entered.
                            List<Document> bsonSimilaritiesList = (List<Document>) similarityDocumentThatContainsWikipediaSimilarity.get("similarities");
                            similarityValue = bsonSimilaritiesList.get(0).get("value", Double.class);
                        }

                        // put in memory
                        documentSimilarities.put(id, similarityValue);
                    }

                    progressBarMongoTokens.step();
                }
                progressBarMongoTokens.stop();

            }

            return documentSimilarities.values()
                    .stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Perform analysis (and retrieval).
     */
    @Override
    protected void performAnalysis() {
        // get preprocessed suspicious documents (vectors storing similarities to Wikipedia)
        List<MongoCollection<Document>> suspiciousSimilaritiesCollections = suspiciousIdLanguageMap.values().stream()
                .distinct()
                .map(language -> getMongoCollection(language, similaritiesCollectionNameSuffix))
                .collect(Collectors.toList());

        // get preprocessed candidate documents (also vectors storing similarities to Wikipedia)
        List<MongoCollection<Document>> candidateSimilaritiesCollections = candidateIdLanguageMap.values().stream()
                .distinct()
                .map(language -> getMongoCollection(language, similaritiesCollectionNameSuffix))
                .collect(Collectors.toList());


        // 4 compare document vector pairs again using cosine similarity:
        suspiciousIdCandidateScoresMap = suspiciousIdTokensMap.entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toMap(suspiciousId -> suspiciousId, suspiciousId -> {
                    // map to suspicious <-> candidate cosine similarity
                    // 4.1 get suspicious similarities vector
                    Document suspiciousQuery = new Document("documentId", suspiciousId);

                    SparseRealVector suspiciousVector = suspiciousSimilaritiesCollections.stream()
                            .filter(mongoCollection -> mongoCollection.find(suspiciousQuery).first() != null)
                            .map(mongoCollection -> mongoCollection.find(suspiciousQuery).first())
                            .map(this::getVectorFromSimilarityDocument)
                            .findFirst().get();

                    // retrieve candidate with highest similarity to the suspicious document

                    return candidateIdTokensMap.keySet().stream()
                            .collect(Collectors.toMap(candidateId -> candidateId,
                                    candidateId -> {
                                        // 4.2 get candidate similarities vector
                                        Document candidateQuery = new Document("documentId", candidateId);

                                        SparseRealVector candidateVector = candidateSimilaritiesCollections.stream()
                                                .filter(mongoCollection -> mongoCollection.find(candidateQuery).first() != null)
                                                .map(mongoCollection -> mongoCollection.find(candidateQuery).first())
                                                .map(this::getVectorFromSimilarityDocument)
                                                .findFirst().get();

                                        // 4.3 get cosine similarity
                                        return Dictionary.cosineSimilarity(suspiciousVector, candidateVector);
                                    }))
                            .entrySet().stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue())) // get maximum similarity value
                            .limit(retrievalCountLimit)
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }));
    }


    /**
     * Take in id, url, title, and tokens from wikipedia article to create a BSON document of the form
     * <p>
     * {
     * id: "12",
     * url: "https://en.wikipedia.org/wiki?curid=12",
     * title: "Anarchism",
     * tokens: [ "token1", "token2", ... ]
     * }
     *
     * @param id                     article id
     * @param url                    article url
     * @param title                  article title
     * @param wikipediaArticleTokens article tokens
     */
    private void storeWikipediaTokenDocument(String id, String url, String title, List<String> wikipediaArticleTokens, String documentLanguage) {
        MongoCollection<Document> tokensCollection = getMongoCollection(documentLanguage, tokensCollectionNameSuffix);

        // main information
        Document tokenDocument = new Document("id", id)
                .append("url", url)
                .append("title", title);

        // ids in language
        Document ids = new Document();

        for (String language : supportedLanguages) {
            String idInLanguage = ConceptUtil.getPageIdInLanguage(id, documentLanguage, language);
            if (idInLanguage != null) {
                ids.append(language, idInLanguage);
            }
        }

        tokenDocument.append("ids", ids);

        // tokens
        BsonArray bsonTokensArray = new BsonArray();
        wikipediaArticleTokens.stream()
                .map(BsonString::new)
                .forEach(bsonTokensArray::add);

        tokenDocument.append("tokens", bsonTokensArray);

        // insert into to collection
        if (tokensCollection.find(new Document("id", id)).first() == null) {
            System.out.println("Insert token document " + id);
            tokensCollection.insertOne(tokenDocument);
        }
    }

    /**
     * Creates a BSON document of the form
     * <p>
     * {
     * documentId: "35157967/0.txt",
     * similarities: [
     * {
     * id: "12",
     * url: "https://en.wikipedia.org/wiki?curid=12",
     * title: "Anarchism",
     * value: 0.0042489
     * },
     * ...
     * ]
     * }
     *
     * @param documentPath    the document's path
     * @param id              article id
     * @param url             article url
     * @param title           article title
     * @param similarityValue similarity document - article.
     * @return BSON document.
     */
    private Document createSimilarityDocument(String documentPath, String id, String url, String title, double similarityValue) {
        Document similarityDocument = new Document("documentId", documentPath);

        List<Document> bsonSimilaritiesList = Collections.singletonList(createSimilaritySubDocument(id, url, title, similarityValue));
        similarityDocument.append("similarities", bsonSimilaritiesList);

        return similarityDocument;
    }

    /**
     * Creates a BSON document list with entries of the form
     * <p>
     * {
     * id: "12",
     * url: "https://en.wikipedia.org/wiki?curid=12",
     * title: "Anarchism",
     * value: 0.0042489
     * }
     *
     * @param id              article id
     * @param url             article url
     * @param title           article title
     * @param similarityValue similarity document - article.
     * @return BSON document list
     */
    private Document createSimilaritySubDocument(String id, String url, String title, double similarityValue) {

        return new Document("id", id)
                .append("url", url)
                .append("title", title)
                .append("value", similarityValue);
    }

    /**
     * Maps a BSON document of the form
     * {
     * documentId: "35157967/0.txt",
     * similarities: [
     * {
     * id: "12",
     * url: "https://en.wikipedia.org/wiki?curid=12",
     * title: "Anarchism",
     * value: 0.0042489
     * },
     * ...
     * ]
     * }
     * to a a vector containing the values.
     *
     * @param bsonDocument BSON document with "similarities" array inside, which has a number filed "value".
     * @return vector containing the values.
     */
    private SparseRealVector getVectorFromSimilarityDocument(Document bsonDocument) {
        List<Document> similarityDocuments = (List<Document>) bsonDocument.get("similarities");
        SparseRealVector vector = new OpenMapRealVector();
        similarityDocuments.forEach(similarityDocument -> {
            double value = similarityDocument.get("value", Double.class);
            vector.append(value);
        });
        return vector;
    }
}
