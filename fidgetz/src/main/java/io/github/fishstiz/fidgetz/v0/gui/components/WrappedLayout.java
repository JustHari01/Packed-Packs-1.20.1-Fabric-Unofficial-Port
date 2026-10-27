package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexElement;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WrappedLayout<T extends Layout> extends AbstractWidget implements
        ContainerEventHandlerPatch,
        FZComponent,
        FZContextMenu.Source,
        FZHoverableContainer {
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<Renderable> renderables = new ArrayList<>();
    protected T layout;
    private ScreenRectangle cachedBounds = ScreenRectangle.empty();
    private @Nullable GuiEventListener focused;
    private boolean dragging;
    protected ScreenRectangle padding = ScreenRectangle.empty();

    protected WrappedLayout(T layout) {
        super(0, 0, 0, 0, CommonComponents.EMPTY);
        this.layout = layout;
    }

    public static <T extends Layout> WrappedLayout<T> wrap(T layout) {
        WrappedLayout<T> wrappedLayout = new WrappedLayout<>(layout);
        wrappedLayout.buildWidgets();
        return wrappedLayout;
    }

    protected void buildWidgets(GuiComponentCollector collector) {
    }

    protected final void buildWidgets() {
        GuiComponentCollector collector = new GuiComponentCollector();
        buildWidgets(collector);
        this.layout.visitWidgets(collector::renderableWidget);
        collector.flushTo(this::addWidget, this::addRenderableOnly);

        if (!children.isEmpty() || !renderables.isEmpty()) {
            arrangeElements();
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Renderable child : renderables) {
            child.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    public void setPadding(int left, int top, int right, int bottom) {
        if (this.padding.left() != left
            || this.padding.top() != top
            || this.padding.right() != right
            || this.padding.bottom() != bottom) {
            this.padding = ScreenRectangleUtils.insets(left, top, right, bottom);
            setSize(getWidth(), getHeight());
        }
    }

    public void setPadding(int padding) {
        setPadding(padding, padding, padding, padding);
    }

    public WrappedLayout<T> padding(int left, int top, int right, int bottom) {
        setPadding(left, top, right, bottom);
        return this;
    }

    public WrappedLayout<T> padding(int padding) {
        return padding(padding, padding, padding, padding);
    }

    private void updateBounds() {
        this.width = layout.getWidth() + this.padding.left() + this.padding.right();
        this.height = layout.getHeight() + this.padding.top() + this.padding.bottom();
        this.cachedBounds = getRectangle();
    }

    @Override
    public void setWidth(int width) {
        if (layout instanceof FZFlexElement flexLayout) {
            flexLayout.fidgetz$setWidth(width - this.padding.left() - this.padding.right());
        }
        updateBounds();
    }

    public void setHeight(int height) {
        if (layout instanceof FZFlexElement flexLayout) {
            flexLayout.fidgetz$setHeight(height - this.padding.top() - this.padding.bottom());
        }
        updateBounds();
    }

    public void setSize(int width, int height) {
        if (layout instanceof FZFlexElement flexLayout) {
            flexLayout.fidgetz$setSize(
                    width - this.padding.left() - this.padding.right(),
                    height - this.padding.top() - this.padding.bottom()
            );
        }
        updateBounds();
    }

    @Override
    public void setX(final int x) {
        layout.setX(x + this.padding.left());
        super.setX(x);
        updateBounds();
    }

    @Override
    public void setY(final int y) {
        layout.setY(y + this.padding.top());
        super.setY(y);
        updateBounds();
    }

    protected void repositionLayout() {
        layout.setPosition(getX() + this.padding.left(), getY() + this.padding.top());
    }

    protected void arrangeElements() {
        layout.arrangeElements();
        repositionLayout();
        updateBounds();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    protected void clearWidgets() {
        for (Iterator<GuiEventListener> it = children.iterator(); it.hasNext(); ) {
            GuiEventListener widget = it.next();

            if (getFocused() == widget) {
                setFocused(null);
            }
            if (fidgetz$getHovered() == widget) {
                fidgetz$setHovered(null);
            }

            it.remove();
        }

        renderables.clear();
    }

    protected void addWidget(GuiEventListener widget) {
        children.add(widget);
    }

    protected void addRenderableOnly(Renderable renderable) {
        renderables.add(renderable);
    }

    protected void removeWidget(GuiEventListener widget) {
        children.remove(widget);
        if (widget instanceof Renderable renderable) {
            renderables.remove(renderable);
        }
        if (getFocused() == widget) {
            setFocused(null);
        }
        if (fidgetz$getHovered() == widget) {
            fidgetz$setHovered(null);
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return children;
    }

    @Override
    public boolean isDragging() {
        return this.dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public @Nullable GuiEventListener getFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        if (!focused) {
            setFocused(null);
        }
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (this.focused != focused) {
            if (this.focused != null) {
                this.focused.setFocused(false);
            }

            if (focused != null) {
                focused.setFocused(true);
            }

            this.focused = focused;
        }
    }

    @Override
    public boolean isFocused() {
        return ContainerEventHandlerPatch.super.isFocused();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (super.isMouseOver(mouseX, mouseY)) {
            return true;
        }

        for (GuiEventListener child : children) {
            if (child.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return ContainerEventHandlerPatch.super.nextFocusPath(navigationEvent);
    }

    @Override
    public @Nullable ComponentPath getCurrentFocusPath() {
        return ContainerEventHandlerPatch.super.getCurrentFocusPath();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ContainerEventHandlerPatch.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        return ContainerEventHandlerPatch.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, final double dx, final double dy) {
        super.mouseDragged(mouseX, mouseY, button, dx, dy);
        return ContainerEventHandlerPatch.super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(this.cachedBounds, this)) {
            this.cachedBounds = super.getRectangle();
        }
        return this.cachedBounds;
    }
}
