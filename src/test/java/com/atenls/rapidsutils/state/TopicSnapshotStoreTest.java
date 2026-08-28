package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.protocol.PayloadData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicSnapshotStoreTest {
    private static final PayloadData EMPTY = new PayloadData.ObjectValue(java.util.Map.of());

    @Test
    void updatesTopicsIndependentlyAndOrdersByRecency() {
        TopicSnapshotStore store = new TopicSnapshotStore();

        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 8)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("boss", 2)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 9)));

        assertEquals(java.util.List.of("dungeon", "boss"), store.snapshot().newestFirst().stream()
                .map(DataEnvelope::topic)
                .toList());
        assertEquals(9, store.snapshot().topics().get("dungeon").sequence());
    }

    @Test
    void rejectsDuplicateAndOlderSequencesWithoutChangingSnapshot() {
        TopicSnapshotStore store = new TopicSnapshotStore();
        store.apply(envelope("boss", 10));

        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("boss", 10)));
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("boss", 9)));
        assertEquals(10, store.snapshot().topics().get("boss").sequence());
    }

    @Test
    void clearAllowsANewConnectionToRestartSequences() {
        TopicSnapshotStore store = new TopicSnapshotStore();
        store.apply(envelope("boss", 10));
        store.clear();

        assertTrue(store.snapshot().topics().isEmpty());
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("boss", 1)));
    }

    private static DataEnvelope envelope(String topic, long sequence) {
        return new DataEnvelope(DataEnvelope.CURRENT_VERSION, topic, sequence, true, EMPTY);
    }
}
