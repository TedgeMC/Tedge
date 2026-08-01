package pl.olafcio.tedge.launcher.util;

import java.nio.file.Path;

public enum Paths {
    ;

    public static final Path BASE_PATH
                      = Path.of(System.getenv("USERPROFILE") + "/.gradle/caches/Tedge-ML");

    public static final Path LIBRARIES_PATH
                      = BASE_PATH.resolve("libraries");
}
