package corsi.vladimiro.hlm.parsing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsing logic for HTTP log files. Lines of log file are parsed
 * in {@link DataPoint} objects.
 */
public class CSVLogParser {

    /**
     * Builder class for {@link CSVLogParser}.
     */
    public static class Builder
    {
        private final BufferedReader br;
        private DataPointListener[] listeners;

        private Builder(BufferedReader br) {
            this.br = br;
        }

        /**
         * @param listeners list of {@link DataPointListener} objects which
         *                  will be notified upon parsing of each {@link DataPoint}.
         * @return this builder.
         */
        public Builder withListeners(@Nonnull DataPointListener... listeners)
        {
            for (var listener : listeners)
            {
                Preconditions.checkNotNull(listener);
            }
            this.listeners = listeners;
            return this;
        }

        /**
         * @return A new {@link CSVLogParser}.
         */
        public CSVLogParser build()
        {
            var parser = new CSVLogParser(this.br);
            if (this.listeners != null)
            {
               parser.listeners.addAll(List.of(this.listeners));
            }
            return parser;
        }
    }

    private final BufferedReader reader;
    private final ArrayList<DataPointListener> listeners;

    /**
     * @param br A {@link BufferedReader} to read log lines.
     * @return a new {@link Builder}.
     */
    public static Builder builder(@Nonnull BufferedReader br)
    {
        Preconditions.checkNotNull(br);
        return new Builder(br);
    }

    private CSVLogParser(@Nonnull BufferedReader br) {
        this.reader = br;
        this.listeners = new ArrayList<>();
    }

    /**
     * Start parsing of log lines.
     * @throws IOException in case of I/O error.
     * @throws CsvValidationException if file is not in valid CSV format.
     */
    public void parse() throws IOException, CsvValidationException {
        try (CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build())
        {
            String[] values;
            while ((values = csvReader.readNext()) != null) {
                try {
                    var dataPoint = parseDataPoint(values);
                    for (var listener : listeners){
                        listener.onDataPoint(dataPoint);
                    }
                } catch (DataPointParseException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Nonnull
    private static DataPoint parseDataPoint(@Nonnull String[] values) throws DataPointParseException {
        try {
            long timestamp = Long.parseLong(values[3]);
            String section = parseSection(values[4]);
            String status = values[5];
            long bytes = Long.parseLong(values[6]);
            String remoteHost = values[0];
            return new DataPoint(
                    timestamp,
                    section,
                    status,
                    bytes,
                    remoteHost
            );
        } catch (Exception e) {
            throw new DataPointParseException(e);
        }
    }

    @Nonnull
    @VisibleForTesting
    static String parseSection(@Nonnull String request) {
        Preconditions.checkNotNull(request);
        int begin = request.indexOf("/");
        int end = request.indexOf("/", begin + 1);
        if (end < 0)
        {
            end = request.indexOf(" HTTP");
        } else
        {
            end = Math.min(end, request.indexOf(" HTTP"));
        }
        return request.substring(begin, end);
    }

    private static class DataPointParseException extends Exception {
        public DataPointParseException(Exception e) {
            super(e);
        }
    }
}
