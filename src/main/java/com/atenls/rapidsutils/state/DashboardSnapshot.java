package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DashboardSnapshot(Map<String, DataEnvelope> topics) {
    public DashboardSnapshot {
        topics = Collections.unmodifiableMap(new LinkedHashMap<>(topics));
    }

    public static DashboardSnapshot empty() {
        return new DashboardSnapshot(Map.of());
    }

    public List<DataEnvelope> newestFirst() {
        ArrayList<DataEnvelope> values = new ArrayList<>(topics.values());
        Collections.reverse(values);
        return List.copyOf(values);
    }
}
