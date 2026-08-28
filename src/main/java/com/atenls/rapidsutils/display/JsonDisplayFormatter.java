package com.atenls.rapidsutils.display;

import com.atenls.rapidsutils.protocol.PayloadData;
import com.atenls.rapidsutils.text.MinecraftColorParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JsonDisplayFormatter {
    public static final int PRIMARY_TEXT = 0xE4E7EB;
    public static final int KEY_TEXT = 0xAEB7C2;
    public static final int MUTED_TEXT = 0x7F8996;
    public static final int NUMBER_TEXT = 0x9CCFD8;
    public static final int BOOLEAN_TEXT = 0xC4A7E7;

    private static final int MAX_DEPTH = 4;
    private static final int MAX_COLLECTION_ENTRIES = 16;
    private static final int MAX_STRING_CHARACTERS = 240;

    public List<HudLine> format(PayloadData data) {
        ArrayList<HudLine> lines = new ArrayList<>();
        if (data instanceof PayloadData.ObjectValue object) {
            appendObject(lines, object, 0);
        } else if (data instanceof PayloadData.ArrayValue array) {
            appendArray(lines, array, 0);
        } else {
            appendScalar(lines, null, (PayloadData.ScalarValue) data, 0);
        }
        if (lines.isEmpty()) {
            lines.add(line(0, "No data", MUTED_TEXT));
        }
        return List.copyOf(lines);
    }

    private void appendObject(List<HudLine> lines, PayloadData.ObjectValue object, int depth) {
        if (object.values().isEmpty()) {
            lines.add(line(depth, "Empty object", MUTED_TEXT));
            return;
        }

        int shown = 0;
        for (Map.Entry<String, PayloadData> entry : object.values().entrySet()) {
            if (shown++ >= MAX_COLLECTION_ENTRIES) {
                lines.add(line(depth, "+ " + (object.values().size() - MAX_COLLECTION_ENTRIES) + " more fields", MUTED_TEXT));
                break;
            }
            appendNamed(lines, entry.getKey(), entry.getValue(), depth);
        }
    }

    private void appendArray(List<HudLine> lines, PayloadData.ArrayValue array, int depth) {
        if (array.values().isEmpty()) {
            lines.add(line(depth, "Empty list", MUTED_TEXT));
            return;
        }

        int shown = Math.min(MAX_COLLECTION_ENTRIES, array.values().size());
        for (int index = 0; index < shown; index++) {
            appendNamed(lines, Integer.toString(index + 1), array.values().get(index), depth);
        }
        if (array.values().size() > shown) {
            lines.add(line(depth, "+ " + (array.values().size() - shown) + " more items", MUTED_TEXT));
        }
    }

    private void appendNamed(List<HudLine> lines, String name, PayloadData value, int depth) {
        if (value instanceof PayloadData.ScalarValue scalar) {
            appendScalar(lines, name, scalar, depth);
            return;
        }

        int size = value instanceof PayloadData.ObjectValue object
                ? object.values().size()
                : ((PayloadData.ArrayValue) value).values().size();
        String type = value instanceof PayloadData.ObjectValue ? "fields" : "items";
        lines.add(new HudLine(depth, List.of(
                new MinecraftColorParser.Segment(name, KEY_TEXT),
                new MinecraftColorParser.Segment("  ·  " + size + " " + type, MUTED_TEXT)
        )));

        if (depth >= MAX_DEPTH) {
            lines.add(line(depth + 1, "Nested content hidden", MUTED_TEXT));
        } else if (value instanceof PayloadData.ObjectValue object) {
            appendObject(lines, object, depth + 1);
        } else {
            appendArray(lines, (PayloadData.ArrayValue) value, depth + 1);
        }
    }

    private void appendScalar(List<HudLine> lines, String name, PayloadData.ScalarValue scalar, int depth) {
        ArrayList<MinecraftColorParser.Segment> spans = new ArrayList<>();
        if (name != null) {
            spans.add(new MinecraftColorParser.Segment(name + "  ", KEY_TEXT));
        }

        int valueColor = switch (scalar.kind()) {
            case STRING -> PRIMARY_TEXT;
            case NUMBER -> NUMBER_TEXT;
            case BOOLEAN -> BOOLEAN_TEXT;
            case NULL -> MUTED_TEXT;
        };
        spans.addAll(MinecraftColorParser.parse(scalar.value(), valueColor, MAX_STRING_CHARACTERS));
        lines.add(new HudLine(depth, spans));
    }

    private static HudLine line(int depth, String text, int color) {
        return new HudLine(depth, List.of(new MinecraftColorParser.Segment(text, color)));
    }
}
