package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.Fidgetz;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.gui.text.TextComponentUtils;
import io.github.fishstiz.fidgetz.v0.utils.*;
import io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.*;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public final class FZDropdown extends Button implements FZComponent, FZContextMenu.Source, FZPopoverContainer, Layout {
    private static final int DEFAULT_ELEMENT_SPACING = 8;
    private static final int ENTRY_SPACING = 4;
    private static final int DEFAULT_SELECTION_HEIGHT = 200;
    private static final Component BLACK_RIGHT_POINTING_TRIANGLE = TextComponentUtils.BLACK_RIGHT_POINTING_TRIANGLE;
    private static final Component BLACK_DOWN_POINTING_TRIANGLE = TextComponentUtils.BLACK_DOWN_POINTING_TRIANGLE;
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private final SelectionContainer selectionContainer;
    private final ContainerEventHandler parentContainer;
    private final Font font;
    private List<FZPopoverMenuItem> items = Collections.emptyList();
    private boolean hideMessage;
    private @Nullable WidgetElements leftIcon;
    private boolean closeOnBlur = true;
    private Component interactSymbol = BLACK_RIGHT_POINTING_TRIANGLE;
    private Component inactiveInteractSymbol = inactiveMessage(interactSymbol);
    private int interactIconWidth;
    private boolean iconWidthDirty = true;
    private ScreenRectangle bounds;

    FZDropdown(int x, int y, int width, int height, Component message, ContainerEventHandler parentContainer) {
        super(x, y, width, height, message, FZButton.NOP, DEFAULT_NARRATION);
        this.font = Minecraft.getInstance().font;
        this.parentContainer = parentContainer;
        this.selectionContainer = new SelectionContainer(parentContainer);
        this.bounds = super.getRectangle();
    }

    FZDropdown(ContainerEventHandler parentContainer) {
        this(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY, parentContainer);
    }

    private static Component inactiveMessage(Component message) {
        return ComponentUtils.mergeStyles(message.copy(), Style.EMPTY.withColor(0xFFA0A0A0));
    }

    public void openSelection() {
        selectionContainer.build(this.items);
        selectionContainer.repositionElements();
        selectionContainer.setOpen(true);
    }

    public void closeSelection() {
        selectionContainer.setOpen(false);
    }

    @Override
    public void onPress() {
        super.onPress();
        if (selectionContainer.isOpen()) {
            closeSelection();
        } else {
            openSelection();
        }
    }

    private int getInteractIconWidth() {
        if (iconWidthDirty) {
            interactIconWidth = font.width(interactSymbol);
            iconWidthDirty = false;
        }
        return interactIconWidth;
    }

    private Component getInteractSymbol() {
        return active ? interactSymbol : inactiveInteractSymbol;
    }

    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        int left = getX();
        int top = getY();
        int width = getWidth();
        int height = getHeight();
        int right = WidgetSizeUtils.right(this);
        int spacing = DEFAULT_ELEMENT_SPACING;

        right -= spacing + getInteractIconWidth();

        if (leftIcon != null) {
            left += spacing + leftIcon.margin().left();

            int iconY = (top + height / 2 - leftIcon.height() / 2) + leftIcon.margin().top() - leftIcon.margin().bottom();

            leftIcon.elements()
                    .get(isActive(), isHoveredOrFocused())
                    .extractRenderState(graphics, left, iconY, leftIcon.width(), leftIcon.height(), mouseX, mouseY, partialTick);

            left += leftIcon.width() + leftIcon.margin().right();
        }

        if (!hideMessage) {
            int color = (isActive() ? CommonColors.WHITE : 0xFFA0A0A0) | Mth.ceil(this.alpha * 255.0F) << 24;
            GuiGraphicsUtils.scrollingText(graphics, getMessage(), left + spacing, top, right - spacing, top + height, color);
        }

        int symbolX = MathUtils.clampOrAverage(right, getX() + spacing, right);
        int symbolY = top + (height / 2) - (font.lineHeight / 2);
        graphics.drawString(font, getInteractSymbol(), symbolX, symbolY, CommonColors.WHITE);

        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), top, width, height, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (selectionContainer.isOpen() && keyCode == InputConstants.KEY_ESCAPE) {
            closeSelection();
            ComponentPath path = NavigationUtils.findPath(parentContainer, this);
            if (path != null) {
                path.applyFocus(true);
            }
            return true;
        }

        if (!selectionContainer.isOpen()) {
            return false;
        }

        ScreenDirection selectionDirection = selectionContainer.isPositionedDown()
                ? ScreenDirection.DOWN
                : ScreenDirection.UP;

        if (NavigationUtils.getDirection(keyCode) != selectionDirection) {
            return false;
        }

        ComponentPath containerPath = NavigationUtils.findPath(parentContainer, selectionContainer);
        ComponentPath selectionPath = selectionContainer.nextFocusPath(
                new FocusNavigationEvent.ArrowNavigation(selectionDirection)
        );

        if (containerPath != null) {
            NavigationUtils.appendPath(containerPath, selectionPath).applyFocus(true);
            return true;
        }

        return false;
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        propsState.contextEntries.accept(collector);
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
        ComponentPath path = super.nextFocusPath(navigationEvent);
        if (path != null || !isFocused() || !selectionContainer.isOpen()) {
            return path;
        }

        ScreenDirection selectionDirection = selectionContainer.isPositionedDown()
                ? ScreenDirection.DOWN
                : ScreenDirection.UP;

        if ((selectionDirection == ScreenDirection.DOWN && NavigationUtils.isUp(navigationEvent, true)) ||
            (selectionDirection == ScreenDirection.UP && NavigationUtils.isDown(navigationEvent, true))) {
            return getCurrentFocusPath();
        }

        return null;
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(bounds, this)) {
            this.bounds = super.getRectangle();
        }
        return this.bounds;
    }

    @Override
    public void fidgetz$visitPopovers(Consumer<FZPopover> visitor) {
        visitor.accept(selectionContainer);
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        layoutElementVisitor.accept(this);
    }

    @Override
    public void setX(int x) {
        if (selectionContainer.isOpen()) {
            selectionContainer.setX(selectionContainer.getX() + (x - getX()));
        }
        super.setX(x);
    }

    @Override
    public void setY(int y) {
        if (selectionContainer.isOpen()) {
            selectionContainer.setY(selectionContainer.getY() + (y - getY()));
        }
        super.setY(y);
    }

    @Override
    public void arrangeElements() {
        if (selectionContainer.isOpen()) {
            selectionContainer.repositionElements();
        }
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return propsState.id;
    }

    void applyProps(Props props) {
        if (props.parentContainer() != parentContainer) {
            throw new UnsupportedOperationException("Updating the parent container of FZDropdown is not supported.");
        }

        propsState.apply(this, props);

        if (props.hideMessage() != TriState.DEFAULT) {
            hideMessage = props.hideMessage().toBoolean(false);
        }
        if (props.closeOnBlur() != TriState.DEFAULT) {
            closeOnBlur = props.closeOnBlur().toBoolean(true);
        }

        props.leftIcon().ifDefined(leftIcon -> this.leftIcon = leftIcon);
        props.containerBackground().ifPresent(selectionContainer::setBackground);
        props.maxContainerHeight().ifPresent(maxHeight -> selectionContainer.maxHeight = maxHeight);
        props.minContainerWidth().ifPresent(minContainerWidth -> selectionContainer.minWidth = minContainerWidth);
        props.maxContainerWidth().ifPresent(maxContainerWidth -> selectionContainer.maxWidth = maxContainerWidth);
        props.preferredDirection().ifPresent(direction -> selectionContainer.rootDirection = direction);

        props.entryDivider().ifDefined(selectionContainer::setEntryDivider);
        props.sectionDivider().ifDefined(selectionContainer::setSectionDivider);
        props.containerPadding().ifPresent(selectionContainer::setPadding);
        props.containerBorder().ifPresent(selectionContainer::setBorder);

        List<FZPopoverMenuItem> previousEntries = this.items;
        this.items = props.entries();
        if (selectionContainer.isOpen() && !Objects.equals(previousEntries, this.items)) {
            selectionContainer.build(this.items);
            selectionContainer.repositionElements();
        }
    }

    public static FZDropdown bind(String key, FZRef<Props> ref) {
        Props props = ref.value();
        FZDropdown dropdown = new FZDropdown(props.parentContainer());
        dropdown.applyProps(props);
        ref.subscribe(key, dropdown::applyProps);
        return dropdown;
    }

    public static Builder builder(ContainerEventHandler container) {
        return new Builder(container);
    }

    private final class SelectionContainer extends FZPopoverMenu {
        private HorizontalDirection rootDirection = HorizontalDirection.RIGHT;
        private int maxHeight = DEFAULT_SELECTION_HEIGHT;
        private Length minWidth = Length.fill();
        private Length maxWidth = Length.fill();
        private boolean focused;

        SelectionContainer(ContainerEventHandler container) {
            super(container);
            setPadding(ScreenRectangleUtils.insets(ENTRY_SPACING));
            setRowSpacing(ENTRY_SPACING);
        }

        private void updateIcon() {
            interactSymbol = isOpen() ? BLACK_DOWN_POINTING_TRIANGLE : BLACK_RIGHT_POINTING_TRIANGLE;
            inactiveInteractSymbol = inactiveMessage(interactSymbol);
            iconWidthDirty = true;
        }

        @Override
        protected void onOpen() {
            super.onOpen();
            updateIcon();
        }

        @Override
        protected void onClose() {
            boolean focused = isFocused() || getFocused() != null;
            super.onClose();
            if (focused) {
                ComponentPath path = NavigationUtils.findPath(parentContainer, FZDropdown.this);
                if (path != null) {
                    path.applyFocus(true);
                }
            }
            updateIcon();
        }

        @Override
        protected void build(List<FZPopoverMenuItem> items) {
            setMaxHeight(maxHeight);
            super.build(items);
        }

        @Override
        public void repositionElements() {
            ScreenRectangle parentBounds = parentContainer.getRectangle();
            ScreenRectangle buttonBounds = FZDropdown.this.getRectangle();
            ScreenRectangle selectionBounds = getRectangle();

            int minWidthLength = minWidth.resolve(buttonBounds.width());
            int maxWidthLength = maxWidth.resolve(buttonBounds.width());;
            if (maxWidthLength > 0 && minWidthLength > maxWidthLength) {
                maxWidthLength = minWidthLength;
            }

            int selectionWidth = MathUtils.clampOptionalMax(selectionBounds.width(), minWidthLength, maxWidthLength);
            int selectionHeight = selectionBounds.height();

            int spaceBelow = parentBounds.bottom() - buttonBounds.bottom();
            int spaceAbove = buttonBounds.top() - parentBounds.top();

            int anchor = rootDirection.flip().edge(buttonBounds);

            HorizontalDirection newDirection = rootDirection.resolve(parentBounds, selectionWidth, anchor);
            anchor = newDirection.flip().edge(buttonBounds);

            int selectionX = newDirection.clamp(parentBounds, selectionWidth, anchor);
            int selectionY;
            if (selectionHeight > spaceBelow) {
                if (spaceAbove > spaceBelow) {
                    selectionHeight = Math.min(selectionHeight, spaceAbove);
                    selectionY = buttonBounds.top() - selectionHeight;
                    setMaxHeight(selectionHeight);
                } else {
                    selectionHeight = spaceBelow;
                    selectionY = buttonBounds.bottom();
                    setMaxHeight(selectionHeight);
                }
            } else {
                selectionY = buttonBounds.bottom();
            }

            setMinWidth(selectionWidth);
            setMaxWidth(selectionWidth);
            super.repositionElements();

            setX(selectionX);
            setY(selectionY);
            setMaxHeight(maxHeight);
        }

        @Override
        protected void extractDialogRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.extractDialogRenderState(graphics, mouseX, mouseY, partialTick);

            if ((FZDropdown.this.closeOnBlur && !isFocused() && !FZDropdown.this.isFocused()) || !FZDropdown.this.isActive()) {
                closeSelection();
            }
        }

        @Override
        public boolean shouldCaptureFocus() {
            return false;
        }

        @Override
        public boolean shouldFocusOnOpen() {
            return false;
        }

        @Override
        public boolean shouldRefocusLastPath() {
            return false;
        }

        @Override
        public void setFocused(boolean focused) {
            this.focused = focused;
            super.setFocused(focused);
        }

        @Override
        public void setFocused(@Nullable GuiEventListener focused) {
            if (focused == null || isOpen()) {
                super.setFocused(focused);
            }
        }

        @Override
        public boolean isFocused() {
            return focused || getFocused() != null;
        }

        @Override
        public @Nullable ComponentPath getCurrentFocusPath() {
            if (!isFocused()) return null;
            return getFocused() == null ? ComponentPath.leaf(this) : super.getCurrentFocusPath();
        }

        private boolean isPositionedUp() {
            return getY() < FZDropdown.this.getY();
        }

        private boolean isPositionedDown() {
            return getY() > FZDropdown.this.getY();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (!isOpen()) {
                return false;
            }

            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }

            ScreenDirection dropdownDirection = isPositionedDown() ? ScreenDirection.UP : ScreenDirection.DOWN;
            if (NavigationUtils.getDirection(keyCode) != dropdownDirection) {
                return false;
            }

            FocusNavigationEvent navigationEvent = new FocusNavigationEvent.ArrowNavigation(dropdownDirection);
            ComponentPath nextPath = nextFocusPath(navigationEvent);
            if (nextPath != null) {
                return false;
            }

            ComponentPath containerPath = NavigationUtils.findPath(parentContainer, FZDropdown.this);
            ComponentPath dropdownPath = FZDropdown.this.nextFocusPath(navigationEvent);
            if (containerPath != null) {
                NavigationUtils.appendPath(containerPath, dropdownPath).applyFocus(true);
                return true;
            }

            return false;
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            if (!isOpen()) return null;

            ComponentPath path = super.nextFocusPath(navigationEvent);
            if (path != null || !isFocused()) return path;

            ScreenDirection dropdownDirection = isPositionedDown() ? ScreenDirection.UP : ScreenDirection.DOWN;
            if ((dropdownDirection == ScreenDirection.DOWN && NavigationUtils.isUp(navigationEvent, true)) ||
                (dropdownDirection == ScreenDirection.UP && NavigationUtils.isDown(navigationEvent, true))) {
                return getCurrentFocusPath();
            }

            return null;
        }

        @Override
        public int getTabOrderGroup() {
            return FZDropdown.this.getTabOrderGroup();
        }

        @Override
        public boolean isActive() {
            return FZDropdown.this.isActive() && super.isActive();
        }

        @Override
        public int fidgetz$popoverOrder() {
            return 1;
        }
    }

    public interface Props extends GuiComponentProps {
        ContainerEventHandler parentContainer();

        List<FZPopoverMenuItem> entries();

        default TriState hideMessage() {
            return TriState.DEFAULT;
        }

        default TriState closeOnBlur() {
            return TriState.DEFAULT;
        }

        default Undefinable<@Nullable WidgetElements> leftIcon() {
            return Undefinable.undefined();
        }

        default Optional<RenderableRectangle> containerBackground() {
            return Optional.empty();
        }

        default OptionalInt maxContainerHeight() {
            return OptionalInt.empty();
        }

        default Optional<HorizontalDirection> preferredDirection() {
            return Optional.empty();
        }

        default Optional<Length> minContainerWidth() {
            return Optional.empty();
        }

        default Optional<Length> maxContainerWidth() {
            return Optional.empty();
        }

        default Optional<ScreenRectangle> containerPadding() {
            return Optional.empty();
        }

        default Optional<ScreenRectangle> containerBorder() {
            return Optional.empty();
        }

        default Undefinable<FZPopoverMenuItem.@Nullable Divider> entryDivider() {
            return Undefinable.undefined();
        }

        default Undefinable<FZPopoverMenuItem.@Nullable Divider> sectionDivider() {
            return Undefinable.undefined();
        }
    }

    private static final class PropsImpl extends GuiComponentPropsBase implements Props {
        private final ContainerEventHandler parentContainer;
        private final List<FZPopoverMenuItem> entries;
        private final TriState hideMessage;
        private final TriState closeOnBlur;
        private final Undefinable<@Nullable WidgetElements> leftIcon;
        private final @Nullable RenderableRectangle containerBackground;
        private final @Nullable Integer maxContainerHeight;
        private final @Nullable HorizontalDirection preferredDirection;
        private final @Nullable Length minContainerWidth;
        private final @Nullable Length maxContainerWidth;
        private final @Nullable ScreenRectangle containerPadding;
        private final @Nullable ScreenRectangle containerBorder;
        private final Undefinable<FZPopoverMenuItem.@Nullable Divider> entryDivider;
        private final Undefinable<FZPopoverMenuItem.@Nullable Divider> sectionDivider;

        private PropsImpl(
                ContainerEventHandler parentContainer,
                List<FZPopoverMenuItem> entries,
                TriState hideMessage,
                TriState closeOnBlur,
                Undefinable<@Nullable WidgetElements> leftIcon,
                @Nullable RenderableRectangle containerBackground,
                @Nullable Integer maxContainerHeight,
                @Nullable HorizontalDirection preferredDirection,
                @Nullable Length minContainerWidth,
                @Nullable Length maxContainerWidth,
                @Nullable ScreenRectangle containerPadding,
                @Nullable ScreenRectangle containerBorder,
                Undefinable<FZPopoverMenuItem.@Nullable Divider> entryDivider,
                Undefinable<FZPopoverMenuItem.@Nullable Divider> sectionDivider,
                GuiComponentProps props
        ) {
            super(props);
            this.parentContainer = parentContainer;
            this.entries = entries;
            this.hideMessage = hideMessage;
            this.closeOnBlur = closeOnBlur;
            this.leftIcon = leftIcon;
            this.preferredDirection = preferredDirection;
            this.containerBackground = containerBackground;
            this.maxContainerHeight = maxContainerHeight;
            this.minContainerWidth = minContainerWidth;
            this.maxContainerWidth = maxContainerWidth;
            this.containerPadding = containerPadding;
            this.containerBorder = containerBorder;
            this.entryDivider = entryDivider;
            this.sectionDivider = sectionDivider;
        }

        @Override
        public ContainerEventHandler parentContainer() {
            return parentContainer;
        }

        @Override
        public List<FZPopoverMenuItem> entries() {
            return entries;
        }

        @Override
        public TriState hideMessage() {
            return hideMessage;
        }

        @Override
        public TriState closeOnBlur() {
            return closeOnBlur;
        }

        @Override
        public Undefinable<@Nullable WidgetElements> leftIcon() {
            return leftIcon;
        }

        @Override
        public Optional<RenderableRectangle> containerBackground() {
            return Optional.ofNullable(containerBackground);
        }

        @Override
        public OptionalInt maxContainerHeight() {
            return wrapBoxedInt(maxContainerHeight);
        }

        @Override
        public Optional<Length> minContainerWidth() {
            return Optional.ofNullable(minContainerWidth);
        }

        @Override
        public Optional<Length> maxContainerWidth() {
            return Optional.ofNullable(maxContainerWidth);
        }

        @Override
        public Optional<HorizontalDirection> preferredDirection() {
            return Optional.ofNullable(preferredDirection);
        }

        @Override
        public Optional<ScreenRectangle> containerPadding() {
            return Optional.ofNullable(containerPadding);
        }

        @Override
        public Optional<ScreenRectangle> containerBorder() {
            return Optional.ofNullable(containerBorder);
        }

        @Override
        public Undefinable<FZPopoverMenuItem.@Nullable Divider> entryDivider() {
            return entryDivider;
        }

        @Override
        public Undefinable<FZPopoverMenuItem.@Nullable Divider> sectionDivider() {
            return sectionDivider;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o) &&
                   Objects.equals(parentContainer, other.parentContainer()) &&
                   Objects.equals(entries, other.entries()) &&
                   hideMessage == other.hideMessage() &&
                   closeOnBlur == other.closeOnBlur() &&
                   Objects.equals(leftIcon, other.leftIcon()) &&
                   Objects.equals(containerBackground(), other.containerBackground()) &&
                   Objects.equals(maxContainerHeight(), other.maxContainerHeight()) &&
                   Objects.equals(minContainerWidth(), other.minContainerWidth()) &&
                   Objects.equals(maxContainerWidth(), other.maxContainerWidth()) &&
                   Objects.equals(preferredDirection(), other.preferredDirection()) &&
                   Objects.equals(containerPadding(), other.containerPadding()) &&
                   Objects.equals(containerBorder(), other.containerBorder()) &&
                   Objects.equals(entryDivider(), other.entryDivider()) &&
                   Objects.equals(sectionDivider(), other.sectionDivider());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(),
                    parentContainer,
                    entries,
                    hideMessage,
                    closeOnBlur,
                    leftIcon,
                    containerBackground,
                    maxContainerHeight,
                    minContainerWidth,
                    maxContainerWidth,
                    preferredDirection,
                    containerPadding,
                    containerBorder,
                    entryDivider,
                    sectionDivider
            );
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private static final WidgetRenderables DEFAULT_ENTRY_BACKGROUND;
        private final ContainerEventHandler container;
        private final List<FZPopoverMenuItem> entries = new ArrayList<>();
        private TriState hideMessage = TriState.DEFAULT;
        private TriState closeOnBlur = TriState.DEFAULT;
        private Undefinable<@Nullable WidgetElements> leftIcon = Undefinable.undefined();
        private @Nullable RenderableRectangle containerBackground;
        private @Nullable Integer maxContainerHeight;
        private @Nullable HorizontalDirection preferredDirection;
        private @Nullable Length minContainerWidth;
        private @Nullable Length maxContainerWidth;
        private @Nullable ScreenRectangle containerPadding;
        private @Nullable ScreenRectangle containerBorder;
        private Undefinable<FZPopoverMenuItem.@Nullable Divider> entryDivider = Undefinable.undefined();
        private Undefinable<FZPopoverMenuItem.@Nullable Divider> sectionDivider = Undefinable.undefined();

        static {
            RenderableRectangle background = Renderables.sprite(Fidgetz.id("widget/popovermenu_entry")).withBlend();
            RenderableRectangle higlighted = Renderables.sprite(Fidgetz.id("widget/popovermenu_entry_highlighted")).withBlend();
            DEFAULT_ENTRY_BACKGROUND = new WidgetRenderables(background, background, higlighted);
        }

        private Builder(ContainerEventHandler container) {
            this.container = container;
        }

        public Builder hideMessage(boolean hideMessage) {
            this.hideMessage = TriState.from(hideMessage);
            return this;
        }

        public Builder hideMessage() {
            return hideMessage(true);
        }

        public Builder closeOnBlur(boolean closeOnBlur) {
            this.closeOnBlur = TriState.from(closeOnBlur);
            return this;
        }

        public Builder closeOnBlur() {
            return closeOnBlur(true);
        }

        public Builder leftIcon(@Nullable WidgetElements leftIcon) {
            this.leftIcon = Undefinable.of(leftIcon);
            return this;
        }

        public Builder containerBackground(@Nullable RenderableRectangle containerBackground) {
            this.containerBackground = containerBackground;
            return this;
        }

        public Builder maxContainerHeight(int maxContainerHeight) {
            this.maxContainerHeight = maxContainerHeight;
            return this;
        }

        public Builder minContainerWidth(int minContainerWidth, HorizontalDirection preferredDirection) {
            this.minContainerWidth = Length.fixed(minContainerWidth);
            this.preferredDirection = preferredDirection;
            return this;
        }

        public Builder minContainerWidth(int minContainerWidth) {
            this.minContainerWidth = Length.fixed(minContainerWidth);
            return this;
        }

        public Builder minContainerWidth(float buttonRatio) {
            this.minContainerWidth = Length.relative(buttonRatio);
            return this;
        }

        public Builder maxContainerWidth(int maxContainerWidth) {
            this.maxContainerWidth = Length.fixed(maxContainerWidth);
            return this;
        }

        public Builder maxContainerWidth(float buttonRatio) {
            this.maxContainerWidth = Length.relative(buttonRatio);
            return this;
        }

        public Builder preferredDirection(HorizontalDirection preferredDirection) {
            this.preferredDirection = preferredDirection;
            return this;
        }

        public Builder containerPadding(int left, int top, int right, int bottom) {
            this.containerPadding = ScreenRectangleUtils.insets(left, top, right, bottom);
            return this;
        }

        public Builder containerPadding(int padding) {
            return containerPadding(padding, padding, padding, padding);
        }

        public Builder containerBorder(int left, int top, int right, int bottom) {
            this.containerBorder = ScreenRectangleUtils.insets(left, top, right, bottom);
            return this;
        }

        public Builder containerBorder(int border) {
            return containerBorder(border, border, border, border);
        }

        private FZButton.Builder defaultButtonBuilder() {
            return FZButton.builder()
                    .height(DEFAULT_HEIGHT)
                    .sprites(DEFAULT_ENTRY_BACKGROUND)
                    .leftAlignedMessage();
        }

        public Builder entry(Component message, Runnable selectionHandler) {
            Objects.requireNonNull(selectionHandler, "selectionHandler cannot be null");
            this.entries.add(FZPopoverMenuItem.fromWidget(defaultButtonBuilder().message(message).onPress(selectionHandler).build()));
            return this;
        }

        public Builder entry(UnaryOperator<FZButton.Builder> entryBuilder) {
            this.entries.add(FZPopoverMenuItem.fromWidget(entryBuilder.apply(defaultButtonBuilder()).build()));
            return this;
        }

        public Builder entry(FZPopoverMenuItem entry) {
            this.entries.add(entry);
            return this;
        }

        public Builder entryWidget(AbstractWidget widget) {
            this.entries.add(FZPopoverMenuItem.fromWidget(widget));
            return this;
        }

        public Builder entryWidget(AbstractWidget widget, FZPopoverMenuItem.Settings settings) {
            this.entries.add(FZPopoverMenuItem.fromWidget(widget, settings));
            return this;
        }

        public Builder entryWidget(AbstractWidget widget, UnaryOperator<FZPopoverMenuItem.Settings> settingsBuilder) {
            this.entries.add(FZPopoverMenuItem.fromWidget(widget, settingsBuilder));
            return this;
        }

        public Builder entryWidget(Function<FZPopoverMenuItem.Context, AbstractWidget> widgetFactory) {
            this.entries.add(FZPopoverMenuItem.fromWidget(widgetFactory));
            return this;
        }

        public Builder entryWidget(
                Function<FZPopoverMenuItem.Context, AbstractWidget> widgetFactory,
                FZPopoverMenuItem.Settings settings
        ) {
            this.entries.add(FZPopoverMenuItem.fromWidget(widgetFactory, settings));
            return this;
        }

        public Builder entryWidget(
                Function<FZPopoverMenuItem.Context, AbstractWidget> widgetFactory,
                UnaryOperator<FZPopoverMenuItem.Settings> settingsBuilder
        ) {
            this.entries.add(FZPopoverMenuItem.fromWidget(widgetFactory, settingsBuilder));
            return this;
        }

        public Builder entries(List<? extends FZPopoverMenuItem> entries) {
            this.entries.addAll(entries);
            return this;
        }

        public Builder entryDivider(FZPopoverMenuItem.@Nullable Divider entryDivider) {
            this.entryDivider = Undefinable.of(entryDivider);
            return this;
        }

        public Builder sectionDivider(FZPopoverMenuItem.@Nullable Divider sectionDivider) {
            this.sectionDivider = Undefinable.of(sectionDivider);
            return this;
        }

        public Props toProps() {
            return new PropsImpl(
                    container,
                    List.copyOf(entries),
                    hideMessage,
                    closeOnBlur,
                    leftIcon,
                    containerBackground,
                    maxContainerHeight,
                    preferredDirection,
                    minContainerWidth,
                    maxContainerWidth,
                    containerPadding,
                    containerBorder,
                    entryDivider,
                    sectionDivider,
                    props
            );
        }

        public FZDropdown build() {
            FZDropdown dropdown = new FZDropdown(container);
            dropdown.applyProps(toProps());
            return dropdown;
        }
    }
}
