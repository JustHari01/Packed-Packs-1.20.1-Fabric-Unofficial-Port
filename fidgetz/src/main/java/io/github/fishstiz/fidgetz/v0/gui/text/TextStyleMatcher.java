package io.github.fishstiz.fidgetz.v0.gui.text;

import net.minecraft.network.chat.Style;

public interface TextStyleMatcher {
    boolean styleable(String input);

    void reset(String input);

    boolean find();

    int start();

    int end();

    Style style();

    TextStyleMatcher copy();
}
