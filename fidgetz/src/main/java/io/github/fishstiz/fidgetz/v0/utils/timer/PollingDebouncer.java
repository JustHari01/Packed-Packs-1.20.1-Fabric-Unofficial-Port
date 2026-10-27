package io.github.fishstiz.fidgetz.v0.utils.timer;

import java.util.function.Consumer;

public abstract class PollingDebouncer<T> extends Debouncer<T> {
    protected PollingDebouncer(Consumer<T> task, long delay) {
        super(task, delay);
    }

    protected PollingDebouncer(Runnable task, long delay) {
        super(task, delay);
    }

    public abstract void abort();

    public abstract void poll();
}
