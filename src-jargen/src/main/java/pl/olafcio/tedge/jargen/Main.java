package pl.olafcio.tedge.jargen;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * The main class of the install-phase jar transformer.
 */
@NullMarked
public class Main {
    public static boolean verbose = false;

    static void main(String[] args_raw) {
        var args = new ArrayList<>(List.of(args_raw));

        verbose = (args.remove("-v") || args.remove("--verbose"));

        if (args.size() != 1) {
            IO.println("Usage: java -jar ... [filepath]");
            System.exit(1);
        }

        if (!args.get(0).endsWith(".jar")) {
            IO.println("Error: Cannot transform a non-.jar file");
            System.exit(1);
        }

        var transformer = new Transformer();

        transformer.transform(args.get(0));
        transformer.write(args.get(0).substring(0, args.get(0).length() - 4) + "-transformed.jar");
    }
}
