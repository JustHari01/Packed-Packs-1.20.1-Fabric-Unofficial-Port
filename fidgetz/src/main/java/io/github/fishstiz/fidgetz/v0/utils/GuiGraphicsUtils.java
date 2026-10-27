package io.github.fishstiz.fidgetz.v0.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.fishstiz.fidgetz.v0.Fidgetz;
import io.github.fishstiz.fidgetz.v0.inject.interfaces.GuiGraphicsScissorTracker;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiGraphicsUtils {
    private static final ResourceLocation BOX_SHADOW_SPRITE = Fidgetz.id("box_shadow");
    private static final float BOX_SHADOW_BORDER = 48f;
    private static final ResourceLocation CHECKERBOARD_TEXTURE = Fidgetz.id("textures/gui/checkerboard.png");
    private static final int CHECKERBOARD_TEXTURE_SIZE = 16;
    private static final int CHECKERBOARD_TEXEL_SIZE = 8;
    private static final Map<ResourceLocation, TextureSize> TEXTURE_SIZES = new ConcurrentHashMap<>();

    public static void sprite(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        ResourceLocation texture = spriteTexture(sprite);
        TextureSize size = TEXTURE_SIZES.computeIfAbsent(texture, GuiGraphicsUtils::readTextureSize);
        if (size.width <= 0 || size.height <= 0) {
            return;
        }

        int border = sprite.equals(BOX_SHADOW_SPRITE) ? 48 : sprite.equals(Fidgetz.id("widget/popovermenu")) ? 1 : 0;
        if (border <= 0) {
            graphics.blit(texture, x, y, width, height, 0, 0, size.width, size.height, size.width, size.height);
            return;
        }

        blitNineSlice(graphics, texture, x, y, width, height, size.width, size.height, border);
    }

    private static ResourceLocation spriteTexture(ResourceLocation sprite) {
        if (sprite.getNamespace().equals("minecraft") && sprite.getPath().startsWith("widget/")) {
            String path = sprite.getPath().substring("widget/".length());
            if (path.startsWith("button")) {
                return new ResourceLocation("minecraft", "textures/gui/widgets.png");
            }
        }
        return new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
    }

    private static TextureSize readTextureSize(ResourceLocation texture) {
        var resource = Minecraft.getInstance().getResourceManager().getResource(texture).orElse(null);
        if (resource == null) {
            return TextureSize.MISSING;
        }
        try (var input = resource.open(); NativeImage image = NativeImage.read(input)) {
                return new TextureSize(image.getWidth(), image.getHeight());
        } catch (IOException | RuntimeException exception) {
            return TextureSize.MISSING;
        }
    }

    private static void blitNineSlice(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight,
            int border
    ) {
        int left = Math.min(border, textureWidth / 2);
        int right = left;
        int top = Math.min(border, textureHeight / 2);
        int bottom = top;
        int[] sourceX = {0, left, textureWidth - right};
        int[] sourceY = {0, top, textureHeight - bottom};
        int[] sourceWidths = {left, textureWidth - left - right, right};
        int[] sourceHeights = {top, textureHeight - top - bottom, bottom};

        int destLeft = Math.min(left, width / 2);
        int destRight = Math.min(right, width - destLeft);
        int destTop = Math.min(top, height / 2);
        int destBottom = Math.min(bottom, height - destTop);
        int[] destX = {x, x + destLeft, x + width - destRight};
        int[] destY = {y, y + destTop, y + height - destBottom};
        int[] destWidths = {destLeft, width - destLeft - destRight, destRight};
        int[] destHeights = {destTop, height - destTop - destBottom, destBottom};

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (sourceWidths[column] == 0 || sourceHeights[row] == 0 || destWidths[column] == 0 || destHeights[row] == 0) {
                    continue;
                }
                graphics.blit(
                        texture,
                        destX[column],
                        destY[row],
                        destWidths[column],
                        destHeights[row],
                        sourceX[column],
                        sourceY[row],
                        sourceWidths[column],
                        sourceHeights[row],
                        textureWidth,
                        textureHeight
                );
            }
        }
    }

    public static boolean containsPointInScissor(GuiGraphics graphics, int x, int y) {
        @Nullable ScreenRectangle bounds = ((GuiGraphicsScissorTracker) (Object) graphics).fidgetz$getCurrentScissor();
        return bounds == null || ScreenRectangleUtils.containsPoint(bounds, x, y);
    }

    private record TextureSize(int width, int height) {
        private static final TextureSize MISSING = new TextureSize(0, 0);
    }

    public static void texture(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int uWidth,
            int vHeight,
            int textureWidth,
            int textureHeight
    ) {
        graphics.blit(
                texture,
                x,
                y,
                width,
                height,
                u,
                v,
                uWidth,
                vHeight,
                textureWidth,
                textureHeight
        );
    }

    public static void texture(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            float u,
            float v,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        texture(
                graphics,
                texture,
                x,
                y,
                u,
                v,
                width,
                height,
                textureWidth,
                textureHeight,
                textureWidth,
                textureHeight
        );
    }

    public static void texture(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        texture(
                graphics,
                texture,
                x,
                y,
                0,
                0,
                width, height,
                textureWidth, textureHeight
        );
    }

    public static void texture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int textureWidth, int textureHeight) {
        texture(graphics, texture, x, y, textureWidth, textureHeight, textureWidth, textureHeight);
    }

    public static void fillFloat(GuiGraphics graphics, float left, float top, float right, float bottom, int color) {
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix4f = graphics.pose().last().pose();
        vertexConsumer.vertex(matrix4f, left, top, 0).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, left, bottom, 0).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, right, bottom, 0).color(color).endVertex();
        vertexConsumer.vertex(matrix4f, right, top, 0).color(color).endVertex();
    }

    public static void fillHorizontal(GuiGraphics graphics, int left, int top, int right, int bottom, int colorFrom, int colorTo) {
        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix4f = graphics.pose().last().pose();
        vertexConsumer.vertex(matrix4f, left, top, 0).color(colorFrom).endVertex();
        vertexConsumer.vertex(matrix4f, left, bottom, 0).color(colorFrom).endVertex();
        vertexConsumer.vertex(matrix4f, right, bottom, 0).color(colorTo).endVertex();
        vertexConsumer.vertex(matrix4f, right, top, 0).color(colorTo).endVertex();
    }

    public static void text(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawString(Minecraft.getInstance().font, text, x, y, color);
    }

    public static void scrollingText(
            GuiGraphics graphics,
            Font font,
            Component component,
            int left,
            int top,
            int right,
            int bottom,
            int color,
            boolean shadow
    ) {
        int textWidth = font.width(component);
        int textY = (top + bottom - font.lineHeight) / 2 + 1;
        int availableWidth = right - left;

        if (textWidth > availableWidth) {
            int overflowWidth = textWidth - availableWidth;
            double timeSeconds = Util.getMillis() / 1000.0;
            double scrollDuration = Math.max(overflowWidth * 0.5, 3.0);
            double scrollFactor = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * timeSeconds / scrollDuration)) / 2.0 + 0.5;
            double scrollOffset = Mth.lerp(scrollFactor, 0.0, overflowWidth);

            graphics.enableScissor(left, top, right, bottom);
            graphics.drawString(font, component, left - (int) scrollOffset, textY, color, shadow);
            graphics.disableScissor();
        } else {
            graphics.drawString(font, component, left, textY, color, shadow);
        }
    }

    public static void scrollingText(GuiGraphics graphics, Component component, int left, int top, int right, int bottom, int color, boolean shadow) {
        scrollingText(graphics, Minecraft.getInstance().font, component, left, top, right, bottom, color, shadow);
    }

    public static void scrollingText(GuiGraphics graphics, Font font, Component component, int left, int top, int right, int bottom, int color) {
        scrollingText(graphics, font, component, left, top, right, bottom, color, true);
    }

    public static void scrollingText(GuiGraphics graphics, Component component, int left, int top, int right, int bottom, int color) {
        scrollingText(graphics, Minecraft.getInstance().font, component, left, top, right, bottom, color);
    }

    public static void scrollingCenteredText(
            GuiGraphics graphics,
            Component component,
            int centerX,
            int left,
            int right,
            int top,
            int bottom,
            int color
    ) {
        Font font = Minecraft.getInstance().font;
        int textWidth = font.width(component);
        int textY = (top + bottom - font.lineHeight) / 2 + 1;
        int availableWidth = right - left;

        if (textWidth > availableWidth) {
            int overflowWidth = textWidth - availableWidth;
            double timeSeconds = Util.getMillis() / 1000.0;
            double scrollDuration = Math.max(overflowWidth * 0.5, 3.0);
            double scrollFactor = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * timeSeconds / scrollDuration)) / 2.0 + 0.5;
            double scrollOffset = Mth.lerp(scrollFactor, 0.0, overflowWidth);

            graphics.enableScissor(left, top, right, bottom);
            graphics.drawString(font, component, left - (int) scrollOffset, textY, color);
            graphics.disableScissor();
        } else {
            int i1 = Mth.clamp(centerX, left + textWidth / 2, right - textWidth / 2);
            graphics.drawCenteredString(font, component, i1, textY, color);
        }
    }

    public static void boxShadow(
            GuiGraphics graphics,
            int x,
            int y,
            int width,
            int height,
            float shadowSize,
            float shadowOffsetX,
            float shadowOffsetY
    ) {
        float scale = shadowSize / BOX_SHADOW_BORDER;
        int baseOffset = Math.round(BOX_SHADOW_BORDER * scale);
        int offsetX = Math.round(baseOffset + shadowOffsetX);
        int offsetY = Math.round(baseOffset + shadowOffsetY);

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        sprite(graphics, BOX_SHADOW_SPRITE, x - offsetX, y - offsetY, width + offsetX * 2, height + offsetY * 2);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    public static void checkerboard(
            GuiGraphics graphics,
            int left,
            int top,
            int width,
            int height,
            int cellSize,
            int uOffset,
            int vOffset
    ) {
        float scale = (float) CHECKERBOARD_TEXEL_SIZE / cellSize;

        float uTexelStart = (uOffset * scale) % CHECKERBOARD_TEXTURE_SIZE;
        float vTexelStart = (vOffset * scale) % CHECKERBOARD_TEXTURE_SIZE;

        if (uTexelStart < 0) uTexelStart += CHECKERBOARD_TEXTURE_SIZE;
        if (vTexelStart < 0) vTexelStart += CHECKERBOARD_TEXTURE_SIZE;

        graphics.blit(
                CHECKERBOARD_TEXTURE,
                left,
                top,
                width,
                height,
                uTexelStart,
                vTexelStart,
                (int) (width * scale),
                (int) (height * scale),
                CHECKERBOARD_TEXTURE_SIZE,
                CHECKERBOARD_TEXTURE_SIZE
        );
    }

    public static void checkerboard(GuiGraphics graphics, int left, int top, int width, int height, int cellSize) {
        checkerboard(graphics, left, top, width, height, cellSize, 0, 0);
    }

    private GuiGraphicsUtils() {
    }
}
