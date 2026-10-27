package io.github.fishstiz.fidgetz.v0.utils;

import org.jetbrains.annotations.Nullable;

import java.util.function.*;

public final class FunctionUtils {
    private static final Runnable NO_OP = () -> {
    };
    private static final Consumer<Object> NO_OP_CONSUMER = ignored -> {
    };
    private static final Supplier<@Nullable Object> NULL_SUPPLIER = () -> null;
    private static final Function<Object, @Nullable Object> NULL_FUNCTION = ignored -> null;

    public static Runnable nop() {
        return NO_OP;
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> nopConsumer() {
        return (Consumer<T>) NO_OP_CONSUMER;
    }

    @SuppressWarnings("unchecked")
    public static <T> Supplier<@Nullable T> nullSupplier() {
        return (Supplier<T>) NULL_SUPPLIER;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Function<T, @Nullable R> nullFunction() {
        return (Function<T, R>) NULL_FUNCTION;
    }

    private FunctionUtils() {
    }
}
