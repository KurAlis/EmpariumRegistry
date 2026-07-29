package net.tommybutz.emparium_registry.network;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.tommybutz.emparium_registry.EmpariumRegistry;
import net.tommybutz.emparium_registry.data.EmpireData;
import net.tommybutz.emparium_registry.data.EmpireSavedData;
import net.tommybutz.emparium_registry.data.FlagStorage;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlagUploadAssembler {

    private record InProgress(UUID uploadId, String empireName, byte[][] chunks, int received) {}

    private static final Map<UUID, InProgress> uploads = new HashMap<>();

    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    public static void receiveChunk(ServerPlayer player, UploadFlagChunkC2SPacket packet) {
        UUID playerId = player.getUUID();
        InProgress current = uploads.get(playerId);

        if (current == null || !current.uploadId.equals(packet.uploadId())) {
            current = new InProgress(packet.uploadId(), packet.empireName(),
                    new byte[packet.totalChunks()][], 0);
        }

        if (packet.chunkIndex() < 0 || packet.chunkIndex() >= current.chunks.length) {
            uploads.remove(playerId);
            return;
        }

        current.chunks[packet.chunkIndex()] = packet.chunkBytes();
        int receivedCount = current.received + 1;
        InProgress updated = new InProgress(current.uploadId, current.empireName, current.chunks, receivedCount);
        uploads.put(playerId, updated);

        if (receivedCount < current.chunks.length) {
            return;
        }

        uploads.remove(playerId);
        assembleAndSave(player, updated);
    }

    private static void assembleAndSave(ServerPlayer player, InProgress upload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] chunk : upload.chunks) {
            if (chunk == null) {
                player.sendSystemMessage(Component.literal("§cFlag upload failed — a chunk went missing."));
                return;
            }
            out.writeBytes(chunk);
        }
        byte[] fullImage = out.toByteArray();

        if (fullImage.length == 0 || fullImage.length > UploadFlagChunkC2SPacket.MAX_TOTAL_BYTES
                || !looksLikePng(fullImage)) {
            player.sendSystemMessage(Component.literal("§cThat image couldn't be uploaded — invalid or too large."));
            return;
        }

        ServerLevel level = (ServerLevel) player.level();
        EmpireSavedData savedData = EmpireSavedData.get(level);
        EmpireData empire = savedData.getEmpireByName(upload.empireName);

        if (empire == null || !savedData.canManage(empire, player)) {
            player.sendSystemMessage(Component.literal("§cYou can't upload a flag for that empire."));
            return;
        }

        try {
            FlagStorage.saveFlag(level.getServer(), empire.getEmpireId(), fullImage);
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cFailed to save the flag image."));
            EmpariumRegistry.LOGGER.error("Failed to save flag for empire {}", empire.getName(), e);
            return;
        }

        player.sendSystemMessage(Component.literal("§aFlag uploaded."));

        FlagImageS2CPacket flagPacket = new FlagImageS2CPacket(empire.getEmpireId(), fullImage);
        for (var online : level.getServer().getPlayerList().getPlayers()) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(online, flagPacket);
        }
    }

    private static boolean looksLikePng(byte[] bytes) {
        if (bytes.length < PNG_SIGNATURE.length) return false;
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (bytes[i] != PNG_SIGNATURE[i]) return false;
        }
        return true;
    }
}