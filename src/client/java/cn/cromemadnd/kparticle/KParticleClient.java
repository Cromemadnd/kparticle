package cn.cromemadnd.kparticle;

import cn.cromemadnd.kparticle.core.KConfig;
import cn.cromemadnd.kparticle.core.KConfigManager;
import cn.cromemadnd.kparticle.core.KParticleGroup;
import cn.cromemadnd.kparticle.core.KParticleStorage;
import cn.cromemadnd.kparticle.networking.KGroupOperations;
import cn.cromemadnd.kparticle.networking.KGroupPayload;
import cn.cromemadnd.kparticle.networking.KParticlePayload;
import cn.cromemadnd.kparticle.networking.KSyncPayload;
import cn.cromemadnd.kparticle.particle.KParticleEffect;
import cn.cromemadnd.kparticle.particle.KParticleTypes;
import cn.cromemadnd.kparticle.particle.K_Particle;
import cn.cromemadnd.kparticle.particle.K_ParticleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.UUID;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.getKparticleType;


public class KParticleClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 进行数据包接收器的注册
        ClientPlayNetworking.registerGlobalReceiver(KParticlePayload.ID, (payload, context) -> {
            Vec3d pos = payload.getPos();
            int count = payload.getCount();
            NbtCompound payloadParams = payload.getParams();
            String particleId = payloadParams.getString("id").isEmpty() ? "kparticle" : payloadParams.getString("id");

            MinecraftClient client = context.client();
            if (KParticleTypes.isNotRegistered(particleId)) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Unregistered particle id %s".formatted(particleId)), false);
                }
                return;
            }

            try {
                String groupId = UUID.randomUUID().toString();
                new K_ParticleManager(groupId, payloadParams);

                for (int i = 0; i < count; i++) {
                    KParticleEffect params = new KParticleEffect(particleId, groupId, (double) i / count, i);

                    if (client.world != null) {
                        client.world.addParticle(params, true, true, pos.x, pos.y, pos.z, 0, 0, 0);
                    }
                }
            } catch (Exception e) {
                assert client.player != null;
                client.player.sendMessage(Text.literal(
                        "%s:\n%s".formatted(e.getMessage(), e.getCause().getMessage()))
                    , false);
                //throw e;
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(KSyncPayload.ID, (payload, context) -> KParticleStorage.setParticleData(payload.getStorage()));
        ClientPlayNetworking.registerGlobalReceiver(KGroupPayload.ID, (payload, context) -> {
            int operation = payload.getOperation();
            KParticleGroup group = KParticleStorage.getGroup(payload.getGroup());

            ClientPlayerEntity player = context.client().player;
            switch (operation) {
                case KGroupOperations.CLEAR: {
                    group.empty();
                }
                break;
                case KGroupOperations.SET: {
                    group.set(payload.getData());
                }
                break;
                case KGroupOperations.MERGE: {
                    group.merge(payload.getData());
                }
                break;
                case KGroupOperations.GET: {
                    assert player != null;
                    player.sendMessage(Text.literal(group.get().toString()), false);
                }
                break;
                default: {
                    assert player != null;
                    player.sendMessage(Text.literal("Unknown operation %d".formatted(payload.getOperation())), false);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            KParticleStorage.clearParticleData();
            KParticleStorage.clearGroup();
        });

        // 读取配置文件，进行粒子工厂注册
        KConfig config = new KConfigManager().loadConfig();
        for (String particleId : config.getParticleIds()) {
            ParticleFactoryRegistry.getInstance().register(getKparticleType(particleId), K_Particle.Factory::new);
        }
    }
}
