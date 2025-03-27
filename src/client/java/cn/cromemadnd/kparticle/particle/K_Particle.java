package cn.cromemadnd.kparticle.particle;

import cn.cromemadnd.kparticle.core.KExpressionBuilder;
import cn.cromemadnd.kparticle.core.KMathFuncs;
import cn.cromemadnd.kparticle.core.KParticleStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.objecthunter.exp4j.Expression;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class K_Particle extends SpriteBillboardParticle {
    private final double _x, _y, _z, p;
    private final int n;
    private boolean immortal = false, hsv = false;
    private float tickSpeed = 1.0f, _age = 0.0f, _maxAge = 1.0f;
    private final SpriteProvider spriteProvider;
    private final List<String> variables = new ArrayList<>();
    private final Map<String, Expression> expressionMap = new ConcurrentHashMap<>();
    private final Map<String, String> attributeMap = new ConcurrentHashMap<>();
    private final ExecutorService asyncPool = Executors.newWorkStealingPool();
    private static final Map<String, String> SINGLE_ATTRIBUTES = Map.of(
        "alpha", "a",
        "brightness", "l",
        "scale", "s",
        "frame", "f",
        "lifetime", "lt",
        "timescale", "ts",
        "age", "ag"
    );

    private static class VariableSnapshot {
        final Map<String, Double> values;
        final double t, tp;

        VariableSnapshot(List<String> variables, double age, double maxAge) {
            this.values = new ConcurrentHashMap<>();
            for (String var : variables) {
                this.values.put(var, KParticleStorage.getParticleData().getDouble(var));
            }
            this.t = (age) / 20.0d;
            this.tp = maxAge == 0.0d ? 0.0d : (age) / maxAge;
        }
    }

    private static class AsyncEvaluator {
        final VariableSnapshot snapshot;
        final ExecutorService asyncPool;
        final Map<String, Expression> expressionMap;
        final Map<String, Future<Double>> futureMap = new ConcurrentHashMap<>();

        AsyncEvaluator(VariableSnapshot snapshot, Map<String, Expression> expressionMap, ExecutorService asyncPool) {
            this.snapshot = snapshot;
            this.expressionMap = expressionMap;
            this.asyncPool = asyncPool;
        }

        void submit(String attribute) {
            Expression expression = this.expressionMap.get(attribute);
            if (expression == null) return;

            futureMap.put(attribute, asyncPool.submit(() -> {
                snapshot.values.forEach(expression::setVariable);
                return expression.setVariable("t", snapshot.t)
                    .setVariable("tp", snapshot.tp)
                    .evaluate();
            }));
        }

        double get(String attribute, double defaultValue) {
            try {
                Future<Double> future = futureMap.get(attribute);
                if (future == null) return defaultValue;
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                return 0.0;
            }
        }
    }

    public K_Particle(ClientWorld world, double x, double y, double z, KParticleEffect params, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this._x = x;
        this._y = y;
        this._z = z;
        this.spriteProvider = spriteProvider;
        this.n = params.n;
        this.p = params.p;
        if (spriteProvider == null) {
            this.markDead();
            return;
        }

        mergeExpressions(toAttributeMap(params.attributes));
        tick();

        KParticleStorage.getGroup(params.attributes.contains("group") ? params.attributes.getString("group") : "default").add(this);
    }

    public static Map<String, String> toAttributeMap(NbtCompound params) {
        /*
            将入参NBT标签转换为属性名-表达式的Map，同时设置hsv、immortal属性。
            实际上是一个“扁平化”的过程。
         */

        // 将NBT扁平化为字典
        final Map<String, String> attributeMap = new HashMap<>();

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

    public void mergeExpressions(Map<String, String> attributeMap) {
        /*
            处理入参属性名-表达式Map，转化为自身属性名-Expression的Map。
            入参Map中包含的属性会覆写已存在的属性; 未包含的属性不会导致已存在属性被删除。
         */
        Pattern dynamicArgsPattern = Pattern.compile("\\{(\\w+)}");
        for (String key : attributeMap.keySet()) {
            String rawExpression = attributeMap.get(key).replaceAll("\\{prev}", // 此处用之前存入的表达式替换{prev}
                this.attributeMap.getOrDefault(key, "")
            );
            this.attributeMap.put(key, rawExpression);
            //System.out.println("%s: %s".formatted(key, rawExpression));

            if (key.equals("lt") && rawExpression.equals("inf")) {
                this.immortal = true;
                continue;
            }
            if (key.equals("hsv")) {
                this.hsv = rawExpression.equals("1");
                continue;
            }

            KExpressionBuilder builder = new KExpressionBuilder(rawExpression.replaceAll("[{}]", ""));
            Matcher matcher = dynamicArgsPattern.matcher(rawExpression);
            while (matcher.find()) {
                String variable_found = matcher.group(1);
                this.variables.add(variable_found);
                builder.variable(variable_found);
            }
            this.expressionMap.put(key, builder.build().setVariable("p", this.p).setVariable("n", this.n));
        }

        AsyncEvaluator asyncEvaluator = new AsyncEvaluator(new VariableSnapshot(this.variables, 0.0d, 1.0d), this.expressionMap, this.asyncPool);
        asyncEvaluator.submit("lt");
        asyncEvaluator.submit("ts");
        asyncEvaluator.submit("ag");

        if (this.expressionMap.containsKey("lt")) {
            this._maxAge = (float) Math.max(asyncEvaluator.get("lt", 1.0d), 1.0d);
            this.expressionMap.remove("lt");
        }

        if (this.expressionMap.containsKey("ts")) {
            this.tickSpeed = (float) asyncEvaluator.get("ts", 1.0d);
            this.expressionMap.remove("ts");
        }

        if (this.expressionMap.containsKey("ag")) {
            this._age = (float) Math.max(asyncEvaluator.get("ag", 0.0d), 0.0d);
            this.expressionMap.remove("ag");
        }
    }

    public void setExpressions(Map<String, String> attributeMap) {
        /*
            处理入参属性名-表达式Map，转化为自身属性名-Expression的Map。
            Map中未包含的属性会被删除。
         */
        this.variables.clear();
        this.expressionMap.clear();
        this.attributeMap.clear();
        mergeExpressions(attributeMap);
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.tickSpeed == 0) return;
        this._age += this.tickSpeed;
        if (!this.immortal && this._age >= this._maxAge) {
            this.markDead();
        } else {
            AsyncEvaluator asyncEvaluator = getAsyncEvaluator(this.variables, this._age, this._maxAge, this.expressionMap, this.asyncPool);

            if (expressionMap.containsKey("x")) {
                this.x = _x + asyncEvaluator.get("x", 0.0d);
                this.y = _y + asyncEvaluator.get("y", 0.0d);
                this.z = _z + asyncEvaluator.get("z", 0.0d);
            }

            if (expressionMap.containsKey("r")) {
                float _red = (float) Math.clamp(asyncEvaluator.get("r", 1.0d), 0.0d, 1.0d);
                float _green = (float) Math.clamp(asyncEvaluator.get("g", 1.0d), 0.0d, 1.0d);
                float _blue = (float) Math.clamp(asyncEvaluator.get("b", 1.0d), 0.0d, 1.0d);

                if (this.hsv) {
                    float[] _rgbResult = KMathFuncs.hsvToRgb(_red, _green, _blue);
                    this.red = _rgbResult[0];
                    this.green = _rgbResult[1];
                    this.blue = _rgbResult[2];
                } else {
                    this.red = _red;
                    this.green = _green;
                    this.blue = _blue;
                }
            } else {
                this.red = this.green = this.blue = 1.0f;
            }

            this.alpha = (float) Math.clamp(asyncEvaluator.get("a", 1.0d), 0.0d, 1.0d);
            this.scale = (float) Math.max(asyncEvaluator.get("s", 0.25d), 0.0d);
            this.setSprite(spriteProvider.getSprite((int) Math.round(asyncEvaluator.get("f", 0.0d) % 1.0d), 1));
        }
    }

    private static @NotNull AsyncEvaluator getAsyncEvaluator(List<String> variables, float _age, float _maxAge, Map<String, Expression> expressionMap, ExecutorService asyncPool) {
        AsyncEvaluator asyncEvaluator = new AsyncEvaluator(new VariableSnapshot(variables, _age, _maxAge), expressionMap, asyncPool);
        asyncEvaluator.submit("x");
        asyncEvaluator.submit("y");
        asyncEvaluator.submit("z");
        asyncEvaluator.submit("r");
        asyncEvaluator.submit("g");
        asyncEvaluator.submit("b");
        asyncEvaluator.submit("a");
        asyncEvaluator.submit("s");
        asyncEvaluator.submit("f");
        return asyncEvaluator;
    }

    @Override
    public int getBrightness(float tint) {
        AsyncEvaluator asyncEvaluator = new AsyncEvaluator(new VariableSnapshot(variables, _age, _maxAge), expressionMap, asyncPool);
        asyncEvaluator.submit("l");
        int l = Math.clamp(Math.round(asyncEvaluator.get("l", 15.0d)), 0, 15);
        return (l << 20) + (l << 4);
    }

    @Override
    public void markDead() {
        asyncPool.shutdownNow();
        this.dead = true;
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Environment(EnvType.CLIENT)
    public static class Factory implements ParticleFactory<KParticleEffect> {
        private final SpriteProvider spriteProvider;

        public Factory(SpriteProvider spriteProvider) {
            this.spriteProvider = spriteProvider;
        }

        public Particle createParticle(KParticleEffect kParticleEffect, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i) {
            return new K_Particle(clientWorld, d, e, f, kParticleEffect, this.spriteProvider);
        }
    }
}