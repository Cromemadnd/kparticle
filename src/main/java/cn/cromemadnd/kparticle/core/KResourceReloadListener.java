package cn.cromemadnd.kparticle.core;

import cn.cromemadnd.kparticle.particle.KParticleTypes;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

import static cn.cromemadnd.kparticle.KParticle.MOD_ID;

@Deprecated
public class KResourceReloadListener implements SimpleSynchronousResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return Identifier.of(MOD_ID, "my_resources");
    }

    private final Consumer<String> registerOperation;
    public KResourceReloadListener(Consumer<String> registerOperation){
        this.registerOperation = registerOperation;
    }

    @Override
    public void reload(ResourceManager manager) {
        for (Identifier entry : manager.findResources("particles", identifier -> identifier.getNamespace().equals(MOD_ID)).keySet()) {
            String path = entry.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf('.'));

            if (KParticleTypes.isNotRegistered(name)) {
                registerOperation.accept(name);
            }
        }
    }
}
