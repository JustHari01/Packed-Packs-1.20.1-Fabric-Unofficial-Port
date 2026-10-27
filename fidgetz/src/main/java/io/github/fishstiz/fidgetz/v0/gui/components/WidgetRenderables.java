package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import net.minecraft.resources.ResourceLocation;

public record WidgetRenderables(
        RenderableRectangle enabled,
        RenderableRectangle disabled,
        RenderableRectangle enabledFocused,
        RenderableRectangle disabledFocused
) {
    public WidgetRenderables(RenderableRectangle sprite) {
        this(sprite, sprite, sprite, sprite);
    }

    public WidgetRenderables(RenderableRectangle sprite, RenderableRectangle focused) {
        this(sprite, sprite, focused, focused);
    }

    public WidgetRenderables(RenderableRectangle enabled, RenderableRectangle disabled, RenderableRectangle focused) {
        this(enabled, disabled, focused, disabled);
    }

    public WidgetRenderables withEnabled(RenderableRectangle enabled) {
        return new WidgetRenderables(enabled, disabled, enabled, disabledFocused);
    }

    public WidgetRenderables withDisabled(RenderableRectangle disabled) {
        return new WidgetRenderables(enabled, disabled, enabledFocused, disabled);
    }

    public WidgetRenderables withEnabledFocused(RenderableRectangle enabledFocused) {
        return new WidgetRenderables(enabled, disabled, enabledFocused, enabled);
    }

    public WidgetRenderables withDisabledFocused(RenderableRectangle disabledFocused) {
        return new WidgetRenderables(enabled, disabled, enabledFocused, disabledFocused);
    }

    public RenderableRectangle get(boolean enabled, boolean focused) {
        if (enabled) {
            return focused ? this.enabledFocused : this.enabled;
        } else {
            return focused ? this.disabledFocused : this.disabled;
        }
    }

    public static WidgetRenderables noFocus(RenderableRectangle enabled, RenderableRectangle disabled) {
        return new WidgetRenderables(enabled, disabled, enabled, disabled);
    }

    public static WidgetRenderables noFocus(ResourceLocation enabled, ResourceLocation disabled) {
        RenderableRectangle enabledSprite = Renderables.sprite(enabled);
        RenderableRectangle disabledSprite = Renderables.sprite(disabled);
        return noFocus(enabledSprite, disabledSprite);
    }

    public static WidgetRenderables sprites(ResourceLocation enabled, ResourceLocation disabled, ResourceLocation enabledFocused) {
        return new WidgetRenderables(
                Renderables.sprite(enabled),
                Renderables.sprite(disabled),
                Renderables.sprite(enabledFocused),
                Renderables.sprite(disabled)
        );
    }
}
