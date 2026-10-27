package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.state.FZKeyed;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class FZButtonBase extends Button implements FZComponent, FZContextMenu.Source {
    protected static final int BIG_WIDTH = 200;
    protected static final WidgetRenderables DEFAULT_RENDERABLES = WidgetRenderables.sprites(
            new ResourceLocation("widget/button"),
            new ResourceLocation("widget/button_disabled"),
            new ResourceLocation("widget/button_highlighted")
    );
    protected static final OnPress NOP = ignored -> {
    };
    private final GuiComponentPropsState propsState = new GuiComponentPropsState();
    private Consumer<PressEvent> pressHandler = FunctionUtils.nopConsumer();
    private ScreenRectangle bounds;

    protected FZButtonBase(int width, int height, Component message) {
        super(0, 0, width, height, message, NOP, DEFAULT_NARRATION);
        bounds = new ScreenRectangle(0, 0, width, height);
    }

    protected FZButtonBase(int width, int height) {
        this(width, height, CommonComponents.EMPTY);
    }

    protected FZButtonBase() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT, CommonComponents.EMPTY);
    }

    @Override
    public void onPress() {
        super.onPress();
        pressHandler.accept(new PressEvent(this));
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        propsState.contextEntries.accept(collector);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        extractOverlay(graphics, mouseX, mouseY, partialTick);
    }

    protected void extractOverlay(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        if (propsState.overlay != null) {
            propsState.overlay.extractRenderState(graphics, getX(), getY(), getWidth(), getHeight(), mouseX, mouseY, a);
        }
    }

    @Override
    public ScreenRectangle getRectangle() {
        if (ScreenRectangleUtils.unequal(bounds, this)) {
            bounds = super.getRectangle();
        }
        return bounds;
    }

    protected void applyProps(Props props) {
        propsState.apply(this, props);
        props.pressHandler().ifPresent(handler -> this.pressHandler = handler.value());
    }

    @Override
    public @Nullable String fidgetz$componentId() {
        return propsState.id;
    }

    @Override
    public boolean fidgetz$shouldTakeFocusAfterInteraction() {
        return propsState.focusOnInteraction;
    }

    public record PressEvent(FZButtonBase target) {
    }

    public interface Props extends GuiComponentProps {
        default Optional<FZKeyed<Consumer<PressEvent>>> pressHandler() {
            return Optional.empty();
        }
    }

    abstract static class PropsImpl extends GuiComponentPropsBase implements Props {
        private final @Nullable FZKeyed<Consumer<PressEvent>> pressHandler;

        PropsImpl(@Nullable FZKeyed<Consumer<PressEvent>> pressHandler, GuiComponentProps props) {
            super(props);
            this.pressHandler = pressHandler;
        }

        @Override
        public Optional<FZKeyed<Consumer<PressEvent>>> pressHandler() {
            return Optional.ofNullable(pressHandler);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Props other)) return false;
            return super.equals(o) &&
                   Objects.equals(pressHandler(), other.pressHandler());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), pressHandler);
        }
    }

    public static abstract class AbstractBuilder<T, B extends FZButtonBase, P extends Props> extends GuiComponentPropsBuilder<T> {
        protected @Nullable FZKeyed<Consumer<PressEvent>> pressHandler;

        protected AbstractBuilder() {
        }

        public T bigWidth() {
            return width(BIG_WIDTH);
        }

        public T smallWidth() {
            return width(SMALL_WIDTH);
        }

        public T square() {
            int size = DEFAULT_HEIGHT;
            return size(size, size);
        }

        public T onPress(Consumer<PressEvent> pressHandler) {
            this.pressHandler = FZKeyed.selfKey(Objects.requireNonNull(pressHandler, "pressHandler cannot be null"));
            return self();
        }

        public T onPress(Runnable clickAction) {
            Objects.requireNonNull(clickAction, "clickAction cannot be null");
            this.pressHandler = FZKeyed.selfKey(ignored -> clickAction.run());
            return self();
        }

        public T onPress(Object key, Consumer<PressEvent> pressHandler) {
            this.pressHandler = new FZKeyed<>(key, Objects.requireNonNull(pressHandler, "pressHandler cannot be null"));
            return self();
        }

        public T onPress(Object key, Runnable clickAction) {
            Objects.requireNonNull(clickAction, "clickAction cannot be null");
            this.pressHandler = new FZKeyed<>(key, ignored -> clickAction.run());
            return self();
        }

        public abstract P toProps();

        public abstract B build();
    }
}
