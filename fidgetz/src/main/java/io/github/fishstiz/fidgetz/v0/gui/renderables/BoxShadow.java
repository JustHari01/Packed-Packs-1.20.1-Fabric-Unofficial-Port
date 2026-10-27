package io.github.fishstiz.fidgetz.v0.gui.renderables;

import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import net.minecraft.client.gui.GuiGraphics;

record BoxShadow(float size, float offsetX, float offsetY) implements RenderableRectangle {
    @Override
    public void extractRenderState(
            GuiGraphics graphics,
            int left,
            int top,
            int width,
            int height,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        GuiGraphicsUtils.boxShadow(graphics, left, top, width, height, size, offsetX, offsetY);
    }
}
