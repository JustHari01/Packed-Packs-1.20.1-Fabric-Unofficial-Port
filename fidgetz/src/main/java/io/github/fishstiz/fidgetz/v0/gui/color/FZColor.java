package io.github.fishstiz.fidgetz.v0.gui.color;

import java.util.Objects;

public final class FZColor implements ColorModel {
    private final RGBA rgba;
    private final HSVA hsva;

    private FZColor(RGBA rgba, HSVA hsva) {
        this.rgba = rgba;
        this.hsva = hsva;
    }

    public static FZColor fromHSVA(HSVA hsva) {
        return new FZColor(HSVAtoRGBA(hsva), hsva);
    }

    public static FZColor fromRGBA(RGBA rgba) {
        return new FZColor(rgba, RGBAtoHSVA(rgba));
    }

    public static FZColor fromARGB(int argb) {
        return fromRGBA(RGBA.fromARGB(argb));
    }

    public static FZColor fromHexString(String hex) {
        return fromRGBA(RGBA.fromARGBHexString(hex));
    }

    public static RGBA HSVAtoRGBA(HSVA hsva) {
        float h = (hsva.hue() % 360f + 360f) % 360f;
        float s = hsva.saturation();
        float v = hsva.value();

        float c = v * s;
        float hp = h / 60f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r1 = 0, g1 = 0, b1 = 0;

        if (hp < 1) {
            r1 = c;
            g1 = x;
        } else if (hp < 2) {
            r1 = x;
            g1 = c;
        } else if (hp < 3) {
            g1 = c;
            b1 = x;
        } else if (hp < 4) {
            g1 = x;
            b1 = c;
        } else if (hp < 5) {
            r1 = x;
            b1 = c;
        } else {
            r1 = c;
            b1 = x;
        }

        float m = v - c;
        int r = Math.round((r1 + m) * 255f);
        int g = Math.round((g1 + m) * 255f);
        int b = Math.round((b1 + m) * 255f);

        return new RGBA(r, g, b, hsva.alpha());
    }

    public static HSVA RGBAtoHSVA(RGBA rgba) {
        float r = rgba.red() / 255f;
        float g = rgba.green() / 255f;
        float b = rgba.blue() / 255f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0f;
        if (delta != 0) {
            if (max == r) {
                h = 60f * (((g - b) / delta) % 6f);
            } else if (max == g) {
                h = 60f * (((b - r) / delta) + 2f);
            } else {
                h = 60f * (((r - g) / delta) + 4f);
            }
            if (h < 0) {
                h += 360f;
            }
        }

        float s = (max == 0) ? 0f : delta / max;
        return new HSVA(h, s, max, rgba.alpha());
    }

    public int red() {
        return rgba.red();
    }

    public int green() {
        return rgba.green();
    }

    public int blue() {
        return rgba.blue();
    }

    public float hue() {
        return hsva.hue();
    }

    public float saturation() {
        return hsva.saturation();
    }

    public float value() {
        return hsva.value();
    }

    @Override
    public float alpha() {
        return rgba.alpha();
    }

    public FZColor withRed(int red) {
        RGBA newRgba = rgba.withRed(red);
        HSVA hsvaConverted = RGBAtoHSVA(newRgba);
        HSVA newHsva = newRgba.isMonochromatic() ? hsvaConverted.withHue(hsva.hue()) : hsvaConverted;
        return new FZColor(newRgba, newHsva);
    }

    public FZColor withGreen(int green) {
        RGBA newRgba = rgba.withGreen(green);
        HSVA hsvaConverted = RGBAtoHSVA(newRgba);
        HSVA newHsva = newRgba.isMonochromatic() ? hsvaConverted.withHue(hsva.hue()) : hsvaConverted;
        return new FZColor(newRgba, newHsva);
    }

    public FZColor withBlue(int blue) {
        RGBA newRgba = rgba.withBlue(blue);
        HSVA hsvaConverted = RGBAtoHSVA(newRgba);
        HSVA newHsva = newRgba.isMonochromatic() ? hsvaConverted.withHue(hsva.hue()) : hsvaConverted;
        return new FZColor(newRgba, newHsva);
    }

    public FZColor withHue(float hue) {
        HSVA newHsva = hsva.withHue(hue);
        return new FZColor(HSVAtoRGBA(newHsva), newHsva);
    }

    public FZColor withSaturation(float saturation) {
        HSVA newHsva = hsva.withSaturation(saturation);
        return new FZColor(HSVAtoRGBA(newHsva), newHsva);
    }

    public FZColor withValue(float value) {
        HSVA newHsva = hsva.withValue(value);
        return new FZColor(HSVAtoRGBA(newHsva), newHsva);
    }

    public FZColor withSV(float saturation, float value) {
        HSVA newHsva = hsva.withSV(saturation, value);
        return new FZColor(HSVAtoRGBA(newHsva), newHsva);
    }

    public FZColor withAlpha(float alpha) {
        return new FZColor(rgba.withAlpha(alpha), hsva.withAlpha(alpha));
    }

    public HSVA hsva() {
        return hsva;
    }

    public RGBA rgba() {
        return rgba;
    }

    public FZColor clamped() {
        return new FZColor(rgba.clamped(), hsva.clamped());
    }

    @Override
    public int toARGB() {
        return rgba.toARGB();
    }

    @Override
    public String toString() {
        return "FZColor{" +
               "hsva=" + hsva +
               ", rgba=" + rgba +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FZColor other)) return false;
        return Objects.equals(rgba, other.rgba) && Objects.equals(hsva, other.hsva);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rgba, hsva);
    }
}