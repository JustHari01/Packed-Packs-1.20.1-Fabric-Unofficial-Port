package io.github.fishstiz.fidgetz.v0.gui.color;

import org.jetbrains.annotations.Range;

public record HSVA(
        @Range(from = 0, to = 360) float hue,
        @Range(from = 0, to = 1) float saturation,
        @Range(from = 0, to = 1) float value,
        @Range(from = 0, to = 1) float alpha
) implements ColorModel {
    public HSVA withHue(float h) {
        return new HSVA(h, saturation, value, alpha);
    }

    public HSVA withSaturation(float s) {
        return new HSVA(hue, s, value, alpha);
    }

    public HSVA withValue(float v) {
        return new HSVA(hue, saturation, v, alpha);
    }

    public HSVA withSV(float s, float v) {
        return new HSVA(hue, s, v, alpha);
    }

    public HSVA withAlpha(float a) {
        return new HSVA(hue, saturation, value, a);
    }

    public HSVA clamped() {
        return new HSVA(
                net.minecraft.util.Mth.clamp(hue, 0.0f, 360.0f),
                net.minecraft.util.Mth.clamp(saturation, 0.0f, 1.0f),
                net.minecraft.util.Mth.clamp(value, 0.0f, 1.0f),
                net.minecraft.util.Mth.clamp(alpha, 0.0f, 1.0f)
        );
    }

    @Override
    public int toARGB() {
        return toARGB(hue, saturation, value, alpha);
    }

    @Override
    public String toString() {
        return String.format("HSVA(%.1f, %.3f, %.3f, %.3f)", hue, saturation, value, alpha);
    }

    public static int toARGB(
            @Range(from = 0, to = 360) float hue,
            @Range(from = 0, to = 1) float saturation,
            @Range(from = 0, to = 1) float value,
            @Range(from = 0, to = 1) float alpha
    ) {
        float c = value * saturation;
        float hp = hue / 60f;
        float x = c * (1 - Math.abs(hp % 2 - 1));
        float r1, g1, b1;

        if (hp < 1) {
            r1 = c;
            g1 = x;
            b1 = 0;
        } else if (hp < 2) {
            r1 = x;
            g1 = c;
            b1 = 0;
        } else if (hp < 3) {
            r1 = 0;
            g1 = c;
            b1 = x;
        } else if (hp < 4) {
            r1 = 0;
            g1 = x;
            b1 = c;
        } else if (hp < 5) {
            r1 = x;
            g1 = 0;
            b1 = c;
        } else {
            r1 = c;
            g1 = 0;
            b1 = x;
        }

        float m = value - c;
        int a = net.minecraft.util.Mth.clamp(Math.round(alpha * 255f), 0, 255);
        int r = net.minecraft.util.Mth.clamp(Math.round((r1 + m) * 255f), 0, 255);
        int g = net.minecraft.util.Mth.clamp(Math.round((g1 + m) * 255f), 0, 255);
        int b = net.minecraft.util.Mth.clamp(Math.round((b1 + m) * 255f), 0, 255);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static HSVA fromARGB(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = (argb >> 16 & 0xFF) / 255f;
        float g = (argb >> 8 & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h;
        if (delta == 0) {
            h = 0;
        } else if (max == r) {
            h = 60 * (((g - b) / delta) % 6);
        } else if (max == g) {
            h = 60 * (((b - r) / delta) + 2);
        } else {
            h = 60 * (((r - g) / delta) + 4);
        }
        if (h < 0) {
            h += 360;
        }

        float s = (max == 0) ? 0 : delta / max;

        return new HSVA(h, s, max, a);
    }
}