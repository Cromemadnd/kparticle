package cn.cromemadnd.kparticle.core;

import cn.cromemadnd.kparticle.particle.K_Particle;
import net.minecraft.nbt.NbtCompound;

import java.util.WeakHashMap;

public class KParticleGroup {
    private final WeakHashMap<K_Particle, Boolean> particles = new WeakHashMap<>();

    public void add(K_Particle particle) {
        this.particles.put(particle, true);
    }

    public void empty() {
        for (K_Particle particle : this.particles.keySet()) {
            particle.markDead();
        }
    }

    /*
    public ArrayList<K_Particle> get(){
        return new ArrayList<>(this.particles.keySet());
    }
    */

    public void merge(NbtCompound params) {
        for (K_Particle particle : this.particles.keySet()) {
            particle.mergeExpressions(K_Particle.toAttributeMap(params));
        }
    }

    public void set(NbtCompound params) {
        for (K_Particle particle : this.particles.keySet()) {
            particle.setExpressions(K_Particle.toAttributeMap(params));
        }
    }
}
