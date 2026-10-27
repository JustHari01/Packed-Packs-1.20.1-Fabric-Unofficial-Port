package io.github.fishstiz.fidgetz.v0.gui.color;

import io.github.fishstiz.fidgetz.v0.Fidgetz;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Range;

public record RGBA(
        @Range(from = 0, to = 255) int red,
        @Range(from = 0, to = 255) int green,
        @Range(from = 0, to = 255) int blue,
        @Range(from = 0, to = 1) float alpha
) implements ColorModel {
    public boolean isMonochromatic() {
        return Math.max(red, Math.max(green, blue)) == Math.min(red, Math.min(green, blue));
    }

    public RGBA withRed(int r) {
        return new RGBA(r, green, blue, alpha);
    }

    public RGBA withGreen(int g) {
        return new RGBA(red, g, blue, alpha);
    }

    public RGBA withBlue(int b) {
        return new RGBA(red, green, b, alpha);
    }

    public RGBA withAlpha(float a) {
        return new RGBA(red, green, blue, a);
    }

    public RGBA clamped() {
        return new RGBA(
                net.minecraft.util.Mth.clamp(red, 0, 255),
                net.minecraft.util.Mth.clamp(green, 0, 255),
                net.minecraft.util.Mth.clamp(blue, 0, 255),
                net.minecraft.util.Mth.clamp(alpha, 0.0f, 1.0f)
        );
    }

    @Override
    public int toARGB() {
        return toARGB(red, green, blue, alpha);
    }

    @Override
    public String toString() {
        return String.format("RGBA(%d, %d, %d, %.3f)", red, green, blue, alpha);
    }

    public static int toARGB(
            @Range(from = 0, to = 255) int red,
            @Range(from = 0, to = 255) int green,
            @Range(from = 0, to = 255) int blue,
            @Range(from = 0, to = 1) float alpha
    ) {
        int a = net.minecraft.util.Mth.clamp(Math.round(alpha * 255f), 0, 255);
        return (a << 24) | (red << 16) | (green << 8) | blue;
    }

    public static RGBA fromARGB(int argb) {
        float a = ((argb >>> 24) & 0xFF) / 255f;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return new RGBA(r, g, b, a);
    }

    public static RGBA fromARGBHexString(String argbHexString) {
        String cleanHex = argbHexString.startsWith("#") ? argbHexString.substring(1) : argbHexString;

        if (cleanHex.isEmpty()) {
            return new RGBA(0, 0, 0, 1.0f);
        }

        if (cleanHex.length() > 8) {
            cleanHex = cleanHex.substring(0, 8);
            Fidgetz.LOG.warn(
                    "[fidgetz] ARGB hex string exceeds maximum length of 8: {}, truncated to {}",
                    argbHexString,
                    cleanHex
            );
        } else if (cleanHex.length() != 6 && cleanHex.length() != 8) {
            Fidgetz.LOG.warn("[fidgetz] Incomplete ARGB hex string '{}'. Defaulting missing channels.", argbHexString);
        }

        int r, g, b;
        float a = 1.0f;

        if (cleanHex.length() == 6) {
            r = parseChannelOrDefault(cleanHex.substring(0, 2), cleanHex, 0, "red");
            g = parseChannelOrDefault(cleanHex.substring(2, 4), cleanHex, 0, "green");
            b = parseChannelOrDefault(cleanHex.substring(4, 6), cleanHex, 0, "blue");
        } else {
            String paddedHex = StringUtils.rightPad(cleanHex, 8, '0');
            a = parseChannelOrDefault(paddedHex.substring(0, 2), paddedHex, 255, "alpha") / 255f;
            r = parseChannelOrDefault(paddedHex.substring(2, 4), paddedHex, 0, "red");
            g = parseChannelOrDefault(paddedHex.substring(4, 6), paddedHex, 0, "green");
            b = parseChannelOrDefault(paddedHex.substring(6, 8), paddedHex, 0, "blue");
        }

        return new RGBA(r, g, b, a);
    }

    private static int parseChannelOrDefault(String channel, String argb, int defaultValue, String name) {
        try {
            return Integer.parseInt(channel, 16);
        } catch (NumberFormatException e) {
            String defaultHexString = String.format("%02X", defaultValue);
            Fidgetz.LOG.warn("[fidgetz] Invalid {} channel {} in ARGB {}, defaulting to {}",
                    name,
                    argb,
                    channel,
                    defaultHexString
            );
            return defaultValue;
        }
    }
}