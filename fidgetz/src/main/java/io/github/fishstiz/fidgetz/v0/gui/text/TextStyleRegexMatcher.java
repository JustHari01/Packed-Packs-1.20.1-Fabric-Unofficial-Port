package io.github.fishstiz.fidgetz.v0.gui.text;

import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextStyleRegexMatcher implements TextStyleMatcher {
    private final @Nullable Predicate<String> styleable;
    private final Matcher matcher;
    private final Style style;
    private boolean matched;

    public TextStyleRegexMatcher(Matcher matcher, Style style, @Nullable Predicate<String> styleable) {
        this.matcher = matcher;
        this.style = style;
        this.styleable = styleable;
    }

    public TextStyleRegexMatcher(Pattern pattern, Style style, @Nullable Predicate<String> styleable) {
        this(pattern.matcher(""), style, styleable);
    }

    public TextStyleRegexMatcher(Pattern pattern, Style style) {
        this(pattern, style, null);
    }

    @Override
    public boolean styleable(String input) {
        return styleable == null || styleable.test(input);
    }

    @Override
    public void reset(String input) {
        matcher.reset(input);
        matched = false;
    }

    @Override
    public boolean find() {
        matched = matcher.find();
        return matched;
    }

    @Override
    public int start() {
        return matcher.start();
    }

    @Override
    public int end() {
        return matcher.end();
    }

    @Override
    public Style style() {
        return matched ? style : Style.EMPTY;
    }

    @Override
    public TextStyleMatcher copy() {
        return new TextStyleRegexMatcher(matcher.pattern(), style, styleable);
    }
}
