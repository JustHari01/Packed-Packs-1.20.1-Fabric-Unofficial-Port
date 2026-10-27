package io.github.fishstiz.fidgetz.v0.gui.components.color;

import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.gui.text.SimpleStringStyler;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import io.github.fishstiz.fidgetz.v0.gui.color.ColorModel;
import io.github.fishstiz.fidgetz.v0.gui.color.HSVA;
import io.github.fishstiz.fidgetz.v0.gui.color.RGBA;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import it.unimi.dsi.fastutil.objects.ObjectIntMutablePair;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.CommonColors;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.Locale;
import java.util.function.*;

public final class ColorWidgets {
    private static final Style ERROR_STYLE = Style.EMPTY.withColor(0xFFFF5555);
    private static final ResourceLocation SLIDER_SPRITE = new ResourceLocation("widget/slider");
    private static final ResourceLocation SLIDER_HIGHLIGHTED_SPRITE = new ResourceLocation("widget/slider_highlighted");
    private static final ResourceLocation BUTTON_SPRITE = new ResourceLocation("widget/button");
    private static final ResourceLocation BUTTON_HIGHLIGHTED_SPRITE = new ResourceLocation("widget/button_highlighted");
    private static final int CHECKERBOARD_CELL_SIZE = 4;

    static RenderableRectangle dynamicFill(IntSupplier argb) {
        return (graphics, left, top, width, height, ignoredMX, ignoredMY, ignoredD) ->
                graphics.fill(left, top, left + width, top + height, argb.getAsInt());
    }

    static RenderableRectangle checkerboard(IntIntMutablePair position) {
        return (graphics, left, top, width, height, ignoredMX, ignoredMY, ignoredD) -> {
            position.left(left).right(top);
            GuiGraphicsUtils.checkerboard(graphics, left, top, width, height, CHECKERBOARD_CELL_SIZE);
        };
    }

    static RenderableRectangle movingCheckerboard(IntIntMutablePair position) {
        return (graphics, left, top, width, height, ignoredMX, ignoredMY, ignoredD) ->
                GuiGraphicsUtils.checkerboard(
                        graphics,
                        left,
                        top,
                        width,
                        height,
                        CHECKERBOARD_CELL_SIZE,
                        left - position.leftInt(),
                        top - position.rightInt()
                );
    }

    static RenderableRectangle horizontalGradient(int colorFrom, int colorTo) {
        return (graphics, left, top, width, height, ignoredMX, ignoredMY, ignoredD) ->
                GuiGraphicsUtils.fillHorizontal(graphics, left, top, left + width, top + height, colorFrom, colorTo);
    }

    static WidgetRenderables thumbWidget(IntIntMutablePair position, IntSupplier argb) {
        RenderableRectangle boxShadow = Renderables.boxShadow(8);
        RenderableRectangle sprite = Renderables.sprite(BUTTON_SPRITE);
        RenderableRectangle spriteHighlighted = Renderables.sprite(BUTTON_HIGHLIGHTED_SPRITE);
        RenderableRectangle checkeredFill = movingCheckerboard(position).then(dynamicFill(argb)).shrink(2);

        RenderableRectangle base = boxShadow.then(sprite).then(checkeredFill);
        RenderableRectangle highlighted = boxShadow.then(spriteHighlighted).then(checkeredFill);

        return new WidgetRenderables(
                base,
                base,
                highlighted,
                base
        );
    }

    static WidgetRenderables sliderBackground(IntIntMutablePair position, RenderableRectangle background) {
        RenderableRectangle sprite = Renderables.sprite(SLIDER_SPRITE);
        RenderableRectangle spriteHighlighted = Renderables.sprite(SLIDER_HIGHLIGHTED_SPRITE);
        RenderableRectangle checkeredBackground = checkerboard(position).then(background.flush()).crop(1);

        return new WidgetRenderables(sprite.then(checkeredBackground), spriteHighlighted.then(checkeredBackground));
    }

