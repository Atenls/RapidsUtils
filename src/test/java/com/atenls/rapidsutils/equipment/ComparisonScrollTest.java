package com.atenls.rapidsutils.equipment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ComparisonScrollTest {
    @Test
    void clampsBothEndsAndStopsEachPanelAtItsOwnBottom() {
        var scroll = new ComparisonScroll();
        scroll.configure(400, 250, 200);
        assertTrue(scroll.scroll(-4));
        assertEquals(72, scroll.offset(400, 200));
        assertEquals(50, scroll.offset(250, 200));
        assertEquals(0, scroll.offset(100, 200));
        assertTrue(scroll.scroll(-100));
        assertEquals(200, scroll.offset(400, 200));
        assertTrue(scroll.scroll(-1));
        assertEquals(200, scroll.offset(400, 200));
        assertTrue(scroll.scroll(100));
        assertEquals(0, scroll.offset(400, 200));
    }

    @Test
    void retainsFractionalWheelTravelAndClampsAfterResize() {
        var scroll = new ComparisonScroll();
        scroll.configure(400, 250, 200);
        for (int i = 0; i < 10; i++) {
            scroll.scroll(-0.1);
        }
        assertEquals(18, scroll.offset(400, 200));
        scroll.configure(400, 250, 390);
        assertEquals(10, scroll.offset(400, 390));
        scroll.configure(400, 250, 500);
        assertEquals(0, scroll.offset(400, 500));
        assertFalse(scroll.scroll(-1));
    }

    @Test
    void resetsBetweenItemsAndRejectsNonVerticalOrInvalidInput() {
        var scroll = new ComparisonScroll();
        scroll.configure(400, 250, 200);
        scroll.scroll(-2);
        assertFalse(scroll.scroll(0));
        assertFalse(scroll.scroll(Double.NaN));
        assertFalse(scroll.scroll(Double.POSITIVE_INFINITY));
        assertEquals(36, scroll.offset(400, 200));
        scroll.reset();
        assertFalse(scroll.scroll(-1));
        scroll.configure(400, 250, 200);
        assertEquals(0, scroll.offset(400, 200));
    }
}
