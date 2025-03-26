package cn.cromemadnd.kparticle.core;

import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;

public class KParticleStorage {
    private static final Map<String, KParticleGroup> kParticleGroupMap = new HashMap<>();
    private static NbtCompound particleData = new NbtCompound();

    public static NbtCompound getParticleData() {
        return particleData;
    }

    public static void setParticleData(NbtCompound data) {
        if (data != null) {
            particleData = data;
        }
    }
    public static void clearParticleData(){
        particleData = new NbtCompound();
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
