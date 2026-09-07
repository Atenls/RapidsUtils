package com.atenls.rapidsutils.resources;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VitalsFontTest {
    @Test
    void bundledFontHasCurvesAsciiCoverageAndStableNumericMetrics() throws Exception {
        try (var definition = getClass().getResourceAsStream("/assets/rapidsutils/font/vitals.json");
             var binary = getClass().getResourceAsStream("/assets/rapidsutils/font/vitals.ttf");
             var license = getClass().getResourceAsStream("/assets/rapidsutils/font/ofl_nunito.txt")) {
            assertNotNull(definition);
            assertNotNull(binary);
            assertNotNull(license);
            assertTrue(new String(license.readAllBytes(), StandardCharsets.UTF_8).contains("SIL OPEN FONT LICENSE"));
            var providers = JsonParser.parseReader(new InputStreamReader(definition, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("providers");
            assertEquals(1, providers.size());
            var provider = providers.get(0).getAsJsonObject();
            assertEquals("ttf", provider.get("type").getAsString());
            assertEquals("rapidsutils:vitals.ttf", provider.get("file").getAsString());
            assertFalse(provider.has("filter"), "Unicode option must not disable this font");
            assertEquals(8, provider.get("oversample").getAsInt());
            Font font = Font.createFont(Font.TRUETYPE_FONT, binary)
                    .deriveFont(provider.get("size").getAsFloat());
            var context = new FontRenderContext(new AffineTransform(), true, true);
            for (char c = 32; c <= 126; c++) assertTrue(font.canDisplay(c), "Missing ASCII glyph: " + c);
            for (char c = '0'; c <= '9'; c++) {
                var glyph = font.createGlyphVector(context, new char[]{c});
                assertEquals(6.0, glyph.getGlyphMetrics(0).getAdvance(), 0.02, "Tabular digit: " + c);
                double height = glyph.getVisualBounds().getHeight();
                assertTrue(height >= 6.8 && height <= 7.5, "Keep seven-pixel visible digit height: " + c);
            }
            double dotAdvance = font.createGlyphVector(context, ".").getGlyphMetrics(0).getAdvance();
            assertTrue(dotAdvance < 3, "Compact decimal point");
            var outline = font.createGlyphVector(context, "0").getOutline().getPathIterator(null);
            boolean hasCurve = false;
            for (double[] point = new double[6]; !outline.isDone(); outline.next()) {
                int segment = outline.currentSegment(point);
                hasCurve |= segment == PathIterator.SEG_QUADTO || segment == PathIterator.SEG_CUBICTO;
            }
            assertTrue(hasCurve, "Zero must have curved outlines, not pixel segments");
        }
    }
}
