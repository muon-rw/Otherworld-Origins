package dev.muon.raven_dnd_origins.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.network.C2SRevertLayerOriginsMessage;
import dev.muon.raven_dnd_origins.network.SelectionSessionFinishedMessage;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.network.OriginsClientNetwork;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

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

    private final List<OriginLayer> layerList;
    private int currentLayerIndex;

    private final Map<Integer, Origin> confirmedSelections = new HashMap<>();
    private final Map<Integer, List<Origin>> layerOriginCache = new HashMap<>();

    private Origin hoveredOrigin = null;
    private Origin selectedOrigin = null;

    private float[] cardExpandProgress = new float[0];
    /** Layer index of a completed (non-active) row slot hovered last frame; drives expand animation. */
    @Nullable
    private Integer hoveredCompletedLayerIndex = null;
    private final Map<Integer, Float> completedCardExpandProgress = new HashMap<>();
    /** Vertical scroll for the active layer's origin options only (under the layer title). */
    private int activeLayerOptionsScrollY = 0;
    private int activeLayerOptionsMaxScrollY = 0;
    private float time = 0.0f;

    private List<FormattedCharSequence> sheetLines = new ArrayList<>();
    private final SessionKind kind;

    /** Queued during active-layer row render (under scissor); drawn in {@link #renderLeftPanel} after scissor ends. */
    private boolean pendingConfirmSelectionHint;
    private int pendingConfirmHintScreenX;
    private int pendingConfirmHintScreenY;

    private static final int LEFT_PANEL_WIDTH = 160;
    private static final int RIGHT_PANEL_WIDTH = OriginDetailPanel.DEFAULT_WIDTH;
    private static final int CARD_COLLAPSED_WIDTH = 16;
    private static final int CARD_EXPANDED_WIDTH = 32;
    private static final int CARD_HEIGHT = 32;
    private static final int ICON_SIZE = 16;
    /** Padding around each grid icon for the translucent hover highlight (see {@link #renderIconGrid}). */
    private static final int ICON_GRID_HOVER_PAD = 2;
    private static final int ICON_GRID_HOVER_BOX = ICON_SIZE + 2 * ICON_GRID_HOVER_PAD;
    private static final int COMPLETED_ROW_GAP = 4;
    private static final int COMPLETED_NAME_GAP = 4;
    /** Horizontal slot when a completed choice shows icon only (no name). */
    private static final int COMPLETED_ICON_COLLAPSED_WIDTH = ICON_SIZE + 2;
    private static final int COMPLETED_PORTRAIT_HEIGHT = 36;
    private static final int COMPLETED_ICON_HEIGHT = 22;
    private static final ResourceLocation CHARACTER_SHEET = RavenDndOrigins.loc("textures/gui/character_sheet.png");

    /**
     * Chooses left vs right of the cursor and clamps to the screen so the confirm hint is not
     * clipped by edges (unlike fixed badge positioning when the cursor sits on the left stack).
     */
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

    private Button selectButton;
    private final OriginDetailPanel detailPanel = new OriginDetailPanel();

    public RavenDndOriginScreen(List<OriginLayer> layerList, int startLayerIndex, SessionKind kind) {
        super(Component.translatable("origins.screen.choose_origin"));
        this.layerList = layerList;
        this.currentLayerIndex = startLayerIndex;
        this.kind = kind;
    }

    @Override
    protected void init() {
        super.init();

        int rightPanelX = this.width - RIGHT_PANEL_WIDTH - 10;
        this.selectButton = this.addRenderableWidget(Button.builder(
            Component.translatable("origins.gui.select"),
            b -> confirmSelection()
        ).bounds(rightPanelX, this.height - 30, RIGHT_PANEL_WIDTH, 20).build());
        this.selectButton.visible = false;

        evaluateCurrentLayer();
        RavenDndOrigins.LOGGER.debug("[OWOriginScreen] init: {} layers, currentLayerIndex={}, kind={}, confirmedSelections={}",
                this.layerList.size(), this.currentLayerIndex, this.kind, this.confirmedSelections.size());
    }

    private static boolean sameOrigin(@Nullable Origin a, @Nullable Origin b) {
        return a != null && b != null && a.id().equals(b.id());
    }

    private void evaluateCurrentLayer() {
        if (this.currentLayerIndex >= this.layerList.size()) {
            finishSelection();
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

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

        if (availableOrigins.isEmpty()) {
            this.currentLayerIndex++;
            evaluateCurrentLayer();
            return;
        }

        this.layerOriginCache.put(this.currentLayerIndex, availableOrigins);
        this.cardExpandProgress = new float[availableOrigins.size()];
        this.selectedOrigin = null;
        this.hoveredOrigin = null;
        this.detailPanel.resetScroll();
        this.activeLayerOptionsScrollY = 0;

        updateButtonStates();
        rebuildCharacterSheetText();
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

        this.currentLayerIndex++;
        evaluateCurrentLayer();
    }

    private void revertToLayer(int index) {
        if (index >= this.currentLayerIndex) return;

        List<ResourceLocation> layersToRevert = new ArrayList<>();
        for (int i = index; i < this.layerList.size(); i++) {
            this.confirmedSelections.remove(i);
            this.layerOriginCache.remove(i);
            this.completedCardExpandProgress.remove(i);
            layersToRevert.add(this.layerList.get(i).id());
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
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F));
        Minecraft.getInstance().setScreen(null);
        PacketDistributor.sendToServer(new SelectionSessionFinishedMessage());
    }

    private void updateButtonStates() {
        this.selectButton.active = this.selectedOrigin != null;
    }

    /**
     * Origin whose details are shown in the right panel: completed-row hover wins, then active-row
     * hover, then the active-layer selection.
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
        return id.equals(RavenDndOrigins.loc("race")) || id.equals(RavenDndOrigins.loc("subrace"));
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
        int apt1 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_one"));
        int apt2 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_two"));
        int free = findLayerIndexForId(RavenDndOrigins.loc("free_feat"));
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
        int apt1 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_one"));
        int apt2 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_two"));
        int free = findLayerIndexForId(RavenDndOrigins.loc("free_feat"));
        if (apt1 < 0 || apt2 < 0 || free < 0) return -1;
        return Math.min(apt1, Math.min(apt2, free));
    }

    /** Draconic Bloodline: class, subclass, and draconic ancestry share one completed row (item icons). */
    private boolean isSubclassDraconicBloodline() {
        int subIdx = findLayerIndexForId(RavenDndOrigins.loc("subclass"));
        if (subIdx < 0) return false;
        Origin sub = this.confirmedSelections.get(subIdx);
        return sub != null && sub.id().equals(RavenDndOrigins.loc("subclass/sorcerer/draconic_bloodline"));
    }

    private boolean isClassSubclassDraconicTripleComplete() {
        if (!isSubclassDraconicBloodline()) return false;
        int classIdx = findLayerIndexForId(RavenDndOrigins.loc("class"));
        int subIdx = findLayerIndexForId(RavenDndOrigins.loc("subclass"));
        int dracIdx = findLayerIndexForId(RavenDndOrigins.loc("draconic_ancestry"));
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
        int classIdx = findLayerIndexForId(RavenDndOrigins.loc("class"));
        int subIdx = findLayerIndexForId(RavenDndOrigins.loc("subclass"));
        int dracIdx = findLayerIndexForId(RavenDndOrigins.loc("draconic_ancestry"));
        if (classIdx < 0 || subIdx < 0 || dracIdx < 0) return -1;
        return Math.min(classIdx, Math.min(subIdx, dracIdx));
    }

    private int getEffectiveCardCollapsedWidth() {
        int paperLeft = this.width / 2 - 128;
        int availableWidth = paperLeft - 10 - 4;
        int optionCount = 0;
        if (this.currentLayerIndex < this.layerList.size()) {
            List<Origin> options = this.layerOriginCache.get(this.currentLayerIndex);
            if (options != null) optionCount = options.size();
        }
        if (optionCount <= 1) return CARD_COLLAPSED_WIDTH;
        int maxCollapsed = Math.max(8, (availableWidth - CARD_EXPANDED_WIDTH) / (optionCount - 1));
        return Math.min(CARD_COLLAPSED_WIDTH, maxCollapsed);
    }

    private int getEffectiveCardExpandedWidth() {
        int paperLeft = this.width / 2 - 128;
        int availableWidth = paperLeft - 10 - 4;
        return Math.min(CARD_EXPANDED_WIDTH, availableWidth);
    }

    private int[] computeFixedWidthCardWidths(int optionCount) {
        int effCollapsed = getEffectiveCardCollapsedWidth();
        int effExpanded = getEffectiveCardExpandedWidth();
        int totalFixedWidth = optionCount * effCollapsed;

        float[] desired = new float[optionCount];
        float totalDesired = 0;
        for (int i = 0; i < optionCount; i++) {
            float progress = i < this.cardExpandProgress.length ? this.cardExpandProgress[i] : 0;
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
                    float target = isHovered ? 1.0f : 0.0f;
                    this.cardExpandProgress[i] = animStep(this.cardExpandProgress[i], target);
                }
            }
        }

        for (Map.Entry<Integer, Origin> entry : this.confirmedSelections.entrySet()) {
            int layerIdx = entry.getKey();
            if (layerIdx >= this.currentLayerIndex) continue;
            float target = (this.hoveredCompletedLayerIndex != null && this.hoveredCompletedLayerIndex.equals(layerIdx))
                    ? 1.0f
                    : 0.0f;
            float cur = this.completedCardExpandProgress.getOrDefault(layerIdx, 0.0f);
            this.completedCardExpandProgress.put(layerIdx, animStep(cur, target));
        }
    }

    private int getCompletedCollapsedWidth(OriginLayer layer) {
        return isPortraitLayer(layer) ? CARD_COLLAPSED_WIDTH : COMPLETED_ICON_COLLAPSED_WIDTH;
    }

    private int getCompletedSlotWidth(OriginLayer layer, Origin origin, float expandProgress) {
        float p = Mth.clamp(expandProgress, 0.0f, 1.0f);
        int collapsed = getCompletedCollapsedWidth(layer);
        int nameW = this.font.width(origin.name());
        int expanded = collapsed + COMPLETED_NAME_GAP + nameW;
        return (int) Mth.lerp(p, collapsed, expanded);
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
            if (!RavenDndOrigins.loc("class").equals(this.layerList.get(idx).id())) continue;
            Origin origin = entry.getValue();
            return origin != null && RavenDndOrigins.loc("class/warlock").equals(origin.id());
        }
        return false;
    }

    private void rebuildCharacterSheetText() {
        this.sheetLines.clear();

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Component playerName = player.getName().copy().withStyle(ChatFormatting.ITALIC);

        String race = getOriginDisplayName(RavenDndOrigins.loc("race"));
        String subrace = getOriginDisplayName(RavenDndOrigins.loc("subrace"));
        String className = getOriginDisplayName(RavenDndOrigins.loc("class"));
        String subclassName = getOriginDisplayName(RavenDndOrigins.loc("subclass"));

        if (subrace != null && race != null && subrace.endsWith(" " + race)) {
            subrace = subrace.substring(0, subrace.length() - race.length() - 1);
        }

        if (race == null) {
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.main_description_name_only", playerName));
            if (this.confirmedSelections.isEmpty()) {
                this.sheetLines.add(FormattedCharSequence.EMPTY);
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.pick_left_to_start"));
            }
            return;
        }

        Component mainText;
        if (className == null) {
            if (race.equals("Other") || race.equals("Undead")) {
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
                    && (!(race.equals("Other") || race.equals("Undead")) || (subrace != null && !subrace.isEmpty()));

            if (warlockPactLine) {
                String heritageData = (race.equals("Other") || race.equals("Undead"))
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
                if (articleTarget != null && !articleTarget.isEmpty()) {
                    char firstChar = Character.toLowerCase(articleTarget.charAt(0));
                    useAn = (firstChar == 'a' || firstChar == 'e' || firstChar == 'i' ||
                            firstChar == 'o' || firstChar == 'u');
                }

                String prefix = "raven_dnd_origins.gui.final_confirm.";
                if (race.equals("Other") || race.equals("Undead")) {
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

        List<String> feats = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                RavenDndOrigins.loc("free_feat"), RavenDndOrigins.loc("first_feat"),
                RavenDndOrigins.loc("second_feat"), RavenDndOrigins.loc("third_feat"),
                RavenDndOrigins.loc("fourth_feat"), RavenDndOrigins.loc("fifth_feat")
        };
        for (ResourceLocation layerId : featLayerIds) {
            String featName = getOriginDisplayName(layerId);
            if (featName != null) feats.add(featName);
        }
        if (!feats.isEmpty()) {
            this.sheetLines.add(FormattedCharSequence.EMPTY);
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_header").withStyle(style -> style.withUnderlined(true)));
            if (feats.size() == 1) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_single", feats.get(0)));
            } else if (feats.size() == 2) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_double", feats.get(0), feats.get(1)));
            } else {
                String allButLast = String.join(", ", feats.subList(0, feats.size() - 1));
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.feats_multiple", allButLast, feats.get(feats.size() - 1)));
            }
        }

        List<String> cantrips = new ArrayList<>();
        String cantrip1 = getOriginDisplayName(RavenDndOrigins.loc("cantrip_one"));
        String cantrip2 = getOriginDisplayName(RavenDndOrigins.loc("cantrip_two"));
        if (cantrip1 != null) cantrips.add(cantrip1);
        if (cantrip2 != null) cantrips.add(cantrip2);
        for (ResourceLocation layerId : new ResourceLocation[]{
                RavenDndOrigins.loc("elemental_discipline_one"),
                RavenDndOrigins.loc("elemental_discipline_two"),
                RavenDndOrigins.loc("elemental_discipline_three"),
                RavenDndOrigins.loc("elemental_discipline_four")
        }) {
            String elemental = getOriginDisplayName(layerId);
            if (elemental != null) cantrips.add(elemental);
        }
        if (!cantrips.isEmpty()) {
            this.sheetLines.add(FormattedCharSequence.EMPTY);
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_header").withStyle(style -> style.withUnderlined(true)));
            if (cantrips.size() == 1) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_single", cantrips.get(0)));
            } else if (cantrips.size() == 2) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_double", cantrips.get(0), cantrips.get(1)));
            } else {
                String allButLast = String.join(", ", cantrips.subList(0, cantrips.size() - 1));
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_multiple", allButLast, cantrips.get(cantrips.size() - 1)));
            }
        }

        List<String> plusOnes = new ArrayList<>();
        List<String> plusTwos = new ArrayList<>();
        String plusOne1 = getOriginDisplayName(RavenDndOrigins.loc("plus_one_aptitude_one"));
        String plusOne2 = getOriginDisplayName(RavenDndOrigins.loc("plus_one_aptitude_two"));
        String plusOneResilient = getOriginDisplayName(RavenDndOrigins.loc("plus_one_aptitude_resilient"));
        String plusTwo1 = getOriginDisplayName(RavenDndOrigins.loc("plus_two_aptitude_one"));
        String plusTwo2 = getOriginDisplayName(RavenDndOrigins.loc("plus_two_aptitude_two"));

        if (plusOne1 != null) plusOnes.add(plusOne1);
        if (plusOne2 != null) plusOnes.add(plusOne2);
        if (plusOneResilient != null) plusOnes.add(plusOneResilient);
        if (plusTwo1 != null) plusTwos.add(plusTwo1);
        if (plusTwo2 != null) plusTwos.add(plusTwo2);

        if (!plusOnes.isEmpty() || !plusTwos.isEmpty()) {
            this.sheetLines.add(FormattedCharSequence.EMPTY);
            addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitudes_header").withStyle(style -> style.withUnderlined(true)));
            if (!plusTwos.isEmpty()) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitude_plus_two", String.join(", ", plusTwos)));
            }
            if (!plusOnes.isEmpty()) {
                addSheetText(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitude_plus_one", String.join(", ", plusOnes)));
            }
        }
    }

    private void addSheetText(Component text) {
        this.sheetLines.addAll(this.font.split(text, 190));
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
        return null;
    }

    private Component getTitleText() {
        if (this.currentLayerIndex >= 0 && this.currentLayerIndex < this.layerList.size()) {
            OriginLayer currentLayer = this.layerList.get(this.currentLayerIndex);
            ResourceLocation layerId = currentLayer.id();

            if (layerId.equals(RavenDndOrigins.loc("wildshape"))) {
                return Component.translatable("raven_dnd_origins.gui.wildshape_choose_title");
            }

            String sourceName = null;
            if (layerId.equals(RavenDndOrigins.loc("cantrip_one"))) {
                sourceName = getOriginDisplayName(RavenDndOrigins.loc("subrace"));
            } else if (layerId.equals(RavenDndOrigins.loc("cantrip_two"))) {
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
            if (RavenDndOrigins.loc("subclass").equals(this.layerList.get(entry.getKey()).id())) {
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
        return getOriginDisplayName(RavenDndOrigins.loc("class"));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.time += delta;
        this.renderBackground(graphics, mouseX, mouseY, delta);

        renderCenterPanel(graphics, mouseX, mouseY);
        renderLeftPanel(graphics, mouseX, mouseY);
        this.detailPanel.render(graphics, getRightPanelDisplayOrigin(), this.width - RIGHT_PANEL_WIDTH - 10,
                RIGHT_PANEL_WIDTH, mouseX, mouseY, this.time);

        boolean hasInfoPanel = getRightPanelDisplayOrigin() != null;
        this.selectButton.visible = hasInfoPanel;
        this.selectButton.active = this.selectedOrigin != null;

        // Widgets only: Screen#render would repaint the background over the panels drawn above.
        for (Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, delta);
        }
    }

    @Nullable
    private Integer findConfirmedPairIndex(int index) {
        ResourceLocation layerId = this.layerList.get(index).id();

        ResourceLocation pairedId;
        if (layerId.equals(RavenDndOrigins.loc("race"))) pairedId = RavenDndOrigins.loc("subrace");
        else if (layerId.equals(RavenDndOrigins.loc("class"))) pairedId = RavenDndOrigins.loc("subclass");
        else if (layerId.equals(RavenDndOrigins.loc("cantrip_one"))) pairedId = RavenDndOrigins.loc("cantrip_two");
        else if (layerId.equals(RavenDndOrigins.loc("cantrip_two"))) pairedId = RavenDndOrigins.loc("cantrip_one");
        else if (layerId.equals(RavenDndOrigins.loc("plus_two_aptitude_one"))) pairedId = RavenDndOrigins.loc("plus_two_aptitude_two");
        else if (layerId.equals(RavenDndOrigins.loc("plus_two_aptitude_two"))) pairedId = RavenDndOrigins.loc("plus_two_aptitude_one");
        else if (layerId.equals(RavenDndOrigins.loc("plus_one_aptitude_one"))) pairedId = RavenDndOrigins.loc("plus_one_aptitude_two");
        else if (layerId.equals(RavenDndOrigins.loc("plus_one_aptitude_two"))) pairedId = RavenDndOrigins.loc("plus_one_aptitude_one");
        else if (layerId.equals(RavenDndOrigins.loc("first_feat"))) pairedId = RavenDndOrigins.loc("second_feat");
        else if (layerId.equals(RavenDndOrigins.loc("second_feat"))) pairedId = RavenDndOrigins.loc("first_feat");
        else if (layerId.equals(RavenDndOrigins.loc("third_feat"))) pairedId = RavenDndOrigins.loc("fourth_feat");
        else if (layerId.equals(RavenDndOrigins.loc("fourth_feat"))) pairedId = RavenDndOrigins.loc("third_feat");
        else if (layerId.equals(RavenDndOrigins.loc("elemental_discipline_one"))) pairedId = RavenDndOrigins.loc("elemental_discipline_two");
        else if (layerId.equals(RavenDndOrigins.loc("elemental_discipline_two"))) pairedId = RavenDndOrigins.loc("elemental_discipline_one");
        else if (layerId.equals(RavenDndOrigins.loc("elemental_discipline_three"))) pairedId = RavenDndOrigins.loc("elemental_discipline_four");
        else if (layerId.equals(RavenDndOrigins.loc("elemental_discipline_four"))) pairedId = RavenDndOrigins.loc("elemental_discipline_three");
        else {
            return null;
        }

        if (isAptitudeFreeTripleComplete()) {
            int apt1 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_one"));
            int apt2 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_two"));
            int free = findLayerIndexForId(RavenDndOrigins.loc("free_feat"));
            if (index == apt1 || index == apt2 || index == free) {
                return null;
            }
        }

        if (isClassSubclassDraconicTripleComplete()) {
            int classI = findLayerIndexForId(RavenDndOrigins.loc("class"));
            int subI = findLayerIndexForId(RavenDndOrigins.loc("subclass"));
            int dracI = findLayerIndexForId(RavenDndOrigins.loc("draconic_ancestry"));
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

    private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        this.hoveredCompletedLayerIndex = null;
        this.pendingConfirmSelectionHint = false;
        int x = 10;
        int y = 10;
        int leftViewportBottom = this.height - 10;
        // Right edge for vertical scroll clip: allow drawing over the center sheet until the description panel.
        int leftOptionsClipRight = this.width - RIGHT_PANEL_WIDTH - 10;
        this.activeLayerOptionsMaxScrollY = 0;

        Set<Integer> renderedAsPair = new HashSet<>();

        for (int i = 0; i <= this.currentLayerIndex; i++) {
            if (i >= this.layerList.size()) break;
            if (renderedAsPair.contains(i)) continue;

            OriginLayer layer = this.layerList.get(i);
            boolean isActive = (i == this.currentLayerIndex);
            boolean isCompleted = (i < this.currentLayerIndex);

            if (isCompleted && !this.confirmedSelections.containsKey(i)) continue;

            if (isActive) {
                graphics.drawString(this.font, layer.name(), x, y, 0xFFFFFF, true);
                y += 12;

                List<Origin> options = this.layerOriginCache.get(i);
                if (options != null && !options.isEmpty()) {
                    int optionsTop = y;
                    int iconsPerRow = Math.max(1, (Math.max(40, this.width / 2 - 128 - x - 4)) / 20);
                    int contentH;
                    if (isPortraitLayer(layer)) {
                        contentH = CARD_HEIGHT + 10;
                    } else {
                        int rows = (options.size() + iconsPerRow - 1) / iconsPerRow;
                        contentH = rows * 20 + 4;
                    }
                    int visibleH = Math.max(0, leftViewportBottom - optionsTop);
                    this.activeLayerOptionsMaxScrollY = Math.max(0, contentH - visibleH);
                    if (this.activeLayerOptionsScrollY > this.activeLayerOptionsMaxScrollY) {
                        this.activeLayerOptionsScrollY = this.activeLayerOptionsMaxScrollY;
                    }
                    int optionsMouseY = mouseY + this.activeLayerOptionsScrollY;
                    if (optionsTop < leftViewportBottom && leftOptionsClipRight > 10) {
                        /*
                         * Icon grid hover + 1px selection inset extend ICON_GRID_HOVER_PAD past each
                         * cell origin; without extra clip margin the left/top edges are scissored off.
                         */
                        int clipPad = isPortraitLayer(layer) ? 0 : ICON_GRID_HOVER_PAD;
                        int clipLeft = Math.max(0, 10 - clipPad);
                        int clipTop = Math.max(0, optionsTop - clipPad);
                        graphics.enableScissor(clipLeft, clipTop, leftOptionsClipRight, leftViewportBottom);
                        graphics.pose().pushPose();
                        graphics.pose().translate(0.0f, (float) -this.activeLayerOptionsScrollY, 0.0f);
                        if (isPortraitLayer(layer)) {
                            renderPortraitRow(graphics, options, x, optionsTop, mouseX, optionsMouseY, mouseX, mouseY);
                        } else {
                            renderIconGrid(graphics, options, x, optionsTop, mouseX, optionsMouseY, mouseX, mouseY);
                        }
                        graphics.pose().popPose();
                        graphics.disableScissor();
                        if (this.pendingConfirmSelectionHint) {
                            renderConfirmSelectionHintTooltip(
                                    graphics, this.pendingConfirmHintScreenX, this.pendingConfirmHintScreenY);
                            this.pendingConfirmSelectionHint = false;
                        }
                    }
                    y += contentH;
                }
            } else {
                int tripleAnchor = aptitudeFreeTripleAnchorIndex();
                if (tripleAnchor >= 0 && isAptitudeFreeTripleComplete() && i == tripleAnchor) {
                    int apt1 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_one"));
                    int apt2 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_two"));
                    int free = findLayerIndexForId(RavenDndOrigins.loc("free_feat"));
                    renderedAsPair.add(apt1);
                    renderedAsPair.add(apt2);
                    renderedAsPair.add(free);
                    renderCompletedTripleRow(graphics, apt1, apt2, free, x, y, mouseX, mouseY);
                } else {
                    int draconicAnchor = classSubclassDraconicTripleAnchorIndex();
                    if (draconicAnchor >= 0 && isClassSubclassDraconicTripleComplete() && i == draconicAnchor) {
                        int classI = findLayerIndexForId(RavenDndOrigins.loc("class"));
                        int subI = findLayerIndexForId(RavenDndOrigins.loc("subclass"));
                        int dracI = findLayerIndexForId(RavenDndOrigins.loc("draconic_ancestry"));
                        renderedAsPair.add(classI);
                        renderedAsPair.add(subI);
                        renderedAsPair.add(dracI);
                        renderCompletedTripleRow(graphics, classI, subI, dracI, x, y, mouseX, mouseY);
                    } else {
                        Integer pairIdx = findConfirmedPairIndex(i);
                        if (pairIdx != null) {
                            renderedAsPair.add(pairIdx);
                            int leftIdx = Math.min(i, pairIdx);
                            int rightIdx = Math.max(i, pairIdx);
                            renderCompletedPairRow(graphics, leftIdx, rightIdx, x, y, mouseX, mouseY);
                        } else {
                            renderCompactCompletedRow(graphics, layer, this.confirmedSelections.get(i), x, y, mouseX, mouseY, i);
                        }
                    }
                }
                y += isPortraitLayer(layer) ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
            }
        }
    }

    private void renderCompletedPairRow(GuiGraphics graphics, int leftIdx, int rightIdx, int x, int y, int mouseX, int mouseY) {
        OriginLayer leftLayer = this.layerList.get(leftIdx);
        Origin leftOrigin = this.confirmedSelections.get(leftIdx);
        OriginLayer rightLayer = this.layerList.get(rightIdx);
        Origin rightOrigin = this.confirmedSelections.get(rightIdx);
        boolean portrait = isPortraitLayer(leftLayer);
        int rowH = portrait ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
        int nameYOff = portrait ? 12 : 4;

        float pL = this.completedCardExpandProgress.getOrDefault(leftIdx, 0.0f);
        float pR = this.completedCardExpandProgress.getOrDefault(rightIdx, 0.0f);
        int wL = getCompletedSlotWidth(leftLayer, leftOrigin, pL);
        int wR = getCompletedSlotWidth(rightLayer, rightOrigin, pR);

        int lx = x;
        int rx = lx + wL + COMPLETED_ROW_GAP;

        boolean leftHov = mouseX >= lx && mouseX < lx + wL && mouseY >= y && mouseY < y + rowH;
        boolean rightHov = mouseX >= rx && mouseX < rx + wR && mouseY >= y && mouseY < y + rowH;

        if (leftHov) {
            this.hoveredCompletedLayerIndex = leftIdx;
        } else if (rightHov) {
            this.hoveredCompletedLayerIndex = rightIdx;
        }

        if (leftHov) graphics.fill(lx - 2, y - 2, lx + wL, y + rowH, 0x22FFFFFF);
        if (rightHov) graphics.fill(rx - 2, y - 2, rx + wR, y + rowH, 0x22FFFFFF);

        renderCompletedSlot(graphics, leftLayer, leftOrigin, lx, y, wL, rowH, pL, nameYOff, leftHov);
        renderCompletedSlot(graphics, rightLayer, rightOrigin, rx, y, wR, rowH, pR, nameYOff, rightHov);
    }

    private void renderCompletedTripleRow(GuiGraphics graphics, int leftIdx, int midIdx, int rightIdx, int x, int y, int mouseX, int mouseY) {
        OriginLayer leftLayer = this.layerList.get(leftIdx);
        Origin leftOrigin = this.confirmedSelections.get(leftIdx);
        OriginLayer midLayer = this.layerList.get(midIdx);
        Origin midOrigin = this.confirmedSelections.get(midIdx);
        OriginLayer rightLayer = this.layerList.get(rightIdx);
        Origin rightOrigin = this.confirmedSelections.get(rightIdx);

        boolean portrait = isPortraitLayer(leftLayer);
        int rowH = portrait ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
        int nameYOff = portrait ? 12 : 4;

        float pL = this.completedCardExpandProgress.getOrDefault(leftIdx, 0.0f);
        float pM = this.completedCardExpandProgress.getOrDefault(midIdx, 0.0f);
        float pR = this.completedCardExpandProgress.getOrDefault(rightIdx, 0.0f);
        int wL = getCompletedSlotWidth(leftLayer, leftOrigin, pL);
        int wM = getCompletedSlotWidth(midLayer, midOrigin, pM);
        int wR = getCompletedSlotWidth(rightLayer, rightOrigin, pR);

        int lx = x;
        int mx = lx + wL + COMPLETED_ROW_GAP;
        int rx = mx + wM + COMPLETED_ROW_GAP;

        boolean leftHov = mouseX >= lx && mouseX < lx + wL && mouseY >= y && mouseY < y + rowH;
        boolean midHov = mouseX >= mx && mouseX < mx + wM && mouseY >= y && mouseY < y + rowH;
        boolean rightHov = mouseX >= rx && mouseX < rx + wR && mouseY >= y && mouseY < y + rowH;

        if (leftHov) {
            this.hoveredCompletedLayerIndex = leftIdx;
        } else if (midHov) {
            this.hoveredCompletedLayerIndex = midIdx;
        } else if (rightHov) {
            this.hoveredCompletedLayerIndex = rightIdx;
        }

        if (leftHov) graphics.fill(lx - 2, y - 2, lx + wL, y + rowH, 0x22FFFFFF);
        if (midHov) graphics.fill(mx - 2, y - 2, mx + wM, y + rowH, 0x22FFFFFF);
        if (rightHov) graphics.fill(rx - 2, y - 2, rx + wR, y + rowH, 0x22FFFFFF);

        renderCompletedSlot(graphics, leftLayer, leftOrigin, lx, y, wL, rowH, pL, nameYOff, leftHov);
        renderCompletedSlot(graphics, midLayer, midOrigin, mx, y, wM, rowH, pM, nameYOff, midHov);
        renderCompletedSlot(graphics, rightLayer, rightOrigin, rx, y, wR, rowH, pR, nameYOff, rightHov);
    }

    private void renderCompactCompletedRow(GuiGraphics graphics, OriginLayer layer, Origin origin, int x, int y, int mouseX, int mouseY, int layerIndex) {
        boolean portrait = isPortraitLayer(layer);
        int rowH = portrait ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
        float p = this.completedCardExpandProgress.getOrDefault(layerIndex, 0.0f);
        int slotW = getCompletedSlotWidth(layer, origin, p);
        boolean isHovered = mouseX >= x && mouseX < x + slotW && mouseY >= y && mouseY < y + rowH;

        if (isHovered) {
            this.hoveredCompletedLayerIndex = layerIndex;
            graphics.fill(x - 2, y - 2, x + slotW, y + rowH, 0x22FFFFFF);
        }

        int nameYOff = portrait ? 12 : 4;
        renderCompletedSlot(graphics, layer, origin, x, y, slotW, rowH, p, nameYOff, isHovered);
    }

    private void renderCompletedSlot(GuiGraphics graphics, OriginLayer layer, Origin origin, int drawX, int y, int slotWidth, int rowHeight, float expandProgress, int nameYOff, boolean bright) {
        int iconW = getCompletedCollapsedWidth(layer);
        if (isPortraitLayer(layer)) {
            ResourceLocation texture = getPortraitTexture(origin.icon());
            if (!bright) RenderSystem.setShaderColor(0.6f, 0.6f, 0.6f, 1.0f);
            graphics.blit(texture, drawX, y, 0, 0, CARD_COLLAPSED_WIDTH, CARD_HEIGHT, 32, 32);
            if (!bright) RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            OriginDetailPanel.renderIcon(graphics, origin, drawX, y);
        }
        int textAvail = slotWidth - iconW - COMPLETED_NAME_GAP;
        if (textAvail > 0 && expandProgress > 0.02f) {
            int textX = drawX + iconW + COMPLETED_NAME_GAP;
            int textColor = bright ? 0xFFFFFF : 0xAAAAAA;
            graphics.enableScissor(textX, y, drawX + slotWidth, y + rowHeight);
            graphics.drawString(this.font, origin.name(), textX, y + nameYOff, textColor, true);
            graphics.disableScissor();
        }
    }

    /** 1px border along the inside edge of a rectangular sprite (top/bottom full width; sides omit corner pixels). */
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
        Component tip = Component.translatable(
                "raven_dnd_origins.gui.choose_origin_double_click_hint",
                Component.translatable("origins.gui.select"));
        int margin = 4;
        int gap = 12;
        int spaceLeft = Math.max(48, screenMouseX - margin - gap);
        int spaceRight = Math.max(48, this.width - margin - gap - screenMouseX);
        int widthLimit = Mth.clamp(Math.max(spaceLeft, spaceRight), 72, 280);
        graphics.renderTooltip(this.font, this.font.split(tip, widthLimit),
                this.confirmHintTooltipPositioner, screenMouseX, screenMouseY);
    }

    /**
     * @param mouseX hit-test coords (Y includes {@link #activeLayerOptionsScrollY} to match the scrolled pose stack)
     * @param screenMouseX real cursor position for tooltips
     */
    private void renderPortraitRow(GuiGraphics graphics, List<Origin> options, int startX, int startY, int mouseX, int mouseY, int screenMouseX, int screenMouseY) {
        boolean hoveredAny = false;
        int effCollapsed = getEffectiveCardCollapsedWidth();
        int totalFixedWidth = options.size() * effCollapsed;
        int[] cardWidths = computeFixedWidthCardWidths(options.size());

        boolean anyCardHovered = mouseX >= startX && mouseX < startX + totalFixedWidth && mouseY >= startY && mouseY < startY + CARD_HEIGHT;
        if (anyCardHovered) {
            graphics.fill(startX, startY, startX + totalFixedWidth, startY + CARD_HEIGHT, 0x22FFFFFF);
        }

        int x = startX;
        for (int i = 0; i < options.size(); i++) {
            Origin origin = options.get(i);
            int width = cardWidths[i];

            boolean isHovered = mouseX >= x && mouseX < x + width && mouseY >= startY && mouseY < startY + CARD_HEIGHT;
            if (isHovered) {
                this.hoveredOrigin = origin;
                hoveredAny = true;
            }

            ResourceLocation texture = getPortraitTexture(origin.icon());
            int uvX = (32 - width) / 2;
            graphics.blit(texture, x, startY, width, CARD_HEIGHT, uvX, 0, width, CARD_HEIGHT, 32, 32);

            boolean isSelected = sameOrigin(this.selectedOrigin, origin);
            if (isSelected) {
                fillInnerBorder1px(graphics, x, startY, width, CARD_HEIGHT, 0xFFFFFFFF);
            }

            if (isSelected && isHovered) {
                this.pendingConfirmSelectionHint = true;
                this.pendingConfirmHintScreenX = screenMouseX;
                this.pendingConfirmHintScreenY = screenMouseY;
            }

            x += width;
        }

        if (!hoveredAny) {
            this.hoveredOrigin = this.selectedOrigin;
        }
    }

    /**
     * @param mouseX hit-test coords (Y includes scroll delta to match the scrolled pose stack)
     * @param screenMouseX real cursor position for tooltips
     */
    private void renderIconGrid(GuiGraphics graphics, List<Origin> options, int startX, int startY, int mouseX, int mouseY, int screenMouseX, int screenMouseY) {
        int x = startX;
        int y = startY;
        int count = 0;
        boolean hoveredAny = false;
        int paperLeft = this.width / 2 - 128;
        int availableWidth = Math.max(40, paperLeft - startX - 4);
        int iconsPerRow = Math.max(1, availableWidth / 20);

        for (Origin origin : options) {
            if (count > 0 && count % iconsPerRow == 0) {
                x = startX;
                y += 20;
            }

            int hx0 = x - ICON_GRID_HOVER_PAD;
            int hy0 = y - ICON_GRID_HOVER_PAD;
            boolean isHovered = mouseX >= hx0 && mouseX < hx0 + ICON_GRID_HOVER_BOX && mouseY >= hy0 && mouseY < hy0 + ICON_GRID_HOVER_BOX;
            if (isHovered) {
                this.hoveredOrigin = origin;
                hoveredAny = true;
                graphics.fill(
                        hx0,
                        hy0,
                        hx0 + ICON_GRID_HOVER_BOX,
                        hy0 + ICON_GRID_HOVER_BOX,
                        0x44FFFFFF);
            }

            OriginDetailPanel.renderIcon(graphics, origin, x, y);

            boolean isSelected = sameOrigin(this.selectedOrigin, origin);
            if (isSelected) {
                fillInnerBorder1px(
                        graphics,
                        hx0,
                        hy0,
                        ICON_GRID_HOVER_BOX,
                        ICON_GRID_HOVER_BOX,
                        0xFFFFFFFF);
            }

            if (isSelected && isHovered) {
                this.pendingConfirmSelectionHint = true;
                this.pendingConfirmHintScreenX = screenMouseX;
                this.pendingConfirmHintScreenY = screenMouseY;
            }

            x += 20;
            count++;
        }

        if (!hoveredAny) {
            this.hoveredOrigin = this.selectedOrigin;
        }
    }

    private ResourceLocation getPortraitTexture(IconData icon) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(icon.stack().getItem());
        if (itemId.getNamespace().equals(RavenDndOrigins.MODID) && itemId.getPath().startsWith("portrait/")) {
            String portraitName = itemId.getPath().substring("portrait/".length());
            return RavenDndOrigins.loc("textures/item/origin_portrait_" + portraitName + ".png");
        }
        return RavenDndOrigins.loc("textures/item/origin_portrait_base.png");
    }

    private void renderCenterPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.kind != SessionKind.INITIAL_CREATION) {
            Origin displayOrigin = getRightPanelDisplayOrigin();
            if (displayOrigin != null) {
                ShapeshiftFormPreview.render(graphics, displayOrigin, this.width / 2, this.height / 2,
                        ShapeshiftFormPreview.DEFAULT_SIZE, this.time);
            }
            return;
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int sheetWidth = 256;
        int sheetHeight = 256;
        int sheetX = centerX - sheetWidth / 2;
        int sheetY = centerY - sheetHeight / 2;

        graphics.blit(CHARACTER_SHEET, sheetX, sheetY, 0, 0, sheetWidth, sheetHeight, sheetWidth, sheetHeight);

        int textY = sheetY + 40;
        int textX = sheetX + (sheetWidth - 190) / 2;

        for (FormattedCharSequence line : this.sheetLines) {
            if (line == FormattedCharSequence.EMPTY) {
                textY += 7;
            } else {
                graphics.drawString(this.font, line, textX, textY, 0x3F3F3F, false);
                textY += 14;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (this.detailPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int x = 10;
        int y = 10;
        int mouseYInt = (int) mouseY;

        Set<Integer> clickedAsPair = new HashSet<>();

        for (int i = 0; i <= this.currentLayerIndex; i++) {
            if (i >= this.layerList.size()) break;
            if (clickedAsPair.contains(i)) continue;

            OriginLayer layer = this.layerList.get(i);
            boolean isActive = (i == this.currentLayerIndex);
            boolean isCompleted = (i < this.currentLayerIndex);

            if (isCompleted && !this.confirmedSelections.containsKey(i)) continue;

            if (isActive) {
                y += 12;

                List<Origin> options = this.layerOriginCache.get(i);
                if (options != null && !options.isEmpty()) {
                    int optionsMouseY = mouseYInt + this.activeLayerOptionsScrollY;
                    if (isPortraitLayer(layer)) {
                        int cx = x;
                        int[] cardWidths = computeFixedWidthCardWidths(options.size());
                        for (int j = 0; j < options.size(); j++) {
                            int width = cardWidths[j];
                            if (mouseX >= cx && mouseX < cx + width && optionsMouseY >= y && optionsMouseY < y + CARD_HEIGHT) {
                                selectOrConfirm(options.get(j));
                                return true;
                            }
                            cx += width;
                        }
                        y += CARD_HEIGHT + 10;
                    } else {
                        int cx = x;
                        int cy = y;
                        int count = 0;
                        int iconsPerRow = Math.max(1, (Math.max(40, this.width / 2 - 128 - x - 4)) / 20);
                        for (Origin option : options) {
                            if (count > 0 && count % iconsPerRow == 0) {
                                cx = x;
                                cy += 20;
                            }
                            int hx0 = cx - ICON_GRID_HOVER_PAD;
                            int hy0 = cy - ICON_GRID_HOVER_PAD;
                            if (mouseX >= hx0 && mouseX < hx0 + ICON_GRID_HOVER_BOX
                                    && optionsMouseY >= hy0 && optionsMouseY < hy0 + ICON_GRID_HOVER_BOX) {
                                selectOrConfirm(option);
                                return true;
                            }
                            cx += 20;
                            count++;
                        }
                        int rows = (options.size() + iconsPerRow - 1) / iconsPerRow;
                        y += rows * 20 + 4;
                    }
                }
            } else {
                boolean portrait = isPortraitLayer(layer);
                int rowH = portrait ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
                int tripleAnchor = aptitudeFreeTripleAnchorIndex();
                if (tripleAnchor >= 0 && isAptitudeFreeTripleComplete() && i == tripleAnchor) {
                    int apt1 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_one"));
                    int apt2 = findLayerIndexForId(RavenDndOrigins.loc("plus_two_aptitude_two"));
                    int free = findLayerIndexForId(RavenDndOrigins.loc("free_feat"));
                    clickedAsPair.add(apt1);
                    clickedAsPair.add(apt2);
                    clickedAsPair.add(free);
                    if (clickCompletedTriple(mouseX, mouseYInt, x, y, apt1, apt2, free)) {
                        return true;
                    }
                } else {
                    int draconicAnchor = classSubclassDraconicTripleAnchorIndex();
                    if (draconicAnchor >= 0 && isClassSubclassDraconicTripleComplete() && i == draconicAnchor) {
                        int classI = findLayerIndexForId(RavenDndOrigins.loc("class"));
                        int subI = findLayerIndexForId(RavenDndOrigins.loc("subclass"));
                        int dracI = findLayerIndexForId(RavenDndOrigins.loc("draconic_ancestry"));
                        clickedAsPair.add(classI);
                        clickedAsPair.add(subI);
                        clickedAsPair.add(dracI);
                        if (clickCompletedTriple(mouseX, mouseYInt, x, y, classI, subI, dracI)) {
                            return true;
                        }
                    } else {
                        Integer pairIdx = findConfirmedPairIndex(i);

                        if (pairIdx != null) {
                            clickedAsPair.add(pairIdx);
                            int leftIdx = Math.min(i, pairIdx);
                            int rightIdx = Math.max(i, pairIdx);
                            OriginLayer leftLayer = this.layerList.get(leftIdx);
                            Origin leftOrigin = this.confirmedSelections.get(leftIdx);
                            OriginLayer rightLayer = this.layerList.get(rightIdx);
                            Origin rightOrigin = this.confirmedSelections.get(rightIdx);
                            float pL = this.completedCardExpandProgress.getOrDefault(leftIdx, 0.0f);
                            float pR = this.completedCardExpandProgress.getOrDefault(rightIdx, 0.0f);
                            int wL = getCompletedSlotWidth(leftLayer, leftOrigin, pL);
                            int wR = getCompletedSlotWidth(rightLayer, rightOrigin, pR);
                            int lx = x;
                            int rx = lx + wL + COMPLETED_ROW_GAP;
                            if (mouseX >= lx && mouseX < lx + wL && mouseYInt >= y && mouseYInt < y + rowH) {
                                revertToLayer(leftIdx);
                                return true;
                            }
                            if (mouseX >= rx && mouseX < rx + wR && mouseYInt >= y && mouseYInt < y + rowH) {
                                revertToLayer(rightIdx);
                                return true;
                            }
                        } else {
                            Origin origin = this.confirmedSelections.get(i);
                            float p = this.completedCardExpandProgress.getOrDefault(i, 0.0f);
                            int slotW = getCompletedSlotWidth(layer, origin, p);
                            if (mouseX >= x && mouseX < x + slotW && mouseYInt >= y && mouseYInt < y + rowH) {
                                revertToLayer(i);
                                return true;
                            }
                        }
                    }
                }
                y += rowH;
            }
        }

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

    private boolean clickCompletedTriple(double mouseX, int mouseY, int x, int y, int leftIdx, int midIdx, int rightIdx) {
        OriginLayer leftLayer = this.layerList.get(leftIdx);
        int rowH = isPortraitLayer(leftLayer) ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
        int wL = getCompletedSlotWidth(leftLayer, this.confirmedSelections.get(leftIdx),
                this.completedCardExpandProgress.getOrDefault(leftIdx, 0.0f));
        int wM = getCompletedSlotWidth(this.layerList.get(midIdx), this.confirmedSelections.get(midIdx),
                this.completedCardExpandProgress.getOrDefault(midIdx, 0.0f));
        int wR = getCompletedSlotWidth(this.layerList.get(rightIdx), this.confirmedSelections.get(rightIdx),
                this.completedCardExpandProgress.getOrDefault(rightIdx, 0.0f));

        int lx = x;
        int mx = lx + wL + COMPLETED_ROW_GAP;
        int rx = mx + wM + COMPLETED_ROW_GAP;

        if (mouseY < y || mouseY >= y + rowH) return false;
        if (mouseX >= lx && mouseX < lx + wL) {
            revertToLayer(leftIdx);
            return true;
        }
        if (mouseX >= mx && mouseX < mx + wM) {
            revertToLayer(midIdx);
            return true;
        }
        if (mouseX >= rx && mouseX < rx + wR) {
            revertToLayer(rightIdx);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.detailPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }

        if (this.activeLayerOptionsMaxScrollY > 0) {
            boolean canScrollActive = scrollY > 0
                    ? this.activeLayerOptionsScrollY > 0
                    : scrollY < 0 && this.activeLayerOptionsScrollY < this.activeLayerOptionsMaxScrollY;
            if (canScrollActive) {
                int ny = this.activeLayerOptionsScrollY - (int) scrollY * 12;
                this.activeLayerOptionsScrollY = ny < 0 ? 0 : Math.min(ny, this.activeLayerOptionsMaxScrollY);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.detailPanel.mouseDragged(mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.detailPanel.mouseReleased();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
