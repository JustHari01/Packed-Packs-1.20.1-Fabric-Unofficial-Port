package io.github.fishstiz.fidgetz.v0.gui.renderables;

import net.minecraft.client.gui.GuiGraphics;

record ColoredRectangle(int color) implements RenderableRectangle {
    @Override
    public void extractRenderState(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY, float partialTick) {
        graphics.fill(left, top, left + width, top + height, color);
    }
}
