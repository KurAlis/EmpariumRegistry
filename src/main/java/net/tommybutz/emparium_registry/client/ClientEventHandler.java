package net.tommybutz.emparium_registry.client;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.tommybutz.emparium_registry.client.gui.RegistryScreen;
import net.tommybutz.emparium_registry.network.RequestEmpireListC2SPacket;

@EventBusSubscriber(modid = net.tommybutz.emparium_registry.EmpariumRegistry.MODID,
        value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        while (ModKeybinds.OPEN_REGISTRY.consumeClick()) {
            if (mc.screen == null) {
                PacketDistributor.sendToServer(new RequestEmpireListC2SPacket());
                mc.setScreen(new RegistryScreen());
            }
        }
    }
}