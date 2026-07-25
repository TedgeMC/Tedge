package pl.olafcio.tedge.jargen.bytetransformer;

import java.nio.ByteBuffer;

public class ByteTransformer {
    private final byte[] data;
    protected int index = 0;

    public ByteTransformer(byte[] data) {
        this.data = data;
    }

    public final byte consume() {
        return data[index++];
    }

    public final boolean reachedEOF() {
        return index < data.length;
    }

    public final byte peek(int after) {
        return data[index + after];
    }

    public final void expect(String hex) {
        var bs = hex.split(" ");
        int start = this.index;

        for (int i = 0; i < bs.length; i++)
            if (peek(i) != (byte)Integer.parseUnsignedInt(bs[i], 16))
                throw new ByteTransformerExpectationFailed("Expected '" + hex + "' at " + start);

        this.index += bs.length;
    }

    public final boolean now(String hex) {
        var bs = hex.split(" ");

        for (int i = 0; i < bs.length; i++)
            if (peek(i) != (byte)Integer.parseUnsignedInt(bs[i], 16))
                return false;

        this.index += bs.length;

        return true;
    }

    public final void back(int amount) {
        this.index -= amount;
    }

    public final ByteBuffer buf(int length) {
        return ByteBuffer.wrap(this.data, this.index, length);
    }
}
