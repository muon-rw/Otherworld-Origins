package dev.muon.raven_dnd_origins.client.debug;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.screen.RavenDndOriginScreen;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dev-only screenshot harness for the origin screen. Set {@code RDO_UI_HARNESS} to {@code all} (or a
 * comma list of preset names) and launch the client: we load {@code RDO_UI_HARNESS_WORLD}, seed
 * client-side picks for each preset (nothing is sent to the server), capture every preset at each
 * window size into {@code screenshots/rdo_<preset>_<gui size>.png}, then quit.
 */
@EventBusSubscriber(value = Dist.CLIENT, modid = RavenDndOrigins.MODID)
public final class OriginScreenHarness {

    private static final String SPEC = System.getenv("RDO_UI_HARNESS");
    private static final String WORLD = Objects.requireNonNullElse(System.getenv("RDO_UI_HARNESS_WORLD"), "New World");
    private static final int SETTLE_TICKS = 20;
    private static final int WORLD_WARMUP_TICKS = 100;

    private record Size(int windowWidth, int windowHeight, int guiScale) {}

    /** Auto GUI scale keeps the GUI near 320x240..480x270, so most sizes here land in that band on purpose. */
    private static final List<Size> SIZES = List.of(
            new Size(640, 480, 0),
            new Size(854, 480, 0),
            new Size(1146, 480, 0),
            new Size(1920, 1080, 0),
            new Size(1920, 1080, 3),
            new Size(1920, 1080, 2));

    private enum Preset {
        START(Map.of(), "race/elf", false),
        SUBCLASS(midCreation(), "subclass/wizard/evocation", false),
        INK(midCreation(), "subclass/wizard/evocation", false),
        REVIEW(fullCharacter(), null, false),
        OVERFLOW(Map.of("race", "race/human_variant"), null, false),
        LEVELUP(levelUpCharacter(), null, false),
        VIEW(fullCharacter(), null, true);

        final Map<String, String> picks;
        final String previewSelection;
        final boolean view;

        Preset(Map<String, String> picks, String previewSelection, boolean view) {
            this.picks = picks;
            this.previewSelection = previewSelection;
            this.view = view;
        }

        private static Map<String, String> midCreation() {
            return Map.of(
                    "race", "race/elf",
                    "plus_one_aptitude_one", "apts/one/dex",
                    "plus_one_aptitude_two", "apts/two/int",
                    "subrace", "subrace/elf/high",
                    "cantrip_one", "cantrips/chain_lightning",
                    "class", "class/wizard");
        }

        private static Map<String, String> levelUpCharacter() {
            Map<String, String> picks = new LinkedHashMap<>(fullCharacter());
            picks.remove("cantrip_two");
            return picks;
        }

        private static Map<String, String> fullCharacter() {
            Map<String, String> picks = new LinkedHashMap<>();
            picks.put("race", "race/elf");
            picks.put("subrace", "subrace/elf/high");
            picks.put("class", "class/wizard");
            picks.put("subclass", "subclass/wizard/evocation");
            picks.put("cantrip_one", "cantrips/chain_lightning");
            picks.put("cantrip_two", "cantrips/two/gust");
            picks.put("plus_one_aptitude_one", "apts/one/dex");
            picks.put("plus_one_aptitude_two", "apts/two/int");
            picks.put("first_feat", "feats/lucky");
            return picks;
        }
    }

    private record Shot(Preset preset, Size size) {}

    private static final List<Shot> SHOTS = new ArrayList<>();
    private static boolean worldRequested;
    private static int ticksInWorld;
    private static int shotIndex = -1;
    private static int wait;
    private static boolean shotTaken;

