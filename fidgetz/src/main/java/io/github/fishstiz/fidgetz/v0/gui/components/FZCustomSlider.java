package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.inject.interfaces.AbstractSliderButtonAccess;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;

public class FZCustomSlider extends AbstractSliderButton implements FZComponent, FZContextMenu.Source {
    private static final int DEFAULT_WIDTH = 64;
    private static final int DEFAULT_HEIGHT = 8;
    private static final int DEFAULT_THUMB_WIDTH = 6;
    private static final int DEFAULT_THUMB_OUTSETS = 1;
    private final boolean bound;
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private ScreenRectangle cachedBounds = super.getRectangle();
    private ScreenRectangle thumbBounds = ScreenRectangle.empty();
    private WidgetRenderables thumb = new WidgetRenderables(Renderables.fill(CommonColors.BLACK));
    private WidgetRenderables background = new WidgetRenderables(Renderables.fill(CommonColors.WHITE));
    private Consumer<ChangeEvent> changeHandler = FunctionUtils.nopConsumer();
    private Consumer<ClickEvent> clickHandler = FunctionUtils.nopConsumer();
    private Consumer<DragEvent> dragHandler = FunctionUtils.nopConsumer();
    private Consumer<ReleaseEvent> releaseHandler = FunctionUtils.nopConsumer();
    private boolean dragging = false;
    private int thumbWidth = DEFAULT_THUMB_WIDTH;
    private int thumbOutsets = DEFAULT_THUMB_OUTSETS;
    private double min = 0;
    private double max = 1;
    private double step = 0;
    private double mappedValue;

    private boolean valueBound;
    private double previousRawValue;
    private double pendingRawValue = -1;
    private double previousValue;

    private FZCustomSlider(boolean bound) {
        super(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY, 0);
        this.bound = bound;
        updateThumbBounds();
    }

