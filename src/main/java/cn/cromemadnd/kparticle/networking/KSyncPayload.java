package cn.cromemadnd.kparticle.networking;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record KSyncPayload(NbtCompound storage) implements CustomPayload {
    public static final Id<KSyncPayload> ID = new CustomPayload.Id<>(NetworkingConstants.KSYNC_PACKET);
    public static final PacketCodec<PacketByteBuf, KSyncPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.NBT_COMPOUND,
        KSyncPayload::storage,
        KSyncPayload::new
    );

    public NbtCompound getStorage() {
        return this.storage;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}