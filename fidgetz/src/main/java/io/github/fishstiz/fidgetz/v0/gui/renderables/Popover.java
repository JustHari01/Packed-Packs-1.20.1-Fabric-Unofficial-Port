package io.github.fishstiz.fidgetz.v0.gui.renderables;

import io.github.fishstiz.fidgetz.v0.gui.components.FZPopover;
import io.github.fishstiz.fidgetz.v0.gui.components.WidgetVisitor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;

import java.util.function.Consumer;

record Popover(Renderable renderable, int fidgetz$popoverOrder) implements Renderable, FZPopover {
    Popover(Renderable renderable) {
        this(renderable, DEFAULT_ORDER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderable.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void fidgetz$visitWidgets(WidgetVisitor visitor) {
    }

    @Override
    public void fidgetz$visitRenderables(Consumer<Renderable> visitor) {
        visitor.accept(this);
    }
}
