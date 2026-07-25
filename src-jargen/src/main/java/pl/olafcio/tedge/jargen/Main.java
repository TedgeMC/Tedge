package pl.olafcio.tedge.jargen;

import org.jspecify.annotations.NullMarked;

/**
 * The main class of the install-phase jar transformer.
 */
@NullMarked
public class Main {
    static void main(String[] args) {
        if (args.length != 1) {
            IO.println("Usage: java -jar ... [filepath]");
            System.exit(1);
        }

        if (!args[0].endsWith(".jar")) {
            IO.println("Error: Cannot transform a non-.jar file");
            System.exit(1);
        }

        var transformer = new Transformer();

        transformer.transform(args[0]);
        transformer.write(args[0].substring(0, args[0].length() - 4) + "-transformed.jar");
    }
}
