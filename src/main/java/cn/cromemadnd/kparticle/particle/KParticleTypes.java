package cn.cromemadnd.kparticle.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleType;

import java.util.HashMap;

public class KParticleTypes {
    private static HashMap<String, ParticleType<KParticleEffect>> particleTypeMap = new HashMap<String, ParticleType<KParticleEffect>>();

    public static ParticleType<KParticleEffect> getKparticleType(String id) {
        if (particleTypeMap.containsKey(id)) {
            return particleTypeMap.get(id);
        } else {
            ParticleType<KParticleEffect> new_type = FabricParticleTypes.complex(
                true,
                KParticleEffect.CODEC,
                KParticleEffect.PACKET_CODEC
            );
            particleTypeMap.put(id, new_type);
            return new_type;
        }
    }
}
