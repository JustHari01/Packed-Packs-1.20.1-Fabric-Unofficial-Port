package io.github.fishstiz.fidgetz.v0.gui.components.events;

import io.github.fishstiz.fidgetz.v0.utils.NavigationUtils;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;

public interface ScrollableContainer extends ContainerEventHandler {
    double scrollRate();

    double scrollAmount();

    void setScrollAmount(double scrollAmount);

    default @Nullable ComponentPath addScrollEffectOnFocus(FocusNavigationEvent event, @Nullable ComponentPath path) {
        if (!(path instanceof ComponentPath.Path pathNode)) {
            return path;
        }

        if (event instanceof FocusNavigationEvent.TabNavigation ||
            event instanceof FocusNavigationEvent.ArrowNavigation) {
            return ComponentPath.path(this, NavigationUtils.afterFocusEffect(pathNode.childPath(), this::scrollOnFocus));
        }

        return path;
    }

    default void scrollOnFocus(GuiEventListener component) {
        if (getFocused() == component) {
            ScreenRectangle area = getRectangle();
            ScreenRectangle componentRectangle = component.getRectangle();
            int topDelta = componentRectangle.top() - area.top();
            int bottomDelta = componentRectangle.bottom() - area.bottom();
            double scrollRate = scrollRate();
            if (topDelta < 0) {
                setScrollAmount(scrollAmount() + topDelta - scrollRate);
            } else if (bottomDelta > 0) {
                setScrollAmount(scrollAmount() + bottomDelta + scrollRate);
            }
        }
    }
}
