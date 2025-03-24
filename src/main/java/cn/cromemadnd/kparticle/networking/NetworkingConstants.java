package cn.cromemadnd.kparticle.networking;

import net.minecraft.util.Identifier;


public class NetworkingConstants {
    public static final Identifier KPARTICLE_PACKET = Identifier.of("kparticle", "spawn_particle");
    public static final Identifier KSYNC_PACKET = Identifier.of("kparticle", "sync_storage");
}