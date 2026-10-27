package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.Fidgetz;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.*;

public sealed interface FZPopoverMenuItem {
    Factory factory();

    Settings settings();

    @FunctionalInterface
    interface Factory {
        Entry create(Context context);
    }

    record Settings(
            boolean closeOnInteract,
            boolean autoDividerAfter,
            boolean stretch,
            float xAlignment
    ) {
        private static final Settings DEFAULT_SETTINGS = new Settings(true, true, true, 0.0f);

        public static Settings defaults() {
            return DEFAULT_SETTINGS;
        }

        public Settings withCloseOnInteract(boolean closeOnInteract) {
            return new Settings(closeOnInteract, autoDividerAfter, stretch, xAlignment);
        }

        public Settings withAutoDividerAfter(boolean autoDividerAfter) {
            return new Settings(closeOnInteract, autoDividerAfter, stretch, xAlignment);
        }

        public Settings withStretch(boolean stretch) {
            return new Settings(closeOnInteract, autoDividerAfter, stretch, xAlignment);
        }

        public Settings withXAlign(float xAlignment) {
            return new Settings(closeOnInteract, autoDividerAfter, stretch, xAlignment);
        }

        public Settings withLeftXAlign() {
            return new Settings(closeOnInteract, autoDividerAfter, stretch, 0.0f);
        }

        public Settings withCenterXAlign() {
            return new Settings(closeOnInteract, autoDividerAfter, stretch, 0.5f);
        }

        public Settings withRightXAlign() {
            return new Settings(closeOnInteract, autoDividerAfter, stretch, 1.0f);
        }
    }

    record Widget(Factory factory, Settings settings) implements FZPopoverMenuItem {
        public Widget(Factory factory) {
            this(factory, Settings.defaults());
        }
    }

    record Divider(@Nullable Factory factory, Settings settings) implements FZPopoverMenuItem {
        private static final Settings DEFAULT_SETTINGS = new Settings(false, false, true, 0.0f);

        public Divider(@Nullable Factory factory) {
            this(factory, DEFAULT_SETTINGS);
        }
    }

    static Divider divider() {
        return new Divider(null);
    }

    static Divider createDivider(RenderableRectangle rectangle, int height) {
        return new Divider(ctx -> new FZPopoverMenuEntryImpl.Divider(ctx, rectangle, height));
    }

    static Divider createDivider(ResourceLocation sprite, int height) {
        return new Divider(ctx -> new FZPopoverMenuEntryImpl.Divider(ctx, Renderables.sprite(sprite), height));
    }

    static Widget fromWidget(Function<Context, AbstractWidget> widgetFactory, Settings settings) {
        // cant use WrappedComponent because of some mapping issue on fabric where the default methods of the Entry
        // interface are used instead of the methods on the concrete WrappedComponent class
        class Wrapped implements Entry, ContainerEventHandlerPatch, FZHoverableContainer {
            private final Context context;
            private final AbstractWidget widget;
            private final List<AbstractWidget> children;
            private boolean dragging;
            private @Nullable GuiEventListener fidgetz$hoveredElement;

            Wrapped(Context context) {
                this.context = context;
                this.widget = widgetFactory.apply(context);
                this.children = List.of(widget);
            }

            @Override
            public Context context() {
                return context;
            }

            @Override
            public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                widget.render(graphics, mouseX, mouseY, partialTick);
            }

            @Override
            public void setX(int x) {
                widget.setX(x);
            }

            @Override
            public void setY(int y) {
                widget.setY(y);
            }

            @Override
            public void setWidth(int width) {
                widget.setWidth(width);
            }

            @Override
            public int getX() {
                return widget.getX();
            }

            @Override
            public int getY() {
                return widget.getY();
            }

            @Override
            public int getWidth() {
                return widget.getWidth();
            }

            @Override
            public int getHeight() {
                return widget.getHeight();
            }

            @Override
            public void setFocused(boolean focused) {
                widget.setFocused(focused);
            }

            @Override
            public boolean isFocused() {
                return widget.isFocused();
            }

            @Override
            public void updateNarration(NarrationElementOutput output) {
                widget.updateNarration(output);
            }

            @Override
            public NarrationPriority narrationPriority() {
                return widget.narrationPriority();
            }

            @Override
            public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
                return ComponentPath.path(this, widget.nextFocusPath(navigationEvent));
            }

