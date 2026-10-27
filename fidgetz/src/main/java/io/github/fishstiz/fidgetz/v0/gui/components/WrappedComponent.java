package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

public class WrappedComponent extends AbstractWidget implements ContainerEventHandler, FZComponent, FZHoverableElement {
    private List<AbstractWidget> children;
    private AbstractWidget widget;
    private boolean dragging;

    public WrappedComponent(AbstractWidget widget) {
        super(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight(), widget.getMessage());
        this.widget = widget;
        this.children = List.of(widget);
    }

    public void set(AbstractWidget widget) {
        if (this.widget != widget) {
            boolean focused = isFocused();
            ScreenRectangle rectangle = getRectangle();
            this.widget = widget;
            this.children = List.of(widget);
            setRectangle(rectangle.width(), rectangle.height(), rectangle.left(), rectangle.top());
            setFocused(focused ? widget : null);
        }
    }

    public AbstractWidget get() {
        return this.widget;
    }

    public static WrappedComponent bind(String key, FZRef<? extends AbstractWidget> ref) {
        WrappedComponent wrapped = new WrappedComponent(ref.value());
        ref.subscribe(key, wrapped::set);
        return wrapped;
    }

    @Override
    public List<AbstractWidget> children() {
        return children;
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
        return widget.isFocused() ? widget : null;
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (focused == null) {
            widget.setFocused(false);
        } else if (focused == widget) {
            widget.setFocused(true);
        }
    }

    // delegate to widget

    @Override
    public @Nullable String fidgetz$componentId() {
        return widget instanceof FZComponent component ? component.fidgetz$componentId() : null;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        widget.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        widget.updateNarration(output);
    }

    @Override
    public int getHeight() {
        return widget.getHeight();
    }

    @Override
    public void setTooltip(@Nullable Tooltip tooltip) {
        super.setTooltip(tooltip);
        widget.setTooltip(tooltip);
    }

    public void setTooltipDelay(Duration delay) {
        int delayMillis = (int) Math.max(0L, Math.min((long) Integer.MAX_VALUE, delay.toMillis()));
        super.setTooltipDelay(delayMillis);
        widget.setTooltipDelay(delayMillis);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        widget.onClick(mouseX, mouseY);
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        widget.onRelease(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return widget.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return widget.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        return widget.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return ComponentPath.path(this, widget.nextFocusPath(navigationEvent));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return widget.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        widget.playDownSound(soundManager);
    }

    @Override
    public int getWidth() {
        return widget.getWidth();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        widget.setWidth(width);
    }

    public void setHeight(int height) {
        WidgetSizeUtils.setHeight(this, height);
        WidgetSizeUtils.setHeight(widget, height);
    }

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        widget.setAlpha(alpha);
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        widget.setMessage(message);
    }

    @Override
    public Component getMessage() {
        return widget.getMessage();
    }

    @Override
    public boolean isFocused() {
        return widget.isFocused();
    }

    @Override
    public boolean isHovered() {
        return widget.isHovered();
    }

    @Override
    public boolean isHoveredOrFocused() {
        return widget.isHoveredOrFocused();
    }

    @Override
    public boolean isActive() {
        return widget.isActive();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        widget.setFocused(focused);
    }

    @Override
    public NarrationPriority narrationPriority() {
        return widget.narrationPriority();
    }

    @Override
    public int getX() {
        return widget.getX();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        widget.setX(x);
    }

    @Override
    public int getY() {
        return widget.getY();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        widget.setY(y);
    }

    public int getRight() {
        return widget.getX() + widget.getWidth();
    }

    public int getBottom() {
        return widget.getY() + widget.getHeight();
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    @Override
    public ScreenRectangle getRectangle() {
        return widget.getRectangle();
    }

    public void setRectangle(int width, int height, int x, int y) {
        setSize(width, height);
        setX(x);
        setY(y);
    }

    @Override
    public boolean fidgetz$isVisible() {
        return widget.visible;
    }

    @Override
    public int getTabOrderGroup() {
        return widget.getTabOrderGroup();
    }

    @Override
    public void setTabOrderGroup(int tabOrderGroup) {
        super.setTabOrderGroup(tabOrderGroup);
        widget.setTabOrderGroup(tabOrderGroup);
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        widget.setPosition(x, y);
    }

    @Override
    public void mouseMoved(double x, double y) {
        widget.mouseMoved(x, y);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollY) {
        return widget.mouseScrolled(x, y, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return widget.charTyped(codePoint, modifiers);
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath() {
        return ComponentPath.path(this, widget.getCurrentFocusPath());
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return !(widget instanceof FZComponent component) || component.fidgetz$shouldTakeFocusAfterInteraction();
    }
}
