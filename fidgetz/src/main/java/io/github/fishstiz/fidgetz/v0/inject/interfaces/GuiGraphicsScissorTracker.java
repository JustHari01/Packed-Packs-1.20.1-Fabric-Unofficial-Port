package io.github.fishstiz.fidgetz.v0.inject.interfaces;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

public interface GuiGraphicsScissorTracker {
    @Nullable ScreenRectangle fidgetz$getCurrentScissor();
}
