package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

public class FZSlider extends AbstractSliderButton implements FZComponent, FZContextMenu.Source {
    private static final int DEFAULT_WIDTH = 150;
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private final boolean bound;
    private ScreenRectangle bounds;
    private Consumer<ChangeEvent> changeHandler = FunctionUtils.nopConsumer();
    private Consumer<FormatEvent> formatHandler = FunctionUtils.nopConsumer();
    private Consumer<ClickEvent> clickHandler = FunctionUtils.nopConsumer();
    private Consumer<DragEvent> dragHandler = FunctionUtils.nopConsumer();
    private Consumer<ReleaseEvent> releaseHandler = FunctionUtils.nopConsumer();
    private @Nullable Component label = null;
    private double min = 0;
    private double max = 1;
    private double step = 0;
    private double mappedValue = 0;
    private boolean dragging;

    private boolean valueBound;
    private double previousRawValue;
    private double pendingRawValue = -1;
    private double previousMappedValue;

    private FZSlider(boolean bound) {
        super(0, 0, DEFAULT_WIDTH, Button.DEFAULT_HEIGHT, CommonComponents.EMPTY, 0);
        this.bound = bound;
        bounds = super.getRectangle();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.dragging = isActive();
        super.onClick(mouseX, mouseY);
        clickHandler.accept(new ClickEvent(this));
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dx, double dy) {
        if (this.dragging) {
            super.onDrag(mouseX, mouseY, dx, dy);
            dragHandler.accept(new DragEvent(this));
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (this.dragging) {
            super.onRelease(mouseX, mouseY);
            releaseHandler.accept(new ReleaseEvent(this));
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

    private void setBoundMappedValue(double newMappedValue) {
        this.valueBound = true;
        this.mappedValue = net.minecraft.util.Mth.clamp(newMappedValue, this.min, this.max);
        this.previousMappedValue = this.mappedValue;

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
                this.value = unmapValue(newMappedValue);
            }
        }

        this.pendingRawValue = -1;
        this.previousRawValue = this.value;
        updateMessage();
    }

    @Override
    protected void applyValue() {
        double rawValue = getRawValue();
        double newMappedValue = mapValue(rawValue);

        if (this.previousMappedValue != newMappedValue) {
            if (!isBound()) {
                this.mappedValue = newMappedValue;
                this.previousMappedValue = newMappedValue;
            } else {
                this.value = previousRawValue;
                this.pendingRawValue = rawValue;
            }
            this.changeHandler.accept(new ChangeEvent(this, newMappedValue));
        }

        this.previousRawValue = getRawValue();
    }

    @Override
    protected void updateMessage() {
        FormatEvent formatEvent = new FormatEvent();
        formatHandler.accept(formatEvent);
        Component formattedValue = formatEvent.getFormattedValue();
        setMessage(label == null || label.getString().isEmpty()
                ? formattedValue
                : CommonComponents.optionNameValue(label, formattedValue));
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(bounds, this)) {
            this.bounds = super.getRectangle();
        }
        return this.bounds;
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
        props.changeHandler().ifPresent(changeHandler -> this.changeHandler = changeHandler.value());
        props.formatHandler().ifPresent(formatHandler -> this.formatHandler = formatHandler.value());
        props.clickHandler().ifPresent(clickHandler -> this.clickHandler = clickHandler.value());
        props.dragHandler().ifPresent(dragHandler -> this.dragHandler = dragHandler.value());
        props.releaseHandler().ifPresent(releaseHandler -> this.releaseHandler = releaseHandler.value());
        props.label().ifPresent(label -> this.label = label);
        props.min().ifPresent(min -> this.min = min);
        props.max().ifPresent(max -> this.max = max);
        props.step().ifPresent(step -> this.step = step);
        props.value().ifPresentOrElse(this::setBoundMappedValue, () -> this.valueBound = false);
    }

