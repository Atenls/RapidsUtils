package com.atenls.rapidsutils.client.network;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.atenls.rapidsutils.protocol.DataEnvelopeParser;
import com.atenls.rapidsutils.state.TopicSnapshotStore;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class RapidsDataReceiver {
    private RapidsDataReceiver() {
    }

    public static void register(TopicSnapshotStore store) {
        PayloadTypeRegistry.playS2C().register(RapidsDataPayload.ID, RapidsDataPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(RapidsDataPayload.ID, (payload, context) ->
                DataEnvelopeParser.parse(payload.json()).ifPresentOrElse(
                        store::apply,
                        () -> RapidsUtilsClient.LOGGER.debug("Ignored invalid rapidsclientdata:data payload")
                ));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> store.clear());
    }
}
