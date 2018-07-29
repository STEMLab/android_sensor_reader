package io.github.stemlab.androidsensorreader.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.github.stemlab.androidsensorreader.pojo.Location;
import io.github.stemlab.androidsensorreader.pojo.Signal;

public class CSVUtils {
    private static final char SEPARATOR = ',';

    public static void writeLine(File filename, List<String> values) throws IOException {
        writeLine(filename, values, SEPARATOR, ' ');
    }

    public static void writeSignal(File filename, List<HashMap> signals) throws IOException {

        for (HashMap map : signals) {
            List<String> values = new ArrayList<>();
            String gyroscope = "{'Gyroscope':" + " [" + String.format("%f", ((Signal) map.get("Gyroscope")).getX()) + ", "
                    + String.format("%f", ((Signal) map.get("Gyroscope")).getY()) + ", " + String.format("%f", ((Signal) map.get("Gyroscope")).getZ()) + "]";
            String accelerometer = "'Accelerometer':" + " [" + String.format("%f", ((Signal) map.get("Accelerometer")).getX()) + ", "
                    + String.format("%f", ((Signal) map.get("Accelerometer")).getY()) + ", " + String.format("%f", ((Signal) map.get("Accelerometer")).getZ()) + "]";
            String time = "'Time':" + String.format("%d", (long) map.get("Time"));
            String location = "'Location':" + " [" + BigDecimal.valueOf(((Location) map.get("Location")).getLatitude()).toPlainString() + ", "
                    + BigDecimal.valueOf(((Location) map.get("Location")).getLongitude()).toPlainString() + ", " + String.format("%f", ((Location) map.get("Location")).getAccuracy()) + "]" + "}";
            values.add(gyroscope);
            values.add(accelerometer);
            values.add(time);
            values.add(location);
            writeLine(filename, values, SEPARATOR, ' ');
        }

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
