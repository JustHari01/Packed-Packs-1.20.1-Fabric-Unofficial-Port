package io.github.fishstiz.packed_packs.compat.respackopts;

import io.github.fishstiz.fidgetz.v0.gui.components.FZContextMenu;
import io.github.fishstiz.packed_packs.PackedPacks;
import io.github.fishstiz.packed_packs.api.Preference;
import io.github.fishstiz.packed_packs.api.context.PackContext;
import io.github.fishstiz.packed_packs.compat.Mod;
import io.github.fishstiz.packed_packs.compat.ModIntegration;
import io.github.fishstiz.packed_packs.config.Config;
import io.github.fishstiz.packed_packs.gui.components.PreferenceHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Path;

public class RespackoptsWidget extends AbstractButton implements FZContextMenu.Source {
    private static final String META_CACHE = "io.gitlab.jfronny.respackopts.util.MetaCache";
    private static final String CONFIGURE_ICON = "respackopts:textures/gui/configure_button.png";
    private final Preference<Boolean> preference;
    private final LayoutElement container;
    private final Pack pack;
    private final Object cacheKey;

    private RespackoptsWidget(Preference<Boolean> preference, LayoutElement container, Pack pack, Object cacheKey) {
        super(0, 0, 20, 20, Component.literal(Mod.RESPACKOPTS.getId()));
        this.preference = preference;
        this.container = container;
        this.pack = pack;
        this.cacheKey = cacheKey;
    }

    public static @Nullable RespackoptsWidget create(Preference<Boolean> preference, LayoutElement container, PackContext context) {
        Pack pack = context.pack();
        if (!isSelectable(pack)) {
            return null;
        }

        Object key = getCacheKey(pack.getTitle().getString());
        return key == null ? null : new RespackoptsWidget(preference, container, pack, key);
    }

    private static boolean isSelectable(Pack pack) {
        return !pack.isFixedPosition() || !pack.isRequired();
    }

    private static @Nullable Object getCacheKey(String displayName) {
        try {
            Class<?> metaCache = Class.forName(META_CACHE);
            return metaCache.getMethod("getKeyByDisplayName", String.class).invoke(null, displayName);
        } catch (ReflectiveOperationException e) {
            PackedPacks.LOGGER.warn("[packed_packs] Could not read Respackopts pack metadata", e);
            return null;
        }
    }

    @Override
    public void onPress() {
        try {
            openConfigScreen();
        } catch (ReflectiveOperationException e) {
            PackedPacks.LOGGER.error("[packed_packs] Failed to open Respackopts configuration for {}", pack.getId(), e);
        }
    }

    private void openConfigScreen() throws ReflectiveOperationException {
        Class<?> metaCache = Class.forName(META_CACHE);
        String configId = (String) metaCache.getMethod("getId", cacheKey.getClass()).invoke(null, cacheKey);
        Object branch = metaCache.getMethod("getBranch", cacheKey.getClass()).invoke(null, cacheKey);
        Path dataLocation = (Path) cacheKey.getClass().getMethod("dataLocation").invoke(cacheKey);

        Class<?> dslClass = Class.forName("io.gitlab.jfronny.libjf.config.api.v1.dsl.DSL");
        Object dsl = dslClass.getMethod("create", String.class).invoke(null, configId);
        Method configMethod = findSingleArgumentMethod(dsl.getClass(), "config");
        Class<?> configFunction = configMethod.getParameterTypes()[0];
        InvocationHandler handler = (proxy, method, args) -> {
            if (method.getDeclaringClass() == Object.class) {
                return switch (method.getName()) {
                    case "toString" -> "Packed Packs Respackopts config function";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            }
            if (args == null || args.length == 0) {
                return null;
            }
            Method buildConfig = findThreeArgumentMethod(branch.getClass(), "buildConfig");
            return buildConfig.invoke(branch, args[0], configId, dataLocation);
        };
        Object function = Proxy.newProxyInstance(configFunction.getClassLoader(), new Class<?>[]{configFunction}, handler);
        Object config = configMethod.invoke(dsl, function);

        Class<?> screenFactoryClass = Class.forName("io.gitlab.jfronny.libjf.config.api.v2.ui.ConfigScreenFactory");
        Object screenFactory = screenFactoryClass.getMethod("getInstance").invoke(null);
        Method createMethod = findCreateMethod(screenFactoryClass, config, Minecraft.getInstance().screen);
        Object built = createMethod.invoke(screenFactory, config, Minecraft.getInstance().screen);
        Screen screen = (Screen) built.getClass().getMethod("get").invoke(built);
        Minecraft.getInstance().setScreen(screen);
    }

    private static Method findSingleArgumentMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1 && method.getParameterTypes()[0].isInterface()) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "(functional interface)");
    }

    private static Method findThreeArgumentMethod(Class<?> type, String name) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 3) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "(3 arguments)");
    }

    private static Method findCreateMethod(Class<?> type, Object config, Screen parent) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals("create") && method.getParameterCount() == 2 &&
                method.getParameterTypes()[0].isInstance(config) && method.getParameterTypes()[1].isInstance(parent)) {
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + ".create(config, screen)");
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.setX(container.getX() + container.getWidth() - 30);
        this.setY(container.getY() + (container.getHeight() - 20) / 2);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 1.0F);
        graphics.blit(new ResourceLocation(CONFIGURE_ICON), getX(), getY(), 0, isHoveredOrFocused() ? 20 : 0, 20, 20, 32, 64);
        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
        this.defaultButtonNarrationText(narration);
    }

    @Override
    public void fidgetz$updateContextEntries(double x, double y, FZContextMenu.Collector collector) {
        collector.addEntry(PreferenceHelper.createEntry(preference, ModIntegration.getWidgetPrefText(preference)));
    }
}
