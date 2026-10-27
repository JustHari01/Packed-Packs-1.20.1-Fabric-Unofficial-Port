package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class FZIcon extends AbstractWidget implements FZComponent, FZContextMenu.Source {
    private static final int DEFAULT_WIDTH = 20;
    private static final int DEFAULT_HEIGHT = 20;
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private WidgetRenderables icon;

    public FZIcon(int x, int y, int width, int height, Component message, WidgetRenderables icon) {
        super(x, y, width, height, message);
        this.icon = icon;
        this.active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        icon.get(isActive(), isHoveredOrFocused()).extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);

        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        propsState.contextEntries.accept(collector);
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return propsState.id;
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    private void applyProps(Props props) {
        propsState.apply(this, props);
        this.icon = props.icon();
    }

    public static Builder builder(WidgetRenderables icon) {
        return new Builder(Objects.requireNonNull(icon, "icon cannot be null"));
    }

    public static Builder builder(RenderableRectangle icon) {
        return new Builder(new WidgetRenderables(Objects.requireNonNull(icon, "icon cannot be null")));
    }

    public static Builder builder(ResourceLocation icon) {
        return new Builder(new WidgetRenderables(Renderables.sprite(Objects.requireNonNull(icon, "icon cannot be null"))));
    }

    public static FZIcon bind(String key, FZRef<Props> ref) {
        Props props = ref.value();
        FZIcon icon = new FZIcon(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY, props.icon());
        icon.applyProps(props);
        ref.subscribe(key, icon::applyProps);
        return icon;
    }

    public interface Props extends GuiComponentProps {
        WidgetRenderables icon();
    }

    private static final class PropsImpl extends GuiComponentPropsBase implements Props {
        private final WidgetRenderables icon;

        private PropsImpl(GuiComponentProps props, WidgetRenderables icon) {
            super(props);
            this.icon = icon;
        }

        @Override
        public WidgetRenderables icon() {
            return icon;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o) && Objects.equals(other.icon(), this.icon);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), icon);
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private final WidgetRenderables icon;

        private Builder(WidgetRenderables icon) {
            this.icon = icon;
        }

        public Props toProps() {
            return new PropsImpl(props, icon);
        }

        public FZIcon build() {
            FZIcon iconWidget = new FZIcon(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY, icon);
            iconWidget.applyProps(toProps());
            return iconWidget;
        }
    }
}
