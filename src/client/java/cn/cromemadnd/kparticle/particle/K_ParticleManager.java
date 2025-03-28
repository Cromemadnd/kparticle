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
        "timescale", "ts"
    );
    private static final Pattern DYNAMIC_ARGS_PATTERN = Pattern.compile("\\{(\\w+)}");
    public static final WeakHashMap<K_ParticleManager, String> ID_MAP = new WeakHashMap<>();

    public final ArrayList<String> variables = new ArrayList<>();
    public final ArrayList<Integer> coords = new ArrayList<>(List.of(0));
    public boolean hsv = false, immortal = false;
    private final WeakHashMap<K_Particle, Boolean> particles = new WeakHashMap<>();
    final ConcurrentHashMap<String, String> attributeMap = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, Double> constAttributeMap = new ConcurrentHashMap<>();
    private final String id;

    public static K_ParticleManager getManager(String id) {
        for (K_ParticleManager instance : ID_MAP.keySet()) {
            if (ID_MAP.get(instance).equals(id)) {
                return instance;
            }
        }
        return null;
    }

    public K_ParticleManager(String id, NbtCompound payloadParams) {
        this.id = id;
        merge(payloadParams);
        ID_MAP.put(this, id);

        KParticleStorage.getGroup(payloadParams.contains("group", NbtElement.STRING_TYPE) ? payloadParams.getString("group") : "default").add(this);
    }

    public void merge(NbtCompound params) {
        /*
            将入参NBT标签“扁平化”为属性名-表达式的Map
         */
        ConcurrentHashMap<String, String> attributeMap = new ConcurrentHashMap<>();

        if (params.contains("hsv", NbtElement.BYTE_TYPE)) {
            this.hsv = params.getBoolean("hsv");
        }

        if (params.contains("coords", NbtElement.LIST_TYPE)) {
            this.coords.clear();
            NbtList coords = params.getList("coords", NbtElement.INT_TYPE);
            for(int i = 0; i < coords.size(); i++){
                this.coords.addLast(coords.getInt(i));
            }
        }
        else if (params.contains("coord", NbtElement.INT_TYPE)) {
            this.coords.clear();
            this.coords.addFirst(params.getInt("coord"));
        }
        //System.out.println(this.coords);
        //System.out.println(this.coords.size());

        if (params.contains("pos", NbtElement.LIST_TYPE)) {
            NbtList pos = params.getList("pos", NbtElement.STRING_TYPE);
            if (pos.isEmpty()) {
                NbtList pos_ = params.getList("pos", NbtElement.DOUBLE_TYPE);
                for(int i = 0; i < this.coords.size(); i++) {
                    constAttributeMap.put("x%d".formatted(i), pos_.getDouble(3 * i));
                    constAttributeMap.put("y%d".formatted(i), pos_.getDouble(3 * i + 1));
                    constAttributeMap.put("z%d".formatted(i), pos_.getDouble(3 * i + 2));
                }
            } else {
                for(int i = 0; i < this.coords.size(); i++) {
                    constAttributeMap.remove("x%d".formatted(i));
                    constAttributeMap.remove("y%d".formatted(i));
                    constAttributeMap.remove("z%d".formatted(i));
                    attributeMap.put("x%d".formatted(i), pos.getString(3 * i));
                    attributeMap.put("y%d".formatted(i), pos.getString(3 * i + 1));
                    attributeMap.put("z%d".formatted(i), pos.getString(3 * i + 2));
                }
            }
        }

        if (params.contains("color", NbtElement.LIST_TYPE)) {
            NbtList color = params.getList("color", NbtElement.STRING_TYPE);
            if (color.isEmpty()) {
                NbtList color_ = params.getList("color", NbtElement.DOUBLE_TYPE);
                constAttributeMap.put("x", color_.getDouble(0));
                constAttributeMap.put("y", color_.getDouble(1));
                constAttributeMap.put("z", color_.getDouble(2));
            } else {
                constAttributeMap.remove("r");
                constAttributeMap.remove("g");
                constAttributeMap.remove("b");
                attributeMap.put("r", color.getString(0));
                attributeMap.put("g", color.getString(1));
                attributeMap.put("b", color.getString(2));
            }
        }

        for (String singleAttribute : SINGLE_ATTRIBUTES.keySet()) {
            if (params.contains(singleAttribute, NbtElement.STRING_TYPE)) {
                constAttributeMap.remove(singleAttribute);
                attributeMap.put(SINGLE_ATTRIBUTES.get(singleAttribute), params.getString(singleAttribute));
            } else if (params.contains(singleAttribute, NbtElement.DOUBLE_TYPE)) {
                constAttributeMap.put(SINGLE_ATTRIBUTES.get(singleAttribute), params.getDouble(singleAttribute));
            }
        }
        //System.out.println(attributeMap);

        /*
            处理Map，初始化动态变量
         */
        attributeMap.forEach((key, value) -> {
            if (key.equals("lt") && value.equals("inf")) {
                this.immortal = true;
                return;
            }

            String rawExpression = value.replaceAll("\\{prev}", this.attributeMap.getOrDefault(key, ""));
            this.attributeMap.put(key, rawExpression);

            Matcher matcher = DYNAMIC_ARGS_PATTERN.matcher(rawExpression);
            while (matcher.find()) {
                String variable_found = matcher.group(1);
                this.variables.add(variable_found);
            }
        });
        this.particles.keySet().forEach(K_Particle::mergeExpressions);
    }

    public void set(NbtCompound params) {
        this.variables.clear();
        this.attributeMap.clear();
        merge(params);
        this.particles.keySet().forEach(K_Particle::clearExpressions);
    }

    public void add(K_Particle particle) {
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
