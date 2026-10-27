package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import io.github.fishstiz.fidgetz.v0.utils.TriState;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public class FZLayoutList extends AbstractListWidget<FZLayoutList.Entry> implements Layout, FZComponent, FZContextMenu.Source {
    protected static final int DEFAULT_WIDTH = 300;
    protected static final int DEFAULT_HEIGHT = 150;
    protected static final int DEFAULT_SCROLL_RATE = 10;
    private final List<Renderable> renderables = new ArrayList<>();
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private FZFlexLayout layout;
    private FZKeyed<Consumer<RefreshEvent>> entryInitializer = FZKeyed.selfKey(FunctionUtils.nopConsumer());
    private int maxContentWidth = DEFAULT_MAX_CONTENT_WIDTH;
    private int contentPaddingLeft;
    private int contentPaddingRight;
    private boolean reserveScrollbarWidth;
    private ScreenRectangle bounds;

    protected FZLayoutList(int width, int height, Component message) {
        super(0, 0, width, height, message);
        setScrollRate(DEFAULT_SCROLL_RATE);
        layout = FZFlexLayout.vertical();
        bounds = super.getRectangle();
    }

    protected FZLayoutList() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY);
    }

    protected void collectEntries(RefreshEvent event) {
        entryInitializer.value().accept(event);
    }

    public final void refreshEntries() {
        Entry lastFocused = getFocused();
        String lastFocusedId = null;

        for (Iterator<Entry> it = children().listIterator(); it.hasNext(); ) {
            Entry entry = it.next();
            it.remove();
            if (lastFocused != null && entry == lastFocused) {
                lastFocusedId = entry.id;
            }
        }

        renderables.clear();

        FZFlexLayout newLayout = FZFlexLayout.vertical().maxWidth(contentWidth()).maxHeight(0);

        MutableBoolean focusUnresolved = new MutableBoolean(true);
        MutableInt count = new MutableInt(0);
        int hashCode = hashCode();

        WidgetVisitor widgetSink = getWidgetSink(lastFocused, lastFocusedId, focusUnresolved, count, hashCode);
        RefreshEvent refreshEvent = new RefreshEvent(widgetSink, newLayout);
        collectEntries(refreshEvent);
        refreshEvent.flushComponents();

        if (focusUnresolved.booleanValue()) setFocused(null);

        this.layout = newLayout;
        arrangeElements();
    }

    private WidgetVisitor getWidgetSink(
            @Nullable GuiEventListener lastFocused,
            @Nullable String lastFocusedId,
            MutableBoolean focusUnresolved,
            MutableInt count,
            int hashCode
    ) {
        return new WidgetVisitor() {
            @Override
            public <T extends GuiEventListener & NarratableEntry> void visitWidget(T widget) {
                String id = widget instanceof FZComponent component ? component.fidgetz$componentId() : null;
                if (id == null)
                    id = "FZList@%s-element-%s-%s".formatted(hashCode, count.intValue(), widget.getClass().getName());

                Entry entry = new Entry(id, widget);
                addEntry(entry);
                count.increment();

                if (widget == lastFocused || (Objects.equals(id, lastFocusedId) && focusUnresolved.booleanValue())) {
                    setFocused(entry);
                    focusUnresolved.setValue(false);
                }
            }
        };
    }

    protected int maxContentWidth() {
        return maxContentWidth;
    }

    protected int contentPaddingLeft() {
        return contentPaddingLeft;
    }

    protected int contentPaddingRight() {
        return contentPaddingRight;
    }

    protected boolean reserveScrollbarWidth() {
        return reserveScrollbarWidth;
    }

    @Override
    protected int contentHeight() {
        return layout.getHeight();
    }

    @Override
    protected int scrollBarX() {
        return Math.min(layout.getX() + layout.getWidth() + contentPaddingRight(), getRight() - scrollbarWidth());
    }

    @Override
    public void arrangeElements() {
        int contentWidth = contentWidth();
        int contentLeft = contentLeft();
        layout.maxWidth(contentWidth);
        layout.arrangeElements();
        layout.setPosition(contentLeft(), getY());

        if (!reserveScrollbarWidth()) {
            int newContentWidth = contentWidth();
            int newContentLeft = contentLeft();
            if (contentWidth != newContentWidth || contentLeft != newContentLeft) {
                layout.fidgetz$setWidth(newContentWidth);
            }
        }

        refreshScrollAmount();
    }

    @Override
    protected void extractEntriesRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRectangle bounds = getRectangle();
        for (Renderable renderable : renderables) {
            if (!(renderable instanceof LayoutElement element) || element.getRectangle().overlaps(bounds)) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        layout.setX(contentLeft());
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        layout.setY(y - (int) scrollAmount());
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        arrangeElements();
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        arrangeElements();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        arrangeElements();
    }

    @Override
    public void setScrollAmount(double scrollAmount) {
        super.setScrollAmount(scrollAmount);
        onChangeScrollAmount(scrollAmount());
    }

    @Override
    final void onChangeScrollAmount(double scrollAmount) {
        layout.setY(getY() - (int) scrollAmount);
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(bounds, this)) {
            this.bounds = super.getRectangle();
        }
        return this.bounds;
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        layoutElementVisitor.accept(this);
    }

    @Override
    public void visitWidgets(Consumer<net.minecraft.client.gui.components.AbstractWidget> widgetVisitor) {
        widgetVisitor.accept(new ListWidgetAdapter(this));
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        propsState.contextEntries.accept(collector);
        FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return propsState.id;
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    private @Nullable Entry getEntryAt(double x, double y) {
        for (Entry entry : children()) {
            if (entry.widget.isMouseOver(x, y)) {
                return entry;
            }
        }
        return null;
    }

    private @Nullable Entry resolveEntry(Entry unknownEntry) {
        for (Entry entry : children()) {
            if (unknownEntry == entry ||
                unknownEntry.widget == entry.widget ||
                unknownEntry.id.equals(entry.id)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean scrolled = updateScrolling(mouseX, mouseY, button);

        Entry entry = getEntryAt(mouseX, mouseY);
        if (entry != null && entry.widget.mouseClicked(mouseX, mouseY, button)) {
            // buttons are only focused after end of mouseClicked,
            // so when a button inside the list reinitializes entries,
            // the new button instance with the same id as the clicked button cannot be refocused
            Entry clickedEntry = resolveEntry(entry);
            if (clickedEntry != null) {
                setFocused(clickedEntry);
                if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                    setDragging(true);
                }
                return true;
            }
        }

        return scrolled;
    }

    protected void applyProps(Props props) {
        this.propsState.apply(this, props);

        props.maxContentWidth().ifPresent(maxWidth -> this.maxContentWidth = maxWidth);
        props.contentPaddingLeft().ifPresent(padding -> this.contentPaddingLeft = padding);
        props.contentPaddingRight().ifPresent(padding -> this.contentPaddingRight = padding);

        if (props.reserveScrollbarWidth() != TriState.DEFAULT) {
            this.reserveScrollbarWidth = props.reserveScrollbarWidth().toBoolean(false);
        }

        props.scrollRate().ifPresent(this::setScrollRate);

        props.refreshHandler().ifPresent(initializer -> {
            FZKeyed<Consumer<RefreshEvent>> previousEntryInitializer = this.entryInitializer;
            this.entryInitializer = initializer;
            if (!Objects.equals(previousEntryInitializer, this.entryInitializer)) {
                this.refreshEntries();
            }
        });
    }

    public static FZLayoutList bind(String key, FZRef<Props> ref) {
        Props props = ref.value();
        FZLayoutList list = new FZLayoutList();
        list.applyProps(props);
        ref.subscribe(key, list::applyProps);
        return list;
    }

    public static Builder builder() {
        return new Builder();
    }

    static final class Entry extends AbstractListWidget.Entry<Entry> implements FZComponent {
        private final String id;
        private final GuiEventListener widget;
        private final List<GuiEventListener> children;

        private Entry(String id, GuiEventListener widget) {
            this.id = id;
            this.widget = widget;
            this.children = List.of(widget);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return children;
        }

        @Override
        public void setFocused(boolean focused) {
            widget.setFocused(focused);
        }

        @Override
        public boolean isFocused() {
            return widget.isFocused();
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            return ComponentPath.path(this, widget.nextFocusPath(navigationEvent));
        }

        @Override
        public ScreenRectangle getRectangle() {
            return widget.getRectangle();
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            widget.mouseMoved(mouseX, mouseY);
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
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            return widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
            return widget.mouseScrolled(mouseX, mouseY, scrollY);
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

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return widget.isMouseOver(mouseX, mouseY);
        }

        @Override
        public @Nullable ComponentPath getCurrentFocusPath() {
            return ComponentPath.path(this, widget.getCurrentFocusPath());
        }

        @Override
        public int getTabOrderGroup() {
            return widget.getTabOrderGroup();
        }

        @Override
        public boolean fidgetz$shouldTakeFocusAfterInteraction() {
            return widget instanceof FZComponent component
                    ? component.fidgetz$shouldTakeFocusAfterInteraction()
                    : FZComponent.super.fidgetz$shouldTakeFocusAfterInteraction();
        }
    }

    public final class RefreshEvent {
        private final GuiComponentCollector collector = new GuiComponentCollector();
        private final WidgetVisitor widgetSink;
        private final FZFlexLayout layout;

        private RefreshEvent(WidgetVisitor widgetSink, FZFlexLayout layout) {
            this.widgetSink = widgetSink;
            this.layout = layout;
        }

        public GuiComponentCollector collector() {
            return collector;
        }

        public FZLayoutList target() {
            return FZLayoutList.this;
        }

        public FZFlexLayout layout() {
            return layout;
        }

        public void flushComponents() {
            layout.visitWidgets(collector::renderableWidget);
            collector.flushTo(widgetSink, renderables::add);
        }
    }

    public interface Props extends GuiComponentProps {
        default OptionalInt maxContentWidth() {
            return OptionalInt.empty();
        }

        default OptionalInt contentPaddingLeft() {
            return OptionalInt.empty();
        }

        default OptionalInt contentPaddingRight() {
            return OptionalInt.empty();
        }

        default TriState reserveScrollbarWidth() {
            return TriState.DEFAULT;
        }

        default OptionalInt scrollRate() {
            return OptionalInt.empty();
        }

        default Optional<FZKeyed<Consumer<RefreshEvent>>> refreshHandler() {
            return Optional.empty();
        }
    }

    protected static final class PropsImpl extends GuiComponentPropsBase implements Props {
        private final @Nullable FZKeyed<Consumer<RefreshEvent>> refreshHandler;
        private final @Nullable Integer maxContentWidth;
        private final @Nullable Integer contentPaddingLeft;
        private final @Nullable Integer contentPaddingRight;
        private final TriState reserveScrollbarWidth;
        private final @Nullable Integer scrollRate;

        protected PropsImpl(
                GuiComponentProps props,
                @Nullable FZKeyed<Consumer<RefreshEvent>> refreshHandler,
                @Nullable Integer maxContentWidth,
                @Nullable Integer contentPaddingLeft,
                @Nullable Integer contentPaddingRight,
                TriState reserveScrollbarWidth,
                @Nullable Integer scrollRate
        ) {
            super(props);
            this.refreshHandler = refreshHandler;
            this.maxContentWidth = maxContentWidth;
            this.contentPaddingLeft = contentPaddingLeft;
            this.contentPaddingRight = contentPaddingRight;
            this.reserveScrollbarWidth = reserveScrollbarWidth;
            this.scrollRate = scrollRate;
        }

        @Override
        public Optional<FZKeyed<Consumer<RefreshEvent>>> refreshHandler() {
            return Optional.ofNullable(refreshHandler);
        }

        @Override
        public OptionalInt maxContentWidth() {
            return wrapBoxedInt(maxContentWidth);
        }

        @Override
        public OptionalInt contentPaddingLeft() {
            return wrapBoxedInt(contentPaddingLeft);
        }

        @Override
        public OptionalInt contentPaddingRight() {
            return wrapBoxedInt(contentPaddingRight);
        }

        @Override
        public TriState reserveScrollbarWidth() {
            return reserveScrollbarWidth;
        }

        @Override
        public OptionalInt scrollRate() {
            return wrapBoxedInt(scrollRate);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o)
                   && Objects.equals(maxContentWidth(), other.maxContentWidth())
                   && Objects.equals(contentPaddingLeft(), other.contentPaddingLeft())
                   && Objects.equals(contentPaddingRight(), other.contentPaddingRight())
                   && reserveScrollbarWidth == other.reserveScrollbarWidth()
                   && Objects.equals(scrollRate(), other.scrollRate())
                   && Objects.equals(refreshHandler(), other.refreshHandler());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(),
                    maxContentWidth,
                    contentPaddingLeft,
                    contentPaddingRight,
                    reserveScrollbarWidth,
                    scrollRate,
                    refreshHandler
            );
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private @Nullable FZKeyed<Consumer<RefreshEvent>> refreshHandler;
        private @Nullable Integer maxContentWidth;
        private @Nullable Integer contentPaddingLeft;
        private @Nullable Integer contentPaddingRight;
        private TriState reserveScrollbarWidth = TriState.DEFAULT;
        private @Nullable Integer scrollRate;

        private Builder() {
        }

        public Builder scrollRate(int scrollRate) {
            this.scrollRate = scrollRate;
            return this;
        }

        public Builder onRefresh(Consumer<RefreshEvent> refreshHandler) {
            this.refreshHandler = FZKeyed.selfKey(refreshHandler);
            return this;
        }


        public Builder onRefresh(Object key, Consumer<RefreshEvent> refreshHandler) {
            this.refreshHandler = new FZKeyed<>(key, Objects.requireNonNull(refreshHandler, "refreshHandler cannot be null"));
            return this;
        }

        public Builder maxContentWidth(int maxContentWidth) {
            this.maxContentWidth = maxContentWidth;
            return this;
        }

        public Builder contentPaddingLeft(int contentPaddingLeft) {
            this.contentPaddingLeft = contentPaddingLeft;
            return this;
        }

        public Builder contentPaddingRight(int contentPaddingRight) {
            this.contentPaddingRight = contentPaddingRight;
            return this;
        }

        public Builder contentPadding(int contentPadding) {
            return contentPaddingLeft(contentPadding).contentPaddingRight(contentPadding);
        }

        public Builder reserveScrollbarWidth(boolean reserveScrollbarWidth) {
            this.reserveScrollbarWidth = TriState.from(reserveScrollbarWidth);
            return this;
        }

        public Builder reserveScrollbarWidth() {
            return reserveScrollbarWidth(true);
        }

        public Props toProps() {
            return new PropsImpl(
                    props,
                    refreshHandler,
                    maxContentWidth,
                    contentPaddingLeft,
                    contentPaddingRight,
                    reserveScrollbarWidth,
                    scrollRate
            );
        }

        public FZLayoutList build() {
            FZLayoutList list = new FZLayoutList(DEFAULT_WIDTH, DEFAULT_HEIGHT, props.message().orElse(CommonComponents.EMPTY));
            list.applyProps(toProps());
            list.refreshEntries();
            return list;
        }
    }
}
