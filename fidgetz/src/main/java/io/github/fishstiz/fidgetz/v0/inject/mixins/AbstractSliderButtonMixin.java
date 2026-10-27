package io.github.fishstiz.fidgetz.v0.inject.mixins;

import io.github.fishstiz.fidgetz.v0.inject.interfaces.AbstractSliderButtonAccess;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractSliderButton.class)
abstract class AbstractSliderButtonMixin implements AbstractSliderButtonAccess {
    @Shadow
    private boolean canChangeValue;

    @Shadow
    protected abstract void setValue(double value);

    @Override
    public boolean fidgetz$canChangeValue() {
        return canChangeValue;
    }

    @Override
    public void fidgetz$setCanChangeValue(boolean canChangeValue) {
        this.canChangeValue = canChangeValue;
    }

    @Override
    public void fidgetz$setValue(double value) {
        setValue(value);
    }
}
