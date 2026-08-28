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
        PayloadData data
) {
    public static final int CURRENT_VERSION = 1;

    public Optional<BigDecimal> durationTicks() {
        return number(duration);
    }

    public Optional<BigDecimal> sortIndex() {
        return number(index);
    }

    private static Optional<BigDecimal> number(PayloadData value) {
        if (value instanceof PayloadData.ScalarValue(PayloadData.ScalarKind kind, String raw)
                && kind == PayloadData.ScalarKind.NUMBER) {
            return Optional.of(new BigDecimal(raw));
        }
        return Optional.empty();
    }
}
