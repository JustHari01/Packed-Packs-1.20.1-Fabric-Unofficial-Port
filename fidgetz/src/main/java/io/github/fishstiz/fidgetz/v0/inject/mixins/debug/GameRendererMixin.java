package io.github.fishstiz.fidgetz.v0.inject.mixins.debug;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.fishstiz.fidgetz.v0.gui.debug.FZDebugOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings({"unused", "UnusedMixin"})
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;flush()V"
    ))
    public void extractTestmodRenderState(
            CallbackInfo ci,
            @Local(ordinal = 0) GuiGraphics graphics
    ) {
        FZDebugOverlay.extractRenderState(graphics);
    }
}
