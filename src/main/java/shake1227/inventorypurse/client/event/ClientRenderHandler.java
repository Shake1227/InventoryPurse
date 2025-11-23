package shake1227.inventorypurse.client.event;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import shake1227.inventorypurse.InventoryPurse;
import shake1227.inventorypurse.network.ModPackets;
import shake1227.inventorypurse.network.client.ClientLockData;
import shake1227.inventorypurse.network.server.ServerShiftRightClickPacket;

@Mod.EventBusSubscriber(modid = InventoryPurse.MOD_ID, value = Dist.CLIENT)
public class ClientRenderHandler {

    /**
     * マウスボタンが押された瞬間に呼ばれるイベント。
     * これを使って不正な操作を未然にキャンセルする。
     */
    @SubscribeEvent
    public static void onMouseClickPre(ScreenEvent.MouseClicked.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        Slot slot = screen.getSlotUnderMouse();
        // プレーヤーのインベントリ外のスロットは無視
        if (slot == null || slot.container != screen.getMinecraft().player.getInventory()) return;

        int order = getOrderIndexForSlot(slot.getContainerSlot());
        int accessibleSlots = ClientLockData.clientAccessibleSlots;

        // A. ロックされたスロットへのあらゆるクリック操作をキャンセルする
        if (order >= accessibleSlots) {
            event.setCanceled(true);
            return;
        }

        // B. 解放済みスロットでの Shift + 右クリックの特別処理
        if (event.getButton() == 1 && Screen.hasShiftDown()) { // 1 = 右クリック
            if (slot.hasItem()) {
                // デフォルトの動作をキャンセルし、専用パケットをサーバに送信する
                event.setCanceled(true);
                ModPackets.sendToServer(new ServerShiftRightClickPacket(slot.getSlotIndex()));
            }
        }
    }

    /**
     * GUI描画後に呼ばれ、ロックされたスロットを視覚的に隠す。
     */
    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Player player = screen.getMinecraft().player;
        if (player == null) return;
        int accessibleSlots = ClientLockData.clientAccessibleSlots;
        if (accessibleSlots >= 36) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        int color = 0xFFC6C6C6;

        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == player.getInventory() && slot.getContainerSlot() >= 0 && slot.getContainerSlot() < 36) {
                if (getOrderIndexForSlot(slot.getContainerSlot()) >= accessibleSlots) {
                    int x = screen.getGuiLeft() + slot.x;
                    int y = screen.getGuiTop() + slot.y;
                    guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, color);
                }
            }
        }
        guiGraphics.pose().popPose();
    }

    private static int getOrderIndexForSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex <= 8) return slotIndex;
        if (slotIndex >= 9 && slotIndex <= 35) {
            int row = (slotIndex - 9) / 9, col = (slotIndex - 9) % 9;
            return 9 + ((2 - row) * 9) + col;
        }
        return -1;
    }
}