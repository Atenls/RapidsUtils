package com.atenls.rapidsutils.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionReportStateTest {
    @Test
    void reportsAtJoinWhenRegisterArrivedBeforeWorldChange() {
        VersionReportState state = alreadyReportedBackend();

        assertFalse(state.onChannelRegistered());
        assertTrue(state.onBackendJoin(true));
        assertFalse(state.onChannelRegistered());
    }

    @Test
    void waitsForRegisterWhenWorldChangeAndJoinArriveFirst() {
        VersionReportState state = alreadyReportedBackend();

        assertFalse(state.onBackendJoin(false));
        assertTrue(state.onChannelRegistered());
        assertFalse(state.onChannelRegistered());
    }

    @Test
    void reportsOnlyOnceWhenRegisterPrecedesJoinOnInitialConnection() {
        VersionReportState state = new VersionReportState();

        assertTrue(state.onChannelRegistered());
        assertFalse(state.onBackendJoin(true));
        assertFalse(state.onChannelRegistered());
    }

    @Test
    void disconnectAllowsTheNextConnectionToReport() {
        VersionReportState state = alreadyReportedBackend();

        state.onDisconnect();

        assertTrue(state.onChannelRegistered());
        assertFalse(state.onChannelRegistered());
    }

    private static VersionReportState alreadyReportedBackend() {
        VersionReportState state = new VersionReportState();
        assertTrue(state.onBackendJoin(true));
        return state;
    }
}
