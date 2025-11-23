package shake1227.inventorypurse.common.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import shake1227.inventorypurse.InventoryPurse;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, InventoryPurse.MOD_ID);

    public static final RegistryObject<CreativeModeTab> INVENTORYPURSE_TAB = CREATIVE_MODE_TABS.register("inventorypurse_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.DRAWSTRING_BAG.get()))
                    .title(Component.translatable("creativetab.inventorypurse_tab"))
                    .displayItems((pParameters, pOutput) -> {
                        pOutput.accept(ModItems.DRAWSTRING_BAG.get());
                        pOutput.accept(ModItems.RED_DRAWSTRING_BAG.get());
                        pOutput.accept(ModItems.BLACK_DRAWSTRING_BAG.get());
                        pOutput.accept(ModItems.WHITE_DRAWSTRING_BAG.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}