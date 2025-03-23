package cn.cromemadnd.kparticle.particle;

import cn.cromemadnd.kparticle.core.util.KConverter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

@Environment(EnvType.CLIENT)
public class K_Particle extends SpriteBillboardParticle {
    private final Expression[] expressions = new Expression[10];
    private final double _x;
    private final double _y;
    private final double _z;
    private final boolean hsv;
    private final SpriteProvider spriteProvider;

    public K_Particle(ClientWorld world, double x, double y, double z, KParticleEffect params, SpriteProvider spriteProvider) {
        super(world, x, y, z);
        this._x = x;
        this._y = y;
        this._z = z;
        this.spriteProvider = spriteProvider;

        double p = params.p;
        //this.maxAge = params.attributes.getInt("lifetime");
        this.maxAge = (int) Math.max(Math.round(new ExpressionBuilder(params.attributes.getString("lifetime")).variables("p").build().setVariable("p", p).evaluate()), 0);
        //System.out.println(this.maxAge);
        NbtList color = params.attributes.getList("color", NbtElement.STRING_TYPE);
        NbtList pos = params.attributes.getList("pos", NbtElement.STRING_TYPE);
        this.hsv = params.attributes.getBoolean("hsv");

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
        for (int i = 0; i < 10; i++) {
            expressions[i] = new ExpressionBuilder(expressions_raw[i]).variables("t", "p").build().setVariable("p", p);
        }

        tick();
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        double t = (double) age / this.maxAge;
        if (this.age++ >= this.maxAge) {
            this.markDead();
        } else {
            this.x = _x + expressions[0].setVariable("t", t).evaluate();
            this.y = _y + expressions[1].setVariable("t", t).evaluate();
            this.z = _z + expressions[2].setVariable("t", t).evaluate();


            float _red = (float) Math.clamp(expressions[3].setVariable("t", t).evaluate(), 0.0d, 1.0d);
            float _green = (float) Math.clamp(expressions[4].setVariable("t", t).evaluate(), 0.0d, 1.0d);
            float _blue = (float) Math.clamp(expressions[5].setVariable("t", t).evaluate(), 0.0d, 1.0d);

            if (hsv) {
                float[] _rgbresult = KConverter.hsvToRgb(_red, _green, _blue);
                this.red = _rgbresult[0];
                this.green = _rgbresult[1];
                this.blue = _rgbresult[2];
            } else {
                this.red = _red;
                this.green = _green;
                this.blue = _blue;
            }

            this.alpha = (float) Math.clamp(expressions[6].setVariable("t", t).evaluate(), 0.0d, 1.0d);
            this.scale = (float) Math.max(0.0, expressions[8].setVariable("t", t).evaluate());
        }
        this.setSprite(spriteProvider.getSprite((int) Math.round(expressions[9].setVariable("t", t).evaluate()) % this.maxAge, this.maxAge));
    }

    public int getBrightness(float tint) {
        double t = (double) age / this.maxAge;
        return Math.clamp(Math.round(expressions[7].setVariable("t", t).evaluate() * 15728880), 0, 15728880);
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