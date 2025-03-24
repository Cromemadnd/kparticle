package cn.cromemadnd.kparticle;

import cn.cromemadnd.kparticle.command.KParticleCommand;
import cn.cromemadnd.kparticle.command.KSyncCommand;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.getKparticleType;

public class KParticle implements ModInitializer {
    public static final String MOD_ID = "kparticle";

    //public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        KParticleCommand.register();
        KSyncCommand.register();
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "kparticle"), getKparticleType("kparticle"));
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "kparticle2"), getKparticleType("kparticle2"));
    }
}