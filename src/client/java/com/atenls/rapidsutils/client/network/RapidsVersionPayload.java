package com.atenls.rapidsutils.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public record RapidsVersionPayload(String version) implements CustomPayload {
    public static final Id<RapidsVersionPayload> ID = new Id<>(Identifier.of("rapidsclientdata", "version"));
    public static final PacketCodec<RegistryByteBuf, RapidsVersionPayload> CODEC =
            PacketCodec.of(RapidsVersionPayload::write, RapidsVersionPayload::read);

    private static RapidsVersionPayload read(RegistryByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return new RapidsVersionPayload(new String(bytes, StandardCharsets.UTF_8));
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeBytes(version.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
