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

    public static final RegistryObject<Item> DRAWSTRING_BAG = ITEMS.register("drawstring_bag",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RED_DRAWSTRING_BAG = ITEMS.register("red_drawstring_bag",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BLACK_DRAWSTRING_BAG = ITEMS.register("black_drawstring_bag",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> WHITE_DRAWSTRING_BAG = ITEMS.register("white_drawstring_bag",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    @SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(DRAWSTRING_BAG);
            event.accept(RED_DRAWSTRING_BAG);
            event.accept(BLACK_DRAWSTRING_BAG);
            event.accept(WHITE_DRAWSTRING_BAG);
        }
    }
}