package io.github.fishstiz.fidgetz.v0.gui.text;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class TextStyleFormatter implements BiFunction<String, Integer, FormattedCharSequence> {
    private final TextStyleMatcher[] matchers;
    private final Supplier<String> fullText;
    private final Executor executor;
    private final StyledSequence readSequence = new StyledSequence();
    private final StyledSequence writeSequence = new StyledSequence();
    private final Runnable styleFinder = this::findStyles;
    private volatile StyledSequence currentSequence = readSequence;
    private volatile String lastText = "";

    public TextStyleFormatter(List<TextStyleMatcher> matchers, Supplier<String> fullText, Executor executor) {
        this.matchers = matchers.stream().map(TextStyleMatcher::copy).toArray(TextStyleMatcher[]::new);
        this.fullText = fullText;
        this.executor = executor;
    }

    public TextStyleFormatter(List<TextStyleMatcher> matchers, Supplier<String> fullText) {
        this(matchers, fullText, Util.backgroundExecutor());
    }

    // prevents flashing unformatted styles when rebuilt
    public void initializeStyles(TextStyleFormatter formatter) {
        StyledSequence prevSeq = formatter.currentSequence;
        String prevText = prevSeq.text;
        if (prevText.isEmpty()) return;

        readSequence.text = prevText;
        readSequence.charStyles.size(prevSeq.charStyles.size());
        for (int i = 0; i < prevSeq.charStyles.size(); i++) {
            readSequence.charStyles.set(i, prevSeq.charStyles.get(i));
        }
        currentSequence = readSequence;
    }

    @Override
    public @Nullable FormattedCharSequence apply(String displayText, Integer displayPos) {
        String currentText = fullText.get();
        if (currentText.isBlank()) return null;

        StyledSequence current = currentSequence;
        current.text = currentText;
        current.displayPos = displayPos;
        current.displayEnd = displayPos + displayText.length();

        if (!currentText.equals(lastText)) {
            lastText = currentText;
            executor.execute(styleFinder);
        }

        return current;
    }

    private void findStyles() {
        StyledSequence write = (currentSequence == readSequence) ? writeSequence : readSequence;
        String text = lastText;

        write.text = text;
        write.charStyles.size(text.length());
        Arrays.fill(write.charStyles.elements(), 0, write.charStyles.size(), null);

        for (TextStyleMatcher matcher : matchers) {
            if (!matcher.styleable(text)) continue;
            matcher.reset(text);
            while (matcher.find()) {
                for (int i = matcher.start(); i < matcher.end(); i++) {
                    write.charStyles.set(i, matcher.style());
                }
            }
        }

        currentSequence = write;
    }

    private static final class StyledSequence implements FormattedCharSequence {
        private final ObjectArrayList<Style> charStyles = new ObjectArrayList<>();
        private volatile String text = "";
        private int displayPos;
        private int displayEnd;

        @Override
        public boolean accept(FormattedCharSink sink) {
            String currentText = text;
            int start = Math.max(0, displayPos);
            int end = Math.min(currentText.length(), displayEnd);

            for (int i = start; i < end; i++) {
                Style style = i < charStyles.size() ? charStyles.get(i) : null;
                if (!sink.accept(i - displayPos, Objects.requireNonNullElse(style, Style.EMPTY), currentText.charAt(i))) {
                    return false;
                }
            }

            return true;
        }
    }
}
