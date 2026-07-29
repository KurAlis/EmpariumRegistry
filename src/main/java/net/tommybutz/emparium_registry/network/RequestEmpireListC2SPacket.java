package net.tommybutz.emparium_registry.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tommybutz.emparium_registry.EmpariumRegistry;
import net.tommybutz.emparium_registry.data.EmpireSavedData;

// Client sends this when opening the registry screen
public record RequestEmpireListC2SPacket() implements CustomPacketPayload {

    public static final Type<RequestEmpireListC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "request_empire_list"));

    // No data to encode — it's just a signal
    public static final StreamCodec<ByteBuf, RequestEmpireListC2SPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestEmpireListC2SPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(RequestEmpireListC2SPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                var savedData = EmpireSavedData.get(serverLevel);
                var empires = savedData.getAllEmpires();
                for (var empire : empires) {
                    empire.setTotalPlaytimeTicks(
                            net.tommybutz.emparium_registry.util.PlaytimeUtil
                                    .getEmpireTotalPlaytimeTicks(serverLevel.getServer(), empire));
                    try {
                        byte[] flagBytes = net.tommybutz.emparium_registry.data.FlagStorage.loadFlag(
                                serverLevel.getServer(), empire.getEmpireId());
                        if (flagBytes != null) {
                            PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player,
                                    new FlagImageS2CPacket(empire.getEmpireId(), flagBytes));
                        }
                    } catch (Exception e) {
                        EmpariumRegistry.LOGGER.warn("Failed to load flag for empire {}", empire.getName(), e);
                    }
                }
                PacketHandler.sendToPlayer(
                        (net.minecraft.server.level.ServerPlayer) player,
                        new EmpireListS2CPacket(empires));
            }
        });
    }
}