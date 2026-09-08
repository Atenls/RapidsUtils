package com.atenls.rapidsutils.resources;

import com.atenls.rapidsutils.util.VitalsFontMetrics;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VitalsFontTest {
    @Test
    void layoutRemainsStableAcrossDisplayScales() {
        for (char c = '0'; c <= '9'; c++) assertEquals(6, VitalsFontMetrics.advance(c));
        assertTrue(VitalsFontMetrics.advance('.') < 3);
        assertEquals(VitalsFontMetrics.width("11.11K"), VitalsFontMetrics.width("88.88K"));
        assertEquals(VitalsFontMetrics.glyphIndex('?'), VitalsFontMetrics.glyphIndex('\u4e2d'));
        assertEquals(1, VitalsFontMetrics.density(1));
        assertEquals(3, VitalsFontMetrics.density(2.5));
        assertEquals(8, VitalsFontMetrics.density(8));
        assertEquals(16, VitalsFontMetrics.density(9));
        assertEquals(16, VitalsFontMetrics.density(32));
    }

    @Test
    void atlasDensitiesHaveSmoothEdgesPaddingAndCompleteAsciiCoverage() throws Exception {
        try (var license = getClass().getResourceAsStream("/assets/rapidsutils/font/ofl_roboto_mono.txt")) {
            assertNotNull(license);
            assertTrue(new String(license.readAllBytes(), StandardCharsets.UTF_8).contains("SIL OPEN FONT LICENSE"));
        }
        for (int density : new int[]{1, 2, 3, 4, 5, 6, 7, 8, 16}) {
            String path = "/assets/rapidsutils/textures/font/vitals_" + density + "x.png";
            try (var binary = getClass().getResourceAsStream(path);
                 var metadata = getClass().getResourceAsStream(path + ".mcmeta")) {
                assertNotNull(binary);
                assertNotNull(metadata);
                var texture = JsonParser.parseReader(new InputStreamReader(metadata, StandardCharsets.UTF_8))
                        .getAsJsonObject().getAsJsonObject("texture");
                assertTrue(texture.get("blur").getAsBoolean(), "Linear sampling required");
                assertTrue(texture.get("clamp").getAsBoolean());
                var atlas = ImageIO.read(binary);
                int cell = VitalsFontMetrics.CELL * density;
                assertEquals(16 * cell, atlas.getWidth());
                assertEquals(6 * cell, atlas.getHeight());
                for (char c = '!'; c <= '~'; c++) {
                    int slot = VitalsFontMetrics.glyphIndex(c);
                    boolean hasInk = false;
                    boolean hasCoverageEdge = false;
                    for (int y = 0; y < cell; y++) {
                        for (int x = 0; x < cell; x++) {
                            int pixel = atlas.getRGB(slot % 16 * cell + x, slot / 16 * cell + y);
                            int alpha = pixel >>> 24;
                            assertEquals(0xFFFFFF, pixel & 0xFFFFFF, "Transparent pixels must not add dark fringes");
                            if (x == 0 || y == 0 || x == cell - 1 || y == cell - 1) {
                                assertEquals(0, alpha, "Atlas glyph must have padding: " + c);
                            }
                            hasInk |= alpha > 0;
                            hasCoverageEdge |= alpha > 0 && alpha < 255;
                        }
                    }
                    assertTrue(hasInk, "Missing glyph: " + c);
                    if ("0235689".indexOf(c) >= 0) {
                        assertTrue(hasCoverageEdge, "Curved digit antialiasing missing: " + c);
                    }
                }
            }
        }
    }
}
