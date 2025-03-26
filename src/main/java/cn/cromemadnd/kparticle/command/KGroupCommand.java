package cn.cromemadnd.kparticle.command;

import cn.cromemadnd.kparticle.networking.KGroupOperations;
import cn.cromemadnd.kparticle.networking.KGroupPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;


public class KGroupCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("kgroup")
            .requires(source -> source.hasPermissionLevel(2))
            .then(CommandManager.argument("group", StringArgumentType.word())
                .then(CommandManager.literal("clear")
                    .then(CommandManager.argument("players", EntityArgumentType.players())
                        .executes(context -> execute(context.getSource(),
                            KGroupOperations.CLEAR,
                            StringArgumentType.getString(context, "group"),
                            new NbtCompound(),
                            EntityArgumentType.getPlayers(context, "players")
                        ))
                    )
                )
                .then(CommandManager.literal("merge")
                    .then(CommandManager.argument("params", NbtCompoundArgumentType.nbtCompound())
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                            .executes(context -> execute(context.getSource(),
                                KGroupOperations.MERGE,
                                StringArgumentType.getString(context, "group"),
                                NbtCompoundArgumentType.getNbtCompound(context, "params"),
                                EntityArgumentType.getPlayers(context, "players")
                            ))
                        )
                    )
                )
                .then(CommandManager.literal("set")
                    .then(CommandManager.argument("params", NbtCompoundArgumentType.nbtCompound())
                        .then(CommandManager.argument("players", EntityArgumentType.players())
                            .executes(context -> execute(context.getSource(),
                                KGroupOperations.SET,
                                StringArgumentType.getString(context, "group"),
                                NbtCompoundArgumentType.getNbtCompound(context, "params"),
                                EntityArgumentType.getPlayers(context, "players")
                            ))
                        )
                    )
                )
            )
        ));
    }

    private static int execute(ServerCommandSource source, int operation, String group, NbtCompound data, Collection<ServerPlayerEntity> players){
        int i = 0;
        for (ServerPlayerEntity player : players) {
            ServerPlayNetworking.send(player, new KGroupPayload(group, operation, data));
            i++;
        }
        int finalI = i;
        source.sendFeedback(() -> Text.literal("Group packet sent to %d players".formatted(finalI)), true);
        return i;
    }
}