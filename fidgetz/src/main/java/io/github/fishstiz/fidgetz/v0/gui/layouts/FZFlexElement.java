package io.github.fishstiz.fidgetz.v0.gui.layouts;

import net.minecraft.client.gui.layouts.LayoutElement;

public interface FZFlexElement extends LayoutElement {
    void fidgetz$setWidth(int width);

    void fidgetz$setHeight(int height);

    default void fidgetz$setSize(int width, int height) {
        this.fidgetz$setWidth(width);
        this.fidgetz$setHeight(height);
    }

    default boolean fidgetz$isVisible() {
        return true;
    }
}
