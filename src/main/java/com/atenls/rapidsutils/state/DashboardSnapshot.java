package com.atenls.rapidsutils.state;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

public record DashboardSnapshot(Map<String, TopicSnapshot> topics) {
    public DashboardSnapshot {
        topics = Collections.unmodifiableMap(new LinkedHashMap<>(topics));
    }

    public static DashboardSnapshot empty() {
        return new DashboardSnapshot(Map.of());
    }

    public List<TopicSnapshot> orderedForHud(ToIntFunction<String> fallbackIndex) {
        ArrayList<TopicSnapshot> values = new ArrayList<>(topics.values());
        values.sort((left, right) -> {
            BigDecimal leftIndex = left.envelope().sortIndex()
                    .orElseGet(() -> BigDecimal.valueOf(fallbackIndex.applyAsInt(left.envelope().topic())));
            BigDecimal rightIndex = right.envelope().sortIndex()
                    .orElseGet(() -> BigDecimal.valueOf(fallbackIndex.applyAsInt(right.envelope().topic())));
            return leftIndex.compareTo(rightIndex);
        });
        return List.copyOf(values);
    }
}
