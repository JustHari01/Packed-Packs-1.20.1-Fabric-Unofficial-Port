package io.github.fishstiz.fidgetz.v0.inject.mixins.access;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EditBox.class)
public interface EditBoxAccess {
    @Accessor("highlightPos")
    int fidgetz$getHighlightPos();

    @Invoker("deleteText")
    void fidgetz$invokeDeleteText(int amount);
}
