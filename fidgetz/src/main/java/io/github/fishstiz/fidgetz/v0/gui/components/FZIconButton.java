package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.utils.Undefinable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class FZIconButton extends FZButtonBase {
    private @Nullable WidgetRenderables background;
    private @Nullable WidgetElements icon;

    private FZIconButton() {
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        if (background != null) {
            background.get(isActive(), isHoveredOrFocused())
                    .extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, a);
        }
        if (icon != null) {
            int x = (getX() + getWidth() / 2 - icon.width() / 2) + icon.margin().left() - icon.margin().right();
            int y = (getY() + getHeight() / 2 - icon.height() / 2) + icon.margin().top() - icon.margin().bottom();
            icon.elements()
                    .get(isActive(), isHoveredOrFocused())
                    .extractRenderState(graphics, x, y, icon.width(), icon.height(), mouseX, mouseY, a);
        }

        extractOverlay(graphics, mouseX, mouseY, a);
    }

    private void applyProps(Props props) {
        super.applyProps(props);
        props.background().ifDefined(background -> this.background = background);
        props.icon().ifDefined(icon -> this.icon = icon);
    }

    public static FZIconButton bind(String key, FZRef<Props> ref) {
        Props props = ref.value();
        FZIconButton button = new FZIconButton();
        button.applyProps(props);
        ref.subscribe(key, button::applyProps);
        return button;
    }

    public static Builder builder() {
        return new Builder(DEFAULT_RENDERABLES);
    }

    public static Builder builder(WidgetRenderables renderables) {
        return new Builder(renderables);
    }

    public static Builder builder(RenderableRectangle renderable) {
        return new Builder(new WidgetRenderables(renderable));
    }

    public static Builder builder(ResourceLocation sprite) {
        return new Builder(new WidgetRenderables(Renderables.sprite(sprite)));
    }

    public interface Props extends FZButtonBase.Props {
        default Undefinable<@Nullable WidgetRenderables> background() {
            return Undefinable.undefined();
        }

        default Undefinable<@Nullable WidgetElements> icon() {
            return Undefinable.undefined();
        }
    }

    private static final class PropsImpl extends FZButtonBase.PropsImpl implements Props {
        private final Undefinable<@Nullable WidgetRenderables> background;
        private final Undefinable<@Nullable WidgetElements> icon;

        PropsImpl(
                Undefinable<@Nullable WidgetRenderables> background,
                Undefinable<@Nullable WidgetElements> icon,
                @Nullable FZKeyed<Consumer<PressEvent>> pressHandler,
                GuiComponentProps props
        ) {
            super(pressHandler, props);
            this.background = background;
            this.icon = icon;
        }

        @Override
        public Undefinable<@Nullable WidgetRenderables> background() {
            return background;
        }

        @Override
        public Undefinable<@Nullable WidgetElements> icon() {
            return icon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o) &&
                   Objects.equals(other.background(), this.background) &&
                   Objects.equals(other.icon(), this.icon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), background, icon);
        }
    }

    public static final class Builder extends AbstractBuilder<Builder, FZIconButton, Props> {
        private Undefinable<@Nullable WidgetRenderables> background;
        private Undefinable<@Nullable WidgetElements> icon = Undefinable.undefined();

        Builder(WidgetRenderables background) {
            this.background = Undefinable.of(background);
        }

        public Builder defaultBackground() {
            this.background = Undefinable.of(DEFAULT_RENDERABLES);
            return this;
        }

        public Builder background(@Nullable WidgetRenderables background) {
            this.background = Undefinable.of(background);
            return this;
        }

        public Builder icon(@Nullable WidgetElements icon) {
            this.icon = Undefinable.of(icon);
            return this;
        }

        @Override
        public Props toProps() {
            return new PropsImpl(background, icon, pressHandler, props);
        }

        @Override
        public FZIconButton build() {
            FZIconButton button = new FZIconButton();
            button.applyProps(toProps());
            return button;
        }
    }
}
