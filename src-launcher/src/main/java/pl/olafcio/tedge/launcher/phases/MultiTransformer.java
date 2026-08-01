package pl.olafcio.tedge.launcher.phases;

import pl.olafcio.tedge.jargen.Transformer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class MultiTransformer {
    protected final HashMap<String, String> toTransform
              = new HashMap<>();

    public String append(Path jar) {
        var jarstr = jar.toString();

        var transformedJarN = jarstr.substring(0, jarstr.length() - 4) + "-transformed.jar";

        toTransform.put(jarstr, transformedJarN);

        return transformedJarN;
    }

    public void transform() {
        boolean said = false;

        for (var entry : toTransform.entrySet()) {
            var transformedJarN = entry.getValue();
            var transformedJar = Path.of(transformedJarN);

            if (!Files.exists(transformedJar)) {
                if (!said) {
                    said = true;

                    IO.println("-----------------------------------");
                    IO.println("    Initially transforming JARs    ");
                    IO.println("-----------------------------------\n");
                }

                IO.println(">> " + transformedJarN);

                var transformer = new Transformer();
                var jarstr = entry.getKey();

                transformer.transform(jarstr);
                transformer.write(transformedJarN);
            }
        }
    }
}
