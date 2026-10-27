package io.github.fishstiz.fidgetz.v0.gui.components;

import org.jetbrains.annotations.Nullable;

public interface FZComponent {
    default @Nullable String fidgetz$componentId() {
        return null;
    }

    default boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return true;
    }
}
