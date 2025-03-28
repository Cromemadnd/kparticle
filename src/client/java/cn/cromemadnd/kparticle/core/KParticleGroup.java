package cn.cromemadnd.kparticle.core;

import cn.cromemadnd.kparticle.particle.K_ParticleManager;
import net.minecraft.nbt.NbtCompound;

import java.util.Set;
import java.util.WeakHashMap;

public class KParticleGroup {
    private final WeakHashMap<K_ParticleManager, Boolean> particleManagers = new WeakHashMap<>();

    public void add(K_ParticleManager particleManager) {
        this.particleManagers.put(particleManager, true);
    }

    public void empty() {
        for (K_ParticleManager particleManager : this.particleManagers.keySet()) {
            particleManager.clear();
        }
    }

    public Set<K_ParticleManager> get(){
        return this.particleManagers.keySet();
    }

    public void merge(NbtCompound params) {
        for (K_ParticleManager particleManager : this.particleManagers.keySet()) {
            particleManager.merge(params);
        }
    }

    public void set(NbtCompound params) {
        for (K_ParticleManager particleManager : this.particleManagers.keySet()) {
            particleManager.set(params);
        }
    }
}
