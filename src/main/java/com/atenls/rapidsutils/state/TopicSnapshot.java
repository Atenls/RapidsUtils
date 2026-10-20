package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;

import java.math.BigDecimal;

public record TopicSnapshot(
        DataEnvelope envelope,
        long receivedAtTick,
        long firstReceivedAtTick,
        BigDecimal fadeInTicks
) {
    private static final BigDecimal PERSISTENT_DURATION = BigDecimal.valueOf(-1L);
    private static final BigDecimal DEFAULT_FADE_IN_TICKS = BigDecimal.valueOf(5L);
    private static final BigDecimal DEFAULT_FADE_OUT_TICKS = BigDecimal.valueOf(15L);

    public TopicSnapshot(DataEnvelope envelope, long receivedAtTick) {
        this(
                envelope,
                receivedAtTick,
                receivedAtTick,
                envelope.fadeInTicks().orElse(DEFAULT_FADE_IN_TICKS)
        );
    }

    public boolean isVisibleAt(long currentTick, BigDecimal fallbackDurationTicks) {
        BigDecimal duration = envelope.durationTicks().orElse(fallbackDurationTicks);
        if (duration.compareTo(PERSISTENT_DURATION) == 0) {
            return true;
        }
        return BigDecimal.valueOf(Math.max(0L, currentTick - receivedAtTick)).compareTo(duration) <= 0;
    }

    public float fadeFactorAt(double currentTick, BigDecimal fallbackDurationTicks) {
        double elapsedSinceFirst = Math.max(0.0D, currentTick - firstReceivedAtTick);
        double fadeInDuration = fadeInTicks.doubleValue();
        float fadeInFactor = fadeInDuration == 0.0D
                ? 1.0F
                : clamp(elapsedSinceFirst / fadeInDuration);

        BigDecimal durationValue = envelope.durationTicks().orElse(fallbackDurationTicks);
        if (durationValue.compareTo(PERSISTENT_DURATION) == 0) {
            return fadeInFactor;
        }

        double duration = Math.max(0.0D, durationValue.doubleValue());
        double elapsedSinceLatest = Math.max(0.0D, currentTick - receivedAtTick);
        if (elapsedSinceLatest <= duration) {
            return fadeInFactor;
        }

        double fadeOutDuration = envelope.fadeOutTicks().orElse(DEFAULT_FADE_OUT_TICKS).doubleValue();
        if (fadeOutDuration == 0.0D) {
            return 0.0F;
        }
        float fadeInAtExpiration = fadeInDuration == 0.0D
                ? 1.0F
                : clamp((receivedAtTick - firstReceivedAtTick + duration) / fadeInDuration);
        return fadeInAtExpiration * clamp(1.0D - (elapsedSinceLatest - duration) / fadeOutDuration);
    }

    public boolean isExpiredAfterFadeAt(double currentTick, BigDecimal fallbackDurationTicks) {
        BigDecimal duration = envelope.durationTicks().orElse(fallbackDurationTicks);
        if (duration.compareTo(PERSISTENT_DURATION) == 0) {
            return false;
        }
        double elapsedSinceLatest = Math.max(0.0D, currentTick - receivedAtTick);
        double activeDuration = Math.max(0.0D, duration.doubleValue());
        double fadeOutDuration = envelope.fadeOutTicks().orElse(DEFAULT_FADE_OUT_TICKS).doubleValue();
        return fadeOutDuration == 0.0D
                ? elapsedSinceLatest > activeDuration
                : elapsedSinceLatest >= activeDuration + fadeOutDuration;
    }

    private static float clamp(double value) {
        return (float) Math.max(0.0D, Math.min(1.0D, value));
    }
}
