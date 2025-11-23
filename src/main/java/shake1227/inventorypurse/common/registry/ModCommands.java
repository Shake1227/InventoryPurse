package shake1227.inventorypurse.common.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import shake1227.inventorypurse.core.event.PlayerEventHandler;

import java.util.UUID;

public class ModCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("inventorypurse")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("bypass")
                                .then(Commands.argument("action", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("add");
                                            builder.suggest("remove");
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String action = StringArgumentType.getString(context, "action");
                                            UUID uuid = player.getUUID();
                                            if ("add".equalsIgnoreCase(action)) {
                                                PlayerEventHandler.addBypassPlayer(uuid);
                                                context.getSource().sendSuccess(() -> Component.literal("Bypass enabled."), true);
                                            } else if ("remove".equalsIgnoreCase(action)) {
                                                PlayerEventHandler.removeBypassPlayer(uuid);
                                                context.getSource().sendSuccess(() -> Component.literal("Bypass disabled."), true);
                                            }
                                            // PlayerTickイベントで状態が更新されるため、ここでの即時更新は不要
                                            return 1;
                                        })
                                )
                        )
        );
    }
}