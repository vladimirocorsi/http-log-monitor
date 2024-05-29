package corsi.vladimiro.hlm;

import corsi.vladimiro.hlm.parsing.CSVLogParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Runs the HTTP log parser.
 * Can be invoked with:
 * <ul>
 *     <li>No arguments: in this case input is expected from stdin.</li>
 *     <li>The path to the log file as the only argument.</li>
 * </ul>
 */
public class Main {

    public static void main(String[] args)
    {
        try {
            final InputStream is;

            switch (args.length) {
                case 0 -> is = System.in;
                case 1 -> is = new FileInputStream(args[0]);
                default -> throw new IllegalArgumentException("Too many arguments");
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
            {
                var parser = CSVLogParser.builder(br)
                        .withListeners(new AlertDataPointListenerV2(), new StatDataPointListener())
                        .build();
                parser.parse();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
