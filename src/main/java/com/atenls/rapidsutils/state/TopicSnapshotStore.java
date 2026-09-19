package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.protocol.PayloadData;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class TopicSnapshotStore {
    private static final BigDecimal DEFAULT_DURATION_TICKS = BigDecimal.valueOf(60L);
    private final AtomicReference<State> current = new AtomicReference<>(State.empty());
    private final LongSupplier tickCounter;
    private final Function<String, BigDecimal> fallbackDurationTicks;

    public TopicSnapshotStore(LongSupplier tickCounter) {
        this(tickCounter, topic -> DEFAULT_DURATION_TICKS);
    }

    public TopicSnapshotStore(LongSupplier tickCounter, Function<String, BigDecimal> fallbackDurationTicks) {
        this.tickCounter = Objects.requireNonNull(tickCounter);
        this.fallbackDurationTicks = Objects.requireNonNull(fallbackDurationTicks);
    }

    public UpdateResult apply(DataEnvelope envelope) {
        while (true) {
            State before = current.get();
            Long previousSequence = before.sequences().get(envelope.topic());
            if (previousSequence != null && envelope.sequence() <= previousSequence) {
                return UpdateResult.STALE;
            }

            Map<String, TopicSnapshot> updatedTopics = new LinkedHashMap<>(before.dashboard().topics());
            boolean removal = isRemoval(envelope);
            if (removal) {
                updatedTopics.remove(envelope.topic());
            } else {
                long receivedAtTick = tickCounter.getAsLong();
                TopicSnapshot previous = updatedTopics.get(envelope.topic());
                if (previous != null && isExpired(previous, receivedAtTick)) {
                    // Reactivation has a new first-seen position, just like tick-based expiry followed by insertion.
                    updatedTopics.remove(envelope.topic());
                    previous = null;
                }
                TopicSnapshot updated = previous == null
                        ? new TopicSnapshot(envelope, receivedAtTick)
                        : new TopicSnapshot(
                                envelope,
                                receivedAtTick,
                                previous.firstReceivedAtTick(),
                                previous.fadeInTicks()
                        );
                updatedTopics.put(envelope.topic(), updated);
            }
            Map<String, Long> updatedSequences = new LinkedHashMap<>(before.sequences());
            updatedSequences.put(envelope.topic(), envelope.sequence());
            State after = new State(new DashboardSnapshot(updatedTopics), Map.copyOf(updatedSequences));
            if (current.compareAndSet(before, after)) {
                return removal ? UpdateResult.REMOVED : UpdateResult.ACCEPTED;
            }
        }
    }

    public DashboardSnapshot snapshot() {
        return current.get().dashboard();
    }

    public long currentTick() {
        return tickCounter.getAsLong();
    }

    /** Tick maintenance, independent of whether the HUD is drawn. Sequence baselines are retained. */
    public void expireCompleted() {
        long currentTick = tickCounter.getAsLong();
        while (true) {
            State before = current.get();
            Map<String, TopicSnapshot> updatedTopics = null;
            for (Map.Entry<String, TopicSnapshot> entry : before.dashboard().topics().entrySet()) {
                if (isExpired(entry.getValue(), currentTick)) {
                    if (updatedTopics == null) {
                        updatedTopics = new LinkedHashMap<>(before.dashboard().topics());
                    }
                    updatedTopics.remove(entry.getKey());
                }
            }
            if (updatedTopics == null) {
                return;
            }
            State after = new State(new DashboardSnapshot(updatedTopics), before.sequences());
            if (current.compareAndSet(before, after)) {
                return;
            }
        }
    }

    private boolean isExpired(TopicSnapshot snapshot, long currentTick) {
        return snapshot.isExpiredAfterFadeAt(currentTick, fallbackDurationTicks.apply(snapshot.envelope().topic()));
    }

    public void expire(String topic, long sequence) {
        while (true) {
            State before = current.get();
            TopicSnapshot existing = before.dashboard().topics().get(topic);
            if (existing == null || existing.envelope().sequence() != sequence) {
                return;
            }
            Map<String, TopicSnapshot> updatedTopics = new LinkedHashMap<>(before.dashboard().topics());
            updatedTopics.remove(topic);
            State after = new State(new DashboardSnapshot(updatedTopics), before.sequences());
            if (current.compareAndSet(before, after)) {
                return;
            }
        }
    }

    public void clear() {
        current.set(State.empty());
    }

    private static boolean isRemoval(DataEnvelope envelope) {
        return (envelope.data() instanceof PayloadData.ScalarValue(PayloadData.ScalarKind kind, String ignored)
                && kind == PayloadData.ScalarKind.NULL)
                || envelope.data() instanceof PayloadData.ObjectValue object && object.values().isEmpty();
    }

    private record State(DashboardSnapshot dashboard, Map<String, Long> sequences) {
        private static State empty() {
            return new State(DashboardSnapshot.empty(), Map.of());
        }
    }

    public enum UpdateResult {
        ACCEPTED,
        REMOVED,
        STALE
    }
}
