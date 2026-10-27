package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.utils.NavigationUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public abstract class FZDialog extends FZContainer implements FZComponent, FZPopover, ContainerEventHandlerPatch {
    protected static final int DEFAULT_POPOVER_ORDER = 0;
    protected final ContainerEventHandler container;
    protected @Nullable String componentId;
    protected int popoverOrder = DEFAULT_POPOVER_ORDER;
    private @Nullable ComponentPath lastContainerFocusPath;
    private boolean open;

    protected FZDialog(ContainerEventHandler container) {
        this.container = container;
    }

    public boolean shouldCloseOnEscape() {
        return true;
    }

    public boolean shouldCaptureClick() {
        return true;
    }

    public boolean shouldCloseAfterClickOutOfBounds() {
        return true;
    }

    public boolean shouldCaptureFocus() {
        return true;
    }

    public boolean shouldFocusOnOpen() {
        return true;
    }

    public boolean shouldRefocusLastPath() {
        return true;
    }

    public boolean isOpen() {
        return open;
    }

    protected void setOpen(boolean open) {
        boolean previous = isOpen();
        this.open = open;

        if (previous != isOpen()) {
            if (isOpen()) {
                lastContainerFocusPath = container.getCurrentFocusPath();
                onOpen();
            } else {
                onClose();
            }
        }
    }

    protected void refocusLastContainerPath() {
        if (shouldRefocusLastPath() && lastContainerFocusPath != null) {
            MutableObject<@Nullable List<? extends GuiEventListener>> current = new MutableObject<>();
            ComponentPath path = NavigationUtils.takeWhile(lastContainerFocusPath, component -> {
                List<? extends GuiEventListener> children = current.getValue();
                if (children == null || children.contains(component)) {
                    current.setValue(component instanceof ContainerEventHandler subContainer ? subContainer.children() : null);
                    return true;
                }
                return false;
            });
            if (path != null) {
                path.applyFocus(true);
            }

            lastContainerFocusPath = null;
        }
    }

    protected void onClose() {
        ComponentPath parentFocusPath = container.getCurrentFocusPath();
        boolean focused = NavigationUtils.inFocusPath(container, this);

        if (focused) {
            NavigationUtils.findParent(parentFocusPath, containerPath -> containerPath.childPath().component() == this)
                    .ifPresent(parentPath -> parentPath.applyFocus(false));
        }

        ComponentPath path = getCurrentFocusPath();
        if (path != null) {
            path.applyFocus(false);
        }
    }

    protected void onOpen() {
        if (shouldFocusOnOpen()) {
            ComponentPath path = NavigationUtils.findPath(container, this);
            if (path != null) {
                path.applyFocus(true);
            }
        }
    }

    @Override
    public abstract ScreenRectangle getRectangle();

    protected boolean areCoordinatesInBounds(double x, double y) {
        return io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils.containsPoint(getRectangle(), (int) x, (int) y);
    }

    protected void extractDialogRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable renderable : renderables()) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    protected float getZ() {
        return Math.abs(DEFAULT_ORDER - fidgetz$popoverOrder());
    }

    @Override
    public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isOpen()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, getZ());
            extractDialogRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isOpen()) return false;
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        return shouldCaptureClick()
               // 1.21.1 only, default mouseClicked would iterate through all children, this will stop it
               || isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isOpen()) {
            return false;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_ESCAPE && shouldCloseOnEscape()) {
            setOpen(false);
            return true;
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return isOpen() && (shouldCaptureClick() || areCoordinatesInBounds(mouseX, mouseY));
    }

    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (!isOpen()) return null;

        ComponentPath next = super.nextFocusPath(event);
        if (next != null) return next;

        if (event instanceof FocusNavigationEvent.InitialFocus && !children().isEmpty()) {
            return NavigationUtils.initialFocus(this);
        }

        return shouldCaptureFocus() ? getCurrentFocusPath() : null;
    }

    public void repositionElements() {
    }

    @Override
    public void fidgetz$visitWidgets(WidgetVisitor visitor) {
        visitor.visitWidget(this);
    }

    @Override
    public void fidgetz$visitRenderables(Consumer<Renderable> visitor) {
        visitor.accept(this);
    }

    @Override
    public int fidgetz$popoverOrder() {
        return popoverOrder;
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return componentId;
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return isOpen();
    }
}
