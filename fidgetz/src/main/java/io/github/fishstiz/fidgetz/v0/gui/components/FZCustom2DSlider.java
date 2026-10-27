package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.fidgetz.v0.utils.NavigationUtils;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public final class FZCustom2DSlider extends AbstractWidget implements FZComponent, FZContextMenu.Source {
    private static final int DEFAULT_CURSOR_SIZE = 8;
    private static final int DEFAULT_WIDTH = 64;
    private static final int DEFAULT_HEIGHT = 64;
    private final boolean bound;
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private WidgetRenderables background = new WidgetRenderables(Renderables.fill(CommonColors.WHITE));
    private WidgetRenderables cursor = new WidgetRenderables(Renderables.fill(CommonColors.BLACK));
    private int cursorSize = DEFAULT_CURSOR_SIZE;
    private ScreenRectangle cursorBounds = ScreenRectangle.empty();
    private ScreenRectangle cachedBounds = super.getRectangle();
    private Consumer<ChangeEvent> changeHandler = FunctionUtils.nopConsumer();
    private Consumer<ClickEvent> clickHandler = FunctionUtils.nopConsumer();
    private Consumer<DragEvent> dragHandler = FunctionUtils.nopConsumer();
    private Consumer<ReleaseEvent> releaseHandler = FunctionUtils.nopConsumer();
    private boolean canChangeValue;
    private boolean dragging;
    private double valueX;
    private double valueY;

    private boolean valueXBound;
    private boolean valueYBound;

    private FZCustom2DSlider(boolean bound) {
        super(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY);
        this.bound = bound;
        updateCursorBounds();
    }

    private static double clamp(double value) {
        return net.minecraft.util.Mth.clamp(value, 0.0, 1.0);
    }

    public double getValueX() {
        return valueX;
    }

    public double getValueY() {
        return valueY;
    }

    private boolean isXBound() {
        return bound && valueXBound;
    }

    private boolean isYBound() {
        return bound && valueYBound;
    }

    private void updateCursorBounds() {
        ScreenRectangle bounds = getRectangle();

        double cursorCenterX = bounds.left() + this.valueX * bounds.width();
        double cursorCenterY = bounds.top() + (1f - this.valueY) * bounds.height();

        int cursorX = (int) (cursorCenterX - this.cursorSize / 2f);
        int cursorY = (int) (cursorCenterY - this.cursorSize / 2f);

        if (ScreenRectangleUtils.unequal(this.cursorBounds, cursorX, cursorY, this.cursorSize, this.cursorSize)) {
            this.cursorBounds = new ScreenRectangle(cursorX, cursorY, this.cursorSize, this.cursorSize);
        }
    }

    private void setBoundX(double x) {
        x = clamp(x);

        this.valueXBound = true;
        if (this.valueX != x) {
            this.valueX = x;
            updateCursorBounds();
        }
    }

    private void setBoundY(double y) {
        y = clamp(y);

        this.valueXBound = true;
        if (this.valueY != y) {
            this.valueY = y;
            updateCursorBounds();
        }
    }

    private void setBoundValues(double x, double y) {
        x = clamp(x);
        y = clamp(y);

        this.valueXBound = true;
        this.valueYBound = true;

        boolean updated = false;

        if (x != this.valueX) {
            this.valueX = x;
            updated = true;
        }

        if (y != this.valueY) {
            this.valueY = y;
            updated = true;
        }

        if (updated) {
            updateCursorBounds();
        }
    }

    private void setValues(double x, double y) {
        x = clamp(x);
        y = clamp(y);

        boolean xChanged = this.valueX != x;
        boolean yChanged = this.valueY != y;

        if (xChanged || yChanged) {
            boolean updated = false;

            if (!isXBound() && xChanged) {
                this.valueX = x;
                updated = true;
            }

            if (!isYBound() && yChanged) {
                this.valueY = y;
                updated = true;
            }

            if (updated) {
                updateCursorBounds();
            }

            this.changeHandler.accept(new ChangeEvent(this, x, y));
        }
    }

    private void setValuesFromMouse(double mouseX, double mouseY) {
        setValues((mouseX - getX()) / getWidth(), 1.0 - (mouseY - getY()) / getHeight());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.canChangeValue = true;
        this.dragging = isActive();
        setValuesFromMouse(mouseX, mouseY);
        clickHandler.accept(new ClickEvent(this));
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dx, double dy) {
        setValuesFromMouse(mouseX, mouseY);
        dragHandler.accept(new DragEvent(this));
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (this.dragging) {
            this.dragging = false;
            super.playDownSound(Minecraft.getInstance().getSoundManager());
            releaseHandler.accept(new ReleaseEvent(this));
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean cursorHovered = ScreenRectangleUtils.containsPoint(cursorBounds, mouseX, mouseY)
                && io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils.containsPointInScissor(graphics, mouseX, mouseY);
        this.isHovered |= cursorHovered;

        this.background.get(isActive(), isFocused() && !this.canChangeValue).extractRenderState(
                graphics,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                mouseX,
                mouseY,
                partialTick
        );

        cursor.get(isActive(), isHovered() || this.canChangeValue).extractRenderState(
                graphics,
                cursorBounds.left(),
                cursorBounds.top(),
                cursorBounds.width(),
                cursorBounds.height(),
                mouseX,
                mouseY,
                partialTick
        );

        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateCursorBounds();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateCursorBounds();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        updateCursorBounds();
    }

    public void setHeight(int height) {
        io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils.setHeight(this, height);
        updateCursorBounds();
    }

    public void setSize(int width, int height) {
        io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils.setSize(this, width, height);
        updateCursorBounds();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || (isActive() && ScreenRectangleUtils.containsPoint(cursorBounds, (int) mouseX, (int) mouseY));
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(cachedBounds, this)) {
            this.cachedBounds = super.getRectangle();
        }
        return super.getRectangle();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CommonInputs.selected(keyCode)) {
            this.canChangeValue = !this.canChangeValue;
            return true;
        }

        if (this.canChangeValue) {
            ScreenDirection screenDirection = NavigationUtils.getDirection(keyCode);

            if (screenDirection != null) {
                switch (screenDirection.getAxis()) {
                    case HORIZONTAL -> {
                        float direction = screenDirection.isPositive() ? 1f : -1f;
                        setValues(this.valueX + direction / (getWidth() - cursorBounds.width()), this.valueY);
                    }
                    case VERTICAL -> {
                        float direction = screenDirection.isPositive() ? -1f : 1f;
                        setValues(this.valueX, this.valueY + direction / (getHeight() - cursorBounds.height()));
                    }
                }
                return true;
            }
        }

        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.canChangeValue = false;
        }
    }

    @Override
    public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent event) {
        ComponentPath path = super.nextFocusPath(event);

        if (event instanceof FocusNavigationEvent.TabNavigation && path != null && path.component() == this) {
            return NavigationUtils.afterFocusEffect(path, (ignored, focused) -> this.canChangeValue = focused);
        }

        return path;
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        propsState.contextEntries.accept(collector);
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return propsState.id;
    }

    private void applyProps(Props props) {
        propsState.apply(this, props);

        props.background().ifPresent(background -> this.background = background);
        props.cursor().ifPresent(cursor -> this.cursor = cursor);
        props.cursorSize().ifPresent(size -> {
            this.cursorSize = size;
            updateCursorBounds();
        });

        props.changeHandler().ifPresent(changeHandler -> this.changeHandler = changeHandler.value());
        props.clickHandler().ifPresent(clickHandler -> this.clickHandler = clickHandler.value());
        props.dragHandler().ifPresent(dragHandler -> this.dragHandler = dragHandler.value());
        props.releaseHandler().ifPresent(releaseHandler -> this.releaseHandler = releaseHandler.value());

        if (props.valueX().isPresent() && props.valueY().isPresent()) {
            setBoundValues(props.valueX().getAsDouble(), props.valueY().getAsDouble());
        } else {
            props.valueY().ifPresentOrElse(this::setBoundX, () -> this.valueYBound = false);
            props.valueY().ifPresentOrElse(this::setBoundY, () -> this.valueYBound = false);
        }
    }

    public static FZCustom2DSlider bind(String key, FZRef<Props> props) {
        FZCustom2DSlider saturationValueArea = new FZCustom2DSlider(true);
        saturationValueArea.applyProps(props.value());
        props.subscribe(key, saturationValueArea::applyProps);
        return saturationValueArea;
    }

    public static Builder builder() {
        return new Builder();
    }

    public record ChangeEvent(FZCustom2DSlider target, double x, double y) {
    }

    public record ClickEvent(FZCustom2DSlider target) {
    }

    public record DragEvent(FZCustom2DSlider target) {
    }

    public record ReleaseEvent(FZCustom2DSlider target) {
    }

    public interface Props extends GuiComponentProps {
        default Optional<WidgetRenderables> background() {
            return Optional.empty();
        }

        default Optional<WidgetRenderables> cursor() {
            return Optional.empty();
        }

        default OptionalInt cursorSize() {
            return OptionalInt.empty();
        }

        default OptionalDouble valueX() {
            return OptionalDouble.empty();
        }

        default OptionalDouble valueY() {
            return OptionalDouble.empty();
        }

        default Optional<FZKeyed<Consumer<ChangeEvent>>> changeHandler() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Consumer<ClickEvent>>> clickHandler() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Consumer<DragEvent>>> dragHandler() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Consumer<ReleaseEvent>>> releaseHandler() {
            return Optional.empty();
        }
    }

    private static final class PropsImpl extends GuiComponentPropsBase implements Props {
        private final @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;
        private final @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler;
        private final @Nullable FZKeyed<Consumer<DragEvent>> dragHandler;
        private final @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler;
        private final @Nullable Double valueX;
        private final @Nullable Double valueY;
        private final @Nullable WidgetRenderables background;
        private final @Nullable WidgetRenderables cursor;
        private final @Nullable Integer cursorSize;

        public PropsImpl(
                GuiComponentProps props,
                @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler,
                @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler,
                @Nullable FZKeyed<Consumer<DragEvent>> dragHandler,
                @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler,
                @Nullable Double valueX,
                @Nullable Double valueY,
                @Nullable WidgetRenderables background,
                @Nullable WidgetRenderables cursor,
                @Nullable Integer cursorSize
        ) {
            super(props);
            this.changeHandler = changeHandler;
            this.clickHandler = clickHandler;
            this.dragHandler = dragHandler;
            this.releaseHandler = releaseHandler;
            this.valueX = valueX;
            this.valueY = valueY;
            this.background = background;
            this.cursor = cursor;
            this.cursorSize = cursorSize;
        }

        @Override
        public Optional<WidgetRenderables> background() {
            return Optional.ofNullable(background);
        }

        @Override
        public Optional<WidgetRenderables> cursor() {
            return Optional.ofNullable(cursor);
        }

        @Override
        public OptionalInt cursorSize() {
            return wrapBoxedInt(cursorSize);
        }

        @Override
        public OptionalDouble valueX() {
            return wrapBoxedDouble(valueX);
        }

        @Override
        public OptionalDouble valueY() {
            return wrapBoxedDouble(valueY);
        }

        @Override
        public Optional<FZKeyed<Consumer<ChangeEvent>>> changeHandler() {
            return Optional.ofNullable(changeHandler);
        }

        @Override
        public Optional<FZKeyed<Consumer<ClickEvent>>> clickHandler() {
            return Optional.ofNullable(clickHandler);
        }

        @Override
        public Optional<FZKeyed<Consumer<DragEvent>>> dragHandler() {
            return Optional.ofNullable(dragHandler);
        }

        @Override
        public Optional<FZKeyed<Consumer<ReleaseEvent>>> releaseHandler() {
            return Optional.ofNullable(releaseHandler);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o) &&
                   Objects.equals(changeHandler(), other.changeHandler()) &&
                   Objects.equals(clickHandler(), other.clickHandler()) &&
                   Objects.equals(dragHandler(), other.dragHandler()) &&
                   Objects.equals(releaseHandler(), other.releaseHandler()) &&
                   Objects.equals(valueX(), other.valueX()) &&
                   Objects.equals(valueY(), other.valueY()) &&
                   Objects.equals(background(), other.background()) &&
                   Objects.equals(cursor(), other.cursor()) &&
                   Objects.equals(cursorSize(), other.cursorSize());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(),
                    changeHandler,
                    clickHandler,
                    dragHandler,
                    releaseHandler,
                    valueX,
                    valueY,
                    background,
                    cursor,
                    cursorSize
            );
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;
        private @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler;
        private @Nullable FZKeyed<Consumer<DragEvent>> dragHandler;
        private @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler;
        private @Nullable Double valueX;
        private @Nullable Double valueY;
        private @Nullable WidgetRenderables background;
        private @Nullable WidgetRenderables cursor;
        private @Nullable Integer cursorSize;

        private Builder() {
        }

        public Builder background(WidgetRenderables background) {
            this.background = background;
            return this;
        }

        public Builder background(RenderableRectangle background) {
            return background(new WidgetRenderables(background));
        }

        public Builder background(ResourceLocation sprite) {
            return background(Renderables.sprite(sprite));
        }

        public Builder cursor(WidgetRenderables cursor) {
            this.cursor = cursor;
            return this;
        }

        public Builder cursor(RenderableRectangle cursor) {
            return cursor(new WidgetRenderables(cursor));
        }

        public Builder cursor(ResourceLocation sprite) {
            return cursor(Renderables.sprite(sprite));
        }

        public Builder cursorSize(int size) {
            this.cursorSize = size;
            return this;
        }

        public Builder valueX(double x) {
            this.valueX = x;
            return this;
        }

        public Builder valueY(double y) {
            this.valueY = y;
            return this;
        }

        public Builder values(double x, double y) {
            return valueX(x).valueY(y);
        }

        public Builder onChange(Consumer<ChangeEvent> changeHandler) {
            this.changeHandler = FZKeyed.selfKey(Objects.requireNonNull(changeHandler, "changeHandler cannot be null"));
            return this;
        }

        public Builder onChange(Object key, Consumer<ChangeEvent> changeHandler) {
            this.changeHandler = new FZKeyed<>(key, Objects.requireNonNull(changeHandler, "changeHandler cannot be null"));
            return this;
        }

        public Builder onClick(Consumer<ClickEvent> clickHandler) {
            this.clickHandler = FZKeyed.selfKey(Objects.requireNonNull(clickHandler, "clickHandler cannot be null"));
            return this;
        }

        public Builder onClick(Object key, Consumer<ClickEvent> clickHandler) {
            this.clickHandler = new FZKeyed<>(key, Objects.requireNonNull(clickHandler, "clickHandler cannot be null"));
            return this;
        }

        public Builder onDrag(Consumer<DragEvent> dragHandler) {
            this.dragHandler = FZKeyed.selfKey(Objects.requireNonNull(dragHandler, "dragHandler cannot be null"));
            return this;
        }

        public Builder onDrag(Object key, Consumer<DragEvent> dragHandler) {
            this.dragHandler = new FZKeyed<>(key, Objects.requireNonNull(dragHandler, "dragHandler cannot be null"));
            return this;
        }

        public Builder onRelease(Consumer<ReleaseEvent> releaseHandler) {
            this.releaseHandler = FZKeyed.selfKey(Objects.requireNonNull(releaseHandler, "releaseHandler cannot be null"));
            return this;
        }

        public Builder onRelease(Object key, Consumer<ReleaseEvent> releaseHandler) {
            this.releaseHandler = new FZKeyed<>(key, Objects.requireNonNull(releaseHandler, "releaseHandler cannot be null"));
            return this;
        }

        public Props toProps() {
            return new PropsImpl(
                    props,
                    changeHandler,
                    clickHandler,
                    dragHandler,
                    releaseHandler,
                    valueX,
                    valueY,
                    background,
                    cursor,
                    cursorSize
            );
        }

        public FZCustom2DSlider build() {
            FZCustom2DSlider saturationValueArea = new FZCustom2DSlider(false);
            saturationValueArea.applyProps(toProps());
            return saturationValueArea;
        }
    }
}
