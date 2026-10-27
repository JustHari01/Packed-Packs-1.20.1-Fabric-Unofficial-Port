package io.github.fishstiz.fidgetz.v0.inject.mixins.access;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface AbstractWidgetAccess {
    @Accessor("height")
    void fidgetz$setHeight(int height);
}
