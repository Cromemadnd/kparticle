package cn.cromemadnd.kparticle.command;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.Command;

import static com.yourmod.core.network.NetworkingConstants.PARTICLE_PACKET_ID;


public class KParticleCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("kparticle").then(CommandManager.argument("params", StringArgumentType.greedyString()).executes(context -> {
                String json = StringArgumentType.getString(context, "params");
                ServerPlayerEntity player = context.getSource().getPlayer();
                // 发送 JSON 数据到客户端
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(json);
                ServerPlayNetworking.send(player, NetworkingConstants.PARTICLE_PACKET_ID, buf);
                return 1;
            })));
        });
    }
}
