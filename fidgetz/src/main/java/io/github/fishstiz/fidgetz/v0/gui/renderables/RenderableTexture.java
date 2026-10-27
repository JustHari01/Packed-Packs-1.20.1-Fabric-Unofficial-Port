package io.github.fishstiz.fidgetz.v0.gui.renderables;

import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

record RenderableTexture(
        ResourceLocation texture,
        int textureWidth,
        int textureHeight,
        float u,
        float v,
        int uWidth,
        int vHeight
) implements RenderableRectangle {
    @Override
    public void extractRenderState(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY, float partialTick) {
        GuiGraphicsUtils.texture(
                graphics,
                texture,
                left,
                top,
                u,
                v,
                width,
                height,
                uWidth,
                vHeight,
                textureWidth,
                textureHeight
        );
    }
}
