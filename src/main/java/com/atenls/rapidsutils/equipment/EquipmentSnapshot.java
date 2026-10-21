package com.atenls.rapidsutils.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** The client-visible DPItemManager attrs/affixes payload, without set bonuses. */
public record EquipmentSnapshot(Map<Key, Value> attributes) {
    private static final int MAX_JSON_LENGTH = 262_144;
    private static final int MAX_ATTRIBUTES = 512;

    public EquipmentSnapshot {
        attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public static EquipmentSnapshot empty() {
        return new EquipmentSnapshot(Map.of());
    }

    /** Reject a malformed whole item instead of displaying a misleading partial comparison. */
    public static EquipmentSnapshot parse(String attrsJson, String affixesJson) {
        Map<Key, Value> attributes = new LinkedHashMap<>();
        readAttributes(array(attrsJson), attributes);
        JsonArray affixes = array(affixesJson);
        if (affixes.size() > MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("Too many affixes");
        }
        for (JsonElement element : affixes) {
            JsonObject affix = element.getAsJsonObject();
            if (affix.has("attributes")) {
                readAttributes(affix.getAsJsonArray("attributes"), attributes);
            }
        }
        return new EquipmentSnapshot(attributes);
    }

    private static JsonArray array(String json) {
        if (json == null || json.isBlank()) {
            return new JsonArray();
        }
        if (json.length() > MAX_JSON_LENGTH) {
            throw new IllegalArgumentException("Equipment JSON too long");
        }
        return JsonParser.parseString(json).getAsJsonArray();
    }

    private static void readAttributes(JsonArray array, Map<Key, Value> target) {
        if (array.size() > MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("Too many attributes");
        }
        for (JsonElement element : array) {
            JsonObject attribute = element.getAsJsonObject();
            if (text(attribute, "source").equalsIgnoreCase("SET")) {
                continue;
            }
            String name = text(attribute, "key").trim().toUpperCase(java.util.Locale.ROOT);
            if (name.isEmpty() || name.length() > 128) {
                throw new IllegalArgumentException("Invalid attribute key");
            }
            boolean percent = attribute.has("percent") && attribute.get("percent").getAsBoolean();
            BigDecimal min = number(attribute.get("min"));
            // Match DPIM AttributeValue: percentage attributes accumulate min only.
            BigDecimal max = percent ? min : number(attribute.get("max"));
            Key key = new Key(name, percent);
            Value value = new Value(text(attribute, "displayName"), min, max);
            target.merge(key, value, Value::add);
            if (target.size() > MAX_ATTRIBUTES) {
                throw new IllegalArgumentException("Too many attribute keys");
            }
        }
    }

    private static BigDecimal number(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid attribute value");
        }
        String raw = value.getAsString();
        if (raw.length() > 128) {
            throw new IllegalArgumentException("Attribute number too long");
        }
        BigDecimal number = new BigDecimal(raw);
        if (number.precision() > 64 || Math.abs((long) number.scale()) > 64) {
            throw new IllegalArgumentException("Attribute number out of range");
        }
        return number;
    }

    private static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    public record Key(String name, boolean percent) { }

    public record Value(String displayName, BigDecimal min, BigDecimal max) {
        public static final Value ZERO = new Value("", BigDecimal.ZERO, BigDecimal.ZERO);

        Value add(Value other) {
            return new Value(displayName.isBlank() ? other.displayName : displayName,
                    min.add(other.min), max.add(other.max));
        }
    }
}
