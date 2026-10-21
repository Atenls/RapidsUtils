package com.atenls.rapidsutils.equipment;

import com.atenls.rapidsutils.text.MinecraftColorParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/** DPIM tags are the eligibility boundary; item names are only a color fallback. */
public final class SlotTintColors {
    private static final Resolution INELIGIBLE = new Resolution(false, OptionalInt.empty());
    private final Map<String, Integer> colors;

    private SlotTintColors(Map<String, Integer> colors) {
        this.colors = Map.copyOf(colors);
    }

    public static SlotTintColors empty() {
        return new SlotTintColors(Map.of());
    }

    /** Overlay #RRGGBB colors by quality name; omitted qualities retain bundled colors. */
    public SlotTintColors withOverrides(Reader reader) {
        Map<String, Integer> merged = new LinkedHashMap<>(colors);
        JsonParser.parseReader(reader).getAsJsonObject().entrySet().forEach(entry -> {
            String value = entry.getValue().getAsString();
            if (!value.matches("#[0-9a-fA-F]{6}")) {
                throw new IllegalArgumentException("Expected #RRGGBB for quality " + entry.getKey());
            }
            merged.put(normalize(entry.getKey()), Integer.parseInt(value.substring(1), 16));
        });
        return new SlotTintColors(merged);
    }

    public Resolution resolve(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank() || tagsJson.length() > 262_144) {
            return INELIGIBLE;
        }
        try {
            JsonObject tags = JsonParser.parseString(tagsJson).getAsJsonObject();
            for (String key : new String[]{"quality-name*", "quality-name"}) {
                var value = tags.get(key);
                if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    continue;
                }
                Integer color = colors.get(normalize(value.getAsString()));
                if (color != null) {
                    return new Resolution(true, OptionalInt.of(color));
                }
            }
            return new Resolution(true, OptionalInt.empty());
        } catch (RuntimeException exception) {
            return INELIGIBLE;
        }
    }

    private static String normalize(String name) {
        StringBuilder plain = new StringBuilder();
        MinecraftColorParser.parse(name, 0).forEach(segment -> plain.append(segment.text()));
        return plain.toString().strip().toLowerCase(Locale.ROOT);
    }

    public record Resolution(boolean eligible, OptionalInt qualityColor) { }
}
