package cn.cromemadnd.kparticle.particle;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.dynamic.Codecs;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.getKparticleType;

public class KParticleEffect implements ParticleEffect {
    public static final MapCodec<KParticleEffect> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codecs.NON_EMPTY_STRING.fieldOf("attributes").forGetter((particle) -> {
            return particle.attributes.toString();
        }), Codecs.NON_NEGATIVE_FLOAT.fieldOf("p").forGetter((particle) -> {
            return (float) particle.p;
        }), Codecs.NON_NEGATIVE_INT.fieldOf("n").forGetter((particle) -> {
            return particle.n;
        }), Codecs.NON_EMPTY_STRING.fieldOf("id").forGetter((particle) -> {
            return particle.id;
        })).apply(instance, KParticleEffect::new);
    });
    public static final PacketCodec<RegistryByteBuf, KParticleEffect> PACKET_CODEC;


    static {
        PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.NBT_COMPOUND,
            (particle) -> particle.attributes,
            PacketCodecs.DOUBLE,
            (particle) -> particle.p,
            PacketCodecs.INTEGER,
            (particle) -> particle.n,
            PacketCodecs.STRING,
            (particle) -> particle.id,
            KParticleEffect::new);
    }

    public final NbtCompound attributes/*, storage*/;
    public final double p;
    public final int n;
    public final String id;

    public KParticleEffect(NbtCompound nbt, double p, int n, String id) {
        this.attributes = nbt;
        this.p = p;
        this.id = id;
        this.n = n;
    }

    public KParticleEffect(String nbt, double p, int n, String id) {
        NbtCompound _attributes;//, _storage;
        try {
            _attributes = StringNbtReader.parse(nbt);
        } catch (CommandSyntaxException e) {
            _attributes = new NbtCompound();
        }
        this.attributes = _attributes;
        this.p = p;
        this.id = id;
        this.n = n;
    }

    public ParticleType<KParticleEffect> getType() {
        return getKparticleType(this.id);
    }
}
