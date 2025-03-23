package cn.cromemadnd.kparticle.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleType;

public class KParticleTypes {
    public static final ParticleType<KParticleEffect> KPARTICLE_TYPE = FabricParticleTypes.complex(
        true,
        KParticleEffect.CODEC,
        KParticleEffect.PACKET_CODEC
    );
}
