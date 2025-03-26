package cn.cromemadnd.kparticle.command;

import cn.cromemadnd.kparticle.networking.KSyncPayload;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class KSyncCommand {
    private static final Identifier KSTORAGE = Identifier.of("kparticle");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("ksync")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("players", EntityArgumentType.players())
                .executes(context -> {
                    int i = 0;
                    for (ServerPlayerEntity player : EntityArgumentType.getPlayers(context, "players")) {
                        ServerPlayNetworking.send(player, new KSyncPayload(context.getSource().getServer().getDataCommandStorage().get(KSTORAGE)));
                        i++;
                    }
                    int finalI = i;
                    context.getSource().sendFeedback(() -> Text.literal("Particle data synced to %d players".formatted(finalI)), true);
                    return i;
                }))
        ));
    }
}