            @Override
            public void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
                widget.visitWidgets(widgetVisitor);
            }

            @Override
            public ScreenRectangle getRectangle() {
                return widget.getRectangle();
            }

            @Override
            public void mouseMoved(double mouseX, double mouseY) {
                widget.mouseMoved(mouseX, mouseY);
            }

            @Override
            public List<AbstractWidget> children() {
                return children;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return widget.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseReleased(double mouseX, double mouseY, int button) {
                return widget.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                return widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            }

            @Override
            public boolean isDragging() {
                return dragging;
            }

            @Override
            public void setDragging(boolean dragging) {
                this.dragging = dragging;
            }

            @Override
            public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
                return widget.mouseScrolled(mouseX, mouseY, scrollY);
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                return widget.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
                return widget.keyReleased(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean charTyped(char codePoint, int modifiers) {
                return widget.charTyped(codePoint, modifiers);
            }

            @Override
            public @Nullable GuiEventListener getFocused() {
                return widget.isFocused() ? widget : null;
            }

            @Override
            public void setFocused(@Nullable GuiEventListener focused) {
                if (focused == null) {
                    widget.setFocused(false);
                } else if (focused == widget) {
                    widget.setFocused(true);
                }
            }

            @Override
            public boolean isMouseOver(double mouseX, double mouseY) {
                return widget.isMouseOver(mouseX, mouseY);
            }

            @Override
            public @Nullable ComponentPath getCurrentFocusPath() {
                return ComponentPath.path(this, widget.getCurrentFocusPath());
            }

            @Override
            public int getTabOrderGroup() {
                return widget.getTabOrderGroup();
            }

            @Override
            public void setPosition(int x, int y) {
                widget.setPosition(x, y);
            }

            @Override
            public boolean isActive() {
                return widget.isActive();
            }

            @Override
            public boolean fidgetz$shouldTakeFocusAfterInteraction() {
                return widget instanceof FZComponent component
                        ? component.fidgetz$shouldTakeFocusAfterInteraction()
                        : Entry.super.fidgetz$shouldTakeFocusAfterInteraction();
            }

            @Override
            public @Nullable GuiEventListener fidgetz$getHovered() {
                return fidgetz$hoveredElement;
            }

            @Override
            public void fidgetz$setHovered(@Nullable GuiEventListener hovered) {
                GuiEventListener previous = fidgetz$getHovered();
                if (previous != hovered) {
                    if (previous instanceof FZHoverableElement previousElement) {
                        previousElement.fidgetz$setHovered(false);
                    }

                    if (hovered instanceof FZHoverableElement hoveredElement) {
                        hoveredElement.fidgetz$setHovered(true);
                    }

                    fidgetz$hoveredElement = hovered;
                }
            }
        }

        return new Widget(Wrapped::new, settings);
    }

    static Widget fromWidget(Function<Context, AbstractWidget> widgetFactory, UnaryOperator<Settings> settingsBuilder) {
        return fromWidget(widgetFactory, settingsBuilder.apply(Settings.defaults()));
    }

    static Widget fromWidget(AbstractWidget widget, Settings settings) {
        return fromWidget(ignored -> widget, settings);
    }

    static Widget fromWidget(AbstractWidget widget, UnaryOperator<Settings> settingsBuilder) {
        return fromWidget(widget, settingsBuilder.apply(Settings.defaults()));
    }

    static Widget fromWidget(Function<Context, AbstractWidget> widgetFactory) {
        return fromWidget(widgetFactory, Settings.defaults());
    }

    static Widget fromWidget(AbstractWidget widget) {
        return fromWidget(widget, Settings.defaults());
    }

    static Builder builder() {
        return new Builder();
    }

    @ApiStatus.NonExtendable
    interface Context {
        void closeMenu();

        boolean isActive();

        boolean isHovered();

        boolean isFocused();

        boolean isChildOpened();

        ScreenRectangle getRectangle();

        default boolean isFocusedOrHovered() {
            return isFocused() || isHovered();
        }
    }

    interface Entry extends LayoutElement, GuiEventListener, NarratableEntry, Renderable, FZComponent {
        int DEFAULT_HEIGHT = 20;

        Context context();

        default List<FZPopoverMenuItem> childItems() {
            return Collections.emptyList();
        }

        @Override
        default void setX(int x) {
        }

        @Override
        default void setY(int y) {
        }

        default void setWidth(int width) {
        }

        @Override
        default int getX() {
            return context().getRectangle().left();
        }

        @Override
        default int getY() {
            return context().getRectangle().top();
        }

        @Override
        default int getHeight() {
            return DEFAULT_HEIGHT;
        }

        @Override
        default void setFocused(boolean focused) {
        }

        @Override
        default boolean isFocused() {
            return context().isFocused();
        }

        @Override
        default void updateNarration(NarrationElementOutput output) {
        }

        @Override
        default NarratableEntry.NarrationPriority narrationPriority() {
            if (isActive()) {
                if (isFocused()) {
                    return NarratableEntry.NarrationPriority.FOCUSED;
                } else if (context().isHovered()) {
                    return NarratableEntry.NarrationPriority.HOVERED;
                }
            }
            return NarratableEntry.NarrationPriority.NONE;
        }

        @Override
        default @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            if (!isActive()) return null;
            return !isFocused() ? ComponentPath.leaf(this) : null;
        }

        @Override
        default void visitWidgets(Consumer<AbstractWidget> widgetVisitor) {
        }

        @Override
        default ScreenRectangle getRectangle() {
            return LayoutElement.super.getRectangle();
        }
    }

    final class Builder {
        private static final int DEFAULT_WIDTH = 150;
        private static final Supplier<WidgetRenderables> DEFAULT_BACKGROUND;
        private final List<FZPopoverMenuItem> children = new ArrayList<>();
        private @Nullable Supplier<@Nullable WidgetRenderables> backgroundSupplier = DEFAULT_BACKGROUND;
        private @Nullable Supplier<@Nullable WidgetElements> iconSupplier;
        private @Nullable Supplier<@Nullable Component> messageSupplier;
        private FZPopoverMenuEntryImpl.@Nullable TooltipHolder tooltipHolder;
        private @Nullable BooleanSupplier activeSupplier;
        private @Nullable Function<PressEvent, Boolean> pressHandler;
        private boolean closeOnInteraction = true;
        private boolean allowDivideAfterEntry = true;
        private boolean playClickSoundOnInteraction = true;
        private int height = Entry.DEFAULT_HEIGHT;
        private int minWidth = DEFAULT_WIDTH;

        static {
            RenderableRectangle background = Renderables.sprite(Fidgetz.id("widget/popovermenu_entry")).withBlend();
            RenderableRectangle highlighted = Renderables.sprite(Fidgetz.id("widget/popovermenu_entry_highlighted")).withBlend();
            WidgetRenderables renderables = new WidgetRenderables(background, background, highlighted);
            DEFAULT_BACKGROUND = () -> renderables;
        }

        private Builder() {
        }

        public record PressEvent(Context context, FZPopoverMenuItem.Entry entry) {
        }

        public Builder background(@Nullable Supplier<@Nullable WidgetRenderables> backgroundSupplier) {
            this.backgroundSupplier = backgroundSupplier;
            return this;
        }

        public Builder background(WidgetRenderables background) {
            Objects.requireNonNull(background, "background cannot be null");
            return background(() -> background);
        }

        public Builder background(RenderableRectangle background) {
            return background(new WidgetRenderables(Objects.requireNonNull(background, "background cannot be null")));
        }

        public Builder icon(@Nullable Supplier<@Nullable WidgetElements> iconSupplier) {
            this.iconSupplier = iconSupplier;
            return this;
        }

        public Builder icon(WidgetElements icon) {
            return icon(() -> icon);
        }

        public Builder icon(WidgetRenderables icon) {
            Objects.requireNonNull(icon, "icon cannot be null");
            return icon(new WidgetElements(icon, 16, 16));
        }

        public Builder icon(RenderableRectangle icon) {
            Objects.requireNonNull(icon, "icon cannot be null");
            return icon(new WidgetRenderables(icon));
        }

        public Builder message(@Nullable Supplier<@Nullable Component> textSupplier) {
            this.messageSupplier = textSupplier;
            return this;
        }

        public Builder message(@Nullable Component message) {
            return message(() -> message);
        }

        public Builder tooltip(@Nullable Supplier<@Nullable Tooltip> tooltipSupplier) {
            this.tooltipHolder = tooltipSupplier == null ? null : new FZPopoverMenuEntryImpl.TooltipHolder(tooltipSupplier);
            return this;
        }

        public Builder tooltip(@Nullable Tooltip tooltip) {
            return tooltip(() -> tooltip);
        }

        public Builder active(@Nullable BooleanSupplier activeSupplier) {
            this.activeSupplier = activeSupplier;
            return this;
        }

        public Builder active(boolean active) {
            return active(() -> active);
        }

        public Builder active() {
            return active(true);
        }

        public Builder child(FZPopoverMenuItem child) {
            children.add(child);
            return this;
        }

        public Builder child(UnaryOperator<Builder> builderConfigurator) {
            children.add(builderConfigurator.apply(builder()).build());
            return this;
        }

        public Builder nextSection() {
            return child(divider());
        }

        public Builder children(List<FZPopoverMenuItem> children) {
            this.children.addAll(children);
            return this;
        }

        public Builder closeOnInteraction(boolean closeOnInteraction) {
            this.closeOnInteraction = closeOnInteraction;
            return this;
        }

        public Builder closeOnInteraction() {
            return closeOnInteraction(true);
        }

        public Builder allowAutoDivideAfterEntry(boolean allowAutoDivideAfterEntry) {
            this.allowDivideAfterEntry = allowAutoDivideAfterEntry;
            return this;
        }

        public Builder allowAutoDivideAfterEntry() {
            this.allowDivideAfterEntry = true;
            return this;
        }

        public Builder playClickSoundOnInteraction(boolean playClickSoundOnInteraction) {
            this.playClickSoundOnInteraction = playClickSoundOnInteraction;
            return this;
        }

        public Builder playClickSoundOnInteraction() {
            return playClickSoundOnInteraction(true);
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder minWidth(int width) {
            this.minWidth = width;
            return this;
        }

        public Builder onPress(@Nullable Function<PressEvent, Boolean> pressHandler) {
            this.pressHandler = pressHandler;
            return this;
        }

        public Builder onPress(BooleanSupplier pressHandler) {
            Objects.requireNonNull(pressHandler, "pressHandler is null");
            return onPress(ignored -> pressHandler.getAsBoolean());
        }

        public Builder onPress(Runnable mouseAction) {
            Objects.requireNonNull(mouseAction, "mouseAction is null");
            return onPress(ignored -> {
                mouseAction.run();
                return true;
            });
        }

        public Builder preventPress() {
            this.pressHandler = null;
            return this;
        }

        public FZPopoverMenuItem build() {
            return new FZPopoverMenuItem.Widget(
                    ctx -> new FZPopoverMenuEntryImpl(
                            ctx,
                            children,
                            backgroundSupplier == null ? FunctionUtils.nullSupplier() : backgroundSupplier,
                            iconSupplier == null ? FunctionUtils.nullSupplier() : iconSupplier,
                            messageSupplier == null ? FunctionUtils.nullSupplier() : messageSupplier,
                            tooltipHolder == null ? new FZPopoverMenuEntryImpl.TooltipHolder(FunctionUtils.nullSupplier()) : tooltipHolder,
                            activeSupplier == null ? () -> true : activeSupplier,
                            pressHandler == null ? ignored -> true : pressHandler,
                            playClickSoundOnInteraction,
                            height,
                            minWidth
                    ),
                    new Settings(closeOnInteraction, allowDivideAfterEntry, true, 0.0f)
            );
        }
    }
}
