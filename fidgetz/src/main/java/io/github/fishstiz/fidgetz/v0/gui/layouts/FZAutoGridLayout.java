package io.github.fishstiz.fidgetz.v0.gui.layouts;

import io.github.fishstiz.fidgetz.v0.utils.MathUtils;
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

public class FZAutoGridLayout extends AbstractLayout implements FZLayout {
    private final Supplier<ScreenRectangle> screenArea;
    private final LayoutSettings defaultChildSettings = LayoutSettings.defaults();
    private final List<ChildContainer> children = new ArrayList<>();
    private final ScreenAxis axis;
    private int minCellWidth;
    private int minCellHeight;
    private int maxCellWidth;
    private int maxCellHeight;
    private int mainSpacing;
    private int crossSpacing;
    private int maxWidth;
    private int maxHeight;
    private Justification mainJustification = Justification.START;
    private Justification crossJustification = Justification.START;

    public FZAutoGridLayout(Supplier<ScreenRectangle> screenArea, ScreenAxis axis) {
        super(0, 0, screenArea.get().width(), screenArea.get().height());
        this.screenArea = screenArea;
        this.axis = axis;
    }

    static FZAutoGridLayout auto(Supplier<ScreenRectangle> screenArea, ScreenAxis axis) {
        return new FZAutoGridLayout(screenArea, axis);
    }

    public static FZAutoGridLayout horizontal() {
        return auto(ScreenRectangle::empty, ScreenAxis.HORIZONTAL);
    }

    public static FZAutoGridLayout horizontal(Screen screen) {
        return new FZAutoGridLayout(screen::getRectangle, ScreenAxis.HORIZONTAL);
    }

    public static FZAutoGridLayout horizontal(LayoutElement container) {
        return new FZAutoGridLayout(container::getRectangle, ScreenAxis.HORIZONTAL);
    }

    public static FZAutoGridLayout vertical() {
        return auto(ScreenRectangle::empty, ScreenAxis.VERTICAL);
    }

    public static FZAutoGridLayout vertical(Screen screen) {
        return new FZAutoGridLayout(screen::getRectangle, ScreenAxis.VERTICAL);
    }

    public static FZAutoGridLayout vertical(LayoutElement container) {
        return new FZAutoGridLayout(container::getRectangle, ScreenAxis.VERTICAL);
    }

    private static <T> T getAxisValue(ScreenAxis axis, T horizontalValue, T verticalValue) {
        return switch (axis) {
            case HORIZONTAL -> horizontalValue;
            case VERTICAL -> verticalValue;
        };
    }

    private <T> T getMainValue(T horizontalValue, T verticalValue) {
        return getAxisValue(axis, horizontalValue, verticalValue);
    }

    private <T> T getCrossValue(T horizontalValue, T verticalValue) {
        return getAxisValue(axis.orthogonal(), horizontalValue, verticalValue);
    }

    public FZAutoGridLayout maxWidth(int maxWidth) {
        this.maxWidth = MathUtils.optionalMin(maxWidth, screenArea.get().width());
        return this;
    }

    public FZAutoGridLayout maxHeight(int maxHeight) {
        this.maxHeight = MathUtils.optionalMin(maxHeight, screenArea.get().height());
        return this;
    }

    public FZAutoGridLayout minCellWidth(int minCellWidth) {
        this.minCellWidth = minCellWidth;
        return this;
    }

    public FZAutoGridLayout minCellHeight(int minCellHeight) {
        this.minCellHeight = minCellHeight;
        return this;
    }

    public FZAutoGridLayout maxCellWidth(int maxCellWidth) {
        this.maxCellWidth = maxCellWidth;
        return this;
    }

    public FZAutoGridLayout maxCellHeight(int maxCellHeight) {
        this.maxCellHeight = maxCellHeight;
        return this;
    }

    public FZAutoGridLayout cellWidth(int cellWidth) {
        return maxCellWidth(cellWidth).minCellWidth(cellWidth);
    }

    public FZAutoGridLayout cellHeight(int cellHeight) {
        return maxCellHeight(cellHeight).minCellHeight(cellHeight);
    }

    public FZAutoGridLayout rowSpacing(int spacing) {
        switch (axis) {
            case VERTICAL -> this.mainSpacing = spacing;
            case HORIZONTAL -> this.crossSpacing = spacing;
        };
        return this;
    }

    public FZAutoGridLayout colSpacing(int spacing) {
        switch (axis) {
            case HORIZONTAL -> this.mainSpacing = spacing;
            case VERTICAL -> this.crossSpacing = spacing;
        }
        return this;
    }

    public FZAutoGridLayout spacing(int spacing) {
        return rowSpacing(spacing).colSpacing(spacing);
    }

    public FZAutoGridLayout justifyContents(Justification justification) {
        this.mainJustification = justification;
        return this;
    }

    public FZAutoGridLayout alignContents(Justification justification) {
        this.crossJustification = justification;
        return this;
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
        maxWidth(width).maxHeight(height);
        if (previousMaxWidth != maxWidth || previousMaxHeight != maxHeight) {
            arrangeElements();
        }
    }

    private int getMaxLength(ScreenAxis axis) {
        return switch (axis) {
            case HORIZONTAL -> MathUtils.eitherOptionalMin(maxWidth, screenArea.get().width());
            case VERTICAL -> MathUtils.eitherOptionalMin(maxHeight, screenArea.get().height());
        };
    }

