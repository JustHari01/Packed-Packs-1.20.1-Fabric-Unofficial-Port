package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class FZContextMenu extends FZPopoverMenu {
    private static final ScreenRectangle DEFAULT_PADDING = ScreenRectangleUtils.insets(4);
    private static final ScreenRectangle DEFAULT_BORDER = ScreenRectangle.empty();
    private static final int DEFAULT_MAX_HEIGHT = 240;
    private static final int DEFAULT_ENTRY_WIDTH = 150;
    private static final int DEFAULT_SPACING = 1;
    private final boolean focusOnOpen;

    private FZContextMenu(FZDialogContainer container, HorizontalDirection preferredDirection, boolean focusOnOpen) {
        super(container);
        this.focusOnOpen = focusOnOpen;
        setPreferredDirection(preferredDirection);
    }

    @Override
    public boolean shouldFocusOnOpen() {
        return super.shouldFocusOnOpen() && focusOnOpen;
    }

    public void open(double x, double y, List<FZPopoverMenuItem> entries) {
        super.openAndPosition((int) x, (int) y, entries);
    }

    public static Builder builder(FZDialogContainer container) {
        return new Builder(container);
    }

    public interface Collector {
        void addEntry(FZPopoverMenuItem entry);

        default void addEntry(UnaryOperator<FZPopoverMenuItem.Builder> configurator) {
            addEntry(configurator.apply(FZPopoverMenuItem.builder()).build());
        }

        default void addWidget(AbstractWidget widget) {
            addEntry(FZPopoverMenuItem.fromWidget(widget));
        }

        default void addWidget(AbstractWidget widget, FZPopoverMenuItem.Settings settings) {
            addEntry(FZPopoverMenuItem.fromWidget(widget, settings));
        }

        default void addWidget(AbstractWidget widget, UnaryOperator<FZPopoverMenuItem.Settings> settingsBuilder) {
            addEntry(FZPopoverMenuItem.fromWidget(widget, settingsBuilder));
        }

        default void addWidget(Function<FZPopoverMenuItem.Context, AbstractWidget> widgetFactory) {
            addEntry(FZPopoverMenuItem.fromWidget(widgetFactory));
        }

        default void addWidget(
                Function<FZPopoverMenuItem.Context, AbstractWidget> widgetFactory,
                FZPopoverMenuItem.Settings settings
        ) {
            addEntry(FZPopoverMenuItem.fromWidget(widgetFactory, settings));
        }

        default void addWidget(
                Function<FZPopoverMenuItem.Context, AbstractWidget> widgetFactory,
                UnaryOperator<FZPopoverMenuItem.Settings> settingsBuilder
        ) {
            addEntry(FZPopoverMenuItem.fromWidget(widgetFactory, settingsBuilder));
        }

        default Collector nextSection() {
            addEntry(FZPopoverMenuItem.divider());
            return this;
        }
    }

    public interface Source {
        default void fidgetz$updateContextEntries(double x, double y, Collector collector) {
            if (this instanceof ContainerEventHandler container) {
                container.getChildAt(x, y)
                        .filter(Source.class::isInstance)
                        .map(Source.class::cast)
                        .ifPresent(child -> child.fidgetz$updateContextEntries(x, y, collector.nextSection()));
            }
        }

        default List<FZPopoverMenuItem> fidgetz$collectContextEntries(double x, double y) {
            List<FZPopoverMenuItem> entries = new ArrayList<>();
            fidgetz$updateContextEntries(x, y, entries::add);
            return entries;
        }
    }

    public final static class Builder {
        private final FZDialogContainer container;
        private HorizontalDirection preferredDirection = HorizontalDirection.RIGHT;
        private ScreenRectangle padding = DEFAULT_PADDING;
        private ScreenRectangle border = DEFAULT_BORDER;
        private @Nullable RenderableRectangle background = DEFAULT_BACKGROUND;
        private FZPopoverMenuItem.@Nullable Divider sectionDivider = FZPopoverMenuEntryImpl.Divider.DEFAULT_SECTION;
        private FZPopoverMenuItem.@Nullable Divider entryDivider = FZPopoverMenuEntryImpl.Divider.DEFAULT_ENTRY;
        private int maxHeight = DEFAULT_MAX_HEIGHT;
        private int minWidth = DEFAULT_ENTRY_WIDTH;
        private int popoverOrder = DEFAULT_POPOVER_ORDER;
        private int rowSpacing = DEFAULT_SPACING;
        private @Nullable String componentId;
        private boolean focusOnOpen = true;

        private Builder(FZDialogContainer container) {
            this.container = container;
        }

        public Builder id(String id) {
            this.componentId = id;
            return this;
        }

        public Builder background(@Nullable RenderableRectangle background) {
            this.background = background;
            return this;
        }

        public Builder sectionDivider(FZPopoverMenuItem.@Nullable Divider sectionDivider) {
            this.sectionDivider = sectionDivider;
            return this;
        }

        public Builder sectionDivider(RenderableRectangle sectionDivider) {
            Objects.requireNonNull(entryDivider, "sectionDivider cannot be null");
            this.sectionDivider = new FZPopoverMenuItem.Divider(ctx -> new FZPopoverMenuEntryImpl.Divider(ctx, sectionDivider));
            return this;
        }

        public Builder entryDivider(FZPopoverMenuItem.@Nullable Divider entryDivider) {
            this.entryDivider = entryDivider;
            return this;
        }

        public Builder entryDivider(RenderableRectangle entryDivider) {
            Objects.requireNonNull(entryDivider, "entryDivider cannot be null");
            this.sectionDivider = new FZPopoverMenuItem.Divider(ctx -> new FZPopoverMenuEntryImpl.Divider(ctx, entryDivider));
            return this;
        }

        public Builder noEntryDivider() {
            this.entryDivider = null;
            return this;
        }

        public Builder rowSpacing(int spacing) {
            this.rowSpacing = spacing;
            return this;
        }

        public Builder preferredDirection(HorizontalDirection preferredDirection) {
            this.preferredDirection = preferredDirection;
            return this;
        }

        public Builder padding(int left, int top, int right, int bottom) {
            this.padding = ScreenRectangleUtils.insets(left, top, right, bottom);
            return this;
        }

        public Builder padding(int padding) {
            return padding(padding, padding, padding, padding);
        }

        public Builder border(int left, int top, int right, int bottom) {
            this.border = ScreenRectangleUtils.insets(left, top, right, bottom);
            return this;
        }

        public Builder border(int border) {
            return border(border, border, border, border);
        }

        public Builder maxHeight(int maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }

        public Builder minWidth(int minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        public Builder popoverOrder(int popoverOrder) {
            this.popoverOrder = popoverOrder;
            return this;
        }

        public Builder focusOnOpen(boolean focusOnOpen) {
            this.focusOnOpen = focusOnOpen;
            return this;
        }

        public Builder focusOnOpen() {
            return focusOnOpen(true);
        }

        public FZContextMenu build() {
            FZContextMenu contextMenu = new FZContextMenu(container, preferredDirection, focusOnOpen);
            contextMenu.componentId = componentId;
            contextMenu.popoverOrder = popoverOrder;
            contextMenu.setBackground(background);
            contextMenu.setSectionDivider(sectionDivider);
            contextMenu.setEntryDivider(entryDivider);
            contextMenu.setPadding(padding);
            contextMenu.setBorder(border);
            contextMenu.setRowSpacing(rowSpacing);
            contextMenu.setMaxHeight(maxHeight);
            contextMenu.setMinWidth(minWidth);
            return contextMenu;
        }

        public FZContextMenu buildAndOpen(double x, double y, List<FZPopoverMenuItem> entries) {
            FZContextMenu contextMenu = build();
            contextMenu.open(x, y, entries);
            return contextMenu;
        }
    }
}
