package com.atenls.rapidsutils.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonLayoutTest {
    @Test
    void keepsVanillaAnchorAndUsesEitherSide() {
        var right = ComparisonLayout.place(1000, 600, 200, 100, 150, 200, 200, 250);
        assertEquals(200, right.x());
        assertEquals(0, right.primaryX());
        assertEquals(158, right.comparisonX());
        assertEquals(1F, right.scale());
        var left = ComparisonLayout.place(1000, 600, 800, 100, 150, 200, 200, 250);
        assertEquals(800, left.x() + left.primaryX());
        assertEquals(0, left.comparisonX());
    }

    @Test
    void fitsBothPanelsAtNarrowSizesAndBottomEdge() {
        for (int width : new int[]{240, 320, 640}) {
            var layout = ComparisonLayout.place(width, 240, width - 20, 220, 230, 350, 330, 400);
            assertTrue(layout.x() >= 12);
            assertTrue(layout.y() >= 12);
            assertTrue(layout.x() + (230 + ComparisonLayout.GAP + 330) * layout.scale() <= width - 11);
            assertTrue(layout.y() + 402 * layout.scale() <= 229);
            assertTrue(layout.comparisonX() >= layout.primaryX() + 230 + ComparisonLayout.GAP
                    || layout.primaryX() >= layout.comparisonX() + 330 + ComparisonLayout.GAP);
        }
    }
}
