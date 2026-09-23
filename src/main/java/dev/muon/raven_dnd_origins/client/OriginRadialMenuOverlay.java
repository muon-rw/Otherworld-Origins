package dev.muon.raven_dnd_origins.client;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.screen.OriginDetailPanel;
import dev.muon.raven_dnd_origins.client.screen.ShapeshiftFormPreview;
import dev.muon.raven_dnd_origins.mixin.client.compat.apoli.RadialMenuAccessor;
import dev.muon.raven_dnd_origins.mixin.client.compat.apoli.RadialMenuScreenAccessor;
import dev.overgrown.apoli.client.radial.RadialMenu;
import dev.overgrown.apoli.client.radial.RadialMenuScreen;
import dev.overgrown.apoli.network.payload.RadialMenuOpenS2C;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Adds the origin description panel and spinning form model to an Apoli radial menu whose entry
 * tooltips are origin names ({@code origin.<namespace>.<path>.name}), such as wildshape selection.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID)
public final class OriginRadialMenuOverlay {

    private static final String ORIGIN_NAME_PREFIX = "origin.";
    private static final String ORIGIN_NAME_SUFFIX = ".name";
    private static final float MODEL_DEPTH_BEHIND_BUTTONS = -200.0f;
    private static final float MODEL_SIZE_PER_INNER_RADIUS = 1.4f;
    private static final int PANEL_EDGE_MARGIN = 10;
    private static final int PANEL_RING_GAP = 6;

    private static final class Session {
        private final RadialMenu menu;
        private final List<Origin> entryOrigins;
        private final OriginDetailPanel panel = new OriginDetailPanel();
        @Nullable
        private Origin shown;
        private float time;
        private boolean closeOnRelease;

        private Session(RadialMenu menu, List<Origin> entryOrigins) {
            this.menu = menu;
            this.entryOrigins = entryOrigins;
        }
    }

    @Nullable
    private static Screen tracked;
    @Nullable
    private static Session session;

    private OriginRadialMenuOverlay() {}

    @Nullable
    private static Session track(@Nullable Screen screen) {
        if (screen != tracked) {
            tracked = screen;
            session = null;
            if (screen instanceof RadialMenuScreen radial) {
                RadialMenu menu = ((RadialMenuScreenAccessor) radial).getRadialMenu();
                List<Origin> origins = resolveOrigins(entries(menu));
                if (origins.stream().anyMatch(Objects::nonNull)) {
                    session = new Session(menu, origins);
                }
            }
        }
        return session;
    }

    private static List<RadialMenuOpenS2C.Entry> entries(RadialMenu radialMenu) {
        return ((RadialMenuAccessor) radialMenu).getEntries();
    }

    private static List<Origin> resolveOrigins(List<RadialMenuOpenS2C.Entry> entries) {
        List<Origin> origins = new ArrayList<>(entries.size());
        for (RadialMenuOpenS2C.Entry entry : entries) {
            origins.add(entry.tooltip().map(OriginRadialMenuOverlay::originNamedBy).orElse(null));
        }
        return origins;
    }

    @Nullable
    private static Origin originNamedBy(Component tooltip) {
        if (!(tooltip.getContents() instanceof TranslatableContents translatable)) return null;
        String key = translatable.getKey();
        if (!key.startsWith(ORIGIN_NAME_PREFIX) || !key.endsWith(ORIGIN_NAME_SUFFIX)) return null;
        String id = key.substring(ORIGIN_NAME_PREFIX.length(), key.length() - ORIGIN_NAME_SUFFIX.length());
        int split = id.indexOf('.');
        if (split < 0) return null;
        ResourceLocation originId = ResourceLocation.tryBuild(id.substring(0, split), id.substring(split + 1));
        return originId != null ? OriginRegistry.get(originId) : null;
    }

    private static int entryDistance(RadialMenuOpenS2C.Entry entry, int guiHeight) {
        return entry.distance() != -1 ? entry.distance() : guiHeight / 4;
    }

