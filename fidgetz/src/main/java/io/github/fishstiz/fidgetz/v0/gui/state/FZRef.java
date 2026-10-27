package io.github.fishstiz.fidgetz.v0.gui.state;

import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface FZRef<T> {
    T value();

    <R> Runnable subscribe(String key, Function<T, R> selector, Consumer<R> callback);

    default <R> Runnable subscribe(String key, Function<T, R> selector, Runnable callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        return subscribe(key, selector, ignored -> callback.run());
    }

    default Runnable subscribe(String key, Consumer<T> callback) {
        return subscribe(key, Function.identity(), callback);
    }

    default Runnable subscribe(String key, Runnable callback) {
        Objects.requireNonNull(callback, "callback cannot be null");
        return subscribe(key, ignored -> callback.run());
    }

    default <R> R bind(String key, R object, BiConsumer<T, R> effect) {
        effect.accept(value(), object);
        subscribe(key, s -> effect.accept(s, object));
        return object;
    }

    default <R> FZRef<R> map(Function<T, R> selector) {
        Objects.requireNonNull(selector, "selector cannot be null");
        FZRef<T> parent = this;
        return new FZRef<>() {
            @Override
            public R value() {
                return selector.apply(parent.value());
            }

            @Override
            public <S> Runnable subscribe(String key, Function<R, S> innerSelector, Consumer<S> callback) {
                MutableObject<S> last = new MutableObject<>(innerSelector.apply(selector.apply(parent.value())));
                return parent.subscribe(key, t -> {
                    S next = innerSelector.apply(selector.apply(t));
                    if (!Objects.equals(next, last.getValue())) {
                        last.setValue(next);
                        callback.accept(next);
                    }
                });
            }
        };
    }
}
