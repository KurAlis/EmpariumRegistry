package net.tommybutz.emparium_registry.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tommybutz.emparium_registry.EmpariumRegistry;

import java.util.UUID;

public record UploadFlagChunkC2SPacket(
        String empireName,
        UUID uploadId,
        int chunkIndex,
        int totalChunks,
        byte[] chunkBytes
) implements CustomPacketPayload {

    public static final int CHUNK_SIZE = 24_000;

    public static final int MAX_TOTAL_BYTES = 1_500_000;

    public static final Type<UploadFlagChunkC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "upload_flag_chunk"));

    public static final StreamCodec<FriendlyByteBuf, UploadFlagChunkC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UploadFlagChunkC2SPacket decode(FriendlyByteBuf buf) {
                    String name = buf.readUtf();
                    UUID id = buf.readUUID();
                    int index = buf.readVarInt();
                    int total = buf.readVarInt();
                    byte[] bytes = ByteBufCodecs.byteArray(CHUNK_SIZE + 64).decode(buf);
                    return new UploadFlagChunkC2SPacket(name, id, index, total, bytes);
                }

                @Override
                public void encode(FriendlyByteBuf buf, UploadFlagChunkC2SPacket packet) {
                    buf.writeUtf(packet.empireName());
                    buf.writeUUID(packet.uploadId());
                    buf.writeVarInt(packet.chunkIndex());
                    buf.writeVarInt(packet.totalChunks());
                    ByteBufCodecs.byteArray(CHUNK_SIZE + 64).encode(buf, packet.chunkBytes());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(UploadFlagChunkC2SPacket packet, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player =
                    (net.minecraft.server.level.ServerPlayer) context.player();
            FlagUploadAssembler.receiveChunk(player, packet);
        });
    }
}