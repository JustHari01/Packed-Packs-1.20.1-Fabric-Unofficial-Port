package io.github.fishstiz.fidgetz.v0.inject.interfaces;

public interface AbstractSliderButtonAccess {
    default boolean fidgetz$canChangeValue() {
        return false;
    }

    default void fidgetz$setCanChangeValue(boolean canChangeValue) {
    }

    default void fidgetz$setValue(double value) {
    }
}
