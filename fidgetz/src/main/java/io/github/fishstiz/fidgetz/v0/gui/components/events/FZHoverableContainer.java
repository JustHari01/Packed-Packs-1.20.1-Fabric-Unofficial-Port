package io.github.fishstiz.fidgetz.v0.gui.components.events;

import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

public interface FZHoverableContainer extends FZHoverableElement {
    private UnsupportedOperationException notImplemented(String method) {
        return new UnsupportedOperationException("""
                FZHoverableContainer is only implemented for ContainerEventHandlers, \
                override %s if implementing on an unsupported class (%s).
                """.formatted(method, getClass().getName())
        );
    }

    default @Nullable GuiEventListener fidgetz$getHovered() {
        throw notImplemented("fidgetz$getHovered");
    }

    default void fidgetz$setHovered(@Nullable GuiEventListener hovered) {
        throw notImplemented("fidgetz$setHovered");
    }

    @Override
    default boolean fidgetz$isHovered() {
        return fidgetz$getHovered() != null;
    }

    @Override
    default void fidgetz$setHovered(boolean hovered) {
        if (!hovered) {
            fidgetz$setHovered(null);
        }
    }
}
