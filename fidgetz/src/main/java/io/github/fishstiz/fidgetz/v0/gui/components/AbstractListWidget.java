package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.gui.components.events.ScrollableContainer;
import io.github.fishstiz.fidgetz.v0.inject.mixins.access.AbstractSelectionListAccess;
import io.github.fishstiz.fidgetz.v0.utils.MathUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
// extend AbstractSelectionList for smooth scrolling compat even though we deprecate a lot of methods and would prefer
// AbstractScrollWidget or custom impl. Avoid on 1.21.1 if possible
public abstract class AbstractListWidget<T extends AbstractListWidget.Entry<T>> extends AbstractSelectionList<T> implements LayoutElement, ScrollableContainer, ContainerEventHandlerPatch {
    protected static final int DEFAULT_MAX_CONTENT_WIDTH = 270;
    protected static final int DEFAULT_SCROLL_RATE = 10;
    protected static final int SEPARATOR_HEIGHT = 2;
    protected boolean active = true;
    protected boolean visible = true;
    private static final ResourceLocation BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
    private static final ResourceLocation INWORLD_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");
    private static final int SCROLLBAR_WIDTH = 6;

    protected AbstractListWidget(Minecraft minecraft, int x, int y, int width, int height, Component message) {
        super(minecraft, width, height, y, y + height, DEFAULT_SCROLL_RATE * 2);
        setLeftPos(x);
        setRenderBackground(false);
        setRenderTopAndBottom(false);
    }

    protected AbstractListWidget(int x, int y, int width, int height, Component message) {
        this(Minecraft.getInstance(), x, y, width, height, message);
    }

    @Override
    @Deprecated
    protected final void renderBackground(GuiGraphics graphics) {
        extractBackgroundRenderState(graphics, 0, 0, 0);
    }

