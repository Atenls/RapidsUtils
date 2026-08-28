package com.atenls.rapidsutils.protocol;

public record DataEnvelope(int version, String topic, long sequence, boolean full, PayloadData data) {
    public static final int CURRENT_VERSION = 1;
}
