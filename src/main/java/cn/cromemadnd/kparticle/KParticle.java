package cn.cromemadnd.kparticle;

import cn.cromemadnd.kparticle.command.KGroupCommand;
import cn.cromemadnd.kparticle.command.KParticleCommand;
import cn.cromemadnd.kparticle.command.KSyncCommand;
import cn.cromemadnd.kparticle.core.KConfig;
import cn.cromemadnd.kparticle.core.KConfigManager;
import cn.cromemadnd.kparticle.networking.KGroupPayload;
import cn.cromemadnd.kparticle.networking.KParticlePayload;
import cn.cromemadnd.kparticle.networking.KSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static cn.cromemadnd.kparticle.particle.KParticleTypes.getKparticleType;

public class KParticle implements ModInitializer {
    public static final String MOD_ID = "kparticle";
    public static final Logger LOGGER = LoggerFactory.getLogger("KParticle");

    @Override
    public void onInitialize() {
        KParticleCommand.register();
        KSyncCommand.register();
        KGroupCommand.register();
        PayloadTypeRegistry.playS2C().register(KParticlePayload.ID, KParticlePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KSyncPayload.ID, KSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(KGroupPayload.ID, KGroupPayload.CODEC);

        // 读取配置文件，进行粒子类型注册
        KConfig config = new KConfigManager().loadConfig();
        for (String particleId : config.getParticleIds()) {
            Registry.register(Registries.PARTICLE_TYPE, Identifier.of(MOD_ID, particleId), getKparticleType(particleId));
        }
    }
}
