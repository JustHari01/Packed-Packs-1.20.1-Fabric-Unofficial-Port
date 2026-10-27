package io.github.fishstiz.fidgetz.v0.inject.mixins;

import io.github.fishstiz.fidgetz.v0.gui.components.FZContextMenu;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GuiEventListener.class)
interface GuiEventListenerMixin extends FZHoverableElement, FZContextMenu.Source {
    @Shadow
    boolean isMouseOver(final double mouseX, final double mouseY);

    @Override
    default boolean fidgetz$isHovered() {
        return false;
    }

    @Override
    default void fidgetz$setHovered(boolean hovered) {
    }

    @Override
    default boolean fidgetz$updateHovered(double mouseX, double mouseY) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        fidgetz$setHovered(hovered);
        return fidgetz$isHovered();
    }
}
