package io.github.fishstiz.fidgetz.v0.gui.components.color;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.CommonColors;

public record SaturationValueGradient(int color) implements RenderableRectangle {
    private static final int BLACK_TRANSPARENT = 0x00000000;

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
        extractRenderState(graphics, left, top, left + width, top + height, color);
    }

    public static void extractRenderState(
            GuiGraphics graphics,
            int left,
            int top,
            int right,
            int bottom,
            int color
    ) {
        GuiGraphicsUtils.fillHorizontal(
                graphics,
                left,
                top,
                right,
                bottom,
                CommonColors.WHITE,
                color
        );

        graphics.fillGradient(
                left,
                top,
                right,
                bottom,
                BLACK_TRANSPARENT,
                CommonColors.BLACK
        );
    }
}
