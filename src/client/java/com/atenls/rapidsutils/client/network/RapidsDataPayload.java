package com.atenls.rapidsutils.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public record RapidsDataPayload(String json) implements CustomPayload {
    public static final Id<RapidsDataPayload> ID = new Id<>(Identifier.of("rapidsclientdata", "data"));
    public static final PacketCodec<RegistryByteBuf, RapidsDataPayload> CODEC =
            PacketCodec.of(RapidsDataPayload::write, RapidsDataPayload::read);

    private static RapidsDataPayload read(RegistryByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return new RapidsDataPayload(new String(bytes, StandardCharsets.UTF_8));
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeBytes(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
