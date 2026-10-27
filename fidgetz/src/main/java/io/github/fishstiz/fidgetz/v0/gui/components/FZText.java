package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class FZText extends StringWidget implements FZComponent, FZContextMenu.Source {
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private ScreenRectangle bounds;

    FZText(Component message) {
        super(message, Minecraft.getInstance().font);
        alignLeft();
        bounds = super.getRectangle();
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        propsState.contextEntries.accept(collector);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.renderWidget(graphics, mouseX, mouseY, a);
        if (isFocused()) {
            graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), CommonColors.WHITE);
        }
        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, a);
        }
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(bounds, this)) {
            bounds = super.getRectangle();
        }
        return bounds;
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return propsState.id;
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    void applyProps(Props props) {
        propsState.apply(this, props);
    }

    public static FZText bind(String key, FZRef<Props> ref) {
        Props props = ref.value();
        FZText text = new FZText(props.message().orElse(CommonComponents.EMPTY));
        text.applyProps(props);
        props.textAlignment().ifPresent(alignment -> alignment.apply(text));
        if (props.width().isEmpty()) {
            text.setWidth(text.getFont().width(text.getMessage()));
        }
        ref.subscribe(key, text::applyProps);
        return text;
    }

    public static Builder builder(Component message) {
        return new Builder(message);
    }

    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT;

        private void apply(FZText widget) {
            switch (this) {
                case LEFT -> widget.alignLeft();
                case CENTER -> widget.alignCenter();
                case RIGHT -> widget.alignRight();
            }
        }
    }

    public interface Props extends GuiComponentProps {
        default Optional<Alignment> textAlignment() {
            return Optional.empty();
        }
    }

    private static final class PropsImpl extends GuiComponentPropsBase implements Props {
        private final @Nullable Alignment textAlignment;

        private PropsImpl(GuiComponentProps props, @Nullable Alignment textAlignment) {
            super(props);
            this.textAlignment = textAlignment;
        }

        @Override
        public Optional<Alignment> textAlignment() {
            return Optional.ofNullable(textAlignment);
        }
    }

    public static final class Builder extends GuiComponentPropsBuilder<Builder> {
        private @Nullable Alignment textAlignment;

        private Builder(Component message) {
            props.message = message;
        }

        public Builder align(Alignment alignment) {
            this.textAlignment = alignment;
            return this;
        }

        public Builder alignLeft() {
            return align(Alignment.LEFT);
        }

        public Builder alignCenter() {
            return align(Alignment.CENTER);
        }

        public Builder alignRight() {
            return align(Alignment.RIGHT);
        }

        public Props toProps() {
            return new PropsImpl(props, textAlignment);
        }

        public FZText build() {
            FZText text = new FZText(props.message().orElse(CommonComponents.EMPTY));
            Props props = toProps();
            text.applyProps(props);
            if (props.width().isEmpty()) {
                text.setWidth(text.getFont().width(text.getMessage()));
            }
            return text;
        }
    }
}
