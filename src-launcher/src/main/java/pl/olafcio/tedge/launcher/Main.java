package pl.olafcio.tedge.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import pl.olafcio.tedge.jargen.Transformer;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The main class for the dev launcher.
 */
@NullMarked
public class Main {

    private static final Path BASE_PATH
                       = Path.of(System.getenv("USERPROFILE") + "/.gradle/caches/Tedge-ML");

    private static final Path LIBRARIES_PATH
                       = BASE_PATH.resolve("libraries");

    public static void main(String[] args) throws IOException {
//        var dry = args.length >= 1 && args[0].equals("--dry");
//        if (dry) {
//            var len = args.length - 1;
//            var args2 = new String[len];
//
//            System.arraycopy(args, 1, args2, 0, len);
//
//            args = args2;
//        }

        if (args.length != 1) {
            IO.println("Usage: java -jar [] <mc version>");
            return;
        }

        IO.println("\n----------------------------------");

        JsonObject obj;

        var id = args[0];
        try {
            obj = findVersion(id, getVersionsJSON());

            if (obj == null)
                throw new Exception();
        } catch (Exception e) {
            obj = findVersion(id, downloadVersionsJSON());

            if (obj == null) {
                IO.println("[Launcher] Version '%s' not found".formatted(id));
                return;
            }
        }

        var versionPath = BASE_PATH.resolve(id);
        Files.createDirectories(versionPath);

        var manifestURL = obj.get("url").getAsString();
        var manifestPath = versionPath.resolve("version.json");

        byte[] manifest;

        if (Files.isRegularFile(manifestPath)) {
            manifest = Files.readAllBytes(manifestPath);
        } else {
            IO.println("[Launcher] Downloading version '%s' (%s)".formatted(id, obj.get("url").getAsString()));

            manifest = Requests.get(manifestURL);

            Files.write(manifestPath, manifest, StandardOpenOption.CREATE);
        }

        var versionJSON = new Gson().fromJson(new String(manifest, StandardCharsets.UTF_8), JsonObject.class);
        var versionLibs = versionJSON.getAsJsonArray("libraries");

        var javaVersion = versionJSON.getAsJsonObject("javaVersion").get("majorVersion").getAsInt();
        if (javaVersion > Runtime.version().feature()) {
            IO.println("[Launcher] Version '%s' requires Java %d; using Java %d".formatted(id, javaVersion, Runtime.version().feature()));
            return;
        }

        Path jar;

        Files.write(jar = versionPath.resolve("client.jar"), Requests.get(versionJSON.getAsJsonObject("downloads")
                                                                           .getAsJsonObject("client")
                                                                           .get("url")
                                                                           .getAsString()));

//        Files.write(versionPath.resolve("server.jar"), Requests.get(versionJSON.getAsJsonObject("downloads")
//                                                                     .getAsJsonObject("server")
//                                                                     .get("url")
//                                                                     .getAsString()));

        var classpath = new ArrayList<String>();

        for (var element : versionLibs) {
            if (!element.isJsonObject())
                continue;

            var lib = (JsonObject) element;
            var downloads = lib.getAsJsonObject("downloads")
                               .getAsJsonObject("artifact");

            var file_path = downloads.get("path").getAsString().replace("../", "");
            var file_url = downloads.get("url").getAsString();

            while (file_path.startsWith("/")) file_path = file_path.substring(1);
            while (file_path.endsWith("/"))   file_path = file_path.substring(0, file_path.length() - 1);

            if (
                    file_path.contains("..") ||
                    file_path.contains("//") ||
                    Arrays.asList(file_path.split("/")).contains("con"))
            {
                IO.println("[Launcher] [WARNING] Version artifact '%s' has illegal elements".formatted(file_path));
                continue;
            }

            op:
            {
                var chars = file_path.toCharArray();

                for (char ch : chars)
                {
                    if ("0123456789QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm._-/".indexOf(ch) == -1)
                    {
                        IO.println("[Launcher] [WARNING] Version artifact '%s' has illegal characters".formatted(file_path));
                        break op;
                    }
                }

                Path path = LIBRARIES_PATH.resolve(file_path);

                classpath.add(path.toString());

                if (Files.exists(path))
                    continue;

                Files.createDirectories(path.getParent());
                Files.write(path, Requests.get(file_url));
            }
        }

        IO.println("[Launcher] %s version '%s'".formatted("Prepared", id));
        IO.println("[Launcher] (file path: %s)".formatted("(file:///%s)".formatted(manifestPath.toString().replace("\\", "/"))));

        IO.println("----------------------------------\n");

        // Finally, use the jargen
        var jarstr = jar.toString();

        var transformedJarN = jarstr.substring(0, jarstr.length() - 4) + "-transformed.jar";
        var transformedJar = Path.of(transformedJarN);

        classpath.add(transformedJarN);

        if (!Files.exists(transformedJar)) {
            IO.println("----------------------------------");
            IO.println("    Initially transforming JAR    ");
            IO.println("----------------------------------\n");

            var transformer = new Transformer();

            transformer.transform(jarstr);
            transformer.write(transformedJarN);
        }

        // Save the classpath
        IO.println("----------------------------------");
        IO.println("         Saving classpath         ");
        IO.println("----------------------------------\n");

        Files.writeString(versionPath.resolve("classpath.txt"), "-cp " + String.join(";", classpath));
    }

    private static JsonObject downloadVersionsJSON() throws IOException {
        var versionsBytes = Requests.get("https://piston-meta.mojang.com/mc/game/version_manifest.json");
        var versionsJSON = new Gson().fromJson(new String(versionsBytes, StandardCharsets.UTF_8), JsonObject.class);

        Files.createDirectories(BASE_PATH);
        Files.writeString(BASE_PATH.resolve("version_manifest.json"), new Gson().toJson(versionsJSON), StandardCharsets.UTF_8);

        return versionsJSON;
    }

    private static JsonObject getVersionsJSON() throws IOException {
        var versionsBytes = Files.readAllBytes(BASE_PATH.resolve("version_manifest.json"));
        var versionsJSON = new Gson().fromJson(new String(versionsBytes, StandardCharsets.UTF_8), JsonObject.class);

        return versionsJSON;
    }

    @Nullable
    private static JsonObject findVersion(String id, JsonObject master) {
        var array = master.getAsJsonArray("versions");
        for (var element : array) {
            if (element.isJsonObject() && element.getAsJsonObject().get("id").getAsString().equals(id)) {
                return element.getAsJsonObject();
            }
        }

        return null;
    }
}
