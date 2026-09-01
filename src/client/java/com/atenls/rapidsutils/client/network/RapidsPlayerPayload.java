package com.atenls.rapidsutils.client.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;

public record RapidsPlayerPayload(String json) implements CustomPayload {
    public static final Id<RapidsPlayerPayload> ID = new Id<>(Identifier.of("rapidsclientdata", "player"));
    public static final PacketCodec<RegistryByteBuf, RapidsPlayerPayload> CODEC =
            PacketCodec.of(RapidsPlayerPayload::write, RapidsPlayerPayload::read);

    private static RapidsPlayerPayload read(RegistryByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return new RapidsPlayerPayload(new String(bytes, StandardCharsets.UTF_8));
    }

    private void write(RegistryByteBuf buffer) {
        buffer.writeBytes(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
