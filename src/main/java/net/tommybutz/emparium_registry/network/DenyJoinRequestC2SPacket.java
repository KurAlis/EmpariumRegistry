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

import java.util.UUID;

public record DenyJoinRequestC2SPacket(String empireName, UUID targetUUID) implements CustomPacketPayload {

    public static final Type<DenyJoinRequestC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "deny_join_request"));

    public static final StreamCodec<FriendlyByteBuf, DenyJoinRequestC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public DenyJoinRequestC2SPacket decode(FriendlyByteBuf buf) {
                    return new DenyJoinRequestC2SPacket(buf.readUtf(), buf.readUUID());
                }

                @Override
                public void encode(FriendlyByteBuf buf, DenyJoinRequestC2SPacket packet) {
                    buf.writeUtf(packet.empireName());
                    buf.writeUUID(packet.targetUUID());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(DenyJoinRequestC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            boolean denied = savedData.denyJoinRequest(packet.empireName(), packet.targetUUID(), player);
            if (denied) {
                PacketHandler.broadcastEmpireList(level, savedData);

                ServerPlayer requester = level.getServer().getPlayerList().getPlayer(packet.targetUUID());
                if (requester != null) {
                    requester.sendSystemMessage(Component.literal(
                            "§cYour request to join §e" + packet.empireName() + " §cwas denied."));
                }
            } else {
                player.sendSystemMessage(Component.literal(
                        "§cCouldn't deny that request — no permission, or it no longer exists."));
            }
        });
    }
}