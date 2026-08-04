package pl.olafcio.tedge.launcher.phases;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class DuplicateReducer {
    protected Path jarClient;
    protected Path jarServer;
    protected Path jarShared;

    protected final Path versionPath;

    public Path clientJAR() {
        return jarClient;
    }
    public Path serverJAR() {
        return jarServer;
    }
    public Path sharedJAR() {
        return jarShared;
    }

    public DuplicateReducer(Path jarClient, Path jarServer, Path versionPath) {
        this.jarClient = jarClient;
        this.jarServer = jarServer;
        this.versionPath = versionPath;
    }

    public void reduce()
           throws IOException
    {
        IO.println("[Launcher] Finding duplicate classes...");

        Path client;
        Path server;

        try (var zip1 = new ZipFile(jarClient.toFile())) {
            try (var zip2 = new ZipFile(jarServer.toFile())) {
                client = saveClientAndShared(zip2, zip1);
                server = saveServerOnly(zip2, zip1);
            }
        }

        Files.delete(jarServer);
        Files.delete(jarClient);

        jarServer = server;
        jarClient = client;
    }

    private Path saveServerOnly(ZipFile server, ZipFile client) throws IOException {
        var resultB = new ByteArrayOutputStream();
        var result = new ZipOutputStream(resultB);

        var entries2 = server.entries();
        while (entries2.hasMoreElements()) {
            var entry = entries2.nextElement();

            if (client.getEntry(entry.getName()) == null) {
                result.putNextEntry(entry);

                try (var stream = server.getInputStream(entry)) {
                    result.write(stream.readAllBytes());
                }
            }
        }

        result.close();

        Path serverPath = versionPath.resolve("server-only.jar");
        try (var stream = new FileOutputStream(serverPath.toFile())) {
            resultB.writeTo(stream);
            resultB.close();
        }

        IO.println("[Launcher] [Duplicate Reduction] Saved server-only.jar");

        return serverPath;
    }

    private Path saveClientAndShared(ZipFile server, ZipFile client)
            throws IOException
    {
        var clientEntries = client.entries();

        var resultB = new ByteArrayOutputStream();
        var result = new ZipOutputStream(resultB);

        var sharedB = new ByteArrayOutputStream();
        var shared = new ZipOutputStream(sharedB);

        while (clientEntries.hasMoreElements()) {
            var entry = clientEntries.nextElement();

            writeToClient:
            {
                if (!entry.getName().contains("META-INF")) {
                    if (
                            entry.getName().startsWith("assets") ||
                            server.getEntry(entry.getName()) != null
                    ) {
                        shared.putNextEntry(entry);

                        try (var stream = client.getInputStream(entry)) {
                            shared.write(stream.readAllBytes());
                        }

                        break writeToClient;
                    }
                }

                result.putNextEntry(entry);

                try (var stream = client.getInputStream(entry)) {
                    result.write(stream.readAllBytes());
                }
            }
        }

        result.close();
        shared.close();

        try (var stream = new FileOutputStream((jarShared = versionPath.resolve("shared.jar")).toFile())) {
            sharedB.writeTo(stream);
            sharedB.close();
        }

        IO.println("[Launcher] [Duplicate Reduction] Saved shared.jar");

        Path clientPath = versionPath.resolve("client-only.jar");
        try (var stream = new FileOutputStream(clientPath.toFile())) {
            resultB.writeTo(stream);
            resultB.close();
        }

        IO.println("[Launcher] [Duplicate Reduction] Saved client-only.jar");

        return clientPath;
    }
}
