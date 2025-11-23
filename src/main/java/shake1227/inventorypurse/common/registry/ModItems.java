package shake1227.inventorypurse.common.registry;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import shake1227.inventorypurse.InventoryPurse;

@Mod.EventBusSubscriber(modid = InventoryPurse.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, InventoryPurse.MOD_ID);

    public static final RegistryObject<Item> INVENTORY_PURSE = ITEMS.register("inventory_purse",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RED_INVENTORY_PURSE = ITEMS.register("red_inventory_purse",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_INVENTORY_PURSE = ITEMS.register("black_inventory_purse",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHITE_INVENTORY_PURSE = ITEMS.register("white_inventory_purse",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(INVENTORY_PURSE);
            event.accept(RED_INVENTORY_PURSE);
            event.accept(BLACK_INVENTORY_PURSE);
            event.accept(WHITE_INVENTORY_PURSE);
        }
    }
}