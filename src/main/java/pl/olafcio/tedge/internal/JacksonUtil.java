package pl.olafcio.tedge.internal;

import com.google.gson.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.*;

public final class JacksonUtil {
    private JacksonUtil() {}

    public static JsonElement convert(JsonNode object) {
        return switch (object) {
            case null -> JsonNull.INSTANCE;

            case StringNode _ -> new JsonPrimitive(object.stringValue());
            case NumericNode _ -> new JsonPrimitive(object.numberValue());
            case BooleanNode _ -> new JsonPrimitive(object.booleanValue());

            case ArrayNode list -> {
                var array = new JsonArray();

                for (var el : list)
                    array.add(convert(el));

                yield array;
            }

            case ObjectNode map -> {
                var hashmap = new JsonObject();

                for (var entry : map.properties())
                    hashmap.add(entry.getKey(), convert(entry.getValue()));

                yield hashmap;
            }

            default -> throw new RuntimeException("Cannot convert '%s' to GSON".formatted(object));
        };
    }
}