    public static FZCustom2DSlider.Builder sv2DSlider(HSVA hsva) {
        HSVA opaque = hsva.withAlpha(1.0f);
        ObjectIntMutablePair<HSVA> color = ObjectIntMutablePair.of(opaque, opaque.toARGB());
        IntIntMutablePair pos = new IntIntMutablePair(0, 0);

        return FZCustom2DSlider.builder()
                .values(hsva.saturation(), hsva.value())
                .background(sliderBackground(pos, new SaturationValueGradient(HSVA.toARGB(hsva.hue(), 1.0f, 1.0f, 1.0f))))
                .cursor(thumbWidget(pos, color::valueInt))
                .onChange(e -> {
                    HSVA newHsva = color.key().withSV((float) e.x(), (float) e.y());
                    color.key(newHsva);
                    color.value(newHsva.toARGB());
                });
    }

    public static FZCustomSlider.Builder hueSlider(float hue) {
        MutableInt color = new MutableInt(HSVA.toARGB(hue, 1.0f, 1.0f, 1.0f));
        IntIntMutablePair pos = new IntIntMutablePair(0, 0);

        return FZCustomSlider.builder()
                .value(hue)
                .min(0)
                .max(360)
                .step(0.01f)
                .background(sliderBackground(pos, new HueGradient()))
                .onChange(e -> color.setValue(HSVA.toARGB((float) e.value(), 1.0f, 1.0f, 1.0f)))
                .thumb(thumbWidget(pos, color::intValue));
    }

    public static FZCustomSlider.Builder saturationSlider(HSVA hsva) {
        HSVA opaque = hsva.withAlpha(1.0f);
        HSVA saturated = opaque.withSaturation(1.0f);
        HSVA desaturated = opaque.withSaturation(0.0f);

        ObjectIntMutablePair<HSVA> color = ObjectIntMutablePair.of(opaque, opaque.toARGB());

        IntIntMutablePair pos = new IntIntMutablePair(0, 0);

        return FZCustomSlider.builder()
                .value(color.key().saturation())
                .step(0.01f)
                .background(sliderBackground(pos, horizontalGradient(desaturated.toARGB(), saturated.toARGB())))
                .onChange(e -> {
                    color.key(color.key().withSaturation((float) e.value()));
                    color.value(color.key().toARGB());
                })
                .thumb(thumbWidget(pos, color::valueInt));
    }

    public static FZCustomSlider.Builder valueSlider(HSVA hsva) {
        HSVA opaque = hsva.withAlpha(1.0f);
        HSVA dark = opaque.withValue(0.0f);
        HSVA bright = opaque.withValue(1.0f);

        ObjectIntMutablePair<HSVA> color = ObjectIntMutablePair.of(opaque, opaque.toARGB());

        IntIntMutablePair pos = new IntIntMutablePair(0, 0);

        return FZCustomSlider.builder()
                .value(color.key().value())
                .step(0.01f)
                .background(sliderBackground(pos, horizontalGradient(dark.toARGB(), bright.toARGB())))
                .onChange(e -> {
                    color.key(color.key().withValue((float) e.value()));
                    color.value(color.key().toARGB());
                })
                .thumb(thumbWidget(pos, color::valueInt));
    }

    public static FZCustomSlider.Builder alphaSlider(ColorModel color) {
        MutableInt argb = new MutableInt(color.toARGB());
        int transparent = argb.intValue() & 0x00FFFFFF;
        int opaque = argb.intValue() | 0xFF000000;

        IntIntMutablePair pos = new IntIntMutablePair(0, 0);

        return FZCustomSlider.builder()
                .value(color.alpha())
                .step(0.01f)
                .onChange(e -> {
                    int a = net.minecraft.util.Mth.clamp(Math.round((float) e.value() * 255f), 0, 255);
                    argb.setValue((argb.intValue() & 0x00FFFFFF) | (a << 24));
                })
                .background(sliderBackground(pos, horizontalGradient(transparent, opaque)))
                .thumb(thumbWidget(pos, argb::intValue));
    }

