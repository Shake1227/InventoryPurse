package shake1227.inventorypurse.core.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import shake1227.inventorypurse.common.registry.ModItems;
import shake1227.inventorypurse.core.config.ServerConfig;
import shake1227.inventorypurse.network.ModPackets;
import shake1227.inventorypurse.network.client.ClientLockData;
import shake1227.modernnotification.api.ModernNotificationAPI;
import shake1227.modernnotification.core.NotificationCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerEventHandler {

    private static final Set<UUID> BYPASS_PLAYERS = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, Integer> lastAccessibleSlots = new ConcurrentHashMap<>();

    public static void addBypassPlayer(UUID uuid) { BYPASS_PLAYERS.add(uuid); }
    public static void removeBypassPlayer(UUID uuid) { BYPASS_PLAYERS.remove(uuid); }

    public static int getAccessibleSlotCount(Player player) {
        if (player.level().isClientSide) {
            return ClientLockData.clientAccessibleSlots;
        }

        if (BYPASS_PLAYERS.contains(player.getUUID())) return 36;

        int baseSlots = ServerConfig.BASE_SLOTS.get();
        int bonusSlots = 0;
        Set<net.minecraft.world.item.Item> checkedItems = new HashSet<>();

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack heldStack = player.getItemInHand(hand);
            if (heldStack.isEmpty() || checkedItems.contains(heldStack.getItem())) continue;

            String handName = hand == InteractionHand.MAIN_HAND ? "mainhand" : "offhand";
            if (heldStack.is(ModItems.DRAWSTRING_BAG.get()) && ServerConfig.PURSE_ACTIVATION.get().contains(handName)) {
                bonusSlots += ServerConfig.PURSE_SLOTS.get();
                checkedItems.add(heldStack.getItem());
            } else if (heldStack.is(ModItems.RED_DRAWSTRING_BAG.get()) && ServerConfig.RED_PURSE_ACTIVATION.get().contains(handName)) {
                bonusSlots += ServerConfig.RED_PURSE_SLOTS.get();
                checkedItems.add(heldStack.getItem());
            } else if (heldStack.is(ModItems.BLACK_DRAWSTRING_BAG.get()) && ServerConfig.BLACK_PURSE_ACTIVATION.get().contains(handName)) {
                bonusSlots += ServerConfig.BLACK_PURSE_SLOTS.get();
                checkedItems.add(heldStack.getItem());
            } else if (heldStack.is(ModItems.WHITE_DRAWSTRING_BAG.get()) && ServerConfig.WHITE_PURSE_ACTIVATION.get().contains(handName)) {
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

        int accessibleSlots = getAccessibleSlotCount(player);
        if (accessibleSlots >= 36) return;

        ItemEntity itemEntity = event.getItem();
        ItemStack stack = itemEntity.getItem();

        if (event.getResult() == Event.Result.DENY) return;

        int originalCount = stack.getCount();
        ItemStack remaining = insertItemToAccessibleSlots(player.getInventory(), stack, accessibleSlots);
        int pickedUpCount = originalCount - remaining.getCount();

        if (pickedUpCount > 0) {
            serverPlayer.take(itemEntity, pickedUpCount);
            itemEntity.setItem(remaining);
            if (remaining.isEmpty()) itemEntity.discard();
        }

        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        ServerPlayer player = (ServerPlayer) event.player;

        int accessibleSlots = getAccessibleSlotCount(player);
        int previousSlots = lastAccessibleSlots.getOrDefault(player.getUUID(), accessibleSlots);

        if (accessibleSlots != previousSlots) {
            if (ServerConfig.SHOW_NOTIFICATIONS.get()) {
                sendChangeNotification(player, previousSlots, accessibleSlots);
            }

            if (accessibleSlots < previousSlots) {
                for (int i = 0; i < 36; i++) {
                    if (getOrderIndexForSlot(i) >= accessibleSlots && getOrderIndexForSlot(i) < previousSlots) {
                        dropItemFromSlot(player, i);
                    }
                }
            }
        }

        lastAccessibleSlots.put(player.getUUID(), accessibleSlots);
        ModPackets.sendTo(new ClientLockData(accessibleSlots), player);
    }

    private void sendChangeNotification(ServerPlayer player, int oldSlots, int newSlots) {
        boolean isExpansion = newSlots > oldSlots;
        String titleKey = isExpansion ? "inventorypurse.notify.expanded.title" : "inventorypurse.notify.reduced.title";
        String messageKey = isExpansion ? "inventorypurse.notify.expanded.message" : "inventorypurse.notify.reduced.message";

        if (ModList.get().isLoaded("modernnotification")) {
            try {
                NotificationCategory category = isExpansion ? NotificationCategory.SUCCESS : NotificationCategory.WARNING;
                String originalMessage = Component.translatable(messageKey, oldSlots, newSlots).getString();

                String modernMessage = originalMessage.replaceFirst("\\(", "&u(");

                ModernNotificationAPI.sendLeftNotification(player, category, modernMessage);
            } catch (Throwable e) {
                e.printStackTrace();
                sendChatNotification(player, titleKey, messageKey, oldSlots, newSlots);
            }
        } else {
            sendChatNotification(player, titleKey, messageKey, oldSlots, newSlots);
        }
    }

    private void sendChatNotification(ServerPlayer player, String titleKey, String messageKey, int oldSlots, int newSlots) {
        player.sendSystemMessage(Component.translatable(titleKey).append(": ").append(Component.translatable(messageKey, oldSlots, newSlots)));
    }

    private void dropItemFromSlot(ServerPlayer player, int slotIndex) {
        ItemStack stack = player.getInventory().getItem(slotIndex);
        if (!stack.isEmpty()) {
            ItemStack toDrop = player.getInventory().removeItem(slotIndex, stack.getCount());
            player.drop(toDrop, false, false);
        }
    }

    private ItemStack insertItemToAccessibleSlots(Inventory inventory, ItemStack stack, int accessibleSlots) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

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

    public static int getOrderIndexForSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex <= 8) return slotIndex;
        if (slotIndex >= 9 && slotIndex <= 35) {
            int row = (slotIndex - 9) / 9;
            int col = (slotIndex - 9) % 9;
            return 9 + ((2 - row) * 9) + col;
        }
        return -1;
    }
}