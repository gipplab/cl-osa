package com.iandadesign.closa.evaluation.impl;

import com.iandadesign.closa.evaluation.EvaluationSet;
import com.iandadesign.closa.util.ConceptUtil;
import com.iandadesign.closa.util.wikidata.WikidataDumpUtil;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.linear.OpenMapRealVector;
import org.apache.commons.math3.linear.SparseRealVector;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Cross-Language Explicit Semantic Analysis, by Potthast, Stein and Anderka (2008).
 * <p>
 * Created by Fabian Marquart on 2018/11/11.
 */
public class CLESAEvaluationSet extends EvaluationSet {

    private static final String databaseName = "wikipedia";

    // text stores documents with format:
    //
    // {
    //   id: "12",
    //   url: "https://en.wikipedia.org/wiki?curid=12",
    //   title: "Anarchism",
    //   ids: { "en": "12", "ja": .... },
    //   text: "..."
    // }
    private static final String articleCollection = "articles";


    // max number of wikipedia articles (recommended by Potthast 2008)
    // "If high retrieval speed or a high multilinguality is desired, documents should be represented as 1000- dimensional concept vectors.
    // At a lower dimension the retrieval quality deteriorates significantly.
    // A reasonable trade-off between retrieval quality and runtime is achieved for a concept space dimensionality between 1 000 and 10 000."
    private static final int wikipediaArticleLimit = 10000;

    private static MongoDatabase database;

    static {
        database = WikidataDumpUtil.getDatabase();
    }


    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @throws IOException if files not found.
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
    public CLESAEvaluationSet(File suspiciousFolder, String suspiciousLanguage, File candidateFolder, String candidateLanguage, int maxFileCount) {
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
    void extractWikipediaArticlesAndStore(String documentLanguage) {
        try {
            // check wikipedia dump files
            if (Files.notExists(Paths.get(System.getProperty("user.home") + "/wikipedia/output_" + documentLanguage + "/"))) {
                throw new FileNotFoundException("The WikiExtractor.py output file is missing or not named according " +
                        "to the format \"output-{LANGUAGE_CODE}\".");
            }

            Path wikipediaExtractedDumpPath = Paths.get(System.getProperty("user.home") + "/wikipedia/output_" + documentLanguage + "/");

            // 2.1 Walk the Wikipedia dump files
            //     only if not all have been processed into the MongoDB collection
            MongoCollection<Document> wikipediaTokensCollection = getMongoCollection(documentLanguage, articleCollection);
            wikipediaTokensCollection.createIndex(new Document("id", 1), new IndexOptions().unique(true));

            if (wikipediaTokensCollection.count() < wikipediaArticleLimit) {
                long missingEntriesCount = wikipediaArticleLimit - wikipediaTokensCollection.count();

                System.out.println("Preprocess Wikipedia dump. Need " + wikipediaArticleLimit + " entries in" +
                        " collection " + documentLanguage + articleCollection + "," +
                        " only " + wikipediaTokensCollection.count() + " present.");

                ProgressBar progressBarDumpInitial = new ProgressBar("Find relevant Wikipedia dump files:", missingEntriesCount, ProgressBarStyle.ASCII).start();


                // get the articles in the Wikipedia dump
                List<Element> documents = Files.walk(wikipediaExtractedDumpPath)
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
                        .collect(Collectors.toList());

                // take the dump files and insert them into the mongo tokens collection
                ProgressBar progressBarDump = new ProgressBar("Walk Wikipedia dump files:", documents.size(), ProgressBarStyle.ASCII).start();

                AtomicInteger failures = new AtomicInteger(0);
                AtomicInteger progress = new AtomicInteger(0);

                ForkJoinPool customThreadPool = new ForkJoinPool(6);

                customThreadPool.submit(() -> documents.parallelStream()
                        .forEach((Element document) -> {
                            try {
                                String id = document.attr("id");
                                String url = document.attr("url");
                                String title = document.attr("title");
                                String text = document.text();

                                storeWikipediaArticleDocument(id, url, title, text, documentLanguage);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                                failures.getAndIncrement();
                            }
                            progressBarDump.stepTo(progress.incrementAndGet());
                        })
                );

                System.out.println("From " + (documents.size()) + " articles, " + failures + " failed with Nullpointer.");

                progressBarDumpInitial.stop();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected List<String> preProcess(String documentPath, String documentLanguage) {
        throw new NotImplementedException();
    }


    /**
     * Perform analysis (and retrieval).
     */
    @Override
    protected void performAnalysis() {
        throw new NotImplementedException();
    }


    /**
     * Take in id, url, title, and text from wikipedia article to create a BSON document of the form
     * <p>
     * {
     * id: "12",
     * url: "https://en.wikipedia.org/wiki?curid=12",
     * title: "Anarchism",
     * text:  { en: "...", ja: "..." }
     * }
     *
     * @param id               article id
     * @param url              article url
     * @param title            article title
     * @param text             article text
     * @param documentLanguage language code
     */
    private void storeWikipediaArticleDocument(String id, String url, String title, String text, String documentLanguage) {
        MongoCollection<Document> tokensCollection = getMongoCollection(documentLanguage, articleCollection);

        // id in language
        String idInLanguage = ConceptUtil.getPageIdInLanguage(id, documentLanguage, "en");

        // main information
        Document articleDocument = new Document("id", idInLanguage)
                .append("url", url)
                .append("title", title);

        articleDocument.append("text", new Document(documentLanguage, text));

        // insert into to collection
        Document existingDocument = tokensCollection.find(new Document("id", id)).first();

        if (existingDocument == null) {
            System.out.println("Insert text document " + id);
            tokensCollection.insertOne(articleDocument);
        } else {
            System.out.println("Update text document " + id);
            tokensCollection.updateOne(existingDocument, existingDocument.get("text", Document.class)
                    .append(documentLanguage, text));
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