    static FZCustomSlider.Builder rgbSlider(
            RGBA rgba,
            ToIntFunction<RGBA> channelFunction,
            BiFunction<RGBA, Integer, RGBA> colorFunction
    ) {
        RGBA opaque = rgba.withAlpha(1.0f);
        RGBA min = colorFunction.apply(opaque, 0);
        RGBA max = colorFunction.apply(opaque, 255);

        ObjectIntMutablePair<RGBA> color = ObjectIntMutablePair.of(opaque, opaque.toARGB());

        IntIntMutablePair pos = new IntIntMutablePair(0, 0);

        return FZCustomSlider.builder()
                .value(channelFunction.applyAsInt(opaque))
                .min(0)
                .max(255)
                .step(1)
                .onChange(e -> {
                    color.key(colorFunction.apply(color.key(), (int) e.value()));
                    color.value(color.key().toARGB());
                })
                .background(sliderBackground(pos, horizontalGradient(min.toARGB(), max.toARGB())))
                .thumb(thumbWidget(pos, color::valueInt));
    }

    public static FZCustomSlider.Builder redSlider(RGBA rgba) {
        return rgbSlider(rgba, RGBA::red, RGBA::withRed);
    }

    public static FZCustomSlider.Builder greenSlider(RGBA rgba) {
        return rgbSlider(rgba, RGBA::green, RGBA::withGreen);
    }

    public static FZCustomSlider.Builder blueSlider(RGBA rgba) {
        return rgbSlider(rgba, RGBA::blue, RGBA::withBlue);
    }

    private static String truncate(Number floatValue) {
        return String.format(Locale.ROOT, "%.0f", floatValue.floatValue());
    }

    private static FZTextField.Builder floatField(
            float floatValue,
            float maxValue,
            FloatConsumer changeHandler
    ) {
        return FZTextField.builder()
                .width(32)
                .height(16)
                .maxLength(3)
                .filter(input -> input.codePoints().allMatch(Character::isDigit))
                .text(truncate(floatValue))
                .onChange(e -> {
                    String text = e.value().trim();
                    if (text.isEmpty()) {
                        changeHandler.accept(0.0f);
                    } else {
                        float parsedValue = Float.parseFloat(text);
                        float newValue = net.minecraft.util.Mth.clamp(parsedValue, 0.0f, maxValue);
                        changeHandler.accept(newValue);
                    }
                })
                .onBlur(e -> {
                    String text = e.target().getValue();
                    float newValue = text.isEmpty() ? 0.0f : net.minecraft.util.Mth.clamp(Float.parseFloat(text), 0.0f, maxValue);
                    e.target().setValue(truncate(floatValue));
                    changeHandler.accept(newValue);
                })
                .styleMatcher(new SimpleStringStyler(
                        ERROR_STYLE,
                        input -> !input.isEmpty() && Float.parseFloat(input) > maxValue
                ));
    }

    private static <T extends Number> FZTextField syncOnBlur(
            FZTextField.Builder builder,
            String key,
            FZRef<T> ref,
            UnaryOperator<Number> formatter
    ) {
        return ref.bind(
                key,
                builder.onBlur(e -> e.target().setValue(truncate(formatter.apply(ref.value())))).build(),
                (value, field) -> {
                    if (!field.isFocused()) {
                        field.setValue(truncate(formatter.apply(value)));
                    }
                }
        );
    }

    private static FZTextField.Builder normalizedFloatField(float floatValue, FloatConsumer changeHandler) {
        return floatField(floatValue * 100.0f, 100.0f, v -> changeHandler.accept(net.minecraft.util.Mth.clamp(v / 100.0f, 0.0f, 1.0f)));
    }

    private static FZTextField boundNormalizedField(
            String key,
            FZRef<Float> ref,
            FloatConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builderBuilder
    ) {
        return syncOnBlur(
                builderBuilder.apply(normalizedFloatField(ref.value(), changeHandler)),
                key,
                ref,
                number -> number.floatValue() * 100.0f
        );
    }

    public static FZTextField.Builder hueField(float hue, FloatConsumer changeHandler) {
        return floatField(hue, 360, changeHandler);
    }

