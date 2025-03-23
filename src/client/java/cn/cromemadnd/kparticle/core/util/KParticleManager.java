package cn.cromemadnd.kparticle.core.util;

import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;

public class KParticleManager extends ParticleManager {
    private static final int MAX_PARTICLE_COUNT = 163840;

    public KParticleManager(ClientWorld world, TextureManager textureManager) {
        super(world, textureManager);
    }
}
