package io.github.fishstiz.fidgetz.v0.inject.mixins;

import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.utils.TriState;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(targets = "net.minecraft.client.gui.components.AbstractSelectionList$Entry")
abstract class AbstractSelectionListEntryMixin implements FZHoverableContainer {
    @Shadow
    public abstract boolean isMouseOver(double mx, double my);

    @Unique
    private TriState fidgetz$hovered = TriState.DEFAULT;

    @Unique
    @Nullable
    private GuiEventListener fidgetz$hoveredElement;

    @Override
    public boolean fidgetz$isHovered() {
        return fidgetz$hovered == TriState.TRUE || fidgetz$getHovered() != null;
    }

    @Override
    public void fidgetz$setHovered(boolean hovered) {
        fidgetz$hovered = TriState.from(hovered);
        if (!hovered) {
            fidgetz$setHovered(null);
        }
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

    @Override
    public @Nullable GuiEventListener fidgetz$getHovered() {
        return fidgetz$hoveredElement;
    }

    @Override
    public boolean fidgetz$updateHovered(double mouseX, double mouseY) {
        fidgetz$setHovered(isMouseOver(mouseX, mouseY));
        boolean hovered = fidgetz$isHovered();
        if (hovered && this instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                if (((FZHoverableElement) child).fidgetz$updateHovered(mouseX, mouseY)) {
                    fidgetz$setHovered(child);
                    return true;
                }
            }
            fidgetz$setHovered(null);
            return true;
        }
        fidgetz$setHovered(null);
        return hovered;
    }
}