    public static FZTextField hueField(String key, FZRef<Float> ref, FloatConsumer changeHandler) {
        return syncOnBlur(floatField(ref.value(), 360, changeHandler), key, ref, UnaryOperator.identity());
    }

    public static FZTextField.Builder saturationField(float saturation, FloatConsumer changeHandler) {
        return normalizedFloatField(saturation, changeHandler);
    }

    public static FZTextField saturationField(
            String key,
            FZRef<Float> ref,
            FloatConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return boundNormalizedField(key, ref, changeHandler, builder);
    }

    public static FZTextField saturationField(String key, FZRef<Float> ref, FloatConsumer changeHandler) {
        return saturationField(key, ref, changeHandler, UnaryOperator.identity());
    }

    public static FZTextField.Builder valueField(float value, FloatConsumer changeHandler) {
        return normalizedFloatField(value, changeHandler);
    }

    public static FZTextField valueField(
            String key,
            FZRef<Float> ref,
            FloatConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return boundNormalizedField(key, ref, changeHandler, builder);
    }

    public static FZTextField valueField(String key, FZRef<Float> ref, FloatConsumer changeHandler) {
        return valueField(key, ref, changeHandler, UnaryOperator.identity());
    }

    private static FZTextField.Builder unsignedByteField(int byteValue, IntConsumer changeHandler) {
        return floatField(byteValue, 255, v -> changeHandler.accept(net.minecraft.util.Mth.clamp((int) v, 0, 255)));
    }

    public static FZTextField.Builder redField(int red, IntConsumer changeHandler) {
        return unsignedByteField(red, changeHandler);
    }

    public static FZTextField redField(
            String key,
            FZRef<Integer> ref,
            IntConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return syncOnBlur(builder.apply(redField(ref.value(), changeHandler)), key, ref, UnaryOperator.identity());
    }

    public static FZTextField redField(String key, FZRef<Integer> ref, IntConsumer changeHandler) {
        return redField(key, ref, changeHandler, UnaryOperator.identity());
    }

    public static FZTextField.Builder greenField(int green, IntConsumer changeHandler) {
        return unsignedByteField(green, changeHandler);
    }

    public static FZTextField greenField(
            String key,
            FZRef<Integer> ref,
            IntConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return syncOnBlur(builder.apply(greenField(ref.value(), changeHandler)), key, ref, UnaryOperator.identity());
    }

    public static FZTextField greenField(String key, FZRef<Integer> ref, IntConsumer changeHandler) {
        return greenField(key, ref, changeHandler, UnaryOperator.identity());
    }

    public static FZTextField.Builder blueField(int blue, IntConsumer changeHandler) {
        return unsignedByteField(blue, changeHandler);
    }

    public static FZTextField blueField(
            String key,
            FZRef<Integer> ref,
            IntConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return syncOnBlur(builder.apply(blueField(ref.value(), changeHandler)), key, ref, UnaryOperator.identity());
    }

    public static FZTextField blueField(String key, FZRef<Integer> ref, IntConsumer changeHandler) {
        return blueField(key, ref, changeHandler, UnaryOperator.identity());
    }

    public static FZTextField.Builder alphaField(int alpha, FloatConsumer changeHandler) {
        return normalizedFloatField(alpha, changeHandler);
    }

    public static FZTextField alphaField(
            String key,
            FZRef<Float> ref,
            FloatConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return boundNormalizedField(key, ref, changeHandler, builder);
    }

    public static FZTextField alphaField(String key, FZRef<Float> ref, FloatConsumer changeHandler) {
        return alphaField(key, ref, changeHandler, UnaryOperator.identity());
    }

    private static boolean isValidHexString(String hexString) {
        return hexString.length() == 9
               || (hexString.length() == 8 && hexString.charAt(0) != '#')
               || (hexString.length() == 7 && hexString.charAt(0) == '#')
               || (hexString.length() == 6 && hexString.charAt(0) != '#');
    }

    private static String formatHex(int argb, boolean allowAlpha) {
        return String.format(
                Locale.ROOT,
                allowAlpha ? "#%08X" : "#%06X",
                allowAlpha ? argb : argb & 0x00FFFFFF
        );
    }

