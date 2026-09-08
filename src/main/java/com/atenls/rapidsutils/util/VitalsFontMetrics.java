package com.atenls.rapidsutils.util;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Immutable layout shared by all prefiltered atlas densities. */
public final class VitalsFontMetrics {
    public static final int CELL = 16;
    public static final int PADDING = 4;
    public static final int CAP_HEIGHT = 7;
    private static final float[] ADVANCES = loadAdvances();

    private VitalsFontMetrics() {}

    private static float[] loadAdvances() {
        try (var stream = Objects.requireNonNull(VitalsFontMetrics.class.getResourceAsStream(
                "/assets/rapidsutils/vitals_metrics.json"));
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            var root = JsonParser.parseReader(reader).getAsJsonObject();
            var array = root.getAsJsonArray("advances");
            if (array.size() != 95 || root.get("cell").getAsInt() != CELL
                    || root.get("padding").getAsInt() != PADDING
                    || root.get("capHeight").getAsInt() != CAP_HEIGHT) {
                throw new IllegalStateException("Invalid bundled vitals font metrics");
            }
            float[] result = new float[95];
            for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsFloat();
            return result;
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static int glyphIndex(char character) {
        return (character >= 32 && character <= 126 ? character : '?') - 32;
    }

    public static float advance(char character) {
        return ADVANCES[glyphIndex(character)];
    }

    public static float width(String text) {
        float width = 0;
        for (int i = 0; i < text.length(); i++) width += advance(text.charAt(i));
        return width;
    }

    public static int density(double guiScale) {
        int requested = (int) Math.ceil(guiScale);
        return requested > 8 ? 16 : Math.max(1, requested);
    }
}
