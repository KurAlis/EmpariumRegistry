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

public record AcceptJoinRequestC2SPacket(String empireName, UUID targetUUID) implements CustomPacketPayload {

    public static final Type<AcceptJoinRequestC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "accept_join_request"));

    public static final StreamCodec<FriendlyByteBuf, AcceptJoinRequestC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public AcceptJoinRequestC2SPacket decode(FriendlyByteBuf buf) {
                    return new AcceptJoinRequestC2SPacket(buf.readUtf(), buf.readUUID());
                }

                @Override
                public void encode(FriendlyByteBuf buf, AcceptJoinRequestC2SPacket packet) {
                    buf.writeUtf(packet.empireName());
                    buf.writeUUID(packet.targetUUID());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AcceptJoinRequestC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            boolean accepted = savedData.acceptJoinRequest(packet.empireName(), packet.targetUUID(), player);
            if (accepted) {
                PacketHandler.broadcastEmpireList(level, savedData);
                ServerPlayer newMember = level.getServer().getPlayerList().getPlayer(packet.targetUUID());
                if (newMember != null) {
                    newMember.sendSystemMessage(Component.literal(
                            "§aYour request to join §e" + packet.empireName() + " §awas accepted!"));
                }
            } else {
                player.sendSystemMessage(Component.literal(
                        "§cCouldn't accept that request — no permission, it's stale, or the empire is full."));
            }
        });
    }
}