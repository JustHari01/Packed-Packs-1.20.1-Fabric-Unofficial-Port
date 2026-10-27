package io.github.fishstiz.fidgetz.v0.gui.components;

import io.github.fishstiz.fidgetz.v0.gui.renderables.RenderableRectangle;
import io.github.fishstiz.fidgetz.v0.utils.WidgetSizeUtils;
import io.github.fishstiz.fidgetz.v0.utils.FunctionUtils;
import net.minecraft.client.gui.components.AbstractWidget;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class GuiComponentPropsState {
    public @Nullable String id;
    public boolean focusOnInteraction = true;
    public @Nullable RenderableRectangle overlay = null;
    public Consumer<FZContextMenu.Collector> contextEntries = FunctionUtils.nopConsumer();
    private int boundWidth;
    private int boundHeight;

    public GuiComponentPropsState() {
    }

    private void apply(GuiComponentProps props) {
        props.id().ifPresent(id -> this.id = id);
        GuiComponentPropsBase.ifNonDefault(props.focusOnInteraction(), f -> this.focusOnInteraction = f.toBoolean(true));
        props.overlay().ifDefined(overlay -> this.overlay = overlay);
        props.contextEntries().ifPresent(entries -> this.contextEntries = entries.value());
    }

    public void apply(AbstractWidget widget, GuiComponentProps props) {
        props.width().ifPresent(width -> {
            // workaround to allow setting an initial width for bound widgets without locking it into that initial width
            // if the width is static. for flex layout
            if (widget.getWidth() != this.boundWidth && this.boundWidth == width) {
                return;
            }
            this.boundWidth = width;
            if (widget.getWidth() != width) {
                widget.setWidth(width);
            }
        });
        props.height().ifPresent(height -> {
            if (widget.getHeight() != this.boundHeight && this.boundHeight == height) {
                return;
            }
            this.boundHeight = height;
            if (widget.getHeight() != height) {
                WidgetSizeUtils.setHeight(widget, height);
            }
        });
        GuiComponentPropsBase.ifNonDefault(props.active(), a -> widget.active = a.toBoolean(true));
        GuiComponentPropsBase.ifNonDefault(props.visible(), a -> widget.visible = a.toBoolean(true));
        props.tooltip().ifDefined(widget::setTooltip);
        props.message().ifPresent(widget::setMessage);
        props.tabOrderGroup().ifPresent(widget::setTabOrderGroup);
        apply(props);
    }

    public void apply(AbstractListWidget<?> widget, GuiComponentProps props) {
        props.width().ifPresent(width -> {
            if (widget.getWidth() != this.boundWidth && this.boundWidth == width) {
                return;
            }
            this.boundWidth = width;
            if (widget.getWidth() != width) {
                widget.setWidth(width);
            }
        });
        props.height().ifPresent(height -> {
            if (widget.getHeight() != this.boundHeight && this.boundHeight == height) {
                return;
            }
            this.boundHeight = height;
            if (widget.getHeight() != height) {
                widget.setHeight(height);
            }
        });
        GuiComponentPropsBase.ifNonDefault(props.active(), a -> widget.setActive(a.toBoolean(true)));
        GuiComponentPropsBase.ifNonDefault(props.visible(), a -> widget.setVisible(a.toBoolean(true)));
        apply(props);
    }
}