    protected void extractBackgroundRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation background = minecraft.level == null ? BACKGROUND : INWORLD_BACKGROUND;
        RenderSystem.enableBlend();
        graphics.blit(
                background,
                getX(),
                getY(),
                getRight(),
                getBottom() + (int) scrollAmount(),
                getWidth(),
                getHeight(),
                32,
                32
        );
        RenderSystem.disableBlend();
    }

    protected void extractSeparatorsRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int color = 0x66FFFFFF;
        graphics.fill(getX(), getY() - SEPARATOR_HEIGHT, getRight(), getY(), color);
        graphics.fill(getX(), getBottom(), getRight(), getBottom() + SEPARATOR_HEIGHT, color);
    }

    @Override
    @Deprecated
    protected final void renderList(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.enableScissor(getX(), getY(), getRight(), getBottom());
        extractEntriesRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
        extractSeparatorsRenderState(graphics, mouseX, mouseY, partialTick);
    }

    protected abstract void extractEntriesRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    @Override
    @Deprecated
    protected final void renderDecorations(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    // workaround for smooth scrolling which directly modifies scrollAmount instead of calling the setter.
    // it assumes that calls to setScrollAmount should snap
    // https://github.com/SmajloSlovakian/Minecraft-Smooth-Scrolling/blob/1.21.1/src/main/java/smsk/smoothscroll/mixin/EntryListWidgetMixin.java
    void onChangeScrollAmount(double scrollAmount) {
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        double lastScrollAmount = scrollAmount();
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        double newScrollAmount = scrollAmount();
        if (lastScrollAmount != newScrollAmount) {
            onChangeScrollAmount(newScrollAmount);
        }
    }

    protected int contentPaddingLeft() {
        return 0;
    }

    protected int contentPaddingRight() {
        return 0;
    }

    public int getX() {
        return x0;
    }

    public int getY() {
        return y0;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRight() {
        return x1;
    }

    public int getBottom() {
        return y1;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setX(int x) {
        setLeftPos(x);
    }

    public void setY(int y) {
        y0 = y;
        y1 = y + height;
    }

    public void setWidth(int width) {
        this.width = width;
        x1 = x0 + width;
    }

    public void setHeight(int height) {
        this.height = height;
        y1 = y0 + height;
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
        for (T entry : children()) {
            if (entry instanceof LayoutElement element) {
                element.visitWidgets(widgetVisitor);
            }
        }
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        GuiEventListener focused = getFocused();
        if (focused instanceof NarratableEntry narratable) {
            narratable.updateNarration(output);
        }
    }

    protected int maxContentWidth() {
        return DEFAULT_MAX_CONTENT_WIDTH;
    }

    protected int contentWidth() {
        int contentWidth = getWidth() - scrollbarReserve() - contentPaddingLeft() - contentPaddingRight();
        return MathUtils.optionalMin(contentWidth, maxContentWidth());
    }

    @Override
    @Deprecated
    public final int getRowWidth() {
        return contentWidth();
    }

    protected abstract int contentHeight();

    @Override
    @Deprecated
    protected final int getMaxPosition() {
        return contentHeight();
    }

    protected final int contentLeft() {
        return Math.min(
                getX() + (getWidth() / 2 - contentWidth() / 2) + contentPaddingLeft() - contentPaddingRight(),
                getRight() - scrollbarReserve() - contentWidth()
        );
    }

    @Override
    @Deprecated
    public final int getRowLeft() {
        return contentLeft();
    }

    @Override
    @Deprecated
    public final int getRowRight() {
        return contentLeft() + contentWidth();
    }

    @Override
    @Deprecated
    protected final int getRowTop(int index) {
        Entry<T> entry = index >= 0 && index < children().size() ? children().get(index) : null;
        return entry == null ? 0 : entry.getRectangle().top();
    }

    @Override
    @Deprecated
    protected final int getRowBottom(int index) {
        Entry<T> entry = index >= 0 && index < children().size() ? children().get(index) : null;
        return entry == null ? 0 : entry.getRectangle().bottom();
    }

    protected final int scrollbarWidth() {
        return SCROLLBAR_WIDTH;
    }

    protected boolean scrollbarVisible() {
        return contentHeight() > getHeight();
    }

    protected boolean reserveScrollbarWidth() {
        return false;
    }

    protected int scrollbarReserve() {
        return reserveScrollbarWidth() || scrollbarVisible() ? scrollbarWidth() : 0;
    }

    protected int scrollBarX() {
        return Math.min(contentLeft() + contentWidth() + contentPaddingRight(), getRight() - scrollbarWidth());
    }

    @Override
    @Deprecated
    protected final int getScrollbarPosition() {
        return scrollBarX();
    }

    @Override
    @Deprecated
    public final double getScrollAmount() {
        return super.getScrollAmount();
    }

    @Override
    public double scrollAmount() {
        return super.getScrollAmount();
    }

    public void refreshScrollAmount() {
        setScrollAmount(scrollAmount());
    }

    public void setScrollRate(double scrollRate) {
        ((AbstractSelectionListAccess) this).fidgetz$setScrollRate(scrollRate);
    }

    @Override
    public final double scrollRate() {
        return (double) itemHeight / 2;
    }

    protected boolean isOverScrollbar(double x, double y) {
        return isMouseOver(x, y) && x >= scrollBarX() && x <= scrollBarX() + scrollbarWidth() && y >= getY() && y < getBottom();
    }

    protected boolean scrollable() {
        return maxScrollAmount() > 0;
    }

    public int maxScrollAmount() {
        return Math.max(0, this.contentHeight() - this.height);
    }

    @Override
    @Deprecated
    public final int getMaxScroll() {
        return maxScrollAmount();
    }

    protected int scrollerHeight() {
        return Mth.clamp((int) ((float) (this.height * this.height) / this.contentHeight()), 32, this.height - 8);
    }

    @Override
    @Deprecated
    protected final void ensureVisible(T entry) {
    }

    public boolean updateScrolling(double mouseX, double mouseY, int button) {
        updateScrollingState(mouseX, mouseY, button);
        return ((AbstractSelectionListAccess) this).fidgetz$getScrolling();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            setFocused(null);
        }
    }

    @Override
    @Deprecated
    public final @Nullable T getSelected() {
        return null;
    }

    @Override
    @Deprecated
    public final void setSelected(@Nullable T selected) {
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (getFocused() != focused) {
            super.setFocused(focused);
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        return addScrollEffectOnFocus(navigationEvent, super.nextFocusPath(navigationEvent));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (((AbstractSelectionListAccess) this).fidgetz$getScrolling()) {
            if (mouseY < this.getY()) {
                this.setScrollAmount(0.0);
            } else if (mouseY > this.getBottom()) {
                this.setScrollAmount(this.maxScrollAmount());
            } else {
                double max = Math.max(1, this.maxScrollAmount());
                int barHeight = this.scrollerHeight();
                double yDragScale = Math.max(1.0, max / (this.height - barHeight));
                this.setScrollAmount(this.scrollAmount() + dragY * yDragScale);
            }

            return true;
        }

        return ContainerEventHandlerPatch.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (isMouseOver(mx, my) && getChildAt(mx, my).filter(child -> child.mouseScrolled(mx, my, scrollY)).isPresent()) {
            return true;
        }
        return super.mouseScrolled(mx, my, scrollY) && scrollable();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean scrolling = updateScrolling(mouseX, mouseY, button);
        return ContainerEventHandlerPatch.super.mouseClicked(mouseX, mouseY, button) || scrolling;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        updateScrollingState(0, 0, -1); // reset scrolling state
        return super.mouseReleased(mouseX, mouseY, button);
    }

    protected abstract static class Entry<T extends Entry<T>> extends AbstractSelectionList.Entry<T> implements ContainerEventHandlerPatch, FZHoverableContainer {
        private @Nullable GuiEventListener focused;
        private boolean isDragging;

        @Override
        public boolean isDragging() {
            return this.isDragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.isDragging = dragging;
        }

        @Override
        public @Nullable GuiEventListener getFocused() {
            return this.focused;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener listener) {
            if (getFocused() != listener) {
                if (this.focused != null) {
                    this.focused.setFocused(false);
                }
                if (listener != null) {
                    listener.setFocused(true);
                }
                this.focused = listener;
            }
        }

        @Override
        public void setFocused(boolean focused) {
            if (!focused) {
                setFocused(null);
            }
        }

        @Override
        @Deprecated
        public final void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            return ContainerEventHandlerPatch.super.mouseReleased(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return ContainerEventHandlerPatch.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
            return ContainerEventHandlerPatch.super.mouseScrolled(mouseX, mouseY, scrollY);
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils.containsPoint(
                    getRectangle(), (int) mouseX, (int) mouseY
            );
        }
    }
}
