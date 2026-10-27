package io.github.fishstiz.fidgetz.v0.inject.mixins;

import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractContainerEventHandler.class)
abstract class AbstractContainerEventHandlerMixin implements ContainerEventHandler, FZHoverableContainer {
    @Unique
    @Nullable
    private GuiEventListener fidgetz$hoveredElement;

    @Override
    public @Nullable GuiEventListener fidgetz$getHovered() {
        return fidgetz$hoveredElement;
    }

    @Override
    public void fidgetz$setHovered(@Nullable GuiEventListener hovered) {
        GuiEventListener previous = fidgetz$getHovered();
        if (previous != hovered) {
            if (previous instanceof FZHoverableElement previousElement) {
                previousElement.fidgetz$setHovered(false);
            }

            if (hovered instanceof FZHoverableElement hoveredElement) {
                hoveredElement.fidgetz$setHovered(true);
            }

            fidgetz$hoveredElement = hovered;
        }
    }
}
