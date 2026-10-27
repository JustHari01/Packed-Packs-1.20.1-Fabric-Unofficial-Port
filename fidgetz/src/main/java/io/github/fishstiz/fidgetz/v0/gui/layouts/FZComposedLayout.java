package io.github.fishstiz.fidgetz.v0.gui.layouts;

import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FZComposedLayout extends ComposedLayout {
    private ScreenRectangle padding = ScreenRectangle.empty();
    private @Nullable Supplier<ScreenRectangle> maxBounds;
    private @Nullable Supplier<ScreenRectangle> alignmentBounds;
    private float alignX;
    private float alignY;

    FZComposedLayout(Layout base) {
        super(base);
    }

    public static FZComposedLayout compose(Layout base) {
        return new FZComposedLayout(base);
    }

    public static Contained contain(Supplier<ScreenRectangle> area, Layout base) {
        return new Contained(area, base);
    }

    public static Contained contain(LayoutElement container, Layout base) {
        return new Contained(container::getRectangle, base);
    }

    public static Contained contain(GuiEventListener container, Layout base) {
        return new Contained(container::getRectangle, base);
    }

    public FZComposedLayout nest() {
        return new FZComposedLayout(this);
    }

    public FZComposedLayout padding(int padding) {
        this.padding = ScreenRectangleUtils.insets(padding);
        return this;
    }

    public FZComposedLayout padding(int left, int top, int right, int bottom) {
        padding = ScreenRectangleUtils.insets(left, top, right, bottom);
        return this;
    }

    public FZComposedLayout align(Supplier<ScreenRectangle> alignmentBounds, float x, float y) {
        this.alignmentBounds = alignmentBounds;
        alignX = net.minecraft.util.Mth.clamp(x, 0f, 1f);
        alignY = net.minecraft.util.Mth.clamp(y, 0f, 1f);
        return this;
    }

    public FZComposedLayout align(GuiEventListener container, float x, float y) {
        return align(container::getRectangle, x, y);
    }

    public FZComposedLayout align(LayoutElement container, float x, float y) {
        return align(container::getRectangle, x, y);
    }

    public FZComposedLayout center(LayoutElement container) {
        return align(container, 0.5f, 0.5f);
    }

    public FZComposedLayout center(GuiEventListener container) {
        return align(container, 0.5f, 0.5f);
    }

    public FZComposedLayout clamp(Supplier<ScreenRectangle> maxBounds) {
        this.maxBounds = maxBounds;
        return this;
    }

    public FZComposedLayout clamp(GuiEventListener container) {
        return clamp(container::getRectangle);
    }

    public FZComposedLayout clamp(LayoutElement container) {
        return clamp(container::getRectangle);
    }

    public FZComposedLayout scrollable(Supplier<ScreenRectangle> maxScrollArea) {
        composed = FZScrollableLayout.from(maxScrollArea, composed);
        return this;
    }

    public FZComposedLayout scrollable(GuiEventListener container) {
        return scrollable(container::getRectangle);
    }

    public FZComposedLayout scrollable(LayoutElement container) {
        return scrollable(container::getRectangle);
    }

    public FZComposedLayout scrollable(int maxHeight) {
        composed = FZScrollableLayout.from(maxHeight, composed);
        return this;
    }

    public FZComposedLayout scrollable(Supplier<ScreenRectangle> maxScrollArea, Consumer<FZScrollableLayout> configurator) {
        FZScrollableLayout scrollable = FZScrollableLayout.from(maxScrollArea, composed);
        configurator.accept(scrollable);
        composed = scrollable;
        return this;
    }

    public FZComposedLayout scrollable(GuiEventListener container, Consumer<FZScrollableLayout> configurator) {
        return scrollable(container::getRectangle, configurator);
    }

    public FZComposedLayout scrollable(LayoutElement container, Consumer<FZScrollableLayout> configurator) {
        return scrollable(container::getRectangle, configurator);
    }

    public FZComposedLayout scrollable(int maxHeight, Consumer<FZScrollableLayout> configurator) {
        FZScrollableLayout scrollable = FZScrollableLayout.from(maxHeight, composed);
        configurator.accept(scrollable);
        composed = scrollable;
        return this;
    }

    public FZScrollableLayout toScrollable(Supplier<ScreenRectangle> maxScrollArea) {
        return FZScrollableLayout.from(maxScrollArea, this);
    }

    public FZScrollableLayout toScrollable(GuiEventListener container) {
        return toScrollable(container::getRectangle);
    }

    public FZScrollableLayout toScrollable(LayoutElement container) {
        return toScrollable(container::getRectangle);
    }

    public FZScrollableLayout toScrollable(int maxHeight) {
        return FZScrollableLayout.from(maxHeight, composed);
    }

    public FZScrollableLayout toScrollable() {
        return FZScrollableLayout.from(composed);
    }

    @Override
    public void fidgetz$setWidth(int width) {
        if (composed instanceof FZFlexElement flexElement) {
            flexElement.fidgetz$setWidth(width - (padding.left() + padding.right()));
        }
    }

    @Override
    public void fidgetz$setHeight(int height) {
        if (composed instanceof FZFlexElement flexElement) {
            flexElement.fidgetz$setHeight(height - (padding.top() + padding.bottom()));
        }
    }

    @Override
    public void fidgetz$setSize(int width, int height) {
        if (composed instanceof FZFlexElement flexElement) {
            flexElement.fidgetz$setSize(width - (padding.left() + padding.right()), height - (padding.top() + padding.bottom()));
        }
    }

    @Override
    public void setX(int x) {
        int clampedX = x + padding.left();
        if (maxBounds != null) {
            ScreenRectangle bounds = maxBounds.get();
            int minX = bounds.left() + padding.left();
            int maxX = Math.max(bounds.left() + padding.left(), bounds.right() - padding.right() - composed.getWidth());
            clampedX = net.minecraft.util.Mth.clamp(clampedX, minX, maxX);
        }
        composed.setX(clampedX);
    }

    @Override
    public void setY(int y) {
        int clampedY = y + padding.top();
        if (maxBounds != null) {
            ScreenRectangle bounds = maxBounds.get();
            int minY = bounds.top() + padding.top();
            int maxY = Math.max(bounds.top() + padding.top(), bounds.bottom() - padding.bottom() - composed.getHeight());
            clampedY = net.minecraft.util.Mth.clamp(clampedY, minY, maxY);
        }
        composed.setY(clampedY);
    }

    @Override
    public void setPosition(int x, int y) {
        setX(x);
        setY(y);
    }

    @Override
    public int getX() {
        return composed.getX() - padding.left();
    }

    @Override
    public int getY() {
        return composed.getY() - padding.top();
    }

    @Override
    public int getWidth() {
        return composed.getWidth() + padding.left() + padding.right();
    }

    @Override
    public int getHeight() {
        return composed.getHeight() + padding.top() + padding.bottom();
    }

    @Override
    public ScreenRectangle getRectangle() {
        return ScreenRectangleUtils.expand(composed.getRectangle(), padding);
    }

    @Override
    public void arrangeElements() {
        composed.arrangeElements();

        if (alignmentBounds != null) {
            ScreenRectangle area = alignmentBounds.get();
            ScreenRectangle inset = new ScreenRectangle(
                    area.left() + padding.left(),
                    area.top() + padding.top(),
                    area.width() - padding.left() - padding.right(),
                    area.height() - padding.top() - padding.bottom()
            );
            FrameLayout.alignInRectangle(composed, inset, alignX, alignY);
        } else {
            composed.setPosition(getX() + padding.left(), getY() + padding.top());
        }

        if (maxBounds != null) {
            ScreenRectangle bounds = maxBounds.get();
            fidgetz$setSize(net.minecraft.util.Mth.clamp(getWidth(), 0, bounds.width()), net.minecraft.util.Mth.clamp(getHeight(), 0, bounds.height()));
            setPosition(composed.getX() - padding.left(), composed.getY() - padding.top());
        }
    }

    public static final class Contained {
        private final Supplier<ScreenRectangle> screenArea;
        private final FZComposedLayout composed;

        private Contained(Supplier<ScreenRectangle> screenArea, Layout base) {
            this.composed = new FZComposedLayout(base);
            this.screenArea = screenArea;
        }

        public Contained padding(int padding) {
            composed.padding(padding);
            return this;
        }

        public Contained padding(int left, int top, int right, int bottom) {
            composed.padding(left, top, right, bottom);
            return this;
        }

        public Contained align(float x, float y) {
            composed.align(screenArea, x, y);
            return this;
        }

        public Contained center() {
            return align(0.5f, 0.5f);
        }

        public Contained clamp() {
            composed.clamp(screenArea);
            return this;
        }

        public Contained arrange() {
            composed.arrangeElements();
            return this;
        }

        public Contained scrollable() {
            composed.scrollable(screenArea);
            return this;
        }

        public Contained scrollable(Consumer<FZScrollableLayout> configurator) {
            composed.scrollable(screenArea, configurator);
            return this;
        }

        public Contained visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
            composed.visitWidgets(widgetVisitor);
            return this;
        }

        public Contained nest() {
            return new Contained(screenArea, composed.nest());
        }

        public FZComposedLayout get() {
            return composed;
        }
    }
}