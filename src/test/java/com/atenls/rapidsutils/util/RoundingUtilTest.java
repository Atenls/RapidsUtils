package com.atenls.rapidsutils.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoundingUtilTest {
    @Test
    void roundsNumbersHalfUp() {
        assertEquals(12.35D, RoundingUtil.round(12.345D, 2));
        assertEquals(-12.35F, RoundingUtil.round(-12.345F, 2));
        assertEquals(13, RoundingUtil.roundToInt(12.5D));
        assertThrows(IllegalArgumentException.class, () -> RoundingUtil.round(1.0D, -1));
    }

    @Test
    void formatsSmallAndLargeValuesLikeTheReferenceUtility() {
        assertEquals("12.35", RoundingUtil.format(12.345D));
        assertEquals("121", RoundingUtil.format(120.9D));
        assertEquals("-2.5", RoundingUtil.format(-2.5F));
        assertEquals("123", RoundingUtil.format(new BigDecimal("123.456")));
    }

    @Test
    void compactsThousandsMillionsAndBillions() {
        assertEquals("999.5", RoundingUtil.longFormat(new BigDecimal("999.5")));
        assertEquals("1.25K", RoundingUtil.longFormat(new BigDecimal("1250")));
        assertEquals("-2.5M", RoundingUtil.longFormat(new BigDecimal("-2500000")));
        assertEquals("3B", RoundingUtil.longFormat(new BigDecimal("3000000000")));
    }
}
