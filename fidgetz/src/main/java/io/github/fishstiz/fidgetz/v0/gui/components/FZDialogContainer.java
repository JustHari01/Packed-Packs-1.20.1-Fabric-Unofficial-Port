package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.List;
import java.util.function.Consumer;

public interface FZDialogContainer extends ContainerEventHandler, FZPopoverContainer {
    default List<? extends FZDialog> fidgetz$Dialogs() {
        return children().stream().filter(FZDialog.class::isInstance).map(FZDialog.class::cast).toList();
    }

    default boolean fidgetz$captureEventForDialogs(int keyCode) {
        if (keyCode == InputConstants.KEY_ESCAPE) {
            for (FZDialog dialog : fidgetz$Dialogs()) {
                if (dialog.isOpen() && dialog.shouldCloseOnEscape()) {
                    GuiEventListener focused = this.getFocused();
                    dialog.setOpen(false);
                    if (focused == dialog) {
                        dialog.refocusLastContainerPath();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    default boolean fidgetz$captureEventForDialogs(double mouseX, double mouseY, int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            for (FZDialog dialog : fidgetz$Dialogs()) {
                if (!dialog.isOpen()) continue;

                if (dialog.shouldCloseAfterClickOutOfBounds() && !dialog.areCoordinatesInBounds((int) mouseX, (int) mouseY)) {
                    dialog.setOpen(false);
                    return dialog.shouldCaptureClick();
                }

                if (dialog.isMouseOver(mouseX, mouseY)) {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    default void fidgetz$visitPopovers(Consumer<FZPopover> visitor) {
        fidgetz$Dialogs().forEach(visitor);
    }

    @Override
    ScreenRectangle getRectangle();
}
