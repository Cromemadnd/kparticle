package cn.cromemadnd.kparticle.networking;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.Vec3d;

public record KParticlePayload(Vec3d pos, NbtCompound params, int count) implements CustomPayload {
    public static final Id<KParticlePayload> ID = new CustomPayload.Id<>(NetworkingConstants.KPARTICLE_PACKET);
    public static final PacketCodec<PacketByteBuf, KParticlePayload> CODEC = PacketCodec.tuple(
        Vec3d.PACKET_CODEC,
        KParticlePayload::pos,
        PacketCodecs.NBT_COMPOUND,
        KParticlePayload::params,
        PacketCodecs.INTEGER,
        KParticlePayload::count,
        KParticlePayload::new
    );

    public Vec3d getPos() {
        return this.pos;
    }

    public NbtCompound getParams() {
        return this.params;
    }

    public int getCount() {
        return this.count;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}