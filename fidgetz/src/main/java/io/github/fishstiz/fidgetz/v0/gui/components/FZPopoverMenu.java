package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.Fidgetz;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZComposedLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZScrollableLayout;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.utils.MathUtils;
import io.github.fishstiz.fidgetz.v0.utils.NavigationUtils;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class FZPopoverMenu extends FZDialog {
    protected static final ScreenRectangle DEFAULT_PADDING = ScreenRectangleUtils.insets(4);
    private static final ScreenRectangle DEFAULT_BORDER = ScreenRectangle.empty();
    protected static final RenderableRectangle DEFAULT_BACKGROUND = Renderables
            .boxShadow(24)
            .then(Renderables.sprite(Fidgetz.id("widget/popovermenu")));
    protected static final int DEFAULT_MAX_HEIGHT = 240;
    protected static final int DEFAULT_SPACING = 1;
    private final @Nullable FZPopoverMenu parent;
    private @Nullable RenderableRectangle background = DEFAULT_BACKGROUND;
    private FZPopoverMenuItem.@Nullable Divider sectionDivider = FZPopoverMenuEntryImpl.Divider.DEFAULT_SECTION;
    private FZPopoverMenuItem.@Nullable Divider entryDivider = FZPopoverMenuEntryImpl.Divider.DEFAULT_ENTRY;
    private ScreenRectangle padding = DEFAULT_PADDING;
    private ScreenRectangle border = DEFAULT_BORDER;
    private int rowSpacing = DEFAULT_SPACING;
    private int maxHeight = DEFAULT_MAX_HEIGHT;
    private int minWidth;
    private int maxWidth;
    private @Nullable ChildDetails child;
    private HorizontalDirection preferredDirection = HorizontalDirection.RIGHT;
    private HorizontalDirection currentDirection = HorizontalDirection.RIGHT;
    private ScreenRectangle bounds = ScreenRectangle.empty();
    private @Nullable FZLayout layout;
    private @Nullable FZScrollableLayout innerLayout;

    private FZPopoverMenu(ContainerEventHandler container, @Nullable FZPopoverMenu parent) {
        super(container);
        this.parent = parent;
    }

    protected FZPopoverMenu(ContainerEventHandler container) {
        this(container, null);
    }

    protected void setBackground(@Nullable RenderableRectangle background) {
        this.background = background;
        if (child != null) {
            child.menu.setBackground(background);
        }
    }

    protected void setSectionDivider(FZPopoverMenuItem.@Nullable Divider sectionDivider) {
        this.sectionDivider = sectionDivider;
        if (child != null) {
            child.menu.setSectionDivider(sectionDivider);
        }
    }

    protected void setEntryDivider(FZPopoverMenuItem.@Nullable Divider entryDivider) {
        this.entryDivider = entryDivider;
        if (child != null) {
            child.menu.setEntryDivider(entryDivider);
        }
    }

    protected void setPreferredDirection(HorizontalDirection preferredDirection) {
        this.preferredDirection = preferredDirection;
        if (child != null) {
            child.menu.setPreferredDirection(preferredDirection);
        }
    }

    protected void setPadding(ScreenRectangle padding) {
        this.padding = padding;
        if (child != null) {
            child.menu.setPadding(padding);
        }
    }

    protected void setBorder(ScreenRectangle border) {
        this.border = border;
        if (child != null) {
            child.menu.setBorder(border);
        }
    }

    protected void setRowSpacing(int rowSpacing) {
        this.rowSpacing = rowSpacing;
        if (child != null) {
            child.menu.setRowSpacing(rowSpacing);
        }
    }

    protected void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
        if (child != null) {
            child.menu.setMaxHeight(maxHeight);
        }
    }

    protected void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        if (child != null) {
            child.menu.setMinWidth(minWidth);
        }
    }

    protected void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        if (child != null) {
            child.menu.setMinWidth(maxWidth);
        }
    }

    protected void setX(int x) {
        if (layout != null) {
            if (child != null) {
                child.menu.setX(child.menu.getX() + x - layout.getX());
            }
            layout.setX(x);
            bounds = layout.getRectangle();
        }
    }

    protected void setY(int y) {
        if (layout != null) {
            if (child != null) {
                child.menu.setY(child.menu.getY() + y - layout.getY());
            }
            layout.setY(y);
            bounds = layout.getRectangle();
        }
    }

    protected int getX() {
        return getRectangle().left();
    }

    protected int getY() {
        return getRectangle().top();
    }

    @Override
    protected float getZ() {
        return parent == null ? super.getZ() : 1f;
    }

    private void applyLayout(@Nullable FZScrollableLayout newScrollableLayout) {
        if (newScrollableLayout == null) {
            this.layout = null;
            this.innerLayout = null;
            this.bounds = ScreenRectangle.empty();
            return;
        }

        int borderX = border.left() + border.right();
        int borderY = border.top() + border.bottom();

        newScrollableLayout.maxHeight(Math.max(0, maxHeight - borderY));

        FZLayout bordered = FZComposedLayout.compose(newScrollableLayout)
                .padding(border.left(), border.top(), border.right(), border.bottom());

        bordered.arrangeElements();

        if (newScrollableLayout.getWidth() + borderX < this.minWidth
            || (this.maxWidth > 0 && newScrollableLayout.getWidth() + borderX > this.maxWidth)) {

            newScrollableLayout.fidgetz$setWidth(MathUtils.clampOptionalMax(
                    newScrollableLayout.getWidth(),
                    this.minWidth - borderX,
                    this.maxWidth - borderX
            ));
        }

        this.layout = bordered;
        this.innerLayout = newScrollableLayout;
        this.bounds = bordered.getRectangle();
    }

    @Override
    public void repositionElements() {
        applyLayout(this.innerLayout);
        if (child != null) {
            child.menu.repositionElements();
        }
    }

    protected HorizontalDirection getPreferredDirection() {
        return HorizontalDirection.RIGHT;
    }

    @Override
    public boolean shouldCaptureClick() {
        return false;
    }

    @Override
    public boolean shouldFocusOnOpen() {
        return parent == null;
    }

    @Override
    public boolean shouldRefocusLastPath() {
        return parent == null;
    }

    @Override
    public ScreenRectangle getRectangle() {
        return bounds;
    }

    protected void build(List<FZPopoverMenuItem> items) {
        FZFlexLayout content = FZFlexLayout.vertical().spacing(rowSpacing);
        content.defaultChildSettings().flexCross();

        int entryCount = 0;
        int maxEntryWidth = 0;

        for (int i = 0; i < items.size(); i++) {
            FZPopoverMenuItem current = items.get(i);
            FZPopoverMenuItem next = (i + 1 < items.size()) ? items.get(i + 1) : null;
            boolean canDivideNext = next != null && !(next instanceof FZPopoverMenuItem.Divider);

            if (current instanceof FZPopoverMenuItem.Divider divider) {
                if (canDivideNext && entryCount > 0) {
                    FZPopoverMenuItem.Divider resolved = FZPopoverMenuEntryImpl.Divider.DEFAULT_SECTION;
                    if (divider.factory() != null) {
                        resolved = divider;
                    }
                    if (sectionDivider != null && sectionDivider.factory() != null) {
                        resolved = sectionDivider;
                    }
                    content.child(new EntryWidget(resolved));
                }
            } else {
                EntryWidget entryWidget = new EntryWidget(current);
                FZPopoverMenuItem.Entry entry = entryWidget.entry;
                content.child(entryWidget);
                maxEntryWidth = Math.max(maxEntryWidth, entry.getWidth());
                entryCount++;
                if (canDivideNext && entryDivider != null && current.settings().autoDividerAfter()) {
                    content.child(new EntryWidget(Objects.requireNonNullElse(
                            entryDivider,
                            FZPopoverMenuEntryImpl.Divider.DEFAULT_ENTRY
                    )));
                }
            }
        }

        if (entryCount == 0) {
            applyLayout(null);
            clearWidgets();
            return;
        }

        content.fidgetz$setWidth(maxEntryWidth);

        FZScrollableLayout newScrollableLayout = FZComposedLayout.compose(content)
                .padding(padding.left(), padding.top(), padding.right(), padding.bottom())
                .toScrollable(container)
                .scrollbarSpacing(0)
                .maxHeight(Math.max(0, maxHeight));

        applyLayout(newScrollableLayout);

        clearWidgets();
        GuiComponentCollector collector = new GuiComponentCollector();
        newScrollableLayout.visitWidgets(collector::renderableWidget);
        collector.flushTo(this::addWidget, this::addRenderableOnly);
    }

    private void openAndPosition(int x, int y, HorizontalDirection direction, List<FZPopoverMenuItem> items) {
        build(items);
        if (layout == null) {
            close();
        } else {
            ScreenRectangle targetBounds = layout.getRectangle();
            ScreenRectangle containerBounds = container.getRectangle();
            HorizontalDirection newDirection = direction.resolve(containerBounds, targetBounds.width(), x);
            layout.setX(newDirection.clamp(containerBounds, targetBounds.width(), x));
            layout.setY(Math.max(0, y + targetBounds.height() > containerBounds.height() ? containerBounds.height() - targetBounds.height() : y));
            this.currentDirection = newDirection;
            this.bounds = layout.getRectangle();
            setOpen(true);
        }
    }

    protected void openAndPosition(int x, int y, List<FZPopoverMenuItem> entries) {
        openAndPosition(x, y, getPreferredDirection(), entries);
    }

    @Override
    protected void clearWidgets() {
        super.clearWidgets();
        this.child = null;
    }

    @Override
    protected void onClose() {
        super.onClose();

        if (child != null) {
            child.menu.close();
        }

        clearWidgets();
        this.currentDirection = getPreferredDirection();
        this.layout = null;
        this.bounds = ScreenRectangle.empty();

        if (parent != null) {
            if (parent.child != null) {
                parent.child.key.setFocused(false);
            }

            parent.child = null;

            if (parent.getFocused() == this) {
                parent.setFocused(null);
                ComponentPath path = menuPath(parent, null);
                if (path != null) {
                    path.applyFocus(true);
                }
            }
        }
    }

    public void close() {
        setOpen(false);
    }

    private void closeAll() {
        if (parent != null) {
            parent.closeAll();
        } else {
            close();
        }
    }

    private void closeChild() {
        if (child != null) {
            child.menu.close();
        }
    }

    private void openChild(EntryWidget entry, List<FZPopoverMenuItem> entries) {
        if (child != null && child.key == entry) return;

        FZPopoverMenu menu = new FZPopoverMenu(container, this);
        menu.background = background;
        menu.sectionDivider = sectionDivider;
        menu.entryDivider = entryDivider;
        menu.padding = padding;
        menu.border = border;
        menu.preferredDirection = preferredDirection;
        menu.rowSpacing = rowSpacing;
        menu.maxHeight = maxHeight;
        menu.minWidth = minWidth;

        ScreenRectangle entryBounds = entry.getRectangle();
        int anchor = currentDirection.edge(entryBounds);
        HorizontalDirection newDirection = currentDirection.resolve(container.getRectangle(), getRectangle().width(), anchor);
        int x = newDirection.edge(entryBounds);
        int y = entryBounds.top();

        menu.openAndPosition(x, y, newDirection, entries);
        closeChild();
        addWidgetFirst(menu);

        child = new ChildDetails(entry, menu);
    }

    @Override
    protected void extractDialogRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (background != null) {
            background.extractRenderState(graphics, bounds.left(), bounds.top(), bounds.width(), bounds.height(), mouseX, mouseY, partialTick);
        }
        super.extractDialogRenderState(graphics, mouseX, mouseY, partialTick);
        if (child != null) {
            child.menu.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected boolean areCoordinatesInBounds(double x, double y) {
        return super.areCoordinatesInBounds(x, y) || (child != null && child.menu.areCoordinatesInBounds(x, y));
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || (child != null && child.menu.isMouseOver(mouseX, mouseY));
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        if (child != null) {
            ComponentPath path = child.menu.nextFocusPath(event);
            if (path != null) {
                return ComponentPath.path(this, path);
            }
        }

        return super.nextFocusPath(event);
    }

    private static @Nullable ComponentPath menuPath(FZPopoverMenu current, @Nullable ComponentPath childPath) {
        if (childPath == null) {
            return current.parent == null
                    ? NavigationUtils.findPath(current.container, current)
                    : menuPath(current.parent, ComponentPath.leaf(current));
        }

        ComponentPath path = childPath.component() == current
                ? childPath
                : ComponentPath.path(current, childPath);

        if (current.parent != null) {
            return menuPath(current.parent, path);
        }

        ComponentPath containerPath = NavigationUtils.findPath(current.container, current);
        return containerPath == null
                ? null
                : NavigationUtils.appendPath(containerPath, path);
    }

    private record ChildDetails(EntryWidget key, FZPopoverMenu menu) {
    }

    private final class EntryWidget extends AbstractWidget implements FZPopoverMenuItem.Context, FZComponent, ContainerEventHandlerPatch {
        private final FZPopoverMenuItem.Entry entry;
        private final FZPopoverMenuItem.Settings settings;
        private final List<FZPopoverMenuItem.Entry> singletonChild;
        private ScreenRectangle entryBounds = ScreenRectangle.empty();
        private @Nullable GuiEventListener focused;
        private boolean lastHovered;
        private boolean dragging;

        private EntryWidget(FZPopoverMenuItem item) {
            super(0, 0, minWidth, 0, CommonComponents.EMPTY);
            this.settings = item.settings();
            this.entry = item.factory().create(this);
            this.singletonChild = List.of(entry);
            this.height = this.entry.getHeight();
            this.width = this.entry.getWidth();
            this.entryBounds = super.getRectangle();
        }

        @Override
        public void closeMenu() {
            closeAll();
        }

        @Override
        public List<FZPopoverMenuItem.Entry> children() {
            return singletonChild;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (isActive() && entry.mouseClicked(mouseX, mouseY, button)) {
                if (settings.closeOnInteract()) {
                    closeMenu();
                } else if (isOpen() && entry.fidgetz$shouldTakeFocusAfterInteraction()) {
                    setFocused(entry);

                    if (button == InputConstants.MOUSE_BUTTON_LEFT) {
                        setDragging(true);
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            entry.render(graphics, mouseX, mouseY, partialTick);
            boolean hovered = isHovered();
            if (hovered && !lastHovered) {
                updateChild();
            }
            lastHovered = hovered;
        }

        private void updateChild() {
            if (FZPopoverMenu.this.getFocused() == null || !FZPopoverMenu.this.isDragging()) {
                List<FZPopoverMenuItem> children = entry.childItems();
                if (children.isEmpty() || !entry.isActive()) {
                    closeChild();
                } else {
                    openChild(this, children);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            entry.updateNarration(output);
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            return ComponentPath.path(this, entry.nextFocusPath(navigationEvent));
        }

        @Override
        public @Nullable ComponentPath getCurrentFocusPath() {
            return ComponentPath.path(this, entry.getCurrentFocusPath());
        }

        @Override
        public boolean isActive() {
            return entry.isActive();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return (super.isMouseOver(mouseX, mouseY) || (isActive() && entry.isMouseOver(mouseX, mouseY)))
                   && (child == null || !child.menu().isMouseOver(mouseX, mouseY));
        }

        @Override
        public boolean isChildOpened() {
            return child != null && child.key == this && child.menu.isOpen();
        }

        private boolean handleKeyboardFocusOnClose() {
            if (parent == null) return false;

            ChildDetails child = parent.child;
            close();

            ComponentPath path = menuPath(parent, child == null
                    ? parent.nextFocusPath(new FocusNavigationEvent.InitialFocus())
                    : NavigationUtils.findPath(parent, child.key)
            );

            if (path != null) {
                path.applyFocus(true);
                return true;
            }

            return false;
        }

        private boolean handleKeyboardFocusOnOpen() {
            if (isChildOpened()) return false;

            List<FZPopoverMenuItem> children = entry.childItems();
            if (children.isEmpty()) return false;

            openChild(this, children);
            if (child == null) return false;

            ComponentPath path = menuPath(child.menu, NavigationUtils.initialFocus(child.menu));
            if (path != null) {
                path.applyFocus(true);
                setFocused(true);
                return true;
            }
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!isOpen()) {
                return false;
            }
            if (entry.keyPressed(keyCode, scanCode, modifiers)) {
                if ((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) &&
                    settings.closeOnInteract()) {
                    closeMenu();
                }
                return true;
            }
            if (keyCode == InputConstants.KEY_LEFT) {
                return handleKeyboardFocusOnClose();
            }
            if (keyCode == InputConstants.KEY_RIGHT ||
                keyCode == InputConstants.KEY_RETURN ||
                keyCode == InputConstants.KEY_SPACE ||
                keyCode == InputConstants.KEY_NUMPADENTER) {
                return handleKeyboardFocusOnOpen();
            }
            return false;
        }

        @Override
        public @Nullable GuiEventListener getFocused() {
            return focused;
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            GuiEventListener previous = getFocused();
            if (previous != focused) {
                if (previous != null) {
                    previous.setFocused(false);
                }
                if (focused != null) {
                    focused.setFocused(true);
                }
                this.focused = focused;
            }
        }

        @Override
        public void setFocused(boolean focused) {
            super.setFocused(focused);
            if (!focused) {
                setFocused(null);
            }
        }

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        @Override
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }

        private void updateEntryX() {
            int availableSpace = getWidth() - entry.getWidth();
            int alignedX = getX() + Math.round(availableSpace * net.minecraft.util.Mth.clamp(settings.xAlignment(), 0.0f, 1.0f));
            entry.setX(alignedX);
        }

        @Override
        public void setX(int x) {
            super.setX(x);
            entryBounds = super.getRectangle();
            updateEntryX();
        }

        @Override
        public void setY(int y) {
            super.setY(y);
            entryBounds = super.getRectangle();
            entry.setY(y);
        }

        public void setSize(int width, int height) {
            setWidth(width);
            setHeight(entry.getHeight());
        }

        @Override
        public void setWidth(int width) {
            super.setWidth(width);
            entryBounds = super.getRectangle();
            if (settings.stretch() || width <= entry.getWidth()) {
                entry.setWidth(width);
            }
            updateEntryX();
        }

        public void setHeight(int height) {
            io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils.setHeight(this, entry.getHeight());
            entryBounds = super.getRectangle();
        }

        @Override
        public int getHeight() {
            return entry.getHeight();
        }

        @Override
        public ScreenRectangle getRectangle() {
            return entryBounds;
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
        public boolean fidgetz$shouldTakeFocusAfterInteraction() {
            return isOpen() && entry.fidgetz$shouldTakeFocusAfterInteraction();
        }

        @Override
        public boolean isFocused() {
            return super.isFocused() || getFocused() != null;
        }

        // override methods to resolve mappings on fabric

        @Override
        public boolean isHovered() {
            return super.isHovered();
        }

        @Override
        public boolean isFocusedOrHovered() {
            return isFocused() || isHovered();
        }
    }
}
