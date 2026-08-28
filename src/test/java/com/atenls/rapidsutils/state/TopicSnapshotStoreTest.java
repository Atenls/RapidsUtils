package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.protocol.PayloadData;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicSnapshotStoreTest {
    private static final PayloadData EMPTY = new PayloadData.ObjectValue(java.util.Map.of());

    @Test
    void updatesTopicsIndependentlyAndOrdersByRecency() {
        AtomicLong clock = new AtomicLong(1_000L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);

        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 8)));
        clock.set(1_250L);
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("boss", 2)));
        clock.set(1_500L);
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 9)));

        assertEquals(java.util.List.of("dungeon", "boss"), store.snapshot().newestFirst().stream()
                .map(snapshot -> snapshot.envelope().topic())
                .toList());
        assertEquals(9, store.snapshot().topics().get("dungeon").envelope().sequence());
        assertEquals(1_500L, store.snapshot().topics().get("dungeon").receivedAtMillis());
    }

    @Test
    void rejectsDuplicateAndOlderSequencesWithoutChangingSnapshot() {
        TopicSnapshotStore store = new TopicSnapshotStore();
        store.apply(envelope("boss", 10));

        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("boss", 10)));
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("boss", 9)));
        assertEquals(10, store.snapshot().topics().get("boss").envelope().sequence());
    }

    @Test
    void clearAllowsANewConnectionToRestartSequences() {
        TopicSnapshotStore store = new TopicSnapshotStore();
        store.apply(envelope("boss", 10));
        store.clear();

        assertTrue(store.snapshot().topics().isEmpty());
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("boss", 1)));
    }

    @Test
    void topicVisibilityExpiresAfterConfiguredDuration() {
        TopicSnapshot snapshot = new TopicSnapshot(envelope("dungeon", 1), 1_000L);

        assertTrue(snapshot.isVisibleAt(4_000L, 3_000L));
        assertEquals(false, snapshot.isVisibleAt(4_001L, 3_000L));
    }

    private static DataEnvelope envelope(String topic, long sequence) {
        return new DataEnvelope(DataEnvelope.CURRENT_VERSION, topic, sequence, true, EMPTY);
    }
}
