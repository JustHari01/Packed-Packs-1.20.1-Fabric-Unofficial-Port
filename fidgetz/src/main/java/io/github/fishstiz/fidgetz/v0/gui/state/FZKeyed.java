package io.github.fishstiz.fidgetz.v0.gui.state;

import java.util.Objects;

public record FZKeyed<T>(Object key, T value) {
    public static <T> FZKeyed<T> selfKey(T value) {
        return new FZKeyed<>(value, value);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FZKeyed<?> keyed) {
            return Objects.equals(key, keyed.key);
        }
        return false;
    }
}
