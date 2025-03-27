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
    private boolean immortal = false, hsv = false;
    private double tickSpeed = 1.0, _age = 0.0, _maxAge = 1.0, lastTickSpeed = 1.0;

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
            if (key.equals("lt") && rawExpression.equals("inf")) {
                this.immortal = true;
                continue;
            }
            if (key.equals("hsv")) {
                this.hsv = rawExpression.equals("1");
                continue;
            }

            KExpressionBuilder builder = new KExpressionBuilder(rawExpression.replaceAll("[{}]", ""));
            manager.variables.forEach(builder::variable);
            this.expressionMap.put(key, builder.build().setVariable("p", this.p).setVariable("n", this.n));
        }

        variableSnapshot.put("t", 0.0d);
        variableSnapshot.put("tp", 0.0d);
        manager.variables.forEach((variable) -> variableSnapshot.put(variable, KParticleStorage.getParticleData().getDouble(variable)));

        if (this.expressionMap.containsKey("lt")) {
            this._maxAge = Math.max(evaluateWithVariables("lt", 1.0d, variableSnapshot), 1.0d);
            this.expressionMap.remove("lt");
        }

        if (this.expressionMap.containsKey("ts")) {
            this.tickSpeed = evaluateWithVariables("ts", 1.0d, variableSnapshot);
            this.expressionMap.remove("ts");
        }

        if (this.expressionMap.containsKey("ag")) {
            this._age = Math.max(evaluateWithVariables("ag", 0.0d, variableSnapshot), 0.0d);
            this.expressionMap.remove("ag");
        }
    }

    public void clearExpressions() {
        /*
            处理入参属性名-表达式Map，转化为自身属性名-Expression的Map。
            Map中未包含的属性会被删除。
         */
        this.expressionMap.clear();
    }

    private double evaluateWithVariables(String attribute, double defaultVal, Map<String, Double> snapshot) {
        if (this.expressionMap.containsKey(attribute)) {
            Expression expression = this.expressionMap.get(attribute);
            snapshot.forEach(expression::setVariable);
            return expression.setVariable("t", snapshot.get("t")).setVariable("tp", snapshot.get("tp")).evaluate();
        } else {
            return defaultVal;
        }
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;

        if (this.lastTickSpeed == 0.0d) {
            this.lastTickSpeed = this.tickSpeed;
            return;
        }
        this._age += this.tickSpeed;

        variableSnapshot.put("t", this._age / 20.0d);
        variableSnapshot.put("tp", this._maxAge == 0.0d ? 0.0d : this._age / this._maxAge);
        manager.variables.forEach((variable) -> variableSnapshot.put(variable, KParticleStorage.getParticleData().getDouble(variable)));

        this.setSprite(spriteProvider.getSprite((int) Math.round(evaluateWithVariables("f", 0.0d, variableSnapshot) % this._maxAge), (int) Math.round(this._maxAge)));
        if (!this.immortal && this._age >= this._maxAge) {
            this.markDead();
        } else {
            this.x = _x + evaluateWithVariables("x", 0.0d, variableSnapshot);
            this.y = _y + evaluateWithVariables("y", 0.0d, variableSnapshot);
            this.z = _z + evaluateWithVariables("z", 0.0d, variableSnapshot);

            if (expressionMap.containsKey("r")) {
                float _red = (float) Math.clamp(evaluateWithVariables("r", 1.0d, variableSnapshot), 0.0d, 1.0d);
                float _green = (float) Math.clamp(evaluateWithVariables("g", 1.0d, variableSnapshot), 0.0d, 1.0d);
                float _blue = (float) Math.clamp(evaluateWithVariables("b", 1.0d, variableSnapshot), 0.0d, 1.0d);

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

            this.alpha = (float) Math.clamp(evaluateWithVariables("a", 1.0d, variableSnapshot), 0.0d, 1.0d);
            this.scale = (float) Math.max(evaluateWithVariables("s", 1.0d, variableSnapshot), 0.0d);
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