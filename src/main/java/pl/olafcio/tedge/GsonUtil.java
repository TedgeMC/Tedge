package pl.olafcio.tedge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.function.Function;

final class GsonUtil {
    private GsonUtil() {}

    public static <T> ArrayList<T> map(JsonArray array, Function<JsonElement, T> mapper) {
        var out = new ArrayList<T>(array.size());

        for (JsonElement element : array) {
            out.add(mapper.apply(element));
        }

        return out;
    }

    public static <T> T[] mapToArray(JsonArray array, Function<JsonElement, T> mapper, Function<Integer, T[]> arraysupplier) {
        var out = arraysupplier.apply(array.size());

        int i = 0;
        for (JsonElement element : array) {
            out[i++] = mapper.apply(element);
        }

        return out;
    }
}
