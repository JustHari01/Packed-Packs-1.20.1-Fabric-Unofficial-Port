package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.sounds.SoundManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Bridges the 1.20.1 selection-list API into layouts that register widgets. */
public final class ListWidgetAdapter extends AbstractWidget implements ContainerEventHandlerPatch,
        FZComponent, FZContextMenu.Source, FZHoverableContainer {
    private final AbstractListWidget<?> list;
    private boolean dragging;

    public ListWidgetAdapter(AbstractListWidget<?> list) {
        super(list.getX(), list.getY(), list.getWidth(), list.getHeight(), CommonComponents.EMPTY);
        this.list = list;
        this.active = list.isActive();
        this.visible = list.isVisible();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return list.children();
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return list.getFocused();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        list.setFocused(focused);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        list.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return list.isFocused();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        list.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.visible = list.isVisible();
        this.active = list.isActive();
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        list.updateNarration(output);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return list.narrationPriority();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return list.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return list.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return list.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return list.mouseScrolled(mouseX, mouseY, scrollY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        list.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return list.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return list.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return list.charTyped(codePoint, modifiers);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return ComponentPath.path(this, list.nextFocusPath(event));
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath() {
        return ComponentPath.path(this, list.getCurrentFocusPath());
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return list.isMouseOver(mouseX, mouseY);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return list.getRectangle();
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        if (getFocused() instanceof AbstractWidget widget) {
            widget.playDownSound(soundManager);
        }
    }

    @Override
    public int getX() {
        return list.getX();
    }

    @Override
    public int getY() {
        return list.getY();
    }

    @Override
    public int getWidth() {
        return list.getWidth();
    }

    @Override
    public int getHeight() {
        return list.getHeight();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        list.setX(x);
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        list.setY(y);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        list.setWidth(width);
    }

    public void setHeight(int height) {
        WidgetSizeUtils.setHeight(this, height);
        list.setHeight(height);
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    @Override
    public boolean fidgetz$isVisible() {
        return list.isVisible();
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return list instanceof FZComponent component ? component.fidgetz$componentId() : null;
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return !(list instanceof FZComponent component) || component.fidgetz$shouldTakeFocusAfterInteraction();
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        if (list instanceof FZContextMenu.Source source) {
            source.fidgetz$updateContextEntries(x, y, collector);
        }
    }

    @Override
    public @Nullable GuiEventListener fidgetz$getHovered() {
        return list instanceof FZHoverableContainer container ? container.fidgetz$getHovered() : null;
    }

    @Override
    public void fidgetz$setHovered(@Nullable GuiEventListener hovered) {
        if (list instanceof FZHoverableContainer container) {
            container.fidgetz$setHovered(hovered);
        }
    }
}
