package com.atenls.rapidsutils.equipment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class EquipmentComparison {
    private EquipmentComparison() { }

    public static List<Row> compare(EquipmentSnapshot candidate, EquipmentSnapshot equipped) {
        Set<EquipmentSnapshot.Key> keys = new LinkedHashSet<>(candidate.attributes().keySet());
        keys.addAll(equipped.attributes().keySet());
        return keys.stream().map(key -> {
            var next = candidate.attributes().getOrDefault(key, EquipmentSnapshot.Value.ZERO);
            var old = equipped.attributes().getOrDefault(key, EquipmentSnapshot.Value.ZERO);
            String name = next.displayName().isBlank() ? old.displayName() : next.displayName();
            return new Row(key, name, old, next, next.min().subtract(old.min()), next.max().subtract(old.max()));
        }).toList();
    }

    public static String format(BigDecimal value) {
        // Avoid turning a small, nonzero difference into a misleading zero.
        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        if (rounded.signum() == 0 && value.signum() != 0) {
            return value.signum() > 0 ? "<0.01" : ">-0.01";
        }
        return rounded.toPlainString();
    }

    public record Row(EquipmentSnapshot.Key key, String displayName, EquipmentSnapshot.Value before,
                      EquipmentSnapshot.Value after, BigDecimal minDelta, BigDecimal maxDelta) {
        /** A mixed range cannot be called an unqualified improvement or loss. */
        public int direction(boolean lowerIsBetter) {
            int min = minDelta.signum();
            int max = maxDelta.signum();
            int direction = min >= 0 && max >= 0 ? Math.max(min, max)
                    : min <= 0 && max <= 0 ? Math.min(min, max) : 0;
            return lowerIsBetter ? -direction : direction;
        }
    }
}
