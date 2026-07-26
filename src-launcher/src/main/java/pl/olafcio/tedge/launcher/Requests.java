package pl.olafcio.tedge.launcher;

import java.io.IOException;
import java.net.URI;

public class Requests {
    private Requests() {}

    public static byte[] get(String url) throws IOException {
        var conn = URI.create(url).toURL().openConnection();
        conn.setDoInput(true);
        conn.connect();

        return conn.getInputStream().readAllBytes();
    }
}
