package cn.cromemadnd.kparticle.core;

import java.util.ArrayList;
import java.util.List;

public class KConfig {
    private final ArrayList<String> particleIds = new ArrayList<>(List.of("kparticle"));
    //private final int maxParticleCount = 32768;

    public KConfig() {
    }

    public ArrayList<String> getParticleIds() {
        return this.particleIds;
    }

    /*
    public int getMaxParticleCount() {
        return this.maxParticleCount;
    }
     */
}