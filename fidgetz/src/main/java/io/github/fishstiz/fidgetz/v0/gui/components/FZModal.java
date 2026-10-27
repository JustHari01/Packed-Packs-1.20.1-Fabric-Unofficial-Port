package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.layouts.FZComposedLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexElement;
import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.utils.*;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import net.minecraft.util.FastColor;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class FZModal extends FZDialog implements FZComponent, FZContextMenu.Source {
    protected static final float DEFAULT_ALIGNMENT = 0.5F;
    protected static final ScreenRectangle DEFAULT_MARGIN = ScreenRectangle.empty();
    protected static final ScreenRectangle DEFAULT_PADDING = ScreenRectangleUtils.insets(8);
    protected static final RenderableRectangle DEFAULT_BACKDROP = Renderables.fill(FastColor.ARGB32.color(127, 0, 0, 0));
    protected static final RenderableRectangle DEFAULT_BACKGROUND = Renderables.boxShadow(24)
            .then(Renderables.sprite(new ResourceLocation("popup/background")));
    private @Nullable String id;
    private Component message = CommonComponents.EMPTY;
    private FZKeyed<Consumer<FZContextMenu.Collector>> contextEntries = FZKeyed.selfKey(FunctionUtils.nopConsumer());
    private Runnable closeHandler = FunctionUtils.nop();
    private Runnable openHandler = FunctionUtils.nop();
    protected Layout layout;
    protected ScreenRectangle margin = DEFAULT_MARGIN;
    protected ScreenRectangle padding = DEFAULT_PADDING;
    protected @Nullable RenderableRectangle backdrop = DEFAULT_BACKDROP;
    protected @Nullable RenderableRectangle background = DEFAULT_BACKGROUND;
    protected boolean captureClick = true;
    protected boolean closeAfterClickOutOfBounds = true;
    protected boolean captureFocus = true;
    protected int width;
    protected int height;
    protected float alignX = DEFAULT_ALIGNMENT;
    protected float alignY = DEFAULT_ALIGNMENT;
    protected boolean flexWidth = false;
    protected boolean flexHeight = false;
    private boolean bound = false;
    private boolean openBound = false;
    private ScreenRectangle bounds;

    protected FZModal(FZDialogContainer container, Layout layout) {
        super(container);
        this.layout = layout;
        this.bounds = layout.getRectangle();
    }

    public void open() {
        if (unbounded()) {
            setOpen(true);
        }
    }

    public void close() {
        setOpen(false);
    }

    @Override
    protected void setOpen(boolean open) {
        // open/close handlers should manually update open state when bound
        if (unbounded()) {
            super.setOpen(open);
        } else if (open) {
            openHandler.run();
        } else {
            closeHandler.run();
        }
    }

    private void setOpenBound(boolean open) {
        openBound = true;
        super.setOpen(open);
    }

    @Override
    protected void onClose() {
        super.onClose();
        clearWidgets();
        if (captureFocus) {
            refocusLastContainerPath();
        }
        if (unbounded()) {
            closeHandler.run();
        }
    }

    @Override
    protected void onOpen() {
        clearWidgets();
        repositionElements();
        layout.visitWidgets(this::addRenderableWidget);
        super.onOpen();
        if (shouldFocusOnOpen()) {
            ComponentPath initialFocus = nextFocusPath(new FocusNavigationEvent.InitialFocus());
            if (initialFocus != null) {
                initialFocus.applyFocus(true);
            }
        }
        if (unbounded()) {
            openHandler.run();
        }
    }

    @Override
    public ScreenRectangle getRectangle() {
        return bounds;
    }

    @Override
    public boolean shouldCaptureClick() {
        return captureClick;
    }

    @Override
    public boolean shouldCloseAfterClickOutOfBounds() {
        return closeAfterClickOutOfBounds;
    }

    @Override
    public boolean shouldCaptureFocus() {
        return captureFocus;
    }

    @Override
    protected void extractDialogRenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (backdrop != null) {
            ScreenRectangle bounds = container.getRectangle();
            backdrop.extractRenderState(graphics, bounds.left(), bounds.top(), bounds.width(), bounds.height(), mouseX, mouseY, partialTick);
        }
        if (background != null) {
            ScreenRectangle bounds = getRectangle();
            background.extractRenderState(graphics, bounds.left(), bounds.top(), bounds.width(), bounds.height(), mouseX, mouseY, partialTick);
        }
        super.extractDialogRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, message);
        super.updateNarration(output);
    }

    @Override
    public void repositionElements() {
        ScreenRectangle containerBounds = container.getRectangle();
        int minWidth = flexWidth ? MathUtils.eitherOptionalMin(width, containerBounds.width()) : width;
        int minHeight = flexHeight ? MathUtils.eitherOptionalMin(height, containerBounds.height()) : height;

        if (layout instanceof FZFlexElement flexElement) {
            if (minWidth > 0 && minHeight > 0) {
                flexElement.fidgetz$setSize(minWidth, minHeight);
            } else if (minWidth > 0) {
                flexElement.fidgetz$setWidth(minWidth);
            } else if (minHeight > 0) {
                flexElement.fidgetz$setHeight(minHeight);
            }
        }

        FZComposedLayout.contain(container, layout)
                .padding(margin.left(), margin.top(), margin.right(), margin.bottom())
                .clamp()
                .arrange();

        ScreenRectangle layoutBounds = layout.getRectangle();
        int newWidth = MathUtils.clampOptionalMax(layoutBounds.width(), minWidth, containerBounds.width());
        int newHeight = MathUtils.clampOptionalMax(layoutBounds.height(), minHeight, containerBounds.height());
        bounds = new ScreenRectangle(layoutBounds.left(), layoutBounds.top(), newWidth, newHeight);
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        contextEntries.value().accept(collector);
        FZContextMenu.Source.super.fidgetz$updateContextEntries(x, y, collector);
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return id;
    }

    protected boolean unbounded() {
        return !bound || !openBound;
    }

    private static void ifNonDefault(TriState triState, Consumer<TriState> consumer) {
        if (triState != TriState.DEFAULT) {
            consumer.accept(triState);
        }
    }

    protected void applyProps(Props props) {
        FZDialogContainer newContainer = props.container();
        if (newContainer != container) {
            throw new UnsupportedOperationException("Updating the container of FZModal is not supported.");
        }

        props.id().ifPresent(id -> this.id = id);
        props.popoverOrder().ifPresent(order -> this.popoverOrder = order);
        props.message().ifPresent(message -> this.message = message);
        props.contextEntries().ifPresent(contextEntries -> this.contextEntries = contextEntries);

        props.backdrop().ifDefined(backdrop -> this.backdrop = backdrop);
        props.background().ifDefined(background -> this.background = background);
        props.openHandler().ifPresent(handler -> this.openHandler = handler.value());
        props.closeHandler().ifPresent(handler -> this.closeHandler = handler.value());

        ifNonDefault(props.captureClick(), captureClick -> this.captureClick = captureClick.toBoolean(true));
        ifNonDefault(props.closeAfterClickOutOfBounds(), close -> this.closeAfterClickOutOfBounds = close.toBoolean(true));
        ifNonDefault(props.captureFocus(), captureFocus -> this.captureFocus = captureFocus.toBoolean(true));

        props.width().ifPresent(width -> this.width = width);
        props.height().ifPresent(height -> this.height = height);
        props.margin().ifPresent(margin -> this.margin = margin);
        props.padding().ifPresent(padding -> this.padding = padding);
        props.alignmentX().ifPresent(alignX -> this.alignX = alignX);
        props.alignmentY().ifPresent(alignY -> this.alignY = alignY);
        ifNonDefault(props.flexWidth(), flexWidth -> this.flexWidth = flexWidth.toBoolean(false));
        ifNonDefault(props.flexHeight(), flexHeight -> this.flexHeight = flexHeight.toBoolean(false));

        Layout baseLayout = props.layout();
        FZComposedLayout.Contained composer = FZComposedLayout.contain(container, baseLayout);

        if (!ScreenRectangleUtils.isInsetsEmpty(padding)) {
            composer.padding(padding.left(), padding.top(), padding.right(), padding.bottom());
        }
        if (alignX > 0 || alignY > 0) {
            composer.align(alignX, alignY);
        }

        this.layout = composer.clamp().get();

        if (isOpen()) {
            repositionElements();
        }

        ifNonDefault(props.open(), open -> setOpenBound(open.toBoolean(isOpen())));
    }

    public static FZModal bind(String key, FZRef<Props> ref) {
        Props props = ref.value();
        FZModal modal = new FZModal(props.container(), props.layout());
        modal.bound = true;
        modal.applyProps(ref.value());
        ref.subscribe(key, modal::applyProps);
        return modal;
    }

    public static Builder builder(FZDialogContainer container, Layout layout) {
        return new Builder(container, layout);
    }

    public interface Props {
        FZDialogContainer container();

        Layout layout();

        default Optional<String> id() {
            return Optional.empty();
        }

        default OptionalInt width() {
            return OptionalInt.empty();
        }

        default OptionalInt height() {
            return OptionalInt.empty();
        }

        default Optional<Component> message() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Consumer<FZContextMenu.Collector>>> contextEntries() {
            return Optional.empty();
        }

        default TriState open() {
            return TriState.DEFAULT;
        }

        default Optional<FZKeyed<Runnable>> closeHandler() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Runnable>> openHandler() {
            return Optional.empty();
        }

        default Optional<ScreenRectangle> margin() {
            return Optional.empty();
        }

        default Optional<ScreenRectangle> padding() {
            return Optional.empty();
        }

        default Undefinable<@Nullable RenderableRectangle> backdrop() {
            return Undefinable.undefined();
        }

        default Undefinable<@Nullable RenderableRectangle> background() {
            return Undefinable.undefined();
        }

        default TriState captureClick() {
            return TriState.DEFAULT;
        }

        default TriState closeAfterClickOutOfBounds() {
            return TriState.DEFAULT;
        }

        default TriState captureFocus() {
            return TriState.DEFAULT;
        }

        default Optional<Float> alignmentX() {
            return Optional.empty();
        }

        default Optional<Float> alignmentY() {
            return Optional.empty();
        }

        default TriState flexWidth() {
            return TriState.DEFAULT;
        }

        default TriState flexHeight() {
            return TriState.DEFAULT;
        }

        default OptionalInt popoverOrder() {
            return OptionalInt.empty();
        }
    }

    private static final class PropsImpl implements Props {
        private final FZDialogContainer container;
        private final @Nullable String id;
        private final @Nullable Integer width;
        private final @Nullable Integer height;
        private final @Nullable Component message;
        private final @Nullable FZKeyed<Consumer<FZContextMenu.Collector>> contextEntries;
        private final Layout layout;
        private final TriState open;
        private final @Nullable FZKeyed<Runnable> closeHandler;
        private final @Nullable FZKeyed<Runnable> openHandler;
        private final @Nullable ScreenRectangle margin;
        private final @Nullable ScreenRectangle padding;
        private final Undefinable<@Nullable RenderableRectangle> backdrop;
        private final Undefinable<@Nullable RenderableRectangle> background;
        private final TriState captureClick;
        private final TriState closeAfterClickOutOfBounds;
        private final TriState captureFocus;
        private final @Nullable Float alignmentX;
        private final @Nullable Float alignmentY;
        private final TriState flexWidth;
        private final TriState flexHeight;
        private final @Nullable Integer popoverOrder;

        private PropsImpl(
                FZDialogContainer container,
                @Nullable String id,
                @Nullable Integer width,
                @Nullable Integer height,
                @Nullable Component message,
                @Nullable FZKeyed<Consumer<FZContextMenu.Collector>> contextEntries,
                Layout layout,
                TriState open,
                @Nullable FZKeyed<Runnable> closeHandler,
                @Nullable FZKeyed<Runnable> openHandler,
                @Nullable ScreenRectangle margin,
                @Nullable ScreenRectangle padding,
                Undefinable<@Nullable RenderableRectangle> backdrop,
                Undefinable<@Nullable RenderableRectangle> background,
                TriState captureClick,
                TriState closeAfterClickOutOfBounds,
                TriState captureFocus,
                @Nullable Float alignmentX,
                @Nullable Float alignmentY,
                TriState flexWidth,
                TriState flexHeight,
                @Nullable Integer popoverOrder
        ) {
            this.container = container;
            this.id = id;
            this.width = width;
            this.height = height;
            this.message = message;
            this.contextEntries = contextEntries;
            this.layout = layout;
            this.open = open;
            this.closeHandler = closeHandler;
            this.openHandler = openHandler;
            this.margin = margin;
            this.padding = padding;
            this.backdrop = backdrop;
            this.background = background;
            this.captureClick = captureClick;
            this.closeAfterClickOutOfBounds = closeAfterClickOutOfBounds;
            this.captureFocus = captureFocus;
            this.alignmentX = alignmentX;
            this.alignmentY = alignmentY;
            this.flexWidth = flexWidth;
            this.flexHeight = flexHeight;
            this.popoverOrder = popoverOrder;
        }

        @Override
        public FZDialogContainer container() {
            return container;
        }

        @Override
        public Optional<String> id() {
            return Optional.ofNullable(id);
        }

        @Override
        public OptionalInt width() {
            return width == null ? OptionalInt.empty() : OptionalInt.of(width);
        }

        @Override
        public OptionalInt height() {
            return height == null ? OptionalInt.empty() : OptionalInt.of(height);
        }

        @Override
        public Optional<Component> message() {
            return Optional.ofNullable(message);
        }

        @Override
        public Optional<FZKeyed<Consumer<FZContextMenu.Collector>>> contextEntries() {
            return Optional.ofNullable(contextEntries);
        }

        @Override
        public Layout layout() {
            return layout;
        }

        @Override
        public TriState open() {
            return open;
        }

        @Override
        public Optional<FZKeyed<Runnable>> closeHandler() {
            return Optional.ofNullable(closeHandler);
        }

        @Override
        public Optional<FZKeyed<Runnable>> openHandler() {
            return Optional.ofNullable(openHandler);
        }

        @Override
        public Optional<ScreenRectangle> margin() {
            return Optional.ofNullable(margin);
        }

        @Override
        public Optional<ScreenRectangle> padding() {
            return Optional.ofNullable(padding);
        }

        @Override
        public Undefinable<@Nullable RenderableRectangle> backdrop() {
            return backdrop;
        }

        @Override
        public Undefinable<@Nullable RenderableRectangle> background() {
            return background;
        }

        @Override
        public TriState captureClick() {
            return captureClick;
        }

        @Override
        public TriState closeAfterClickOutOfBounds() {
            return closeAfterClickOutOfBounds;
        }

        @Override
        public TriState captureFocus() {
            return captureFocus;
        }

        @Override
        public Optional<Float> alignmentX() {
            return Optional.ofNullable(alignmentX);
        }

        @Override
        public Optional<Float> alignmentY() {
            return Optional.ofNullable(alignmentY);
        }

        @Override
        public TriState flexWidth() {
            return flexWidth;
        }

        @Override
        public TriState flexHeight() {
            return flexHeight;
        }

        @Override
        public OptionalInt popoverOrder() {
            return popoverOrder == null ? OptionalInt.empty() : OptionalInt.of(popoverOrder);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Props other)) return false;
            return Objects.equals(container, other.container()) &&
                   Objects.equals(id(), other.id()) &&
                   Objects.equals(width(), other.width()) &&
                   Objects.equals(height(), other.height()) &&
                   Objects.equals(message(), other.message()) &&
                   Objects.equals(contextEntries(), other.contextEntries()) &&
                   Objects.equals(layout, other.layout()) &&
                   open == other.open() &&
                   Objects.equals(closeHandler(), other.closeHandler()) &&
                   Objects.equals(openHandler(), other.openHandler()) &&
                   Objects.equals(margin(), other.margin()) &&
                   Objects.equals(padding(), other.padding()) &&
                   Objects.equals(backdrop(), other.backdrop()) &&
                   Objects.equals(background(), other.background()) &&
                   captureClick == other.captureClick() &&
                   closeAfterClickOutOfBounds == other.closeAfterClickOutOfBounds() &&
                   captureFocus == other.captureFocus() &&
                   Objects.equals(alignmentX(), other.alignmentX()) &&
                   Objects.equals(alignmentY(), other.alignmentY()) &&
                   flexWidth == other.flexWidth() &&
                   flexHeight == other.flexHeight() &&
                   Objects.equals(popoverOrder(), other.popoverOrder());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    container,
                    id,
                    width,
                    height,
                    message,
                    contextEntries,
                    layout,
                    open,
                    closeHandler,
                    openHandler,
                    margin,
                    padding,
                    backdrop,
                    background,
                    captureClick,
                    closeAfterClickOutOfBounds,
                    captureFocus,
                    alignmentX,
                    alignmentY,
                    flexWidth,
                    flexHeight,
                    popoverOrder
            );
        }
    }

    public static final class Builder {
        private final FZDialogContainer container;
        private final Layout layout;
        private @Nullable String id;
        private @Nullable Integer width;
        private @Nullable Integer height;
        private @Nullable Component message;
        private @Nullable FZKeyed<Consumer<FZContextMenu.Collector>> contextEntries;
        private TriState open = TriState.DEFAULT;
        private @Nullable FZKeyed<Runnable> closeHandler;
        private @Nullable FZKeyed<Runnable> openHandler;
        private @Nullable ScreenRectangle margin;
        private @Nullable ScreenRectangle padding;
        private Undefinable<@Nullable RenderableRectangle> background = Undefinable.undefined();
        private Undefinable<@Nullable RenderableRectangle> backdrop = Undefinable.undefined();
        private TriState captureClick = TriState.DEFAULT;
        private TriState closeAfterClickOutOfBounds = TriState.DEFAULT;
        private TriState captureFocus = TriState.DEFAULT;
        private @Nullable Float alignmentX;
        private @Nullable Float alignmentY;
        private TriState flexWidth = TriState.DEFAULT;
        private TriState flexHeight = TriState.DEFAULT;
        private @Nullable Integer popoverOrder;

        Builder(FZDialogContainer container, Layout layout) {
            this.container = container;
            this.layout = layout;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder size(int width, int height) {
            return width(width).height(height);
        }

        public Builder message(@Nullable Component message) {
            this.message = message;
            return this;
        }

        public Builder contextEntries(FZPopoverMenuItem entry, FZPopoverMenuItem... rest) {
            Objects.requireNonNull(entry, "entry cannot be null");
            this.contextEntries = FZKeyed.selfKey(collector -> {
                collector.addEntry(entry);
                for (FZPopoverMenuItem e : rest) {
                    collector.addEntry(Objects.requireNonNull(e, "entry cannot be null"));
                }
            });
            return this;
        }

        public Builder contextEntries(Consumer<FZContextMenu.Collector> contextSupplier) {
            this.contextEntries = FZKeyed.selfKey(Objects.requireNonNull(contextSupplier, "contextSupplier cannot be null"));
            return this;
        }

        public Builder open(boolean open) {
            this.open = TriState.from(open);
            return this;
        }

        public Builder open() {
            return open(true);
        }

        public Builder onOpen(FZKeyed<Runnable> openHandler) {
            this.openHandler = Objects.requireNonNull(openHandler, "openHandler cannot be null");
            return this;
        }

        public Builder onOpen(Runnable openHandler) {
            this.openHandler = FZKeyed.selfKey(Objects.requireNonNull(openHandler, "openHandler cannot be null"));
            return this;
        }

        public Builder onClose(FZKeyed<Runnable> closeHandler) {
            this.closeHandler = Objects.requireNonNull(closeHandler, "closeHandler cannot be null");
            return this;
        }

        public Builder onClose(Runnable closeHandler) {
            this.closeHandler = FZKeyed.selfKey(Objects.requireNonNull(closeHandler, "closeHandler cannot be null"));
            return this;
        }

        public Builder margin(int margin) {
            this.margin = ScreenRectangleUtils.insets(margin);
            return this;
        }

        public Builder margin(int left, int top, int right, int bottom) {
            this.margin = ScreenRectangleUtils.insets(left, top, right, bottom);
            return this;
        }

        public Builder padding(int padding) {
            this.padding = ScreenRectangleUtils.insets(padding);
            return this;
        }

        public Builder padding(int left, int top, int right, int bottom) {
            this.padding = ScreenRectangleUtils.insets(left, top, right, bottom);
            return this;
        }

        public Builder backdrop(@Nullable RenderableRectangle backdrop) {
            this.backdrop = Undefinable.of(backdrop);
            return this;
        }

        public Builder background(@Nullable RenderableRectangle background) {
            this.background = Undefinable.of(background);
            return this;
        }

        public Builder captureClick(boolean captureClick) {
            this.captureClick = TriState.from(captureClick);
            return this;
        }

        public Builder captureClick() {
            return captureClick(true);
        }

        public Builder closeAfterClickOutOfBounds(boolean closeAfterClickOutOfBounds) {
            this.closeAfterClickOutOfBounds = TriState.from(closeAfterClickOutOfBounds);
            return this;
        }

        public Builder closeAfterClickOutOfBounds() {
            return closeAfterClickOutOfBounds(true);
        }

        public Builder captureFocus(boolean captureFocus) {
            this.captureFocus = TriState.from(captureFocus);
            return this;
        }

        public Builder captureFocus() {
            return captureFocus(true);
        }

        public Builder alignX(float alignX) {
            this.alignmentX = net.minecraft.util.Mth.clamp(0f, alignX, 1f);
            return this;
        }

        public Builder alignY(float alignY) {
            this.alignmentY = net.minecraft.util.Mth.clamp(0f, alignY, 1f);
            return this;
        }

        public Builder align(float alignX, float alignY) {
            return alignX(alignX).alignY(alignY);
        }

        public Builder centered(boolean centered) {
            this.alignmentX = net.minecraft.util.Mth.clamp(0f, centered ? 0.5f : alignmentX == null ? 0f : alignmentX, 1f);
            this.alignmentY = net.minecraft.util.Mth.clamp(0f, centered ? 0.5f : alignmentY == null ? 0f : alignmentY, 1f);
            return this;
        }

        public Builder centered() {
            return centered(true);
        }

        public Builder uncentered() {
            return centered(false);
        }

        public Builder alignTopLeft() {
            return alignX(0f).alignY(0f);
        }

        public Builder alignTopCenter() {
            return alignX(0.5f).alignY(0f);
        }

        public Builder alignTopRight() {
            return alignX(1f).alignY(0f);
        }

        public Builder alignMiddleLeft() {
            return alignX(0f).alignY(0.5f);
        }

        public Builder alignMiddleRight() {
            return alignX(1f).alignY(0.5f);
        }

        public Builder alignBottomLeft() {
            return alignX(0f).alignY(1f);
        }

        public Builder alignBottomCenter() {
            return alignX(0.5f).alignY(1f);
        }

        public Builder alignBottomRight() {
            return alignX(1f).alignY(1f);
        }

        public Builder flexWidth(boolean flex) {
            this.flexWidth = TriState.from(flex);
            return this;
        }

        public Builder flexWidth() {
            return flexWidth(true);
        }

        public Builder flexHeight(boolean flex) {
            this.flexHeight = TriState.from(flex);
            return this;
        }

        public Builder flexHeight() {
            return flexHeight(true);
        }

        public Builder flex() {
            return flexWidth().flexHeight();
        }

        public Builder popoverOrder(int order) {
            this.popoverOrder = order;
            return this;
        }

        public Props toProps() {
            return new PropsImpl(
                    container,
                    id,
                    width,
                    height,
                    message,
                    contextEntries,
                    layout,
                    open,
                    closeHandler,
                    openHandler,
                    margin,
                    padding,
                    backdrop,
                    background,
                    captureClick,
                    closeAfterClickOutOfBounds,
                    captureFocus,
                    alignmentX,
                    alignmentY,
                    flexWidth,
                    flexHeight,
                    popoverOrder
            );
        }

        public FZModal build() {
            FZModal modal = new FZModal(container, layout);
            modal.applyProps(toProps());
            return modal;
        }

        public FZModal buildAndOpen() {
            FZModal modal = build();
            modal.setOpen(true);
            return modal;
        }
    }
}
