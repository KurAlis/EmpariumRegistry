package net.tommybutz.emparium_registry.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tommybutz.emparium_registry.EmpariumRegistry;
import net.tommybutz.emparium_registry.data.EmpireData;
import net.tommybutz.emparium_registry.data.EmpireSavedData;

public record CreateEmpireC2SPacket(
        String name,
        String ideology,
        String capitalName,
        String flagUrl,
        String description,
        String flagRatio,
        boolean isPublic
) implements CustomPacketPayload {

    public static final Type<CreateEmpireC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "create_empire"));

    public static final StreamCodec<FriendlyByteBuf, CreateEmpireC2SPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CreateEmpireC2SPacket decode(FriendlyByteBuf buf) {
                    return new CreateEmpireC2SPacket(
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readUtf(),
                            buf.readBoolean()
                    );
                }

                @Override
                public void encode(FriendlyByteBuf buf, CreateEmpireC2SPacket packet) {
                    buf.writeUtf(packet.name());
                    buf.writeUtf(packet.ideology());
                    buf.writeUtf(packet.capitalName());
                    buf.writeUtf(packet.flagUrl());
                    buf.writeUtf(packet.description());
                    buf.writeUtf(packet.flagRatio());
                    buf.writeBoolean(packet.isPublic());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(CreateEmpireC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            // Validation
            if (savedData.isNameTaken(packet.name())) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§cThat empire name is already taken!"));
                return;
            }
            if (savedData.getEmpireByPlayer(player.getUUID()) != null) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§cYou are already part of an empire!"));
                return;
            }

            // Create the empire
            EmpireData empire = new EmpireData(
                    packet.name(), packet.ideology(), packet.capitalName(),
                    player.getUUID(), player.getName().getString(), level.getGameTime());
            empire.setFlagUrl(packet.flagUrl());
            empire.setDescription(packet.description());
            empire.setFlagRatio(EmpireData.FlagRatio.valueOf(packet.flagRatio()));
            empire.setPublic(packet.isPublic());

            boolean created = savedData.createEmpire(empire);
            if (created) {
                player.sendSystemMessage(
                        net.minecraft.network.chat.Component.literal(
                                "§aEmpire §e" + packet.name() + " §acreated successfully!"));
                PacketHandler.broadcastEmpireList(level, savedData);
            }
        });
    }
}