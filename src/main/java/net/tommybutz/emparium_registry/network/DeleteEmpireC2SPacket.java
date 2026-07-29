package net.tommybutz.emparium_registry.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tommybutz.emparium_registry.EmpariumRegistry;
import net.tommybutz.emparium_registry.data.EmpireSavedData;

public record DeleteEmpireC2SPacket(String empireName) implements CustomPacketPayload {

    public static final Type<DeleteEmpireC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "delete_empire"));

    public static final StreamCodec<FriendlyByteBuf, DeleteEmpireC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public DeleteEmpireC2SPacket decode(FriendlyByteBuf buf) {
                    return new DeleteEmpireC2SPacket(buf.readUtf());
                }

                @Override
                public void encode(FriendlyByteBuf buf, DeleteEmpireC2SPacket packet) {
                    buf.writeUtf(packet.empireName());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DeleteEmpireC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            boolean deleted = savedData.deleteEmpireIfAuthorized(packet.empireName(), player);
            if (deleted) {
                player.sendSystemMessage(Component.literal("§aEmpire §e" + packet.empireName() + " §adeleted."));
                PacketHandler.broadcastEmpireList(level, savedData);
            } else {
                player.sendSystemMessage(Component.literal(
                        "§cYou don't have permission to delete that empire, or it no longer exists."));
            }
        });
    }
}