package net.tommybutz.emparium_registry.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tommybutz.emparium_registry.EmpariumRegistry;
import net.tommybutz.emparium_registry.data.EmpireSavedData;

public record LeaveEmpireC2SPacket() implements CustomPacketPayload {

    public static final Type<LeaveEmpireC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "leave_empire"));

    public static final StreamCodec<ByteBuf, LeaveEmpireC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new LeaveEmpireC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(LeaveEmpireC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            ServerLevel level = (ServerLevel) player.level();
            EmpireSavedData savedData = EmpireSavedData.get(level);

            boolean left = savedData.leaveEmpire(player.getUUID());
            if (left) {
                player.sendSystemMessage(Component.literal("§aYou left your empire."));
                PacketHandler.broadcastEmpireList(level, savedData);
            } else {
                player.sendSystemMessage(Component.literal(
                        "§cCouldn't leave — you're not in an empire, or you're its emperor (dissolve or transfer it instead)."));
            }
        });
    }
}