package cn.cromemadnd.kparticle.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static cn.cromemadnd.kparticle.KParticle.LOGGER;
import static cn.cromemadnd.kparticle.KParticle.MOD_ID;

public class KConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = Paths.get("config", "%s.json".formatted(MOD_ID));

    public KConfig loadConfig() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                KConfig defaultConfig = new KConfig();
                String json = GSON.toJson(defaultConfig);
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.write(CONFIG_PATH, json.getBytes());
                return defaultConfig;
            }

            return GSON.fromJson(Arrays.toString(Files.readAllBytes(CONFIG_PATH)), KConfig.class);
        } catch (Exception e) {
            LOGGER.warn(Arrays.toString(e.getStackTrace()));
            return new KConfig();
        }
    }
}