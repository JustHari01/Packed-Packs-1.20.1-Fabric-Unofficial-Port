package io.github.fishstiz.fidgetz.v0.gui.renderables;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Supplier;

public final class Renderables {
    private static final RenderableRectangle EMPTY = (ignoredG, ignoredL, ignoredT, ignoredW, ignoredH, ignoreMX, ignoredMY, ignoredD) -> {
    };

    private Renderables() {
    }

    public static RenderableRectangle empty() {
        return EMPTY;
    }

    public static RenderableRectangle fill(int color) {
        return new ColoredRectangle(color);
    }

    public static RenderableRectangle outline(int color) {
        return new Outline(color);
    }

    public static RenderableRectangle text(String text) {
        return text(Component.literal(Objects.requireNonNull(text, "text cannot be null")));
    }

    public static RenderableRectangle text(Component text) {
        Objects.requireNonNull(text, "text cannot be null");
        return new TextRectangle(() -> text);
    }

    public static RenderableRectangle text(Supplier<Component> text) {
        return new TextRectangle(Objects.requireNonNull(text, "text cannot be null"));
    }

    public static RenderableRectangle boxShadow(float scale) {
        return new BoxShadow(scale, 0, 0);
    }

    public static RenderableRectangle boxShadow(float scale, int xOffset, int yOffset) {
        return new BoxShadow(scale, xOffset, yOffset);
    }

    public static RenderableRectangle sprite(ResourceLocation sprite) {
        return new RenderableGuiSprite(Objects.requireNonNull(sprite, "sprite cannot be null"));
    }

    public static RenderableRectangle texture(
            ResourceLocation texture,
            int textureWidth,
            int textureHeight,
            float u,
            float v,
            int uWidth,
            int vHeight
    ) {
        return new RenderableTexture(Objects.requireNonNull(texture, "texture cannot be null"), textureWidth, textureHeight, u, v, uWidth, vHeight);
    }

    public static RenderableRectangle texture(ResourceLocation texture, int textureWidth, int textureHeight, int width, int height) {
        return texture(texture, textureWidth, textureHeight, 0, 0, width, height);
    }

    public static RenderableRectangle texture(ResourceLocation texture, int textureWidth, int textureHeight) {
        return texture(texture, textureWidth, textureHeight, 0, 0, textureWidth, textureHeight);
    }
}
