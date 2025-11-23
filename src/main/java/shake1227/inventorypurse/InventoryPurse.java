package shake1227.inventorypurse;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import shake1227.inventorypurse.common.registry.ModCommands;
import shake1227.inventorypurse.common.registry.ModCreativeTabs;
import shake1227.inventorypurse.common.registry.ModItems;
import shake1227.inventorypurse.core.config.ServerConfig;
import shake1227.inventorypurse.core.event.PlayerEventHandler;
import shake1227.inventorypurse.network.ModPackets;

@Mod(InventoryPurse.MOD_ID)
public class InventoryPurse {
    public static final String MOD_ID = "inventorypurse";
    public static final Logger LOGGER = LogManager.getLogger();

    public InventoryPurse() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        MinecraftForge.EVENT_BUS.register(new ModCommands());

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC, "inventorypurse-server.toml");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModPackets::register);
    }
}