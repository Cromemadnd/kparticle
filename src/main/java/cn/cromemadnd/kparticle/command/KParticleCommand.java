package cn.cromemadnd.kparticle.command;

import cn.cromemadnd.kparticle.networking.KParticlePayload;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;

public class KParticleCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("kparticle")
                .then(CommandManager.argument("pos", Vec3ArgumentType.vec3())
                    .then(CommandManager.argument("params", NbtCompoundArgumentType.nbtCompound())
                        .then(CommandManager.argument("viewers", EntityArgumentType.players())
                            .then(CommandManager.argument("count", IntegerArgumentType.integer())
                                .executes(context -> {
                                    int i = 0;
                                    for (ServerPlayerEntity viewer : EntityArgumentType.getPlayers(context, "viewers")) {
                                        ServerPlayNetworking.send(viewer, new KParticlePayload(
                                            Vec3ArgumentType.getVec3(context, "pos"),
                                            NbtCompoundArgumentType.getNbtCompound(context, "params"),
                                            IntegerArgumentType.getInteger(context, "count")
                                        ));
                                        i++;
                                    }
                                    return i;
                                })))))
            );
        });
    }
}