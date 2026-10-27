package io.github.fishstiz.fidgetz.v0.gui.screens;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class FZScreen extends Screen implements FZDialogContainer, FZHoverableContainer, FZContextMenu.Source, ContainerEventHandlerPatch {
    protected static final String GLOBAL_CONTEXT_MENU_ID = "FZScreen:FZContextMenu";
    protected final FZDialogManager dialogManager = new FZDialogManager(this);
    protected ScreenRectangle screenRectangle = ScreenRectangle.empty();

    protected FZScreen(Component title) {
        super(title);
    }


    @Override
    protected void init() {
        GuiComponentCollector collector = new GuiComponentCollector();
        collectChildren(collector);
        collector.flushTo(this::addWidget, this::addRenderableOnly);
    }

    protected abstract void collectChildren(GuiComponentCollector collector);

    private void updateHovered(Object child) {
        if (shouldUpdateHovered() && child instanceof FZHoverableElement hoverable) {
            hoverable.fidgetz$setHovered(fidgetz$getHovered() == hoverable);
        }
    }

    @Override
    protected <T extends GuiEventListener & NarratableEntry> T addWidget(T widget) {
        updateHovered(widget);
        return super.addWidget(widget);
    }

    @Override
    protected <T extends Renderable> T addRenderableOnly(T renderable) {
        updateHovered(renderable);
        return super.addRenderableOnly(renderable);
    }

    @Override
    protected <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget) {
        updateHovered(widget);
        return super.addRenderableWidget(widget);
    }

    @Override
    protected void removeWidget(GuiEventListener widget) {
        super.removeWidget(widget);
        if (widget == getFocused()) {
            setFocused(null);
        }
        if (widget == fidgetz$getHovered()) {
            fidgetz$setHovered(null);
        }
    }

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        setFocused(null);
        fidgetz$setHovered(null);
    }

    protected FZContextMenu.Builder buildContextMenu() {
        return FZContextMenu.builder(this).id(GLOBAL_CONTEXT_MENU_ID);
    }

    protected void openContextMenu(double x, double y, boolean focus) {
        dialogManager.put(buildContextMenu()
                .focusOnOpen(focus)
                .buildAndOpen(x, y, fidgetz$collectContextEntries(x, y)));
    }

    protected boolean canOpenContextMenu(double mouseX, double mouseY, int button) {
        return button == InputConstants.MOUSE_BUTTON_RIGHT &&
               dialogManager.get(GLOBAL_CONTEXT_MENU_ID).filter(menu -> menu.isMouseOver(mouseX, mouseY)).isEmpty();
    }

    protected boolean shouldUpdateHovered() {
        return true;
    }

    protected boolean shouldCloseOnEscape() {
        return true;
    }

    @Override
    @Deprecated
    public final boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    @ApiStatus.Internal
    protected void changeFocus(ComponentPath componentPath) {
        ComponentPath currentPath = getCurrentFocusPath();
        // avoid clearing and reapplying focus
        if (currentPath != null && currentPath.equals(componentPath)) {
            componentPath.applyFocus(true);
        } else {
            super.changeFocus(componentPath);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (shouldUpdateHovered()) {
            fidgetz$updateHovered(mouseX, mouseY);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (fidgetz$captureEventForDialogs(mouseX, mouseY, button)) {
            return true;
        }

        boolean clicked = ContainerEventHandlerPatch.super.mouseClicked(mouseX, mouseY, button);

        if (canOpenContextMenu(mouseX, mouseY, button)) {
            openContextMenu(mouseX, mouseY, !clicked);
        }

        return clicked;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (fidgetz$captureEventForDialogs(keyCode) || super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_ESCAPE && shouldCloseOnEscape()) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) {
            setFocused(null);
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (getFocused() != focused) {
            super.setFocused(focused);
        }
    }

    @Override
    public List<? extends FZDialog> fidgetz$Dialogs() {
        return dialogManager.dialogs();
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(screenRectangle, 0, 0, width, height)) {
            this.screenRectangle = super.getRectangle();
        }
        return screenRectangle;
    }
}
