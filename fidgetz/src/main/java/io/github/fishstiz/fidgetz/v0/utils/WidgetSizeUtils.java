package io.github.fishstiz.fidgetz.v0.utils;

import io.github.fishstiz.fidgetz.v0.inject.mixins.access.AbstractWidgetAccess;
import net.minecraft.client.gui.components.AbstractWidget;

public final class WidgetSizeUtils {
    public static void setHeight(AbstractWidget widget, int height) {
        ((AbstractWidgetAccess) (Object) widget).fidgetz$setHeight(height);
    }

    public static void setSize(AbstractWidget widget, int width, int height) {
        widget.setWidth(width);
        setHeight(widget, height);
    }

    public static int right(AbstractWidget widget) {
        return widget.getX() + widget.getWidth();
    }

    public static int bottom(AbstractWidget widget) {
        return widget.getY() + widget.getHeight();
    }

    private WidgetSizeUtils() {
    }
}
