CL-OSA (cross-language ontology-based similarity analysis) readme
=================================================================

Plagiarism detection for Java
-----------------------------

CL-OSA is an algorithm to retrieve similar documents written in different languages by leveraging entity and ontology
information from a local Wikidata dump.

The algorithm can be used to assist in plagiarism detection by performing ranked retrieval of potential source documents
for a suspicious input document.

The input is
* a suspicious document (.txt)
* a list of candidate documents (.txt)

The output is
* a score map of the suspicious id as key and a candidate id score map as value.

#### Plagiarism detection for your files

To execute the application as a standalone, you need to

* [Set up a MongoDB database](#setting-up-the-mongodb-database)
* [Import the Java sources](#importing-the-java-sources)
* [Use the OntologyUtil class](#how-to-use)

Use the code snippet and adjust the file paths

    String suspiciousPath = "~/documents/en/35157967/0.txt";
    String candidateFolderPath = "~/documents/ja/";

    List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
         .stream()
         .map(File::getPath)
         .collect(Collectors.toList());

    Map<String, Double> candidateScoresMap = new OntologyBasedSimilarityAnalysis()
        .executeAlgorithmAndComputeScores(suspiciousPath, candidatePaths);

    System.out.println(candidateScoresMap);



#### Supported languages

* English (en)
* French (fr)
* German (de)
* Spanish (es)
* Chinese (zh)
* Japanese (ja)



### What CL-OSA does

CL-OSA takes documents as input and ranks them according to their semantic similarity.


### What CL-OSA does not

CL-OSA does not scan the documents for plagiarized passages, it only considers whole documents.
If you care about smaller detection granularity,
you can split up your documents and detect candidates for each chunk or sentence. 
For this purpose, we recommend the script:
[chunking.php and splitinsentences.php](https://github.com/FerreroJeremy/Cross-Language-Dataset/tree/master/scripts)
by Jérémy Ferrero.

Also, CL-OSA does not search the internet
for possible sources. This is addressed in different algorithms.



Setting up the MongoDB database
-------------------------------

### Alternatives to local storage

To use CL-OSA, you need to setup a MongoDB database. Alternatively, you can use the Wikidata SPARQL API directly.
In this case, you have to change the static import inside
[WikidataEntityExtractor](/pds-backend-core/src/main/java/org/sciplore/pds/util/wikidata/WikidataEntityExtractor.java) from
> import static org.sciplore.pds.util.wikidata.WikidataDumpUtil.*;

to
> import static org.sciplore.pds.util.wikidata.WikidataSparqlUtil.*;

However, this alternative is only recommended for testing purposes as
1. the number of queries is limited,
2. Wikidata is updated so frequently such that results become non-deterministic, and
3. querying a public web service is slow. 


### The setup process

Launch a new MongoDB instance on your desired host and port. 

The default is a MongoDB instance running on localhost, port 27017. When using a different host or port,
you need to modify the "host" variable inside [WikidataDumpUtil](/pds-backend-core/src/main/java/org/sciplore/pds/util/wikidata/WikidataDumpUtil.java).

Then, run this [Python script](/pds-backend-core/wikidata-dump-mongo-import.py) to import the Wikidata dump.

Usage is
    
    python wikidata-dump-mongo-import.py -h <host> -p <port>

If the current directory already contains a file named *latest-all.json.bz2*
the download step will be skipped. Otherwise, the download of a file of approx. 30 GB will start from
[Wikidata's dump site](https://dumps.wikimedia.org/wikidatawiki/entities/latest-all.json.bz2).

When the file has finished downloading or is already present, it will begin importing its contents
into the given MongoDB instance. This takes about 52 hours. Make sure you have enough disk space available
for the database, about 275 GB.


When the import and index creation has finished, you should have a database called "wikidata", containing
the collections "entities", "entitiesGraph", and "entitiesHierarchyPersistent".


Importing the Java sources
--------------------------

I recommend using IntelliJ IDEA to open the project and install the Maven dependencies. If you prefer using other tools,
you will still need Maven. Checkout the project from the
[GitHub repository](https://github.com/bishutsuka/citeplag-dev-backend/tree/clpd-merge-backup-new)
and make sure you are inside the branch *clpd-merge-backup-new*.

To get all dependencies running, run

    mvn install
    
inside the *citeplag-dev-backend/pds-backend-core* directory. 

How to use
----------

### API

Instantiate OntologyBasedSimilarityAnalysis with a LanguageDetector and a TextClassifier and execute the method "executeAlgorithmAndComputeScores". First argument is the suspicious file path
and second argument is the candidate file path.

    String suspiciousPath = "~/documents/en/35157967/0.txt";
    String candidateFolderPath = "~/documents/ja/";

    List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
         .stream()
         .map(File::getPath)
         .collect(Collectors.toList());

    Map<String, Double> candidateScoreMap = new OntologyBasedSimilarityAnalysis()
         .executeAlgorithmAndComputeScores(suspiciousPath, candidatePaths);
                                                            
    System.out.println(candidateScoreMap);
    
The output is a scored map of candidates.

Pre-processing steps will be saved to a directory named *preprocessed* which will be created in your
home directory if the input documents are also in the home directory. Otherwise, the directory will be created in
the root directory.

The [test class](/com/iandadesign/closa/OntologyBasedSimilarityAnalysisTest.java)
contains unit tests as demonstration. It uses the test resource files that come with the Java project.

If you already know that your files are of a certain language or topic, instantiate a fitting
LanguageDetector and TextClassifier and provide them to the OntologyBasedSimilarityAnalysis constructor:

    LanguageDetector languageDetector = new LanguageDetector(Arrays.asList("en", "ja"));
    TextClassifier textClassifier = new TextClassifier(Arrays.asList("fiction", "neutral"));

    OntologyBasedSimilarityAnalysis analysis = new OntologyBasedSimilarityAnalysis(languageDetector, textClassifier);

### .jar

When working with the .jar package, usage is the following:

    java -jar closa-1.0-SNAPSHOT.jar -s suspicious_file.txt -c candidate_folder -o output.txt [-l lang1 lang2 -t topic1 topic2]


###Entity extraction

If you are interested in extracting Wikidata entities from a text, you can use WikidataEntityExtractor's
methods "extractEntitiesFromText" or "annotateEntitiesInText".


#### .jar

    java -cp closa-1.0-SNAPSHOT.jar com.iandadesign.closa.commandLine.WikidataEntityExtraction -i input.txt -o output.txt [-l lang1 lang2 -t topic1 topic2 -a]

The -a flag switches from simple entity list output to entity annotations inside the text using xml-like tags, e.g.

    Scientists prove there is water on Mars
    
becomes

    Scientists<span token="Scientists" qid="Q901"/> prove there is water<span token="water" qid="Q283"/> on Mars<span token="Mars" qid="Q111"/>

Evaluation
----------

If you desire to evaluate CL-OSA's in terms of precision, recall and F1-score, instantiate the class
CLOSAEvaluationSet with the directory containing the suspicious files and the directory containing the
candidate files.

### Equal number of suspicious and candidate documents

    try {
        EvaluationSet evaluationSetCLOSA = new CLOSAEvaluationSet(
                new File("~/documents/en"), "en",
                new File("~/documents/ja"), "ja"
        );

        evaluationSetCLOSA.printEvaluation();

    } catch (IOException e) {
        e.printStackTrace();
    }
    
Both folders need to contain the same amount of files because the evaluation method
 assumes a one-to-one mapping between suspicious and candidate files to use as a ground truth
 to evaluate against.
Because of this, the suspicious files and their respective candidate file have to be named the same.
If the suspicious file name contains the language code, the candidate has to contain its own language code instead, i.e.

| suspicious file name | candidate file name |
|----------------------|---------------------|
| 001028739.EN.txt     | 001028739.ZH.txt    |
| pan-0-0-en.txt       | pan-0-0-es.txt      |
| Fragment 014 05.txt  | Fragment 014 05.txt |



If you don't know the documents' languages or you are mixing languages inside the directories, you can
omit the languages parameter, but then both file names have to be exactly the same:

    try {
        EvaluationSet evaluationSetCLOSA = new CLOSAEvaluationSet(
                new File("~/fragments"),
                new File("~/sources")
        );
        
        evaluationSetCLOSA.printEvaluation();
        
    } catch (IOException e) {
        e.printStackTrace();
    }

### More candidate documents than suspicious documents

If you would like to increase the pool of possible candidate files that have no suspicious file associated,
you can add a third directory parameter:


    try {
        EvaluationSet evaluationSetCLOSA = new CLOSAEvaluationSet(
                new File("~/documents/en"), "en",
                new File("~/documents/ja"), "ja",
                new File("~/documents/extra")
        );

        evaluationSetCLOSA.printEvaluation();

    } catch (IOException e) {
        e.printStackTrace();
    }



    
How it works
------------

The CL-OSA pipeline consists of the following steps:

1. Preprocessing
    1. Tokenization and named entity recognition
    2. Text classification
    3. Entity extraction
        1. Entity filtering by topic
        2. Entity filtering by named entity type
    4. Entity disambiguation
2. Analysis
    1. Calculate cosine similarity between all document pairs
    2. Return top ranked document


Implementation details
----------------------

### Class descriptions

The classes of concern, sorted by decreasing order of relevance. For general use, the first is enough.

1. **OntologyBasedSimilarityAnalysis**: main class
2. **WikidataEntity**: represents a Wikidata entity
3. **WikidataEntityExtractor**: converts a text to a list of Wikidata entities
4. **WikidataDisambiguator**: resolves lists of Wikidata entity candidates to a single one
5. **WikidataDumpUtil**: queries entity and ontology data from the MongoDB database
6. **WikidataSparqlUtil**: queries entity properties from Wikidata's SPARQL endpoint
7. **TokenUtil**: performs tokenization on texts, including NER
8. **Dictionary**: builds inverted index dictionaries
9. **WordNetUtil**: uses WordNet to map verbs to nouns of same meaning


### Stack trace

The methods called, starting with the "main" method executeAlgorithmAndComputeScores inside OntologyUtil:

* OntologyBasedSimilarityAnalysis.executeAlgorithmAndComputeScores
    * OntologyBasedSimilarityAnalysis.preProcess
        * OntologyBasedSimilarityAnalysis.extractEntitiesFromText
            * WikidataEntityExtractor.extractEntitiesFromText
                * extractEntitiesFromTextWithoutDisambiguation
                    * TokenUtil.namedEntityTokenize
                    * TextClassificationUtil.classifyText
                    * WordNetUtil.mapVerbToNoun
                    * WikidataDumpUtil.getEntitiesByToken
                        * WikidataSparqlUtil.isInstance
                        * WikidataSparqlUtil.isLocation
                        * WikidataSparqlUtil.isHuman
                        * WikidataSparqlUtil.isOrganization
                        * WikidataSparqlUtil.isNaturalNumber
                        * WikidataSparqlUtil.isHumanLanguage
                        * WikidataSparqlUtil.getPropertyValue
                    * TokenUtil.isLatinAlphabet
                    * WikidataSparqlUtil.isGene
                    * WikidataSparqlUtil.isNaturalNumber
                * WikidataEntity.ancestorCountDisambiguate
    * OntologyBasedSimilarityAnalysis.performCosineSimilarityAnalysis
        * Dictionary
        