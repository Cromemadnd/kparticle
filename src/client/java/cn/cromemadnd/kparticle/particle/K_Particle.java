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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class K_Particle extends SpriteBillboardParticle {
    private final double _x;
    private final double _y;
    private final double _z;
    private final double p;
    private boolean immortal = false;
    private boolean hsv = true;
    private float tickSpeed = 1.0f;
    private float _age = 0.0f;
    private float _maxAge = 1.0f;
    private final SpriteProvider spriteProvider;
    private final List<String> variables = new ArrayList<>();
    private static final Map<String, String> SINGLE_ATTRIBUTES = Map.of(
        "alpha", "a",
        "brightness", "l",
        "scale", "s",
        "frame", "f",
        "lifetime", "lt",
        "timescale", "ts",
        "age", "ag"
    );
    private final Map<String, Expression> expressionMap = new HashMap<>();
    private final Map<String, String> attributeMap = new HashMap<>();

    public K_Particle(ClientWorld world, double x, double y, double z, KParticleEffect params, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this._x = x;
        this._y = y;
        this._z = z;
        this.spriteProvider = spriteProvider;
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
            if (key.equals("hsv")){
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
            this.expressionMap.put(key, builder.build().setVariable("p", this.p));
        }

        if (this.expressionMap.containsKey("lt")) {
            this._maxAge = (float) Math.max(evaluateWithVariables("lt", 1.0d), 1.0d);
            this.expressionMap.remove("lt");
        }

        if (this.expressionMap.containsKey("ts")) {
            this.tickSpeed = (float) evaluateWithVariables("ts", 0.0d);
            this.expressionMap.remove("ts");
        }

        if (this.expressionMap.containsKey("ag")) {
            this._age = (float) Math.max(evaluateWithVariables("ag", 0.0d), 0.0d);
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

    private double evaluateWithVariables(String attribute, double defaultVal) {
        if (this.expressionMap.containsKey(attribute)) {
            Expression expression = this.expressionMap.get(attribute);
            for (String variable : this.variables) {
                expression.setVariable(variable, KParticleStorage.getParticleData().getDouble(variable));
            }
            return expression.setVariable("t", this._age / 20.0d).evaluate();
        } else {
            return defaultVal;
        }
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        this._age += this.tickSpeed;
        if (!this.immortal && this._age >= this._maxAge) {
            this.markDead();
        } else {
            this.x = _x + evaluateWithVariables("x", 0.0d);
            this.y = _y + evaluateWithVariables("y", 0.0d);
            this.z = _z + evaluateWithVariables("z", 0.0d);

            if (expressionMap.containsKey("r")) {
                float _red = (float) Math.clamp(evaluateWithVariables("r", 1.0d), 0.0d, 1.0d);
                float _green = (float) Math.clamp(evaluateWithVariables("g", 1.0d), 0.0d, 1.0d);
                float _blue = (float) Math.clamp(evaluateWithVariables("b", 1.0d), 0.0d, 1.0d);

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

            this.alpha = (float) Math.clamp(evaluateWithVariables("a", 1.0d), 0.0d, 1.0d);
            this.scale = (float) Math.max(evaluateWithVariables("s", 1.0d), 0.0d);
        }
        //System.out.println("%d: %d".formatted((int) Math.round(evaluateWithVariables("f", 0.0d) % this._maxAge), Math.round(this._maxAge)));
        this.setSprite(spriteProvider.getSprite((int) Math.round(evaluateWithVariables("f", 0.0d) % this._maxAge), Math.round(this._maxAge)));
    }

    public int getBrightness(float tint) {
        int l = Math.clamp(Math.round(evaluateWithVariables("l", 15.0d)), 0, 15);
        //System.out.println(l);
        return (l << 20) + (l << 4);
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