    public <T extends AbstractWidget> T child(T child, LayoutSettings settings) {
        children.add(new ChildContainer(child, settings, () -> child.visible));
        return child;
    }

    public <T extends AbstractWidget> T child(T child) {
        return child(child, defaultChildSettings);
    }

    public <T extends FZFlexElement> T child(T child, LayoutSettings settings) {
        children.add(new ChildContainer(child, settings, child::fidgetz$isVisible));
        return child;
    }

    public <T extends FZFlexElement> T child(T child) {
        return child(child, defaultChildSettings);
    }

    public <T extends LayoutElement> T child(T child, LayoutSettings settings) {
        children.add(new ChildContainer(child, settings, () -> true));
        return child;
    }

    public <T extends LayoutElement> T child(T child) {
        return child(child, defaultChildSettings);
    }

    @Override
    public void arrangeElements() {
        super.arrangeElements();

        final ScreenAxis crossAxis = axis.orthogonal();
        int maxMain = getMaxLength(axis);
        int maxCross = getMaxLength(crossAxis);

        int mainMaxCell = getAxisValue(axis, this.maxCellWidth, this.maxCellHeight);
        int crossMaxCell = getAxisValue(crossAxis, this.maxCellWidth, this.maxCellHeight);

        int mainCellSize = MathUtils.optionalMin(getAxisValue(axis, this.minCellWidth, this.minCellHeight), mainMaxCell);
        int crossCellSize = MathUtils.optionalMin(getAxisValue(crossAxis, this.minCellWidth, this.minCellHeight), crossMaxCell);

        List<ChildContainer> visibleChildren = new ArrayList<>(children.size());

        for (ChildContainer container : children) {
            if (container.visible()) {
                visibleChildren.add(container);
                mainCellSize = MathUtils.clampOptionalMax(container.getLength(axis), mainCellSize, mainMaxCell);
                crossCellSize = MathUtils.clampOptionalMax(container.getLength(crossAxis), crossCellSize, crossMaxCell);
            }
        }

        if (visibleChildren.isEmpty() || (mainCellSize == 0 && crossCellSize == 0)) {
            this.width = 0;
            this.height = 0;
            return;
        }

        if (maxMain == 0) {
            maxMain = visibleChildren.size() * mainCellSize + Math.max(0, visibleChildren.size() - 1) * this.mainSpacing;
        }

        int mainCount = (maxMain + this.mainSpacing) / (mainCellSize + this.mainSpacing);
        int crossCount = (int) Math.ceil((double) visibleChildren.size() / mainCount);

        Justification.Results mainJustified = this.mainJustification.compute(
                getAxisValue(axis, getX(), getY()),
                this.mainSpacing,
                mainCount,
                maxMain - (mainCount * (mainCellSize + this.mainSpacing) - this.mainSpacing)
        );

        int occupiedCross = crossCount * (crossCellSize + this.crossSpacing) - this.crossSpacing;

        Justification.Results crossJustified = this.crossJustification.compute(
                getAxisValue(crossAxis, getX(), getY()),
                this.crossSpacing,
                crossCount,
                Math.max(0, maxCross - occupiedCross)
        );

        int[] mainOffsets = new int[mainCount];
        int mainPos = mainJustified.pos();
        for (int i = 0; i < mainCount; i++) {
            mainOffsets[i] = mainPos;
            mainPos += mainCellSize + mainJustified.adjustSpacing(i);
        }

        int[] crossOffsets = new int[crossCount];
        int crossPos = crossJustified.pos();
        for (int i = 0; i < crossCount; i++) {
            crossOffsets[i] = crossPos;
            crossPos += crossCellSize + crossJustified.adjustSpacing(i);
        }

        for (int i = 0; i < visibleChildren.size(); i++) {
            ChildContainer container = visibleChildren.get(i);
            int mainIndex = i % mainCount;
            int crossIndex = i / mainCount;

            container.setPosition(axis, mainOffsets[mainIndex], mainCellSize);
            container.setPosition(crossAxis, crossOffsets[crossIndex], crossCellSize);
        }

        if (maxCross == 0) {
            maxCross = Math.max(0, occupiedCross);
        }

        ScreenPosition size = ScreenPosition.of(axis, maxMain, maxCross);
        this.width = size.x();
        this.height = size.y();
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        this.children.forEach(container -> layoutElementVisitor.accept(container.child));
    }

    public void removeChildren() {
        this.children.clear();
    }

    private static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
        private final BooleanSupplier visibleSupplier;

        protected ChildContainer(
                LayoutElement child,
                LayoutSettings layoutSettings,
                BooleanSupplier visibleSupplier
        ) {
            super(child, layoutSettings);
            this.visibleSupplier = visibleSupplier;
        }

        boolean visible() {
            return visibleSupplier.getAsBoolean();
        }

        private void setPosition(ScreenAxis axis, int position, int availableSpace) {
            switch (axis) {
                case HORIZONTAL -> setX(position, availableSpace);
                case VERTICAL -> setY(position, availableSpace);
            }
        }

        private int getLength(ScreenAxis axis) {
            return switch (axis) {
                case HORIZONTAL -> getWidth();
                case VERTICAL -> getHeight();
            };
        }

        private int getPosition(ScreenAxis axis) {
            return switch (axis) {
                case HORIZONTAL -> child.getX();
                case VERTICAL -> child.getY();
            };
        }
    }
}