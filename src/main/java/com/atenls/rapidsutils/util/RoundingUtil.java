package com.atenls.rapidsutils.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RoundingUtil {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1_000L);
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000L);
    private static final BigDecimal BILLION = BigDecimal.valueOf(1_000_000_000L);

    private RoundingUtil() {
    }

    public static double round(double value, int places) {
        return rounded(BigDecimal.valueOf(value), places).doubleValue();
    }

    public static float round(float value, int places) {
        return rounded(BigDecimal.valueOf(value), places).floatValue();
    }

    public static int roundToInt(double value) {
        return (int) Math.round(value);
    }

    public static int roundToInt(float value) {
        return Math.round(value);
    }

    public static String format(double value) {
        return format(BigDecimal.valueOf(value));
    }

    public static String format(float value) {
        return format(BigDecimal.valueOf(value));
    }

    public static String format(BigDecimal value) {
        int places = value.abs().compareTo(BigDecimal.valueOf(100L)) > 0 ? 0 : 2;
        return plain(rounded(value, places));
    }

    public static String longFormat(double value) {
        return longFormat(BigDecimal.valueOf(value));
    }

    public static String longFormat(float value) {
        return longFormat(BigDecimal.valueOf(value));
    }

    public static String longFormat(BigDecimal value) {
        BigDecimal absolute = value.abs();
        if (absolute.compareTo(BILLION) >= 0) {
            return compact(value, BILLION, "B");
        }
        if (absolute.compareTo(MILLION) >= 0) {
            return compact(value, MILLION, "M");
        }
        if (absolute.compareTo(THOUSAND) >= 0) {
            return compact(value, THOUSAND, "K");
        }
        return plain(rounded(value, 2));
    }

    private static String compact(BigDecimal value, BigDecimal divisor, String suffix) {
        return plain(value.divide(divisor, 2, RoundingMode.HALF_UP)) + suffix;
    }

    private static BigDecimal rounded(BigDecimal value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException("小数位数不能为负数");
        }
        return value.setScale(places, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.toBigInteger().toString() : stripped.toPlainString();
    }
}
