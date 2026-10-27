package io.github.fishstiz.fidgetz.v0.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class FZContainer extends AbstractContainerEventHandler implements Renderable, NarratableEntry, ContainerEventHandlerPatch {
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<NarratableEntry> narratables = new ArrayList<>();
    private final List<Renderable> renderables = new ArrayList<>();
    private @Nullable NarratableEntry lastNarratable;

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    protected List<? extends NarratableEntry> narratables() {
        return narratables;
    }

    protected List<? extends Renderable> renderables() {
        return renderables;
    }

    protected <T extends GuiEventListener & NarratableEntry & Renderable> T addRenderableWidget(T widget) {
        children.add(widget);
        renderables.add(widget);
        narratables.add(widget);
        return widget;
    }

    protected <T extends GuiEventListener & NarratableEntry & Renderable> T addRenderableWidgetFirst(T widget) {
        children.add(0, widget);
        renderables.add(0, widget);
        narratables.add(0, widget);
        return widget;
    }

    protected <T extends GuiEventListener & NarratableEntry & Renderable> T addRenderableWidget(int index, T widget) {
        int clampedChildrenIndex = Math.min(index, children.size());
        int clampedNarratablesIndex = Math.min(index, narratables.size());
        int clampedRenderablesIndex = Math.min(index, renderables.size());
        children.add(clampedChildrenIndex, widget);
        narratables.add(clampedNarratablesIndex, widget);
        renderables.add(clampedRenderablesIndex, widget);
        return widget;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T widget) {
        children.add(widget);
        narratables.add(widget);
        return widget;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidget(int index, T widget) {
        int clampedChildrenIndex = Math.min(index, children.size());
        int clampedNarratablesIndex = Math.min(index, narratables.size());
        children.add(clampedChildrenIndex, widget);
        narratables.add(clampedNarratablesIndex, widget);
        return widget;
    }

    protected <T extends GuiEventListener & NarratableEntry> T addWidgetFirst(T widget) {
        children.add(0, widget);
        narratables.add(0, widget);
        return widget;
    }

    protected <T extends Renderable> T addRenderableOnly(T renderable) {
        renderables.add(renderable);
        return renderable;
    }

    protected <T extends Renderable> T addRenderableOnly(int index, T renderable) {
        renderables.add(Math.min(index, renderables.size()), renderable);
        return renderable;
    }

    protected <T extends Renderable> T addRenderableOnlyFirst(T renderable) {
        renderables.add(0, renderable);
        return renderable;
    }

    protected void removeWidget(GuiEventListener widget) {
        children.remove(widget);

        if (widget instanceof NarratableEntry) {
            narratables.remove(widget);
        }

        if (widget instanceof Renderable) {
            renderables.remove(widget);
        }
    }

    protected void clearWidgets() {
        setFocused(false);
        children.clear();
        narratables.clear();
        renderables.clear();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : renderables()) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (isFocused()) {
            return NarrationPriority.FOCUSED;
        }
        for (GuiEventListener child : children()) {
            if (child instanceof AbstractWidget widget && widget.isHovered()) {
                return NarrationPriority.HOVERED;
            }
        }
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        List<? extends NarratableEntry> narratables = narratables();
        Screen.NarratableSearchResult result = Screen.findNarratableWidget(narratables, lastNarratable);
        if (result != null) {
            if (result.priority.isTerminal()) {
                lastNarratable = result.entry;
            }
            result.entry.updateNarration(output.nest());
        }
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) {
            setFocused(null);
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (getFocused() != focused) {
            super.setFocused(focused);
        }
    }
}
