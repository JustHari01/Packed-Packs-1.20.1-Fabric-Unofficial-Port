package io.github.fishstiz.fidgetz.v0.gui.components.color;

import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.color.HSVA;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;

public record HueGradient() implements RenderableRectangle {
    private static final int GRADIENTS = 6;

    @Override
    public void extractRenderState(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY, float partialTick) {
        extractRenderState(graphics, left, top, width, height);
    }

    public static void extractRenderState(GuiGraphics graphics, int left, int top, int width, int height) {
        float gradientWidth = (float) width / GRADIENTS;
        int bottom = top + height;

        VertexConsumer vertexConsumer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f matrix4f = graphics.pose().last().pose();

        for (int i = 0; i < GRADIENTS; i++) {
            int colorLeft = HSVA.toARGB(i * 60f, 1.0f, 1.0f, 1.0f);
            int colorRight = HSVA.toARGB((i + 1) * 60f, 1.0f, 1.0f, 1.0f);

            float x0 = left + i * gradientWidth;
            float x1 = left + (i + 1) * gradientWidth;

            vertexConsumer.vertex(matrix4f, x0, top, 0).color(colorLeft).endVertex();
            vertexConsumer.vertex(matrix4f, x0, bottom, 0).color(colorLeft).endVertex();
            vertexConsumer.vertex(matrix4f, x1, bottom, 0).color(colorRight).endVertex();
            vertexConsumer.vertex(matrix4f, x1, top, 0).color(colorRight).endVertex();
        }
    }
}
