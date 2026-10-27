package io.github.fishstiz.fidgetz.v0.gui.layouts;

import io.github.fishstiz.fidgetz.v0.utils.IntIntBiConsumer;
import io.github.fishstiz.fidgetz.v0.utils.MathUtils;
import io.github.fishstiz.fidgetz.v0.utils.TriState;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.AbstractLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FZFlexLayout extends AbstractLayout implements FZLayout {
    private final Supplier<ScreenRectangle> screenArea;
    private final Settings defaultChildSettings = Settings.defaults();
    private final List<ChildContainer> children = new ArrayList<>();
    private final ScreenAxis axis;
    private int maxWidth;
    private int maxHeight;
    private int mainSpacing;
    private int lineSpacing;
    private Justification mainJustification = Justification.START;
    private Justification crossJustification = Justification.START;
    private boolean wrap;
    private boolean visible = true;

    FZFlexLayout(Supplier<ScreenRectangle> screenArea, ScreenAxis axis) {
        super(0, 0, screenArea.get().width(), screenArea.get().height());
        this.screenArea = screenArea;
        this.axis = axis;
    }

    static FZFlexLayout auto(ScreenAxis axis) {
        return new FZFlexLayout(ScreenRectangle::empty, axis);
    }

    public static FZFlexLayout horizontal(Screen screen) {
        return new FZFlexLayout(screen::getRectangle, ScreenAxis.HORIZONTAL);
    }

    public static FZFlexLayout horizontal(LayoutElement container) {
        return new FZFlexLayout(container::getRectangle, ScreenAxis.HORIZONTAL);
    }

    public static FZFlexLayout horizontal() {
        return auto(ScreenAxis.HORIZONTAL);
    }

    public static FZFlexLayout vertical(Screen screen) {
        return new FZFlexLayout(screen::getRectangle, ScreenAxis.VERTICAL);
    }

    public static FZFlexLayout vertical(LayoutElement container) {
        return new FZFlexLayout(container::getRectangle, ScreenAxis.VERTICAL);
    }

    public static FZFlexLayout vertical() {
        return auto(ScreenAxis.VERTICAL);
    }

    private static <T> T getAxisValue(ScreenAxis axis, T horizontalValue, T verticalValue) {
        return switch (axis) {
            case HORIZONTAL -> horizontalValue;
            case VERTICAL -> verticalValue;
        };
    }

    public FZFlexLayout maxWidth(int maxWidth) {
        this.maxWidth = MathUtils.optionalMin(maxWidth, screenArea.get().width());
        return this;
    }

    public FZFlexLayout maxHeight(int maxHeight) {
        this.maxHeight = MathUtils.optionalMin(maxHeight, screenArea.get().height());
        return this;
    }

    public FZFlexLayout maxSize(int maxWidth, int maxHeight) {
        return maxWidth(maxWidth).maxHeight(maxHeight);
    }

    public FZFlexLayout alignContents(Justification justification) {
        this.crossJustification = justification;
        return this;
    }

    public FZFlexLayout justifyContents(Justification justification) {
        this.mainJustification = justification;
        return this;
    }

    public FZFlexLayout spacing(int spacing) {
        this.mainSpacing = spacing;
        this.lineSpacing = spacing;
        return this;
    }

    public FZFlexLayout verticalSpacing(int spacing) {
        switch (axis) {
            case HORIZONTAL -> this.lineSpacing = spacing;
            case VERTICAL -> this.mainSpacing = spacing;
        }
        return this;
    }

    public FZFlexLayout horizontalSpacing(int spacing) {
        switch (axis) {
            case HORIZONTAL -> this.mainSpacing = spacing;
            case VERTICAL -> this.lineSpacing = spacing;
        }
        return this;
    }

    public FZFlexLayout wrap(boolean wrap) {
        this.wrap = wrap;
        return this;
    }

    public FZFlexLayout wrap() {
        return this.wrap(true);
    }

    public FZFlexLayout visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public FZFlexLayout visible() {
        return visible(true);
    }

    public FZFlexLayout invisible() {
        return visible(false);
    }

    @Override
    public boolean fidgetz$isVisible() {
        return visible;
    }

    @Override
    public void fidgetz$setWidth(int width) {
        int previousMaxWidth = this.maxWidth;
        maxWidth(width);
        if (previousMaxWidth != maxWidth) {
            arrangeElements();
        }
    }

    @Override
    public void fidgetz$setHeight(int height) {
        int previousMaxHeight = this.maxHeight;
        maxHeight(height);
        if (previousMaxHeight != maxHeight) {
            arrangeElements();
        }
    }

    @Override
    public void fidgetz$setSize(int width, int height) {
        int previousMaxWidth = this.maxWidth;
        int previousMaxHeight = this.maxHeight;
        maxSize(width, height);
        if (previousMaxWidth != maxWidth || previousMaxHeight != maxHeight) {
            arrangeElements();
        }
    }

    public ScreenAxis mainAxis() {
        return axis;
    }

    private boolean canWrap() {
        return wrap && getMaxLength(axis) > 0;
    }

    private int getPosition(ScreenAxis axis) {
        return switch (axis) {
            case HORIZONTAL -> getX();
            case VERTICAL -> getY();
        };
    }

    private int getMaxLength(ScreenAxis axis) {
        return switch (axis) {
            case HORIZONTAL -> MathUtils.eitherOptionalMin(maxWidth, screenArea.get().width());
            case VERTICAL -> MathUtils.eitherOptionalMin(maxHeight, screenArea.get().height());
        };
    }

    public FZFlexLayout also(Consumer<FZFlexLayout> configurator) {
        configurator.accept(this);
        return this;
    }

    public <T extends LayoutElement> T child(T child, LayoutSettings layoutSettings) {
        children.add(new ChildContainer(child, layoutSettings));
        return child;
    }

    public <T extends LayoutElement> T child(T child) {
        return child(child, defaultChildSettings.layoutSettings);
    }

    public <T extends AbstractWidget> T child(T child, Settings settings) {
        children.add(new FlexChildContainer(child, settings));
        return child;
    }

    public <T extends AbstractWidget> T child(T child, LayoutSettings layoutSettings) {
        return child(child, defaultChildSettings.copyWithLayoutSettings(layoutSettings));
    }

    public <T extends AbstractWidget> T child(T child) {
        return child(child, defaultChildSettings);
    }

    public <T extends FZFlexElement> T child(T child, Settings settings) {
        children.add(new FlexChildContainer(child, settings));
        return child;
    }

    public <T extends FZFlexElement> T child(T child, LayoutSettings layoutSettings) {
        return child(child, defaultChildSettings.copyWithLayoutSettings(layoutSettings));
    }

    public <T extends FZFlexElement> T child(T child) {
        return child(child, defaultChildSettings);
    }

    public FZFlexSpacerElement spacer(Settings settings) {
        FZFlexSpacerElement spacer = new FZFlexSpacerElement();
        children.add(new FlexChildContainer(spacer, settings));
        return spacer;
    }

    public FZFlexSpacerElement spacer() {
        return spacer(defaultChildSettings);
    }

    /**
     * @deprecated use {@link #removeChildren()}
     */
    @Deprecated
    public void clear() {
        children.clear();
    }

    public void removeChildren() {
        children.clear();
    }

    public Settings newChildSettings() {
        return defaultChildSettings.copy();
    }

    public Settings defaultChildSettings() {
        return defaultChildSettings;
    }

    public Settings flexChildMainSettings() {
        return newChildSettings().flexMain();
    }

    public Settings flexChildCrossSettings() {
        return newChildSettings().flexCross();
    }

    public Settings flexChildSettings() {
        return newChildSettings().flexBoth();
    }

    public Settings flexChildVerticalSettings() {
        return switch (axis) {
            case HORIZONTAL -> flexChildCrossSettings();
            case VERTICAL -> flexChildMainSettings();
        };
    }

    public Settings flexChildHorizontalSettings() {
        return switch (axis) {
            case HORIZONTAL -> flexChildMainSettings();
            case VERTICAL -> flexChildCrossSettings();
        };
    }

    private int getEffectiveMax(ChildContainer container) {
        return container.getMaxLength(axis);
    }

    private int getClampedPreferred(ChildContainer container) {
        return MathUtils.clampOptionalMax(
                container.getPreferredLength(axis), container.getMinLength(axis), getEffectiveMax(container));
    }

    private static int remainderShare(int index, int remainder) {
        return index < Math.abs(remainder) ? Integer.signum(remainder) : 0;
    }

    private int sumPreferredOrMinLengths(List<ChildContainer> flexChildren) {
        int sum = 0;
        for (ChildContainer container : flexChildren) {
            int preferred = container.getPreferredLength(axis);
            int min = container.getMinLength(axis);
            sum += preferred > 0 ? preferred : min;
        }
        return sum;
    }

    private int[] distributeMainAxisFlexLengths(List<ChildContainer> flexChildren, int availableSpace) {
        List<ChildContainer> remaining = new ArrayList<>(flexChildren);
        int[] lengths = new int[flexChildren.size()];

        while (!remaining.isEmpty()) {
            int sumPreferred = 0;
            for (ChildContainer container : remaining) {
                sumPreferred += getClampedPreferred(container);
            }

            int extraSpace = availableSpace - sumPreferred;
            int share = extraSpace / remaining.size();
            int remainder = extraSpace % remaining.size();
            List<ChildContainer> nextRemaining = new ArrayList<>();
            boolean anyFrozen = false;

            for (int i = 0; i < remaining.size(); i++) {
                ChildContainer container = remaining.get(i);
                int idx = flexChildren.indexOf(container);
                int minFlexMain = container.getMinLength(axis);
                int maxFlexMain = getEffectiveMax(container);
                int preferred = getClampedPreferred(container);
                int desiredLength = preferred + share + remainderShare(i, remainder);
                int clampedLength = MathUtils.clampOptionalMax(desiredLength, minFlexMain, maxFlexMain);

                if (clampedLength != desiredLength && clampedLength == minFlexMain) {
                    lengths[idx] = clampedLength;
                    availableSpace -= clampedLength;
                    anyFrozen = true;
                } else if (maxFlexMain > 0 && clampedLength == maxFlexMain) {
                    lengths[idx] = clampedLength;
                    availableSpace -= clampedLength;
                    anyFrozen = true;
                } else {
                    nextRemaining.add(container);
                }
            }

            if (!anyFrozen) {
                for (int i = 0; i < remaining.size(); i++) {
                    ChildContainer container = remaining.get(i);
                    int idx = flexChildren.indexOf(container);
                    int minFlexMain = container.getMinLength(axis);
                    int maxFlexMain = getEffectiveMax(container);
                    int preferred = getClampedPreferred(container);
                    lengths[idx] = MathUtils.clampOptionalMax(
                            preferred + share + remainderShare(i, remainder), minFlexMain, maxFlexMain);
                }
                break;
            }
            remaining = nextRemaining;
        }

        return lengths;
    }

    private int applyFlexLengths(List<ChildContainer> flexMainChildren, int[] lengths, int crossLength) {
        ScreenAxis crossAxis = axis.orthogonal();
        int occupied = 0;

        for (int i = 0; i < flexMainChildren.size(); i++) {
            ChildContainer container = flexMainChildren.get(i);
            if (container.flexCross()) {
                int minFlexCross = container.getMinLength(crossAxis);
                int maxFlexCross = container.getMaxLength(crossAxis);
                container.setSize(axis, lengths[i], MathUtils.clampOptionalMax(crossLength, minFlexCross, maxFlexCross));
            } else {
                container.setLength(axis, lengths[i]);
            }
            occupied += container.getLength(axis);
        }

        return occupied;
    }

    @Override
    public int getWidth() {
        return MathUtils.optionalMin(super.getWidth(), this.maxWidth);
    }

    @Override
    public int getHeight() {
        return MathUtils.optionalMin(super.getHeight(), this.maxHeight);
    }

    private void arrangeLinear() {
        super.arrangeElements();

        final ScreenAxis crossAxis = axis.orthogonal();
        final int maxMain = getMaxLength(axis);
        final int maxCross = getMaxLength(crossAxis);

        List<ChildContainer> flexMain = new ArrayList<>();
        List<ChildContainer> flexCrossOnly = new ArrayList<>();
        int occupiedMain = 0;
        int occupiedCross = maxCross;
        int visibleCount = 0;

        for (ChildContainer container : children) {
            if (!container.visible()) continue;

            if (container.flexMain()) {
                flexMain.add(container);
            } else {
                occupiedMain += container.getLength(axis);
                if (container.flexCross()) {
                    flexCrossOnly.add(container);
                }
            }

            visibleCount++;
            int crossLength = container.getLength(crossAxis);
            if (container.flexCross()) {
                int minFlexCross = container.getMinLength(crossAxis);
                int maxFlexCross = container.getMaxLength(crossAxis);
                crossLength = MathUtils.clampOptionalMax(crossLength, minFlexCross, maxFlexCross);
            }
            occupiedCross = MathUtils.clampOptionalMax(crossLength, occupiedCross, maxCross);
        }

        int totalSpacing = mainSpacing * Math.max(0, visibleCount - 1);
        if (!flexMain.isEmpty()) {
            int availableSpace = maxMain <= 0
                    ? sumPreferredOrMinLengths(flexMain)
                    : maxMain - occupiedMain - totalSpacing;
            int[] lengths = distributeMainAxisFlexLengths(flexMain, availableSpace);
            occupiedMain += applyFlexLengths(flexMain, lengths, occupiedCross);
        }

        occupiedMain += totalSpacing;

        for (ChildContainer container : flexCrossOnly) {
            int minFlexCross = container.getMinLength(crossAxis);
            int maxFlexCross = container.getMaxLength(crossAxis);
            container.setLength(crossAxis, MathUtils.clampOptionalMax(occupiedCross, minFlexCross, maxFlexCross));
        }

        int mainResult = maxMain == 0 ? Math.max(0, occupiedMain) : maxMain;
        int crossResult = maxCross == 0 ? Math.max(0, occupiedCross) : maxCross;

        Justification.Results justified = mainJustification.compute(
                getPosition(axis),
                mainSpacing,
                visibleCount,
                Math.max(0, maxMain - occupiedMain)
        );

        int mainPos = justified.pos();
        int crossPos = getPosition(crossAxis);

        for (int i = 0; i < children.size(); i++) {
            ChildContainer container = children.get(i);
            if (container.visible()) {
                container.setPosition(axis, mainPos, mainResult);
                container.setPosition(crossAxis, crossPos, crossResult);
                mainPos = container.getPosition(axis) + container.getLength(axis) + justified.adjustSpacing(i);
            }
        }

        ScreenPosition resultSize = ScreenPosition.of(axis, mainResult, crossResult);
        this.width = resultSize.x();
        this.height = resultSize.y();
    }

    private void arrangeWrapped() {
        super.arrangeElements();

        final ScreenAxis crossAxis = axis.orthogonal();
        final int maxMain = getMaxLength(axis);
        final int maxCross = getMaxLength(crossAxis);

        if (maxMain <= 0) {
            throw new RuntimeException("cannot arrange wrap if max main is <= 0");
        }

        List<List<ChildContainer>> lines = new ArrayList<>();
        List<ChildContainer> currentLine = new ArrayList<>();
        IntArrayList lineCrossSizes = new IntArrayList();
        int currentLineMain = 0;
        int currentLineCount = 0;
        int currentLineCross = 0;
        int totalCross = 0;

        for (ChildContainer container : children) {
            if (!container.visible()) continue;

            int basisSize = container.flexMain() ? container.getMinLength(axis) : container.getLength(axis);
            if (currentLineCount > 0 && (currentLineMain + mainSpacing + basisSize > maxMain)) {
                lines.add(currentLine);
                lineCrossSizes.add(currentLineCross);
                totalCross += currentLineCross;
                currentLine = new ArrayList<>();
                currentLineMain = 0;
                currentLineCross = 0;
                currentLineCount = 0;
            }

            currentLine.add(container);
            currentLineMain += (currentLineCount > 0 ? mainSpacing : 0) + basisSize;
            currentLineCross = Math.max(currentLineCross, container.getLength(crossAxis));
            currentLineCount++;
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
            lineCrossSizes.add(currentLineCross);
            totalCross += currentLineCross;
        }

        int totalLineSpacing = lineSpacing * Math.max(0, lines.size() - 1);
        totalCross += totalLineSpacing;

        Justification.Results crossJustified = crossJustification.compute(
                getPosition(crossAxis),
                lineSpacing,
                lines.size(),
                Math.max(0, maxCross - totalCross)
        );

        int crossPos = crossJustified.pos();
        int maxOccupiedMain = 0;

        int equalLineCross = 0;
        if (crossJustification == Justification.STRETCH && totalCross < maxCross) {
            equalLineCross = (maxCross - totalLineSpacing) / lines.size();
        }

        for (int li = 0; li < lines.size(); li++) {
            List<ChildContainer> line = lines.get(li);
            int lineCross = equalLineCross > 0 ? equalLineCross : lineCrossSizes.getInt(li);

            List<ChildContainer> lineFlexMain = new ArrayList<>();
            List<ChildContainer> lineFlexCrossOnly = new ArrayList<>();
            int occupiedMain = 0;
            int visibleCount = line.size();

            for (ChildContainer container : line) {
                if (container.flexMain()) {
                    lineFlexMain.add(container);
                } else {
                    occupiedMain += container.getLength(axis);
                    if (container.flexCross()) lineFlexCrossOnly.add(container);
                }
            }

            int totalSpacing = mainSpacing * Math.max(0, visibleCount - 1);
            if (!lineFlexMain.isEmpty()) {
                int availableSpace = maxMain - occupiedMain - totalSpacing;
                int[] lengths = distributeMainAxisFlexLengths(lineFlexMain, availableSpace);
                occupiedMain += applyFlexLengths(lineFlexMain, lengths, lineCross);
            }

            for (ChildContainer container : lineFlexCrossOnly) {
                container.setLength(crossAxis, lineCross);
            }

            occupiedMain += totalSpacing;
            maxOccupiedMain = Math.max(maxOccupiedMain, occupiedMain);

            Justification.Results mainJustified = mainJustification.compute(
                    getPosition(axis),
                    mainSpacing,
                    visibleCount,
                    Math.max(0, maxMain - occupiedMain)
            );

            int mainPos = mainJustified.pos();

            for (int i = 0; i < line.size(); i++) {
                ChildContainer container = line.get(i);
                if (container.visible()) {
                    container.setPosition(axis, mainPos, container.getLength(axis));
                    container.setPosition(crossAxis, crossPos, lineCross);
                    mainPos += container.getLength(axis) + mainJustified.adjustSpacing(i);
                }
            }

            crossPos += lineCross + crossJustified.adjustSpacing(li);
        }

        int crossResult = Math.max(0, Math.max(totalCross, maxCross));
        ScreenPosition resultSize = ScreenPosition.of(axis, maxMain, crossResult);
        this.width = resultSize.x();
        this.height = resultSize.y();
    }

    @Override
    public void arrangeElements() {
        if (canWrap()) {
            arrangeWrapped();
        } else {
            arrangeLinear();
        }
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        children.forEach(container -> layoutElementVisitor.accept(container.child));
    }

    public static class Settings implements LayoutSettings {
        private final LayoutSettings layoutSettings;
        private boolean flexMain;
        private boolean flexCross;
        private int minWidth;
        private int minHeight;
        private int maxWidth;
        private int maxHeight;
        private int preferredWidth;
        private int preferredHeight;
        private TriState visible = TriState.DEFAULT;

        Settings(LayoutSettings layoutSettings) {
            this.layoutSettings = layoutSettings;
        }

        public static Settings defaults() {
            return new Settings(LayoutSettings.defaults());
        }

        public Settings flexMain(boolean flexMain) {
            this.flexMain = flexMain;
            return this;
        }

        public Settings flexCross(boolean flexCross) {
            this.flexCross = flexCross;
            return this;
        }

        public Settings flexMain() {
            return flexMain(true);
        }

        public Settings flexCross() {
            return flexCross(true);
        }

        public Settings flexBoth() {
            return flexMain().flexCross();
        }

        public Settings flexMainOnly() {
            return flexMain().flexCross(false);
        }

        public Settings flexCrossOnly() {
            return flexCross().flexMain(false);
        }

        public Settings unflex() {
            return flexMain(false).flexCross(false);
        }

        public Settings minFlexWidth(int minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        public Settings minFlexHeight(int minHeight) {
            this.minHeight = minHeight;
            return this;
        }

        public Settings maxFlexWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Settings maxFlexHeight(int maxHeight) {
            this.maxHeight = maxHeight;
            return this;
        }

        public Settings preferredFlexWidth(int preferredWidth) {
            this.preferredWidth = preferredWidth;
            return this;
        }

        public Settings preferredFlexHeight(int preferredHeight) {
            this.preferredHeight = preferredHeight;
            return this;
        }

        public Settings preferredFlexSize(int preferredWidth, int preferredHeight) {
            return preferredFlexWidth(preferredWidth).preferredFlexHeight(preferredHeight);
        }

        public Settings visible(boolean visible) {
            this.visible = TriState.from(visible);
            return this;
        }

        public Settings visible() {
            return visible(true);
        }

        public Settings invisible() {
            return visible(false);
        }

        public Settings defaultVisibility() {
            this.visible = TriState.DEFAULT;
            return this;
        }

        @Override
        public Settings padding(int padding) {
            layoutSettings.padding(padding);
            return this;
        }

        @Override
        public Settings padding(int horizontal, int vertical) {
            layoutSettings.padding(horizontal, vertical);
            return this;
        }

        @Override
        public Settings padding(int left, int top, int right, int bottom) {
            layoutSettings.padding(left, top, right, bottom);
            return this;
        }

        @Override
        public Settings paddingLeft(int padding) {
            layoutSettings.paddingLeft(padding);
            return this;
        }

        @Override
        public Settings paddingTop(int padding) {
            layoutSettings.paddingTop(padding);
            return this;
        }

        @Override
        public Settings paddingRight(int padding) {
            layoutSettings.paddingRight(padding);
            return this;
        }

        @Override
        public Settings paddingBottom(int padding) {
            layoutSettings.paddingBottom(padding);
            return this;
        }

        @Override
        public Settings paddingHorizontal(int padding) {
            layoutSettings.paddingHorizontal(padding);
            return this;
        }

        @Override
        public Settings paddingVertical(int padding) {
            layoutSettings.paddingVertical(padding);
            return this;
        }

        @Override
        public Settings align(float xAlignment, float yAlignment) {
            layoutSettings.align(xAlignment, yAlignment);
            return this;
        }

        @Override
        public Settings alignHorizontally(float xAlignment) {
            layoutSettings.alignHorizontally(xAlignment);
            return this;
        }

        @Override
        public Settings alignVertically(float yAlignment) {
            layoutSettings.alignVertically(yAlignment);
            return this;
        }

        @Override
        public Settings alignHorizontallyLeft() {
            LayoutSettings.super.alignHorizontallyLeft();
            return this;
        }

        @Override
        public Settings alignHorizontallyCenter() {
            LayoutSettings.super.alignHorizontallyCenter();
            return this;
        }

        @Override
        public Settings alignHorizontallyRight() {
            LayoutSettings.super.alignHorizontallyRight();
            return this;
        }

        @Override
        public Settings alignVerticallyTop() {
            LayoutSettings.super.alignVerticallyTop();
            return this;
        }

        @Override
        public Settings alignVerticallyMiddle() {
            LayoutSettings.super.alignVerticallyMiddle();
            return this;
        }

        @Override
        public Settings alignVerticallyBottom() {
            LayoutSettings.super.alignVerticallyBottom();
            return this;
        }

        @Override
        public Settings copy() {
            return copyFields(new Settings(layoutSettings.copy()));
        }

        private Settings copyWithLayoutSettings(LayoutSettings layoutSettings) {
            return copyFields(new Settings(layoutSettings.copy()));
        }

        private Settings copyFields(Settings copy) {
            copy.flexMain = flexMain;
            copy.flexCross = flexCross;
            copy.minWidth = minWidth;
            copy.minHeight = minHeight;
            copy.maxWidth = maxWidth;
            copy.maxHeight = maxHeight;
            copy.preferredWidth = preferredWidth;
            copy.preferredHeight = preferredHeight;
            copy.visible = visible;
            return copy;
        }

        @Override
        public LayoutSettingsImpl getExposed() {
            return layoutSettings.getExposed();
        }
    }

    private static class ChildContainer extends AbstractChildWrapper {
        protected ChildContainer(LayoutElement child, LayoutSettings layoutSettings) {
            super(child, layoutSettings);
        }

        boolean flexMain() {
            return false;
        }

        boolean flexCross() {
            return false;
        }

        boolean visible() {
            return true;
        }

        void setSize(ScreenAxis axis, int length, int otherLength) {
        }

        void setLength(ScreenAxis axis, int length) {
        }

        int getLength(ScreenAxis axis) {
            return switch (axis) {
                case HORIZONTAL -> getWidth();
                case VERTICAL -> getHeight();
            };
        }

        int getPreferredLength(ScreenAxis axis) {
            return 0;
        }

        int getMaxLength(ScreenAxis axis) {
            return getLength(axis);
        }

        int getMinLength(ScreenAxis axis) {
            return getLength(axis);
        }

        void setPosition(ScreenAxis axis, int position, int availableSpace) {
            switch (axis) {
                case HORIZONTAL -> setX(position, availableSpace);
                case VERTICAL -> setY(position, availableSpace);
            }
        }

        int getPosition(ScreenAxis axis) {
            return switch (axis) {
                case HORIZONTAL -> child.getX();
                case VERTICAL -> child.getY();
            };
        }
    }

    private static final class FlexChildContainer extends ChildContainer {
        private final Settings flexSettings;
        private final BooleanSupplier visibilityGetter;
        private final IntIntBiConsumer sizeSetter;

        FlexChildContainer(LayoutElement child, Settings flexSettings, BooleanSupplier visibilityGetter, IntIntBiConsumer sizeSetter) {
            super(child, flexSettings.layoutSettings);
            this.visibilityGetter = visibilityGetter;
            this.sizeSetter = sizeSetter;
            this.flexSettings = flexSettings;
        }

        FlexChildContainer(FZFlexElement child, Settings flexSettings) {
            this(child, flexSettings, child::fidgetz$isVisible, child::fidgetz$setSize);
        }

        FlexChildContainer(AbstractWidget child, Settings flexSettings) {
            this(child, flexSettings, () -> child.visible,
                    (width, height) -> io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils.setSize(child, width, height));
        }

        @Override
        boolean flexMain() {
            return flexSettings.flexMain;
        }

        @Override
        boolean flexCross() {
            return flexSettings.flexCross;
        }

        @Override
        boolean visible() {
            return flexSettings.visible.toBoolean(visibilityGetter.getAsBoolean());
        }

        @Override
        void setLength(ScreenAxis axis, int length) {
            switch (axis) {
                case HORIZONTAL -> sizeSetter.accept(length, child.getHeight());
                case VERTICAL -> sizeSetter.accept(child.getWidth(), length);
            }
        }

        @Override
        void setSize(ScreenAxis axis, int length, int otherLength) {
            ScreenPosition position = ScreenPosition.of(axis, length, otherLength);
            sizeSetter.accept(position.x(), position.y());
        }

        @Override
        int getMaxLength(ScreenAxis axis) {
            return getAxisValue(axis, flexSettings.maxWidth, flexSettings.maxHeight);
        }

        @Override
        int getMinLength(ScreenAxis axis) {
            return getAxisValue(axis, flexSettings.minWidth, flexSettings.minHeight);
        }

        @Override
        int getPreferredLength(ScreenAxis axis) {
            return getAxisValue(axis, flexSettings.preferredWidth, flexSettings.preferredHeight);
        }
    }
}
