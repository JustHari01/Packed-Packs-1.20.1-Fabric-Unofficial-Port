package io.github.fishstiz.fidgetz.v0.utils;

import io.github.fishstiz.fidgetz.v0.gui.components.FZContextMenu;
import io.github.fishstiz.fidgetz.v0.inject.interfaces.ContextMenuSourceConsumer;
import io.github.fishstiz.fidgetz.v0.inject.interfaces.WidgetOperator;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class GuiHooks {
    public static void modifyRenderables(Screen screen, UnaryOperator<List<Renderable>> modifier) {
        ((WidgetOperator) screen).fidgetz$modifyRenderables(modifier);
    }

    public static void modifyWidgets(Screen screen, UnaryOperator<List<GuiEventListener>> modifier) {
        ((WidgetOperator) screen).fidgetz$modifyWidgets(modifier);
    }

    public static void modifyNarratables(Screen screen, UnaryOperator<List<NarratableEntry>> modifier) {
        ((WidgetOperator) screen).fidgetz$modifyNarratables(modifier);
    }

    public static <T extends GuiEventListener & NarratableEntry> T addWidget(Screen screen, T widget) {
        return ((WidgetOperator) screen).fidgetz$addWidget(widget);
    }

    public static <T extends Renderable> T addRenderableOnly(Screen screen, T renderable) {
        return ((WidgetOperator) screen).fidgetz$addRenderableOnly(renderable);
    }

    public static <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(Screen screen, T widget) {
        return ((WidgetOperator) screen).fidgetz$addRenderableWIdget(widget);
    }

    public static void removeWidget(Screen screen, GuiEventListener widget) {
        ((WidgetOperator) screen).fidgetz$removeWidget(widget);
    }

    public static void rebuildWidgets(Screen screen) {
        ((WidgetOperator) screen).fidgetz$rebuildWidgets();
    }

    public static void supplyContextMenuEntries(AbstractWidget widget, Consumer<FZContextMenu.Collector> entrySupplier) {
        ((ContextMenuSourceConsumer) widget).fidgetz$setContextMenuSource(entrySupplier);
    }

    private GuiHooks() {
    }
}
