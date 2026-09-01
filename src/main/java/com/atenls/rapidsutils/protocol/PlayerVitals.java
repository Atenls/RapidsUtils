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

    private static float ratio(BigDecimal value, BigDecimal maximum) {
        if (maximum.signum() <= 0) {
            return 0.0F;
        }
        BigDecimal clamped = value.max(BigDecimal.ZERO).min(maximum);
        return clamped.divide(maximum, MathContext.DECIMAL64).floatValue();
    }
}