    private static String formatHex(int argb) {
        return String.format(Locale.ROOT, "#%08X", argb);
    }

    public static FZTextField.Builder hexField(int argb, boolean allowAlpha, IntConsumer changeHandler) {
        MutableInt color = new MutableInt(argb);

        String regex = allowAlpha ? "^#?[0-9a-fA-F]{0,8}$" : "^#?[0-9a-fA-F]{0,6}$";

        return FZTextField.builder()
                .width(64)
                .height(16)
                .maxLength(9)
                .filter(input -> input.matches(regex))
                .text(formatHex(argb, allowAlpha))
                .onChange(e -> {
                    String text = e.value().trim();

                    if (!text.isEmpty()) {
                        e.target().setValue(text.toUpperCase());
                    }

                    if (isValidHexString(text)) {
                        int newColor = RGBA.fromARGBHexString(text).toARGB();
                        if (!allowAlpha) {
                            newColor = (newColor & 0x00FFFFFF) | (argb & 0xFF000000);
                        }
                        color.setValue(newColor);
                        changeHandler.accept(newColor);
                    }
                })
                .onBlur(e -> e.target().setValue(formatHex(color.intValue(), allowAlpha)))
                .styleMatcher(new SimpleStringStyler(
                        ERROR_STYLE,
                        input -> !isValidHexString(input)
                ));
    }

    public static FZTextField.Builder hexField(int argb, IntConsumer changeHandler) {
        return hexField(argb, true, changeHandler);
    }

    public static FZTextField hexField(
            String key,
            boolean allowAlpha,
            FZRef<Integer> ref,
            IntConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return ref.bind(
                key,
                builder.apply(hexField(ref.value(), allowAlpha, changeHandler)
                        .onBlur(e -> e.target().setValue(formatHex(ref.value(), allowAlpha)))).build(),
                (value, field) -> {
                    if (!field.isFocused()) {
                        field.setValue(formatHex(value, allowAlpha));
                    }
                }
        );
    }

    public static FZTextField hexField(
            String key,
            FZRef<Integer> ref,
            IntConsumer changeHandler,
            UnaryOperator<FZTextField.Builder> builder
    ) {
        return hexField(key, true, ref, changeHandler, builder);
    }

    public static FZTextField hexField(String key, boolean allowAlpha, FZRef<Integer> ref, IntConsumer changeHandler) {
        return hexField(key, allowAlpha, ref, changeHandler, UnaryOperator.identity());
    }

    public static FZTextField hexField(String key, FZRef<Integer> ref, IntConsumer changeHandler) {
        return hexField(key, true, ref, changeHandler);
    }

    public static WidgetRenderables colorPreviewSprites(IntSupplier argb) {
        RenderableRectangle sprite = Renderables.sprite(BUTTON_SPRITE);
        RenderableRectangle spriteHighlighted = Renderables.sprite(BUTTON_HIGHLIGHTED_SPRITE);
        RenderableRectangle checkered = checkerboard(new IntIntMutablePair(0, 0)).then(dynamicFill(argb)).shrink(1);
        RenderableRectangle enabled = sprite.then(checkered);

        return new WidgetRenderables(enabled, enabled, spriteHighlighted.then(checkered));
    }

    public static WidgetRenderables colorPreviewSprites(int argb) {
        return colorPreviewSprites(() -> argb);
    }

    public static FZIcon.Builder colorPreview(IntSupplier argb) {
        return FZIcon.builder(colorPreviewSprites(argb)).width(64).height(16);
    }

    public static FZIcon.Builder colorPreview(int argb) {
        return colorPreview(() -> argb);
    }

    public static FZIconButton.Builder colorSwatch(int argb, Component name) {
        return FZIconButton.builder()
                .background(ColorWidgets.colorPreviewSprites(argb))
                .size(16, 16)
                .tooltip(name);
    }

    public static FZIconButton.Builder colorSwatch(int argb) {
        return colorSwatch(argb, Component.literal(formatHex(argb)));
    }

    private ColorWidgets() {
    }
}
