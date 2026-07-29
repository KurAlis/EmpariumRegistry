package net.tommybutz.emparium_registry.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tommybutz.emparium_registry.data.EmpireData;
import net.tommybutz.emparium_registry.data.EmpireSavedData;
import net.tommybutz.emparium_registry.util.PlaytimeUtil;

import java.util.List;

public class PacketHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0");

        // Client -> Server
        registrar.playToServer(
                RequestEmpireListC2SPacket.TYPE,
                RequestEmpireListC2SPacket.STREAM_CODEC,
                RequestEmpireListC2SPacket::handle
        );
        registrar.playToServer(
                CreateEmpireC2SPacket.TYPE,
                CreateEmpireC2SPacket.STREAM_CODEC,
                CreateEmpireC2SPacket::handle
        );
        registrar.playToServer(
                JoinEmpireC2SPacket.TYPE,
                JoinEmpireC2SPacket.STREAM_CODEC,
                JoinEmpireC2SPacket::handle
        );

        // Server -> Client
        registrar.playToClient(
                EmpireListS2CPacket.TYPE,
                EmpireListS2CPacket.STREAM_CODEC,
                EmpireListS2CPacket::handle
        );

        registrar.playToServer(
                UpdateEmpireC2SPacket.TYPE,
                UpdateEmpireC2SPacket.STREAM_CODEC,
                UpdateEmpireC2SPacket::handle
        );
        registrar.playToServer(
                DeleteEmpireC2SPacket.TYPE,
                DeleteEmpireC2SPacket.STREAM_CODEC,
                DeleteEmpireC2SPacket::handle
        );
        registrar.playToServer(
                AcceptJoinRequestC2SPacket.TYPE,
                AcceptJoinRequestC2SPacket.STREAM_CODEC,
                AcceptJoinRequestC2SPacket::handle
        );
        registrar.playToServer(
                DenyJoinRequestC2SPacket.TYPE,
                DenyJoinRequestC2SPacket.STREAM_CODEC,
                DenyJoinRequestC2SPacket::handle
        );
        registrar.playToServer(
                UploadFlagChunkC2SPacket.TYPE,
                UploadFlagChunkC2SPacket.STREAM_CODEC,
                UploadFlagChunkC2SPacket::handle
        );
        registrar.playToClient(
                FlagImageS2CPacket.TYPE,
                FlagImageS2CPacket.STREAM_CODEC,
                FlagImageS2CPacket::handle
        );
        registrar.playToServer(
                LeaveEmpireC2SPacket.TYPE,
                LeaveEmpireC2SPacket.STREAM_CODEC,
                LeaveEmpireC2SPacket::handle);
        registrar.playToServer(
                KickMemberC2SPacket.TYPE,
                KickMemberC2SPacket.STREAM_CODEC,
                KickMemberC2SPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, EmpireListS2CPacket packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    public static void broadcastEmpireList(ServerLevel level, EmpireSavedData savedData) {
        List<EmpireData> empires = savedData.getAllEmpires();
        for (EmpireData empire : empires) {
            empire.setTotalPlaytimeTicks(PlaytimeUtil.getEmpireTotalPlaytimeTicks(level.getServer(), empire));
        }
        EmpireListS2CPacket packet = new EmpireListS2CPacket(empires);
        for (var player : level.getServer().getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }
}