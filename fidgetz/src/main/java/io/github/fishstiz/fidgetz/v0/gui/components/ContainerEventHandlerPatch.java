package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.ContainerEventHandler;

/**
 * also add this:
 * {@snippet :
 *    @Override
 *    public void setFocused(boolean focused) {
 *        if (!focused) {
 *            setFocused(null);
 *        }
 *    }
 *
 *    @Override
 *    public void setFocused(@Nullable GuiEventListener focused) {
 *        if (getFocused() != focused) {
 *            super.setFocused(focused);
 *        }
 *    }
 * }
 */
public interface ContainerEventHandlerPatch extends ContainerEventHandler {
    @Override
    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.isMouseClickHandled(mouseX, mouseY, button);
    }

    // propagates mouse click to the hovered child only
    private boolean isMouseClickHandled(double mouseX, double mouseY, int button) {
        return this.getChildAt(mouseX, mouseY).map(child -> {
            if (child.mouseClicked(mouseX, mouseY, button)) {
                if (!(child instanceof FZComponent component) || component.fidgetz$shouldTakeFocusAfterInteraction()) {
                    this.setFocused(child);
                }
                if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                    this.setDragging(true);
                }
                return true;
            }
            return false;
        }).orElse(false);
    }

    @Override
    default boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return ContainerEventHandler.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return ContainerEventHandler.super.mouseScrolled(mouseX, mouseY, scrollY);
    }
}
