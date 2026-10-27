package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.gui.state.FZRef;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import io.github.fishstiz.fidgetz.v0.utils.TriState;
import io.github.fishstiz.fidgetz.v0.utils.Undefinable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.CommonColors;
import net.minecraft.util.Mth;
import io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

public final class FZButton extends FZButtonBase {
    private static final int DEFAULT_SPACING = 8;
    private static final int TEXT_MARGIN = 4;
    private @Nullable WidgetRenderables sprites = DEFAULT_RENDERABLES;
    private @Nullable WidgetElements leftIcon;
    private @Nullable WidgetElements rightIcon;
    private boolean centeredMessage = true;

    FZButton() {
    }

    private void extractSprite(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (sprites != null) {
            sprites.get(isActive(), isHoveredOrFocused()).extractRenderState(
                    graphics,
                    getX(),
                    getY(),
                    getWidth(),
                    getHeight(),
                    mouseX,
                    mouseY,
                    partialTick
            );
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        extractSprite(graphics, mouseX, mouseY, partialTick);

        int margin = centeredMessage ? TEXT_MARGIN : DEFAULT_SPACING;
        int spacing = DEFAULT_SPACING;
        int left = getX() + margin;
        int right = WidgetSizeUtils.right(this) - margin;
        int top = getY();
        int height = getHeight();
        int bottom = top + height;

        if (leftIcon != null) {
            left += spacing - margin + leftIcon.margin().left();

            int iconTop = (top + height / 2 - leftIcon.height() / 2) + leftIcon.margin().top() - leftIcon.margin().bottom();

            leftIcon.elements()
                    .get(isActive(), isHoveredOrFocused())
                    .extractRenderState(graphics, left, iconTop, leftIcon.width(), leftIcon.height(), mouseX, mouseY, partialTick);

            left += leftIcon.width() + leftIcon.margin().right() + spacing / 2;
        }

        if (rightIcon != null) {
            right -= spacing - margin + rightIcon.width() + rightIcon.margin().right();

            int iconTop = (top + height / 2 - rightIcon.height() / 2) + rightIcon.margin().top() - rightIcon.margin().bottom();

            rightIcon.elements()
                    .get(isActive(), isHoveredOrFocused())
                    .extractRenderState(graphics, right, iconTop, rightIcon.width(), rightIcon.height(), mouseX, mouseY, partialTick);

            right -= spacing / 2 + rightIcon.margin().left();
        }

        int color = isActive() ? CommonColors.WHITE : 0xFFA0A0A0;
        if (centeredMessage) {
            renderString(graphics, Minecraft.getInstance().font, color | Mth.ceil(this.alpha * 255.0F) << 24);
        } else {
            GuiGraphicsUtils.scrollingText(graphics, getMessage(), left, top, right, bottom, color);
        }

        extractOverlay(graphics, mouseX, mouseY, partialTick);
    }

    void applyProps(Props props) {
        super.applyProps(props);
        props.sprites().ifDefined(sprites -> this.sprites = sprites);
        props.leftIcon().ifDefined(leftIcon -> this.leftIcon = leftIcon);
        props.rightIcon().ifDefined(rightIcon -> this.rightIcon = rightIcon);
        if (props.centeredMessage() != TriState.DEFAULT) {
            this.centeredMessage = props.centeredMessage().toBoolean(true);
        }
    }

    public static FZButton bind(String key, FZRef<Props> ref) {
        FZButton button = new FZButton();
        button.applyProps(ref.value());
        ref.subscribe(key, button::applyProps);
        return button;
    }

    public static Builder builder() {
        return new Builder();
    }

    public interface Props extends FZButtonBase.Props {
        default Undefinable<@Nullable WidgetRenderables> sprites() {
            return Undefinable.undefined();
        }

        default Undefinable<@Nullable WidgetElements> leftIcon() {
            return Undefinable.undefined();
        }

        default Undefinable<@Nullable WidgetElements> rightIcon() {
            return Undefinable.undefined();
        }

        default TriState centeredMessage() {
            return TriState.DEFAULT;
        }
    }

    private static final class PropsImpl extends FZButtonBase.PropsImpl implements Props {
        private final Undefinable<@Nullable WidgetRenderables> sprites;
        private final Undefinable<@Nullable WidgetElements> leftIcon;
        private final Undefinable<@Nullable WidgetElements> rightIcon;
        private final TriState centeredMessage;

        PropsImpl(
                Undefinable<@Nullable WidgetRenderables> sprites,
                Undefinable<@Nullable WidgetElements> leftIcon,
                Undefinable<@Nullable WidgetElements> rightIcon,
                TriState centeredMessage,
                @Nullable FZKeyed<Consumer<PressEvent>> pressHandler,
                GuiComponentProps props
        ) {
            super(pressHandler, props);
            this.sprites = sprites;
            this.leftIcon = leftIcon;
            this.rightIcon = rightIcon;
            this.centeredMessage = centeredMessage;
        }

        @Override
        public Undefinable<@Nullable WidgetRenderables> sprites() {
            return sprites;
        }

        @Override
        public Undefinable<@Nullable WidgetElements> leftIcon() {
            return leftIcon;
        }

        @Override
        public Undefinable<@Nullable WidgetElements> rightIcon() {
            return rightIcon;
        }

        @Override
        public TriState centeredMessage() {
            return centeredMessage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o) &&
                   Objects.equals(sprites, other.sprites()) &&
                   Objects.equals(leftIcon, other.leftIcon()) &&
                   Objects.equals(rightIcon, other.rightIcon()) &&
                   centeredMessage.equals(other.centeredMessage());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), sprites, leftIcon, rightIcon, centeredMessage);
        }
    }

    public static final class Builder extends AbstractBuilder<Builder, FZButton, Props> {
        private Undefinable<@Nullable WidgetRenderables> sprites = Undefinable.undefined();
        private Undefinable<@Nullable WidgetElements> leftIcon = Undefinable.undefined();
        private Undefinable<@Nullable WidgetElements> rightIcon = Undefinable.undefined();
        private TriState centeredMessage = TriState.DEFAULT;

        private Builder() {
        }

        public Builder sprites(@Nullable WidgetRenderables sprites) {
            this.sprites = Undefinable.of(sprites);
            return this;
        }

        public Builder leftIcon(@Nullable WidgetElements leftIcon) {
            this.leftIcon = Undefinable.of(leftIcon);
            return this;
        }

        public Builder leftIcon(RenderableRectangle leftIcon, int width, int height) {
            WidgetRenderables sprites = new WidgetRenderables(Objects.requireNonNull(leftIcon, "leftIcon cannot be null"));
            this.leftIcon = Undefinable.of(new WidgetElements(sprites, width, height));
            return this;
        }

        public Builder leftIcon(WidgetRenderables leftIcon, int width, int height) {
            this.leftIcon = Undefinable.of(new WidgetElements(leftIcon, width, height));
            return this;
        }

        public Builder rightIcon(@Nullable WidgetElements rightIcon) {
            this.rightIcon = Undefinable.of(rightIcon);
            return this;
        }

        public Builder rightIcon(RenderableRectangle rightIcon, int width, int height) {
            WidgetRenderables sprites = new WidgetRenderables(Objects.requireNonNull(rightIcon, "leftIcon cannot be null"));
            this.rightIcon = Undefinable.of(new WidgetElements(sprites, width, height));
            return this;
        }

        public Builder rightIcon(WidgetRenderables rightIcon, int width, int height) {
            this.rightIcon = Undefinable.of(new WidgetElements(rightIcon, width, height));
            return this;
        }

        public Builder centeredMessage() {
            this.centeredMessage = TriState.TRUE;
            return this;
        }

        public Builder leftAlignedMessage() {
            this.centeredMessage = TriState.FALSE;
            return this;
        }

        @Override
        public Props toProps() {
            return new PropsImpl(sprites, leftIcon, rightIcon, centeredMessage, pressHandler, props);
        }

        @Override
        public FZButton build() {
            FZButton button = new FZButton();
            button.applyProps(toProps());
            return button;
        }
    }
}
