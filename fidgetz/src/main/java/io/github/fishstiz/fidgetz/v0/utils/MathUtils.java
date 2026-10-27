package io.github.fishstiz.fidgetz.v0.utils;

public final class MathUtils {
    public static int clampOrAverage(int value, int min, int max) {
        return min > max ? (min + max) / 2 : net.minecraft.util.Mth.clamp(value, min, max);
    }

    public static int clampSafe(int value, int min, int max) {
        return min > max ? max : net.minecraft.util.Mth.clamp(value, min, max);
    }

    public static int clampOptionalMax(int value, int min, int max) {
        return max > 0 ? clampSafe(value, min, max) : Math.max(min, value);
    }

    public static int optionalMin(int value, int min) {
        return min > 0 ? Math.min(value, min) : value;
    }

    public static int eitherOptionalMin(int value, int min) {
        return min > 0 ? optionalMin(min, value) : value;
    }

    private MathUtils() {
    }
}
