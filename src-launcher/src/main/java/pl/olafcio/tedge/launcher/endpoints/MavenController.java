package pl.olafcio.tedge.launcher.endpoints;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pl.olafcio.tedge.launcher.util.Paths;
import pl.olafcio.tedge.launcher.util.Requests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public final class MavenController {
    public static void downloadMavenLibraries(JsonArray versionLibs, ArrayList<String> classpath)
            throws IOException
    {
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
                    Arrays.asList(file_path.split("/")).contains("con")
            ) {
                IO.println("[Launcher] [WARNING] Version artifact '%s' has illegal elements".formatted(file_path));
                continue;
            }

            op:
            {
                var chars = file_path.toCharArray();

                for (char ch : chars) {
                    if ("0123456789QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm._-/".indexOf(ch) == -1) {
                        IO.println("[Launcher] [WARNING] Version artifact '%s' has illegal characters".formatted(file_path));
                        break op;
                    }
                }

                Path path = Paths.LIBRARIES_PATH.resolve(file_path);

                classpath.add(path.toString());

                if (Files.exists(path))
                    continue;

                Files.createDirectories(path.getParent());
                Files.write(path, Requests.get(file_url));
            }
        }
    }
}
