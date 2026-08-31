package com.atenls.rapidsutils.state;

public final class VersionReportState {
    private boolean joinedBackend;
    private boolean sentForBackend;

    public boolean onBackendJoin(boolean channelAvailable) {
        if (joinedBackend) {
            sentForBackend = false;
        } else {
            joinedBackend = true;
        }
        return channelAvailable && markSent();
    }

    public boolean onChannelRegistered() {
        return markSent();
    }

    public void onDisconnect() {
        joinedBackend = false;
        sentForBackend = false;
    }

    private boolean markSent() {
        if (sentForBackend) {
            return false;
        }
        sentForBackend = true;
        return true;
    }
}
