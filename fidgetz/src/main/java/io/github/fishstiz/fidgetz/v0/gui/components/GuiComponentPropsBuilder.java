package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.utils.TriState;
import io.github.fishstiz.fidgetz.v0.utils.Undefinable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public abstract class GuiComponentPropsBuilder<T> {
    protected final GuiComponentPropsBase props = GuiComponentPropsBase.defaults();

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T) this;
    }

    public T id(String id) {
        props.id = id;
        return self();
    }

    public T width(int width) {
        props.width = width;
        return self();
    }

    public T height(int height) {
        props.height = height;
        return self();
    }

    public T size(int width, int height) {
        props.width = width;
        props.height = height;
        return self();
    }

    public T active(boolean active) {
        props.active = TriState.from(active);
        return self();
    }

    public T active() {
        return active(true);
    }

    public T inactive() {
        return active(false);
    }

    public T visible(boolean visible) {
        props.visible = TriState.from(visible);
        return self();
    }

    public T visible() {
        return visible(true);
    }

    public T focusOnInteraction(boolean focusOnInteraction) {
        props.focusOnInteraction = TriState.from(focusOnInteraction);
        return self();
    }

    public T focusOnInteraction() {
        return focusOnInteraction(true);
    }

    public T message(@Nullable Component message) {
        props.message = message;
        return self();
    }

    public T tooltip(@Nullable Tooltip tooltip) {
        props.tooltip = Undefinable.of(tooltip);
        return self();
    }

    public T tooltip(@Nullable Component tooltip) {
        props.tooltip = Undefinable.of(tooltip == null ? null : Tooltip.create(tooltip));
        return self();
    }

    public T overlay(@Nullable RenderableRectangle overlay) {
        props.overlay = Undefinable.of(overlay);
        return self();
    }

    public T contextEntries(FZPopoverMenuItem entry, FZPopoverMenuItem... rest) {
        Objects.requireNonNull(entry, "entry cannot be null");
        props.contextEntries = FZKeyed.selfKey(collector -> {
            collector.addEntry(entry);
            for (FZPopoverMenuItem e : rest) {
                collector.addEntry(Objects.requireNonNull(e, "entry cannot be null"));
            }
        });
        return self();
    }

    public T contextEntries(Consumer<FZContextMenu.Collector> contextSupplier) {
        props.contextEntries = FZKeyed.selfKey(Objects.requireNonNull(contextSupplier, "contextSupplier cannot be null"));
        return self();
    }

    public T tabOrderGroup(int tabOrderGroup) {
        props.tabOrderGroup = tabOrderGroup;
        return self();
    }
}
