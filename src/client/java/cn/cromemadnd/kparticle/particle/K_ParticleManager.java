package cn.cromemadnd.kparticle.particle;

import cn.cromemadnd.kparticle.core.KParticleStorage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class K_ParticleManager {
    private static final Map<String, String> SINGLE_ATTRIBUTES = Map.of(
        "alpha", "a",
        "brightness", "l",
        "scale", "s",
        "frame", "f",
        "lifetime", "lt",
        "timescale", "ts",
        "age", "ag"
    );
    private static final Pattern DYNAMIC_ARGS_PATTERN = Pattern.compile("\\{(\\w+)}");
    public static final WeakHashMap<K_ParticleManager, String> ID_MAP = new WeakHashMap<>();

    public final List<String> variables = new ArrayList<>();
    private final WeakHashMap<K_Particle, Boolean> particles = new WeakHashMap<>();
    final ConcurrentHashMap<String, String> attributeMap = new ConcurrentHashMap<>();
    private final String id;

    public static ConcurrentHashMap<String, String> toAttributeMap(NbtCompound params) {
        /*
            将入参NBT标签转换为属性名-表达式的Map，同时设置hsv、immortal属性。
            实际上是一个“扁平化”的过程。
         */

        // 将NBT扁平化为字典
        final ConcurrentHashMap<String, String> attributeMap = new ConcurrentHashMap<>();

        if (params.contains("hsv")) {
            attributeMap.put("hsv", params.getBoolean("hsv") ? "1" : "");
        }

        if (params.contains("pos", NbtElement.LIST_TYPE)) {
            NbtList pos = params.getList("pos", NbtElement.STRING_TYPE);
            attributeMap.put("x", pos.getString(0));
            attributeMap.put("y", pos.getString(1));
            attributeMap.put("z", pos.getString(2));
        }
        if (params.contains("color", NbtElement.LIST_TYPE)) {
            NbtList color = params.getList("color", NbtElement.STRING_TYPE);
            attributeMap.put("r", color.getString(0));
            attributeMap.put("g", color.getString(1));
            attributeMap.put("b", color.getString(2));
        }
        for (String singleAttribute : SINGLE_ATTRIBUTES.keySet()) {
            if (params.contains(singleAttribute, NbtElement.STRING_TYPE)) {
                attributeMap.put(SINGLE_ATTRIBUTES.get(singleAttribute),
                    params.getString(singleAttribute)
                );
            }
        }
        return attributeMap;
    }

    public static K_ParticleManager getManager(String id) {
        for (K_ParticleManager instance : ID_MAP.keySet()) {
            if (ID_MAP.get(instance).equals(id)){
                return instance;
            }
        }
        return null;
    }

    public K_ParticleManager(String id, NbtCompound payloadParams){
        this.id = id;
        merge(toAttributeMap(payloadParams));
        ID_MAP.put(this, id);

        KParticleStorage.getGroup(
            payloadParams.contains("group", NbtElement.STRING_TYPE) ? payloadParams.getString("group") : "default"
        ).add(this);
    }

    public void merge(Map<String, String> attributeMap) {
        attributeMap.forEach((key, value) -> {
            String rawExpression = value.replaceAll("\\{prev}", // 此处用之前存入的表达式替换{prev}
                this.attributeMap.getOrDefault(key, "")
            );
            this.attributeMap.put(key, rawExpression);

            Matcher matcher = DYNAMIC_ARGS_PATTERN.matcher(rawExpression);
            while (matcher.find()) {
                String variable_found = matcher.group(1);
                this.variables.add(variable_found);
            }
        });
        this.particles.keySet().forEach(K_Particle::mergeExpressions);
    }

    public void set(Map<String, String> attributeMap) {
        this.variables.clear();
        this.attributeMap.clear();
        merge(attributeMap);
        this.particles.keySet().forEach(K_Particle::clearExpressions);
    }

    public void add(K_Particle particle){
        this.particles.put(particle, true);
    }

    public void clear() {
        this.particles.keySet().forEach(K_Particle::markDead);
    }

    @Override
    public String toString() {
        return this.id;
    }
}
