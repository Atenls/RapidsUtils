package com.atenls.rapidsutils.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DataEnvelopeParser {
    private static final int MAX_TOPIC_LENGTH = 64;
    private static final int MAX_DATA_DEPTH = 64;

    private DataEnvelopeParser() {
    }

    public static Optional<DataEnvelope> parse(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (!parsed.isJsonObject()) {
                return Optional.empty();
            }

            JsonObject root = parsed.getAsJsonObject();
            Integer version = exactInt(root.get("version"));
            Long sequence = exactLong(root.get("sequence"));
            String topic = string(root.get("topic"));
            Boolean full = bool(root.get("full"));

            if (version == null || version != DataEnvelope.CURRENT_VERSION
                    || sequence == null || sequence < 0
                    || topic == null || topic.isBlank() || topic.length() > MAX_TOPIC_LENGTH
                    || !Boolean.TRUE.equals(full)
                    || !root.has("duration") || !root.has("index") || !root.has("data")) {
                return Optional.empty();
            }

            PayloadData duration = convert(root.get("duration"), 0);
            PayloadData index = convert(root.get("index"), 0);
            PayloadData x = convert(root.get("x"), 0);
            PayloadData y = convert(root.get("y"), 0);
            PayloadData opacity = convert(root.get("opacity"), 0);
            PayloadData fadeIn = convert(root.get("fadeIn"), 0);
            PayloadData fadeOut = convert(root.get("fadeOut"), 0);
            PayloadData data = convert(root.get("data"), 0);
            return Optional.of(new DataEnvelope(
                    version, topic, sequence, true, duration, index, x, y, opacity, fadeIn, fadeOut, data
            ));
        } catch (JsonParseException | ArithmeticException | IllegalStateException | ClassCastException e) {
            return Optional.empty();
        }
    }

    private static PayloadData convert(JsonElement element, int depth) {
        if (depth > MAX_DATA_DEPTH) {
            throw new JsonParseException("Payload data is nested too deeply");
        }
        if (element == null || element.isJsonNull()) {
            return new PayloadData.ScalarValue(PayloadData.ScalarKind.NULL, "null");
        }
        if (element.isJsonObject()) {
            Map<String, PayloadData> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                values.put(entry.getKey(), convert(entry.getValue(), depth + 1));
            }
            return new PayloadData.ObjectValue(values);
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            ArrayList<PayloadData> values = new ArrayList<>(array.size());
            for (JsonElement item : array) {
                values.add(convert(item, depth + 1));
            }
            return new PayloadData.ArrayValue(values);
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            return new PayloadData.ScalarValue(PayloadData.ScalarKind.BOOLEAN, primitive.getAsString());
        }
        if (primitive.isNumber()) {
            return new PayloadData.ScalarValue(PayloadData.ScalarKind.NUMBER, primitive.getAsString());
        }
        return new PayloadData.ScalarValue(PayloadData.ScalarKind.STRING, primitive.getAsString());
    }

    private static Integer exactInt(JsonElement element) {
        Long value = exactLong(element);
        if (value == null || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            return null;
        }
        return value.intValue();
    }

    private static Long exactLong(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        return new BigDecimal(element.getAsString()).longValueExact();
    }

    private static String string(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
                ? element.getAsString()
                : null;
    }

    private static Boolean bool(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean()
                : null;
    }
}
