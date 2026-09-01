package com.atenls.rapidsutils.protocol;

import java.math.BigDecimal;
import java.util.Optional;

public record DataEnvelope(
        int version,
        String topic,
        long sequence,
        boolean full,
        PayloadData duration,
        PayloadData index,
        PayloadData x,
        PayloadData y,
        PayloadData opacity,
        PayloadData fadeIn,
        PayloadData fadeOut,
        PayloadData data
) {
    public static final int CURRENT_VERSION = 1;

    public Optional<BigDecimal> durationTicks() {
        return number(duration);
    }

    public Optional<BigDecimal> sortIndex() {
        return number(index);
    }

    public Optional<BigDecimal> screenX() {
        return number(x);
    }

    public Optional<BigDecimal> screenY() {
        return number(y);
    }

    public Optional<BigDecimal> resolvedScreenX(int screenWidth) {
        return screenX().map(value -> resolveCoordinate(value, screenWidth));
    }

    public Optional<BigDecimal> resolvedScreenY(int screenHeight) {
        return screenY().map(value -> resolveCoordinate(value, screenHeight));
    }

    public Optional<BigDecimal> panelOpacity() {
        return number(opacity);
    }

    public Optional<BigDecimal> fadeInTicks() {
        return nonNegativeNumber(fadeIn);
    }

    public Optional<BigDecimal> fadeOutTicks() {
        return nonNegativeNumber(fadeOut);
    }

    public DataEnvelope(
            int version,
            String topic,
            long sequence,
            boolean full,
            PayloadData duration,
            PayloadData index,
            PayloadData x,
            PayloadData y,
            PayloadData opacity,
            PayloadData data
    ) {
        this(version, topic, sequence, full, duration, index, x, y, opacity, nullValue(), nullValue(), data);
    }

    private static Optional<BigDecimal> number(PayloadData value) {
        if (value instanceof PayloadData.ScalarValue(PayloadData.ScalarKind kind, String raw)
                && kind == PayloadData.ScalarKind.NUMBER) {
            return Optional.of(new BigDecimal(raw));
        }
        return Optional.empty();
    }

    private static Optional<BigDecimal> nonNegativeNumber(PayloadData value) {
        return number(value).filter(number -> number.signum() >= 0);
    }

    private static PayloadData nullValue() {
        return new PayloadData.ScalarValue(PayloadData.ScalarKind.NULL, "null");
    }

    private static BigDecimal resolveCoordinate(BigDecimal value, int screenSize) {
        if (value.signum() >= 0 && value.compareTo(BigDecimal.ONE) < 0 && value.scale() > 0) {
            return value.multiply(BigDecimal.valueOf(screenSize));
        }
        return value;
    }
}