    private static int modelSize(RadialMenu menu, int guiHeight) {
        int innerRadius = Integer.MAX_VALUE;
        for (RadialMenuOpenS2C.Entry entry : entries(menu)) {
            int halfButton = Math.max(entry.buttonWidth(), entry.buttonHeight()) / 2;
            innerRadius = Math.min(innerRadius, entryDistance(entry, guiHeight) - halfButton);
        }
        return Math.min(ShapeshiftFormPreview.DEFAULT_SIZE, (int) (innerRadius * MODEL_SIZE_PER_INNER_RADIUS));
    }

    private static int ringRightEdge(RadialMenu menu, int guiWidth, int guiHeight) {
        int reach = 0;
        for (RadialMenuOpenS2C.Entry entry : entries(menu)) {
            reach = Math.max(reach, entryDistance(entry, guiHeight) + entry.buttonWidth() / 2);
        }
        return guiWidth / 2 + reach;
    }

    @SubscribeEvent
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        Session s = track(event.getScreen());
        if (s == null) return;

        Button[] buttons = s.menu.getButtons();
        for (int i = 0; i < buttons.length; i++) {
            Origin origin = s.entryOrigins.get(i);
            if (buttons[i] == null || origin == null) continue;
            buttons[i].setTooltip(null);
            if (buttons[i].isHovered()) {
                s.shown = origin;
            }
        }

        s.time += event.getPartialTick();
        if (s.shown == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0f, 0.0f, MODEL_DEPTH_BEHIND_BUTTONS);
        ShapeshiftFormPreview.render(graphics, s.shown, graphics.guiWidth() / 2, graphics.guiHeight() / 2,
                modelSize(s.menu, graphics.guiHeight()), s.time);
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        Session s = track(event.getScreen());
        if (s == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int guiWidth = graphics.guiWidth();
        int x = Math.max(guiWidth - OriginDetailPanel.DEFAULT_WIDTH - PANEL_EDGE_MARGIN,
                ringRightEdge(s.menu, guiWidth, graphics.guiHeight()) + PANEL_RING_GAP);
        s.panel.render(graphics, s.shown, x, guiWidth - PANEL_EDGE_MARGIN - x,
                event.getMouseX(), event.getMouseY(), s.time);
    }

    /**
     * The toggle key closes the menu on release: closing on press leaves the key held with no screen
     * open, which Apoli reads as a fresh press and reopens the menu.
     */
    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        Session s = track(event.getScreen());
        if (s != null && ModKeybinds.CANTRIP_THREE_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            s.closeOnRelease = true;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        Session s = track(event.getScreen());
        if (s != null && s.closeOnRelease
                && ModKeybinds.CANTRIP_THREE_KEY.matches(event.getKeyCode(), event.getScanCode())) {
            event.getScreen().onClose();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Session s = track(event.getScreen());
        if (s == null) return;
        if (ModKeybinds.CANTRIP_THREE_KEY.matchesMouse(event.getButton())) {
            s.closeOnRelease = true;
            event.setCanceled(true);
        } else if (s.panel.mouseClicked(event.getMouseX(), event.getMouseY(), event.getButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        Session s = track(event.getScreen());
        if (s != null && s.panel.mouseDragged(event.getMouseY(), event.getMouseButton())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        Session s = track(event.getScreen());
        if (s == null) return;
        if (s.closeOnRelease && ModKeybinds.CANTRIP_THREE_KEY.matchesMouse(event.getButton())) {
            event.getScreen().onClose();
            event.setCanceled(true);
        } else {
            s.panel.mouseReleased();
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        Session s = track(event.getScreen());
        if (s != null && s.panel.mouseScrolled(event.getMouseX(), event.getMouseY(), event.getScrollDeltaY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Session s = track(Minecraft.getInstance().screen);
        if (s != null && s.shown != null) {
            ShapeshiftFormPreview.tickIdle(s.shown);
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        track(null);
    }
}
