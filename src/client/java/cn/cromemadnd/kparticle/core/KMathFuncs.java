package cn.cromemadnd.kparticle.core;

import net.objecthunter.exp4j.function.Function;

public class KMathFuncs {
    // Generated using DeepSeek
    public static float[] hsvToRgb(float h, float s, float v) {
        float r, g, b;

        // 将 h 从 0~1 映射到 0~360
        h *= 360;

        if (s == 0) {
            // 灰度模式（饱和度 s=0）
            r = g = b = v;
        } else {
            // 计算色相扇区（0~5）
            h /= 60;          // 将色相划分为 6 个扇区
            int sector = (int) Math.floor(h);
            float fract = h - sector; // 扇区内的小数部分（0~1）

            // 中间计算值
            float p = v * (1 - s);
            float q = v * (1 - s * fract);
            float t = v * (1 - s * (1 - fract));

            // 根据扇区分配 RGB 值
            switch (sector) {
                case 0:
                    r = v;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = v;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = v;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = v;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = v;
                    break;
                default:
                    r = v;
                    g = p;
                    b = q;
                    break;
            }
        }

        return new float[]{r, g, b};
    }

    public static final Function max = new Function("max", 2) {
        @Override
        public double apply(double... args) {
            return Math.max(args[0], args[1]);
        }
    };
    public static final Function min = new Function("min", 2) {
        @Override
        public double apply(double... args) {
            return Math.min(args[0], args[1]);
        }
    };
    public static final Function clamp = new Function("clamp", 3) {
        @Override
        public double apply(double... args) {
            return Math.clamp(args[0], args[1], args[2]);
        }
    };
    public static final Function random = new Function("random", 0) {
        @Override
        public double apply(double... args) {
            return Math.random();
        }
    };
    public static final Function _if = new Function("if", 3) {
        @Override
        public double apply(double... args) {
            return args[0] > 0 ? args[1] : args[2];
        }
    };
}
