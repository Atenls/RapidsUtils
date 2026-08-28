package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.DataEnvelope;

public record TopicSnapshot(DataEnvelope envelope, long receivedAtMillis) {
    public boolean isVisibleAt(long nowMillis, long durationMillis) {
        return nowMillis - receivedAtMillis <= durationMillis;
    }
}
