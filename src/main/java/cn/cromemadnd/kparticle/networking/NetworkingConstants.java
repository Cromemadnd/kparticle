package cn.cromemadnd.kparticle.networking;

import net.minecraft.util.Identifier;
import static cn.cromemadnd.kparticle.KParticle.MOD_ID;

public class NetworkingConstants {
    public static final Identifier KPARTICLE_PACKET = Identifier.of(MOD_ID, "spawn_particle");
    public static final Identifier KSYNC_PACKET = Identifier.of(MOD_ID, "sync_storage");
    public static final Identifier KGROUP_PACKET = Identifier.of(MOD_ID, "group_operation");
}