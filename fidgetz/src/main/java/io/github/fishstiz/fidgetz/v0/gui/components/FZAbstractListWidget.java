package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

public abstract class FZAbstractListWidget<E extends FZAbstractListWidget.Entry<E>> extends AbstractListWidget<E> {
    protected static final int DEFAULT_SCROLL_RATE = 10;
    private @Nullable E hovered;
    private ScreenRectangle bounds;
    private int cachedContentHeight;
    private double previousScrollAmount;

    protected FZAbstractListWidget(int x, int y, int width, int height, Component message) {
        super(Minecraft.getInstance(), x, y, width, height, message);
        this.bounds = new ScreenRectangle(getX(), getY(), getWidth(), getHeight());
        setScrollRate(DEFAULT_SCROLL_RATE);
    }

    protected FZAbstractListWidget(Component message) {
        this(0, 0, 0, 0, message);
    }

    protected FZAbstractListWidget() {
        this(CommonComponents.EMPTY);
    }

    @Override
    protected int addEntry(E entry) {
        entry.index = children().size();
        super.addEntry(entry);
        return entry.getIndex();
    }

    @Override
    protected boolean removeEntry(E entry) {
        if (super.removeEntry(entry)) {
            entry.index = -1;
            if (entry == getFocused()) {
                setFocused(null);
            }
            if (entry == getHovered()) {
                setHovered(null);
            }

            return true;
        }
        return false;
    }

    protected void clearEntries() {
        for (Iterator<E> iterator = children().iterator(); iterator.hasNext(); ) {
            E entry = iterator.next();
            entry.index = -1;
            iterator.remove();
        }

        setFocused(null);
        setHovered(null);

        this.cachedContentHeight = 0;

        refreshScrollAmount();
    }

    @Override
    public void setFocused(@Nullable GuiEventListener focused) {
        if (focused == null || focused instanceof Entry) {
            super.setFocused(focused);
        } else {
            focused.setFocused(false);
        }
    }

    @Override
    public @Nullable E getFocused() {
        return super.getFocused();
    }

    protected void setHovered(@Nullable E hovered) {
        if (this.hovered != hovered) {
            if (this.hovered != null) {
                this.hovered.setHovered(false);
            }
            if (hovered != null) {
                hovered.setHovered(true);
            }
            this.hovered = hovered;
        }
    }

    protected @Nullable E getHovered() {
        return this.hovered;
    }

    protected @Nullable E getPreviousEntry(@Nullable E current) {
        if (current != null && current.getIndex() > 0) {
            return this.children().get(current.getIndex() - 1);
        } else if (current == null && !this.children().isEmpty()) {
            return this.children().get(0);
        }
        return null;
    }

    protected @Nullable E getNextEntry(@Nullable E current) {
        if (current != null && current.getIndex() + 1 < this.children().size()) {
            return this.children().get(current.getIndex() + 1);
        } else if (current == null && !this.children().isEmpty()) {
            return this.children().get(0);
        }
        return null;
    }

    protected void extractFocusedRenderState(GuiGraphics graphics, E focused) {
        graphics.renderOutline(focused.getX(), focused.getY(), focused.getWidth(), focused.getHeight(), CommonColors.WHITE);
    }

