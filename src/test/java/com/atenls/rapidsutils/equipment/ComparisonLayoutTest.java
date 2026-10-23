package com.atenls.rapidsutils.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonLayoutTest {
    @Test
    void keepsVanillaAnchorAndUsesEitherSide() {
        var right = ComparisonLayout.place(1000, 600, 200, 100, 150, 200, 200, 250);
        assertEquals(200, right.x());
        assertEquals(0, right.primaryX());
        assertEquals(162, right.comparisonX());
        assertEquals(1F, right.scale());
        var left = ComparisonLayout.place(1000, 600, 800, 100, 150, 200, 200, 250);
        assertEquals(800, left.x() + left.primaryX());
        assertEquals(212, left.primaryX());
        assertEquals(0, left.comparisonX());
    }

    @Test
    void clipsHeightWithoutShrinkingAndKeepsPanelsSeparate() {
        for (int width : new int[]{240, 320, 640}) {
            var layout = ComparisonLayout.place(width, 240, width - 20, 220, 230, 350, 330, 400);
            assertTrue(layout.x() >= 12);
            assertTrue(layout.y() >= 12);
            assertEquals(1F, layout.scale());
            assertEquals(216, layout.viewportHeight());
            assertTrue(layout.y() + layout.viewportHeight() <= 228);
            assertTrue(layout.comparisonX() >= layout.primaryX() + 230 + ComparisonLayout.GAP
                    || layout.primaryX() >= layout.comparisonX() + 330 + ComparisonLayout.GAP);
        }
    }

    @Test
    void keepsShortPanelsAtNativeHeightAndBoundsVerySmallWindows() {
        var shortPanels = ComparisonLayout.place(1000, 600, 200, 550, 150, 100, 200, 150);
        assertEquals(150, shortPanels.viewportHeight());
        assertEquals(438, shortPanels.y());
        var tiny = ComparisonLayout.place(100, 20, 5, 5, 150, 200, 200, 250);
        assertEquals(1F, tiny.scale());
        assertEquals(1, tiny.viewportHeight());
    }
}