    private OriginScreenHarness() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (SPEC == null) return;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) {
            if (!worldRequested && mc.screen instanceof TitleScreen) {
                worldRequested = true;
                RavenDndOrigins.LOGGER.info("[UiHarness] opening world '{}'", WORLD);
                mc.createWorldOpenFlows().openWorld(WORLD, () -> RavenDndOrigins.LOGGER.error("[UiHarness] could not open '{}'", WORLD));
            }
            return;
        }
        if (++ticksInWorld < WORLD_WARMUP_TICKS || OriginLayers.enabledOrdered().isEmpty()) return;

        if (shotIndex < 0) {
            planShots();
            shotIndex = 0;
            beginShot(mc);
            return;
        }
        if (--wait > 0) {
            // About 300ms before the capture, so the reveal is caught mid-stroke.
            if (wait == 6 && SHOTS.get(shotIndex).preset() == Preset.INK && mc.screen instanceof RavenDndOriginScreen screen) {
                screen.replayInkForPreview(48);
            }
            return;
        }

        if (!shotTaken) {
            Shot shot = SHOTS.get(shotIndex);
            String name = "rdo_%s_%dx%d.png".formatted(shot.preset().name().toLowerCase(),
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                    message -> RavenDndOrigins.LOGGER.info("[UiHarness] {}", message.getString()));
            shotTaken = true;
            wait = 4;
            return;
        }

        if (++shotIndex >= SHOTS.size()) {
            RavenDndOrigins.LOGGER.info("[UiHarness] done, {} screenshots", SHOTS.size());
            mc.stop();
            return;
        }
        beginShot(mc);
    }

    private static void planShots() {
        List<Preset> presets = new ArrayList<>();
        for (Preset preset : Preset.values()) {
            if (SPEC.equalsIgnoreCase("all")
                    || java.util.Arrays.stream(SPEC.split(",")).anyMatch(name -> name.trim().equalsIgnoreCase(preset.name()))) {
                presets.add(preset);
            }
        }
        for (Size size : SIZES) {
            for (Preset preset : presets) {
                SHOTS.add(new Shot(preset, size));
            }
        }
    }

    private static void beginShot(Minecraft mc) {
        Shot shot = SHOTS.get(shotIndex);
        Size size = shot.size();
        if (mc.options.guiScale().get() != size.guiScale()) {
            mc.options.guiScale().set(size.guiScale());
        }
        if (mc.getWindow().getWidth() != size.windowWidth() || mc.getWindow().getHeight() != size.windowHeight()) {
            mc.getWindow().setWindowed(size.windowWidth(), size.windowHeight());
        }
        mc.resizeDisplay();
        mc.getToasts().clear();
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, size.windowHeight() - 1);

        seedPicks(mc, shot.preset().picks);
        if (shot.preset() == Preset.REVIEW) {
            fillRemainingLayers(mc, null);
        } else if (shot.preset() == Preset.OVERFLOW) {
            fillRemainingLayers(mc, RavenDndOrigins.loc("free_feat"));
        }
        if (shot.preset().view) {
            mc.setScreen(RavenDndOriginScreen.forViewing(mc.player));
        } else if (shot.preset() == Preset.LEVELUP) {
            OriginLayer cantripTwo = OriginLayers.get(RavenDndOrigins.loc("cantrip_two"));
            mc.setScreen(new RavenDndOriginScreen(new ArrayList<>(List.of(cantripTwo)), 0, SessionKind.LEVEL_UP));
        } else {
            RavenDndOriginScreen screen = new RavenDndOriginScreen(
                    new ArrayList<>(OriginLayers.enabledOrdered()), 0, SessionKind.INITIAL_CREATION);
            mc.setScreen(screen);
            if (shot.preset().previewSelection != null) {
                screen.selectForPreview(RavenDndOrigins.loc(shot.preset().previewSelection));
            }
        }
        shotTaken = false;
        wait = SETTLE_TICKS;
    }

    /** Picks the first valid option for every still-empty layer, mirroring how the screen evaluates them in order. */
    private static void fillRemainingLayers(Minecraft mc, @Nullable ResourceLocation stopBefore) {
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (layer.id().equals(stopBefore)) return;
            Map<ResourceLocation, ResourceLocation> current = new HashMap<>(OriginsClientState.get(mc.player.getUUID()));
            ResourceLocation existing = current.get(layer.id());
            if (existing != null && !existing.equals(OriginRegistry.EMPTY_ID)) continue;
            for (ResourceLocation originId : layer.availableOrigins(mc.player)) {
                Origin origin = OriginRegistry.get(originId);
                if (origin != null && origin.choosable() && OriginManager.availableTo(mc.player, layer.id(), originId)) {
                    current.put(layer.id(), originId);
                    OriginsClientState.setOrigins(mc.player.getUUID(), current);
                    break;
                }
            }
        }
    }

    private static void seedPicks(Minecraft mc, Map<String, String> picks) {
        Map<ResourceLocation, ResourceLocation> seeded = new HashMap<>();
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            seeded.put(layer.id(), OriginRegistry.EMPTY_ID);
        }
        picks.forEach((layer, origin) -> seeded.put(RavenDndOrigins.loc(layer), RavenDndOrigins.loc(origin)));
        OriginsClientState.setOrigins(mc.player.getUUID(), seeded);
    }
}
