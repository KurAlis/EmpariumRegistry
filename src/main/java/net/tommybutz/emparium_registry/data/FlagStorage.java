package net.tommybutz.emparium_registry.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class FlagStorage {

    private static Path getFlagFile(MinecraftServer server, UUID empireId) {
        Path dir = server.getWorldPath(LevelResource.ROOT).resolve("emparium_registry/flags");
        return dir.resolve(empireId + ".png");
    }

    public static void saveFlag(MinecraftServer server, UUID empireId, byte[] pngBytes) throws IOException {
        Path file = getFlagFile(server, empireId);
        Files.createDirectories(file.getParent());
        Files.write(file, pngBytes);
    }

    public static byte[] loadFlag(MinecraftServer server, UUID empireId) throws IOException {
        Path file = getFlagFile(server, empireId);
        if (!Files.exists(file)) return null;
        return Files.readAllBytes(file);
    }

    public static boolean hasFlag(MinecraftServer server, UUID empireId) {
        return Files.exists(getFlagFile(server, empireId));
    }
}