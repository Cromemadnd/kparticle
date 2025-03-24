package cn.cromemadnd.kparticle.particle;

import cn.cromemadnd.kparticle.KParticleClient;
import cn.cromemadnd.kparticle.core.util.KMathFuncs;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Environment(EnvType.CLIENT)
public class K_Particle extends SpriteBillboardParticle {
    private final Expression[] expressions = new Expression[10];
    private final double _x;
    private final double _y;
    private final double _z;
    private final boolean hsv;
    private final SpriteProvider spriteProvider;
    private final List<String> variables = new ArrayList<>();

    public K_Particle(ClientWorld world, double x, double y, double z, KParticleEffect params, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this._x = x;
        this._y = y;
        this._z = z;
        this.spriteProvider = spriteProvider;
        System.out.println(spriteProvider);
        //System.out.println(world.getPlayers());

        double p = params.p;
        this.maxAge = (int) Math.max(Math.round(new ExpressionBuilder(params.attributes.getString("lifetime")).variables("p").build().setVariable("p", p).evaluate()), 0);
        NbtList color = params.attributes.getList("color", NbtElement.STRING_TYPE);
        NbtList pos = params.attributes.getList("pos", NbtElement.STRING_TYPE);
        this.hsv = params.attributes.getBoolean("hsv");

        // 构造表达式
        final String[] expressions_raw = {
            pos.isEmpty() ? "0" : pos.getString(0),
            pos.isEmpty() ? "0" : pos.getString(1),
            pos.isEmpty() ? "0" : pos.getString(2),
            color.isEmpty() ? "1" : color.getString(0),
            color.isEmpty() ? "1" : color.getString(1),
            color.isEmpty() ? "1" : color.getString(2),
            params.attributes.getString("alpha").isEmpty() ? "1" : params.attributes.getString("alpha"),
            params.attributes.getString("brightness").isEmpty() ? "1" : params.attributes.getString("brightness"),
            params.attributes.getString("scale").isEmpty() ? "1" : params.attributes.getString("scale"),
            params.attributes.getString("frame").isEmpty() ? "0" : params.attributes.getString("frame"),
        };
        // 处理动态变量
        Pattern dynamicArgsPattern = Pattern.compile("\\{(\\w+)}");
        for (int i = 0; i < expressions_raw.length; i++) {
            ExpressionBuilder builder = new ExpressionBuilder(expressions_raw[i].replaceAll("[{}]", ""))
                .function(KMathFuncs.max)
                .function(KMathFuncs.min)
                .function(KMathFuncs.clamp)
                .function(KMathFuncs.random)
                .variables("t", "p");

            Matcher matcher = dynamicArgsPattern.matcher(expressions_raw[i]);
            while (matcher.find()) {
                String variable_found = matcher.group(1);
                this.variables.add(variable_found);
                builder.variable(variable_found);
            }
            expressions[i] = builder.build().setVariable("p", p);
        }

        // 需要计算t = 0时的参数
        this.age = -1;
        tick();
    }

    private double evaluateWithVariables(Expression expression, double t) {
        for (String variable : this.variables) {
            expression.setVariable(variable, KParticleClient.particle_storage.getDouble(variable));
        }
        return expression.setVariable("t", t).evaluate();
    }

    @Override
    public void tick() {
        //System.out.println(this.storage.get(KPARTICLE_STORAGE));
        //System.out.println(KParticleClient.particle_storage);
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        double t = (double) age / this.maxAge;
        if (++this.age >= this.maxAge) {
            this.markDead();
        } else {
            this.x = _x + evaluateWithVariables(expressions[0], t);
            this.y = _y + evaluateWithVariables(expressions[1], t);
            this.z = _z + evaluateWithVariables(expressions[2], t);

            float _red = (float) Math.clamp(evaluateWithVariables(expressions[3], t), 0.0d, 1.0d);
            float _green = (float) Math.clamp(evaluateWithVariables(expressions[4], t), 0.0d, 1.0d);
            float _blue = (float) Math.clamp(evaluateWithVariables(expressions[5], t), 0.0d, 1.0d);

            if (hsv) {
                float[] _rgbresult = KMathFuncs.hsvToRgb(_red, _green, _blue);
                this.red = _rgbresult[0];
                this.green = _rgbresult[1];
                this.blue = _rgbresult[2];
            } else {
                this.red = _red;
                this.green = _green;
                this.blue = _blue;
            }

            this.alpha = (float) Math.clamp(evaluateWithVariables(expressions[6], t), 0.0d, 1.0d);
            //System.out.println();
            //System.out.println(this.alpha);
            this.scale = (float) Math.max(evaluateWithVariables(expressions[8], t), 0.0d);
        }
        //this.sprite = spriteProvider
        this.setSprite(spriteProvider.getSprite((int) Math.round(evaluateWithVariables(expressions[9], t)) % this.maxAge, this.maxAge));
    }

    public int getBrightness(float tint) {
        double t = (double) age / this.maxAge;
        return (int) Math.round(Math.clamp(evaluateWithVariables(expressions[7], t), 0.0d, 1.0d) * 15728880);
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