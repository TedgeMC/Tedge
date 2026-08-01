package pl.olafcio.tedge.launcher;

import java.io.IOException;

/**
 * @deprecated Use {@linkplain pl.olafcio.tedge.launcher.util.Requests util.Requests} instead.
 */
@Deprecated(forRemoval = true)
public class Requests {
    private Requests() {}

    public static byte[] get(String url) throws IOException {
        return pl.olafcio.tedge.launcher.util.Requests.get(url);
    }
}
