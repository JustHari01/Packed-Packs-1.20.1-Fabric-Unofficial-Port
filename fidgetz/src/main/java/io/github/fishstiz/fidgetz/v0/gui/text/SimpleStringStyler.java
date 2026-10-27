package io.github.fishstiz.fidgetz.v0.gui.text;

import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class SimpleStringStyler implements TextStyleMatcher {
    private final Predicate<String> predicate;
    private final Style style;
    private @Nullable String input;
    private boolean found;

    public SimpleStringStyler(Style style, Predicate<String> predicate) {
        this.style = style;
        this.predicate = predicate;
    }

    @Override
    public boolean styleable(String input) {
        return predicate.test(input);
    }

    @Override
    public void reset(String input) {
        this.input = input;
        this.found = false;
    }

    @Override
    public boolean find() {
        if (!found && input != null && styleable(input)) {
            found = true;
            return true;
        }
        return false;
    }

    @Override
    public int start() {
        return 0;
    }

    @Override
    public int end() {
        return input != null ? input.length() : 0;
    }

    @Override
    public Style style() {
        return style;
    }

    @Override
    public TextStyleMatcher copy() {
        SimpleStringStyler copy = new SimpleStringStyler(style, predicate);
        copy.input = this.input;
        copy.found = this.found;
        return copy;
    }
}