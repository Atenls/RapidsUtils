package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;
import com.atenls.rapidsutils.protocol.PayloadData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

public final class TopicSnapshotStore {
    private final AtomicReference<State> current = new AtomicReference<>(State.empty());
    private final LongSupplier tickCounter;

    public TopicSnapshotStore(LongSupplier tickCounter) {
        this.tickCounter = tickCounter;
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
