package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.PlayerVitals;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerVitalsStateTest {
    @Test
    void becomesActiveOnUpdateAndClearsOnConnectionReset() {
        PlayerVitalsState state = new PlayerVitalsState();
        assertTrue(state.snapshot().isEmpty());

        PlayerVitals vitals = new PlayerVitals(
                BigDecimal.TEN, BigDecimal.valueOf(20), BigDecimal.ONE,
                BigDecimal.valueOf(30), BigDecimal.valueOf(40), BigDecimal.TWO
        );
        state.update(vitals);
        assertEquals(vitals, state.snapshot().orElseThrow());

        state.clear();
        assertTrue(state.snapshot().isEmpty());
    }
}
