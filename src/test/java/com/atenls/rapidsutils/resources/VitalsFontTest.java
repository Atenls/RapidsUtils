package com.atenls.rapidsutils.resources;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VitalsFontTest {
    @Test
    void bundledFontHasCompleteAsciiCoverageAndStableNumericMetrics() throws Exception {
        try (var definition = getClass().getResourceAsStream("/assets/rapidsutils/font/vitals.json");
             var texture = getClass().getResourceAsStream("/assets/rapidsutils/textures/font/vitals.png")) {
            assertNotNull(definition);
            assertNotNull(texture);
            var providers = JsonParser.parseReader(new InputStreamReader(definition, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("providers");
            var bitmap = providers.get(1).getAsJsonObject();
            // No default-font reference or Unicode-option filter: this font is self-contained.
            assertEquals(2, providers.size());
            assertEquals("space", providers.get(0).getAsJsonObject().get("type").getAsString());
            assertEquals("bitmap", bitmap.get("type").getAsString());
            assertFalse(bitmap.has("filter"));
            assertEquals(8, bitmap.get("height").getAsInt());
            assertEquals(7, bitmap.get("ascent").getAsInt());
            var atlas = ImageIO.read(texture);
            var rows = bitmap.getAsJsonArray("chars");
            assertEquals(48, atlas.getHeight());
            assertEquals(128, atlas.getWidth());
            boolean[] seen = new boolean[127];
            for (int row = 0; row < rows.size(); row++) {
                String chars = rows.get(row).getAsString();
                assertEquals(16, chars.length());
                for (int col = 0; col < chars.length(); col++) {
                    char c = chars.charAt(col);
                    if (c == 0) continue;
                    assertFalse(seen[c], "Duplicate glyph: " + c);
                    seen[c] = true;
                    int right = -1;
                    int top = 8;
                    int bottom = -1;
                    for (int y = 0; y < 8; y++) {
                        for (int x = 0; x < 8; x++) {
                            int alpha = atlas.getRGB(col * 8 + x, row * 8 + y) >>> 24;
                            assertTrue(alpha == 0 || alpha == 255, "Pixels must remain crisp");
                            if (alpha != 0) {
                                right = Math.max(right, x);
                                top = Math.min(top, y);
                                bottom = Math.max(bottom, y);
                            }
                        }
                    }
                    assertTrue(right >= 0, "Empty glyph: " + c);
                    assertTrue(bottom <= 6, "Glyph exceeds seven visible rows: " + c);
                    if (Character.isDigit(c) || "KMB".indexOf(c) >= 0) {
                        assertEquals(4, right, "Numeric glyph width: " + c);
                        assertEquals(0, top, "Numeric glyph top: " + c);
                        assertEquals(6, bottom, "Numeric glyph bottom: " + c);
                    }
                    if (c == '.') assertEquals(0, right, "Compact decimal point");
                }
            }
            for (int c = 33; c <= 126; c++) assertTrue(seen[c], "Missing ASCII glyph: " + c);
        }
    }
}
