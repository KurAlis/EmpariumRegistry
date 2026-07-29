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

import java.util.Arrays;
import java.util.List;

public record UpdateEmpireC2SPacket(
        String originalName,
        String newName,
        String ideology,
        String capitalName,
        String flagUrl,
        String description,
        String colonies,
        String flagRatio,
        boolean isPublic
) implements CustomPacketPayload {

    public static final Type<UpdateEmpireC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "update_empire"));

    public static final StreamCodec<FriendlyByteBuf, UpdateEmpireC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public UpdateEmpireC2SPacket decode(FriendlyByteBuf buf) {
                    return new UpdateEmpireC2SPacket(
                            buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf(),
                            buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
                }

                @Override
                public void encode(FriendlyByteBuf buf, UpdateEmpireC2SPacket packet) {
                    buf.writeUtf(packet.originalName());
                    buf.writeUtf(packet.newName());
                    buf.writeUtf(packet.ideology());
                    buf.writeUtf(packet.capitalName());
                    buf.writeUtf(packet.flagUrl());
                    buf.writeUtf(packet.description());
                    buf.writeUtf(packet.colonies());
                    buf.writeUtf(packet.flagRatio());
                    buf.writeBoolean(packet.isPublic());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(UpdateEmpireC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            List<String> colonyList = Arrays.stream(packet.colonies().split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            boolean updated = savedData.updateEmpireIfAuthorized(
                    packet.originalName(), packet.newName(), packet.ideology(), packet.capitalName(),
                    packet.flagUrl(), packet.description(), colonyList, packet.flagRatio(), packet.isPublic(), player);

            if (updated) {
                player.sendSystemMessage(Component.literal("§aEmpire updated."));
                PacketHandler.broadcastEmpireList(level, savedData);
            } else {
                player.sendSystemMessage(Component.literal(
                        "§cCouldn't update that empire — no permission, it no longer exists, or the new name is taken."));
            }
        });
    }
}