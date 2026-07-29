package net.tommybutz.emparium_registry.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tommybutz.emparium_registry.EmpariumRegistry;
import net.tommybutz.emparium_registry.data.EmpireData;

import java.util.ArrayList;
import java.util.List;

public record EmpireListS2CPacket(List<EmpireData> empires) implements CustomPacketPayload {

    public static final Type<EmpireListS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(EmpariumRegistry.MODID, "empire_list"));

    public static final StreamCodec<FriendlyByteBuf, EmpireListS2CPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public EmpireListS2CPacket decode(FriendlyByteBuf buf) {
                    int count = buf.readInt();
                    List<EmpireData> empires = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        EmpireData data = EmpireData.load(buf.readNbt());
                        data.setTotalPlaytimeTicks(buf.readLong());
                        empires.add(data);
                    }
                    return new EmpireListS2CPacket(empires);
                }

                @Override
                public void encode(FriendlyByteBuf buf, EmpireListS2CPacket packet) {
                    buf.writeInt(packet.empires().size());
                    for (EmpireData empire : packet.empires()) {
                        buf.writeNbt(empire.save());
                        buf.writeLong(empire.getTotalPlaytimeTicks());
                    }
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(EmpireListS2CPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientEmpireCache.setEmpires(packet.empires());

            var screen = net.minecraft.client.Minecraft.getInstance().screen;
            if (screen instanceof net.tommybutz.emparium_registry.client.gui.RegistryScreen registryScreen) {
                registryScreen.refreshEmpires();
            }
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                for (var empire : packet.empires()) {
                    ClientPendingFlagUploads.checkAndFire(empire, mc.player.getUUID());
                }
            }
        });
    }
}