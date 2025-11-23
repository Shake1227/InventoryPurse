package shake1227.inventorypurse.core.event;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
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
    public void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity().level().isClientSide) return;
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (BYPASS_PLAYERS.contains(player.getUUID())) return;

        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();
        int accessibleSlots = getAccessibleSlotCount(player);

        if (event.getResult() == Event.Result.DENY) return;

        int originalCount = stack.getCount();
        ItemStack remaining = insertItemToAccessibleSlots(player.getInventory(), stack, accessibleSlots);
        int pickedUpCount = originalCount - remaining.getCount();

        if (pickedUpCount > 0) {
            serverPlayer.take(itemEntity, pickedUpCount);
            itemEntity.setItem(remaining);
            if (remaining.isEmpty()) itemEntity.discard();
            event.setCanceled(true);
        }
    }

    // ■ Shift + 右クリック処理
    // クライアントから送られてきた slotIndex は「コンテナ全体の絶対ID」である
    public static void handleShiftRightClick(ServerPlayer player, int fromSlotId) {
        PlayerEventHandler handler = new PlayerEventHandler();
        int accessibleSlots = handler.getAccessibleSlotCount(player);
        AbstractContainerMenu menu = player.containerMenu;

        // 範囲チェック
        if (fromSlotId < 0 || fromSlotId >= menu.slots.size()) {
            syncInventory(player);
            return;
        }

        Slot fromSlot = menu.getSlot(fromSlotId);

        // ロック判定: スロットがプレイヤーインベントリの一部なら、ロック範囲か確認
        if (fromSlot.container == player.getInventory()) {
            // getContainerSlot() はインベントリ内の相対ID (0-35など) を返す
            if (getOrderIndexForSlot(fromSlot.getContainerSlot()) >= accessibleSlots) {
                syncInventory(player);
                return;
            }
        }

        if (!fromSlot.hasItem()) {
            syncInventory(player);
            return;
        }

        ItemStack sourceStack = fromSlot.getItem();
        if (sourceStack.isEmpty()) {
            syncInventory(player);
            return;
        }

        // ここからは「プレイヤーインベントリ」に対する操作
        Inventory inv = player.getInventory();
        ItemStack toMove = sourceStack.copy();

        // 移動処理
        int countBefore = toMove.getCount();
        ItemStack remaining = handler.insertItemToAccessibleSlots(inv, toMove, accessibleSlots);
        int movedCount = countBefore - remaining.getCount();

        if (movedCount > 0) {
            // 標準のShiftクリックは「可能な限り移動」なので、右クリック（1個）ではなく全移動を基本とする
            // クライアントで右クリック判定してるので、意図としては「Shift+右クリック」だが
            // 「瞬間移動」の再現なら全移動が正しい挙動となる。
            sourceStack.shrink(movedCount);
            if (sourceStack.isEmpty()) {
                fromSlot.set(ItemStack.EMPTY);
            }
            fromSlot.setChanged();
            player.getInventory().setChanged();
        }

        // 強制同期
        syncInventory(player);
    }

    private static void syncInventory(ServerPlayer player) {
        player.connection.send(new ClientboundContainerSetContentPacket(
                player.containerMenu.containerId,
                player.containerMenu.incrementStateId(),
                player.containerMenu.getItems(),
                player.containerMenu.getCarried()
        ));
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        ServerPlayer player = (ServerPlayer) event.player;
        int accessibleSlots = getAccessibleSlotCount(player);
        int previousSlots = lastAccessibleSlots.getOrDefault(player.getUUID(), accessibleSlots);

        if (accessibleSlots < previousSlots) {
            for (int i = 0; i < 36; i++) {
                if (getOrderIndexForSlot(i) >= accessibleSlots && getOrderIndexForSlot(i) < previousSlots) {
                    dropItemFromSlot(player, i);
                }
            }
        }
        lastAccessibleSlots.put(player.getUUID(), accessibleSlots);

        cancelInvalidPlacements(player, accessibleSlots);
        ModPackets.sendTo(new ClientLockData(accessibleSlots), player);
    }

    private void cancelInvalidPlacements(ServerPlayer player, int accessibleSlots) {
        Inventory inventory = player.getInventory();
        boolean changed = false;

        for (int i = 0; i < 36; i++) {
            if (getOrderIndexForSlot(i) >= accessibleSlots) {
                if (!inventory.getItem(i).isEmpty()) {
                    ItemStack originalStack = inventory.getItem(i).copy();

                    inventory.setItem(i, ItemStack.EMPTY);
                    forceSyncSlot(player, i, ItemStack.EMPTY);

                    ItemStack remaining = insertItemToAccessibleSlots(inventory, originalStack, accessibleSlots);

                    if (!remaining.isEmpty()) {
                        player.drop(remaining, false, false);
                    }
                    changed = true;
                }
            }
        }

        if (changed) {
            player.containerMenu.broadcastChanges();
            player.getInventory().setChanged();
        }
    }

    private static void forceSyncSlot(ServerPlayer player, int inventoryIndex, ItemStack stack) {
        for (Slot slot : player.containerMenu.slots) {
            if (slot.container == player.getInventory() && slot.getContainerSlot() == inventoryIndex) {
                player.connection.send(new ClientboundContainerSetSlotPacket(
                        player.containerMenu.containerId,
                        player.containerMenu.incrementStateId(),
                        slot.index,
                        stack
                ));
                break;
            }
        }
    }

    private void dropItemFromSlot(ServerPlayer player, int slotIndex) {
        ItemStack stack = player.getInventory().getItem(slotIndex);
        if (!stack.isEmpty()) {
            ItemStack toDrop = player.getInventory().removeItem(slotIndex, stack.getCount());
            player.drop(toDrop, false, false);
            forceSyncSlot(player, slotIndex, ItemStack.EMPTY);
        }
    }

    private ItemStack insertItemToAccessibleSlots(Inventory inventory, ItemStack stack, int accessibleSlots) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        // 1. マージ
        for (int i = 0; i < 36; i++) {
            if (getOrderIndexForSlot(i) >= accessibleSlots) continue;

            ItemStack slotStack = inventory.getItem(i);
            if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                int limit = Math.min(slotStack.getMaxStackSize(), inventory.getMaxStackSize());
                int transfer = Math.min(stack.getCount(), limit - slotStack.getCount());
                if (transfer > 0) {
                    slotStack.grow(transfer);
                    stack.shrink(transfer);
                    if (stack.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // 2. 新規配置
        for (int i = 0; i < 36 && !stack.isEmpty(); i++) {
            if (getOrderIndexForSlot(i) >= accessibleSlots) continue;

            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, stack.copy());
                stack.setCount(0);
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

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