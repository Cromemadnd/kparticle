package cn.cromemadnd.kparticle.particle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.dynamic.Codecs;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.getKparticleType;

public class KParticleEffect implements ParticleEffect {
    public static final MapCodec<KParticleEffect> CODEC = RecordCodecBuilder.mapCodec(
        (instance) -> instance.group(
            Codecs.NON_EMPTY_STRING.fieldOf("pid").forGetter((particle) -> particle.particleId),
            Codecs.NON_EMPTY_STRING.fieldOf("mid").forGetter((particle) -> particle.managerId),
            Codecs.NON_NEGATIVE_FLOAT.fieldOf("p").forGetter((particle) -> (float) particle.p),
            Codecs.NON_NEGATIVE_INT.fieldOf("n").forGetter((particle) -> particle.n)
        ).apply(instance, KParticleEffect::new)
    );
    public static final PacketCodec<RegistryByteBuf, KParticleEffect> PACKET_CODEC;


    static {
        PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            (particle) -> particle.particleId,
            PacketCodecs.STRING,
            (particle) -> particle.managerId,
            PacketCodecs.DOUBLE,
            (particle) -> particle.p,
            PacketCodecs.INTEGER,
            (particle) -> particle.n,
            KParticleEffect::new);
    }

    public final double p;
    public final int n;
    public final String particleId, managerId;

    public KParticleEffect(String particleId, String managerId, double p, int n) {
        this.p = p;
        this.n = n;
        this.managerId = managerId;
        this.particleId = particleId;
    }

    public ParticleType<KParticleEffect> getType() {
        return getKparticleType(this.particleId);
    }
}
