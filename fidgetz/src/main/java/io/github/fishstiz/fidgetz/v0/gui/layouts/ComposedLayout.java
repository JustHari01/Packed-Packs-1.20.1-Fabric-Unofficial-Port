package io.github.fishstiz.fidgetz.v0.gui.layouts;

import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import java.util.function.Consumer;

abstract class ComposedLayout implements FZLayout {
    protected Layout composed;

    ComposedLayout(Layout composed) {
        this.composed = composed;
    }

    @Override
    public void fidgetz$setWidth(int width) {
        if (composed instanceof FZFlexElement flexElement) {
            flexElement.fidgetz$setWidth(width);
        }
    }

    @Override
    public void fidgetz$setHeight(int height) {
        if (composed instanceof FZFlexElement flexElement) {
            flexElement.fidgetz$setHeight(height);
        }
    }

    @Override
    public void fidgetz$setSize(int width, int height) {
        if (composed instanceof FZFlexElement flexElement) {
            flexElement.fidgetz$setSize(width, height);
        }
    }

    @Override
    public boolean fidgetz$isVisible() {
        if (composed instanceof FZFlexElement flexElement) {
            return flexElement.fidgetz$isVisible();
        }
        return true;
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> layoutElementVisitor) {
        composed.visitChildren(layoutElementVisitor);
    }

    @Override
    public void setX(int x) {
        composed.setX(x);
    }

    @Override
    public void setY(int y) {
        composed.setY(y);
    }

    @Override
    public int getX() {
        return composed.getX();
    }

    @Override
    public int getY() {
        return composed.getY();
    }

    @Override
    public int getWidth() {
        return composed.getWidth();
    }

    @Override
    public int getHeight() {
        return composed.getHeight();
    }

    @Override
    public ScreenRectangle getRectangle() {
        return composed.getRectangle();
    }

    @Override
    public abstract void arrangeElements();
}
