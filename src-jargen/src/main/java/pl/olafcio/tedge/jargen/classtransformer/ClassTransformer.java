package pl.olafcio.tedge.jargen.classtransformer;

import pl.olafcio.tedge.jargen.bytetransformer.ByteTransformer;

import java.nio.charset.StandardCharsets;

public class ClassTransformer extends ByteTransformer {
    public ClassTransformer(byte[] data) {
        super(data);
    }

    public String parseText() {
        var length = consume();
        var str = new byte[length];

        for (int i = 0; i < length; i++)
            str[i] = consume();

        return new String(str, StandardCharsets.UTF_8);
    }
}
