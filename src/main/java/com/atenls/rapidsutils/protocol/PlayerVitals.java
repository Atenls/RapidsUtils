package com.atenls.rapidsutils.protocol;

import java.math.BigDecimal;
import java.math.MathContext;

public record PlayerVitals(
        BigDecimal health,
        BigDecimal healthMax,
        BigDecimal healthRegen,
        BigDecimal mana,
        BigDecimal manaMax,
        BigDecimal manaRegen
) {
    public float healthRatio() {
        return ratio(health, healthMax);
    }

    public float manaRatio() {
        return ratio(mana, manaMax);
    }

    public HealthBand healthBand() {
        if (healthMax.signum() > 0 && health.multiply(BigDecimal.valueOf(100L))
                .compareTo(healthMax.multiply(BigDecimal.valueOf(65L))) > 0) {
            return HealthBand.GREEN;
        }
        if (healthMax.signum() > 0 && health.multiply(BigDecimal.valueOf(100L))
                .compareTo(healthMax.multiply(BigDecimal.valueOf(35L))) > 0) {
            return HealthBand.BLUE;
        }
        return HealthBand.RED;
    }

    private static float ratio(BigDecimal value, BigDecimal maximum) {
        if (maximum.signum() <= 0) {
            return 0.0F;
        }
        BigDecimal clamped = value.max(BigDecimal.ZERO).min(maximum);
        return clamped.divide(maximum, MathContext.DECIMAL64).floatValue();
    }

    public enum HealthBand {
        GREEN,
        BLUE,
        RED
    }
}
