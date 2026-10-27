package io.github.fishstiz.fidgetz.v0.gui.components;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.fishstiz.fidgetz.v0.Fidgetz;
import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.gui.renderables.Renderables;
import io.github.fishstiz.fidgetz.v0.utils.GuiGraphicsUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

final class FZPopoverMenuEntryImpl implements FZPopoverMenuItem.Entry {
    private static final Component CHEVRON_RIGHT = Component.literal(">");
    private static final int CHEVRON_RIGHT_WIDTH = Minecraft.getInstance().font.width(CHEVRON_RIGHT);
    private static final int DEFAULT_TEXT_COLOR = CommonColors.WHITE;
    private static final int DEFAULT_INACTIVE_TEXT_COLOR = 0xFFA0A0A0;
    private static final int SPACING = 8;
    private final FZPopoverMenuItem.Context context;
    private final List<FZPopoverMenuItem> children;
    private final Supplier<@Nullable WidgetRenderables> backgroundSupplier;
    private final Supplier<@Nullable WidgetElements> iconSupplier;
    private final Supplier<@Nullable Component> messageSupplier;
    private final TooltipHolder tooltipHolder;
    private final BooleanSupplier activeSupplier;
    private final Function<FZPopoverMenuItem.Builder.PressEvent, @Nullable Boolean> pressHandler;
    private final boolean playClickSoundOnInteraction;
    private final int height;
    private final int minWidth;
    private int width;

    FZPopoverMenuEntryImpl(
            FZPopoverMenuItem.Context context,
            List<FZPopoverMenuItem> children,
            Supplier<@Nullable WidgetRenderables> backgroundSupplier,
            Supplier<@Nullable WidgetElements> iconSupplier,
            Supplier<@Nullable Component> messageSupplier,
            TooltipHolder tooltipHolder,
            BooleanSupplier activeSupplier,
            Function<FZPopoverMenuItem.Builder.PressEvent, Boolean> pressHandler,
            boolean playClickSoundOnInteraction,
            int height,
            int minWidth
    ) {
        this.context = context;
        this.children = children;
        this.backgroundSupplier = backgroundSupplier;
        this.iconSupplier = iconSupplier;
        this.messageSupplier = messageSupplier;
        this.tooltipHolder = tooltipHolder;
        this.activeSupplier = activeSupplier;
        this.pressHandler = pressHandler;
        this.playClickSoundOnInteraction = playClickSoundOnInteraction;
        this.height = height;
        this.minWidth = minWidth;
    }

    @Override
    public FZPopoverMenuItem.Context context() {
        return context;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenRectangle bounds = context.getRectangle();
        final int left = bounds.left();
        final int top = bounds.top();
        final int width = bounds.width();
        final int height = bounds.height();

        WidgetRenderables background = backgroundSupplier.get();
        if (background != null) {
            background.get(context.isActive(), context.isFocusedOrHovered())
                    .extractRenderState(graphics, left, top, width, height, mouseX, mouseY, partialTick);
        }

        int x = left;

        WidgetElements icon = iconSupplier.get();
        if (icon != null) {
            x += SPACING + icon.margin().left();
            int iconWidth = icon.width();
            int iconHeight = icon.height();
            int iconY = (top + SPACING + ((height - SPACING * 2) - iconHeight) / 2) + icon.margin().top() - icon.margin().bottom();

            icon.elements()
                    .get(context.isActive(), context.isFocusedOrHovered())
                    .extractRenderState(graphics, x, iconY, iconWidth, iconHeight, mouseX, mouseY, partialTick);
            x += iconWidth + icon.margin().right();
        }

        int innerRight = left + width - SPACING;

        if (!children.isEmpty()) {
            int chevronX = (left + width) - CHEVRON_RIGHT_WIDTH - SPACING;
            innerRight = chevronX - SPACING;
            Font font = Minecraft.getInstance().font;
            int chevronHeight = font.lineHeight;
            int chevronColor = activeSupplier.getAsBoolean() ? DEFAULT_TEXT_COLOR : DEFAULT_INACTIVE_TEXT_COLOR;
            int chevronY = top + (height - chevronHeight) / 2;
            graphics.drawString(font, CHEVRON_RIGHT, chevronX, chevronY + 1, chevronColor);
        }

        Component text = messageSupplier.get();
        if (text != null) {
            x += SPACING;
            int textWidth = Math.max(0, innerRight - x);
            GuiGraphicsUtils.scrollingText(graphics, text, x, top + 1, x + textWidth, top + height + 1, activeSupplier.getAsBoolean()
                    ? DEFAULT_TEXT_COLOR
                    : DEFAULT_INACTIVE_TEXT_COLOR
            );
        }

        if (context.isHovered()) {
            tooltipHolder.refreshTooltipForNextRenderPass(true, isFocused(), bounds);
        }
    }

