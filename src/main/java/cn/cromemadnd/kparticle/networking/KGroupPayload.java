package cn.cromemadnd.kparticle.networking;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;


public record KGroupPayload(String group, int operation, NbtCompound data) implements CustomPayload {
    public static final Id<KGroupPayload> ID = new CustomPayload.Id<>(NetworkingConstants.KGROUP_PACKET);
    public static final PacketCodec<PacketByteBuf, KGroupPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.STRING,
        KGroupPayload::group,
        PacketCodecs.INTEGER,
        KGroupPayload::operation,
        PacketCodecs.NBT_COMPOUND,
        KGroupPayload::data,
        KGroupPayload::new
    );

    public int getOperation(){
        return this.operation;
    }

    public String getGroup(){
        return this.group;
    }

    public NbtCompound getData() {
        return this.data;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}