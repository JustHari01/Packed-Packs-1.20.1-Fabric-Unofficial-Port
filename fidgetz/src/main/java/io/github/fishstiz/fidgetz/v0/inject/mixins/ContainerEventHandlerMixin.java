package io.github.fishstiz.fidgetz.v0.inject.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.fishstiz.fidgetz.v0.gui.components.*;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableContainer;
import io.github.fishstiz.fidgetz.v0.gui.components.events.FZHoverableElement;
import io.github.fishstiz.fidgetz.v0.utils.ScreenRectangleUtils;
import io.github.fishstiz.fidgetz.v0.gui.screens.FZScreen;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(ContainerEventHandler.class)
interface ContainerEventHandlerMixin extends GuiEventListener, FZHoverableContainer, FZContextMenu.Source {
    @Shadow
    List<? extends GuiEventListener> children();

    @Override
    default @Nullable GuiEventListener fidgetz$getHovered() {
        return null;
    }

    @Override
    default void fidgetz$setHovered(@Nullable GuiEventListener hovered) {
    }

    @Override
    default boolean fidgetz$isHovered() {
        return FZHoverableContainer.super.fidgetz$isHovered();
    }

    @Override
    default void fidgetz$setHovered(boolean hovered) {
        FZHoverableContainer.super.fidgetz$setHovered(hovered);
    }

    @Override
    default boolean fidgetz$updateHovered(double mouseX, double mouseY) {
        if (isMouseOver(mouseX, mouseY)) {
            for (GuiEventListener child : children()) {
                if (((FZHoverableElement) child).fidgetz$updateHovered(mouseX, mouseY)) {
                    fidgetz$setHovered(child);
                    return true;
                }
            }
            fidgetz$setHovered(null);
            return true;
        }
        fidgetz$setHovered(null);
        return false;
    }

    @WrapOperation(method = "getCurrentFocusPath", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/events/GuiEventListener;getCurrentFocusPath()Lnet/minecraft/client/gui/ComponentPath;"
    ))
    private ComponentPath fixContainerCurrentFocusPath(GuiEventListener instance, Operation<ComponentPath> original) {
        ComponentPath path = original.call(instance);
        // ContainerEventHandler by default returns null on getCurrentFocusPath if it does not have a focused child,
        // which is a problem if the container itself is the focused child of a parent container.
        // this probably should be in vanilla, but only apply for fidgetz in case of compat issues
        if (!(this instanceof FZComponent) && !(this instanceof FZScreen) && !(this instanceof ContainerEventHandlerPatch)) return path;
        return path == null ? ComponentPath.leaf(instance) : path;
    }

    @WrapMethod(method = "nextFocusPath")
    private ComponentPath initializeExcludedAreas(FocusNavigationEvent navigationEvent, Operation<ComponentPath> original) {
        if (this instanceof FZDialogContainer container) {
            for (FZDialog dialog : container.fidgetz$Dialogs()) {
                if (dialog.isOpen() && dialog.shouldCaptureFocus()) {
                    ComponentPath path = dialog.nextFocusPath(navigationEvent);
                    if (path != null) {
                        return ComponentPath.path((ContainerEventHandler) this, path);
                    }
                }
            }
        }

        return original.call(navigationEvent);
    }

    @ModifyExpressionValue(
            method = {"handleTabNavigation", "nextFocusPathInDirection", "nextFocusPathVaguelyInDirection"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/events/ContainerEventHandler;children()Ljava/util/List;")
    )
    private List<? extends GuiEventListener> removeOccludedChildren(List<? extends GuiEventListener> original) {
        if (!(this instanceof FZDialogContainer container)) return original;

        List<? extends FZDialog> dialogs = container.fidgetz$Dialogs();
        if (dialogs.isEmpty()) return original;

        List<GuiEventListener> potentialChildren = new ArrayList<>(original.size());
        for (GuiEventListener child : original) {
            boolean visible = true;
            for (FZDialog dialog : dialogs) {
                if (child == dialog) {
                    break;
                }
                if (dialog.isOpen() && ScreenRectangleUtils.encompasses(dialog.getRectangle(), child.getRectangle())) {
                    visible = false;
                    break;
                }
            }

            if (visible) {
                potentialChildren.add(child);
            }
        }

        return potentialChildren;
    }
}