    @Override
    protected void extractEntriesRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRectangle bounds = getRectangle();
        for (E entry : children()) {
            if (entry.getRectangle().overlaps(bounds)) {
                if (entry.isMouseOver(mouseX, mouseY)) {
                    setHovered(entry);
                } else if (getHovered() == entry) {
                    setHovered(null);
                }
                entry.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        E focused = getFocused();
        if (focused != null) {
            extractFocusedRenderState(graphics, focused);
        }
    }

    @Override
    protected int contentHeight() {
        return cachedContentHeight;
    }

    protected int rowSpacing() {
        return 0;
    }

    private void refreshEntries() {
        int childX = contentLeft();
        int childY = getY() - (int) scrollAmount();
        int childWidth = contentWidth();
        int totalheight = 0;

        for (int i = 0; i < children().size(); i++) {
            E child = children().get(i);
            child.index = i;

            int height = child.getHeight();
            int marginTop = child.getMarginTop();
            int marginBottom = child.getMarginBottom();

            child.setBounds(childX, childY + marginTop, childWidth, height);
            int occupiedHeight = height + marginTop + marginBottom + (i + 1 < children().size() ? rowSpacing() : 0);

            childY += occupiedHeight;
            totalheight += occupiedHeight;
        }

        this.cachedContentHeight = totalheight;

        if (!reserveScrollbarWidth()) {
            int newChildX = contentLeft();
            int newChildWidth = contentWidth();
            if (newChildWidth != childWidth || newChildX != childX) {
                for (E child : children()) {
                    child.setBounds(newChildX, child.getY(), newChildWidth, child.getHeight());
                }
            }
        }
    }

    protected void repositionEntries() {
        refreshEntries();
        refreshScrollAmount();
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(scrollAmount);
        double currentScrollAmount = scrollAmount();
        if (this.previousScrollAmount != currentScrollAmount) {
            // SmoothScrolling is directly modifying the scroll amount field before using the setter to redraw only,
            // so can't check diff with just another local variable as scroll amount is already updated when this is called.
            // https://github.com/SmajloSlovakian/Minecraft-Smooth-Scrolling/blob/54b2fcb0c3a9494781961f571a1426bafb24958f/src/client/java/io/github/smajloslovakian/smoothscroll/mixin/client/Widgets/AbstractScrollAreaMixin.java#L41-L47
            onChangeScrollAmount(currentScrollAmount);
        }
    }

    @Override
    final void onChangeScrollAmount(double scrollAmount) {
        this.previousScrollAmount = scrollAmount;
        refreshEntries();
    }

    protected void scrollToEntry(E entry) {
        int topDelta = entry.getY() - getY() - 2;
        if (topDelta < 0) {
            scroll(topDelta);
        }
        int bottomDelta = getBottom() - entry.getY() - entry.getHeight() - 2;
        if (bottomDelta < 0) {
            scroll(-bottomDelta);
        }
    }

    protected void centerScrollOn(E entry) {
        int y = 0;
        for (E child : this.children()) {
            if (child == entry) {
                y += child.getHeight() / 2;
                break;
            }
            y += child.getHeight();
        }
        setScrollAmount(y - getHeight() / 2.0);
    }

    protected void scroll(double amount) {
        setScrollAmount(scrollAmount() + amount);
    }

    private void updateBounds() {
        this.bounds = new ScreenRectangle(getX(), getY(), getWidth(), getHeight());
    }

    @Override
    public void setX(int x) {
        int previousX = getX();
        super.setX(x);
        updateBounds();
        if (previousX != getX()) {
            repositionEntries();
        }
    }

    @Override
    public void setY(int y) {
        int previousY = getY();
        super.setY(y);
        updateBounds();
        if (previousY != getY()) {
            repositionEntries();
        }
    }

    @Override
    public void setWidth(int width) {
        int previousWidth = getWidth();
        super.setWidth(width);
        updateBounds();
        if (previousWidth != getWidth()) {
            int childWidth = contentWidth();
            for (E child : children()) {
                child.setWidth(childWidth);
            }
        }
    }

    @Override
    public void setHeight(int height) {
        boolean prevScrollbarVisible = scrollbarVisible();
        int previousHeight = getHeight();
        super.setHeight(height);
        updateBounds();
        if (previousHeight != getHeight()) {
            if (prevScrollbarVisible != scrollbarVisible()) {
                repositionEntries();
            } else {
                refreshScrollAmount();
            }
        }
    }

    @Override
    public void setSize(int width, int height) {
        int previousWidth = getWidth();
        int previousHeight = getHeight();
        super.setSize(width, height);
        updateBounds();
        if (previousWidth != getWidth() || previousHeight != getHeight()) {
            repositionEntries();
        }
    }

    @Override
    public ScreenRectangle getRectangle() {
        return bounds;
    }

    protected abstract static class Entry<E extends Entry<E>> extends AbstractListWidget.Entry<E> implements
            LayoutElement,
            Renderable,
            NarratableEntry,
            FZHoverableContainer,
            ContainerEventHandlerPatch {
        private ScreenRectangle bounds;
        int index;
        private boolean focused;
        private boolean hovered;

        protected Entry(int height) {
            this.bounds = new ScreenRectangle(0, 0, 0, height);
        }

        protected Entry() {
            this.bounds = ScreenRectangle.empty();
        }

        protected int getIndex() {
            return index;
        }

        @Override
        public void setX(int x) {
            this.bounds = ScreenRectangleUtils.withLeft(bounds, x);
        }

        @Override
        public void setY(int y) {
            this.bounds = ScreenRectangleUtils.withTop(bounds, y);
        }

        protected void setWidth(int width) {
            this.bounds = ScreenRectangleUtils.withWidth(bounds, width);
        }

        protected void setHeight(int height) {
            this.bounds = ScreenRectangleUtils.withHeight(bounds, height);
        }

        protected void setBounds(int x, int y, int width, int height) {
            this.bounds = new ScreenRectangle(x, y, width, height);
        }

        @Override
        public int getX() {
            return bounds.left();
        }

        @Override
        public int getY() {
            return bounds.top();
        }

        @Override
        public int getWidth() {
            return bounds.width();
        }

        @Override
        public int getHeight() {
            return bounds.height();
        }

        protected int getMarginTop() {
            return 0;
        }

        protected int getMarginBottom() {
            return 0;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
            if (!focused) {
                setFocused(null);
            }
        }

        @Override
        public boolean isFocused() {
            return focused;
        }

        void setHovered(boolean hovered) {
            this.hovered = hovered;
            if (!hovered) {
                fidgetz$setHovered(null);
            }
        }

        public boolean isHovered() {
            return hovered && fidgetz$isHovered();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return ScreenRectangleUtils.containsPoint(getRectangle(), (int) mouseX, (int) mouseY);
        }

        @Override
        public ScreenRectangle getRectangle() {
            return bounds;
        }

        @Override
        public NarrationPriority narrationPriority() {
            if (isFocused()) {
                return NarrationPriority.FOCUSED;
            } else if (isHovered()) {
                return NarrationPriority.HOVERED;
            }
            return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(NarrationElementOutput output) {
        }

        @Override
        public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
        }
    }
}
