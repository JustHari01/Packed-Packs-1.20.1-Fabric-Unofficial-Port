package io.github.fishstiz.fidgetz.v0.gui.components.color;

import io.github.fishstiz.fidgetz.v0.gui.color.FZColor;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZFlexLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.FZAutoGridLayout;
import io.github.fishstiz.fidgetz.v0.gui.layouts.Justification;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZMutableRef;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import io.github.fishstiz.fidgetz.v0.utils.TriState;
import io.github.fishstiz.fidgetz.v0.utils.Undefinable;
import it.unimi.dsi.fastutil.ints.IntObjectPair;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public final class FZColorPicker extends WrappedLayout<FZFlexLayout> {
    private static final List<List<Widget>> DEFAULT_WIDGETS = List.of(
            List.of(Widget.SV, Widget.HSV, Widget.RGB),
            List.of(Widget.PREVIEW, Widget.ALPHA, Widget.HEX)
    );
    private static final int PREFERRED_SIZE = 64;
    private static final int DEFAULT_INITIAL_COLOR = 0xFFFF5555;
    private static final int DEFAULT_PADDING = 1;
    private static final int DEFAULT_SPACING = 8;
    private static final int CHANNEL_SPACING = 8;
    private static final int LABEL_WIDTH = 8;
    private static final int SLIDER_HEIGHT = 16;
    private static final int SWATCH_SIZE = 16;
    private final boolean bound;
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private final FZMutableRef<FZColor> colorRef;
    private final FZFlexLayout mainLayout;
    private RenderableRectangle background = Renderables.empty();
    private List<List<Widget>> widgets = DEFAULT_WIDGETS;
    private List<IntObjectPair<Component>> swatches = Collections.emptyList();
    private Consumer<ChangeEvent> changeHandler = FunctionUtils.nopConsumer();
    private @Nullable FZAutoGridLayout swatchLayout;
    private int spacing;
    private boolean alphaEnabled = true;
    private boolean colorBound;

    private FZColorPicker(boolean bound, int spacing, FZColor initialColor) {
        super(FZFlexLayout.vertical().spacing(spacing));
        this.mainLayout = FZFlexLayout.vertical().spacing(spacing);
        this.colorRef = new FZMutableRef<>(initialColor);
        this.bound = bound;
        this.spacing = spacing;
    }

    private static FZFlexLayout channelRow(
            String label,
            String sliderId,
            FZRef<FZCustomSlider.Props> sliderProps,
            FZTextField textField
    ) {
        FZFlexLayout row = FZFlexLayout.horizontal().spacing(CHANNEL_SPACING);
        row.defaultChildSettings().alignVerticallyMiddle();
        row.child(FZText.builder(Component.literal(label)).width(LABEL_WIDTH).build());
        FZCustomSlider slider = FZCustomSlider.bind(sliderId, sliderProps);
        row.child(slider, row.flexChildHorizontalSettings().preferredFlexWidth(PREFERRED_SIZE));
        row.child(textField);
        return row;
    }

    private boolean addWidget(FZFlexLayout layout, Widget widget, int index) {
        switch (widget) {
            case SV -> layout.child(
                    FZCustom2DSlider.bind(
                            "SaturationValueArea@" + index,
                            colorRef.map(color -> ColorWidgets.sv2DSlider(color.hsva())
                                    .onChange(e -> handleSVChange((float) e.x(), (float) e.y()))
                                    .toProps()
                            )
                    ),
                    FZFlexLayout.Settings.defaults()
            );
            case HUE -> layout.child(
                    FZCustomSlider.bind(
                            "HueSlider@" + index,
                            colorRef.map(FZColor::hue).map(hue -> ColorWidgets.hueSlider(hue)
                                    .onChange(e -> handleHueChange((float) e.value()))
                                    .toProps()
                            )
                    ),
                    layout.flexChildSettings()
                            .maxFlexHeight(SLIDER_HEIGHT)
                            .preferredFlexWidth(PREFERRED_SIZE)
            );
            case HSV -> {
                FZFlexLayout hsv = layout.child(FZFlexLayout.vertical(), layout.flexChildHorizontalSettings());
                hsv.spacing(this.spacing);

                hsv.child(
                        channelRow(
                                "H:",
                                "HueSlider@" + index,
                                colorRef.map(FZColor::hue).map(hue -> ColorWidgets.hueSlider(hue)
                                        .height(SLIDER_HEIGHT)
                                        .onChange(e -> handleHueChange((float) e.value()))
                                        .toProps()
                                ),
                                ColorWidgets.hueField(
                                        "HueField@" + index,
                                        colorRef.map(FZColor::hue),
                                        this::handleHueChange
                                )
                        ),
                        hsv.flexChildHorizontalSettings()
                );

                hsv.child(
                        channelRow(
                                "S:",
                                "SaturationSlider@" + index,
                                colorRef.map(color -> ColorWidgets.saturationSlider(color.hsva())
                                        .height(SLIDER_HEIGHT)
                                        .onChange(e -> handleSaturationChange((float) e.value()))
                                        .toProps()
                                ),
                                ColorWidgets.saturationField(
                                        "SaturationField@" + index,
                                        colorRef.map(FZColor::saturation),
                                        this::handleSaturationChange
                                )
                        ),
                        hsv.flexChildHorizontalSettings()
                );

                hsv.child(
                        channelRow(
                                "V:",
                                "ValueSlider@" + index,
                                colorRef.map(color -> ColorWidgets.valueSlider(color.hsva())
                                        .height(SLIDER_HEIGHT)
                                        .onChange(e -> handleValueChange((float) e.value()))
                                        .toProps()
                                ),
                                ColorWidgets.valueField(
                                        "ValueField@" + index,
                                        colorRef.map(FZColor::value),
                                        this::handleValueChange
                                )
                        ),
                        hsv.flexChildHorizontalSettings()
                );
            }
            case RGB -> {
                FZFlexLayout rgb = layout.child(FZFlexLayout.vertical(), layout.flexChildHorizontalSettings());
                rgb.spacing(this.spacing);

                rgb.child(
                        channelRow(
                                "R:",
                                "RedSlider@" + index,
                                colorRef.map(color -> ColorWidgets.redSlider(color.rgba())
                                        .height(SLIDER_HEIGHT)
                                        .onChange(e -> handleRedChange((int) e.value()))
                                        .toProps()
                                ),
                                ColorWidgets.redField(
                                        "RedField@" + index,
                                        colorRef.map(FZColor::red),
                                        this::handleRedChange
                                )
                        ),
                        rgb.flexChildHorizontalSettings()
                );

                rgb.child(
                        channelRow(
                                "G:",
                                "GreenSlider@" + index,
                                colorRef.map(color -> ColorWidgets.greenSlider(color.rgba())
                                        .height(SLIDER_HEIGHT)
                                        .onChange(e -> handleGreenChange((int) e.value()))
                                        .toProps()
                                ),
                                ColorWidgets.greenField(
                                        "GreenField@" + index,
                                        colorRef.map(FZColor::green),
                                        this::handleGreenChange
                                )
                        ),
                        rgb.flexChildHorizontalSettings()
                );

                rgb.child(
                        channelRow(
                                "B:",
                                "BlueSlider@" + index,
                                colorRef.map(color -> ColorWidgets.blueSlider(color.rgba())
                                        .height(SLIDER_HEIGHT)
                                        .onChange(e -> handleBlueChange((int) e.value()))
                                        .toProps()
                                ),
                                ColorWidgets.blueField(
                                        "BlueField@" + index,
                                        colorRef.map(FZColor::blue),
                                        this::handleBlueChange
                                )
                        ),
                        rgb.flexChildHorizontalSettings()
                );
            }
            case ALPHA_SLIDER -> {
                if (!this.alphaEnabled) return false;

                layout.child(
                        FZCustomSlider.bind(
                                "AlphaSlider@" + index,
                                colorRef.map(color -> ColorWidgets.alphaSlider(color.rgba())
                                        .onChange(e -> handleAlphaChange((float) e.value()))
                                        .toProps()
                                )
                        ),
                        layout.flexChildSettings()
                                .maxFlexHeight(SLIDER_HEIGHT)
                                .preferredFlexWidth(PREFERRED_SIZE)
                );
            }
            case ALPHA -> {
                if (!this.alphaEnabled) return false;

                FZFlexLayout alpha = layout.child(FZFlexLayout.horizontal(), layout.flexChildHorizontalSettings());
                alpha.spacing(CHANNEL_SPACING).defaultChildSettings().alignVerticallyMiddle();

                alpha.child(FZText.builder(Component.literal("A:")).width(LABEL_WIDTH).build());

                alpha.child(
                        FZCustomSlider.bind(
                                "AlphaSlider@" + index,
                                colorRef.map(color -> ColorWidgets.alphaSlider(color.rgba())
                                        .height(SLIDER_HEIGHT)
                                        .onChange(e -> handleAlphaChange((float) e.value()))
                                        .toProps()
                                )
                        ),
                        alpha.flexChildHorizontalSettings()
                );

                alpha.child(ColorWidgets.alphaField(
                        "AlphaField@" + index,
                        colorRef.map(FZColor::alpha),
                        this::handleAlphaChange
                ));
            }
            case PREVIEW -> layout.child(
                    FZIcon.bind(
                            "ColorPreview@" + index,
                            colorRef.map(color -> ColorWidgets.colorPreview(color.toARGB())
                                    .height(SLIDER_HEIGHT)
                                    .toProps()
                            )
                    ),
                    layout.flexChildSettings()
                            .preferredFlexWidth(PREFERRED_SIZE)
                            .maxFlexWidth(PREFERRED_SIZE)
                            .maxFlexHeight(PREFERRED_SIZE)
            );
            case HEX_FIELD -> layout.child(
                    ColorWidgets.hexField(
                            "HexField@" + index,
                            this.alphaEnabled,
                            colorRef.map(FZColor::toARGB),
                            this::handleHexChange
                    ),
                    layout.flexChildHorizontalSettings()
            );
            case HEX -> {
                FZFlexLayout hex = layout.child(FZFlexLayout.horizontal(), layout.flexChildHorizontalSettings());
                hex.spacing(this.spacing).defaultChildSettings().alignVerticallyMiddle();

                hex.child(FZText.builder(Component.literal("Hex:")).width(20).build());

                hex.child(
                        ColorWidgets.hexField(
                                "HexField@" + index,
                                this.alphaEnabled,
                                colorRef.map(FZColor::toARGB),
                                this::handleHexChange
                        ),
                        hex.flexChildHorizontalSettings()
                );
            }
        }

        return true;
    }

    @Override
    protected void buildWidgets(GuiComponentCollector collector) {
        this.layout.removeChildren();
        this.mainLayout.removeChildren();

        colorRef.clearSubscribers();

        int index = 0;
        for (List<Widget> row : this.widgets) {
            FZFlexLayout rowLayout = FZFlexLayout.horizontal();
            rowLayout.spacing(this.spacing);
            rowLayout.justifyContents(Justification.CENTER).alignContents(Justification.CENTER);

            boolean added = false;
            for (Widget widget : row) {
                added |= addWidget(rowLayout, widget, index++);
            }

            if (added) {
                mainLayout.child(rowLayout, mainLayout.flexChildHorizontalSettings());
            }
        }

        this.layout.child(this.mainLayout, this.layout.flexChildHorizontalSettings());

        buildSwatches();
    }

    private void buildSwatches() {
        if (this.swatchLayout != null) {
            this.swatchLayout.visitWidgets(this::removeWidget);
            this.swatchLayout.removeChildren();
            this.layout.removeChildren();
            this.layout.child(this.mainLayout, this.layout.flexChildHorizontalSettings());
        }

        if (this.swatches.isEmpty()) {
            this.swatchLayout = null;
            return;
        }

        mainLayout.arrangeElements();

        int containerWidth = mainLayout.getWidth();

        FZAutoGridLayout newSwatchLayout = FZAutoGridLayout.horizontal()
                .spacing(this.spacing)
                .maxWidth(containerWidth)
                .justifyContents(Justification.SPACE_BETWEEN);

        for (IntObjectPair<Component> swatch : this.swatches) {
            int argb = swatch.leftInt();
            Component name = swatch.right();

            newSwatchLayout.child(ColorWidgets.colorSwatch(argb, name)
                    .size(SWATCH_SIZE, SWATCH_SIZE)
                    .onPress(() -> handleChange(FZColor.fromARGB(argb)))
                    .build());
        }

        this.swatchLayout = this.layout.child(newSwatchLayout, this.layout.flexChildSettings());
    }

    private void refreshWidgets() {
        clearWidgets();
        GuiComponentCollector collector = new GuiComponentCollector();
        this.layout.visitWidgets(collector::renderableWidget);
        collector.flushTo(this::addWidget, this::addRenderableOnly);
        arrangeElements();
    }

    private boolean isBound() {
        return bound && colorBound;
    }

    private void handleChange(FZColor color) {
        if (!isBound()) {
            colorRef.set(color);
        }

        this.changeHandler.accept(new ChangeEvent(this, color));
    }

    private void handleHexChange(int argb) {
        handleChange(FZColor.fromARGB(argb));
    }

    private void handleSVChange(float saturation, float value) {
        handleChange(colorRef.value().withSV(saturation, value));
    }

    private void handleHueChange(float hue) {
        handleChange(colorRef.value().withHue(hue));
    }

    private void handleSaturationChange(float saturation) {
        handleChange(colorRef.value().withSaturation(saturation));
    }

    private void handleValueChange(float value) {
        handleChange(colorRef.value().withValue(value));
    }

    private void handleRedChange(int red) {
        handleChange(colorRef.value().withRed(red));
    }

    private void handleGreenChange(int green) {
        handleChange(colorRef.value().withGreen(green));
    }

    private void handleBlueChange(int blue) {
        handleChange(colorRef.value().withBlue(blue));
    }

    private void handleAlphaChange(float alpha) {
        handleChange(colorRef.value().withAlpha(alpha));
    }

    public FZColor getColor() {
        return colorRef.value();
    }

    public void setAlphaEnabled(boolean alphaEnabled) {
        if (this.alphaEnabled != alphaEnabled) {
            this.alphaEnabled = alphaEnabled;
            clearWidgets();
            buildWidgets();
        }
    }

    public void setColor(int color) {
        colorRef.set(FZColor.fromARGB(color));
    }

    public void setColor(FZColor color) {
        colorRef.set(color);
    }

    private void updateSwatchLayoutWidth(int width) {
        if (this.swatchLayout != null) {
            this.swatchLayout.maxWidth(width - this.padding.left() - this.padding.right());
            if (width != getWidth()) {
                arrangeElements();
            }
        }
    }

    @Override
    public void setWidth(int width) {
        updateSwatchLayoutWidth(width);
        super.setWidth(width);
    }

    @Override
    public void setSize(int width, int height) {
        updateSwatchLayoutWidth(width);
        super.setSize(width, height);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        background.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);

        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return propsState.id;
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        propsState.contextEntries.accept(collector);
    }

    @Override
    protected void addWidget(GuiEventListener widget) {
        if (widget instanceof FZHoverableElement hoverableElement) {
            hoverableElement.fidgetz$setHovered(false);
        }
        super.addWidget(widget);
    }

    private void applyProps(Props props) {
        propsState.apply(this, props);
        props.background().ifDefined(background -> this.background = background == null ? Renderables.empty() : background);
        props.padding().ifPresent(padding -> setPadding(padding.left(), padding.top(), padding.right(), padding.bottom()));

        int newSpacing = props.spacing().orElse(this.spacing);
        boolean spacingUpdated = newSpacing != this.spacing;
        this.spacing = newSpacing;

        TriState newAlphaEnabled = props.alphaEnabled();
        boolean alphaUpdated = newAlphaEnabled != TriState.DEFAULT && newAlphaEnabled.toBoolean(true) != this.alphaEnabled;
        if (alphaUpdated) {
            this.alphaEnabled = newAlphaEnabled.toBoolean(true);
        }

        List<IntObjectPair<Component>> newSwatches = props.swatches();
        boolean swatchesUpdated = !Objects.equals(newSwatches, this.swatches);
        if (swatchesUpdated) {
            this.swatches = List.copyOf(newSwatches);
        }

        List<List<Widget>> newWidgets = props.widgets();
        boolean widgetsUpdated = !Objects.equals(newWidgets, this.widgets);
        if (widgetsUpdated) {
            this.widgets = copyWidgets(newWidgets);
        }

        if (alphaUpdated || widgetsUpdated || spacingUpdated) {
            buildWidgets();
        } else if (swatchesUpdated) {
            buildSwatches();
            refreshWidgets();
        }

        props.changeHandler().ifPresent(changeHandler -> this.changeHandler = changeHandler.value());
        props.color().ifPresentOrElse(
                color -> {
                    this.colorBound = true;
                    colorRef.set(color);
                },
                () -> this.colorBound = false
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static FZColorPicker bind(String key, FZRef<Props> props) {
        Props propValues = props.value();

        FZColorPicker colorPicker = new FZColorPicker(
                true,
                propValues.spacing().orElse(DEFAULT_SPACING),
                propValues.color().orElseGet(() -> FZColor.fromARGB(DEFAULT_INITIAL_COLOR))
        );
        colorPicker.padding = propValues.padding().orElseGet(() -> ScreenRectangleUtils.insets(DEFAULT_PADDING));
        colorPicker.widgets = propValues.widgets() == DEFAULT_WIDGETS
                ? DEFAULT_WIDGETS
                : copyWidgets(propValues.widgets());
        colorPicker.swatches = List.copyOf(propValues.swatches());
        colorPicker.alphaEnabled = propValues.alphaEnabled().toBoolean(true);
        colorPicker.buildWidgets();
        colorPicker.applyProps(propValues);
        props.subscribe(key, colorPicker::applyProps);
        return colorPicker;
    }

    private static List<List<Widget>> copyWidgets(List<List<Widget>> widgets) {
        return widgets.stream().map(List::copyOf).toList();
    }

    public record ChangeEvent(FZColorPicker target, FZColor color) {
    }

    public enum Widget {
        SV,
        HUE,
        HSV,
        RGB,
        ALPHA,
        ALPHA_SLIDER,
        HEX,
        HEX_FIELD,
        PREVIEW
    }

    public interface Props extends GuiComponentProps {
        List<List<Widget>> widgets();

        default Optional<FZColor> color() {
            return Optional.empty();
        }

        default Optional<ScreenRectangle> padding() {
            return Optional.empty();
        }

        default OptionalInt spacing() {
            return OptionalInt.empty();
        }

        default Undefinable<@Nullable RenderableRectangle> background() {
            return Undefinable.undefined();
        }

        default List<IntObjectPair<Component>> swatches() {
            return Collections.emptyList();
        }

        default TriState alphaEnabled() {
            return TriState.DEFAULT;
        }

        default Optional<FZKeyed<Consumer<ChangeEvent>>> changeHandler() {
            return Optional.empty();
        }
    }

    private static final class PropsImpl extends GuiComponentPropsBase implements Props {
        private final List<List<Widget>> widgets;
        private final @Nullable FZColor color;
        private final @Nullable ScreenRectangle padding;
        private final @Nullable Integer spacing;
        private final Undefinable<@Nullable RenderableRectangle> background;
        private final List<IntObjectPair<Component>> swatches;
        private final TriState alphaEnabled;
        private final @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;

        public PropsImpl(
                GuiComponentProps props,
                @Nullable FZColor color,
                @Nullable ScreenRectangle padding,
                @Nullable Integer spacing,
                Undefinable<@Nullable RenderableRectangle> background,
                List<IntObjectPair<Component>> swatches,
                List<List<Widget>> widgets,
                TriState alphaEnabled,
                @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler
        ) {
            super(props);
            this.color = color;
            this.padding = padding;
            this.spacing = spacing;
            this.background = background;
            this.changeHandler = changeHandler;
            this.widgets = List.copyOf(widgets);
            this.alphaEnabled = alphaEnabled;
            this.swatches = List.copyOf(swatches);
        }

        @Override
        public Optional<FZColor> color() {
            return Optional.ofNullable(color);
        }

        @Override
        public Optional<ScreenRectangle> padding() {
            return Optional.ofNullable(padding);
        }

        @Override
        public OptionalInt spacing() {
            return wrapBoxedInt(spacing);
        }

        @Override
        public Undefinable<@Nullable RenderableRectangle> background() {
            return background;
        }

        @Override
        public List<IntObjectPair<Component>> swatches() {
            return swatches;
        }

        @Override
        public List<List<Widget>> widgets() {
            return widgets;
        }

        @Override
        public TriState alphaEnabled() {
            return alphaEnabled;
        }

        @Override
        public Optional<FZKeyed<Consumer<ChangeEvent>>> changeHandler() {
            return Optional.ofNullable(changeHandler);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(other) &&
                   Objects.equals(color(), other.color()) &&
                   Objects.equals(padding(), other.padding()) &&
                   Objects.equals(spacing(), other.spacing()) &&
                   Objects.equals(background(), other.background()) &&
                   Objects.equals(widgets(), other.widgets()) &&
                   alphaEnabled == other.alphaEnabled() &&
                   Objects.equals(changeHandler(), other.changeHandler());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(),
                    color,
                    padding,
                    spacing,
                    background,
                    widgets,
                    alphaEnabled,
                    changeHandler
            );
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private final List<IntObjectPair<Component>> swatches = new ArrayList<>();
        private List<List<Widget>> widgets = DEFAULT_WIDGETS;
        private @Nullable FZColor color;
        private @Nullable ScreenRectangle padding;
        private @Nullable Integer spacing;
        private Undefinable<@Nullable RenderableRectangle> background = Undefinable.undefined();
        private @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;
        private TriState alphaEnabled = TriState.DEFAULT;

        private Builder() {
        }

        public Builder color(FZColor color) {
            this.color = color;
            return this;
        }

        public Builder color(int color) {
            return color(FZColor.fromARGB(color));
        }

        public Builder padding(int left, int top, int right, int bottom) {
            this.padding = ScreenRectangleUtils.insets(left, top, right, bottom);
            return this;
        }

        public Builder padding(int padding) {
            return padding(padding, padding, padding, padding);
        }

        public Builder spacing(int spacing) {
            this.spacing = spacing;
            return this;
        }

        public Builder background(@Nullable RenderableRectangle background) {
            this.background = Undefinable.of(background);
            return this;
        }

        public Builder background(@Nullable ResourceLocation background) {
            this.background = Undefinable.of(background == null ? null : Renderables.sprite(background));
            return this;
        }

        public Builder swatch(int argb, Component name) {
            swatches.add(IntObjectPair.of(argb, name.copy()));
            return this;
        }

        public Builder swatch(TextColor textColor) {
            return swatch(
                    textColor.getValue() | 0xFF000000,
                    Component.literal(textColor.toString())
            );
        }

        public Builder row(Widget... widgets) {
            if (this.widgets == DEFAULT_WIDGETS) {
                this.widgets = new ArrayList<>();
            }
            if (Objects.requireNonNull(widgets, "row widgets cannot be null").length > 0) {
                this.widgets.add(List.of(widgets));
            }
            return this;
        }

        public Builder alphaEnabled(boolean enabled) {
            this.alphaEnabled = TriState.from(enabled);
            return this;
        }

        public Builder alphaEnabled() {
            return alphaEnabled(true);
        }

        public Builder alphaDisabled(boolean disabled) {
            return alphaEnabled(!disabled);
        }

        public Builder alphaDisabled() {
            return alphaDisabled(true);
        }

        public Builder onChange(Object key, Consumer<ChangeEvent> changeHandler) {
            this.changeHandler = new FZKeyed<>(key, Objects.requireNonNull(changeHandler, "changeHandler cannot be null"));
            return this;
        }

        public Builder onChange(Consumer<ChangeEvent> changeHandler) {
            return onChange(changeHandler, changeHandler);
        }

        public Props toProps() {
            return new PropsImpl(
                    props,
                    color,
                    padding,
                    spacing,
                    background,
                    swatches,
                    widgets,
                    alphaEnabled,
                    changeHandler
            );
        }

        public FZColorPicker build() {
            Props props = toProps();
            FZColorPicker colorPicker = new FZColorPicker(
                    false,
                    props.spacing().orElse(DEFAULT_SPACING),
                    props.color().orElseGet(() -> FZColor.fromARGB(DEFAULT_INITIAL_COLOR))
            );
            colorPicker.padding = props.padding().orElseGet(() -> ScreenRectangleUtils.insets(DEFAULT_PADDING));
            colorPicker.swatches = props.swatches();
            colorPicker.widgets = props.widgets() == DEFAULT_WIDGETS
                    ? DEFAULT_WIDGETS
                    : props.widgets();
            colorPicker.alphaEnabled = props.alphaEnabled().toBoolean(true);
            colorPicker.buildWidgets();
            colorPicker.applyProps(props);
            return colorPicker;
        }
    }
}