    private void playClickSound() {
        if (playClickSoundOnInteraction) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (context.isActive() &&
            button == InputConstants.MOUSE_BUTTON_LEFT &&
            Boolean.TRUE.equals(pressHandler.apply(new FZPopoverMenuItem.Builder.PressEvent(context, this)))) {
            playClickSound();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (context.isActive() &&
            (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) &&
            Boolean.TRUE.equals(pressHandler.apply(new FZPopoverMenuItem.Builder.PressEvent(context, this)))) {
            playClickSound();
            return true;
        }
        return false;
    }

    @Override
    public List<FZPopoverMenuItem> childItems() {
        return children;
    }

    @Override
    public boolean isActive() {
        return activeSupplier.getAsBoolean();
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        Component message = messageSupplier.get();
        if (message != null) {
            output.add(NarratedElementType.TITLE, AbstractWidget.wrapDefaultNarrationMessage(message));
            if (isActive()) {
                if (isFocused()) {
                    output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
                } else if (context.isHovered()) {
                    output.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
                }
            }
        }
        tooltipHolder.updateNarration(output);
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getWidth() {
        return Math.max(width, minWidth);
    }

    @Override
    public int getHeight() {
        return height;
    }

    record Divider(
            FZPopoverMenuItem.Context context,
            RenderableRectangle rectangle,
            int height
    ) implements FZPopoverMenuItem.Entry {
        static final int DEFAULT_HEIGHT = 4;
        static final FZPopoverMenuItem.Divider DEFAULT_SECTION = new FZPopoverMenuItem.Divider(
                ctx -> new Divider(ctx, Renderables.sprite(Fidgetz.id("widget/popovermenu_section_divider")).withBlend())
        );
        static final FZPopoverMenuItem.Divider DEFAULT_ENTRY = new FZPopoverMenuItem.Divider(
                ctx -> new Divider(ctx, Renderables.sprite(Fidgetz.id("widget/popovermenu_entry_divider")).withBlend())
        );

        Divider(FZPopoverMenuItem.Context context, RenderableRectangle rectangle) {
            this(context, rectangle, DEFAULT_HEIGHT);
        }

        @Override
        public int getWidth() {
            return context().getRectangle().width();
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public @Nullable ComponentPath nextFocusPath(FocusNavigationEvent navigationEvent) {
            return null;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            ScreenRectangle bounds = context.getRectangle();
            rectangle.extractRenderState(graphics, bounds.left(), bounds.top(), bounds.width(), bounds.height(), mouseX, mouseY, partialTick);
        }

        @Override
        public NarrationPriority narrationPriority() {
            return NarrationPriority.NONE;
        }
    }

    static final class TooltipHolder {
        private final Supplier<@Nullable Tooltip> tooltipSupplier;
        private @Nullable Tooltip lastTooltip;
        private @Nullable List<net.minecraft.util.FormattedCharSequence> tooltipLines;

        TooltipHolder(Supplier<@Nullable Tooltip> tooltipSupplier) {
            this.tooltipSupplier = tooltipSupplier;
        }

        void refreshTooltipForNextRenderPass(boolean hovering, boolean focused, ScreenRectangle screenRectangle) {
            Tooltip tooltip = tooltipSupplier.get();
            if (tooltip == null || (!hovering && !focused)) return;

            List<net.minecraft.util.FormattedCharSequence> lines = tooltip.toCharSequence(Minecraft.getInstance());
            if (tooltipLines == null || !tooltipLines.equals(lines) || !Objects.equals(lastTooltip, tooltip)) {
                tooltipLines = lines;
                lastTooltip = tooltip;
            }

            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) screen.setTooltipForNextRenderPass(tooltipLines);
        }

        void updateNarration(NarrationElementOutput output) {
            Tooltip tooltip = tooltipSupplier.get();
            if (tooltip != null) tooltip.updateNarration(output);
        }
    }
}
