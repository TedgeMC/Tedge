package pl.olafcio.tedge.launcher.endpoints;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import pl.olafcio.tedge.launcher.util.Paths;
import pl.olafcio.tedge.launcher.util.Requests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class MinecraftController {
    public static JsonObject downloadVersionsJSON()
           throws IOException
    {
        var versionsBytes = Requests.get("https://piston-meta.mojang.com/mc/game/version_manifest.json");
        var versionsJSON = new Gson().fromJson(new String(versionsBytes, StandardCharsets.UTF_8), JsonObject.class);

        Files.createDirectories(Paths.BASE_PATH);
        Files.writeString(Paths.BASE_PATH.resolve("version_manifest.json"), new Gson().toJson(versionsJSON), StandardCharsets.UTF_8);

        return versionsJSON;
    }
}
