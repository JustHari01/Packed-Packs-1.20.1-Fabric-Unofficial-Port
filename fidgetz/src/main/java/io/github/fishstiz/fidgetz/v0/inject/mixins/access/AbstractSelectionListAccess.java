package io.github.fishstiz.fidgetz.v0.inject.mixins.access;

import io.github.fishstiz.fidgetz.v0.inject.interfaces.ScrollableSelectionList;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccess extends ScrollableSelectionList {
    @Accessor("scrolling")
    boolean fidgetz$getScrolling();
}
