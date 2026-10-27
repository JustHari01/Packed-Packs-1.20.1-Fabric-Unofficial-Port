package io.github.fishstiz.fidgetz.v0.inject.mixins;

import io.github.fishstiz.fidgetz.v0.inject.interfaces.WidgetOperator;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.UnaryOperator;

@Mixin(Screen.class)
abstract class ScreenMixin implements WidgetOperator {
    @Shadow
    @Final
    @Mutable
    private List<Renderable> renderables;

    @Shadow
    @Final
    @Mutable
    private List<NarratableEntry> narratables;

    @Shadow
    @Final
    @Mutable
    private List<GuiEventListener> children;

    @Shadow
    protected abstract <T extends Renderable> T addRenderableOnly(T renderable);

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Shadow
    protected abstract <T extends GuiEventListener & NarratableEntry> T addWidget(T widget);

    @Shadow
    protected abstract void removeWidget(GuiEventListener widget);

    @Shadow
    protected abstract void rebuildWidgets();

    @Override
    public void fidgetz$modifyWidgets(UnaryOperator<List<GuiEventListener>> modifier) {
        children = modifier.apply(children);
    }

    @Override
    public void fidgetz$modifyNarratables(UnaryOperator<List<NarratableEntry>> modifier) {
        narratables = modifier.apply(narratables);
    }

    @Override
    public void fidgetz$modifyRenderables(UnaryOperator<List<Renderable>> modifier) {
        renderables = modifier.apply(renderables);
    }

    @Override
    public <T extends Renderable> T fidgetz$addRenderableOnly(T renderable) {
        return addRenderableOnly(renderable);
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry & Renderable> T fidgetz$addRenderableWIdget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public <T extends GuiEventListener & NarratableEntry> T fidgetz$addWidget(T widget) {
        return addWidget(widget);
    }

    @Override
    public void fidgetz$removeWidget(GuiEventListener widget) {
        removeWidget(widget);
    }

    @Override
    public void fidgetz$rebuildWidgets() {
        rebuildWidgets();
    }
}
