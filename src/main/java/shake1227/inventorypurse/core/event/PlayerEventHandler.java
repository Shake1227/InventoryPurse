package shake1227.inventorypurse.core.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import shake1227.inventorypurse.common.registry.ModItems;
import shake1227.inventorypurse.core.config.ServerConfig;
import shake1227.inventorypurse.network.ModPackets;
import shake1227.inventorypurse.network.client.ClientLockData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEventHandler {

    private static final Set<UUID> BYPASS_PLAYERS = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, Integer> lastAccessibleSlots = new ConcurrentHashMap<>();

    public static void addBypassPlayer(UUID uuid) { BYPASS_PLAYERS.add(uuid); }
    public static void removeBypassPlayer(UUID uuid) { BYPASS_PLAYERS.remove(uuid); }

    public int getAccessibleSlotCount(Player player) {
        if (BYPASS_PLAYERS.contains(player.getUUID())) return 36;
        int baseSlots = ServerConfig.BASE_SLOTS.get();
        int bonusSlots = 0;
        Set<net.minecraft.world.item.Item> checkedItems = new HashSet<>();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldStack = player.getItemInHand(hand);
            if (heldStack.isEmpty() || checkedItems.contains(heldStack.getItem())) continue;
            String handName = hand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand";
            if (heldStack.is(ModItems.INVENTORY_PURSE.get()) && ServerConfig.PURSE_ACTIVATION.get().contains(handName)) {
                bonusSlots += ServerConfig.PURSE_SLOTS.get();
                checkedItems.add(heldStack.getItem());
            } else if (heldStack.is(ModItems.RED_INVENTORY_PURSE.get()) && ServerConfig.RED_PURSE_ACTIVATION.get().contains(handName)) {
                bonusSlots += ServerConfig.RED_PURSE_SLOTS.get();
                checkedItems.add(heldStack.getItem());
            } else if (heldStack.is(ModItems.BLACK_INVENTORY_PURSE.get()) && ServerConfig.BLACK_PURSE_ACTIVATION.get().contains(handName)) {
                bonusSlots += ServerConfig.BLACK_PURSE_SLOTS.get();
                checkedItems.add(heldStack.getItem());
            } else if (heldStack.is(ModItems.WHITE_INVENTORY_PURSE.get()) && ServerConfig.WHITE_PURSE_ACTIVATION.get().contains(handName)) {
                bonusSlots += ServerConfig.WHITE_PURSE_SLOTS.get();
                checkedItems.add(heldStack.getItem());
            }
        }
        return Math.min(36, baseSlots + bonusSlots);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        ServerPlayer player = (ServerPlayer) event.player;
        int accessibleSlots = getAccessibleSlotCount(player);
        int previousSlots = lastAccessibleSlots.getOrDefault(player.getUUID(), accessibleSlots);

        // A. スロット数が変わった時だけ、旧ロック範囲のアイテムをドロップ
        if (accessibleSlots < previousSlots) {
            for (int i = 0; i < 36; i++) {
                if (getOrderIndexForSlot(i) >= accessibleSlots && getOrderIndexForSlot(i) < previousSlots) {
                    dropItemFromSlot(player, i);
                }
            }
        }
        lastAccessibleSlots.put(player.getUUID(), accessibleSlots);

        // B. 毎Tick、現在のロック範囲に不正に入ったアイテムをキャンセル（移動 or ドロップ）
        cancelInvalidPlacements(player, accessibleSlots);

        ModPackets.sendTo(new ClientLockData(accessibleSlots), player);
    }

    private void cancelInvalidPlacements(ServerPlayer player, int accessibleSlots) {
        Inventory inventory = player.getInventory();
        boolean changed = false;
        for (int i = 0; i < 36; i++) {
            if (getOrderIndexForSlot(i) >= accessibleSlots) {
                if (!inventory.getItem(i).isEmpty()) {
                    ItemStack stack = inventory.getItem(i).copy();
                    inventory.setItem(i, ItemStack.EMPTY); // まずスロットを空にする

                    // 解放済みスロットに移動を試みる
                    ItemStack remaining = moveToAccessible(inventory, stack, accessibleSlots);

                    // 残りがあればドロップ
                    if (!remaining.isEmpty()) {
                        player.drop(remaining, false, false);
                    }
                    changed = true;
                }
            }
        }
        if (changed) player.containerMenu.broadcastChanges();
    }

    // Shift+右クリックの処理（パケットから呼ばれる）
    public static void handleShiftRightClick(ServerPlayer player, int fromSlotIndex) {
        PlayerEventHandler handler = new PlayerEventHandler(); // non-staticメソッドを呼ぶため
        int accessibleSlots = handler.getAccessibleSlotCount(player);
        AbstractContainerMenu menu = player.containerMenu;
        if (fromSlotIndex < 0 || fromSlotIndex >= menu.slots.size()) return;

        Slot fromSlot = menu.getSlot(fromSlotIndex);
        if (!fromSlot.hasItem()) return;

        // ターゲットスロットを探す (解放済み & 空き)
        int targetSlotIndex = -1;
        for (int i = 0; i < 36; i++) {
            if (getOrderIndexForSlot(i) < accessibleSlots && player.getInventory().getItem(i).isEmpty()) {
                targetSlotIndex = i;
                break;
            }
        }

        if (targetSlotIndex != -1) {
            ItemStack sourceStack = fromSlot.getItem();
            ItemStack newStack = sourceStack.copy();
            newStack.setCount(1); // 1個だけ移動

            player.getInventory().setItem(targetSlotIndex, newStack);
            sourceStack.shrink(1);
            menu.broadcastChanges();
        }
    }

    private void dropItemFromSlot(ServerPlayer player, int slotIndex) {
        ItemStack stack = player.getInventory().getItem(slotIndex);
        if (!stack.isEmpty()) {
            player.drop(player.getInventory().removeItem(slotIndex, stack.getCount()), false, false);
        }
    }

    private ItemStack moveToAccessible(Inventory inventory, ItemStack stack, int accessibleSlots) {
        for (int i = 0; i < 36 && !stack.isEmpty(); i++) {
            if (getOrderIndexForSlot(i) < accessibleSlots) {
                if (inventory.getItem(i).isEmpty()) {
                    inventory.setItem(i, stack.copy());
                    stack.setCount(0);
                    return ItemStack.EMPTY;
                }
            }
        }
        return stack;
    }

    // 他のイベントハンドラ
    @SubscribeEvent public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent e) { scheduleUpdate(e.getEntity()); }
    @SubscribeEvent public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent e) { scheduleUpdate(e.getEntity()); }
    @SubscribeEvent public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent e) { scheduleUpdate(e.getEntity()); }

    private void scheduleUpdate(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.getServer().execute(() -> {
                lastAccessibleSlots.put(sp.getUUID(), getAccessibleSlotCount(sp));
            });
        }
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