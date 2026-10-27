package io.github.fishstiz.fidgetz.v0.utils;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Undefinable<T extends @Nullable Object> {
    private static final Undefinable<?> UNDEFINED = new Undefinable<>(null, false);
    private final T value;
    private final boolean defined;

    private Undefinable(T value, boolean defined) {
        this.value = value;
        this.defined = defined;
    }

    @SuppressWarnings("unchecked")
    public static <T> Undefinable<@Nullable T> undefined() {
        return (Undefinable<T>) UNDEFINED;
    }

    public static <T extends @Nullable Object> Undefinable<T> of(T value) {
        return new Undefinable<>(value, true);
    }

    public boolean isDefined() {
        return defined;
    }

    public boolean isUndefined() {
        return !defined;
    }

    public T get() {
        return value;
    }

    public void ifDefined(Consumer<? super @Nullable T> action) {
        if (defined) action.accept(value);
    }

    public T orElse(T defaultValue) {
        if (!defined) return defaultValue;
        return value;
    }

    public <R> Undefinable<@Nullable R> map(Function<? super T, ? extends R> mapper) {
        if (!defined) return undefined();
        return Undefinable.of(mapper.apply(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Undefinable<?> other)) return false;
        if (!defined && !other.defined) return true;
        if (defined != other.defined) return false;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return defined ? Objects.hashCode(value) : 0;
    }

    @Override
    public String toString() {
        return defined ? "Undefinable.of(" + value + ")" : "Undefinable.undefined()";
    }
}