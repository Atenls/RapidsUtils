package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.protocol.PayloadData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicSnapshotStoreTest {
    private static final PayloadData EMPTY = new PayloadData.ObjectValue(java.util.Map.of());
    private static final PayloadData NULL = new PayloadData.ScalarValue(PayloadData.ScalarKind.NULL, "null");

    @Test
    void capsDistinctActiveTopicsWithoutEvictingExistingSnapshots() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        for (int i = 0; i < TopicSnapshotStore.MAX_TRACKED_TOPICS; i++) {
            assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED,
                    store.apply(envelope("topic-" + i, 1, number("-1"), NULL)));
        }
        var full = store.snapshot();
        assertEquals(TopicSnapshotStore.MAX_TRACKED_TOPICS, full.topics().size());
        assertEquals(TopicSnapshotStore.UpdateResult.CAPACITY_REACHED, store.apply(envelope("overflow", 1)));
        assertSame(full, store.snapshot());
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("topic-0", 1)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("topic-0", 2)));
        assertEquals(TopicSnapshotStore.MAX_TRACKED_TOPICS, store.snapshot().topics().size());
    }

    @Test
    void removalOnlyTrafficIsBoundedWhileKnownTopicsRemainUsable() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        fillWithRemovalMessages(store);
        var emptyButFull = store.snapshot();
        assertTrue(emptyButFull.topics().isEmpty());
        for (int i = 0; i < 100; i++) {
            assertEquals(TopicSnapshotStore.UpdateResult.CAPACITY_REACHED,
                    store.apply(envelope("overflow-" + i, 100, NULL, NULL, NULL)));
        }
        assertSame(emptyButFull, store.snapshot());
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("topic-0", 1)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("topic-0", 2)));
        assertEquals(TopicSnapshotStore.UpdateResult.REMOVED, store.apply(envelope("topic-0", 3, NULL, NULL, EMPTY)));
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("topic-0", 2)));
        assertEquals(TopicSnapshotStore.UpdateResult.CAPACITY_REACHED, store.apply(envelope("overflow", 1)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("topic-0", 4)));
    }

    @Test
    void expiryKeepsCapacityAccountingAndRejectsStaleResurrection() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        fillWithRemovalMessages(store);
        store.apply(envelope("topic-0", 2, number("20"), NULL));
        clock.set(135L);
        store.expireCompleted();
        assertTrue(store.snapshot().topics().isEmpty());
        assertEquals(TopicSnapshotStore.UpdateResult.CAPACITY_REACHED, store.apply(envelope("overflow", 1)));
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("topic-0", 2)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("topic-0", 3)));
        assertEquals(135L, store.snapshot().topics().get("topic-0").firstReceivedAtTick());
    }

    @Test
    void worldOrDisconnectResetReleasesCapacityAndSequenceBaselines() {
        TopicSnapshotStore store = new TopicSnapshotStore(() -> 0L);
        fillWithRemovalMessages(store);
        assertEquals(TopicSnapshotStore.UpdateResult.CAPACITY_REACHED, store.apply(envelope("new-world", 100)));
        store.clear();
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("new-world", 1)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("topic-0", 0)));
    }

    private static void fillWithRemovalMessages(TopicSnapshotStore store) {
        for (int i = 0; i < TopicSnapshotStore.MAX_TRACKED_TOPICS; i++) {
            assertEquals(TopicSnapshotStore.UpdateResult.REMOVED,
                    store.apply(envelope("topic-" + i, 1, NULL, NULL, i % 2 == 0 ? NULL : EMPTY)));
        }
    }

    @Test
    void updatesTopicsIndependentlyWithoutChangingStableDefaultOrder() {
        AtomicLong clock = new AtomicLong(1_000L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);

        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 8)));
        clock.set(1_025L);
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("boss", 2)));
        clock.set(1_050L);
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("dungeon", 9)));

        assertEquals(java.util.List.of("dungeon", "boss"), store.snapshot().orderedForHud(topic -> 10).stream()
                .map(snapshot -> snapshot.envelope().topic())
                .toList());
        assertEquals(9, store.snapshot().topics().get("dungeon").envelope().sequence());
        assertEquals(1_050L, store.snapshot().topics().get("dungeon").receivedAtTick());
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
    void fadesInOnceAndFadesOutAfterTheLatestSnapshotExpires() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        store.apply(envelope("notice", 1, number("20"), NULL));

        TopicSnapshot first = store.snapshot().topics().get("notice");
        assertEquals(0.0F, first.fadeFactorAt(100.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertEquals(0.4F, first.fadeFactorAt(102.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertEquals(1.0F, first.fadeFactorAt(105.0D, BigDecimal.valueOf(60L)), 0.0001F);

        clock.set(110L);
        store.apply(envelope("notice", 2, number("20"), NULL));
        TopicSnapshot updated = store.snapshot().topics().get("notice");
        assertEquals(100L, updated.firstReceivedAtTick());
        assertEquals(110L, updated.receivedAtTick());
        assertEquals(1.0F, updated.fadeFactorAt(110.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertEquals(2.0F / 3.0F, updated.fadeFactorAt(135.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertEquals(0.0F, updated.fadeFactorAt(145.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertTrue(updated.isExpiredAfterFadeAt(145.0D, BigDecimal.valueOf(60L)));
    }

    @Test
    void completedFadeRemovalAllowsTheTopicToFadeInAgain() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        store.apply(envelope("notice", 1));
        store.apply(envelope("notice", 2));

        store.expire("notice", 1);
        assertTrue(store.snapshot().topics().containsKey("notice"));
        store.expire("notice", 2);
        assertTrue(store.snapshot().topics().isEmpty());

        clock.set(200L);
        store.apply(envelope("notice", 3));
        TopicSnapshot repeated = store.snapshot().topics().get("notice");
        assertEquals(200L, repeated.firstReceivedAtTick());
        assertEquals(0.0F, repeated.fadeFactorAt(200.0D, BigDecimal.valueOf(60L)), 0.0001F);
    }

    @Test
    void tickMaintenanceExpiresWithoutRenderingAndPreservesSequenceBaselines() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        store.apply(envelope("notice", 4, number("20"), NULL));
        store.apply(envelope("persistent", 1, number("-1"), NULL));
        var before = store.snapshot();

        clock.set(134L);
        store.expireCompleted();
        assertSame(before, store.snapshot());
        clock.set(135L);
        store.expireCompleted();
        assertEquals(java.util.Set.of("persistent"), store.snapshot().topics().keySet());
        assertEquals(TopicSnapshotStore.UpdateResult.STALE, store.apply(envelope("notice", 4)));
        assertEquals(TopicSnapshotStore.UpdateResult.ACCEPTED, store.apply(envelope("notice", 5)));
        assertEquals(135L, store.snapshot().topics().get("notice").firstReceivedAtTick());
    }

    @Test
    void updateAfterFadeCompletionRestartsBeforeCleanupAndMovesToNewFirstSeenOrder() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        store.apply(envelope("notice", 1, number("20"), NULL));
        store.apply(envelope("persistent", 1, number("-1"), NULL));

        clock.set(135L);
        store.apply(envelopeWithFade("notice", 2, number("20"), "10", "15"));
        TopicSnapshot updated = store.snapshot().topics().get("notice");
        assertEquals(135L, updated.firstReceivedAtTick());
        assertEquals(new BigDecimal("10"), updated.fadeInTicks());
        assertEquals(0.0F, updated.fadeFactorAt(135.0D, BigDecimal.valueOf(60L)));
        assertEquals(0.5F, updated.fadeFactorAt(140.0D, BigDecimal.valueOf(60L)));
        assertEquals(java.util.List.of("persistent", "notice"),
                store.snapshot().orderedForHud(topic -> 10).stream().map(s -> s.envelope().topic()).toList());
        store.expireCompleted();
        assertSame(updated, store.snapshot().topics().get("notice"));
    }

    @Test
    void updateDuringFadeOutKeepsOriginalFadeInAndRefreshesExpiration() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        store.apply(envelopeWithFade("notice", 1, number("20"), "10", "15"));
        clock.set(134L);
        store.apply(envelopeWithFade("notice", 2, number("20"), "40", "15"));

        TopicSnapshot updated = store.snapshot().topics().get("notice");
        assertEquals(100L, updated.firstReceivedAtTick());
        assertEquals(new BigDecimal("10"), updated.fadeInTicks());
        assertEquals(1.0F, updated.fadeFactorAt(134.0D, BigDecimal.valueOf(60L)));
        clock.set(135L);
        store.expireCompleted();
        assertSame(updated, store.snapshot().topics().get("notice"));
    }

    @Test
    void fallbackChangesApplyToTickExpiryAndReactivation() {
        AtomicLong clock = new AtomicLong(100L);
        AtomicLong fallback = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get, topic -> BigDecimal.valueOf(fallback.get()));
        store.apply(envelope("cleanup", 1));
        store.apply(envelope("update", 1));
        clock.set(140L);
        store.expireCompleted();
        assertEquals(2, store.snapshot().topics().size());

        fallback.set(20L);
        store.apply(envelope("update", 2));
        assertEquals(140L, store.snapshot().topics().get("update").firstReceivedAtTick());
        store.expireCompleted();
        assertEquals(java.util.Set.of("update"), store.snapshot().topics().keySet());
    }

    @Test
    void tickExpiryUsesTopicFallbacksButHonorsServerDurations() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get,
                topic -> BigDecimal.valueOf(topic.equals("long") ? 100L : 20L));
        store.apply(envelope("short", 1));
        store.apply(envelope("long", 1));
        store.apply(envelope("server", 1, number("100"), NULL));
        clock.set(135L);
        store.expireCompleted();
        assertEquals(java.util.Set.of("long", "server"), store.snapshot().topics().keySet());
    }

    @Test
    void zeroAndFractionalFadeDurationsKeepExpirationBoundaries() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        store.apply(envelopeWithFade("instant", 1, number("20"), "0", "0"));
        store.apply(envelopeWithFade("fractional", 1, number("20.5"), "0", "0.5"));
        clock.set(120L);
        store.expireCompleted();
        assertEquals(2, store.snapshot().topics().size());
        clock.set(121L);
        store.expireCompleted();
        assertTrue(store.snapshot().topics().isEmpty());
    }

    @Test
    void persistentTopicDoesNotRestartOnAnUpdateLongAfterArrival() {
        AtomicLong clock = new AtomicLong(100L);
        TopicSnapshotStore store = new TopicSnapshotStore(clock::get);
        store.apply(envelope("persistent", 1, number("-1"), NULL));
        clock.set(100_000L);
        store.expireCompleted();
        assertFalse(store.snapshot().topics().isEmpty());
        store.apply(envelope("persistent", 2, number("-1"), NULL));
        assertEquals(100L, store.snapshot().topics().get("persistent").firstReceivedAtTick());
    }

    @Test
    void serverFadeDurationsOverrideTheDefaults() {
        DataEnvelope envelope = new DataEnvelope(
                DataEnvelope.CURRENT_VERSION,
                "notice",
                1,
                true,
                number("10"),
                NULL,
                NULL,
                NULL,
                NULL,
                number("2"),
                number("4"),
                new PayloadData.ObjectValue(java.util.Map.of("shown", number("1")))
        );
        TopicSnapshot snapshot = new TopicSnapshot(envelope, 100L);

        assertEquals(0.5F, snapshot.fadeFactorAt(101.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertEquals(1.0F, snapshot.fadeFactorAt(110.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertEquals(0.5F, snapshot.fadeFactorAt(112.0D, BigDecimal.valueOf(60L)), 0.0001F);
        assertTrue(snapshot.isExpiredAfterFadeAt(114.0D, BigDecimal.valueOf(60L)));
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

    private static DataEnvelope envelopeWithFade(String topic, long sequence, PayloadData duration, String fadeIn, String fadeOut) {
        DataEnvelope base = envelope(topic, sequence, duration, NULL);
        return new DataEnvelope(base.version(), base.topic(), base.sequence(), base.full(),
                base.duration(), base.index(), base.x(), base.y(), base.opacity(),
                number(fadeIn), number(fadeOut), base.data());
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
        return new DataEnvelope(DataEnvelope.CURRENT_VERSION, topic, sequence, true,
                duration, index, NULL, NULL, NULL, data);
    }

    private static PayloadData number(String value) {
        return new PayloadData.ScalarValue(PayloadData.ScalarKind.NUMBER, value);
    }
}
