package com.atenls.rapidsutils.state;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DashboardSnapshot(Map<String, TopicSnapshot> topics) {
    private static final BigDecimal DEFAULT_INDEX = BigDecimal.TEN;

    public DashboardSnapshot {
        topics = Collections.unmodifiableMap(new LinkedHashMap<>(topics));
    }

    public static DashboardSnapshot empty() {
        return new DashboardSnapshot(Map.of());
    }

    public List<TopicSnapshot> orderedForHud() {
        ArrayList<TopicSnapshot> values = new ArrayList<>(topics.values());
        values.sort((left, right) -> {
            BigDecimal leftIndex = left.envelope().sortIndex().orElse(DEFAULT_INDEX);
            BigDecimal rightIndex = right.envelope().sortIndex().orElse(DEFAULT_INDEX);
            return leftIndex.compareTo(rightIndex);
        });
        return List.copyOf(values);
    }
}
