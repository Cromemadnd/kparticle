package cn.cromemadnd.kparticle.particle;

import cn.cromemadnd.kparticle.core.KExpressionBuilder;
import cn.cromemadnd.kparticle.core.KMathFuncs;
import cn.cromemadnd.kparticle.core.KParticleStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.objecthunter.exp4j.Expression;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class K_Particle extends SpriteBillboardParticle {
    private final double _x, _y, _z, p;
    private final int n;
    private final K_ParticleManager manager;
    private final SpriteProvider spriteProvider;
    private final Map<String, Expression> expressionMap = new ConcurrentHashMap<>();
    private final Map<String, Double> variableSnapshot = new ConcurrentHashMap<>();
    private double tickSpeed, _age, _maxAge;

    public K_Particle(ClientWorld world, double x, double y, double z, KParticleEffect params, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this._x = x;
        this._y = y;
        this._z = z;
        this.spriteProvider = spriteProvider;
        this.p = params.p;
        this.n = params.n;
        this.manager = K_ParticleManager.getManager(params.managerId);
        if (spriteProvider == null || this.manager == null) {
            this.markDead();
        }

        assert this.manager != null;
        this.manager.add(this);
        mergeExpressions();
        tick();
    }

    public void mergeExpressions() {
        /*
            处理入参属性名-表达式Map，转化为自身属性名-Expression的Map。
            入参Map中包含的属性会覆写已存在的属性; 未包含的属性不会导致已存在属性被删除。
         */
        for (String key : manager.attributeMap.keySet()) {
            String rawExpression = manager.attributeMap.get(key);
            KExpressionBuilder builder = new KExpressionBuilder(rawExpression.replaceAll("[{}]", ""));
            manager.variables.forEach(builder::variable);
            this.expressionMap.put(key, builder.build().setVariable("p", this.p).setVariable("n", this.n));
        }

        manager.variables.forEach((variable) -> variableSnapshot.put(variable, KParticleStorage.getParticleData(variable)));
        variableSnapshot.put("t", 0.0d);
        variableSnapshot.put("c", 0.0d);

        this._maxAge = Math.max(evaluateWithVariables("lt", 1.0d, variableSnapshot), 1.0d);
        this.expressionMap.remove("lt");
        this.tickSpeed = evaluateWithVariables("ts", 1.0d, variableSnapshot);
        this.expressionMap.remove("ts");
        this._age = Math.max(evaluateWithVariables("ag", 0.0d, variableSnapshot), 0.0d);
        this.expressionMap.remove("ag");
    }

    public void clearExpressions() {
        /*
            处理入参属性名-表达式Map，转化为自身属性名-Expression的Map。
            Map中未包含的属性会被删除。
         */
        this.expressionMap.clear();
    }

    private double evaluateWithVariables(String attribute, double defaultVal, Map<String, Double> snapshot) {
        if (manager.constAttributeMap.containsKey(attribute)) {
            return manager.constAttributeMap.get(attribute);
        }

        if (this.expressionMap.containsKey(attribute)) {
            Expression expression = this.expressionMap.get(attribute);
            snapshot.forEach(expression::setVariable);
            return expression.evaluate();
        } else {
            return defaultVal;
        }
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.tickSpeed == 0.0d) return;
        this._age += this.tickSpeed;

        manager.variables.forEach((variable) -> variableSnapshot.put(variable, KParticleStorage.getParticleData(variable)));
        variableSnapshot.put("t", this._age / 20.0d);
        variableSnapshot.put("c", manager.immortal ? 0.0d : this._age / this._maxAge);

        this.setSprite(spriteProvider.getSprite((int) Math.round(evaluateWithVariables("f", 0.0d, variableSnapshot) % this._maxAge), (int) Math.round(this._maxAge)));
        if (!manager.immortal && this._age >= this._maxAge) {
            this.markDead();
        } else {
            double __x = 0, __y = 0, __z = 0;
            for (int i = 0; i < this.manager.coords.size(); i++) {
                double _x = evaluateWithVariables("x%d".formatted(i), 0.0d, variableSnapshot);
                double _y = evaluateWithVariables("y%d".formatted(i), 0.0d, variableSnapshot);
                double _z = evaluateWithVariables("z%d".formatted(i), 0.0d, variableSnapshot);

                switch (manager.coords.get(i)) {
                    case 0: { // 直角坐标系（MC，Y为竖轴）
                        // x, y, z
                        __x += _x;
                        __y += _y;
                        __z += _z;
                    }
                    break;
                    case 1: { // 球坐标系（MC，theta为水平面俯角，phi为与z+轴的角(x-方向为90°)）
                        // r, phi(deg), theta(deg)
                        __x -= _x * Math.cos(Math.toRadians(_z)) * Math.sin(Math.toRadians(_y));
                        __y -= _x * Math.sin(Math.toRadians(_z));
                        __z += _x * Math.cos(Math.toRadians(_z)) * Math.cos(Math.toRadians(_y));
                    }
                    break;
                    case 2: { // 球坐标系（数学）
                        // r, theta(rad), phi(rad)
                        __x += _x * Math.sin(_y) * Math.cos(_z);
                        __y += _x * Math.cos(_y);
                        __z += _x * Math.sin(_y) * Math.sin(_z);
                    }
                    break;
                    case 3: { // 柱坐标系（MC，phi为与z+轴的角(x-方向为90°)）
                        // r, phi(deg), z
                        __x -= _x * Math.sin(Math.toRadians(_y));
                        __y += _z;
                        __z += _x * Math.cos(Math.toRadians(_y));
                    }
                    break;
                    case 4: { // 柱坐标系（数学）
                        // r, phi(deg), z
                        __x += _x * Math.cos(_y);
                        __y += _z;
                        __z += _x * Math.sin(_y);
                    }
                    break;
                }
            }
            this.x = this._x + __x;
            this.y = this._y + __y;
            this.z = this._z + __z;

            float _red = (float) Math.clamp(evaluateWithVariables("r", 1.0d, variableSnapshot), 0.0d, 1.0d);
            float _green = (float) Math.clamp(evaluateWithVariables("g", 1.0d, variableSnapshot), 0.0d, 1.0d);
            float _blue = (float) Math.clamp(evaluateWithVariables("b", 1.0d, variableSnapshot), 0.0d, 1.0d);

            if (manager.hsv) {
                float[] _rgbResult = KMathFuncs.hsvToRgb(_red, _green, _blue);
                this.red = _rgbResult[0];
                this.green = _rgbResult[1];
                this.blue = _rgbResult[2];
            } else {
                this.red = _red;
                this.green = _green;
                this.blue = _blue;
            }

            this.alpha = (float) Math.clamp(evaluateWithVariables("a", 1.0d, variableSnapshot), 0.0d, 1.0d);
            this.scale = (float) Math.max(evaluateWithVariables("s", 0.1d, variableSnapshot), 0.0d);
        }
    }

    public int getBrightness(float tint) {
        int l = Math.clamp(Math.round(evaluateWithVariables("l", 15.0d, variableSnapshot)), 0, 15);
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