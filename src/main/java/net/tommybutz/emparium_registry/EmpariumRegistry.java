package net.tommybutz.emparium_registry;

import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.tommybutz.emparium_registry.network.PacketHandler;
import org.slf4j.Logger;

@Mod(EmpariumRegistry.MODID)
public class EmpariumRegistry {

    public static final String MODID = "emparium_registry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EmpariumRegistry(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(PacketHandler::register);
        if (FMLEnvironment.dist.isClient()) {
            modEventBus.addListener(this::clientSetup);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void clientSetup(FMLClientSetupEvent event) {
        // Client setup if needed later
    }
}