package pl.olafcio.tedge.launcher.util;

import java.io.IOException;
import java.net.URI;

public enum Requests {
    ;

    public static byte[] get(String url) throws IOException {
        var conn = URI.create(url).toURL().openConnection();
        conn.setDoInput(true);
        conn.connect();

        return conn.getInputStream().readAllBytes();
    }
}
