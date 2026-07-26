package pl.olafcio.tedge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.UUID;

public class Main {
    static void main(String[] args) {
        byte[] data;

        try (var stream = Main.class.getResourceAsStream("/version.json")) {
            data = stream.readAllBytes();
        } catch (IOException e) {
            IO.println("[Tedge] Failed to fetch Minecraft version; aborting");
            System.exit(1);

            return;
        }

        var gson = new Gson().fromJson(new String(data, StandardCharsets.UTF_8), JsonObject.class);
        var version = getMCVersion(gson);

        IO.println("Running Tedge-DEV on Minecraft " + version);

        if (args.length == 1) {
            if (args[0].equals("@dev")) {
                args = new String[]{
                        "--offlineDeveloperMode",
                        "--uuid", UUID.randomUUID().toString(),
                        "--accessToken", "",
                        "--username", "Dev" + System.currentTimeMillis() % 1000L,
                        "--version", version,
                        "--versionType", "Tedge"
                };
            }
        }

        net.minecraft.SharedConstants.setVersion(new net.minecraft.WorldVersion.Simple(
                gson.get("id").getAsString(),
                gson.get("name").getAsString(),
                new net.minecraft.world.level.storage.DataVersion(
                        gson.get("world_version").getAsInt(),
                        gson.get("series_id").getAsString()
                ),
                gson.get("protocol_version").getAsInt(),
                new net.minecraft.server.packs.metadata.pack.PackFormat(
                        gson.getAsJsonObject("pack_version").get("resource_major").getAsInt(),
                        gson.getAsJsonObject("pack_version").get("resource_minor").getAsInt()
                ),
                new net.minecraft.server.packs.metadata.pack.PackFormat(
                        gson.getAsJsonObject("pack_version").get("data_major").getAsInt(),
                        gson.getAsJsonObject("pack_version").get("data_minor").getAsInt()
                ),
                Date.from(Instant.parse(gson.get("build_time").getAsString())),
                gson.get("stable").getAsBoolean()
        ));

        net.minecraft.client.main.Main.main(args);
    }

    @NonNull
    private static String getMCVersion(JsonObject gson) {
        String version;

        if (gson.has("name")) {
            version = gson.get("name").getAsString();
        } else if (gson.has("id")) {
            version = gson.get("id").getAsString();
        } else {
            IO.println("[Tedge] Failed to fetch Minecraft version; aborting");
            System.exit(1);

            return null;
        }

        return version;
    }
}
