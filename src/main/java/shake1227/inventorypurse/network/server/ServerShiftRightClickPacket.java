package shake1227.inventorypurse.network.server;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import shake1227.inventorypurse.core.event.PlayerEventHandler;

import java.util.function.Supplier;

/**
 * Client -> Server
 * Player Shift+Right-Clicked on a slot. The server should handle placing one item.
 */
public class ServerShiftRightClickPacket {

    private final int slotId;

    public ServerShiftRightClickPacket(int slotId) {
        this.slotId = slotId;
    }

    public ServerShiftRightClickPacket(FriendlyByteBuf buf) {
        this.slotId = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(slotId);
    }

    public static void handle(ServerShiftRightClickPacket message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null) {
                // サーバ側のイベントハンドラに処理を委譲
                PlayerEventHandler.handleShiftRightClick(player, message.slotId);
            }
        });
        context.get().setPacketHandled(true);
    }
}