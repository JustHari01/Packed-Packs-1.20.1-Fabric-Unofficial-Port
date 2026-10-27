package io.github.fishstiz.fidgetz.v0.gui.layouts;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.function.Consumer;

public final class FZFlexSpacerElement implements FZFlexElement {
    private int x;
    private int y;
    private int width;
    private int height;

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
    }

    @Override
    public void fidgetz$setWidth(int width) {
        this.width = width;
    }

    @Override
    public void fidgetz$setHeight(int height) {
        this.height = height;
    }

    public FZFlexSpacerElement width(int width) {
        fidgetz$setWidth(width);
        return this;
    }

    public FZFlexSpacerElement height(int height) {
        fidgetz$setHeight(height);
        return this;
    }

    public FZFlexSpacerElement size(int width, int height) {
        return width(width).height(height);
    }
}
