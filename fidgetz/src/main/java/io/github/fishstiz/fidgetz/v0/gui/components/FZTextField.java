package io.github.fishstiz.fidgetz.v0.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.inject.mixins.access.EditBoxAccess;
import io.github.fishstiz.fidgetz.v0.utils.*;
import io.github.fishstiz.fidgetz.v0.gui.text.TextStyleFormatter;
import io.github.fishstiz.fidgetz.v0.gui.text.TextStyleMatcher;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class FZTextField extends EditBox implements FZComponent, FZContextMenu.Source {
    private static final char SECTION_SIGN_PLACEHOLDER = '¶';
    private static final char SECTION_SIGN = '§';
    private static final int DEFAULT_MAX_LENGTH = 64;
    private static final int DEFAULT_WIDTH = 150;
    private static final int DEFAULT_HEIGHT = 20;
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private final Formatter formatter = new Formatter();
    private final Consumer<String> responder;
    private final boolean bound;
    private Consumer<ChangeEvent> changeHandler = FunctionUtils.nopConsumer();
    private Consumer<ConfirmEvent> confirmHandler = FunctionUtils.nopConsumer();
    private Consumer<BlurEvent> blurHandler = FunctionUtils.nopConsumer();
    private ScreenRectangle bounds;
    private Predicate<String> filter = ignored -> true;
    private boolean allowSectionSign;
    private int disableResponderCount = 0;
    private String previousValue = "";
    private int previousCursorPos;
    private int previousHighlightPos;

    // when bound
    private boolean valueBound;
    private int pendingCursorPos;
    private int pendingHighlightPos;
    private @Nullable String pendingValue;

    // for allowing section sign
    private IntArrayList sectionSignPositions = IntArrayList.of();
    private int sectionSignStart;

    private FZTextField(Font font, int width, int height, Component narration, boolean bound) {
        super(font, 0, 0, width, height, narration);
        this.bound = bound;
        bounds = super.getRectangle();
        setMaxLength(DEFAULT_MAX_LENGTH);
        super.setFormatter(formatter);
        this.responder = this::handleChange;
        super.setResponder(responder);
    }

    private FZTextField(boolean bound) {
        this(Minecraft.getInstance().font, DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY, bound);
    }

    private boolean isBound() {
        return bound && valueBound;
    }

    private void disableResponder() {
        disableResponderCount++;
        if (disableResponderCount > 0) {
            super.setResponder(FunctionUtils.nopConsumer());
        }
    }

    private void enableResponder() {
        disableResponderCount--;
        if (disableResponderCount <= 0) {
            super.setResponder(responder);
        }
    }

    private int getHighlightPosition() {
        return ((EditBoxAccess) (Object) this).fidgetz$getHighlightPos();
    }

    private String replacePlaceholders(String value) {
        if (sectionSignPositions.isEmpty() || value.isEmpty()) {
            return value;
        }

        StringBuilder stringBuilder = new StringBuilder(value);
        for (int position : sectionSignPositions) {
            int absolutePosition = sectionSignStart + position;
            if (absolutePosition >= value.length()) {
                continue;
            }
            if (value.charAt(absolutePosition) == SECTION_SIGN_PLACEHOLDER) {
                stringBuilder.setCharAt(absolutePosition, SECTION_SIGN);
            }
        }

        return stringBuilder.toString();
    }

    @Override
    public void setValue(String value) {
        int cursorPos = getCursorPosition();
        int highlightPos = getHighlightPosition();

        disableResponder();
        super.setValue(value);
        enableResponder();

        if (value.equalsIgnoreCase(getValue())) {
            setCursorPosition(cursorPos);
            setHighlightPos(highlightPos);
        }

        this.previousValue = value;
        this.previousCursorPos = getCursorPosition();
        this.previousHighlightPos = getHighlightPosition();
    }

    private void handleChange(String originalValue) {
        String value = replacePlaceholders(originalValue);
        boolean valid = filter.test(value);
        boolean isBound = isBound();

        disableResponder();

        int cursorPos = getCursorPosition();
        int highlightPos = getHighlightPosition();

        if (!valid || isBound) {
            super.setValue(previousValue);
            setCursorPosition(previousCursorPos);
            setHighlightPos(previousHighlightPos);

            if (valid) {
                pendingValue = value;
                pendingCursorPos = cursorPos;
                pendingHighlightPos = highlightPos;
                changeHandler.accept(new ChangeEvent(this, value));
            }
        } else {
            if (!value.equalsIgnoreCase(originalValue)) {
                super.setValue(value);
                setCursorPosition(cursorPos);
                setHighlightPos(highlightPos);
            }

            previousValue = value;
            previousHighlightPos = getHighlightPosition();
            previousCursorPos = getCursorPosition();
            changeHandler.accept(new ChangeEvent(this, value));
        }

        enableResponder();
    }

    private void setBoundValue(String value) {
        this.valueBound = true;
        disableResponder();

        super.setValue(value);
        if (pendingValue != null && pendingValue.equalsIgnoreCase(value)) {
            setCursorPosition(pendingCursorPos);
            setHighlightPos(pendingHighlightPos);
        } else {
            moveCursorToEnd();
            setHighlightPos(getCursorPosition());
        }

        this.previousValue = value;
        this.previousHighlightPos = getHighlightPosition();
        this.previousCursorPos = getCursorPosition();
        this.pendingValue = null;

        enableResponder();
    }

    @Override
    public void insertText(String input) {
        if (!allowSectionSign) {
            super.insertText(input);
            return;
        }

        IntArrayList positions = new IntArrayList();
        StringBuilder stringBuilder = new StringBuilder(input.length());

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == SECTION_SIGN) {
                positions.add(i);
                stringBuilder.append(SECTION_SIGN_PLACEHOLDER);
            } else {
                stringBuilder.append(c);
            }
        }

        sectionSignStart = Math.min(getCursorPosition(), getHighlightPosition());
        sectionSignPositions = positions;
        super.insertText(stringBuilder.toString());
        sectionSignPositions = IntArrayList.of();
        sectionSignStart = 0;
    }

    public void deleteCharsToPos(int pos) {
        String previousValue = getValue();
        int previousCursorPos = getCursorPosition();
        int previousHighlightPos = getHighlightPosition();

        ((EditBoxAccess) (Object) this).fidgetz$invokeDeleteText(pos - getCursorPosition());

        String newValue = getValue();
        if (newValue.equalsIgnoreCase(previousValue) || newValue.length() == previousValue.length()) {
            setCursorPosition(previousCursorPos);
            setHighlightPos(previousHighlightPos);
        }
    }

    @Override
    public void setHighlightPos(int pos) {
        super.setHighlightPos(pos);
        if (disableResponderCount <= 0 && previousValue.equalsIgnoreCase(getValue())) {
            previousHighlightPos = getHighlightPosition();
        }
    }

    @Override
    public void setCursorPosition(int pos) {
        super.setCursorPosition(pos);
        if (disableResponderCount <= 0 && previousValue.equalsIgnoreCase(getValue())) {
            previousCursorPos = getCursorPosition();
        }
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        moveCursorTo(getCursorPosition(), false);
    }

    public void setSize(int width, int height) {
        io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils.setSize(this, width, height);
        moveCursorTo(getCursorPosition(), false);
    }

    public void moveCursorTo(int delta, boolean select) {
        boolean unchanged = previousValue.equals(getValue());
        if (unchanged) disableResponder();
        setCursorPosition(delta);
        if (!select) {
            setHighlightPos(getCursorPosition());
        }
        if (unchanged) enableResponder();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.renderWidget(graphics, mouseX, mouseY, a);
        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, a);
        }
    }

    @Override
    public void setFocused(boolean focused) {
        boolean previousFocused = isFocused();
        super.setFocused(focused);
        if (previousFocused && !isFocused()) {
            blurHandler.accept(new BlurEvent(this));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getValue().isEmpty() && (keyCode == InputConstants.KEY_LEFT || keyCode == InputConstants.KEY_RIGHT)) {
            return false;
        }
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            ConfirmEvent confirmEvent = new ConfirmEvent(this, keyCode, scanCode, modifiers);
            confirmHandler.accept(confirmEvent);
            return confirmEvent.confirmed;
        }
        return false;
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
    @Deprecated
    public void setResponder(Consumer<String> responder) {
        throw new UnsupportedOperationException("setResponder is not supported. Use builder to set the onChange handler instead.");
    }

    @Override
    public void setFormatter(BiFunction<String, Integer, FormattedCharSequence> textFormatter) {
        this.formatter.formatters = CollectionUtils.addLast(this.formatter.formatters, formatter);
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(bounds, this)) {
            this.bounds = super.getRectangle();
        }
        return this.bounds;
    }

    private void applyProps(Props props) {
        propsState.apply(this, props);
        if (props.editable() != TriState.DEFAULT) setEditable(props.editable().toBoolean(true));
        if (props.allowSectionSign() != TriState.DEFAULT) allowSectionSign = props.allowSectionSign().toBoolean(false);
        props.filter().ifPresent(filter -> this.filter = filter.value());
        props.maxLength().ifPresent(this::setMaxLength);
        props.hint().ifPresent(this::setHint);
        props.suggestion().ifDefined(this::setSuggestion);
        props.textColor().ifPresent(this::setTextColor);

        props.changeHandler().ifPresent(changeHandler -> this.changeHandler = changeHandler.value());
        props.confirmHandler().ifPresent(confirmHandler -> this.confirmHandler = confirmHandler.value());
        props.blurHandler().ifPresent(blurHandler -> this.blurHandler = blurHandler.value());

        props.text().ifPresentOrElse(this::setBoundValue, () -> valueBound = false);

        if (props.styleMatchers().isPresent() && props.formatters().isPresent()) {
            List<BiFunction<String, Integer, FormattedCharSequence>> formatters = props.formatters()
                    .get()
                    .stream()
                    .map(FZKeyed::value)
                    .collect(Collectors.toCollection(ArrayList::new));

            TextStyleFormatter styleMatchers = new TextStyleFormatter(props.styleMatchers().get(), this::getValue);
            for (BiFunction<String, Integer, FormattedCharSequence> previousFormatter : formatter.formatters) {
                if (previousFormatter instanceof TextStyleFormatter previousStyleMatchers) {
                    // just initialize the first one, this is just to prevent flashing unformatted styles when props change.
                    // will fix itself anyway if styleMatchers have genuinely changed
                    styleMatchers.initializeStyles(previousStyleMatchers);
                    break;
                }
            }
            formatters.add(styleMatchers);
            formatter.formatters = formatters;
        } else if (props.styleMatchers().isPresent()) {
            TextStyleFormatter styleMatchers = new TextStyleFormatter(props.styleMatchers().get(), this::getValue);
            for (BiFunction<String, Integer, FormattedCharSequence> previousFormatter : formatter.formatters) {
                if (previousFormatter instanceof TextStyleFormatter previousStyleMatchers) {
                    styleMatchers.initializeStyles(previousStyleMatchers);
                    break;
                }
            }
            formatter.formatters = List.of(styleMatchers);
        } else if (props.formatters().isPresent()) {
            formatter.formatters = props.formatters().get().stream().map(FZKeyed::value).toList();
        }
    }

    public static FZTextField bind(String key, FZRef<Props> ref) {
        FZTextField textField = new FZTextField(true);
        textField.applyProps(ref.value());
        ref.subscribe(key, textField::applyProps);
        return textField;
    }

    public static Builder builder() {
        return new Builder();
    }

    private static final class Formatter implements BiFunction<String, Integer, FormattedCharSequence> {
        private List<BiFunction<String, Integer, FormattedCharSequence>> formatters = Collections.emptyList();

        @Override
        public FormattedCharSequence apply(String text, Integer offset) {
            for (BiFunction<String, Integer, FormattedCharSequence> formatter : formatters) {
                FormattedCharSequence result = formatter.apply(text, offset);
                if (result != null) return result;
            }
            return FormattedCharSequence.forward(text, Style.EMPTY);
        }
    }

    public record ChangeEvent(FZTextField target, String value) {
    }

    public static final class ConfirmEvent {
        private final FZTextField target;
        private final int keyCode;
        private final int scanCode;
        private final int modifiers;
        private boolean confirmed;

        public ConfirmEvent(FZTextField target, int keyCode, int scanCode, int modifiers) {
            this.target = target;
            this.keyCode = keyCode;
            this.scanCode = scanCode;
            this.modifiers = modifiers;
        }

        public FZTextField target() {
            return target;
        }

        public int keyCode() {
            return keyCode;
        }

        public int scanCode() {
            return scanCode;
        }

        public int modifiers() {
            return modifiers;
        }

        public void confirm() {
            this.confirmed = true;
        }
    }

    public record BlurEvent(FZTextField target) {
    }

    public interface Props extends GuiComponentProps {
        default Optional<String> text() {
            return Optional.empty();
        }

        default OptionalInt textColor() {
            return OptionalInt.empty();
        }

        default TriState editable() {
            return TriState.DEFAULT;
        }

        default Optional<Component> hint() {
            return Optional.empty();
        }

        default Undefinable<@Nullable String> suggestion() {
            return Undefinable.undefined();
        }

        default OptionalInt maxLength() {
            return OptionalInt.empty();
        }

        default TriState allowSectionSign() {
            return TriState.DEFAULT;
        }

        default Optional<List<FZKeyed<BiFunction<String, Integer, FormattedCharSequence>>>> formatters() {
            return Optional.empty();
        }

        default Optional<List<TextStyleMatcher>> styleMatchers() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Predicate<String>>> filter() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Consumer<ChangeEvent>>> changeHandler() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Consumer<ConfirmEvent>>> confirmHandler() {
            return Optional.empty();
        }

        default Optional<FZKeyed<Consumer<BlurEvent>>> blurHandler() {
            return Optional.empty();
        }
    }

    private static final class PropsImpl extends GuiComponentPropsBase implements Props {
        private final @Nullable String text;
        private final @Nullable Integer textColor;
        private final TriState editable;
        private final @Nullable Component hint;
        private final Undefinable<@Nullable String> suggestion;
        private final @Nullable Integer maxLength;
        private final TriState allowSectionSign;
        private final @Nullable List<FZKeyed<BiFunction<String, Integer, FormattedCharSequence>>> formatters;
        private final @Nullable List<TextStyleMatcher> styleMatchers;
        private final @Nullable FZKeyed<Predicate<String>> filter;
        private final @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;
        private final @Nullable FZKeyed<Consumer<ConfirmEvent>> confirmHandler;
        private final @Nullable FZKeyed<Consumer<BlurEvent>> blurHandler;

        private PropsImpl(
                GuiComponentProps props,
                @Nullable String text,
                @Nullable Integer textColor,
                TriState editable,
                @Nullable Component hint,
                Undefinable<@Nullable String> suggestion,
                @Nullable Integer maxLength,
                TriState allowSectionSign,
                @Nullable List<FZKeyed<BiFunction<String, Integer, FormattedCharSequence>>> formatters,
                @Nullable List<TextStyleMatcher> styleMatchers,
                @Nullable FZKeyed<Predicate<String>> filter,
                @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler,
                @Nullable FZKeyed<Consumer<ConfirmEvent>> confirmHandler,
                @Nullable FZKeyed<Consumer<BlurEvent>> blurHandler
        ) {
            super(props);
            this.text = text;
            this.textColor = textColor;
            this.editable = editable;
            this.hint = hint;
            this.suggestion = suggestion;
            this.maxLength = maxLength;
            this.allowSectionSign = allowSectionSign;
            this.formatters = formatters;
            this.styleMatchers = styleMatchers;
            this.filter = filter;
            this.changeHandler = changeHandler;
            this.confirmHandler = confirmHandler;
            this.blurHandler = blurHandler;
        }

        @Override
        public Optional<String> text() {
            return Optional.ofNullable(text);
        }

        @Override
        public OptionalInt textColor() {
            return wrapBoxedInt(textColor);
        }

        @Override
        public TriState editable() {
            return editable;
        }

        @Override
        public Optional<Component> hint() {
            return Optional.ofNullable(hint);
        }

        @Override
        public Undefinable<@Nullable String> suggestion() {
            return suggestion;
        }

        @Override
        public OptionalInt maxLength() {
            return maxLength == null ? OptionalInt.empty() : OptionalInt.of(maxLength);
        }

        @Override
        public TriState allowSectionSign() {
            return allowSectionSign;
        }

        @Override
        public Optional<List<FZKeyed<BiFunction<String, Integer, FormattedCharSequence>>>> formatters() {
            return Optional.ofNullable(formatters);
        }

        @Override
        public Optional<List<TextStyleMatcher>> styleMatchers() {
            return Optional.ofNullable(styleMatchers);
        }

        @Override
        public Optional<FZKeyed<Predicate<String>>> filter() {
            return Optional.ofNullable(filter);
        }

        @Override
        public Optional<FZKeyed<Consumer<ChangeEvent>>> changeHandler() {
            return Optional.ofNullable(changeHandler);
        }

        @Override
        public Optional<FZKeyed<Consumer<ConfirmEvent>>> confirmHandler() {
            return Optional.ofNullable(confirmHandler);
        }

        @Override
        public Optional<FZKeyed<Consumer<BlurEvent>>> blurHandler() {
            return Optional.ofNullable(blurHandler);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o) &&
                   Objects.equals(text(), other.text()) &&
                   Objects.equals(textColor(), other.textColor()) &&
                   editable == other.editable() &&
                   Objects.equals(hint(), other.hint()) &&
                   Objects.equals(suggestion(), other.suggestion()) &&
                   Objects.equals(maxLength(), other.maxLength()) &&
                   allowSectionSign == other.allowSectionSign() &&
                   Objects.equals(formatters(), other.formatters()) &&
                   Objects.equals(styleMatchers(), other.styleMatchers()) &&
                   Objects.equals(filter(), other.filter()) &&
                   Objects.equals(changeHandler(), other.changeHandler()) &&
                   Objects.equals(confirmHandler(), other.confirmHandler()) &&
                   Objects.equals(blurHandler(), other.blurHandler());
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    super.hashCode(),
                    text,
                    textColor,
                    editable,
                    hint,
                    suggestion,
                    maxLength,
                    allowSectionSign,
                    formatters,
                    styleMatchers,
                    filter,
                    changeHandler,
                    confirmHandler,
                    blurHandler
            );
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private @Nullable String text;
        private @Nullable Integer textColor;
        private TriState editable = TriState.DEFAULT;
        private @Nullable Component hint;
        private Undefinable<@Nullable String> suggestion = Undefinable.undefined();
        private @Nullable Integer maxLength;
        private TriState allowSectionSign = TriState.DEFAULT;
        private @Nullable List<FZKeyed<BiFunction<String, Integer, FormattedCharSequence>>> formatters;
        private @Nullable FZKeyed<Predicate<String>> filter;
        private @Nullable FZKeyed<Consumer<ChangeEvent>> changeHandler;
        private @Nullable FZKeyed<Consumer<ConfirmEvent>> confirmHandler;
        private @Nullable FZKeyed<Consumer<BlurEvent>> blurHandler;
        private @Nullable List<TextStyleMatcher> styleMatchers;

        private Builder() {
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder textColor(int color) {
            this.textColor = color;
            return this;
        }

        public Builder editable(boolean editable) {
            this.editable = TriState.from(editable);
            return this;
        }

        public Builder editable() {
            return editable(true);
        }

        public Builder uneditable() {
            return editable(false);
        }

        public Builder hint(Component hint) {
            this.hint = hint;
            return this;
        }

        public Builder suggestion(@Nullable String suggestion) {
            this.suggestion = Undefinable.of(suggestion);
            return this;
        }

        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder allowSectionSign(boolean allowSectionSign) {
            this.allowSectionSign = TriState.from(allowSectionSign);
            return this;
        }

        public Builder allowSectionSign() {
            return allowSectionSign(true);
        }

        public Builder filter(Object key, Predicate<String> filter) {
            this.filter = new FZKeyed<>(key, filter);
            return this;
        }

        public Builder filter(Predicate<String> filter) {
            this.filter = FZKeyed.selfKey(filter);
            return this;
        }

        public Builder formatter(Object key, BiFunction<String, Integer, FormattedCharSequence> formatter) {
            if (formatters == null) {
                formatters = Lists.newArrayList(new FZKeyed<>(key, formatter));
            } else {
                formatters.add(new FZKeyed<>(key, formatter));
            }
            return this;
        }

        public Builder formatter(BiFunction<String, Integer, FormattedCharSequence> formatter) {
            return formatter(formatter, formatter);
        }

        public Builder styleMatcher(TextStyleMatcher styleMatcher) {
            if (this.styleMatchers == null) {
                this.styleMatchers = Lists.newArrayList(styleMatcher);
            } else {
                this.styleMatchers.add(styleMatcher);
            }
            return this;
        }

        public Builder styleMatchers(List<TextStyleMatcher> styleMatchers) {
            if (this.styleMatchers == null) {
                this.styleMatchers = new ArrayList<>(styleMatchers);
            } else {
                this.styleMatchers.addAll(styleMatchers);
            }
            return this;
        }

        public Builder onChange(Object key, Consumer<ChangeEvent> changeHandler) {
            this.changeHandler = new FZKeyed<>(key, Objects.requireNonNull(changeHandler, "changeHandler cannot be null"));
            return this;
        }

        public Builder onChange(Consumer<ChangeEvent> changeHandler) {
            return onChange(changeHandler, changeHandler);
        }

        public Builder onConfirm(Object key, Consumer<ConfirmEvent> confirmHandler) {
            this.confirmHandler = new FZKeyed<>(key, Objects.requireNonNull(confirmHandler, "confirmHandler cannot be null"));
            return this;
        }

        public Builder onConfirm(Consumer<ConfirmEvent> confirmHandler) {
            return onConfirm(confirmHandler, confirmHandler);
        }

        public Builder onBlur(Object key, Consumer<BlurEvent> blurHandler) {
            this.blurHandler = new FZKeyed<>(key, Objects.requireNonNull(blurHandler, "blurHandler cannot be null"));
            return this;
        }

        public Builder onBlur(Consumer<BlurEvent> blurHandler) {
            return onBlur(blurHandler, blurHandler);
        }

        public Props toProps() {
            return new PropsImpl(
                    props,
                    text,
                    textColor,
                    editable,
                    hint,
                    suggestion,
                    maxLength,
                    allowSectionSign,
                    formatters,
                    styleMatchers,
                    filter,
                    changeHandler,
                    confirmHandler,
                    blurHandler
            );
        }

        public FZTextField build() {
            FZTextField textField = new FZTextField(false);
            textField.applyProps(toProps());
            return textField;
        }
    }
}
