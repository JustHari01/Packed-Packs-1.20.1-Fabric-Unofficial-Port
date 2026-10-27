package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.utils.TriState;
import io.github.fishstiz.fidgetz.v0.utils.Undefinable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

public interface GuiComponentProps {
    default Optional<String> id() {
        return Optional.empty();
    }

    default OptionalInt width() {
        return OptionalInt.empty();
    }

    default OptionalInt height() {
        return OptionalInt.empty();
    }

    default TriState active() {
        return TriState.DEFAULT;
    }

    default TriState visible() {
        return TriState.DEFAULT;
    }

    default TriState focusOnInteraction() {
        return TriState.DEFAULT;
    }

    default Optional<Component> message() {
        return Optional.empty();
    }

    default Undefinable<@Nullable Tooltip> tooltip() {
        return Undefinable.undefined();
    }

    default Undefinable<@Nullable RenderableRectangle> overlay() {
        return Undefinable.undefined();
    }

    default Optional<FZKeyed<Consumer<FZContextMenu.Collector>>> contextEntries() {
        return Optional.empty();
    }

    default OptionalInt tabOrderGroup() {
        return OptionalInt.empty();
    }
}