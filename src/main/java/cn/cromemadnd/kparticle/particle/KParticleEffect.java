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

import static cn.cromemadnd.kparticle.particle.KParticleTypes.KPARTICLE_TYPE;

public class KParticleEffect implements ParticleEffect {
    public static final MapCodec<KParticleEffect> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(Codecs.NON_EMPTY_STRING.fieldOf("attributes").forGetter((particle) -> {
            return particle.attributes.toString();
        }), Codecs.NON_NEGATIVE_FLOAT.fieldOf("n").forGetter((particle) -> {
            return (float) particle.p;
        })).apply(instance, KParticleEffect::new);
    });
    public static final PacketCodec<RegistryByteBuf, KParticleEffect> PACKET_CODEC;

    static {
        PACKET_CODEC = PacketCodec.tuple(PacketCodecs.NBT_COMPOUND, (particle) -> {
            return particle.attributes;
        }, PacketCodecs.DOUBLE, (particle) -> {
            return particle.p;
        }, KParticleEffect::new);
    }

    public final NbtCompound attributes;
    public final double p;

    public KParticleEffect(NbtCompound nbt, double p) {
        this.attributes = nbt;
        this.p = p;
    }

    public KParticleEffect(String nbt, double p) {
        NbtCompound _attributes;
        //this.attributes = nbt;
        try {
            _attributes = StringNbtReader.parse(nbt);
            //System.out.println(_attributes);
        } catch (CommandSyntaxException e) {
            _attributes = new NbtCompound();
            System.out.println(e);
        }
        this.attributes = _attributes;
        this.p = p;
        //System.out.println(this.attributes);
    }

    public ParticleType<KParticleEffect> getType() {
        return KPARTICLE_TYPE;
    }
}
