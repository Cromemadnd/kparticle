package cn.cromemadnd.kparticle;

import cn.cromemadnd.kparticle.networking.KParticlePayload;
import cn.cromemadnd.kparticle.particle.KParticleEffect;
import cn.cromemadnd.kparticle.particle.K_Particle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.KPARTICLE_TYPE;

public class KParticleClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ParticleFactoryRegistry.getInstance().register(KPARTICLE_TYPE, K_Particle.Factory::new);
		PayloadTypeRegistry.playS2C().register(KParticlePayload.ID, KParticlePayload.CODEC);

		ClientPlayNetworking.registerGlobalReceiver(KParticlePayload.ID, (payload, context) -> {
			Vec3d pos = payload.getPos();
			int count = payload.getCount();
			//System.out.println(count);

			MinecraftClient client = context.client();
			for (int i = 0; i < count; i++) {
				KParticleEffect params = new KParticleEffect(payload.getParams(), (double) i / count);
				client.world.addParticle(params, pos.x, pos.y, pos.z, 0, 0, 0);
			}
		});
	}
}