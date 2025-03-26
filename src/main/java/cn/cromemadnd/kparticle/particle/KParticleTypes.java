package cn.cromemadnd.kparticle.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleType;

import java.util.HashMap;

public class KParticleTypes {
    private static final HashMap<String, ParticleType<KParticleEffect>> particleTypeMap = new HashMap<>();

    public static boolean isNotRegistered(String id){
        return !particleTypeMap.containsKey(id);
    }

    public static ParticleType<KParticleEffect> getKparticleType(String id) {
        //System.out.println("issued getKParticleType(%s)".formatted(id));
        if (particleTypeMap.containsKey(id)) {
            //System.out.println("found %s".formatted(particleTypeMap.get(id)));
            return particleTypeMap.get(id);
        } else {
            ParticleType<KParticleEffect> new_type = FabricParticleTypes.complex(
                true,
                KParticleEffect.CODEC,
                KParticleEffect.PACKET_CODEC
            );
            particleTypeMap.put(id, new_type);
            //System.out.println("created %s".formatted(new_type));
            return new_type;
        }
    }
}
