package com.iandadesign.closa.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Some random functions for writing data to csv from the web.
 *
 * @author Johannes Stegmüller
 */
public class CSVUtil {

    public static String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public static String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(CSVUtil::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }
}
