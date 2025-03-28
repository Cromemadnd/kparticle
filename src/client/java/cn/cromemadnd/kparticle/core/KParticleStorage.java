package cn.cromemadnd.kparticle.core;

import net.minecraft.nbt.NbtCompound;

import java.util.concurrent.ConcurrentHashMap;

public class KParticleStorage {
    private static final ConcurrentHashMap<String, KParticleGroup> kParticleGroupMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Double> particleData = new ConcurrentHashMap<>();

    public static double getParticleData(String key) {
        return particleData.getOrDefault(key, 0.0d);
    }

    public static void setParticleData(NbtCompound data) {
        data.getKeys().forEach((key) -> particleData.put(key, data.getDouble(key)));
    }

    public static void clearParticleData(){
        particleData.clear();
    }

    public static KParticleGroup getGroup(String id) {
        if (kParticleGroupMap.containsKey(id)) {
            return kParticleGroupMap.get(id);
        } else {
            KParticleGroup newGroup = new KParticleGroup();
            kParticleGroupMap.put(id, newGroup);
            return newGroup;
        }
    }

    public static void clearGroup(){
        kParticleGroupMap.clear();
    }
}
