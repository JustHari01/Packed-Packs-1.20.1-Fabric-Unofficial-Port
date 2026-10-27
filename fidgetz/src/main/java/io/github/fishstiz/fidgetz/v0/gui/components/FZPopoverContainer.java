package io.github.fishstiz.fidgetz.v0.gui.components;

import java.util.function.Consumer;

public interface FZPopoverContainer {
    void fidgetz$visitPopovers(Consumer<FZPopover> visitor);
}
