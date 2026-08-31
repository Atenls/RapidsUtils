package com.atenls.rapidsutils.client.network;

import com.atenls.rapidsutils.client.RapidsUtilsClient;
import com.atenls.rapidsutils.protocol.DataEnvelopeParser;
import com.atenls.rapidsutils.state.TopicSnapshotStore;
import net.fabricmc.fabric.api.client.networking.v1.C2SPlayChannelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import java.util.concurrent.atomic.AtomicBoolean;

public final class RapidsDataReceiver {
    private RapidsDataReceiver() {
    }

    public static void register(TopicSnapshotStore store) {
        AtomicBoolean versionSent = new AtomicBoolean();
        PayloadTypeRegistry.playC2S().register(RapidsVersionPayload.ID, RapidsVersionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RapidsDataPayload.ID, RapidsDataPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(RapidsDataPayload.ID, (payload, context) ->
                DataEnvelopeParser.parse(payload.json()).ifPresentOrElse(
                        store::apply,
                        () -> RapidsUtilsClient.LOGGER.debug("Ignored invalid rapidsclientdata:data payload")
                ));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (ClientPlayNetworking.canSend(RapidsVersionPayload.ID)) {
                sendVersion(sender, versionSent);
            }
        });
        C2SPlayChannelEvents.REGISTER.register((handler, sender, client, channels) -> {
            if (channels.contains(RapidsVersionPayload.ID.id())) {
                sendVersion(sender, versionSent);
            }
        });
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> store.clear());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            versionSent.set(false);
            store.clear();
        });
    }

    private static void sendVersion(PacketSender sender, AtomicBoolean versionSent) {
        if (versionSent.compareAndSet(false, true)) {
            sender.sendPacket(new RapidsVersionPayload(RapidsUtilsClient.VERSION));
        }
    }
}
