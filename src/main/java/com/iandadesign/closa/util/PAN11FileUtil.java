package com.iandadesign.closa.util;

import com.iandadesign.closa.language.LanguageDetector;
import com.iandadesign.closa.model.ExtendedAnalysisParameters;
import com.iandadesign.closa.model.SalvadorAnalysisParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.iandadesign.closa.model.SalvadorAnalysisParameters.LANGUAGE;

/**
 * Operations for Files, mostly for the PAN11-Plagiarism Contest.
 * @author Johannes Stegmüller
 */
public class PAN11FileUtil {
    public static int getSourceIdOfFile(File file){
        return Integer.parseInt(file.getName().replaceAll("\\D+", ""));
    }

    public static boolean removeDirectory(File directoryToBeDeleted){
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                removeDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public static void writeFileListToDirectory(List<File> filesToWrite, String directoryPath, ExtendedLogUtil logUtil){
        try {
            File dir = new File(directoryPath);
            if (!dir.exists() || !dir.isDirectory()) {
                //if the file is present then it will show the msg
                dir.mkdirs();
            }

            for (File file : filesToWrite) {
                File destinationFile = new File(directoryPath + "/"+ file.getName());
                FileUtils.copyFile(file, destinationFile);
            }
        }catch(Exception e){
            logUtil.logAndWriteError(false, "exception copying files", e.toString());

        }
    }

    public static List<File> getTextFilesFromTopLevelDir(String topFolderPath, ExtendedAnalysisParameters params, boolean candOrSusp, String filetype){
        File myFiles = new File(topFolderPath);
        final LanguageDetector langdet;
        if(params.USE_LANGUAGE_WHITELISTING){
            langdet = new LanguageDetector();
        }else{
            langdet = null;
        }

        if(!params.USE_FILE_FILTER){

            // Get all files to preprocess
            List<File> myFilesFiltered = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(filetype)) // Filter .xml files and others only take txt.
                    //.map(File::getPath)
                    .filter(file -> getDocumentLanguageAndCheckIfWhitelisted(langdet, file,params, candOrSusp))
                    .collect(Collectors.toList());
            return myFilesFiltered;
        }else{


            // Apply the local filter in preprocessing
            List<File> myFilesFiltered = FileUtils.listFiles(myFiles, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
                    .stream()
                    .filter(file -> file.getName().endsWith(filetype)) // Filter .xml files and others only take txt.
                    .filter(file -> params.panFileFilter.checkIfFilenameWhitelisted(file.getName(), candOrSusp))
                    //.map(File::getPath)
                    .filter(file -> getDocumentLanguageAndCheckIfWhitelisted(langdet, file,params, candOrSusp))
                    .collect(Collectors.toList());
            return myFilesFiltered;
        }

    }

    public static void getPlagiarismSection(){
        String suspiciousFilePath = "src/main/resources/PAN2011Test2/suspicious-document00034.txt";
        String candidateFilePath = "src/main/resources/PAN2011Test2/candidates/source-document03317.txt";
        List<Long> sectionOffsets = new ArrayList(Arrays.asList(213, 2448));
        List<Long> sectionLengths = new ArrayList(Arrays.asList(1711, 516));
        getPlagiarisedSentences(candidateFilePath,5359,1707); // Seems correct
        getPlagiarisedSentences(suspiciousFilePath,213,1711); // Seems correct
    }

    /**
     * This is a utility method for getting the plagiarized sections in the PAN2011 testset.
     * @param filepath path to file to look at
     * @param sectionOffset offset of section to check
     * @param sectionLength length of section to check
     * @return String of the plagiarised section
     */
    public static String getPlagiarisedSentences(String filepath, long sectionOffset, long sectionLength){
       StringBuilder stringBuilder = new StringBuilder();
       long characterCount = 0;
        File candidateFile = new File(filepath);
        try (FileReader fr = new FileReader(candidateFile)) {
            int content;
            while ((content = fr.read()) != -1) {
                if(characterCount>=sectionOffset && characterCount<=(sectionOffset+sectionLength)){
                    stringBuilder.append((char) content);
                }
                //System.out.print((char) content);
                characterCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String returnVAL = stringBuilder.toString();
        System.out.println("Plagiarized section for file "+filepath+" sectionOffset:"+sectionOffset+" sectionLength:"+sectionLength);
        System.out.println(returnVAL);

        return returnVAL;
    }

    public static boolean getDocumentLanguageAndCheckIfWhitelisted(LanguageDetector langdet, File fileToCheck, ExtendedAnalysisParameters params, boolean candOrSusp){
        if(!params.USE_LANGUAGE_WHITELISTING){
            return true;
        }
        if(!candOrSusp){
            return true; // dont filter suispicious files
        }
        // TODO eventually make this less redundant and log to fileoutput
        try{
            String language = langdet.detectLanguage(FileUtils.readFileToString(fileToCheck, StandardCharsets.UTF_8));
            return params.panFileFilter.checkIfLanguageWhitelisted(language);
        }catch(Exception ex){
            System.err.println("Exception during processing file "+ fileToCheck+ " " + ex.toString());
            return false;
        }


    }

    public static List<File> getRepresentativeDataset(ExtendedLogUtil logUtil, List<File> suspiciousFiles){
        logUtil.logAndWriteStandard(false, "Selecting Representative Test files,- not the complete corpus will be used");
        if(!SalvadorAnalysisParameters.SORT_SUSPICIOUS_FILES_BY_SIZE){
            logUtil.logAndWriteStandard(false, "For representative fileset files should be sorted!");
            return null;
        }
        if(SalvadorAnalysisParameters.SUSP_FILE_SELECTION_OFFSET!=0){
            logUtil.logAndWriteStandard(false, "SUSP_FILE_SELECTION_OFFSET has to be 0!");
            return null;
        }
        if(SalvadorAnalysisParameters.BATCHED_OFFSET_INCREMENT!=1){
            logUtil.logAndWriteStandard(false, "BATCHED_OFFSET_INCREMENT has to be 1!");
            return null;
        }
        if(!SalvadorAnalysisParameters.PREFILTER.equals("NONE")){
            logUtil.logAndWriteStandard(false, "PREFILTER has to be NONE");
            return null;
        }
        if(LANGUAGE.equals("es")){

            if(SalvadorAnalysisParameters.SUSP_FILE_LIMIT!=304){
                logUtil.logAndWriteStandard(false, "SUSP_FILE_LIMIT has to be 304!");
                return null;
            }

            List<File> newSuspiciousFiles = new ArrayList<>();
            /*
            newSuspiciousFiles.add(suspiciousFiles.get(1));
            newSuspiciousFiles.add(suspiciousFiles.get(124));
            newSuspiciousFiles.add(suspiciousFiles.get(125));
            newSuspiciousFiles.add(suspiciousFiles.get(197));
            newSuspiciousFiles.add(suspiciousFiles.get(198));

             */
            newSuspiciousFiles.add(suspiciousFiles.get(290));
            newSuspiciousFiles.add(suspiciousFiles.get(291));

            newSuspiciousFiles.add(suspiciousFiles.get(293));
            suspiciousFiles = newSuspiciousFiles;


        }
        if(LANGUAGE.equals("de")){
            if(SalvadorAnalysisParameters.SUSP_FILE_LIMIT!=251){
                logUtil.logAndWriteStandard(false, "SUSP_FILE_LIMIT has to be 251!");
                return null;
            }
            List<File> newSuspiciousFiles = new ArrayList<>();
            newSuspiciousFiles.add(suspiciousFiles.get(1));
            newSuspiciousFiles.add(suspiciousFiles.get(70));
            newSuspiciousFiles.add(suspiciousFiles.get(90));
            newSuspiciousFiles.add(suspiciousFiles.get(91));
            newSuspiciousFiles.add(suspiciousFiles.get(170));
            newSuspiciousFiles.add(suspiciousFiles.get(171));
            newSuspiciousFiles.add(suspiciousFiles.get(172));
            newSuspiciousFiles.add(suspiciousFiles.get(239));
            newSuspiciousFiles.add(suspiciousFiles.get(240));
            suspiciousFiles = newSuspiciousFiles;
        }
        return suspiciousFiles;
    }
}
