package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.protocol.PayloadData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicSnapshotStoreTest {
    private static final PayloadData EMPTY = new PayloadData.ObjectValue(java.util.Map.of());
    private static final PayloadData NULL = new PayloadData.ScalarValue(PayloadData.ScalarKind.NULL, "null");

    @Test
    void updatesTopicsIndependentlyWithoutChangingStableDefaultOrder() {
        AtomicLong clock = new AtomicLong(1_000L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);

        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 8)));
        clock.set(1_250L);
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("boss", 2)));
        clock.set(1_500L);
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 9)));

        assertEquals(java.util.List.of("dungeon", "boss"), store.snapshot().orderedForHud(topic -> 10).stream()
                .map(snapshot -> snapshot.envelope().topic())
                .toList());
        assertEquals(9, store.snapshot().topics().get("dungeon").envelope().sequence());
        assertEquals(1_500L, store.snapshot().topics().get("dungeon").receivedAtTick());
    }

    @Test
    void rejectsDuplicateAndOlderSequencesWithoutChangingSnapshot() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        store.apply(envelope("boss", 10));

        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("boss", 10)));
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("boss", 9)));
        assertEquals(10, store.snapshot().topics().get("boss").envelope().sequence());
    }

    @Test
    void clearAllowsAWorldOrConnectionChangeToRestartSequences() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        store.apply(envelope("boss", 10));
        store.clear();

        assertTrue(store.snapshot().topics().isEmpty());
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("boss", 1)));
    }

    @Test
    void numericDurationExpiresInClientTicks() {
        TopicSnapshot snapshot = new TopicSnapshot(envelope("dungeon", 1, number("3"), NULL), 100L);

        assertTrue(snapshot.isVisibleAt(103L, BigDecimal.valueOf(60L)));
        assertEquals(false, snapshot.isVisibleAt(104L, BigDecimal.valueOf(60L)));
    }

    @Test
    void nullDurationFallsBackAndMinusOneRemainsVisible() {
        TopicSnapshot fallback = new TopicSnapshot(envelope("fallback", 1, NULL, NULL), 100L);
        assertTrue(fallback.isVisibleAt(160L, BigDecimal.valueOf(60L)));
        assertEquals(false, fallback.isVisibleAt(161L, BigDecimal.valueOf(60L)));

        TopicSnapshot persistent = new TopicSnapshot(envelope("persistent", 1, number("-1"), NULL), 100L);
        assertTrue(persistent.isVisibleAt(1_000_000L, BigDecimal.valueOf(60L)));
    }

    @Test
    void nullAndEmptyObjectDataRemoveTopicWithoutAllowingStaleResurrection() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        store.apply(envelope("dungeon", 1));

        assertEquals(TopicSnapshotStore.UpdateResult.REMOVED,
                store.apply(envelope("dungeon", 2, NULL, NULL, NULL)));
        assertTrue(store.snapshot().topics().isEmpty());
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("dungeon", 1)));

        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 3)));
        assertEquals(TopicSnapshotStore.UpdateResult.REMOVED,
                store.apply(envelope("dungeon", 4, NULL, NULL, EMPTY)));
        assertTrue(store.snapshot().topics().isEmpty());

        store.clear();
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 1)));
    }

    @Test
    void numericIndexOverridesPerTopicFallbackOrdering() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        store.apply(envelope("fallback-a", 1, NULL, NULL));
        store.apply(envelope("high", 1, NULL, number("20")));
        store.apply(envelope("fallback-b", 1, NULL, NULL));
        store.apply(envelope("low", 1, NULL, number("1")));
        store.apply(envelope("fallback-text", 1, NULL,
                new PayloadData.ScalarValue(PayloadData.ScalarKind.STRING, "later")));
        store.apply(envelope("exact-ten", 1, NULL, number("10")));
        store.apply(envelope("fallback-a", 2, NULL, NULL));

        assertEquals(java.util.List.of(
                        "low", "fallback-b", "fallback-text", "fallback-a", "exact-ten", "high"
                ),
                store.snapshot().orderedForHud(topic -> switch (topic) {
                    case "fallback-b" -> 5;
                    case "fallback-text" -> 7;
                    case "fallback-a" -> 8;
                    default -> 10;
                }).stream()
                        .map(snapshot -> snapshot.envelope().topic())
                        .toList());
    }

    private static DataEnvelope envelope(String topic, long sequence) {
        return envelope(topic, sequence, NULL, NULL);
    }

    private static DataEnvelope envelope(String topic, long sequence, PayloadData duration, PayloadData index) {
        return envelope(topic, sequence, duration, index,
                new PayloadData.ObjectValue(java.util.Map.of(
                        "shown",
                        new PayloadData.ScalarValue(PayloadData.ScalarKind.BOOLEAN, "true")
                )));
    }

    private static DataEnvelope envelope(
            String topic,
            long sequence,
            PayloadData duration,
            PayloadData index,
            PayloadData data
    ) {
        return new DataEnvelope(DataEnvelope.CURRENT_VERSION, topic, sequence, true, duration, index, data);
    }

    private static PayloadData number(String value) {
        return new PayloadData.ScalarValue(PayloadData.ScalarKind.NUMBER, value);
    }
}
