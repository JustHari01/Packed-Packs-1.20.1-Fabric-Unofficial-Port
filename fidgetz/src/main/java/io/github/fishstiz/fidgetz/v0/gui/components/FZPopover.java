package io.github.fishstiz.fidgetz.v0.gui.components;

import net.minecraft.client.gui.components.Renderable;

import java.util.function.Consumer;

public interface FZPopover {
    int DEFAULT_ORDER = 100;

    void fidgetz$visitWidgets(WidgetVisitor visitor);

    void fidgetz$visitRenderables(Consumer<Renderable> visitor);

    default int fidgetz$popoverOrder() {
        return DEFAULT_ORDER;
    }
}
