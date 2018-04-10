package io.github.stemlab.androidsensorreader.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVUtils {
    private static final char SEPARATOR = ',';

    public static void writeLine(File filename, List<String> values) throws IOException {
        writeLine(filename, values, SEPARATOR, ' ');
    }

    public static void writeLine(File filename, float[] values) throws IOException {
        List<String> lines = new ArrayList<>(values.length);
        for (float f : values) {
            lines.add(String.valueOf(f));
        }
        writeLine(filename, lines, SEPARATOR, ' ');
    }

    private static String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    public static void writeLine(File file, List<String> values, char separators, char customQuote) throws IOException {

        boolean first = true;

        if (separators == ' ') {
            separators = SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
            }

            first = false;
        }
        sb.append("\n");
        FileOutputStream stream = new FileOutputStream(file, true);
        try {
            stream.write(sb.toString().getBytes());
        } finally {
            stream.close();
        }
    }
}
