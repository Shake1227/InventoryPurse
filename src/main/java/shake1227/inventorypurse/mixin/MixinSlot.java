package shake1227.inventorypurse.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import shake1227.inventorypurse.core.event.PlayerEventHandler;

@Mixin(Slot.class)
public abstract class MixinSlot {

    @Shadow @Final public Container container;
    @Shadow public abstract int getContainerSlot();

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private void isActive(CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof Inventory inventory) {
            if (isLocked(inventory.player)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void mayPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof Inventory inventory) {
            if (isLocked(inventory.player)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void mayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof Inventory inventory) {
            if (isLocked(inventory.player)) {
                cir.setReturnValue(false);
            }
        }
    }

    private boolean isLocked(Player player) {
        int accessibleSlots = PlayerEventHandler.getAccessibleSlotCount(player);
        int orderIndex = PlayerEventHandler.getOrderIndexForSlot(this.getContainerSlot());
        return orderIndex != -1 && orderIndex >= accessibleSlots;
    }
}