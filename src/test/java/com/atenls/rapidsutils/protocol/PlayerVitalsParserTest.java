package com.atenls.rapidsutils.protocol;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerVitalsParserTest {
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
        assertEquals(PlayerVitals.HealthBand.GREEN, vitals.healthBand());
    }

    @Test
    void usesExactHealthColorThresholds() {
        assertEquals(PlayerVitals.HealthBand.GREEN, vitalsAt(65.1).healthBand());
        assertEquals(PlayerVitals.HealthBand.BLUE, vitalsAt(65).healthBand());
        assertEquals(PlayerVitals.HealthBand.BLUE, vitalsAt(35.1).healthBand());
        assertEquals(PlayerVitals.HealthBand.RED, vitalsAt(35).healthBand());
        assertEquals(PlayerVitals.HealthBand.RED, vitalsAt(0).healthBand());
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

    private static PlayerVitals vitalsAt(double health) {
        return new PlayerVitals(
                BigDecimal.valueOf(health), BigDecimal.valueOf(100), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.valueOf(100), BigDecimal.ZERO
        );
    }
}
