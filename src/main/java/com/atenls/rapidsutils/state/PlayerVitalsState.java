package com.atenls.rapidsutils.state;

import com.atenls.rapidsutils.protocol.PlayerVitals;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public final class PlayerVitalsState {
    private final AtomicReference<PlayerVitals> current = new AtomicReference<>();

    public void update(PlayerVitals vitals) {
        current.set(vitals);
    }

    public Optional<PlayerVitals> snapshot() {
        return Optional.ofNullable(current.get());
    }

    public void clear() {
        current.set(null);
    }
}
