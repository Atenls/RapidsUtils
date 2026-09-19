package com.atenls.rapidsutils.protocol;

import com.atenls.rapidsutils.state.PlayerVitalsState;
import com.atenls.rapidsutils.util.RoundingUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerVitalsParserTest {
    @Test
    void rejectsExtremeNumbersInAllVitalsFieldsAndRetainsLastValidState() {
        String valid = """
                {"health":1,"health_max":1,"health_regen":1,"mana":1,"mana_max":1,"mana_regen":1}
                """;
        PlayerVitalsState state = new PlayerVitalsState();
        PlayerVitalsParser.parse(valid).ifPresent(state::update);
        PlayerVitals previous = state.snapshot().orElseThrow();
        for (String field : java.util.List.of("health", "health_max", "health_regen", "mana", "mana_max", "mana_regen")) {
            for (String raw : java.util.List.of("1.1e-2147483647", "1e1000000", "1e-1000000",
                    "9".repeat(129), "1e+" + "0".repeat(254) + "1")) {
                var parsed = PlayerVitalsParser.parse(valid.replace("\"" + field + "\":1", "\"" + field + "\":" + raw));
                assertTrue(parsed.isEmpty(), field + "=" + raw);
                parsed.ifPresent(state::update);
                assertSame(previous, state.snapshot().orElseThrow());
            }
        }
    }

    @Test
    void boundedScientificVitalsCanBeFormattedAndDivided() {
        PlayerVitals vitals = PlayerVitalsParser.parse("""
                {"health":1e128,"health_max":2e128,"health_regen":-1e-128,
                 "mana":1e-128,"mana_max":2e-128,"mana_regen":0}
                """).orElseThrow();
        assertEquals(0.5F, vitals.healthRatio());
        assertEquals(0.5F, vitals.manaRatio());
        assertDoesNotThrow(() -> RoundingUtil.longFormat(vitals.health()));
        assertEquals("0", RoundingUtil.longFormat(vitals.mana()));
    }

    @Test
    void parsesCompletePlayerVitalsPayload() {
        PlayerVitals vitals = PlayerVitalsParser.parse("""
                {"health":72.5,"health_max":100,"health_regen":2.25,
                 "mana":48,"mana_max":80,"mana_regen":-1}
                """).orElseThrow();

        assertEquals("72.5", vitals.health().toPlainString());
        assertEquals("100", vitals.healthMax().toPlainString());
        assertEquals("2.25", vitals.healthRegen().toPlainString());
        assertEquals("48", vitals.mana().toPlainString());
        assertEquals("80", vitals.manaMax().toPlainString());
        assertEquals("-1", vitals.manaRegen().toPlainString());
        assertEquals(0.725F, vitals.healthRatio(), 0.0001F);
        assertEquals(0.6F, vitals.manaRatio(), 0.0001F);
    }

    @Test
    void clampsRatiosWithoutChangingDisplayedValues() {
        PlayerVitals vitals = PlayerVitalsParser.parse("""
                {"health":125,"health_max":100,"health_regen":0,
                 "mana":-4,"mana_max":0,"mana_regen":0}
                """).orElseThrow();

        assertEquals(1.0F, vitals.healthRatio());
        assertEquals(0.0F, vitals.manaRatio());
        assertEquals("125", vitals.health().toPlainString());
        assertEquals("-4", vitals.mana().toPlainString());
    }

    @Test
    void rejectsMalformedMissingAndNonNumericFields() {
        assertTrue(PlayerVitalsParser.parse("not json").isEmpty());
        assertTrue(PlayerVitalsParser.parse("[]").isEmpty());
        assertTrue(PlayerVitalsParser.parse("""
                {"health":10,"health_max":20,"health_regen":1,
                 "mana":5,"mana_max":10}
                """).isEmpty());
        assertTrue(PlayerVitalsParser.parse("""
                {"health":"10","health_max":20,"health_regen":1,
                 "mana":5,"mana_max":10,"mana_regen":1}
                """).isEmpty());
    }
}
