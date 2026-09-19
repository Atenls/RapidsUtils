package com.atenls.rapidsutils.protocol;

import java.math.BigDecimal;

/** Bounds arithmetic work before a wire number reaches state or rendering. */
final class ProtocolNumber {
    private static final int MAX_CHARACTERS = 256;
    private static final int MAX_PRECISION = 128;
    private static final int MAX_ABSOLUTE_SCALE = 128;

    private ProtocolNumber() {
    }

    static BigDecimal parse(String raw) {
        if (raw.length() > MAX_CHARACTERS) {
            throw new NumberFormatException("Protocol number is too long");
        }
        BigDecimal value = new BigDecimal(raw);
        // This also keeps subsequent double conversions finite and nonzero for nonzero values.
        if (value.precision() > MAX_PRECISION
                || value.scale() < -MAX_ABSOLUTE_SCALE || value.scale() > MAX_ABSOLUTE_SCALE) {
            throw new NumberFormatException("Protocol number precision or scale is out of bounds");
        }
        return value;
    }
}
