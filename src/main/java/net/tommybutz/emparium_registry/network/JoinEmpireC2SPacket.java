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

public record JoinEmpireC2SPacket(String empireName) implements CustomPacketPayload {

    public static final Type<JoinEmpireC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "join_empire"));

    public static final StreamCodec<FriendlyByteBuf, JoinEmpireC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public JoinEmpireC2SPacket decode(FriendlyByteBuf buf) {
                    return new JoinEmpireC2SPacket(buf.readUtf());
                }

                @Override
                public void encode(FriendlyByteBuf buf, JoinEmpireC2SPacket packet) {
                    buf.writeUtf(packet.empireName());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(JoinEmpireC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            EmpireSavedData.JoinResult result = savedData.requestJoin(
                    packet.empireName(), player.getUUID(), player.getName().getString());

            switch (result) {
                case JOINED -> {
                    player.sendSystemMessage(Component.literal("§aYou joined §e" + packet.empireName() + "§a!"));
                    PacketHandler.broadcastEmpireList(level, savedData);
                }
                case REQUEST_SENT -> {
                    player.sendSystemMessage(Component.literal(
                            "§eJoin request sent to §f" + packet.empireName() + "§e — the emperor needs to accept it."));

                    var empire = savedData.getEmpireByName(packet.empireName());
                    if (empire != null) {
                        ServerPlayer emperor = level.getServer().getPlayerList().getPlayer(empire.getEmperorUUID());
                        if (emperor != null) {
                            emperor.sendSystemMessage(Component.literal(
                                    "§e" + player.getName().getString() + " §frequested to join §e" + packet.empireName() + "§f."));
                        }
                        PacketHandler.broadcastEmpireList(level, savedData);
                    }
                }
                case ALREADY_IN_EMPIRE -> player.sendSystemMessage(Component.literal("§cYou're already part of an empire."));
                case ALREADY_REQUESTED -> player.sendSystemMessage(Component.literal("§cYou already have a pending request for that empire."));
                case EMPIRE_FULL -> player.sendSystemMessage(Component.literal("§cThat empire is full."));
                case EMPIRE_NOT_FOUND -> player.sendSystemMessage(Component.literal("§cThat empire doesn't exist."));
            }
        });
    }
}