    private void updateThumbBounds() {
        ScreenRectangle bounds = getRectangle();
        int thumbCenter = (int) (bounds.left() + this.value * bounds.width());
        int left = (thumbCenter - this.thumbWidth / 2) - this.thumbOutsets;
        int top = bounds.top() - this.thumbOutsets;
        int width = this.thumbWidth + this.thumbOutsets * 2;
        int height = bounds.height() + this.thumbOutsets * 2;

        if (ScreenRectangleUtils.unequal(this.thumbBounds, left, top, width, height)) {
            this.thumbBounds = new ScreenRectangle(left, top, width, height);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean thumbHovered = ScreenRectangleUtils.containsPoint(thumbBounds, mouseX, mouseY)
                && io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils.containsPointInScissor(graphics, mouseX, mouseY);
        this.isHovered |= thumbHovered;

        background.get(isActive(), isFocused() && !canChangeValue()).extractRenderState(
                graphics,
                getX(),
                getY(),
                getWidth(),
                getHeight(),
                mouseX,
                mouseY,
                partialTick
        );

        thumb.get(isActive(), isHovered() || canChangeValue()).extractRenderState(
                graphics,
                thumbBounds.left(),
                thumbBounds.top(),
                thumbBounds.width(),
                thumbBounds.height(),
                mouseX,
                mouseY,
                partialTick
        );

        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);
        }
    }

    private boolean isBound() {
        return bound && valueBound;
    }

    private double getRawValue() {
        return this.value;
    }

    public double getValue() {
        return this.mappedValue;
    }

    private double unmapValue(double mappedValue) {
        if (this.max <= this.min) return 0.0;
        double clamped = net.minecraft.util.Mth.clamp(mappedValue, this.min, this.max);
        return (clamped - this.min) / (this.max - this.min);
    }

    private double mapValue(double rawValue) {
        double clamped = this.min + (rawValue * (this.max - this.min));
        return step > 0 ? Math.round(clamped / this.step) * this.step : clamped;
    }

    private void setBoundValue(double newValue) {
        this.valueBound = true;
        this.mappedValue = net.minecraft.util.Mth.clamp(newValue, this.min, this.max);
        this.previousValue = this.mappedValue;

        if (this.pendingRawValue >= 0 && mapValue(this.pendingRawValue) == this.mappedValue) {
            this.value = this.pendingRawValue;
        } else if (mapValue(this.previousRawValue) == this.mappedValue) {
            this.value = this.previousRawValue;
        } else {
            if (this.mappedValue == this.min) {
                this.value = 0;
            } else if (this.mappedValue == this.max) {
                this.value = 1;
            } else {
                this.value = unmapValue(newValue);
            }
        }

        this.pendingRawValue = -1;
        this.previousRawValue = this.value;

        updateThumbBounds();
    }

    @Override
    protected void applyValue() {
        double rawValue = getRawValue();
        double newValue = mapValue(rawValue);

        if (this.previousValue != newValue) {
            if (isBound()) {
                this.value = previousRawValue;
                this.pendingRawValue = rawValue;
            } else {
                this.mappedValue = newValue;
                this.previousValue = newValue;
                updateThumbBounds();
            }

            this.changeHandler.accept(new ChangeEvent(this, newValue));
        }

        this.previousRawValue = getRawValue();
    }

    @Override
    protected void updateMessage() {
    }

    private boolean canChangeValue() {
        return ((AbstractSliderButtonAccess) this).fidgetz$canChangeValue();
    }

    private void setCanChangeValue(boolean canChangeValue) {
        ((AbstractSliderButtonAccess) this).fidgetz$setCanChangeValue(canChangeValue);
    }

    private void setWhyTFIsThisPrivateForAnAbstractClassMethodValue(double value) {
        ((AbstractSliderButtonAccess) this).fidgetz$setValue(value);
    }

    private void setValueFromMouse(double mouseX) {
        setWhyTFIsThisPrivateForAnAbstractClassMethodValue((mouseX - getX()) / (double) getWidth());
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.dragging = isActive();
        setValueFromMouse(mouseX);
        clickHandler.accept(new ClickEvent(this));
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dx, double dy) {
        if (this.dragging) {
            setValueFromMouse(mouseX);
            dragHandler.accept(new DragEvent(this));
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (this.dragging) {
            this.dragging = false;
            super.onRelease(mouseX, mouseY);
            releaseHandler.accept(new ReleaseEvent(this));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CommonInputs.selected(keyCode)) {
            setCanChangeValue(!canChangeValue());;
            return true;
        }

        if (canChangeValue() && (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT)) {
            float direction = keyCode == InputConstants.KEY_LEFT ? -1f : 1f;
            setWhyTFIsThisPrivateForAnAbstractClassMethodValue(this.value + direction / (this.width - this.thumbWidth));
            return true;
        }

        return false;
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateThumbBounds();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateThumbBounds();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        updateThumbBounds();
    }

    public void setHeight(int height) {
        io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils.setHeight(this, height);
        updateThumbBounds();
    }

    public void setSize(int width, int height) {
        io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils.setSize(this, width, height);
        updateThumbBounds();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || (isActive() && ScreenRectangleUtils.containsPoint(thumbBounds, (int) mouseX, (int) mouseY));
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
        return this.cachedBounds;
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
        props.min().ifPresent(min -> this.min = min);
        props.max().ifPresent(max -> this.max = max);
        props.step().ifPresent(step -> this.step = step);
        props.background().ifPresent(background -> this.background = background);
        props.thumb().ifPresent(thumb -> this.thumb = thumb);
        props.thumbWidth().ifPresent(thumbWidth -> {
            this.thumbWidth = thumbWidth;
            updateThumbBounds();
        });
        props.thumbOutsets().ifPresent(thumbOutsets -> {
            this.thumbOutsets = thumbOutsets;
            updateThumbBounds();
        });
        props.changeHandler().ifPresent(changeHandler -> this.changeHandler = changeHandler.value());
        props.clickHandler().ifPresent(clickHandler -> this.clickHandler = clickHandler.value());
        props.dragHandler().ifPresent(dragHandler -> this.dragHandler = dragHandler.value());
        props.releaseHandler().ifPresent(releaseHandler -> this.releaseHandler = releaseHandler.value());
        props.value().ifPresentOrElse(this::setBoundValue, () -> this.valueBound = false);
    }

    public static FZCustomSlider bind(String key, FZRef<Props> props) {
        FZCustomSlider slider = new FZCustomSlider(true);
        slider.applyProps(props.value());
        props.subscribe(key, slider::applyProps);
        return slider;
    }

    public static Builder builder() {
        return new Builder();
    }

    public record ChangeEvent(FZCustomSlider target, double value) {
    }

    public record ClickEvent(FZCustomSlider target) {
    }

    public record DragEvent(FZCustomSlider target) {
    }

    public record ReleaseEvent(FZCustomSlider target) {
    }

    public interface Props extends GuiComponentProps {
        default OptionalDouble value() {
            return OptionalDouble.empty();
        }

        default OptionalDouble min() {
            return OptionalDouble.empty();
        }

        default OptionalDouble max() {
            return OptionalDouble.empty();
        }

        default OptionalDouble step() {
            return OptionalDouble.empty();
        }

        default Optional<WidgetRenderables> background() {
            return Optional.empty();
        }

        default Optional<WidgetRenderables> thumb() {
            return Optional.empty();
        }

        default OptionalInt thumbWidth() {
            return OptionalInt.empty();
        }

        default OptionalInt thumbOutsets() {
            return OptionalInt.empty();
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
        private final @Nullable Double value;
        private final @Nullable Double min;
        private final @Nullable Double max;
        private final @Nullable Double step;
        private final @Nullable WidgetRenderables background;
        private final @Nullable WidgetRenderables thumb;
        private final @Nullable Integer thumbWidth;
        private final @Nullable Integer thumbOutsets;

        private PropsImpl(
                GuiComponentProps props,
                @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler,
                @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler,
                @Nullable FZKeyed<Consumer<DragEvent>> dragHandler,
                @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler,
                @Nullable Double value,
                @Nullable Double min,
                @Nullable Double max,
                @Nullable Double step,
                @Nullable WidgetRenderables background,
                @Nullable WidgetRenderables thumb,
                @Nullable Integer thumbWidth,
                @Nullable Integer thumbOutsets
        ) {
            super(props);
            this.changeHandler = changeHandler;
            this.clickHandler = clickHandler;
            this.dragHandler = dragHandler;
            this.releaseHandler = releaseHandler;
            this.value = value;
            this.min = min;
            this.max = max;
            this.step = step;
            this.background = background;
            this.thumb = thumb;
            this.thumbWidth = thumbWidth;
            this.thumbOutsets = thumbOutsets;
        }

        @Override
        public OptionalDouble value() {
            return wrapBoxedDouble(value);
        }

        @Override
        public OptionalDouble min() {
            return wrapBoxedDouble(min);
        }

        @Override
        public OptionalDouble max() {
            return wrapBoxedDouble(max);
        }

        @Override
        public OptionalDouble step() {
            return wrapBoxedDouble(step);
        }

        @Override
        public Optional<WidgetRenderables> background() {
            return Optional.ofNullable(background);
        }

        @Override
        public Optional<WidgetRenderables> thumb() {
            return Optional.ofNullable(thumb);
        }

        @Override
        public OptionalInt thumbWidth() {
            return wrapBoxedInt(thumbWidth);
        }

        @Override
        public OptionalInt thumbOutsets() {
            return wrapBoxedInt(thumbOutsets);
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
                   Objects.equals(value(), other.value()) &&
                   Objects.equals(min(), other.min()) &&
                   Objects.equals(max(), other.max()) &&
                   Objects.equals(step(), other.step()) &&
                   Objects.equals(background(), other.background()) &&
                   Objects.equals(thumb(), other.thumb()) &&
                   Objects.equals(thumbWidth(), other.thumbWidth()) &&
                   Objects.equals(thumbOutsets(), other.thumbOutsets()) &&
                   Objects.equals(changeHandler(), other.changeHandler()) &&
                   Objects.equals(clickHandler(), other.clickHandler()) &&
                   Objects.equals(dragHandler(), other.dragHandler()) &&
                   Objects.equals(releaseHandler(), other.releaseHandler());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(),
                    value,
                    min,
                    max,
                    step,
                    background,
                    thumb,
                    changeHandler,
                    clickHandler,
                    dragHandler,
                    releaseHandler
            );
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;
        private @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler;
        private @Nullable FZKeyed<Consumer<DragEvent>> dragHandler;
        private @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler;
        private @Nullable Double value;
        private @Nullable Double min = null;
        private @Nullable Double max = null;
        private @Nullable Double step = null;
        private @Nullable WidgetRenderables background;
        private @Nullable WidgetRenderables thumb;
        private @Nullable Integer thumbWidth;
        private @Nullable Integer thumbOutsets;

        private Builder() {
        }

        public Builder value(double value) {
            this.value = value;
            return this;
        }

        public Builder min(double min) {
            this.min = min;
            return this;
        }

        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder step(double step) {
            this.step = step;
            return this;
        }

        public Builder background(WidgetRenderables background) {
            this.background = background;
            return this;
        }

        public Builder background(RenderableRectangle background) {
            return background(new WidgetRenderables(background));
        }

        public Builder background(ResourceLocation background) {
            return background(new WidgetRenderables(Renderables.sprite(background)));
        }

        public Builder thumb(WidgetRenderables thumb) {
            this.thumb = thumb;
            return this;
        }

        public Builder thumb(RenderableRectangle thumb) {
            return thumb(new WidgetRenderables(thumb));
        }

        public Builder thumb(ResourceLocation thumb) {
            return thumb(new WidgetRenderables(Renderables.sprite(thumb)));
        }

        public Builder thumbWidth(int width) {
            this.thumbWidth = width;
            return this;
        }

        public Builder thumbOutsets(int outsets) {
            this.thumbOutsets = outsets;
            return this;
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
                    value,
                    min,
                    max,
                    step,
                    background,
                    thumb,
                    thumbWidth,
                    thumbOutsets
            );
        }

        public FZCustomSlider build() {
            FZCustomSlider slider = new FZCustomSlider(false);
            slider.applyProps(toProps());
            return slider;
        }
    }
}
