package com.iandadesign.closa.evaluation;

import com.iandadesign.closa.language.LanguageDetector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class for an evaluationSet, containing suspicious and candidate documents.
 * Can perform evaluation on the documents.
 */
public abstract class EvaluationSet {

    // documents
    protected Map<String, String> suspiciousIdCandidateIdMap = new HashMap<>();
    protected Map<String, List<String>> candidateIdTokensMap = new HashMap<>();
    protected Map<String, List<String>> suspiciousIdTokensMap = new HashMap<>();

    protected Map<String, Map<String, Double>> suspiciousIdCandidateScoresMap;

    protected Map<String, String> suspiciousIdLanguageMap = new HashMap<>();
    protected Map<String, String> candidateIdLanguageMap = new HashMap<>();

    protected Set<String> documentLanguages = new HashSet<>();

    private LanguageDetector languageDetector = new LanguageDetector();

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder contains the suspicious files
     * @param candidateFolder  contains the candidate files, named identically to the suspicious ones.
     */
    public EvaluationSet(File suspiciousFolder, File candidateFolder) throws IOException {
        this(suspiciousFolder, candidateFolder, (int) Files.walk(candidateFolder.toPath()).parallel().filter(path -> !path.toFile().isDirectory()).count());
    }

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @throws IOException if files cannot be accessed.
     */
    public EvaluationSet(File suspiciousFolder, String suspiciousLanguage,
                         File candidateFolder, String candidateLanguage) throws IOException {
        this(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage,
                (int) Files.walk(candidateFolder.toPath()).parallel().filter(path -> !path.toFile().isDirectory()).count());
    }

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder contains the suspicious files
     * @param candidateFolder  contains the candidate files, named identically to the suspicious ones.
     */
    public EvaluationSet(File suspiciousFolder, File candidateFolder, int fileCountLimit) {

        System.out.println("Analyzing " + fileCountLimit + " file pairs...");

        FileUtils.listFiles(suspiciousFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .filter(file -> !file.getName().substring(0, 1).equals("_"))
                .limit(fileCountLimit)
                .forEach(suspiciousFile -> {
                    String parentDirectory = suspiciousFile.getParentFile().getName();
                    String candidateFileName = suspiciousFile.getName();

                    File candidateFile = new File(candidateFolder
                            + "/" + parentDirectory + "/" + candidateFileName);

                    initializeOneFilePair(suspiciousFile, candidateFile);
                });
    }

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder   contains the suspicious files
     * @param suspiciousLanguage suspicious files' language
     * @param candidateFolder    contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage  candidate files' language
     * @param fileCountLimit     limits the number of files that should be considered.
     */
    public EvaluationSet(File suspiciousFolder, String suspiciousLanguage,
                         File candidateFolder, String candidateLanguage,
                         int fileCountLimit) {

        System.out.println("Analyzing " + fileCountLimit + " file pairs... (" + this.getClass().getSimpleName() + ")");

        Map<File, File> files = FileUtils.listFiles(suspiciousFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .filter(file -> !file.getName().substring(0, 1).equals("_"))
                .collect(Collectors.toMap(file -> file,
                        file -> {
                            // suspicious file to candidate file mapping
                            String parentDirectory = file.getParentFile().getName();
                            parentDirectory = parentDirectory.length() == 2 ? "" : (parentDirectory + "/");

                            String candidateFileName;

                            if (file.getName().toLowerCase().contains(suspiciousLanguage)) {
                                if (!suspiciousLanguage.equals(candidateLanguage)) {

                                    if (file.getName().contains(suspiciousLanguage.toUpperCase())) {
                                        candidateFileName = file.getName().replace(suspiciousLanguage.toUpperCase(),
                                                candidateLanguage.toUpperCase());
                                    } else {
                                        candidateFileName = file.getName().replace(suspiciousLanguage, candidateLanguage);
                                    }
                                } else {
                                    candidateFileName = file.getName().replace(suspiciousLanguage.toUpperCase(),
                                            "ZH");
                                }
                            } else {
                                candidateFileName = file.getName();
                            }

                            return new File(candidateFolder
                                    + "/" + parentDirectory + candidateFileName);
                        }))
                .entrySet().stream()
                .filter(entry -> entry.getValue().exists())
                .limit(fileCountLimit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<File> keys = new ArrayList<>(files.keySet());

        IntStream.range(0, keys.size())
                .parallel()
                .forEach(i -> {
                    System.out.println("Initialize alignment " + (i + 1) + " of " + (keys.size() + 1) + ":");
                    initializeOneFilePair(keys.get(i), suspiciousLanguage, files.get(keys.get(i)), candidateLanguage);
                });
    }


    /**
     * Initializes the evaluationSet. The files are in the same folder, distinguished by a one letter
     * suffix.
     *
     * @param folder           contains all files
     * @param suspiciousSuffix suspicious files end with this string
     * @param candidateSuffix  candidate files end with that string
     */
    public EvaluationSet(File folder, String suspiciousSuffix, String candidateSuffix) {
        System.out.println("Analyzing " + FileUtils.listFiles(folder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .size() + " files... (" + this.getClass().getSimpleName() + ")");

        Map<File, File> files = FileUtils.listFiles(folder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .filter(file -> !file.getName().substring(0, 1).equals("_"))
                .filter(file -> file.getName().endsWith(suspiciousSuffix + ".txt"))
                .collect(Collectors.toMap(suspiciousFile -> suspiciousFile,
                        suspiciousFile -> {
                            // suspicious file to candidate file mapping
                            return new File(String.format("%s/%s", suspiciousFile.getParent(), suspiciousFile.getName().replace(suspiciousSuffix, candidateSuffix)));
                        }))
                .entrySet().stream()
                .filter(entry -> Files.exists(entry.getValue().toPath()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<File> keys = new ArrayList<>(files.keySet());

        for (int i = 0; i < keys.size(); i++) {
            System.out.println("Initialize alignment " + (i + 1) + " of " + (keys.size() + 1) + ":");
            initializeOneFilePair(keys.get(i), files.get(keys.get(i)));
        }
    }

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder     contains the suspicious files
     * @param suspiciousLanguage   suspicious files' language
     * @param candidateFolder      contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage    candidate files' language
     * @param extraCandidateFolder extra candidate files, without corresponding suspicious files.
     * @param fileCountLimit       limits the number of files that should be considered.
     */
    public EvaluationSet(File suspiciousFolder, String suspiciousLanguage,
                         File candidateFolder, String candidateLanguage,
                         File extraCandidateFolder, int fileCountLimit) {
        this(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, fileCountLimit);

        FileUtils.listFiles(extraCandidateFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                .stream()
                .sorted()
                .filter(file -> !file.getName().equals(".DS_Store"))
                .filter(file -> !file.getName().substring(0, 1).equals("_"))
                .forEach(extraCandidateFile -> {
                    List<String> extraCandidateTokens = preProcess(extraCandidateFile.getPath(),
                            candidateLanguage);

                    saveDocumentTokensToFile(extraCandidateFile.getPath(), extraCandidateTokens);

                    candidateIdTokensMap.put(extraCandidateFile.getPath(), extraCandidateTokens);
                });
    }

    /**
     * Initializes the evaluationSet. The files have to be named identically, only the directories
     * should be named differently.
     *
     * @param suspiciousFolder     contains the suspicious files
     * @param suspiciousLanguage   suspicious files' language
     * @param candidateFolder      contains the candidate files, named identically to the suspicious ones.
     * @param candidateLanguage    candidate files' language
     * @param extraCandidateFolder extra candidate files, without corresponding suspicious files.
     * @throws IOException if files cannot be accessed.
     */
    public EvaluationSet(File suspiciousFolder, String suspiciousLanguage,
                         File candidateFolder, String candidateLanguage,
                         File extraCandidateFolder) throws IOException {
        this(suspiciousFolder, suspiciousLanguage, candidateFolder, candidateLanguage, extraCandidateFolder,
                (int) Files.walk(candidateFolder.toPath()).parallel().filter(path -> !path.toFile().isDirectory()).count());
    }


    /**
     * Initializes one file pair.
     *
     * @param suspiciousFile     suspicious file
     * @param suspiciousLanguage suspicious file's language
     * @param candidateFile      matching candidate file
     * @param candidateLanguage  candidate file's language
     */
    private void initializeOneFilePair(File suspiciousFile, String suspiciousLanguage, File candidateFile, String candidateLanguage) {
        System.out.println("Preprocessing \t" + suspiciousFile.getPath());
        System.out.println("and \t\t" + candidateFile.getPath());

        documentLanguages.add(suspiciousLanguage);
        documentLanguages.add(candidateLanguage);

        List<String> suspiciousTokens = preProcess(suspiciousFile.getPath(), suspiciousLanguage);
        saveDocumentTokensToFile(suspiciousFile.getPath(), suspiciousTokens);

        List<String> candidateTokens = preProcess(candidateFile.getPath(), candidateLanguage);
        saveDocumentTokensToFile(candidateFile.getPath(), candidateTokens);

        suspiciousIdLanguageMap.put(suspiciousFile.getPath(), suspiciousLanguage);
        candidateIdLanguageMap.put(candidateFile.getPath(), candidateLanguage);

        suspiciousIdTokensMap.put(suspiciousFile.getPath(), suspiciousTokens);
        candidateIdTokensMap.put(candidateFile.getPath(), candidateTokens);

        suspiciousIdCandidateIdMap.put(suspiciousFile.getPath(), candidateFile.getPath());

        System.out.println("Preprocessed.");
    }

    /**
     * Initializes one file pair.
     *
     * @param suspiciousFile suspicious file
     * @param candidateFile  matching candidate file
     */
    private void initializeOneFilePair(File suspiciousFile, File candidateFile) {
        try {
            initializeOneFilePair(suspiciousFile,
                    languageDetector.detectLanguage(FileUtils.readFileToString(suspiciousFile, StandardCharsets.UTF_8)),
                    candidateFile,
                    languageDetector.detectLanguage(FileUtils.readFileToString(candidateFile, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Abstract method that should be used for document processing.
     *
     * @param documentPath     document's path
     * @param documentLanguage the document's language
     * @return the preprocessed document as token list.
     */
    protected abstract List<String> preProcess(String documentPath, String documentLanguage);


    /**
     * Abstract method that should perform the retrieval algorithm.
     */
    protected abstract void performAnalysis();


    /**
     * Prints the evaluation results with precision, recall and f-measure.
     */
    public void printEvaluation() {
        // perform analysis
        performAnalysis();

        Map<Float, Integer> alignedDocumentSimilarities = new HashMap<>();

        double meanReciprocalRank = 0.0;

        for (String suspiciousId : suspiciousIdCandidateScoresMap.keySet()) {
            String candidateId = suspiciousIdCandidateIdMap.get(suspiciousId);

            if (suspiciousIdCandidateScoresMap.get(suspiciousId).containsKey(candidateId)) {
                Float alignedDocumentSimilarityPercent = (float) (suspiciousIdCandidateScoresMap.get(suspiciousId).get(candidateId) * 100);
                alignedDocumentSimilarityPercent = Math.round(alignedDocumentSimilarityPercent / 10.0f) * 10.0f;
                if (alignedDocumentSimilarities.containsKey(alignedDocumentSimilarityPercent)) {
                    alignedDocumentSimilarities.put(alignedDocumentSimilarityPercent, alignedDocumentSimilarities.get(alignedDocumentSimilarityPercent) + 1);
                } else {
                    alignedDocumentSimilarities.put(alignedDocumentSimilarityPercent, 1);
                }
            }

            int rank = 1;

            List<String> candidatesSorted = suspiciousIdCandidateScoresMap.get(suspiciousId).entrySet()
                    .stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .map(Map.Entry::getKey).collect(Collectors.toList());


            for (String currentCandidateId : candidatesSorted) {
                if (currentCandidateId.equals(candidateId)) {
                    System.out.println("Candidate " + candidateId + " on rank " + rank);
                    meanReciprocalRank += 1.0 / rank;
                    break;
                }
                rank++;
            }
        }

        Map<Float, Float> alignedDocumentSimilaritiesPercent = alignedDocumentSimilarities.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (float) e.getValue() * 100.0f / suspiciousIdCandidateScoresMap.size()));

        meanReciprocalRank = meanReciprocalRank / suspiciousIdCandidateIdMap.size();

        List<Float> precisions = new ArrayList<>();
        List<Float> recalls = new ArrayList<>();
        List<Float> fMeasures = new ArrayList<>();

        StringBuilder evaluation = new StringBuilder();

        int maximumRank = 50;

        List<Integer> ranks = Arrays.asList(1, 2, 3, 5, 10, 20, 50);

        for (int currentRank : ranks) {

            System.out.println();
            evaluation.append("Values for ranks 1 to ").append(currentRank).append(":\n\n");

            // print evaluation
            int truePositives = 0;
            int falsePositives = 0;
            int selectedElements = 0;
            int relevantElements = suspiciousIdTokensMap.size();
            int collectionSize = candidateIdTokensMap.size();
            int irrelevantElements = collectionSize - relevantElements;

            for (String suspiciousId : suspiciousIdCandidateScoresMap.keySet()) {
                int candidateCount = suspiciousIdCandidateScoresMap.get(suspiciousId).size();


                if (candidateCount > 0) {
                    selectedElements += suspiciousIdCandidateScoresMap
                            .get(suspiciousId)
                            .entrySet()
                            .stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .limit(currentRank)
                            .collect(Collectors.toList())
                            .size();

                    if (suspiciousIdCandidateScoresMap
                            .get(suspiciousId)
                            .entrySet()
                            .stream()
                            .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                            .limit(currentRank)
                            .map(Map.Entry::getKey)
                            .collect(Collectors.toList())
                            .contains(suspiciousIdCandidateIdMap.get(suspiciousId))) {
                        truePositives += 1;

                    } else {
                        falsePositives += 1;
                    }
                }
            }

            evaluation.append("True positives: ").append(truePositives).append("\n");
            evaluation.append("Relevant elements: ").append(relevantElements).append("\n");
            evaluation.append("Irrelevant elements: ").append(irrelevantElements).append("\n");
            evaluation.append("Collection size: ").append(collectionSize).append("\n");
            evaluation.append("Selected elements: ").append(selectedElements).append("\n");
            evaluation.append("False positives: ").append(falsePositives).append("\n");
            evaluation.append("False negatives: ").append(relevantElements - truePositives).append("\n");

            float precision = (float) truePositives / selectedElements;
            float recall = (float) truePositives / relevantElements;
            float fMeasure = 2.0f * (precision * recall) / (precision + recall);

            precisions.add(precision * 100.0f);
            recalls.add(recall * 100.0f);
            fMeasures.add(fMeasure * 100.0f);
        }


        evaluation.append("\n").append("Ranks 1 to ").append(maximumRank);
        evaluation.append("\n\n");
        evaluation.append("Precision: ").append(precisions).append("\n");
        evaluation.append("Recall: ").append(recalls).append("\n");
        evaluation.append("F-Measure: ").append(fMeasures);
        evaluation.append("\n\n").append("Mean reciprocal rank: ").append(meanReciprocalRank * 100.0f).append("\n");
        evaluation.append("\n\n").append("Aligned document similarities");
        evaluation.append("\n\n");
        evaluation.append(alignedDocumentSimilarities).append("\n\n");
        evaluation.append(alignedDocumentSimilaritiesPercent);

        System.out.println(evaluation);

        File evaluationFile = new File(String.format("%s/preprocessed/evaluation.txt", System.getProperty("user.home")));
        try {
            FileUtils.writeStringToFile(evaluationFile, evaluation.toString(), "UTF-8");
            System.out.println("Evaluation written to " + evaluationFile.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Count files in directory and subdirectories.
     *
     * @param directory parent directory
     * @return file count in directory and subdirectories.
     */
    private long fileCount(Path directory) {
        try {
            return Files.walk(directory)
                    .parallel()
                    .filter(path -> !path.toFile().isDirectory())
                    .count();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Method that should be used for saving intermediate results.
     *
     * @param originalDocumentPath : path to the original document.
     * @param documentTokens       : the tokens that have been extracted from the document.
     */
    protected void saveDocumentTokensToFile(String originalDocumentPath, List<String> documentTokens) {
        if (documentTokens == null) {
            throw new IllegalArgumentException("Document tokens have to be non-null.");
        }

        Path newFullPath;
        if (originalDocumentPath.contains("evaluation")) {
            newFullPath = Paths.get(originalDocumentPath.replace("evaluation", "evaluation/preprocessed/" + this.getClass().getSimpleName()));
        } else {
            newFullPath = Paths.get(originalDocumentPath.replace(System.getProperty("user.home"),
                    System.getProperty("user.home") + "/preprocessed/" + this.getClass().getSimpleName()));
        }

        File newFile = new File(newFullPath.toString());

        try {
            if (!Files.exists(newFullPath.getParent())) {
                Files.createDirectories(newFullPath.getParent());
            }

            if (Files.exists(newFullPath)) {
                Files.delete(newFullPath);
            }
            Files.createFile(newFullPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(newFile));

            for (String token : documentTokens) {
                writer.write(token);
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                assert writer != null;
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}