package com.atenls.rapidsutils.equipment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class EquipmentComparisonTest {
    @Test
    void aggregatesBaseAffixesAndRunesAndIgnoresSetAttributes() {
        var item = EquipmentSnapshot.parse("""
                [{"key":"HEALTH","min":10,"max":10},
                 {"key":"HEALTH","min":50,"max":50,"source":"SET"}]
                """, """
                [{"group":"normal","attributes":[{"key":"health","min":2.1,"max":2.1}]},
                 {"group":"rune","attributes":[{"key":"HEALTH","min":3.2,"max":3.2}]}]
                """);
        assertEquals(new BigDecimal("15.3"), item.attributes().get(new EquipmentSnapshot.Key("HEALTH", false)).min());
    }

    @Test
    void keepsRawAndPercentageChannelsSeparateAndUsesPercentageMin() {
        var item = EquipmentSnapshot.parse("""
                [{"key":"HEALTH","min":10,"max":20},
                 {"key":"HEALTH","min":5,"max":99,"percent":true}]
                """, "[]");
        assertEquals(2, item.attributes().size());
        assertEquals(new BigDecimal("5"), item.attributes().get(new EquipmentSnapshot.Key("HEALTH", true)).max());
    }

    @Test
    void reportsRemovedAttributesAndMixedRanges() {
        var old = EquipmentSnapshot.parse("""
                [{"key":"ATTACK","min":10,"max":20},{"key":"HEALTH","min":5,"max":5}]
                """, "[]");
        var next = EquipmentSnapshot.parse("[{\"key\":\"ATTACK\",\"min\":12,\"max\":18}]", "[]");
        var rows = EquipmentComparison.compare(next, old);
        assertEquals(0, rows.getFirst().direction(false));
        assertEquals(new BigDecimal("-5"), rows.get(1).minDelta());
        assertEquals(-1, rows.get(1).direction(false));
        assertEquals(1, rows.get(1).direction(true));
    }

    @Test
    void rejectsMalformedAndExtremeNumbersInsteadOfPartialTotals() {
        for (String json : new String[]{"{}", "[null]", "[{\"key\":\"X\",\"min\":1e99999,\"max\":1}]",
                "[{\"key\":\"X\",\"min\":\"NaN\",\"max\":1}]"}) {
            assertThrows(RuntimeException.class, () -> EquipmentSnapshot.parse(json, "[]"));
        }
        assertThrows(RuntimeException.class, () -> EquipmentSnapshot.parse("[]", "[{\"attributes\":42}]"));
    }

    @Test
    void retainsSmallDifferences() {
        assertEquals("<0.01", EquipmentComparison.format(new BigDecimal("0.0001")));
        assertEquals(">-0.01", EquipmentComparison.format(new BigDecimal("-0.0001")));
        assertEquals("12.35", EquipmentComparison.format(new BigDecimal("12.345")));
    }
}
