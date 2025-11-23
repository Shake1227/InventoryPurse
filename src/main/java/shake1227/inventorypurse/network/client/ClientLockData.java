package shake1227.inventorypurse.network.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ClientLockData {
    public static int clientAccessibleSlots = 36;
    private final int accessibleSlots;

    public ClientLockData(int accessibleSlots) {
        this.accessibleSlots = accessibleSlots;
    }

    public ClientLockData(FriendlyByteBuf buf) {
        this.accessibleSlots = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.accessibleSlots);
    }

    public static void handle(ClientLockData message, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                clientAccessibleSlots = message.accessibleSlots;
            });
        });
        context.get().setPacketHandled(true);
    }
}