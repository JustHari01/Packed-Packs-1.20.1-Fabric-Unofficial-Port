package io.github.fishstiz.fidgetz.v0.gui.components;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;

@FunctionalInterface
public interface WidgetVisitor {
    <T extends GuiEventListener & NarratableEntry> void visitWidget(T widget);
}
