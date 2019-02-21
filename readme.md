CL-OSA (cross-language ontology-based similarity analysis) readme
=================================================================

Description
-----------

CL-OSA is an algorithm to retrieve similar documents written in different languages by leveraging entity and ontology information from Wikidata.

The algorithm can be used to assist in plagiarism detection by performing ranked retrieval of potential source documents for a suspicious input document.

The input is
* a suspicious document (.txt)
* a list of candidate documents (.txt)

The output is
* the most similar candidate document.

Supported languages are:
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
    
inside the *citeplag-dev-backend/pds-backend-core* directory. Due to a problem with Maven repositories,
you might encounter the problem that the Atilika Kuromoji dependency could not be resolved. In this case,
download it from [Atilika's GitHub repository](https://github.com/atilika/kuromoji/downloads) and
import it into the Maven project manually:

    mvn install:install-file -Dfile=~/kuromoji-0.7.7/lib/kuromoji-0.7.7.jar -DgroupId=org.atilika.kuromoji \
        -DartifactId=kuromoji -Dversion=0.7.7 -Dpackaging=jar
        
If you do not need Japanese language support, you can remove the dependency from the .pom file and delete
the method namedEntityTokenizeJapanese inside TokenUtil. 
This does not affect the other languages.

Usage
-----

Now, you can test you text documents. 

Use OntologyUtil's method "executeAlgorithmAndGetCandidates". First argument is the suspicious file path
and second argument is the candidate file path.

    String suspiciousPath = "~/documents/en/35157967/0.txt";
    String candidateFolderPath = "~/documents/ja/";

    List<String> candidatePaths = FileUtils.listFiles(new File(candidateFolderPath), TrueFileFilter.TRUE, TrueFileFilter.TRUE)
         .stream()
         .sorted()
         .filter(file -> !file.getName().equals(".DS_Store"))
         .map(File::getPath)
         .collect(Collectors.toList());

    List<String> candidates = OntologyUtil.executeAlgorithmAndGetCandidates(suspiciousPath, candidatePaths);

    System.out.println(candidates);
    
The output is a ranked list of candidates, with the first being the top-ranked document.
By default, CL-OSA returns only the top-ranked document.

<!--- VVV
Good would be some result examples or even a reference/link to test class with which you can try a mini example.
Units test with some resource files with a simple example would be perfect.
---> 

Pre-processing steps will be saved to a directory named *preprocessed* which will be created in your
home directory if the input documents are also in the home directory. Otherwise, the directory will be created in
the root directory.

The [test class](/pds-backend-core/src/test/java/org/sciplore/pds/util/OntologyUtilTest.java)
contains unit tests as demonstration. It uses the test resource files that come with the Java project.


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

1. **OntologyUtil**: main class
2. **WikidataEntity**: represents a Wikidata entity
3. **WikidataEntityExtractor**: converts a text to a list of Wikidata entities
4. **WikidataDisambiguator**: resolves lists of Wikidata entity candidates to a single one
5. **WikidataDumpUtil**: queries entity and ontology data from the MongoDB database
6. **WikidataSparqlUtil**: queries entity properties from Wikidata's SPARQL endpoint
7. **TokenUtil**: performs tokenization on texts, including NER
8. **Dictionary**: builds inverted index dictionaries
9. **WordNetUtil**: uses WordNet to map verbs to nouns of same meaning


### Stack trace

The methods called, starting with the "main" method executeAlgorithmAndGetCandidates inside OntologyUtil:

* OntologyUtil.executeAlgorithmAndGetCandidates
    * OntologyUtil.preProcess
        * OntologyUtil.extractEntitiesFromText
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
    * OntologyUtil.performCosineSimilarityAnalysis
        * Dictionary
        
        
### HyPlag integration

When using CL-OSA as part of HyPlag, it can be used like any other heuristic algorithm that is included 
with HyPlag:

    curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' -d '{ \ 
       "algorithmIds": [ \ 
         "closa" \ 
       ], \ 
       "scopes": [ \ 
         "0" \ 
       ], \ 
       "selectedDocumentIds": [ \ 
         1, 2, 3 \ 
       ], \ 
       "sourceDocumentId": 0 \ 
     }' 'http://localhost:8080/detection?external_id=1&resultTimeout=600'

The result can be obtained like such:

    curl -X GET --header 'Accept: application/json' 'http://localhost:8080/result/algorithm/closa?srcDocId=0&selectedDocIds=1%2C%202%2C%203'

Integration into the current HyPlag system is not yet completed, but it works on the current configuration.
Configuration will be moved to the *config/example.application.yaml* file, including the following parameters:

* Choice of local MongoDB or SPARQL endpoint usage (default is MongoDB)
* MongoDB server, port and database names (defaults are localhost, 27017, wikidata)
* Number of output documents for ranked retrieval (default is 1)
* API keys for TextClassificationUtil

<!--- VVV 
A small section on integration with HyPlag can still be done.
In theory, the algorithms are also usable when the system is running as a server application and works
with indexed documents from the DB / ES, correct?
At least that's what it looks like to me, when I look at the algorithms.
On the actual practical site a few changes are necessary, but that can be stated.
--->
