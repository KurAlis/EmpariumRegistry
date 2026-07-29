package net.tommybutz.emparium_registry.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = net.tommybutz.emparium_registry.EmpariumRegistry.MODID,
        value = Dist.CLIENT)
public class ModKeybinds {

    public static final KeyMapping OPEN_REGISTRY = new KeyMapping(
            "key.emparium_registry.open_registry",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories.emparium_registry"
    );

    @SubscribeEvent
    public static void registerKeybinds(RegisterKeyMappingsEvent event) {
        event.register(OPEN_REGISTRY);
    }
}