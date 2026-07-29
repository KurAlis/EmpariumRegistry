package net.tommybutz.emparium_registry.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tommybutz.emparium_registry.EmpariumRegistry;

import java.util.UUID;

public record FlagImageS2CPacket(UUID empireId, byte[] imageBytes) implements CustomPacketPayload {

    public static final Type<FlagImageS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "flag_image"));

    public static final StreamCodec<FriendlyByteBuf, FlagImageS2CPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public FlagImageS2CPacket decode(FriendlyByteBuf buf) {
                    UUID id = buf.readUUID();
                    byte[] bytes = ByteBufCodecs.byteArray(UploadFlagChunkC2SPacket.MAX_TOTAL_BYTES + 64).decode(buf);
                    return new FlagImageS2CPacket(id, bytes);
                }

                @Override
                public void encode(FriendlyByteBuf buf, FlagImageS2CPacket packet) {
                    buf.writeUUID(packet.empireId());
                    ByteBufCodecs.byteArray(UploadFlagChunkC2SPacket.MAX_TOTAL_BYTES + 64).encode(buf, packet.imageBytes());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FlagImageS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientFlagCache.setFlag(packet.empireId(), packet.imageBytes()));
    }
}