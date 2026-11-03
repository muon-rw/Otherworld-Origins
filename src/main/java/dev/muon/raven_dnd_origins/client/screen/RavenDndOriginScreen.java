package dev.muon.raven_dnd_origins.client.screen;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.locale.Language;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import com.mojang.realmsclient.RealmsMainScreen;
import dev.muon.raven_dnd_origins.client.RavenDndOriginsClientEvents;
import dev.muon.raven_dnd_origins.network.C2SRevertLayerOriginsMessage;
import dev.muon.raven_dnd_origins.network.SelectionSessionFinishedMessage;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.network.OriginsClientNetwork;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginLayers;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RavenDndOriginScreen extends Screen {

    /**
     * Matches stock {@code ChooseOriginScreen} (impact, then datapack {@code order}), with a final
     * stable tie-breaker so lists stay consistent when multiple origins share impact and default order.
     */
    private static final Comparator<Origin> CHOOSABLE_ORIGIN_ORDER = Comparator
            .comparingInt((Origin o) -> o.impact().level())
            .thenComparingInt(Origin::order)
            .thenComparing(o -> o.id().toString());

    private static final ResourceLocation LAYER_CANTRIP_ONE = RavenDndOrigins.loc("cantrip_one");
    private static final ResourceLocation LAYER_CANTRIP_TWO = RavenDndOrigins.loc("cantrip_two");
    private static final ResourceLocation LAYER_CLASS = RavenDndOrigins.loc("class");
    private static final ResourceLocation LAYER_DRACONIC_ANCESTRY = RavenDndOrigins.loc("draconic_ancestry");
    private static final ResourceLocation LAYER_ELEMENTAL_DISCIPLINE_FOUR = RavenDndOrigins.loc("elemental_discipline_four");
    private static final ResourceLocation LAYER_ELEMENTAL_DISCIPLINE_ONE = RavenDndOrigins.loc("elemental_discipline_one");
    private static final ResourceLocation LAYER_ELEMENTAL_DISCIPLINE_THREE = RavenDndOrigins.loc("elemental_discipline_three");
    private static final ResourceLocation LAYER_ELEMENTAL_DISCIPLINE_TWO = RavenDndOrigins.loc("elemental_discipline_two");
    private static final ResourceLocation LAYER_FIFTH_FEAT = RavenDndOrigins.loc("fifth_feat");
    private static final ResourceLocation LAYER_FIRST_FEAT = RavenDndOrigins.loc("first_feat");
    private static final ResourceLocation LAYER_FOURTH_FEAT = RavenDndOrigins.loc("fourth_feat");
    private static final ResourceLocation LAYER_FREE_FEAT = RavenDndOrigins.loc("free_feat");
    private static final ResourceLocation LAYER_PLUS_ONE_APTITUDE_ONE = RavenDndOrigins.loc("plus_one_aptitude_one");
    private static final ResourceLocation LAYER_PLUS_ONE_APTITUDE_RESILIENT = RavenDndOrigins.loc("plus_one_aptitude_resilient");
    private static final ResourceLocation LAYER_PLUS_ONE_APTITUDE_TWO = RavenDndOrigins.loc("plus_one_aptitude_two");
    private static final ResourceLocation LAYER_PLUS_TWO_APTITUDE_ONE = RavenDndOrigins.loc("plus_two_aptitude_one");
    private static final ResourceLocation LAYER_PLUS_TWO_APTITUDE_TWO = RavenDndOrigins.loc("plus_two_aptitude_two");
    private static final ResourceLocation LAYER_RACE = RavenDndOrigins.loc("race");
    private static final ResourceLocation LAYER_SECOND_FEAT = RavenDndOrigins.loc("second_feat");
    private static final ResourceLocation LAYER_SUBCLASS = RavenDndOrigins.loc("subclass");
    private static final ResourceLocation LAYER_SUBRACE = RavenDndOrigins.loc("subrace");
    private static final ResourceLocation LAYER_THIRD_FEAT = RavenDndOrigins.loc("third_feat");
    private static final ResourceLocation LAYER_WILDSHAPE = RavenDndOrigins.loc("wildshape");
    private static final ResourceLocation RACE_OTHER = RavenDndOrigins.loc("race/other");
    private static final ResourceLocation RACE_UNDEAD = RavenDndOrigins.loc("race/undead");

    private static final Map<ResourceLocation, ResourceLocation> RAIL_ROW_PARTNERS = Map.ofEntries(
            Map.entry(LAYER_RACE, LAYER_SUBRACE), Map.entry(LAYER_SUBRACE, LAYER_RACE),
            Map.entry(LAYER_CLASS, LAYER_SUBCLASS), Map.entry(LAYER_SUBCLASS, LAYER_CLASS),
            Map.entry(LAYER_CANTRIP_ONE, LAYER_CANTRIP_TWO), Map.entry(LAYER_CANTRIP_TWO, LAYER_CANTRIP_ONE),
            Map.entry(LAYER_PLUS_TWO_APTITUDE_ONE, LAYER_PLUS_TWO_APTITUDE_TWO), Map.entry(LAYER_PLUS_TWO_APTITUDE_TWO, LAYER_PLUS_TWO_APTITUDE_ONE),
            Map.entry(LAYER_PLUS_ONE_APTITUDE_ONE, LAYER_PLUS_ONE_APTITUDE_TWO), Map.entry(LAYER_PLUS_ONE_APTITUDE_TWO, LAYER_PLUS_ONE_APTITUDE_ONE),
            Map.entry(LAYER_FIRST_FEAT, LAYER_SECOND_FEAT), Map.entry(LAYER_SECOND_FEAT, LAYER_FIRST_FEAT),
            Map.entry(LAYER_THIRD_FEAT, LAYER_FOURTH_FEAT), Map.entry(LAYER_FOURTH_FEAT, LAYER_THIRD_FEAT),
            Map.entry(LAYER_ELEMENTAL_DISCIPLINE_ONE, LAYER_ELEMENTAL_DISCIPLINE_TWO), Map.entry(LAYER_ELEMENTAL_DISCIPLINE_TWO, LAYER_ELEMENTAL_DISCIPLINE_ONE),
            Map.entry(LAYER_ELEMENTAL_DISCIPLINE_THREE, LAYER_ELEMENTAL_DISCIPLINE_FOUR), Map.entry(LAYER_ELEMENTAL_DISCIPLINE_FOUR, LAYER_ELEMENTAL_DISCIPLINE_THREE));

    private final List<OriginLayer> layerList;
    private int currentLayerIndex;

    private final Map<Integer, Origin> confirmedSelections = new HashMap<>();
    private final Map<Integer, List<Origin>> layerOriginCache = new HashMap<>();

    private Origin hoveredOrigin = null;
    private Origin selectedOrigin = null;

    private float[] cardExpandProgress = new float[0];
    @Nullable
    private Integer hoveredCompletedLayerIndex = null;
    private int activeLayerOptionsScrollY = 0;
    private int activeLayerOptionsMaxScrollY = 0;
    private int sheetScrollY = 0;
    private int sheetMaxScrollY = 0;
    private float time = 0.0f;

    private enum SheetLineKind { TEXT, GAP, PENCIL }

    private record SheetLine(SheetLineKind kind, FormattedCharSequence text, String plain, int length) {}

    private final List<SheetLine> sheetLines = new ArrayList<>();
    private List<String> previousSheetText = List.of();
    /** Per sheet line: offset into the current reveal, or -1 for a line that was already on the sheet. */
    private int[] lineInkStart = new int[0];
    private int inkTotalChars;
    private final SessionKind kind;

    private OriginScreenLayout layout;
    private boolean layerEvaluated;
    private int sheetTextWidth = 190;

    private record RailSlot(int layerIndex, OriginScreenLayout.Rect rect) {}

    private record OptionSlot(Origin origin, OriginScreenLayout.Rect rect) {}

    /** Rebuilt every frame by the render pass so clicks hit exactly what was drawn. */
    private final List<RailSlot> railSlots = new ArrayList<>();
    private final List<OptionSlot> optionSlots = new ArrayList<>();
    @Nullable
    private OriginScreenLayout.Rect optionsViewport;
    @Nullable
    private OriginScreenLayout.Rect trayRect;
    private boolean compactRail;
    @Nullable
    private OriginScreenLayout.Rect trayScrollbar;
    private boolean draggingTrayScrollbar;
    /** Every layer is picked but the session stays open, so seals can still be reverted before the player commits. */
    private boolean reviewing;

    private boolean animateNextSheetChange;
    private long inkStartMillis;
    private int armedRevertIndex = -1;
    private final Map<ResourceLocation, ResourceLocation> revertedPicksToPreselect = new HashMap<>();
    private int keyboardFocusedOption = -1;
    private boolean scrollToKeyboardFocus;
    private int optionColumns = 1;
    private int keyboardFocusedSeal = -1;
    @Nullable
    private OriginScreenLayout.Rect anchoredSealRect;
    private boolean tooltipAnchoredToSeal;
    private long lastLayerChangeMillis;
    private int trayThumbY;
    private int trayThumbHeight;
    private int trayThumbGrabOffset;
    @Nullable
    private List<int[]> completedGroupsCache;
    private final Map<Integer, String> groupLabelCache = new HashMap<>();
    private final Map<ResourceLocation, Boolean> previewableCache = new HashMap<>();
    private int titleCacheLayer = -1;
    private int titleCacheWidth = -1;
    private List<FormattedCharSequence> titleCacheLines = List.of();

    /** Queued during active-layer row render (under scissor); drawn after the scissor ends. */
    private boolean pendingConfirmSelectionHint;
    private int pendingConfirmHintScreenX;
    private int pendingConfirmHintScreenY;

    private static final int CARD_COLLAPSED_WIDTH = 16;
    private static final int CARD_EXPANDED_WIDTH = 32;
    private static final int CARD_HEIGHT = 32;
    private static final int OPTION_CELL = 22;
    private static final int RAIL_ROW_HEIGHT = 26;
    private static final int COMPACT_SEAL_STEP = 12;
    private static final int COMPACT_MIN_ROW = 14;
    private static final int COMPACT_MAX_ROW = 22;
    private static final int LIST_MODE_MIN_WIDTH = 130;
    private static final int LIST_ROW_HEIGHT = 22;
    private static final int MIN_CARD_WIDTH = 12;
    /** We alphabetize longer option lists so players can find a name by scanning or typing. */
    private static final int ALPHABETICAL_ABOVE = 12;
    /** A revert that would clear at least this many later picks needs a second click on the same seal. */
    private static final int REVERT_CONFIRM_THRESHOLD = 3;
    /** We ignore Enter for this long after a layer change, so a held or repeating key can't run through picks and the review. */
    private static final long ENTER_DEBOUNCE_MS = 350;
    private static final int MIN_TRAY_HEIGHT = 88;
    private static final int TRAY_SCROLLBAR_WIDTH = 5;
    private static final ResourceLocation REVIEW_BUTTON_KEY = RavenDndOrigins.loc("review");
    private static final int TRAY_PADDING = 7;
    private static final float INK_MIN_CHARS_PER_MS = 0.05f;
    private static final long INK_MAX_DURATION_MS = 1500;
    private static final int INK_WET_TRAIL = 8;
    private static final ItemStack QUILL = new ItemStack(Items.FEATHER);

    /** Places the confirm hint on whichever side of the cursor has room, so it doesn't clip when the cursor is over the rail. */
    private final ClientTooltipPositioner confirmHintTooltipPositioner =
            (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) ->
                    computeConfirmHintTooltipPos(screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight);

    private static Vector2i computeConfirmHintTooltipPos(
            int sw, int sh, int mx, int my, int tw, int th) {
        final int margin = 4;
        final int gap = 12;
        int preferLeftX = mx - gap - tw;
        int preferRightX = mx + gap;
        boolean fitsLeft = preferLeftX >= margin && preferLeftX + tw <= sw - margin;
        boolean fitsRight = preferRightX >= margin && preferRightX + tw <= sw - margin;
        int x;
        if (fitsLeft && (!fitsRight || mx >= sw - mx)) {
            x = preferLeftX;
        } else if (fitsRight) {
            x = preferRightX;
        } else if (preferLeftX >= preferRightX) {
            x = Mth.clamp(preferLeftX, margin, Math.max(margin, sw - margin - tw));
        } else {
            x = Mth.clamp(preferRightX, margin, Math.max(margin, sw - margin - tw));
        }
        int y = my - 12;
        if (y < margin) {
            y = margin;
        } else if (y + th + 3 > sh) {
            y = sh - th - 3;
        }
        return new Vector2i(x, y);
    }

    @Nullable
    private DeskButton selectButton;
    @Nullable
    private ResourceLocation selectButtonOriginId;
    private final OriginDetailPanel detailPanel = new OriginDetailPanel();

    private final boolean readOnly;

    public RavenDndOriginScreen(List<OriginLayer> layerList, int startLayerIndex, SessionKind kind) {
        super(Component.translatable("origins.screen.choose_origin"));
        this.layerList = new ArrayList<>(layerList);
        this.currentLayerIndex = startLayerIndex;
        this.kind = kind;
        this.readOnly = false;
    }

    private RavenDndOriginScreen(List<OriginLayer> layerList, Map<Integer, Origin> picks) {
        super(Component.translatable("origins.screen.view_origin"));
        this.layerList = layerList;
        this.currentLayerIndex = layerList.size();
        this.kind = SessionKind.INITIAL_CREATION;
        this.readOnly = true;
        this.confirmedSelections.putAll(picks);
        this.selectedOrigin = picks.get(0);
    }

    /** Shows every chosen origin in the creation layout, with completed slots selecting instead of reverting. */
    public static RavenDndOriginScreen forViewing(Player player) {
        Map<ResourceLocation, ResourceLocation> picks = OriginsClientState.get(player.getUUID());
        Map<ResourceLocation, ResourceLocation> swaps = OriginsClientState.getSwaps(player.getUUID());
        List<OriginLayer> layers = new ArrayList<>();
        Map<Integer, Origin> chosen = new HashMap<>();
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (layer.hidden() || layer.swappable()) continue;
            ResourceLocation originId = swaps.getOrDefault(layer.id(),
                    picks.getOrDefault(layer.id(), OriginRegistry.EMPTY_ID));
            if (originId.equals(OriginRegistry.EMPTY_ID)) continue;
            Origin origin = OriginRegistry.get(originId);
            if (origin == null) continue;
            chosen.put(layers.size(), origin);
            layers.add(layer);
        }
        return new RavenDndOriginScreen(layers, chosen);
    }

    /** Lets the UI harness capture the ink-write reveal without confirming a pick over the network. */
    public void replayInkForPreview(int characters) {
        java.util.Arrays.fill(this.lineInkStart, -1);
        int remaining = characters;
        for (int i = this.sheetLines.size() - 1; i >= 0 && remaining > 0; i--) {
            SheetLine line = this.sheetLines.get(i);
            if (line.kind() != SheetLineKind.TEXT) continue;
            this.lineInkStart[i] = 0;
            remaining -= line.length();
        }
        this.inkTotalChars = 0;
        for (int i = 0; i < this.sheetLines.size(); i++) {
            if (this.lineInkStart[i] < 0) continue;
            this.lineInkStart[i] = this.inkTotalChars;
            this.inkTotalChars += this.sheetLines.get(i).length();
        }
        this.inkStartMillis = Util.getMillis();
    }

    /** Lets the UI harness show a selected option without simulating clicks. */
    public void selectForPreview(ResourceLocation originId) {
        List<Origin> options = this.layerOriginCache.get(this.currentLayerIndex);
        if (options == null) return;
        for (Origin option : options) {
            if (option.id().equals(originId)) {
                this.selectedOrigin = option;
                updateButtonStates();
                return;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.layout = OriginScreenLayout.compute(this.width, this.height, true);
        OriginScreenLayout.Rect sheet = this.layout.sheet();
        this.sheetTextWidth = sheet == null ? 190 : sheet.width() - 2 * sheetPadding(sheet);

        OriginScreenLayout.Rect b = this.layout.button();
        if (this.readOnly) {
            this.addRenderableWidget(new DeskButton(b.x(), b.y(), b.width(), b.height(),
                    CommonComponents.GUI_DONE, button -> onClose()));
        } else {
            this.selectButton = this.addRenderableWidget(new DeskButton(b.x(), b.y(), b.width(), b.height(),
                    Component.translatable("origins.gui.select"), button -> pressSelectButton()));
            this.selectButtonOriginId = null;
        }

        invalidateRailCaches();
        // Screen#init also runs on every resize; only the first one may advance the session.
        if (!this.layerEvaluated && !this.readOnly) {
            this.layerEvaluated = true;
            evaluateCurrentLayer();
            RavenDndOrigins.LOGGER.debug("[OWOriginScreen] init: {} layers, currentLayerIndex={}, kind={}, confirmedSelections={}",
                    this.layerList.size(), this.currentLayerIndex, this.kind, this.confirmedSelections.size());
        } else {
            rebuildCharacterSheetText();
        }
        updateButtonStates();
    }

    /** Insets that keep text off the scroll's rolled edges, scaled with the stretched texture. */
    private static int sheetPadding(OriginScreenLayout.Rect sheet) {
        return Math.max(20, sheet.width() * 33 / DeskStyle.SHEET_SIZE);
    }

    private static int sheetTopInset(OriginScreenLayout.Rect sheet) {
        return Math.max(26, sheet.height() * 38 / DeskStyle.SHEET_SIZE);
    }

    private static int sheetBottomInset(OriginScreenLayout.Rect sheet) {
        return Math.max(22, sheet.height() * 32 / DeskStyle.SHEET_SIZE);
    }

    private static boolean sameOrigin(@Nullable Origin a, @Nullable Origin b) {
        return a != null && b != null && a.id().equals(b.id());
    }

    private void evaluateCurrentLayer() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        this.reviewing = false;
        this.armedRevertIndex = -1;
        this.keyboardFocusedOption = -1;
        this.keyboardFocusedSeal = -1;
        this.lastLayerChangeMillis = Util.getMillis();
        invalidateRailCaches();
        setFocused(null);
        if (this.currentLayerIndex >= this.layerList.size() && !appendUnlockedLayers(player)) {
            if (offersReview()) {
                enterReview();
            } else {
                finishSelection();
            }
            return;
        }

        OriginLayer currentLayer = this.layerList.get(this.currentLayerIndex);

        ResourceLocation existing = OriginsClientState.get(player.getUUID()).get(currentLayer.id());
        if (existing != null && !existing.equals(OriginRegistry.EMPTY_ID)) {
            Origin existingOrigin = OriginRegistry.get(existing);
            if (existingOrigin != null) {
                this.confirmedSelections.put(this.currentLayerIndex, existingOrigin);
                this.currentLayerIndex++;
                evaluateCurrentLayer();
                return;
            }
        }

        List<Origin> availableOrigins = new ArrayList<>();
        for (ResourceLocation originId : currentLayer.availableOrigins(player)) {
            Origin origin = OriginRegistry.get(originId);
            if (origin != null && origin.choosable()
                    && OriginManager.availableTo(player, currentLayer.id(), originId)) {
                availableOrigins.add(origin);
            }
        }
        availableOrigins.sort(CHOOSABLE_ORIGIN_ORDER);
        if (availableOrigins.size() > ALPHABETICAL_ABOVE) {
            availableOrigins.sort(Comparator.comparing(o -> o.name().getString(), localeCollator()));
        }

        if (availableOrigins.isEmpty()) {
            this.currentLayerIndex++;
            evaluateCurrentLayer();
            return;
        }

        this.layerOriginCache.put(this.currentLayerIndex, availableOrigins);
        this.cardExpandProgress = new float[availableOrigins.size()];
        ResourceLocation remembered = this.revertedPicksToPreselect.remove(currentLayer.id());
        this.selectedOrigin = remembered == null ? null : availableOrigins.stream()
                .filter(o -> o.id().equals(remembered)).findFirst().orElse(null);
        this.hoveredOrigin = null;
        this.detailPanel.resetScroll();
        this.activeLayerOptionsScrollY = 0;

        updateButtonStates();
        rebuildCharacterSheetText();
    }

    /**
     * Appends empty layers that this session's picks unlocked, e.g. Resilient's ability score after a
     * level-up feat pick; the session only lists layers that were valid when it opened.
     */
    private boolean appendUnlockedLayers(Player player) {
        Map<ResourceLocation, ResourceLocation> picks = OriginsClientState.get(player.getUUID());
        boolean appended = false;
        for (OriginLayer layer : OriginLayers.enabledOrdered()) {
            if (this.layerList.stream().anyMatch(l -> l.id().equals(layer.id()))) continue;
            ResourceLocation existing = picks.get(layer.id());
            if (existing != null && !existing.equals(OriginRegistry.EMPTY_ID)) continue;
            if (OriginManager.hasChoosableOrigins(player, layer)) {
                this.layerList.add(layer);
                appended = true;
            }
        }
        return appended;
    }

    /** Mirrors the server-side pick locally so the next layer's conditions see it before the sync lands. */
    private static void setClientPick(Player player, ResourceLocation layerId, ResourceLocation originId) {
        Map<ResourceLocation, ResourceLocation> picks =
                new HashMap<>(OriginsClientState.get(player.getUUID()));
        picks.put(layerId, originId);
        OriginsClientState.setOrigins(player.getUUID(), picks);
    }

    private void confirmSelection() {
        if (this.selectedOrigin == null || this.currentLayerIndex >= this.layerList.size()) return;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        OriginLayer currentLayer = this.layerList.get(this.currentLayerIndex);
        OriginsClientNetwork.sendChoose(currentLayer.id(), this.selectedOrigin.id(), false);
        setClientPick(player, currentLayer.id(), this.selectedOrigin.id());

        this.confirmedSelections.put(this.currentLayerIndex, this.selectedOrigin);

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));

        this.animateNextSheetChange = true;
        this.currentLayerIndex++;
        evaluateCurrentLayer();
        triggerImmediateNarration(false);
    }

    /** Creation and reselection replace earlier picks wholesale, so they end on a review; level-ups only add. */
    private boolean offersReview() {
        return !this.readOnly && (this.kind == SessionKind.INITIAL_CREATION || this.kind == SessionKind.RESELECTION);
    }

    private void enterReview() {
        this.reviewing = true;
        this.lastLayerChangeMillis = Util.getMillis();
        triggerImmediateNarration(false);
        this.selectedOrigin = null;
        this.hoveredOrigin = null;
        this.detailPanel.resetScroll();
        rebuildCharacterSheetText();
        updateButtonStates();
    }

    private void pressSelectButton() {
        if (this.reviewing) {
            finishSelection();
        } else {
            confirmSelection();
        }
    }

    private void clickCompletedSlot(int index) {
        if (this.readOnly) {
            this.selectedOrigin = this.confirmedSelections.get(index);
            this.detailPanel.resetScroll();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        } else if (this.armedRevertIndex == index || laterChoiceCount(index) < REVERT_CONFIRM_THRESHOLD) {
            revertToLayer(index);
        } else {
            armRevert(index);
        }
    }

    private void revertToLayer(int index) {
        if (index >= this.currentLayerIndex) return;

        List<ResourceLocation> layersToRevert = new ArrayList<>();
        this.armedRevertIndex = -1;
        this.keyboardFocusedSeal = -1;
        // Only layers that actually hold a pick; the server clears what its session allows and resyncs the rest.
        for (int i = index; i < this.layerList.size(); i++) {
            Origin reverted = this.confirmedSelections.remove(i);
            if (reverted != null) {
                this.revertedPicksToPreselect.put(this.layerList.get(i).id(), reverted.id());
                layersToRevert.add(this.layerList.get(i).id());
            }
            this.layerOriginCache.remove(i);
        }

        PacketDistributor.sendToServer(new C2SRevertLayerOriginsMessage(layersToRevert));

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            for (ResourceLocation layerId : layersToRevert) {
                setClientPick(player, layerId, OriginRegistry.EMPTY_ID);
            }
        }

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));

        this.currentLayerIndex = index;
        evaluateCurrentLayer();
    }

    private void finishSelection() {
        if (!this.readOnly) {
            RavenDndOriginsClientEvents.markSessionFinished();
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F));
        Minecraft.getInstance().setScreen(null);
        PacketDistributor.sendToServer(new SelectionSessionFinishedMessage());
    }

    private void updateButtonStates() {
        if (this.selectButton != null) {
            this.selectButton.active = this.reviewing || this.selectedOrigin != null;
        }
    }

    /**
     * Returns the origin shown on the detail page: a hovered or keyboard-focused seal first, then a hovered or
     * keyboard-focused option, then the current selection.
     */
    @Nullable
    private Origin getRightPanelDisplayOrigin() {
        if (this.hoveredCompletedLayerIndex != null) {
            Origin o = this.confirmedSelections.get(this.hoveredCompletedLayerIndex);
            if (o != null) {
                return o;
            }
        }
        if (this.hoveredOrigin != null) {
            return this.hoveredOrigin;
        }
        return this.selectedOrigin;
    }

    private boolean isPortraitLayer(OriginLayer layer) {
        ResourceLocation id = layer.id();
        return id.equals(LAYER_RACE) || id.equals(LAYER_SUBRACE);
    }

    private int findLayerIndexForId(ResourceLocation layerId) {
        for (int i = 0; i < this.layerList.size(); i++) {
            if (layerId.equals(this.layerList.get(i).id())) return i;
        }
        return -1;
    }

    /**
     * Human variant: +2/+2 stat picks ({@code plus_two_aptitude_*}) and free feat share one completed row.
     */
    private boolean isAptitudeFreeTripleComplete() {
        int apt1 = findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_ONE);
        int apt2 = findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_TWO);
        int free = findLayerIndexForId(LAYER_FREE_FEAT);
        if (apt1 < 0 || apt2 < 0 || free < 0) return false;
        if (apt1 >= this.currentLayerIndex || apt2 >= this.currentLayerIndex || free >= this.currentLayerIndex) {
            return false;
        }
        return this.confirmedSelections.containsKey(apt1)
                && this.confirmedSelections.containsKey(apt2)
                && this.confirmedSelections.containsKey(free);
    }

    /** Layer index where the +2/+2/free-feat triple row is emitted (first of the three in stack order). */
    private int aptitudeFreeTripleAnchorIndex() {
        int apt1 = findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_ONE);
        int apt2 = findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_TWO);
        int free = findLayerIndexForId(LAYER_FREE_FEAT);
        if (apt1 < 0 || apt2 < 0 || free < 0) return -1;
        return Math.min(apt1, Math.min(apt2, free));
    }

    /** Draconic Bloodline: class, subclass, and draconic ancestry share one completed row (item icons). */
    private boolean isSubclassDraconicBloodline() {
        int subIdx = findLayerIndexForId(LAYER_SUBCLASS);
        if (subIdx < 0) return false;
        Origin sub = this.confirmedSelections.get(subIdx);
        return sub != null && sub.id().equals(RavenDndOrigins.loc("subclass/sorcerer/draconic_bloodline"));
    }

    private boolean isClassSubclassDraconicTripleComplete() {
        if (!isSubclassDraconicBloodline()) return false;
        int classIdx = findLayerIndexForId(LAYER_CLASS);
        int subIdx = findLayerIndexForId(LAYER_SUBCLASS);
        int dracIdx = findLayerIndexForId(LAYER_DRACONIC_ANCESTRY);
        if (classIdx < 0 || subIdx < 0 || dracIdx < 0) return false;
        if (classIdx >= this.currentLayerIndex || subIdx >= this.currentLayerIndex || dracIdx >= this.currentLayerIndex) {
            return false;
        }
        return this.confirmedSelections.containsKey(classIdx)
                && this.confirmedSelections.containsKey(subIdx)
                && this.confirmedSelections.containsKey(dracIdx);
    }

    private int classSubclassDraconicTripleAnchorIndex() {
        if (!isSubclassDraconicBloodline()) return -1;
        int classIdx = findLayerIndexForId(LAYER_CLASS);
        int subIdx = findLayerIndexForId(LAYER_SUBCLASS);
        int dracIdx = findLayerIndexForId(LAYER_DRACONIC_ANCESTRY);
        if (classIdx < 0 || subIdx < 0 || dracIdx < 0) return -1;
        return Math.min(classIdx, Math.min(subIdx, dracIdx));
    }

    private int[] computeFixedWidthCardWidths(int optionCount, int availableWidth, int progressOffset) {
        int effExpanded = Math.min(CARD_EXPANDED_WIDTH, availableWidth);
        int effCollapsed = optionCount <= 1
                ? CARD_COLLAPSED_WIDTH
                : Math.min(CARD_COLLAPSED_WIDTH, Math.max(MIN_CARD_WIDTH, (availableWidth - effExpanded) / (optionCount - 1)));
        int totalFixedWidth = optionCount * effCollapsed;

        float[] desired = new float[optionCount];
        float totalDesired = 0;
        for (int i = 0; i < optionCount; i++) {
            int card = progressOffset + i;
            float progress = card < this.cardExpandProgress.length ? this.cardExpandProgress[card] : 0;
            desired[i] = effCollapsed + progress * (effExpanded - effCollapsed);
            totalDesired += desired[i];
        }

        int[] widths = new int[optionCount];
        if (totalDesired > 0 && optionCount > 0) {
            float scale = (float) totalFixedWidth / totalDesired;
            float cumFloat = 0;
            int cumInt = 0;
            for (int i = 0; i < optionCount; i++) {
                cumFloat += desired[i] * scale;
                int newCumInt = Math.round(cumFloat);
                widths[i] = newCumInt - cumInt;
                cumInt = newCumInt;
            }
        }
        return widths;
    }

    @Override
    public void tick() {
        super.tick();
        updateAnimations();

        if (this.kind != SessionKind.INITIAL_CREATION) {
            Origin displayOrigin = getRightPanelDisplayOrigin();
            if (displayOrigin != null) {
                ShapeshiftFormPreview.tickIdle(displayOrigin);
            }
        }
    }

    private static final float CARD_ANIM_SPEED = 0.7f;
    private static final float CARD_ANIM_SNAP = 0.01f;

    private static float animStep(float current, float target) {
        float next = Mth.lerp(CARD_ANIM_SPEED, current, target);
        return Math.abs(next - target) < CARD_ANIM_SNAP ? target : next;
    }

    private void updateAnimations() {
        if (this.currentLayerIndex < this.layerList.size()) {
            List<Origin> options = this.layerOriginCache.get(this.currentLayerIndex);
            if (options != null && isPortraitLayer(this.layerList.get(this.currentLayerIndex))) {
                for (int i = 0; i < this.cardExpandProgress.length; i++) {
                    boolean isHovered = sameOrigin(this.hoveredOrigin, options.get(i));
                    this.cardExpandProgress[i] = animStep(this.cardExpandProgress[i], isHovered ? 1.0f : 0.0f);
                }
            }
        }
    }

    /** Subrace + race for "… heritage" / "a …" phrases; avoids double spaces when subrace is absent. */
    private static String heritagePhrase(@Nullable String subrace, String race) {
        if (subrace != null && !subrace.isEmpty()) {
            return subrace + " " + race;
        }
        return race;
    }

    private boolean selectedClassIsWarlock() {
        for (Map.Entry<Integer, Origin> entry : this.confirmedSelections.entrySet()) {
            int idx = entry.getKey();
            if (idx >= this.layerList.size()) continue;
            if (!LAYER_CLASS.equals(this.layerList.get(idx).id())) continue;
            Origin origin = entry.getValue();
            return origin != null && RavenDndOrigins.loc("class/warlock").equals(origin.id());
        }
        return false;
    }

    private void rebuildCharacterSheetText() {
        composeCharacterSheetText();
        updateInk();
    }

    private void composeCharacterSheetText() {
        this.sheetLines.clear();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Component playerName = player.getName().copy().withStyle(style -> style.withItalic(true).withColor(DeskStyle.RUBRIC & 0xFFFFFF));

        String race = getOriginDisplayName(LAYER_RACE);
        String subrace = getOriginDisplayName(LAYER_SUBRACE);
        String className = getOriginDisplayName(LAYER_CLASS);
        String subclassName = getOriginDisplayName(LAYER_SUBCLASS);

        if (subrace != null && race != null && subrace.endsWith(" " + race)) {
            subrace = subrace.substring(0, subrace.length() - race.length() - 1);
        }
        boolean heritageless = isHeritagelessRace();

        if (race == null) {
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.main_description_name_only", playerName));
            if (this.confirmedSelections.isEmpty()) {
                addSheetGap();
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.pick_left_to_start"));
            }
            appendSheetSections();
            return;
        }

        Component mainText;
        if (className == null) {
            if (heritageless) {
                if (subrace == null || subrace.isEmpty()) {
                    mainText = Component.translatable("raven_dnd_origins.gui.final_confirm.main_description_name_only", playerName);
                } else {
                    mainText = Component.translatable("raven_dnd_origins.gui.final_confirm.main_description_race_only_no_race",
                            playerName, subrace);
                }
            } else {
                mainText = Component.translatable("raven_dnd_origins.gui.final_confirm.main_description_race_only",
                        playerName, heritagePhrase(subrace, race));
            }
        } else {
            boolean hasSubclass = subclassName != null && !subclassName.isEmpty();
            boolean warlockPactLine = hasSubclass
                    && selectedClassIsWarlock()
                    && (!heritageless || (subrace != null && !subrace.isEmpty()));

            if (warlockPactLine) {
                String heritageData = heritageless
                        ? subrace
                        : heritagePhrase(subrace, race);
                mainText = Component.translatable(
                        "raven_dnd_origins.gui.final_confirm.main_description_warlock_subclass",
                        playerName,
                        subclassName,
                        heritageData);
            } else {
                String articleTarget = hasSubclass ? subclassName : className;
                boolean useAn = false;
                if (articleTarget != null && !articleTarget.isEmpty() && usesEnglishArticles()) {
                    char firstChar = Character.toLowerCase(articleTarget.charAt(0));
                    useAn = (firstChar == 'a' || firstChar == 'e' || firstChar == 'i' ||
                            firstChar == 'o' || firstChar == 'u');
                }

                String prefix = "raven_dnd_origins.gui.final_confirm.";
                if (heritageless) {
                    if (subrace != null && !subrace.isEmpty()) {
                        if (hasSubclass) {
                            String key = prefix + (useAn ? "main_description_no_race_an" : "main_description_no_race");
                            mainText = Component.translatable(key, playerName, subclassName, className, subrace);
                        } else {
                            String key = prefix + (useAn ? "main_description_no_race_no_subclass_an" : "main_description_no_race_no_subclass");
                            mainText = Component.translatable(key, playerName, className, subrace);
                        }
                    } else {
                        if (hasSubclass) {
                            String key = prefix + (useAn ? "main_description_simple_an" : "main_description_simple");
                            mainText = Component.translatable(key, playerName, subclassName, className);
                        } else {
                            String key = prefix + (useAn ? "main_description_simple_no_subclass_an" : "main_description_simple_no_subclass");
                            mainText = Component.translatable(key, playerName, className);
                        }
                    }
                } else {
                    if (hasSubclass) {
                        String key = prefix + (useAn ? "main_description_an" : "main_description");
                        mainText = Component.translatable(key, playerName, subclassName, className, heritagePhrase(subrace, race));
                    } else {
                        String key = prefix + (useAn ? "main_description_no_subclass_an" : "main_description_no_subclass");
                        mainText = Component.translatable(key, playerName, className, heritagePhrase(subrace, race));
                    }
                }
            }
        }
        addSheetText(mainText);

        appendSheetSections();
    }

    /** One fixed order for every section, so pencilled placeholders turn into ink in the same spot. */
    private void appendSheetSections() {
        List<String> cantrips = new ArrayList<>();
        String cantrip1 = getOriginDisplayName(LAYER_CANTRIP_ONE);
        String cantrip2 = getOriginDisplayName(LAYER_CANTRIP_TWO);
        if (cantrip1 != null) cantrips.add(cantrip1);
        if (cantrip2 != null) cantrips.add(cantrip2);
        for (ResourceLocation layerId : new ResourceLocation[]{
                LAYER_ELEMENTAL_DISCIPLINE_ONE,
                LAYER_ELEMENTAL_DISCIPLINE_TWO,
                LAYER_ELEMENTAL_DISCIPLINE_THREE,
                LAYER_ELEMENTAL_DISCIPLINE_FOUR
        }) {
            String elemental = getOriginDisplayName(layerId);
            if (elemental != null) cantrips.add(elemental);
        }
        if (!cantrips.isEmpty()) {
            addSheetGap();
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_header").withStyle(style -> style.withColor(DeskStyle.RUBRIC & 0xFFFFFF)));
            if (cantrips.size() == 1) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_single", cantrips.get(0)));
            } else if (cantrips.size() == 2) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_double", cantrips.get(0), cantrips.get(1)));
            } else {
                String allButLast = String.join(listSeparator(), cantrips.subList(0, cantrips.size() - 1));
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_multiple", allButLast, cantrips.get(cantrips.size() - 1)));
            }
        } else {
            addPendingSection("raven_dnd_origins.gui.final_confirm.cantrips_header", "cantrip_one", "cantrip_two", "elemental_discipline_one", "elemental_discipline_two", "elemental_discipline_three", "elemental_discipline_four");
        }

        List<String> plusOnes = new ArrayList<>();
        List<String> plusTwos = new ArrayList<>();
        String plusOne1 = getOriginDisplayName(LAYER_PLUS_ONE_APTITUDE_ONE);
        String plusOne2 = getOriginDisplayName(LAYER_PLUS_ONE_APTITUDE_TWO);
        String plusOneResilient = getOriginDisplayName(LAYER_PLUS_ONE_APTITUDE_RESILIENT);
        String plusTwo1 = getOriginDisplayName(LAYER_PLUS_TWO_APTITUDE_ONE);
        String plusTwo2 = getOriginDisplayName(LAYER_PLUS_TWO_APTITUDE_TWO);

        if (plusOne1 != null) plusOnes.add(plusOne1);
        if (plusOne2 != null) plusOnes.add(plusOne2);
        if (plusOneResilient != null) plusOnes.add(plusOneResilient);
        if (plusTwo1 != null) plusTwos.add(plusTwo1);
        if (plusTwo2 != null) plusTwos.add(plusTwo2);

        if (!plusOnes.isEmpty() || !plusTwos.isEmpty()) {
            addSheetGap();
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitudes_header").withStyle(style -> style.withColor(DeskStyle.RUBRIC & 0xFFFFFF)));
            if (!plusTwos.isEmpty()) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitude_plus_two", String.join(listSeparator(), plusTwos)));
            }
            if (!plusOnes.isEmpty()) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitude_plus_one", String.join(listSeparator(), plusOnes)));
            }
        } else {
            addPendingSection("raven_dnd_origins.gui.final_confirm.aptitudes_header", "plus_one_aptitude_one", "plus_one_aptitude_two", "plus_one_aptitude_resilient", "plus_two_aptitude_one", "plus_two_aptitude_two");
        }

        List<String> feats = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                LAYER_FREE_FEAT, LAYER_FIRST_FEAT,
                LAYER_SECOND_FEAT, LAYER_THIRD_FEAT,
                LAYER_FOURTH_FEAT, LAYER_FIFTH_FEAT
        };
        for (ResourceLocation layerId : featLayerIds) {
            String featName = getOriginDisplayName(layerId);
            if (featName != null) feats.add(featName);
        }
        if (!feats.isEmpty()) {
            addSheetGap();
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_header").withStyle(style -> style.withColor(DeskStyle.RUBRIC & 0xFFFFFF)));
            if (feats.size() == 1) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_single", feats.get(0)));
            } else if (feats.size() == 2) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_double", feats.get(0), feats.get(1)));
            } else {
                String allButLast = String.join(listSeparator(), feats.subList(0, feats.size() - 1));
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_multiple", allButLast, feats.get(feats.size() - 1)));
            }
        } else {
            addPendingSection("raven_dnd_origins.gui.final_confirm.feats_header", "free_feat", "first_feat", "second_feat", "third_feat", "fourth_feat", "fifth_feat");
        }
    }

    /** Pencils in a section heading while its layers are still ahead in this session and none is picked yet. */
    private void addPendingSection(String headerKey, String... layerPaths) {
        if (this.readOnly) return;
        for (String path : layerPaths) {
            int index = findLayerIndexForId(RavenDndOrigins.loc(path));
            if (index >= this.currentLayerIndex) {
                addSheetGap();
                FormattedCharSequence label = Component.translatable(headerKey).getVisualOrderText();
                this.sheetLines.add(new SheetLine(SheetLineKind.PENCIL, label, "", 0));
                return;
            }
        }
    }

    private void addSheetText(Component text) {
        for (FormattedCharSequence line : this.font.split(text, this.sheetTextWidth)) {
            this.sheetLines.add(new SheetLine(SheetLineKind.TEXT, line, plainText(line), codepointCount(line)));
        }
    }

    private void addSheetGap() {
        this.sheetLines.add(new SheetLine(SheetLineKind.GAP, FormattedCharSequence.EMPTY, "", 0));
    }

    /** "Other" and "Undead" are umbrella races whose subrace names the creature, so the prose omits a heritage. */
    private boolean isHeritagelessRace() {
        ResourceLocation raceId = pickedOriginId(LAYER_RACE);
        return RACE_OTHER.equals(raceId) || RACE_UNDEAD.equals(raceId);
    }

    /** The a/an choice only makes sense in English; other languages carry their articles in the lang templates. */
    private static boolean usesEnglishArticles() {
        return Minecraft.getInstance().getLanguageManager().getSelected().startsWith("en");
    }

    private static String listSeparator() {
        return Language.getInstance().getOrDefault("raven_dnd_origins.gui.list_separator", ", ");
    }

    private static java.text.Collator localeCollator() {
        String code = Minecraft.getInstance().getLanguageManager().getSelected();
        return java.text.Collator.getInstance(java.util.Locale.forLanguageTag(code.replace('_', '-')));
    }

    @Nullable
    private ResourceLocation pickedOriginId(ResourceLocation layerId) {
        for (Map.Entry<Integer, Origin> entry : this.confirmedSelections.entrySet()) {
            int idx = entry.getKey();
            if (idx < this.layerList.size() && layerId.equals(this.layerList.get(idx).id()) && entry.getValue() != null) {
                return entry.getValue().id();
            }
        }
        Player player = Minecraft.getInstance().player;
        if (this.readOnly || player == null || findLayerIndexForId(layerId) >= 0) return null;
        ResourceLocation settled = OriginsClientState.get(player.getUUID()).get(layerId);
        return settled == null || settled.equals(OriginRegistry.EMPTY_ID) ? null : settled;
    }

    @Nullable
    private String getOriginDisplayName(ResourceLocation layerId) {
        for (Map.Entry<Integer, Origin> entry : this.confirmedSelections.entrySet()) {
            int idx = entry.getKey();
            if (idx >= this.layerList.size()) continue;
            if (!layerId.equals(this.layerList.get(idx).id())) continue;
            Origin origin = entry.getValue();
            if (origin != null && !origin.id().equals(OriginRegistry.EMPTY_ID)) {
                return origin.name().getString();
            }
        }
        // A layer outside this session is already settled, so the player's saved pick still belongs on the sheet.
        Player player = Minecraft.getInstance().player;
        if (this.readOnly || player == null || findLayerIndexForId(layerId) >= 0) return null;
        ResourceLocation settled = OriginsClientState.get(player.getUUID()).get(layerId);
        Origin origin = settled == null || settled.equals(OriginRegistry.EMPTY_ID) ? null : OriginRegistry.get(settled);
        return origin == null ? null : origin.name().getString();
    }

    private Component getTitleText() {
        if (this.currentLayerIndex >= 0 && this.currentLayerIndex < this.layerList.size()) {
            OriginLayer currentLayer = this.layerList.get(this.currentLayerIndex);
            ResourceLocation layerId = currentLayer.id();

            if (layerId.equals(LAYER_WILDSHAPE)) {
                return Component.translatable("raven_dnd_origins.gui.wildshape_choose_title");
            }

            String sourceName = null;
            if (layerId.equals(LAYER_CANTRIP_ONE)) {
                sourceName = getOriginDisplayName(LAYER_SUBRACE);
            } else if (layerId.equals(LAYER_CANTRIP_TWO)) {
                sourceName = getCantripTwoSourceName();
            }

            if (sourceName != null) {
                return Component.translatable("raven_dnd_origins.gui.cantrip_choose_title", sourceName);
            }

            if (!currentLayer.chooseTitleKey().isEmpty()) {
                return currentLayer.chooseTitle();
            }
            return Component.translatable("origins.gui.choose_origin.title", currentLayer.name());
        }
        return this.title;
    }

    @Nullable
    private String getCantripTwoSourceName() {
        Origin subclassOrigin = null;
        for (Map.Entry<Integer, Origin> entry : this.confirmedSelections.entrySet()) {
            if (LAYER_SUBCLASS.equals(this.layerList.get(entry.getKey()).id())) {
                subclassOrigin = entry.getValue();
                break;
            }
        }

        if (subclassOrigin != null) {
            ResourceLocation originId = subclassOrigin.id();
            if (originId.equals(RavenDndOrigins.loc("subclass/rogue/arcane_trickster"))
                    || originId.equals(RavenDndOrigins.loc("subclass/fighter/eldritch_knight"))) {
                return subclassOrigin.name().getString();
            }
        }
        return getOriginDisplayName(LAYER_CLASS);
    }

    @Nullable
    private Integer findConfirmedPairIndex(int index) {
        ResourceLocation layerId = this.layerList.get(index).id();

        ResourceLocation pairedId = RAIL_ROW_PARTNERS.get(layerId);
        if (pairedId == null) {
            return null;
        }

        if (isAptitudeFreeTripleComplete()) {
            int apt1 = findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_ONE);
            int apt2 = findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_TWO);
            int free = findLayerIndexForId(LAYER_FREE_FEAT);
            if (index == apt1 || index == apt2 || index == free) {
                return null;
            }
        }

        if (isClassSubclassDraconicTripleComplete()) {
            int classI = findLayerIndexForId(LAYER_CLASS);
            int subI = findLayerIndexForId(LAYER_SUBCLASS);
            int dracI = findLayerIndexForId(LAYER_DRACONIC_ANCESTRY);
            if (index == classI || index == subI || index == dracI) {
                return null;
            }
        }

        for (int j = 0; j < this.layerList.size(); j++) {
            if (j == index || !this.confirmedSelections.containsKey(j)) continue;
            if (pairedId.equals(this.layerList.get(j).id())) return j;
        }
        return null;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.time += delta;
        this.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(0, 0, this.width, this.height, DeskStyle.SCREEN_DIM);

        renderCenterPanel(graphics);
        renderRail(graphics, mouseX, mouseY);

        Origin displayOrigin = getRightPanelDisplayOrigin();
        if (displayOrigin != null) {
            this.detailPanel.render(graphics, displayOrigin, this.layout.panel(), displayLayerName(displayOrigin),
                    mouseX, mouseY, this.time);
        } else {
            this.detailPanel.renderPlaceholder(graphics, this.layout.panel(),
                    Component.translatable("raven_dnd_origins.gui.detail_placeholder"));
        }

        if (this.selectButton != null) {
            updateButtonStates();
            this.selectButton.setEmphasized(this.reviewing);
            ResourceLocation id = this.reviewing ? REVIEW_BUTTON_KEY
                    : this.selectedOrigin == null ? null : this.selectedOrigin.id();
            if (!java.util.Objects.equals(id, this.selectButtonOriginId)) {
                this.selectButtonOriginId = id;
                this.selectButton.setMessage(selectButtonLabel());
            }
        }

        // Widgets only: Screen#render would repaint the background over the panels drawn above.
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, delta);
        }

        if (this.pendingConfirmSelectionHint) {
            renderConfirmSelectionHintTooltip(graphics, this.pendingConfirmHintScreenX, this.pendingConfirmHintScreenY);
            this.pendingConfirmSelectionHint = false;
        }
        if (this.hoveredCompletedLayerIndex != null) {
            OriginScreenLayout.Rect anchor = this.tooltipAnchoredToSeal ? this.anchoredSealRect : null;
            graphics.renderTooltip(this.font, completedSlotTooltip(this.hoveredCompletedLayerIndex),
                    this.confirmHintTooltipPositioner,
                    anchor == null ? mouseX : anchor.right(), anchor == null ? mouseY : anchor.y() + 4);
        }
    }

    // ------------------------------------------------------------------ narration

    @Override
    public Component getNarrationMessage() {
        return this.reviewing ? Component.translatable("raven_dnd_origins.gui.review.title") : getTitleText();
    }

    @Override
    protected void updateNarrationState(NarrationElementOutput output) {
        super.updateNarrationState(output);
        Component hint = narrationHint();
        if (hint != null) {
            output.add(NarratedElementType.HINT, hint);
        }
    }

    @Nullable
    private Component narrationHint() {
        if (this.hoveredCompletedLayerIndex != null) {
            OriginLayer layer = this.layerList.get(this.hoveredCompletedLayerIndex);
            Origin origin = this.confirmedSelections.get(this.hoveredCompletedLayerIndex);
            MutableComponent hint = Component.empty().append(layer.name());
            if (origin != null) hint.append(": ").append(origin.name());
            if (this.armedRevertIndex == this.hoveredCompletedLayerIndex) {
                hint.append(". ").append(Component.translatable("raven_dnd_origins.gui.revert.armed"));
            }
            return hint;
        }
        List<Origin> options = currentOptions();
        if (options != null && this.hoveredOrigin != null) {
            int index = options.indexOf(this.hoveredOrigin);
            Component option = Component.translatable("raven_dnd_origins.gui.narration.option",
                    this.hoveredOrigin.name(), index + 1, options.size());
            return sameOrigin(this.hoveredOrigin, this.selectedOrigin)
                    ? Component.translatable("raven_dnd_origins.gui.narration.selected", option)
                    : option;
        }
        if (this.reviewing) {
            List<String> picks = new ArrayList<>();
            for (int[] group : completedGroups()) picks.add(groupLabel(group));
            return Component.translatable("raven_dnd_origins.gui.narration.review", String.join(listSeparator(), picks));
        }
        return null;
    }

    private List<FormattedCharSequence> completedSlotTooltip(int layerIndex) {
        OriginLayer layer = this.layerList.get(layerIndex);
        Origin origin = this.confirmedSelections.get(layerIndex);
        List<FormattedCharSequence> lines = new ArrayList<>();
        if (this.readOnly) {
            lines.add(layer.name().getVisualOrderText());
            if (this.compactRail && origin != null) {
                lines.add(origin.name().copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
            }
            return lines;
        }
        lines.add(Component.translatable("raven_dnd_origins.gui.revert.change", layer.name()).getVisualOrderText());
        if (this.compactRail && origin != null) {
            lines.add(origin.name().copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
        }
        int later = laterChoiceCount(layerIndex);
        if (later > 0) {
            Component clears = later == 1
                    ? Component.translatable("raven_dnd_origins.gui.revert.clears_one")
                    : Component.translatable("raven_dnd_origins.gui.revert.clears", later);
            lines.add(clears.copy().withStyle(ChatFormatting.GRAY).getVisualOrderText());
            lines.add(Component.translatable("raven_dnd_origins.gui.revert.remembered")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC).getVisualOrderText());
        }
        if (this.armedRevertIndex == layerIndex) {
            lines.add(Component.translatable("raven_dnd_origins.gui.revert.armed")
                    .withStyle(ChatFormatting.GOLD).getVisualOrderText());
        } else if (!this.tooltipAnchoredToSeal) {
            lines.add(Component.translatable("raven_dnd_origins.gui.revert.keyboard")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC).getVisualOrderText());
        }
        return lines;
    }

    private Component selectButtonLabel() {
        if (this.reviewing) {
            return Component.translatable(this.kind == SessionKind.RESELECTION
                    ? "raven_dnd_origins.gui.review.keep"
                    : "raven_dnd_origins.gui.review.begin");
        }
        return this.selectedOrigin == null
                ? Component.translatable("origins.gui.select")
                : Component.translatable("raven_dnd_origins.gui.choose_origin_button", this.selectedOrigin.name());
    }

    @Nullable
    private Component displayLayerName(@Nullable Origin displayOrigin) {
        if (displayOrigin == null) return null;
        if (this.hoveredCompletedLayerIndex != null) {
            return this.layerList.get(this.hoveredCompletedLayerIndex).name();
        }
        for (Map.Entry<Integer, Origin> entry : this.confirmedSelections.entrySet()) {
            if (sameOrigin(entry.getValue(), displayOrigin) && entry.getKey() < this.layerList.size()) {
                if (this.readOnly || entry.getKey() < this.currentLayerIndex) {
                    return this.layerList.get(entry.getKey()).name();
                }
            }
        }
        if (this.currentLayerIndex < this.layerList.size()) {
            return this.layerList.get(this.currentLayerIndex).name();
        }
        return null;
    }

    // ------------------------------------------------------------------ rail

    private void invalidateRailCaches() {
        this.completedGroupsCache = null;
        this.groupLabelCache.clear();
        this.titleCacheLayer = -1;
    }

    private List<int[]> completedGroups() {
        if (this.completedGroupsCache != null) {
            return this.completedGroupsCache;
        }
        List<int[]> groups = new ArrayList<>();
        Set<Integer> grouped = new HashSet<>();
        int aptAnchor = isAptitudeFreeTripleComplete() ? aptitudeFreeTripleAnchorIndex() : -1;
        int dracAnchor = isClassSubclassDraconicTripleComplete() ? classSubclassDraconicTripleAnchorIndex() : -1;
        int end = Math.min(this.currentLayerIndex, this.layerList.size());
        for (int i = 0; i < end; i++) {
            if (grouped.contains(i) || !this.confirmedSelections.containsKey(i)) continue;
            int[] group;
            if (i == aptAnchor) {
                group = new int[]{
                        findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_ONE),
                        findLayerIndexForId(LAYER_PLUS_TWO_APTITUDE_TWO),
                        findLayerIndexForId(LAYER_FREE_FEAT)};
            } else if (i == dracAnchor) {
                group = new int[]{
                        findLayerIndexForId(LAYER_CLASS),
                        findLayerIndexForId(LAYER_SUBCLASS),
                        findLayerIndexForId(LAYER_DRACONIC_ANCESTRY)};
            } else {
                Integer pair = findConfirmedPairIndex(i);
                group = pair == null ? new int[]{i} : new int[]{Math.min(i, pair), Math.max(i, pair)};
            }
            for (int g : group) grouped.add(g);
            groups.add(group);
        }
        this.completedGroupsCache = groups;
        return groups;
    }

    private List<Integer> completedLayerOrder() {
        List<Integer> order = new ArrayList<>();
        for (int[] group : completedGroups()) {
            for (int layerIndex : group) order.add(layerIndex);
        }
        return order;
    }

    private int laterChoiceCount(int layerIndex) {
        int count = 0;
        for (int index : this.confirmedSelections.keySet()) {
            if (index > layerIndex && index < this.currentLayerIndex) count++;
        }
        return count;
    }

    private int lastCompletedLayerIndex() {
        int last = -1;
        for (int index : this.confirmedSelections.keySet()) {
            if (index < this.currentLayerIndex) last = Math.max(last, index);
        }
        return last;
    }

    private void renderRail(GuiGraphics graphics, int mouseX, int mouseY) {
        this.hoveredCompletedLayerIndex = null;
        this.tooltipAnchoredToSeal = false;
        this.anchoredSealRect = null;
        this.pendingConfirmSelectionHint = false;
        this.railSlots.clear();
        this.optionSlots.clear();
        this.optionsViewport = null;
        this.trayRect = null;
        this.trayScrollbar = null;
        this.activeLayerOptionsMaxScrollY = 0;

        OriginScreenLayout.Rect rail = this.layout.rail();
        graphics.blitSprite(DeskStyle.STRAP, rail.x(), 0, DeskStyle.STRAP_WIDTH, this.height);

        boolean hasActiveLayer = !this.readOnly && this.currentLayerIndex < this.layerList.size();
        List<int[]> groups = completedGroups();
        int room = rail.height() - (hasActiveLayer || this.reviewing ? MIN_TRAY_HEIGHT : 0);
        this.compactRail = groups.size() * RAIL_ROW_HEIGHT > room;

        int y = rail.y() + 2;
        if (this.compactRail) {
            // Seals overlap within a group and rows tighten; a second column keeps every group on its own line
            // without crowding out the tray.
            int columns = groups.size() * COMPACT_MIN_ROW > room ? 2 : 1;
            int rows = (groups.size() + columns - 1) / columns;
            int rowStep = Mth.clamp((room - DeskStyle.SEAL_SIZE) / Math.max(1, rows - 1), COMPACT_MIN_ROW, COMPACT_MAX_ROW);
            int columnWidth = rail.width() / columns;
            for (int i = 0; i < groups.size(); i++) {
                int column = i / rows;
                renderSealRow(graphics, groups.get(i), rail, rail.x() + column * columnWidth, y + (i % rows) * rowStep,
                        mouseX, mouseY, true);
            }
            y += (rows - 1) * rowStep + DeskStyle.SEAL_SIZE;
        } else {
            for (int[] group : groups) {
                renderSealRow(graphics, group, rail, rail.x(), y, mouseX, mouseY, false);
                y += RAIL_ROW_HEIGHT;
            }
        }

        // Keyboard focus or an armed revert stands in for the mouse, with the tooltip pinned beside the seal.
        if (this.hoveredCompletedLayerIndex == null) {
            int anchored = this.keyboardFocusedSeal >= 0 ? this.keyboardFocusedSeal : this.armedRevertIndex;
            if (anchored >= 0 && this.confirmedSelections.containsKey(anchored) && this.anchoredSealRect != null) {
                this.hoveredCompletedLayerIndex = anchored;
                this.tooltipAnchoredToSeal = true;
            }
        }

        int trayY = Math.min(y + 4, rail.bottom() - MIN_TRAY_HEIGHT);
        if (hasActiveLayer) {
            renderActiveTray(graphics, rail, trayY, mouseX, mouseY);
        } else if (this.reviewing) {
            renderReviewTray(graphics, rail, trayY);
        }
    }

    private void renderReviewTray(GuiGraphics graphics, OriginScreenLayout.Rect rail, int trayY) {
        int trayX = rail.x() + DeskStyle.STRAP_WIDTH + 4;
        int trayW = rail.right() - trayX;
        int innerW = trayW - 2 * TRAY_PADDING - 2;
        if (innerW < OPTION_CELL || trayY >= rail.bottom() - 24) return;
        List<FormattedCharSequence> title = this.font.split(Component.translatable("raven_dnd_origins.gui.review.title"), innerW);
        List<FormattedCharSequence> hint = this.font.split(Component.translatable("raven_dnd_origins.gui.review.hint"), innerW);
        int trayH = Math.min(TRAY_PADDING * 2 + 4 + title.size() * 10 + 4 + hint.size() * 10, rail.bottom() - trayY);
        DeskStyle.panel(graphics, trayX, trayY, trayW, trayH);
        int ty = trayY + TRAY_PADDING + 2;
        for (FormattedCharSequence line : title) {
            graphics.drawString(this.font, line, trayX + TRAY_PADDING + 1, ty, DeskStyle.GILT, true);
            ty += 10;
        }
        ty += 4;
        for (FormattedCharSequence line : hint) {
            graphics.drawString(this.font, line, trayX + TRAY_PADDING + 1, ty, DeskStyle.CREAM, true);
            ty += 10;
        }
    }

    private void renderSealRow(GuiGraphics graphics, int[] group, OriginScreenLayout.Rect rail, int x, int y,
                               int mouseX, int mouseY, boolean compact) {
        int sealStep = compact ? COMPACT_SEAL_STEP : 18;
        // Later seals are drawn over earlier ones, so the topmost seal under the cursor wins the hover.
        int hoveredK = -1;
        for (int k = group.length - 1; k >= 0; k--) {
            int sx = x + k * sealStep;
            if (mouseX >= sx && mouseX < sx + DeskStyle.SEAL_SIZE && mouseY >= y && mouseY < y + DeskStyle.SEAL_SIZE) {
                hoveredK = k;
                break;
            }
        }
        for (int k = 0; k < group.length; k++) {
            renderSeal(graphics, group[k], x + k * sealStep, y, k == hoveredK);
        }
        if (compact) return;

        int tagX = x + DeskStyle.SEAL_SIZE + (group.length - 1) * sealStep + 4;
        int maxTextW = rail.right() - tagX - 10;
        if (maxTextW < 14) return;
        List<FormattedCharSequence> lines = this.font.split(Component.literal(groupLabel(group)), maxTextW);
        if (lines.size() > 2) {
            lines = List.of(lines.get(0), ellipsizeSequence(lines.subList(1, lines.size()), maxTextW));
        }
        int textW = 0;
        for (FormattedCharSequence line : lines) textW = Math.max(textW, this.font.width(line));
        boolean twoLines = lines.size() > 1;
        OriginScreenLayout.Rect tag = new OriginScreenLayout.Rect(tagX, twoLines ? y : y + 4, textW + 10, twoLines ? 24 : 16);
        int lastLayer = group[group.length - 1];
        boolean hovered = tag.contains(mouseX, mouseY);
        if (hovered) this.hoveredCompletedLayerIndex = lastLayer;
        this.railSlots.add(new RailSlot(lastLayer, tag));
        DeskStyle.tag(graphics, tag.x(), tag.y(), tag.width(), tag.height());
        if (this.armedRevertIndex == lastLayer) {
            fillInnerBorder1px(graphics, tag.x(), tag.y(), tag.width(), tag.height(), DeskStyle.PAGE_RUBRIC);
        }
        int ty = tag.y() + 4;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, tag.x() + 5, ty, hovered ? DeskStyle.HOVER_TEXT : DeskStyle.CREAM, true);
            ty += 10;
        }
    }

    private FormattedCharSequence ellipsizeSequence(List<FormattedCharSequence> rest, int maxWidth) {
        StringBuilder text = new StringBuilder();
        for (FormattedCharSequence line : rest) {
            line.accept((index, style, codepoint) -> {
                text.appendCodePoint(codepoint);
                return true;
            });
            text.append(' ');
        }
        return ellipsize(text.toString().trim(), maxWidth).getVisualOrderText();
    }

    private void renderSeal(GuiGraphics graphics, int layerIndex, int x, int y, boolean hovered) {
        OriginLayer layer = this.layerList.get(layerIndex);
        Origin origin = this.confirmedSelections.get(layerIndex);
        if (origin == null) return;
        OriginScreenLayout.Rect rect = new OriginScreenLayout.Rect(x, y, DeskStyle.SEAL_SIZE, DeskStyle.SEAL_SIZE);
        if (hovered) this.hoveredCompletedLayerIndex = layerIndex;
        this.railSlots.add(new RailSlot(layerIndex, rect));

        boolean selected = this.readOnly && sameOrigin(this.selectedOrigin, origin);
        boolean armed = this.armedRevertIndex == layerIndex;
        boolean railFocused = this.keyboardFocusedSeal == layerIndex;
        if (railFocused || (armed && this.keyboardFocusedSeal < 0)) {
            this.anchoredSealRect = rect;
        }
        hovered |= railFocused;
        DeskStyle.seal(graphics, x, y, DeskStyle.SealGroup.of(layer.id()), hovered || selected || armed);
        drawOptionIcon(graphics, layer, origin, x + 4, y + 4);
        if (armed) {
            fillInnerBorder1px(graphics, x - 1, y - 1, DeskStyle.SEAL_SIZE + 2, DeskStyle.SEAL_SIZE + 2, DeskStyle.PAGE_RUBRIC);
            fillInnerBorder1px(graphics, x, y, DeskStyle.SEAL_SIZE, DeskStyle.SEAL_SIZE, DeskStyle.PAGE_RUBRIC);
        } else if (selected || hovered) {
            graphics.blitSprite(DeskStyle.SOCKET_SELECTED, x - 1, y - 1, DeskStyle.SEAL_SIZE + 2, DeskStyle.SEAL_SIZE + 2);
        }
    }

    private void drawOptionIcon(GuiGraphics graphics, OriginLayer layer, Origin origin, int x, int y) {
        if (isPortraitLayer(layer)) {
            graphics.blit(getPortraitTexture(origin.icon()), x, y, 8, 2, 16, 16, 32, 32);
        } else {
            OriginDetailPanel.renderIcon(graphics, origin, x, y);
        }
    }

    private Component ellipsize(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return Component.literal(text);
        }
        String ellipsis = "...";
        return Component.literal(this.font.plainSubstrByWidth(text, Math.max(0, maxWidth - this.font.width(ellipsis))) + ellipsis);
    }

    /**
     * Joins the group's names, dropping any name a later one already contains ("Elf" inside "High Elf")
     * and the class when its subclass is in the same group, since the subclass names it more precisely.
     */
    private String groupLabel(int[] group) {
        return this.groupLabelCache.computeIfAbsent(group[0], first -> buildGroupLabel(group));
    }

    private String buildGroupLabel(int[] group) {
        boolean hasSubclass = false;
        for (int layerIndex : group) {
            if (LAYER_SUBCLASS.equals(this.layerList.get(layerIndex).id())) hasSubclass = true;
        }
        List<String> names = new ArrayList<>();
        for (int layerIndex : group) {
            Origin origin = this.confirmedSelections.get(layerIndex);
            if (origin == null) continue;
            if (hasSubclass && LAYER_CLASS.equals(this.layerList.get(layerIndex).id())) continue;
            names.add(origin.name().getString());
        }
        List<String> kept = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            boolean covered = false;
            for (int j = i + 1; j < names.size(); j++) {
                if (names.get(j).contains(names.get(i))) {
                    covered = true;
                    break;
                }
            }
            if (!covered) kept.add(names.get(i));
        }
        return String.join(listSeparator(), kept);
    }

    // ------------------------------------------------------------------ tray

    @Nullable
    private List<Origin> currentOptions() {
        if (this.readOnly || this.reviewing || this.currentLayerIndex >= this.layerList.size()) return null;
        return this.layerOriginCache.get(this.currentLayerIndex);
    }

    private static int balancedColumns(int count, int maxColumns) {
        if (count <= 0 || maxColumns <= 1) return Math.max(1, maxColumns);
        int rows = (count + maxColumns - 1) / maxColumns;
        return (count + rows - 1) / rows;
    }

    private void renderActiveTray(GuiGraphics graphics, OriginScreenLayout.Rect rail, int trayY, int mouseX, int mouseY) {
        OriginLayer layer = this.layerList.get(this.currentLayerIndex);
        List<Origin> options = this.layerOriginCache.get(this.currentLayerIndex);
        int trayX = rail.x() + DeskStyle.STRAP_WIDTH + 4;
        int trayW = rail.right() - trayX;
        int innerW = trayW - 2 * TRAY_PADDING;
        if (innerW < OPTION_CELL || trayY >= rail.bottom() - 24) return;
        int optionCount = options == null ? 0 : options.size();

        boolean portrait = isPortraitLayer(layer);
        boolean listMode = innerW >= LIST_MODE_MIN_WIDTH;
        // Portraits never shrink to a crop: narrow trays use sliding strips that open the hovered card to full width.
        boolean cardStrip = portrait && !listMode;
        boolean captioned = !listMode;

        if (this.titleCacheLayer != this.currentLayerIndex || this.titleCacheWidth != innerW) {
            List<FormattedCharSequence> lines = this.font.split(getTitleText(), innerW - 2);
            this.titleCacheLines = lines.size() > 3 ? lines.subList(0, 3) : lines;
            this.titleCacheLayer = this.currentLayerIndex;
            this.titleCacheWidth = innerW;
        }
        List<FormattedCharSequence> title = this.titleCacheLines;
        int headerH = TRAY_PADDING + 2 + title.size() * 10 + (captioned ? 11 : 0) + 3;

        int gridW = innerW;
        OptionLayout optionLayout = layoutOptions(optionCount, gridW, listMode, cardStrip, portrait);
        if (optionLayout.contentHeight() > rail.bottom() - trayY - headerH - TRAY_PADDING) {
            gridW = innerW - TRAY_SCROLLBAR_WIDTH;
            optionLayout = layoutOptions(optionCount, gridW, listMode, cardStrip, portrait);
        }
        int columns = optionLayout.columnsOrCardsPerStrip();
        int rowH = optionLayout.rowHeight();
        int contentH = optionLayout.contentHeight();
        this.optionColumns = columns;
        int trayH = Math.min(headerH + contentH + TRAY_PADDING, rail.bottom() - trayY);
        this.trayRect = new OriginScreenLayout.Rect(trayX, trayY, trayW, trayH);

        DeskStyle.panel(graphics, trayX, trayY, trayW, trayH);
        int ty = trayY + TRAY_PADDING + 2;
        for (FormattedCharSequence line : title) {
            graphics.drawString(this.font, line, trayX + TRAY_PADDING + 1, ty, DeskStyle.GILT, true);
            ty += 10;
        }
        int captionY = ty + 1;
        if (options == null || options.isEmpty()) return;

        OriginScreenLayout.Rect viewport = new OriginScreenLayout.Rect(
                trayX + TRAY_PADDING - 2, trayY + headerH - 2, innerW + 4, trayH - headerH - TRAY_PADDING + 4);
        this.optionsViewport = viewport;
        int viewH = viewport.height() - 4;
        this.activeLayerOptionsMaxScrollY = Math.max(0, contentH - viewH);
        if (this.scrollToKeyboardFocus && this.keyboardFocusedOption >= 0) {
            int focusTop = (this.keyboardFocusedOption / columns) * rowH;
            if (focusTop < this.activeLayerOptionsScrollY) {
                this.activeLayerOptionsScrollY = focusTop;
            } else if (focusTop + rowH > this.activeLayerOptionsScrollY + viewH) {
                this.activeLayerOptionsScrollY = focusTop + rowH - viewH;
            }
            this.scrollToKeyboardFocus = false;
        }
        this.activeLayerOptionsScrollY = Mth.clamp(this.activeLayerOptionsScrollY, 0, this.activeLayerOptionsMaxScrollY);

        Origin pointerHover = null;
        Origin focused = this.keyboardFocusedOption >= 0 && this.keyboardFocusedOption < optionCount ? options.get(this.keyboardFocusedOption) : null;
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.right(), viewport.bottom());
        int originX = trayX + TRAY_PADDING;
        int originY = trayY + headerH - this.activeLayerOptionsScrollY;
        if (cardStrip) {
            for (int start = 0; start < optionCount; start += columns) {
                List<Origin> strip = options.subList(start, Math.min(optionCount, start + columns));
                Origin hovered = renderPortraitStrip(graphics, strip, start, originX, originY + (start / columns) * rowH,
                        gridW, viewport, mouseX, mouseY, focused);
                if (hovered != null) pointerHover = hovered;
            }
        } else {
            int cellW = listMode ? gridW / columns : OPTION_CELL;
            int gridX = originX + (gridW - columns * cellW) / 2;
            int socket = portrait ? CARD_HEIGHT + 2 : 20;
            for (int i = 0; i < optionCount; i++) {
                Origin origin = options.get(i);
                int cx = gridX + (i % columns) * cellW + 1;
                int cy = originY + (i / columns) * rowH + 1;
                OriginScreenLayout.Rect hit = new OriginScreenLayout.Rect(cx, cy, listMode ? cellW - 2 : socket, socket);
                boolean hovered = viewport.contains(mouseX, mouseY) && hit.contains(mouseX, mouseY);
                boolean lit = hovered || origin == focused;
                boolean selected = sameOrigin(this.selectedOrigin, origin);
                if (listMode && lit) {
                    graphics.fill(hit.x(), hit.y(), hit.right(), hit.bottom(), DeskStyle.HOVER_WASH);
                }
                graphics.blitSprite(lit ? DeskStyle.SOCKET_HOVER : DeskStyle.SOCKET, cx, cy, socket, socket);
                if (portrait) {
                    graphics.blit(getPortraitTexture(origin.icon()), cx + 1, cy + 1, 0, 0, CARD_HEIGHT, CARD_HEIGHT, 32, 32);
                } else {
                    drawOptionIcon(graphics, layer, origin, cx + 2, cy + 2);
                }
                if (selected) {
                    graphics.blitSprite(DeskStyle.SOCKET_SELECTED, cx - 2, cy - 2, socket + 4, socket + 4);
                }
                if (listMode) {
                    graphics.drawString(this.font, ellipsize(origin.name().getString(), cellW - socket - 10), cx + socket + 4,
                            cy + (socket - 8) / 2, lit ? DeskStyle.HOVER_TEXT : selected ? DeskStyle.GILT : DeskStyle.CREAM, true);
                }
                this.optionSlots.add(new OptionSlot(origin, hit));
                if (hovered) {
                    pointerHover = origin;
                    if (selected) queueConfirmHint(mouseX, mouseY);
                }
            }
        }
        graphics.disableScissor();
        if (this.activeLayerOptionsMaxScrollY > 0) {
            renderTrayScrollbar(graphics, trayX + trayW - TRAY_PADDING - 3, viewport, contentH, mouseX, mouseY);
        }
        this.hoveredOrigin = pointerHover != null ? pointerHover : focused != null ? focused : this.selectedOrigin;

        if (captioned && this.hoveredOrigin != null) {
            DeskStyle.scrollingString(graphics, this.font, this.hoveredOrigin.name(), trayX + TRAY_PADDING + 1, captionY,
                    innerW - 2, DeskStyle.CREAM);
        }
    }

    private record OptionLayout(int columnsOrCardsPerStrip, int rowHeight, int contentHeight) {}

    private OptionLayout layoutOptions(int count, int width, boolean listMode, boolean cardStrip, boolean portrait) {
        if (cardStrip) {
            // Each strip keeps its collapsed cards at least MIN_CARD_WIDTH wide, wrapping into balanced strips.
            int fit = Math.max(1, (width - CARD_EXPANDED_WIDTH) / MIN_CARD_WIDTH + 1);
            int perStrip = balancedColumns(count, Math.min(Math.max(1, count), fit));
            int strips = (count + perStrip - 1) / perStrip;
            return new OptionLayout(perStrip, CARD_HEIGHT + 4, strips * (CARD_HEIGHT + 4));
        }
        int columns = listMode ? (width >= 2 * LIST_MODE_MIN_WIDTH ? 2 : 1) : balancedColumns(count, Math.max(1, width / OPTION_CELL));
        int rowH = listMode ? (portrait ? CARD_HEIGHT + 4 : LIST_ROW_HEIGHT) : OPTION_CELL;
        int rows = (count + columns - 1) / columns;
        return new OptionLayout(columns, rowH, rows * rowH);
    }

    private void renderTrayScrollbar(GuiGraphics graphics, int trackX, OriginScreenLayout.Rect viewport, int contentH,
                                     int mouseX, int mouseY) {
        int trackY = viewport.y() + 2;
        int trackH = viewport.height() - 4;
        // The hit area spans the whole right gutter, wider than the 3px track it draws.
        this.trayScrollbar = new OriginScreenLayout.Rect(trackX - 3, trackY, TRAY_PADDING + 3, trackH);
        int thumbH = Math.max(12, trackH * trackH / Math.max(1, contentH));
        int thumbY = trackY + Math.round((trackH - thumbH) * (this.activeLayerOptionsScrollY / (float) this.activeLayerOptionsMaxScrollY));
        this.trayThumbY = thumbY;
        this.trayThumbHeight = thumbH;
        boolean lit = this.draggingTrayScrollbar || this.trayScrollbar.contains(mouseX, mouseY);
        graphics.fill(trackX, trackY, trackX + 3, trackY + trackH, DeskStyle.LEATHER_DARK);
        graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, lit ? DeskStyle.BRASS_LIGHT : DeskStyle.BRASS);
        graphics.fill(trackX, thumbY + thumbH - 1, trackX + 3, thumbY + thumbH, DeskStyle.BRASS_DARK);
    }

    private void dragTrayScrollbar(double mouseY) {
        OriginScreenLayout.Rect track = this.trayScrollbar;
        if (track == null || this.activeLayerOptionsMaxScrollY <= 0) return;
        int travel = Math.max(1, track.height() - this.trayThumbHeight);
        float part = Mth.clamp((float) (mouseY - this.trayThumbGrabOffset - track.y()) / travel, 0f, 1f);
        this.activeLayerOptionsScrollY = Math.round(part * this.activeLayerOptionsMaxScrollY);
    }

    @Nullable
    private Origin renderPortraitStrip(GuiGraphics graphics, List<Origin> options, int progressOffset, int startX, int startY,
                                       int availableWidth, OriginScreenLayout.Rect viewport, int mouseX, int mouseY,
                                       @Nullable Origin focused) {
        int[] cardWidths = computeFixedWidthCardWidths(options.size(), availableWidth, progressOffset);
        int total = 0;
        for (int w : cardWidths) total += w;
        int x = startX + Math.max(0, (availableWidth - total) / 2);
        graphics.blitSprite(DeskStyle.SOCKET, x - 1, startY - 1, total + 2, CARD_HEIGHT + 2);
        Origin pointerHover = null;
        for (int i = 0; i < options.size(); i++) {
            Origin origin = options.get(i);
            int width = cardWidths[i];
            OriginScreenLayout.Rect card = new OriginScreenLayout.Rect(x, startY, width, CARD_HEIGHT);
            boolean hovered = viewport.contains(mouseX, mouseY) && card.contains(mouseX, mouseY);
            int uvX = (32 - width) / 2;
            graphics.blit(getPortraitTexture(origin.icon()), x, startY, width, CARD_HEIGHT, uvX, 0, width, CARD_HEIGHT, 32, 32);
            boolean selected = sameOrigin(this.selectedOrigin, origin);
            if (selected || origin == focused) {
                fillInnerBorder1px(graphics, x, startY, width, CARD_HEIGHT, selected ? DeskStyle.BRASS_LIGHT : DeskStyle.CREAM);
            }
            this.optionSlots.add(new OptionSlot(origin, card));
            if (hovered) {
                pointerHover = origin;
                if (selected) queueConfirmHint(mouseX, mouseY);
            }
            x += width;
        }
        return pointerHover;
    }

    private void queueConfirmHint(int mouseX, int mouseY) {
        this.pendingConfirmSelectionHint = true;
        this.pendingConfirmHintScreenX = mouseX;
        this.pendingConfirmHintScreenY = mouseY;
    }

    /** 1px border along the inside edge of a rectangle (top/bottom full width; sides omit corner pixels). */
    private static void fillInnerBorder1px(GuiGraphics graphics, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        graphics.fill(x, y, x + w, Math.min(y + 1, y + h), argb);
        if (h <= 1) return;
        graphics.fill(x, y + h - 1, x + w, y + h, argb);
        if (w <= 1) return;
        graphics.fill(x, y + 1, x + 1, y + h - 1, argb);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, argb);
    }

    private void renderConfirmSelectionHintTooltip(GuiGraphics graphics, int screenMouseX, int screenMouseY) {
        Component tip = Component.translatable("raven_dnd_origins.gui.choose_origin_double_click_hint");
        int margin = 4;
        int gap = 12;
        int spaceLeft = Math.max(48, screenMouseX - margin - gap);
        int spaceRight = Math.max(48, this.width - margin - gap - screenMouseX);
        int widthLimit = Mth.clamp(Math.max(spaceLeft, spaceRight), 72, 280);
        graphics.renderTooltip(this.font, this.font.split(tip, widthLimit),
                this.confirmHintTooltipPositioner, screenMouseX, screenMouseY);
    }

    private ResourceLocation getPortraitTexture(IconData icon) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(icon.stack().getItem());
        if (itemId.getNamespace().equals(RavenDndOrigins.MODID) && itemId.getPath().startsWith("portrait/")) {
            String portraitName = itemId.getPath().substring("portrait/".length());
            return RavenDndOrigins.loc("textures/item/origin_portrait_" + portraitName + ".png");
        }
        return RavenDndOrigins.loc("textures/item/origin_portrait_base.png");
    }

    // ------------------------------------------------------------------ sheet

    private void renderCenterPanel(GuiGraphics graphics) {
        OriginScreenLayout.Rect sheet = this.layout.sheet();
        if (sheet == null) return;

        if (this.kind != SessionKind.INITIAL_CREATION) {
            Origin displayOrigin = getRightPanelDisplayOrigin();
            if (displayOrigin != null && this.previewableCache.computeIfAbsent(displayOrigin.id(),
                    id -> ShapeshiftFormPreview.canPreview(displayOrigin))) {
                ShapeshiftFormPreview.render(graphics, displayOrigin, sheet.x() + sheet.width() / 2,
                        sheet.y() + sheet.height() / 2, ShapeshiftFormPreview.DEFAULT_SIZE, this.time);
                return;
            }
        }

        graphics.blit(DeskStyle.SHEET, sheet.x(), sheet.y(), sheet.width(), sheet.height(),
                0, 0, DeskStyle.SHEET_SIZE, DeskStyle.SHEET_SIZE, DeskStyle.SHEET_SIZE, DeskStyle.SHEET_SIZE);
        int pad = sheetPadding(sheet);
        int textX = sheet.x() + pad;
        int ruleRight = sheet.right() - pad;
        int top = sheet.y() + sheetTopInset(sheet);
        int bottom = sheet.bottom() - sheetBottomInset(sheet);
        graphics.enableScissor(textX - 2, top - 14, ruleRight + 2, bottom);

        int revealed = inkRevealedChars();
        boolean inking = revealed < this.inkTotalChars;
        int textY = top - this.sheetScrollY;
        int[] quillAt = null;
        int quillContentY = -1;
        for (int i = 0; i < this.sheetLines.size(); i++) {
            SheetLine line = this.sheetLines.get(i);
            switch (line.kind()) {
                case GAP -> textY += 6;
                case PENCIL -> {
                    graphics.drawString(this.font, line.text(), textX, textY, DeskStyle.PENCIL, false);
                    DeskStyle.pencilRule(graphics, textX + this.font.width(line.text()) + 4, ruleRight, textY + 7);
                    textY += 12;
                }
                case TEXT -> {
                    int start = i < this.lineInkStart.length ? this.lineInkStart[i] : -1;
                    if (start < 0) {
                        graphics.drawString(this.font, line.text(), textX, textY, DeskStyle.INK, false);
                    } else {
                        int n = line.length();
                        int visible = Mth.clamp(revealed - start, 0, n);
                        if (visible > 0) {
                            graphics.drawString(this.font, slice(line.text(), 0, visible), textX, textY, DeskStyle.INK, false);
                            int wetFrom = Math.max(0, visible - INK_WET_TRAIL);
                            if (inking && wetFrom < visible) {
                                int wetX = textX + this.font.width(slice(line.text(), 0, wetFrom));
                                graphics.drawString(this.font, slice(line.text(), wetFrom, visible), wetX, textY, DeskStyle.WET_INK, false);
                            }
                        }
                        if (quillAt == null && inking && revealed >= start && revealed < start + n) {
                            quillAt = new int[]{textX + this.font.width(slice(line.text(), 0, visible)), textY};
                            quillContentY = textY + this.sheetScrollY;
                        }
                    }
                    textY += 12;
                }
            }
        }
        graphics.disableScissor();
        this.sheetMaxScrollY = Math.max(0, textY + this.sheetScrollY - bottom);
        if (quillContentY >= 0) {
            // Follow the quill when it writes below the fold.
            int lineFromTop = quillContentY - top;
            int viewH = bottom - top;
            if (lineFromTop - this.sheetScrollY > viewH - 12) {
                this.sheetScrollY = lineFromTop - viewH + 12;
            }
        }
        this.sheetScrollY = Mth.clamp(this.sheetScrollY, 0, this.sheetMaxScrollY);

        int arrowX = sheet.x() + sheet.width() / 2;
        if (this.sheetScrollY > 0) {
            DeskStyle.scrollArrow(graphics, arrowX, top - 8, true);
        }
        if (this.sheetScrollY < this.sheetMaxScrollY) {
            DeskStyle.scrollArrow(graphics, arrowX, bottom + 1, false);
        }
        if (quillAt != null && quillAt[1] >= top - 4 && quillAt[1] < bottom) {
            graphics.renderItem(QUILL, quillAt[0] - 1, quillAt[1] - 9);
        }
    }

    private int inkRevealedChars() {
        if (this.inkTotalChars <= 0) return 0;
        long elapsed = Util.getMillis() - this.inkStartMillis;
        float speed = Math.max(INK_MIN_CHARS_PER_MS, this.inkTotalChars / (float) INK_MAX_DURATION_MS);
        return Math.min(this.inkTotalChars, (int) (elapsed * speed));
    }

    private static int codepointCount(FormattedCharSequence seq) {
        int[] n = {0};
        seq.accept((index, style, codepoint) -> {
            n[0]++;
            return true;
        });
        return n[0];
    }

    private static String plainText(FormattedCharSequence seq) {
        StringBuilder text = new StringBuilder();
        seq.accept((index, style, codepoint) -> {
            text.appendCodePoint(codepoint);
            return true;
        });
        return text.toString();
    }

    private static FormattedCharSequence slice(FormattedCharSequence seq, int from, int to) {
        return sink -> {
            int[] n = {0};
            return seq.accept((index, style, codepoint) -> {
                int k = n[0]++;
                if (k >= to) return false;
                return k < from || sink.accept(index, style, codepoint);
            });
        };
    }

    /**
     * Marks the lines the quill should write: only lines that weren't on the previous sheet, so confirming a
     * feat writes the feat line instead of re-inking every section below it.
     */
    private void updateInk() {
        Map<String, Integer> previous = new HashMap<>();
        for (String plain : this.previousSheetText) {
            previous.merge(plain, 1, Integer::sum);
        }
        this.lineInkStart = new int[this.sheetLines.size()];
        this.inkTotalChars = 0;
        List<String> current = new ArrayList<>();
        for (int i = 0; i < this.sheetLines.size(); i++) {
            SheetLine line = this.sheetLines.get(i);
            this.lineInkStart[i] = -1;
            if (line.kind() != SheetLineKind.TEXT) continue;
            current.add(line.plain());
            int seen = previous.getOrDefault(line.plain(), 0);
            if (seen > 0) {
                previous.put(line.plain(), seen - 1);
            } else if (this.animateNextSheetChange) {
                this.lineInkStart[i] = this.inkTotalChars;
                this.inkTotalChars += line.length();
            }
        }
        this.previousSheetText = current;
        this.inkStartMillis = Util.getMillis();
        this.animateNextSheetChange = false;
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (this.detailPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (this.trayScrollbar != null && this.trayScrollbar.contains(mouseX, mouseY)) {
            this.draggingTrayScrollbar = true;
            boolean onThumb = mouseY >= this.trayThumbY && mouseY < this.trayThumbY + this.trayThumbHeight;
            this.trayThumbGrabOffset = onThumb ? (int) mouseY - this.trayThumbY : this.trayThumbHeight / 2;
            dragTrayScrollbar(mouseY);
            return true;
        }
        if (this.optionsViewport != null && this.optionsViewport.contains(mouseX, mouseY)) {
            for (OptionSlot slot : this.optionSlots) {
                if (slot.rect().contains(mouseX, mouseY)) {
                    this.armedRevertIndex = -1;
                    this.keyboardFocusedSeal = -1;
                    setFocused(null);
                    List<Origin> options = currentOptions();
                    this.keyboardFocusedOption = options == null ? -1 : options.indexOf(slot.origin());
                    selectOrConfirm(slot.origin());
                    return true;
                }
            }
        }
        // Overlapping seals register in draw order, so the last match is the one drawn on top.
        for (int i = this.railSlots.size() - 1; i >= 0; i--) {
            RailSlot slot = this.railSlots.get(i);
            if (slot.rect().contains(mouseX, mouseY)) {
                setFocused(null);
                this.keyboardFocusedSeal = -1;
                clickCompletedSlot(slot.layerIndex());
                return true;
            }
        }
        this.armedRevertIndex = -1;
        this.keyboardFocusedSeal = -1;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Second click on the already-selected option confirms it. */
    private void selectOrConfirm(Origin clicked) {
        if (sameOrigin(clicked, this.selectedOrigin)) {
            confirmSelection();
        } else {
            this.selectedOrigin = clicked;
            updateButtonStates();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !this.readOnly) {
            if (this.armedRevertIndex >= 0 || this.keyboardFocusedSeal >= 0) {
                this.armedRevertIndex = -1;
                this.keyboardFocusedSeal = -1;
            } else {
                confirmLeave();
            }
            return true;
        }
        boolean arrow = keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (arrow && (shift || this.readOnly)) {
            moveKeyboardSealFocus(keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            return true;
        }
        boolean enter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        if (enter && Util.getMillis() - this.lastLayerChangeMillis < ENTER_DEBOUNCE_MS) {
            return true;
        }
        List<Origin> options = currentOptions();
        if (arrow && options != null && !options.isEmpty()) {
            // Arrow keys always belong to the option grid, even after the button took focus from a click or Tab.
            setFocused(null);
        }
        boolean buttonFocused = getFocused() instanceof DeskButton;
        if (options != null && !options.isEmpty() && !buttonFocused) {
            int columns = Math.max(1, this.optionColumns);
            switch (keyCode) {
                case GLFW.GLFW_KEY_LEFT -> {
                    moveFocus(options, -1);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveFocus(options, 1);
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    moveFocus(options, -columns);
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    moveFocus(options, columns);
                    return true;
                }
                default -> {
                }
            }
            if (enter) {
                if (this.keyboardFocusedOption >= 0 && this.keyboardFocusedOption < options.size()) {
                    selectOrConfirm(options.get(this.keyboardFocusedOption));
                } else if (this.selectedOrigin != null) {
                    confirmSelection();
                }
                return true;
            }
        } else if (this.reviewing && enter && !buttonFocused) {
            finishSelection();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.readOnly) {
            int target = this.keyboardFocusedSeal >= 0 ? this.keyboardFocusedSeal : lastCompletedLayerIndex();
            if (target >= 0) {
                if (this.armedRevertIndex == target) {
                    revertToLayer(target);
                } else {
                    armRevert(target);
                }
                return true;
            }
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_PAGE_UP -> {
                this.detailPanel.scrollBy(-this.detailPanel.pageHeight());
                return true;
            }
            case GLFW.GLFW_KEY_PAGE_DOWN -> {
                this.detailPanel.scrollBy(this.detailPanel.pageHeight());
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                this.sheetScrollY = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                this.sheetScrollY = this.sheetMaxScrollY;
                return true;
            }
            default -> {
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Jumps to the next option whose name starts with the typed character, so long lists like feats are quick to find. */
    @Override
    public boolean charTyped(char typed, int modifiers) {
        List<Origin> options = currentOptions();
        if (options != null && !options.isEmpty() && Character.isLetterOrDigit(typed)) {
            String prefix = String.valueOf(Character.toLowerCase(typed));
            for (int step = 1; step <= options.size(); step++) {
                int index = Math.floorMod(Math.max(this.keyboardFocusedOption, -1) + step, options.size());
                if (options.get(index).name().getString().toLowerCase(java.util.Locale.ROOT).startsWith(prefix)) {
                    this.keyboardFocusedOption = index;
                    this.scrollToKeyboardFocus = true;
                    triggerImmediateNarration(false);
                    return true;
                }
            }
            return true;
        }
        return super.charTyped(typed, modifiers);
    }

    private void moveFocus(List<Origin> options, int delta) {
        if (this.keyboardFocusedOption < 0) {
            int selected = this.selectedOrigin == null ? -1 : options.indexOf(this.selectedOrigin);
            this.keyboardFocusedOption = Math.max(0, selected);
        } else {
            this.keyboardFocusedOption = Mth.clamp(this.keyboardFocusedOption + delta, 0, options.size() - 1);
        }
        this.scrollToKeyboardFocus = true;
        this.keyboardFocusedSeal = -1;
        triggerImmediateNarration(false);
    }

    /** Players step through completed seals with Shift+arrows; in the read-only view, plain arrows browse the picks. */
    private void moveKeyboardSealFocus(int delta) {
        List<Integer> order = completedLayerOrder();
        if (order.isEmpty()) return;
        int current = order.indexOf(this.readOnly ? indexOfSelectedPick() : this.keyboardFocusedSeal);
        int next = current < 0 ? (delta < 0 ? order.size() - 1 : 0) : Mth.clamp(current + delta, 0, order.size() - 1);
        int layerIndex = order.get(next);
        if (this.readOnly) {
            this.selectedOrigin = this.confirmedSelections.get(layerIndex);
            this.detailPanel.resetScroll();
        } else {
            this.keyboardFocusedSeal = layerIndex;
            if (this.armedRevertIndex != layerIndex) this.armedRevertIndex = -1;
        }
        triggerImmediateNarration(false);
    }

    private int indexOfSelectedPick() {
        for (Map.Entry<Integer, Origin> entry : this.confirmedSelections.entrySet()) {
            if (sameOrigin(entry.getValue(), this.selectedOrigin)) return entry.getKey();
        }
        return -1;
    }

    private void armRevert(int layerIndex) {
        this.armedRevertIndex = layerIndex;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.7F));
        triggerImmediateNarration(false);
    }

    /** We offer to leave on Esc; the server keeps the session, so the player resumes these picks on return. */
    private void confirmLeave() {
        Minecraft mc = Minecraft.getInstance();
        Screen self = this;
        mc.setScreen(new ConfirmScreen(leave -> {
            if (leave) {
                leaveWorld(mc);
            } else {
                mc.setScreen(self);
            }
        }, Component.translatable("raven_dnd_origins.gui.leave.title"), Component.translatable("raven_dnd_origins.gui.leave.message")));
    }

    /** Mirrors vanilla's pause-menu disconnect so singleplayer saves and multiplayer returns to the server list. */
    private static void leaveWorld(Minecraft mc) {
        boolean local = mc.isLocalServer();
        ServerData server = mc.getCurrentServer();
        if (mc.level != null) {
            mc.level.disconnect();
        }
        if (local) {
            mc.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            mc.disconnect();
        }
        TitleScreen title = new TitleScreen();
        if (local) {
            mc.setScreen(title);
        } else if (server != null && server.isRealm()) {
            mc.setScreen(new RealmsMainScreen(title));
        } else {
            mc.setScreen(new JoinMultiplayerScreen(title));
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.detailPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }

        if (this.activeLayerOptionsMaxScrollY > 0 && this.trayRect != null && this.trayRect.contains(mouseX, mouseY)) {
            boolean canScrollActive = scrollY > 0
                    ? this.activeLayerOptionsScrollY > 0
                    : scrollY < 0 && this.activeLayerOptionsScrollY < this.activeLayerOptionsMaxScrollY;
            if (canScrollActive) {
                int ny = this.activeLayerOptionsScrollY - (int) scrollY * 12;
                this.activeLayerOptionsScrollY = Mth.clamp(ny, 0, this.activeLayerOptionsMaxScrollY);
                return true;
            }
        }

        OriginScreenLayout.Rect sheet = this.layout.sheet();
        if (this.sheetMaxScrollY > 0 && sheet != null && sheet.contains(mouseX, mouseY)) {
            this.sheetScrollY = Mth.clamp(this.sheetScrollY - (int) scrollY * 12, 0, this.sheetMaxScrollY);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.detailPanel.mouseDragged(mouseY, button)) {
            return true;
        }
        if (this.draggingTrayScrollbar && button == 0) {
            dragTrayScrollbar(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.detailPanel.mouseReleased();
        this.draggingTrayScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.readOnly;
    }
}
