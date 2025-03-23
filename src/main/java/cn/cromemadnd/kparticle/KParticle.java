package cn.cromemadnd.kparticle;

import cn.cromemadnd.kparticle.command.KParticleCommand;
import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.KPARTICLE_TYPE;

public class KParticle implements ModInitializer {
    public static final String MOD_ID = "kparticle";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        KParticleCommand.register();
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, "kparticle"), KPARTICLE_TYPE);

        //LOGGER.info("Hello Fabric world!");
    }
}