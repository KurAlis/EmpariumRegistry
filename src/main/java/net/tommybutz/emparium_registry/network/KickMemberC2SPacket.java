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

public record KickMemberC2SPacket(String empireName, UUID targetUUID) implements CustomPacketPayload {

    public static final Type<KickMemberC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "kick_member"));

    public static final StreamCodec<FriendlyByteBuf, KickMemberC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public KickMemberC2SPacket decode(FriendlyByteBuf buf) {
                    return new KickMemberC2SPacket(buf.readUtf(), buf.readUUID());
                }

                @Override
                public void encode(FriendlyByteBuf buf, KickMemberC2SPacket packet) {
                    buf.writeUtf(packet.empireName());
                    buf.writeUUID(packet.targetUUID());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(KickMemberC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            boolean kicked = savedData.kickMember(packet.empireName(), packet.targetUUID(), player);
            if (kicked) {
                PacketHandler.broadcastEmpireList(level, savedData);

                ServerPlayer kickedPlayer = level.getServer().getPlayerList().getPlayer(packet.targetUUID());
                if (kickedPlayer != null) {
                    kickedPlayer.sendSystemMessage(Component.literal(
                            "§cYou were removed from §e" + packet.empireName() + "§c."));
                }
            } else {
                player.sendSystemMessage(Component.literal(
                        "§cCouldn't kick that player — no permission, they're the emperor, or they're not a member."));
            }
        });
    }
}