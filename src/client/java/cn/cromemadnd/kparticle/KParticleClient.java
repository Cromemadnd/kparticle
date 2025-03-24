package cn.cromemadnd.kparticle;

import cn.cromemadnd.kparticle.networking.KParticlePayload;
import cn.cromemadnd.kparticle.networking.KSyncPayload;
import cn.cromemadnd.kparticle.particle.KParticleEffect;
import cn.cromemadnd.kparticle.particle.K_Particle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.getKparticleType;

public class KParticleClient implements ClientModInitializer {
	public static NbtCompound particle_storage = new NbtCompound();

	@Override
	public void onInitializeClient() {
		ParticleFactoryRegistry.getInstance().register(getKparticleType("kparticle"), K_Particle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(getKparticleType("kparticle2"), K_Particle.Factory::new);
		PayloadTypeRegistry.playS2C().register(KParticlePayload.ID, KParticlePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(KSyncPayload.ID, KSyncPayload.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(KParticlePayload.ID, (payload, context) -> {
			Vec3d pos = payload.getPos();
			int count = payload.getCount();

			MinecraftClient client = context.client();
			try {
				for (int i = 0; i < count; i++) {
					KParticleEffect params = new KParticleEffect(payload.getParams(), (double) i / count, "kparticle2");
					if (client.world != null) {
						client.world.addParticle(params, pos.x, pos.y, pos.z, 0, 0, 0);
						//System.out.println("added");
					}
				}
			} catch (Exception e) {
				if (client.player != null) {
					client.player.sendMessage(Text.literal(
							"%s:\n%s".formatted(e.getMessage(), e.getCause().getMessage()))
						, false);
				}
				throw e;
			}

			/*
            try (MinecraftClient client = context.client()) {
                try {
                    for (int i = 0; i < count; i++) {
                        KParticleEffect params = new KParticleEffect(payload.getParams(), (double) i / count);
                        if (client.world != null) {
                            client.world.addParticle(params, pos.x, pos.y, pos.z, 0, 0, 0);
                        }
                    }
                }
				catch (Exception e) {
					System.out.println(e.getMessage());
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("sb"), true);
                    }
                }
            }*/
		});

		ClientPlayNetworking.registerGlobalReceiver(KSyncPayload.ID, (payload, context) -> {
			KParticleClient.particle_storage = payload.getStorage();
		});
	}
}