    @SuppressWarnings({"MalformedFormatString", "StringConcatenationInFormatCall"})
    public static Component defaultValueFormat(double value, int fractionDigits) {
        return Component.literal(String.format("%." + Math.max(0, fractionDigits) + "f", value));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FZSlider bind(String key, FZRef<Props> props) {
        FZSlider slider = new FZSlider(true);
        slider.applyProps(props.value());
        props.subscribe(key, slider::applyProps);
        return slider;
    }

    public final class FormatEvent {
        private @Nullable Component formattedValue;

        private FormatEvent() {
        }

        public FZSlider target() {
            return FZSlider.this;
        }

        public void format(Component formattedValue) {
            this.formattedValue = formattedValue;
        }

        private Component getFormattedValue() {
            return this.formattedValue == null
                    ? defaultValueFormat(getValue(), step > 0 && step % 1 == 0 ? 0 : 2)
                    : this.formattedValue;
        }
    }

    public record ChangeEvent(FZSlider target, double value) {
    }

    public record ClickEvent(FZSlider target) {
    }

    public record DragEvent(FZSlider target) {
    }

    public record ReleaseEvent(FZSlider target) {
    }

    public interface Props extends GuiComponentProps {
        default Optional<Component> label() {
            return Optional.empty();
        }

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

        default Optional<FZKeyed<Consumer<FormatEvent>>> formatHandler() {
            return Optional.empty();
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
        private final @Nullable FZKeyed<Consumer<FormatEvent>> formatHandler;
        private final @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler;
        private final @Nullable FZKeyed<Consumer<DragEvent>> dragHandler;
        private final @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler;
        private final @Nullable Component label;
        private final @Nullable Double min;
        private final @Nullable Double max;
        private final @Nullable Double step;
        private final @Nullable Double value;

        private PropsImpl(
                GuiComponentProps props,
                @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler,
                @Nullable FZKeyed<Consumer<FormatEvent>> formatHandler,
                @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler,
                @Nullable FZKeyed<Consumer<DragEvent>> dragHandler,
                @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler,
                @Nullable Component label,
                @Nullable Double min,
                @Nullable Double max,
                @Nullable Double step,
                @Nullable Double value
        ) {
            super(props);
            this.changeHandler = changeHandler;
            this.formatHandler = formatHandler;
            this.clickHandler = clickHandler;
            this.dragHandler = dragHandler;
            this.releaseHandler = releaseHandler;
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = value;
        }

        @Override
        public Optional<Component> label() {
            return Optional.ofNullable(label);
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
        public OptionalDouble value() {
            return wrapBoxedDouble(value);
        }

        @Override
        public Optional<FZKeyed<Consumer<ChangeEvent>>> changeHandler() {
            return Optional.ofNullable(changeHandler);
        }

        @Override
        public Optional<FZKeyed<Consumer<FormatEvent>>> formatHandler() {
            return Optional.ofNullable(formatHandler);
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
                   Objects.equals(formatHandler(), other.formatHandler()) &&
                   Objects.equals(clickHandler(), other.clickHandler()) &&
                   Objects.equals(dragHandler(), other.dragHandler()) &&
                   Objects.equals(releaseHandler(), other.releaseHandler()) &&
                   Objects.equals(label(), other.label()) &&
                   Objects.equals(min(), other.min()) &&
                   Objects.equals(max(), other.max()) &&
                   Objects.equals(step(), other.step()) &&
                   Objects.equals(value(), other.value());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(),
                    changeHandler,
                    formatHandler,
                    clickHandler,
                    dragHandler,
                    releaseHandler,
                    label,
                    min,
                    max,
                    step,
                    value
            );
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;
        private @Nullable FZKeyed<Consumer<FormatEvent>> formatHandler;
        private @Nullable FZKeyed<Consumer<ClickEvent>> clickHandler;
        private @Nullable FZKeyed<Consumer<DragEvent>> dragHandler;
        private @Nullable FZKeyed<Consumer<ReleaseEvent>> releaseHandler;
        private @Nullable Component label = null;
        private @Nullable Double min = null;
        private @Nullable Double max = null;
        private @Nullable Double step = null;
        private @Nullable Double value = null;

        private Builder() {
        }

        public Builder label(Component label) {
            this.label = label;
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

        public Builder value(double value) {
            this.value = value;
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

        public Builder onFormat(Consumer<FormatEvent> formatHandler) {
            this.formatHandler = FZKeyed.selfKey(Objects.requireNonNull(formatHandler, "formatHandler cannot be null"));
            return this;
        }

        public Builder onFormat(Object key, Consumer<FormatEvent> formatHandler) {
            this.formatHandler = new FZKeyed<>(key, Objects.requireNonNull(formatHandler, "formatHandler cannot be null"));
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
                    formatHandler,
                    clickHandler,
                    dragHandler,
                    releaseHandler,
                    label,
                    min,
                    max,
                    step,
                    value
            );
        }

        public FZSlider build() {
            FZSlider slider = new FZSlider(false);
            slider.applyProps(toProps());
            slider.updateMessage();
            return slider;
        }
    }
}
