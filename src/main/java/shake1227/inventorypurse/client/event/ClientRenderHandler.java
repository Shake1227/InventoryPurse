package shake1227.inventorypurse.client.event;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import shake1227.inventorypurse.InventoryPurse;
import shake1227.inventorypurse.network.ModPackets;
import shake1227.inventorypurse.network.client.ClientLockData;
import shake1227.inventorypurse.network.server.ServerShiftRightClickPacket;

@Mod.EventBusSubscriber(modid = InventoryPurse.MOD_ID, value = Dist.CLIENT)
public class ClientRenderHandler {

    // ★最優先で実行して、確実にバニラの挙動をキャンセルする
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseClickPre(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        // 1. ロックされたスロットへの操作キャンセル
        if (cancelEventIfLocked(screen)) {
            event.setCanceled(true);
            return;
        }

        // 2. Shift + 右クリック（MOD独自機能）の完全乗っ取り
        if (event.getButton() == 1 && Screen.hasShiftDown()) { // 1 = 右クリック
            // ★重要: アイテムの有無に関わらず、まずイベントをキャンセルしてバニラ動作を封じる
            event.setCanceled(true);

            Slot slot = screen.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) {
                // ★最重要修正: getSlotIndex() ではなく index (コンテナ全体の絶対ID) を送る
                // これによりサーバー側でのスロット特定ズレを解消する
                ModPackets.sendToServer(new ServerShiftRightClickPacket(slot.index));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseReleasedPre(ScreenEvent.MouseButtonReleased.Pre event) {
        if (cancelEventIfLocked(event.getScreen())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseDragPre(ScreenEvent.MouseDragged.Pre event) {
        if (cancelEventIfLocked(event.getScreen())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseScrollPre(ScreenEvent.MouseScrolled.Pre event) {
        if (cancelEventIfLocked(event.getScreen())) event.setCanceled(true);
    }

    private static boolean cancelEventIfLocked(Screen screen) {
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) return false;
        Slot slot = containerScreen.getSlotUnderMouse();
        // インベントリ外（チェスト側など）は干渉しない
        if (slot == null || slot.container != containerScreen.getMinecraft().player.getInventory()) return false;

        int order = getOrderIndexForSlot(slot.getContainerSlot());
        int accessibleSlots = ClientLockData.clientAccessibleSlots;

        return order >= accessibleSlots;
    }

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