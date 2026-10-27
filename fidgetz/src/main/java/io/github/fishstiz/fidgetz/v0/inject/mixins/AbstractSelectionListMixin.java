package io.github.fishstiz.fidgetz.v0.inject.mixins;

import io.github.fishstiz.fidgetz.v0.inject.interfaces.ScrollableSelectionList;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractSelectionList.class)
abstract class AbstractSelectionListMixin implements ScrollableSelectionList {
    @Shadow
    @Final
    @Mutable
    protected int itemHeight;

    @Override
    public void fidgetz$setScrollRate(double scrollRate) {
        this.itemHeight = (int) scrollRate * 2;
    }
}
