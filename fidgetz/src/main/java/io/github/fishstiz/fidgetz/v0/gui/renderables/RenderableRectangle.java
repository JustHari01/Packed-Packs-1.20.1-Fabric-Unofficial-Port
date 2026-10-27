package io.github.fishstiz.fidgetz.v0.gui.renderables;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.function.Consumer;
import java.util.function.Supplier;

@FunctionalInterface
public interface RenderableRectangle {
    void extractRenderState(GuiGraphics graphics, int left, int top, int width, int height, int mouseX, int mouseY, float partialTick);

    default RenderableRectangle withBlend() {
        return ((graphics, left, top, width, height, mouseX, mouseY, partialTick) -> {
            RenderSystem.enableBlend();
            extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
            RenderSystem.disableBlend();
        });
    }

    default RenderableRectangle shrink(int inset) {
        return (graphics, left, top, width, height, mouseX, mouseY, partialTick) ->
                extractRenderState(
                        graphics,
                        left + inset,
                        top + inset,
                        width - (inset * 2),
                        height - (inset * 2),
                        mouseX,
                        mouseY,
                        partialTick
                );
    }

    default RenderableRectangle expand(int outset) {
        return shrink(-outset);
    }

    default RenderableRectangle crop(int inset) {
        return (graphics, left, top, width, height, mouseX, mouseY, partialTick) -> {
            graphics.enableScissor(left + inset, top + inset, (left + width) - inset, (top + height) - inset);
            extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
            graphics.disableScissor();
        };
    }

    default RenderableRectangle pose(Consumer<PoseStack> transformer) {
        return ((graphics, left, top, width, height, mouseX, mouseY, partialTick) -> {
            graphics.pose().pushPose();
            transformer.accept(graphics.pose());
            extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        });
    }

    default RenderableRectangle then(RenderableRectangle after) {
        return (graphics, left, top, width, height, mouseX, mouseY, partialTick) -> {
            extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
            after.extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
        };
    }

    default RenderableRectangle flush() {
        return (graphics, left, top, width, height, mouseX, mouseY, partialTick) -> {
            extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
            graphics.flush();
        };
    }

    default Renderable toRenderable(ScreenRectangle bounds) {
        int left = bounds.left();
        int top = bounds.top();
        int width = bounds.width();
        int height = bounds.height();
        return (graphics, mouseX, mouseY, partialTick) ->
                extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
    }

    default Renderable toRenderable(Supplier<ScreenRectangle> boundsSupplier) {
        return (graphics, mouseX, mouseY, partialTick) -> {
            ScreenRectangle bounds = boundsSupplier.get();
            extractRenderState(graphics, bounds.left(), bounds.top(), bounds.width(), bounds.height(), mouseX, mouseY, partialTick);
        };
    }

    default Renderable toPopover(ScreenRectangle bounds) {
        return new Popover(toRenderable(bounds));
    }

    default Renderable toPopover(ScreenRectangle bounds, int order) {
        return new Popover(toRenderable(bounds), order);
    }

    default Renderable toPopover(Supplier<ScreenRectangle> boundsSupplier) {
        return new Popover(toRenderable(boundsSupplier));
    }

    default Renderable toPopover(Supplier<ScreenRectangle> boundsSupplier, int order) {
        return new Popover(toRenderable(boundsSupplier), order);
    }
}
