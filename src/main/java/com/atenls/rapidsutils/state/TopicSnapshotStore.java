package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class TopicSnapshotStore {
    private final AtomicReference<DashboardSnapshot> current = new AtomicReference<>(DashboardSnapshot.empty());

    public UpdateResult apply(DataEnvelope envelope) {
        while (true) {
            DashboardSnapshot before = current.get();
            DataEnvelope existing = before.topics().get(envelope.topic());
            if (existing != null && envelope.sequence() <= existing.sequence()) {
                return UpdateResult.STALE;
            }

            Map<String, DataEnvelope> updated = new LinkedHashMap<>(before.topics());
            updated.remove(envelope.topic());
            updated.put(envelope.topic(), envelope);
            DashboardSnapshot after = new DashboardSnapshot(updated);
            if (current.compareAndSet(before, after)) {
                return UpdateResult.ACCEPTED;
            }
        }
    }

    public DashboardSnapshot snapshot() {
        return current.get();
    }

    public void clear() {
        current.set(DashboardSnapshot.empty());
    }

    public enum UpdateResult {
        ACCEPTED,
        STALE
    }
}
