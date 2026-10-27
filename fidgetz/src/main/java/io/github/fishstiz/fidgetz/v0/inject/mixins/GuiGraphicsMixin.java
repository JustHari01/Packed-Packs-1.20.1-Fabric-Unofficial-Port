package io.github.fishstiz.fidgetz.v0.inject.mixins;

import io.github.fishstiz.fidgetz.v0.inject.interfaces.GuiGraphicsScissorTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

@Mixin(GuiGraphics.class)
abstract class GuiGraphicsMixin implements GuiGraphicsScissorTracker {
    @Unique
    private final Deque<ScreenRectangle> fidgetz$scissors = new ArrayDeque<>();

    @Inject(method = "enableScissor(IIII)V", at = @At("HEAD"))
    private void fidgetz$trackScissor(int left, int top, int right, int bottom, CallbackInfo callbackInfo) {
        ScreenRectangle rectangle = new ScreenRectangle(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
        @Nullable ScreenRectangle parent = fidgetz$scissors.peek();
        if (parent != null) {
            int clippedLeft = Math.max(rectangle.left(), parent.left());
            int clippedTop = Math.max(rectangle.top(), parent.top());
            int clippedRight = Math.min(rectangle.right(), parent.right());
            int clippedBottom = Math.min(rectangle.bottom(), parent.bottom());
            rectangle = new ScreenRectangle(
                    clippedLeft,
                    clippedTop,
                    Math.max(0, clippedRight - clippedLeft),
                    Math.max(0, clippedBottom - clippedTop)
            );
        }
        fidgetz$scissors.push(rectangle);
    }

    @Inject(method = "disableScissor()V", at = @At("HEAD"))
    private void fidgetz$untrackScissor(CallbackInfo callbackInfo) {
        if (!fidgetz$scissors.isEmpty()) {
            fidgetz$scissors.pop();
        }
    }

    @Override
    public @Nullable ScreenRectangle fidgetz$getCurrentScissor() {
        return fidgetz$scissors.peek();
    }
}
