package net.tommybutz.emparium_registry.network;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.tommybutz.emparium_registry.EmpariumRegistry;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientFlagCache {

    private static final Map<UUID, ResourceLocation> textures = new HashMap<>();
    private static final Map<UUID, DynamicTexture> owned = new HashMap<>();

    public static void setFlag(UUID empireId, byte[] pngBytes) {
        NativeImage image;
        try {
            image = NativeImage.read(new ByteArrayInputStream(pngBytes));
        } catch (Exception e) {
            EmpariumRegistry.LOGGER.warn("Received an unreadable flag image for {}", empireId, e);
            return;
        }

        DynamicTexture oldTexture = owned.remove(empireId);
        if (oldTexture != null) {
            oldTexture.close();
        }

        DynamicTexture newTexture = new DynamicTexture(image);
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                EmpariumRegistry.MODID, "dynamic/flag_" + empireId);

        Minecraft.getInstance().getTextureManager().register(location, newTexture);
        owned.put(empireId, newTexture);
        textures.put(empireId, location);
    }

    private static ResourceLocation previewTexture;
    private static byte[] previewSourceBytes;

    public static ResourceLocation getOrCreatePreviewTexture(byte[] pngBytes) {
        if (previewSourceBytes == pngBytes && previewTexture != null) {
            return previewTexture;
        }

        try {
            var image = com.mojang.blaze3d.platform.NativeImage.read(new java.io.ByteArrayInputStream(pngBytes));
            var texture = new net.minecraft.client.renderer.texture.DynamicTexture(image);
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    EmpariumRegistry.MODID, "dynamic/flag_preview");
            Minecraft.getInstance().getTextureManager().register(location, texture);
            previewTexture = location;
            previewSourceBytes = pngBytes;
            return location;
        } catch (Exception e) {
            return null;
        }
    }

    public static ResourceLocation getFlagTexture(UUID empireId) {
        return textures.get(empireId);
    }

    public static boolean hasFlag(UUID empireId) {
        return textures.containsKey(empireId);
    }
}