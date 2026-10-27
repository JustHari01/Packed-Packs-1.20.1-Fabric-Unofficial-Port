package io.github.fishstiz.fidgetz.v0.gui.components;

import net.minecraft.client.gui.navigation.ScreenRectangle;

public enum HorizontalDirection {
    LEFT {
        @Override
        HorizontalDirection resolve(ScreenRectangle container, int width, int anchor) {
            return anchor - width * FLIP_THRESHOLD < 0 ? flip() : this;
        }

        @Override
        int clamp(ScreenRectangle container, int width, int anchor) {
            return Math.max(0, anchor - width);
        }

        @Override
        int edge(ScreenRectangle bounds) {
            return bounds.left();
        }

        @Override
        HorizontalDirection flip() {
            return RIGHT;
        }
    },
    RIGHT {
        @Override
        HorizontalDirection resolve(ScreenRectangle container, int width, int anchor) {
            return anchor + width * FLIP_THRESHOLD > container.width() ? flip() : this;
        }

        @Override
        int clamp(ScreenRectangle container, int width, int anchor) {
            return Math.max(container.left(), anchor + width > container.right() ? container.width() - width : anchor);
        }

        @Override
        int edge(ScreenRectangle bounds) {
            return bounds.right();
        }

        @Override
        HorizontalDirection flip() {
            return LEFT;
        }
    };

    private static final float FLIP_THRESHOLD = 0.75f;

    abstract HorizontalDirection resolve(ScreenRectangle container, int width, int anchor);

    abstract int clamp(ScreenRectangle container, int width, int anchor);

    abstract int edge(ScreenRectangle bounds);

    abstract HorizontalDirection flip();
}
