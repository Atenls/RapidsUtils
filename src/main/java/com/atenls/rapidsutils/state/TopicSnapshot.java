package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;

import java.math.BigDecimal;

public record TopicSnapshot(DataEnvelope envelope, long receivedAtTick) {
    private static final BigDecimal PERSISTENT_DURATION = BigDecimal.valueOf(-1L);

    public boolean isVisibleAt(long currentTick, BigDecimal fallbackDurationTicks) {
        BigDecimal duration = envelope.durationTicks().orElse(fallbackDurationTicks);
        if (duration.compareTo(PERSISTENT_DURATION) == 0) {
            return true;
        }
        return BigDecimal.valueOf(Math.max(0L, currentTick - receivedAtTick)).compareTo(duration) <= 0;
    }
}
