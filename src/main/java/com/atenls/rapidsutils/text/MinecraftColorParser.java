package com.atenls.rapidsutils.text;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftColorParser {
    private static final int[] LEGACY_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private MinecraftColorParser() {
    }

    public static List<Segment> parse(String input, int defaultColor, int maxVisibleCharacters) {
        if (input == null || input.isEmpty() || maxVisibleCharacters <= 0) {
            return List.of();
        }

        ArrayList<Segment> result = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        int currentColor = defaultColor & 0xFFFFFF;
        int visibleCharacters = 0;

        for (int index = 0; index < input.length();) {
            int ampersandHex = readAmpersandHexColor(input, index);
            if (ampersandHex >= 0) {
                flush(result, currentText, currentColor);
                currentColor = ampersandHex;
                index += 8;
                continue;
            }

            if (input.charAt(index) == '§' && index + 1 < input.length()) {
                int hexColor = readHexColor(input, index);
                if (hexColor >= 0) {
                    flush(result, currentText, currentColor);
                    currentColor = hexColor;
                    index += 14;
                    continue;
                }

                char code = Character.toLowerCase(input.charAt(index + 1));
                int legacyIndex = Character.digit(code, 16);
                if (legacyIndex >= 0) {
                    flush(result, currentText, currentColor);
                    currentColor = LEGACY_COLORS[legacyIndex];
                    index += 2;
                    continue;
                }
                if (code == 'r') {
                    flush(result, currentText, currentColor);
                    currentColor = defaultColor & 0xFFFFFF;
                    index += 2;
                    continue;
                }
                if (code >= 'k' && code <= 'o') {
                    index += 2;
                    continue;
                }
            }

            if (visibleCharacters >= maxVisibleCharacters) {
                currentText.append('…');
                break;
            }

            int codePoint = input.codePointAt(index);
            currentText.appendCodePoint(codePoint);
            visibleCharacters++;
            index += Character.charCount(codePoint);
        }

        flush(result, currentText, currentColor);
        return List.copyOf(result);
    }

    private static int readHexColor(String input, int start) {
        if (start + 14 > input.length() || Character.toLowerCase(input.charAt(start + 1)) != 'x') {
            return -1;
        }

        int color = 0;
        for (int pair = 0; pair < 6; pair++) {
            int sectionIndex = start + 2 + pair * 2;
            if (input.charAt(sectionIndex) != '§') {
                return -1;
            }
            int digit = Character.digit(input.charAt(sectionIndex + 1), 16);
            if (digit < 0) {
                return -1;
            }
            color = (color << 4) | digit;
        }
        return color;
    }

    private static int readAmpersandHexColor(String input, int start) {
        if (start + 8 > input.length() || input.charAt(start) != '&' || input.charAt(start + 1) != '#') {
            return -1;
        }

        int color = 0;
        for (int index = start + 2; index < start + 8; index++) {
            int digit = Character.digit(input.charAt(index), 16);
            if (digit < 0) {
                return -1;
            }
            color = (color << 4) | digit;
        }
        return color;
    }

    private static void flush(List<Segment> result, StringBuilder text, int color) {
        if (text.isEmpty()) {
            return;
        }

        String value = text.toString();
        text.setLength(0);
        if (!result.isEmpty() && result.getLast().color() == color) {
            Segment previous = result.removeLast();
            result.add(new Segment(previous.text() + value, color));
        } else {
            result.add(new Segment(value, color));
        }
    }

    public record Segment(String text, int color) {
    }
}
