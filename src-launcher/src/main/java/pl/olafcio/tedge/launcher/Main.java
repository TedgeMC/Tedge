package pl.olafcio.tedge.launcher;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import pl.olafcio.tedge.launcher.endpoints.MavenController;
import pl.olafcio.tedge.launcher.endpoints.MinecraftController;
import pl.olafcio.tedge.launcher.phases.DuplicateReducer;
import pl.olafcio.tedge.launcher.phases.MultiTransformer;
import pl.olafcio.tedge.launcher.util.Paths;
import pl.olafcio.tedge.launcher.util.Requests;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.zip.ZipFile;

/**
 * The main class for the dev launcher.
 */
@NullMarked
public class Main {
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
            obj = findVersion(id, MinecraftController.downloadVersionsJSON());

            if (obj == null) {
                IO.println("[Launcher] Version '%s' not found".formatted(id));
                return;
            }
        }

        var versionPath = Paths.BASE_PATH.resolve(id);
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

        Path jarClient;
        Path jarServer;
        Path jarShared;

        var classpathClient = new ArrayList<String>();
        var classpathServer = new ArrayList<String>();

        boolean jargenPending = true;
        boolean classpathPending = false;

        if (
                (jarClient = versionPath.resolve("client-only-transformed.jar")).toFile().isFile() &&
                (jarServer = versionPath.resolve("server-only-transformed.jar")).toFile().isFile() &&
                (jarShared = versionPath.resolve("shared-transformed.jar")).toFile().isFile()
        ) {
            MavenController.downloadMavenLibraries(versionLibs, classpathClient);

            IO.println("[Launcher] All reduced resources on disk.");

            jargenPending = false;
        } else if (
                (jarClient = versionPath.resolve("client-only.jar")).toFile().isFile() &&
                (jarServer = versionPath.resolve("server-only.jar")).toFile().isFile() &&
                (jarShared = versionPath.resolve("shared.jar")).toFile().isFile()
        ) {
            MavenController.downloadMavenLibraries(versionLibs, classpathClient);

            IO.println("[Launcher] All internet resources on disk.");
        } else {
                jarClient        = resolve(versionPath, "client.jar",         versionJSON, "client");
            var jarServerWrapper = resolve(versionPath, "server-wrapper.jar", versionJSON, "server");

            MavenController.downloadMavenLibraries(versionLibs, classpathClient);

            IO.println("[Launcher] All internet resources on disk.");

            try (var zip = new ZipFile(jarServerWrapper.toFile())) {
                var entry = zip.getEntry("META-INF/versions/%1$s/server-%1$s.jar".formatted(obj.get("id").getAsString()));

                try (var stream = zip.getInputStream(entry)) {
                    jarServer = versionPath.resolve("server.jar");

                    try (var file = new FileOutputStream(jarServer.toFile())) {
                        stream.transferTo(file);
                    }
                }

                IO.println("[Launcher] Extracted server version JAR.");

                var entries = zip.entries();

                saving:
                    while (entries.hasMoreElements()) {
                        var el = entries.nextElement();
                        var name = el.getName();

                        if (el.isDirectory())
                            continue;

                        if (name.contains("libraries")) {
                            name = name.substring(name.indexOf("libraries") + 9)
                                       .replace("\\", "/");

                            if (name.contains("//") || name.contains("..")) {
                                IO.println("[Launcher] Blocked suspicious library: '%s'".formatted(name));
                                continue;
                            }

                            var chars = name.toCharArray();
                            for (char ch : chars) {
                                if ("0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM-_./".indexOf(ch) == -1) {
                                    IO.println("[Launcher] Blocked suspicious library: '%s'".formatted(name));
                                    continue saving;
                                }
                            }

                            if (name.startsWith("/"))
                                name = name.substring(1);

                            if (name.isEmpty())
                                continue;

                            try (var stream = zip.getInputStream(el)) {
                                Path path = Paths.LIBRARIES_PATH.resolve(name);

                                Files.createDirectories(path.getParent());

                                try (var file = new FileOutputStream(path.toFile())) {
                                    stream.transferTo(file);
                                }

                                classpathServer.add(path.toString());
                            }
                        }
                    }
            }

            Files.delete(jarServerWrapper);

            var reducer = new DuplicateReducer(jarClient, jarServer, versionPath);
            reducer.reduce();

            jarClient = reducer.clientJAR();
            jarServer = reducer.serverJAR();
            jarShared = reducer.sharedJAR();

            IO.println("[Launcher] %s version '%s'".formatted("Prepared", id));
            IO.println("[Launcher] (file path: %s)".formatted("(file:///%s)".formatted(manifestPath.toString().replace("\\", "/"))));

            classpathPending = true;
        }

        IO.println("----------------------------------\n");

        // Finally, use the jargen
        var transformer = new MultiTransformer();

        String transformedShared = transformer.append(jarShared);

        classpathClient.add(transformedShared);
        classpathServer.add(transformedShared);

        classpathClient.add(transformer.append(jarClient));
        classpathServer.add(transformer.append(jarServer));

        if (jargenPending)
            transformer.transform();

        // Save the classpath
        if (classpathPending) {
            IO.println("----------------------------------");
            IO.println("         Saving classpath         ");
            IO.println("----------------------------------\n");

            Files.writeString(versionPath.resolve("classpath.txt"), "-cp " + String.join(";", classpathClient));
            Files.writeString(versionPath.resolve("classpath-server.txt"), "-cp " + String.join(";", classpathServer));
        }
    }

    private static Path resolve(Path versionPath, String storedName, JsonObject versionJSON, String type)
            throws IOException
    {
        Path jar = versionPath.resolve(storedName);

        if (!Files.isRegularFile(jar)) {
            IO.println("[Launcher] Downloading " + type);

            Files.write(jar, Requests.get(versionJSON.getAsJsonObject("downloads")
                                     .getAsJsonObject(type)
                                     .get("url")
                                     .getAsString()));
        }

        return jar;
    }

    private static JsonObject getVersionsJSON() throws IOException {
        var versionsBytes = Files.readAllBytes(Paths.BASE_PATH.resolve("version_manifest.json"));
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
