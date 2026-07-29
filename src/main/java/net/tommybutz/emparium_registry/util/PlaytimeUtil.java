package net.tommybutz.emparium_registry.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.tommybutz.emparium_registry.data.EmpireData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class PlaytimeUtil {

    public static long getPlaytimeTicks(MinecraftServer server, UUID playerUUID) {
        Path statsFile = server.getWorldPath(LevelResource.ROOT)
                .resolve("stats")
                .resolve(playerUUID + ".json");

        if (!Files.exists(statsFile)) return 0L;

        try {
            String content = Files.readString(statsFile);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            if (!root.has("stats")) return 0L;
            JsonObject stats = root.getAsJsonObject("stats");
            if (!stats.has("minecraft:custom")) return 0L;
            JsonObject custom = stats.getAsJsonObject("minecraft:custom");
            if (!custom.has("minecraft:play_time")) return 0L;
            return custom.get("minecraft:play_time").getAsLong();
        } catch (IOException | RuntimeException e) {
            return 0L;
        }
    }

    public static long getEmpireTotalPlaytimeTicks(MinecraftServer server, EmpireData empire) {
        long total = getPlaytimeTicks(server, empire.getEmperorUUID());
        for (UUID member : empire.getMemberUUIDs()) {
            total += getPlaytimeTicks(server, member);
        }
        return total;
    }

    public static String formatTicksAsDuration(long ticks) {
        long totalSeconds = ticks / 20L;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }
}