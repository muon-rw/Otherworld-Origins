package dev.muon.raven_dnd_origins.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.client.shapeshift.FakeEntityCache;
import dev.muon.raven_dnd_origins.client.shapeshift.ShapeshiftRenderHelper;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.muon.raven_dnd_origins.network.C2SRevertLayerOriginsMessage;
import dev.muon.raven_dnd_origins.network.SelectionSessionFinishedMessage;
import dev.muon.raven_dnd_origins.power.AllowedSpellsPower;
import dev.muon.raven_dnd_origins.power.LeveledAttributePower;
import dev.muon.raven_dnd_origins.power.ShapeshiftPower;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import dev.muon.raven_dnd_origins.selection.SessionKind;
import dev.muon.raven_dnd_origins.util.ElementalDisciplineSpellDisplay;
import dev.overgrown.apoli.client.ApoliKeyMappings;
import dev.overgrown.apoli.client.IconRenderer;
import dev.overgrown.apoli.data.IconData;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.origins.badge.Badge;
import dev.overgrown.origins.badge.KeybindBadge;
import dev.overgrown.origins.badge.TooltipBadge;
import dev.overgrown.origins.client.BadgeClientState;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.network.OriginsClientNetwork;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginLayer;
import dev.overgrown.origins.origin.OriginManager;
import dev.overgrown.origins.origin.OriginRegistry;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector2i;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    /**
     * Virtual scroll in the right description pane: ranges from 0 through top/bottom overscroll
     * padding plus content overflow. Effective content offset is
     * {@code clamp(virtual - overscroll, 0, rightPanelContentMaxScroll)}.
     */
    private int rightPanelScrollPos = 0;
    /** Pixels of description content that extend past the scissor (updated each render). */
    private int rightPanelContentMaxScroll = 0;
    private static final int RIGHT_PANEL_SCROLL_OVERSCROLL_PX = 24;
    @Nullable
    private ResourceLocation lastRightPanelOriginId = null;
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

    private boolean rightPanelScrollbarDragging;
    private double rightPanelScrollbarDragStartMouseY;
    private int rightPanelScrollbarDragStartEffScroll;

    private static final int LEFT_PANEL_WIDTH = 160;
    private static final int RIGHT_PANEL_WIDTH = 160;
    /**
     * Top of the scrollable description area (below icon/name/impact); matches stock Origins scissor.
     */
    private static final int RIGHT_PANEL_CONTENT_TOP_OFFSET = 28;
    /**
     * Track/thumb widths and thumb height from stock {@code OriginDisplayScreen} (sprite used only as a ruler).
     */
    private static final int RIGHT_PANEL_SCROLLBAR_TRACK_W = 8;
    private static final int RIGHT_PANEL_SCROLLBAR_THUMB_W = 6;
    private static final int RIGHT_PANEL_SCROLLBAR_THUMB_H = 27;
    private static final int RIGHT_PANEL_SCROLLBAR_TRACK_ARGB = 0x44FFFFFF;
    private static final int RIGHT_PANEL_SCROLLBAR_THUMB_ARGB = 0x77FFFFFF;
    private static final int RIGHT_PANEL_SCROLLBAR_THUMB_ACTIVE_ARGB = 0xAAFFFFFF;
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
     * Power badges in the right panel sit next to the screen edge. Vanilla/default tooltip
     * placement grows to the right of the cursor, and Origins line-wrap width uses
     * {@code screenWidth - mouseX}, which collapses there. This positioner keeps the tooltip
     * opening to the left (outer right edge near the cursor, with the same 12px gap vanilla uses).
     */
    private static final ClientTooltipPositioner BADGE_TOOLTIP_POSITIONER = (screenWidth, screenHeight, mouseX, mouseY, tooltipWidth, tooltipHeight) -> {
        Vector2i pos = new Vector2i(mouseX - 12 - tooltipWidth, mouseY - 12);
        if (pos.x < 4) {
            pos.x = 4;
        }
        if (pos.x + tooltipWidth > screenWidth - 4) {
            pos.x = Math.max(4, screenWidth - tooltipWidth - 4);
        }
        int bottomSpace = tooltipHeight + 3;
        if (pos.y + bottomSpace > screenHeight) {
            pos.y = screenHeight - bottomSpace;
        }
        return pos;
    };

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
        this.rightPanelScrollPos = RIGHT_PANEL_SCROLL_OVERSCROLL_PX;
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
                ResourceLocation entityTypeId = getShapeshiftEntityType(displayOrigin);
                if (entityTypeId != null) {
                    Entity previewEntity = FakeEntityCache.getOrCreate(PREVIEW_ENTITY_CACHE_ID, entityTypeId);
                    if (previewEntity instanceof LivingEntity living) {
                        tickWildshapePreviewIdle(living);
                    }
                }
            }
        }
    }

    /** Advances client age + walk decay so {@link InventoryScreen#renderEntityInInventory} shows idle posing. */
    private static void tickWildshapePreviewIdle(LivingEntity living) {
        Player player = Minecraft.getInstance().player;
        double anchorX = player != null ? player.getX() : 0;
        double anchorY = player != null ? player.getY() : 0;
        double anchorZ = player != null ? player.getZ() : 0;

        if (living.tickCount == 0) {
            living.setPos(anchorX, anchorY, anchorZ);
        }

        living.xo = living.getX();
        living.yo = living.getY();
        living.zo = living.getZ();
        living.yBodyRotO = living.yBodyRot;
        living.yRotO = living.getYRot();
        living.yHeadRotO = living.yHeadRot;
        living.xRotO = living.getXRot();

        living.tickCount++;
        living.calculateEntityAnimation(living instanceof FlyingAnimal);
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
        renderRightPanel(graphics, mouseX, mouseY);

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
            renderIcon(graphics, origin, drawX, y);
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

            renderIcon(graphics, origin, x, y);

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
            renderWildshapePreview(graphics);
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

    private static final int PREVIEW_ENTITY_CACHE_ID = Integer.MIN_VALUE;

    private static final float ISOMETRIC_TILT = (float) Math.toRadians(-20);

    private void renderWildshapePreview(GuiGraphics graphics) {
        Origin displayOrigin = getRightPanelDisplayOrigin();
        if (displayOrigin == null) return;

        ResourceLocation entityTypeId = getShapeshiftEntityType(displayOrigin);
        if (entityTypeId == null) return;

        Entity previewEntity = FakeEntityCache.getOrCreate(PREVIEW_ENTITY_CACHE_ID, entityTypeId);
        if (!(previewEntity instanceof LivingEntity living)) return;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        float bbHeight = living.getBbHeight();
        float bbWidth = living.getBbWidth();
        float maxDim = Math.max(bbHeight, bbWidth);
        int scale = Math.max(10, (int) (80.0f / maxDim));

        int yPos = centerY + (int) (bbHeight * scale / 2);

        float spinAngleDeg = (float) Math.toDegrees(this.time * 0.04f);
        renderPreviewEntity(graphics, living, centerX, yPos, scale, 180 + spinAngleDeg);
    }

    private void renderPreviewEntity(GuiGraphics graphics, LivingEntity living, int x, int y,
                                     int scale, float facingDeg) {
        float savedBodyRot = living.yBodyRot;
        float savedBodyRotO = living.yBodyRotO;
        float savedYRot = living.getYRot();
        float savedXRot = living.getXRot();
        float savedHeadRot = living.yHeadRot;
        float savedHeadRotO = living.yHeadRotO;

        living.yBodyRot = facingDeg;
        living.yBodyRotO = facingDeg;
        living.setYRot(facingDeg);
        living.setXRot(0);
        living.yHeadRot = facingDeg;
        living.yHeadRotO = facingDeg;

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = new Quaternionf().rotateX(ISOMETRIC_TILT);
        pose.mul(camera);

        ShapeshiftRenderHelper.setRenderingShapeshiftBody(true);
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, new Vector3f(), pose, camera, living);
        ShapeshiftRenderHelper.setRenderingShapeshiftBody(false);

        living.yBodyRot = savedBodyRot;
        living.yBodyRotO = savedBodyRotO;
        living.setYRot(savedYRot);
        living.setXRot(savedXRot);
        living.yHeadRot = savedHeadRot;
        living.yHeadRotO = savedHeadRotO;
    }

    @Nullable
    private static ResourceLocation getShapeshiftEntityType(Origin origin) {
        Set<ResourceLocation> visited = new HashSet<>();
        for (ResourceLocation powerId : origin.powers()) {
            ResourceLocation found = findShapeshiftType(powerId, visited);
            if (found != null) return found;
        }
        return null;
    }

    @Nullable
    private static ResourceLocation findShapeshiftType(ResourceLocation powerId, Set<ResourceLocation> visited) {
        if (!visited.add(powerId)) return null;
        Power power = ApoliPowers.get(powerId);
        if (power == null) return null;
        if (power.config() instanceof ShapeshiftPower.Configuration cfg) {
            return cfg.entityType();
        }
        if (power.config() instanceof MultiplePower.Cfg multi) {
            for (ResourceLocation sub : multi.subPowerIds()) {
                ResourceLocation found = findShapeshiftType(sub, visited);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static final ResourceLocation SCHOOL_BADGE_PLACEHOLDER =
            ResourceLocation.fromNamespaceAndPath("origins", "textures/gui/badge/isaacfanta/star.png");

    private static final ResourceLocation SCALING_LEVEL_BADGE =
            RavenDndOrigins.loc("textures/gui/wildshape/scaling_level.png");
    private static final ResourceLocation SCALING_MAGIC_BADGE =
            RavenDndOrigins.loc("textures/gui/wildshape/scaling_magic.png");

    private record AllowedSpellBadge(ResourceLocation sprite, List<Component> tooltip) {}

    @Nullable
    private List<Component> pendingAllowedSpellsTooltip;
    private boolean allowedSpellsEverHovered = false;
    private int allowedSpellsTooltipScroll = 0;
    private int allowedSpellsLastHoveredIdx = -1;
    private int allowedSpellsStripX, allowedSpellsStripY, allowedSpellsStripW, allowedSpellsStripH;
    private boolean allowedSpellsStripActive = false;

    private boolean renderAllowedSpellsBadges(GuiGraphics graphics, int rightX, int y, int mouseX, int mouseY, Origin origin) {
        List<AllowedSpellBadge> badges = collectAllowedSpellBadges(origin);
        boolean isSubclass = origin.id().getPath().startsWith("subclass/");
        if (badges.isEmpty() && isSubclass) {
            badges = List.of(new AllowedSpellBadge(SCHOOL_BADGE_PLACEHOLDER, List.of(
                    Component.translatable("raven_dnd_origins.gui.allowed_spells.none")
                            .withStyle(ChatFormatting.GRAY)
            )));
        }
        return renderTopRightBadgeStrip(graphics, rightX, y, mouseX, mouseY, badges,
                "raven_dnd_origins.gui.allowed_spells.hint");
    }

    private boolean renderWildshapeScalingBadges(GuiGraphics graphics, int rightX, int y, int mouseX, int mouseY, Origin origin) {
        if (!origin.id().getPath().startsWith("wildshape/")) return false;
        List<AllowedSpellBadge> badges = buildWildshapeScalingBadges(origin);
        return renderTopRightBadgeStrip(graphics, rightX, y, mouseX, mouseY, badges,
                "raven_dnd_origins.gui.scaling.hint");
    }

    private boolean renderTopRightBadgeStrip(GuiGraphics graphics, int rightX, int y, int mouseX, int mouseY,
                                             List<AllowedSpellBadge> badges, String hintKey) {
        if (badges.isEmpty()) return false;
        int stride = 10;
        int n = badges.size();
        int startX = rightX - (n * stride - 2);
        int stripW = n * stride - 2;
        this.allowedSpellsStripX = startX;
        this.allowedSpellsStripY = y;
        this.allowedSpellsStripW = stripW;
        this.allowedSpellsStripH = 8;
        this.allowedSpellsStripActive = true;

        int hoveredIdx = -1;
        for (int i = 0; i < n; i++) {
            int x = startX + i * stride;
            graphics.blit(badges.get(i).sprite(), x, y, 0, 0, 8, 8, 8, 8);
            if (mouseX >= x && mouseX < x + 8 && mouseY >= y && mouseY < y + 8) {
                hoveredIdx = i;
            }
        }
        if (hoveredIdx >= 0) {
            this.pendingAllowedSpellsTooltip = badges.get(hoveredIdx).tooltip();
            this.allowedSpellsEverHovered = true;
            if (hoveredIdx != this.allowedSpellsLastHoveredIdx) {
                this.allowedSpellsTooltipScroll = 0;
                this.allowedSpellsLastHoveredIdx = hoveredIdx;
            }
        } else {
            this.allowedSpellsLastHoveredIdx = -1;
        }

        if (!this.allowedSpellsEverHovered) {
            float pulse = 0.5f + 0.5f * Mth.sin(this.time * 0.18f);
            int alpha = (int) (0x55 + pulse * 0xAA);
            int color = (alpha << 24) | 0xFFFFFF;
            int x0 = startX - 1;
            int y0 = y - 1;
            int x1 = startX + stripW + 1;
            int y1 = y + 8 + 1;
            graphics.fill(x0, y0, x1, y0 + 1, color);
            graphics.fill(x0, y1 - 1, x1, y1, color);
            graphics.fill(x0, y0, x0 + 1, y1, color);
            graphics.fill(x1 - 1, y0, x1, y1, color);

            Component label = Component.translatable(hintKey);
            int lw = this.font.width(label);
            int lx = startX + stripW - lw;
            int ly = y + 11;
            int textAlpha = (int) (0xAA + pulse * 0x55);
            int textColor = (textAlpha << 24) | 0xFFFFFF;
            graphics.drawString(this.font, label.getVisualOrderText(), lx, ly, textColor, true);
        }
        return true;
    }

    private List<AllowedSpellBadge> buildWildshapeScalingBadges(Origin origin) {
        List<Component> charLines = new ArrayList<>();
        List<Component> magicLines = new ArrayList<>();
        Set<ResourceLocation> visited = new HashSet<>();
        for (ResourceLocation powerId : origin.powers()) {
            collectWildshapeScalingLines(powerId, visited, charLines, magicLines);
        }
        if (charLines.isEmpty() && magicLines.isEmpty()) return List.of();

        List<AllowedSpellBadge> out = new ArrayList<>(2);
        Component note = Component.translatable("raven_dnd_origins.gui.scaling.note")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC);

        List<Component> charTip = new ArrayList<>();
        charTip.add(Component.translatable("raven_dnd_origins.gui.scaling.char_header").withStyle(ChatFormatting.GOLD));
        charTip.addAll(charLines);
        charTip.add(Component.empty());
        charTip.add(note);
        out.add(new AllowedSpellBadge(SCALING_LEVEL_BADGE, charTip));

        List<Component> magicTip = new ArrayList<>();
        magicTip.add(Component.translatable("raven_dnd_origins.gui.scaling.magic_header").withStyle(ChatFormatting.GOLD));
        magicTip.addAll(magicLines);
        magicTip.add(Component.empty());
        magicTip.add(note);
        out.add(new AllowedSpellBadge(SCALING_MAGIC_BADGE, magicTip));
        return out;
    }

    private void collectWildshapeScalingLines(ResourceLocation powerId, Set<ResourceLocation> visited,
                                              List<Component> charLines, List<Component> magicLines) {
        if (!visited.add(powerId)) return;
        Power power = ApoliPowers.get(powerId);
        if (power == null) return;
        if (power.config() instanceof LeveledAttributePower.Configuration cfg) {
            if (cfg.valuePerLevel() != 0.0) {
                Component line = formatScalingLine(cfg);
                if (line != null) {
                    if (cfg.aptitude().isPresent()) {
                        magicLines.add(line);
                    } else {
                        charLines.add(line);
                    }
                }
            }
        }
        if (power.config() instanceof MultiplePower.Cfg multi) {
            for (ResourceLocation sub : multi.subPowerIds()) {
                collectWildshapeScalingLines(sub, visited, charLines, magicLines);
            }
        }
    }

    @Nullable
    private Component formatScalingLine(LeveledAttributePower.Configuration cfg) {
        Optional<Holder<Attribute>> attr = cfg.attributeHolder();
        if (attr.isEmpty()) return null;
        double v = cfg.valuePerLevel();
        String formatted;
        if (cfg.operation().vanillaOperation() == AttributeModifier.Operation.ADD_VALUE) {
            formatted = (v >= 0 ? "+" : "") + formatScalingValue(v);
        } else {
            formatted = (v >= 0 ? "+" : "") + formatScalingValue(v * 100.0) + "%";
        }
        return Component.translatable("raven_dnd_origins.gui.scaling.line",
                Component.translatable(attr.get().value().getDescriptionId()),
                Component.literal(formatted)).withStyle(ChatFormatting.GRAY);
    }

    private static String formatScalingValue(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.format(Locale.ROOT, "%d", (long) v);
        }
        String s = String.format(Locale.ROOT, "%.3f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private List<AllowedSpellBadge> collectAllowedSpellBadges(Origin origin) {
        List<AllowedSpellBadge> categoryBadges = new ArrayList<>();
        Set<String> seenEntries = new HashSet<>();
        Set<ResourceLocation> visited = new HashSet<>();
        List<Component> additionalLines = new ArrayList<>();
        for (ResourceLocation powerId : origin.powers()) {
            collectAllowedSpellBadges(powerId, visited, categoryBadges, seenEntries, additionalLines);
        }
        List<AllowedSpellBadge> out = new ArrayList<>(categoryBadges);
        if (!additionalLines.isEmpty()) {
            List<Component> tt = new ArrayList<>();
            tt.add(Component.translatable("raven_dnd_origins.gui.allowed_spells.additional_header").withStyle(ChatFormatting.GOLD));
            tt.addAll(additionalLines);
            out.add(new AllowedSpellBadge(SCHOOL_BADGE_PLACEHOLDER, tt));
        }
        return out;
    }

    private void collectAllowedSpellBadges(ResourceLocation powerId, Set<ResourceLocation> visited,
                                           List<AllowedSpellBadge> categoryBadges, Set<String> seenEntries,
                                           List<Component> additionalLines) {
        if (!visited.add(powerId)) return;
        Power power = ApoliPowers.get(powerId);
        if (power == null) return;
        if (power.config() instanceof AllowedSpellsPower.Configuration cfg) {
            for (String entry : cfg.entries()) {
                if (!seenEntries.add(entry)) continue;
                if (entry.startsWith("#")) {
                    AllowedSpellBadge b = makeCategoryBadge(entry.substring(1));
                    if (b != null) categoryBadges.add(b);
                } else if (entry.startsWith("@")) {
                    Component line = makeSchoolLine(entry.substring(1));
                    if (line != null) additionalLines.add(line);
                } else {
                    ResourceLocation loc = parseAllowedSpellsLoc(entry);
                    if (loc == null) continue;
                    AbstractSpell spell = SpellRegistry.getSpell(loc);
                    if (spell != null && spell != SpellRegistry.none()) {
                        additionalLines.add(Component.translatable("raven_dnd_origins.gui.allowed_spells.entry",
                                Component.translatable(spell.getComponentId())).withStyle(ChatFormatting.GRAY));
                    }
                }
            }
        }
        if (power.config() instanceof MultiplePower.Cfg multi) {
            for (ResourceLocation sub : multi.subPowerIds()) {
                collectAllowedSpellBadges(sub, visited, categoryBadges, seenEntries, additionalLines);
            }
        }
    }

    @Nullable
    private AllowedSpellBadge makeCategoryBadge(String tagId) {
        ResourceLocation loc = parseAllowedSpellsLoc(tagId);
        if (loc == null) return null;
        TagKey<AbstractSpell> tagKey = TagKey.create(SpellRegistry.SPELL_REGISTRY_KEY, loc);
        Optional<HolderSet.Named<AbstractSpell>> tag = SpellRegistry.REGISTRY.getTag(tagKey);
        if (tag.isEmpty()) return null;
        List<Component> tt = new ArrayList<>();
        tt.add(Component.translatable("raven_dnd_origins.gui.allowed_spells.category_header",
                prettifyName(loc.getPath())).withStyle(ChatFormatting.GOLD));
        for (Holder<AbstractSpell> spell : tag.get()) {
            tt.add(Component.translatable("raven_dnd_origins.gui.allowed_spells.entry",
                    Component.translatable(spell.value().getComponentId())).withStyle(ChatFormatting.GRAY));
        }
        ResourceLocation sprite = RavenDndOrigins.loc("textures/gui/spell_category/" + loc.getPath() + ".png");
        return new AllowedSpellBadge(sprite, tt);
    }

    @Nullable
    private Component makeSchoolLine(String schoolId) {
        ResourceLocation loc = parseAllowedSpellsLoc(schoolId);
        if (loc == null) return null;
        SchoolType school = SchoolRegistry.REGISTRY.get(loc);
        if (school == null) return null;
        return Component.translatable("raven_dnd_origins.gui.allowed_spells.school_line",
                school.getDisplayName().copy()).withStyle(ChatFormatting.GRAY);
    }

    @Nullable
    private static ResourceLocation parseAllowedSpellsLoc(String id) {
        return id.contains(":")
                ? ResourceLocation.tryParse(id)
                : ResourceLocation.tryBuild(IronsSpellbooks.MODID, id);
    }

    private static String prettifyName(String s) {
        if (s.isEmpty()) return s;
        String[] parts = s.split("[_/]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Origin displayOrigin = getRightPanelDisplayOrigin();
        if (displayOrigin == null) {
            this.lastRightPanelOriginId = null;
            return;
        }

        ResourceLocation originId = displayOrigin.id();
        if (!Objects.equals(originId, this.lastRightPanelOriginId)) {
            this.rightPanelScrollPos = RIGHT_PANEL_SCROLL_OVERSCROLL_PX;
            this.lastRightPanelOriginId = originId;
            this.rightPanelContentMaxScroll = 0;
        }

        int panelX = this.width - RIGHT_PANEL_WIDTH - 10;
        int panelY = 20;
        int panelHeight = this.height - 60;
        int textWidth = RIGHT_PANEL_WIDTH - 16;

        graphics.fill(panelX, panelY, panelX + RIGHT_PANEL_WIDTH, panelY + panelHeight, 0x88000000);

        renderIcon(graphics, displayOrigin, panelX + 5, panelY + 5);
        graphics.drawString(this.font, displayOrigin.name(), panelX + 25, panelY + 9, 0xFFFFFF, true);

        this.pendingAllowedSpellsTooltip = null;
        this.allowedSpellsStripActive = false;
        int stripRightX = panelX + RIGHT_PANEL_WIDTH - 5;
        int stripY = panelY + 9;
        boolean spellBadgesShown = RavenDndOriginsConfig.enableSpellRestrictions()
                && renderAllowedSpellsBadges(graphics, stripRightX, stripY, mouseX, mouseY, displayOrigin);
        if (!spellBadgesShown) {
            renderWildshapeScalingBadges(graphics, stripRightX, stripY, mouseX, mouseY, displayOrigin);
        }

        graphics.enableScissor(panelX, panelY + RIGHT_PANEL_CONTENT_TOP_OFFSET, panelX + RIGHT_PANEL_WIDTH, panelY + panelHeight);

        int startY = panelY + RIGHT_PANEL_CONTENT_TOP_OFFSET + 2;
        int o = RIGHT_PANEL_SCROLL_OVERSCROLL_PX;
        int effScroll = this.rightPanelContentMaxScroll <= 0
                ? 0
                : Mth.clamp(this.rightPanelScrollPos - o, 0, this.rightPanelContentMaxScroll);

        this.renderedBadges.clear();

        int y = startY - effScroll;

        Component orgDesc = appendExtraInfo(displayOrigin.description(), displayOrigin);
        for (FormattedCharSequence line : this.font.split(orgDesc, textWidth)) {
            graphics.drawString(this.font, line, panelX + 5, y, 0xCCCCCC, false);
            y += 12;
        }

        y += 14;

        Player viewer = Minecraft.getInstance().player;
        List<ResourceLocation> shownPowers = viewer != null ? displayOrigin.powersFor(viewer) : displayOrigin.powers();
        for (ResourceLocation powerId : shownPowers) {
            Power power = ApoliPowers.get(powerId);
            if (power == null || power.hidden()) continue;

            MutableComponent underlined = power.displayName(powerId).copy().withStyle(ChatFormatting.UNDERLINE);
            FormattedCharSequence name = Language.getInstance().getVisualOrder(this.font.substrByWidth(underlined, textWidth));
            graphics.drawString(this.font, name, panelX + 5, y, 0xFFFFFF, false);

            int tw = this.font.width(name);
            int badgeX = panelX + 5 + tw + 4;
            int bi = 0;
            for (Badge badge : BadgeClientState.get(powerId)) {
                int bx = badgeX + 10 * bi;
                this.renderedBadges.add(new RenderedBadge(powerId, badge, bx, y - 1));
                int spriteSize = BadgeClientState.spriteSize(badge.spriteId());
                graphics.blit(badge.spriteId(), bx, y - 1, 9, 9, 0.0F, 0.0F, spriteSize, spriteSize, spriteSize, spriteSize);
                bi++;
            }

            for (FormattedCharSequence line : this.font.split(power.displayDescription(powerId), textWidth)) {
                y += 12;
                graphics.drawString(this.font, line, panelX + 5 + 2, y, 0xCCCCCC, false);
            }
            y += 14;
        }

        graphics.disableScissor();

        this.rightPanelContentMaxScroll = Math.max(0, y + effScroll - (panelY + panelHeight));
        int virtualMax = this.rightPanelContentMaxScroll <= 0
                ? 0
                : o + this.rightPanelContentMaxScroll + o;
        if (virtualMax <= 0) {
            this.rightPanelScrollPos = 0;
        } else {
            this.rightPanelScrollPos = Mth.clamp(this.rightPanelScrollPos, 0, virtualMax);
        }

        renderRightPanelScrollbar(graphics, mouseX, mouseY);

        for (RenderedBadge rb : this.renderedBadges) {
            if (mouseX >= rb.x && mouseX < rb.x + 9 && mouseY >= rb.y && mouseY < rb.y + 9 && rb.badge.hasTooltip()) {
                renderBadgeTooltip(graphics, rb, mouseX, mouseY);
            }
        }

        if (this.pendingAllowedSpellsTooltip != null) {
            drawAllowedSpellsTooltip(graphics, this.pendingAllowedSpellsTooltip, mouseX, mouseY);
            this.pendingAllowedSpellsTooltip = null;
        }
    }

    /**
     * Origins' own {@code Badge#renderTooltip} places tooltips to the right of the cursor, which
     * collapses against the screen edge these badges sit on; text badges are re-rendered here with
     * {@link #BADGE_TOOLTIP_POSITIONER}. Other badge types fall back to their own renderer.
     */
    private void renderBadgeTooltip(GuiGraphics graphics, RenderedBadge rb, int mouseX, int mouseY) {
        int widthLimit = Math.max(64, mouseX - 24);
        Component text = badgeTooltipText(rb.badge);
        if (text == null) {
            rb.badge.renderTooltip(graphics, this.font, mouseX, mouseY, widthLimit, rb.powerId, this.time);
            return;
        }
        graphics.renderTooltip(this.font, this.font.split(text, widthLimit),
                BADGE_TOOLTIP_POSITIONER, mouseX, mouseY);
    }

    @Nullable
    private static Component badgeTooltipText(Badge badge) {
        if (badge instanceof TooltipBadge tooltip) {
            return tooltip.text();
        }
        if (badge instanceof KeybindBadge keybind) {
            var mapping = ApoliKeyMappings.resolve(keybind.keyId());
            Component keyName = mapping != null
                    ? mapping.getTranslatedKeyMessage()
                    : Component.translatable("origins.gui.badge.unbound");
            return Component.translatable(keybind.text(),
                    Component.literal("[").append(keyName).append("]"));
        }
        return null;
    }

    private int allowedSpellsTooltipMaxScroll = 0;

    private void drawAllowedSpellsTooltip(GuiGraphics graphics, List<Component> lines, int mouseX, int mouseY) {
        int lineHeight = 10;
        int padding = 4;
        int margin = 8;

        int maxLineWidth = Math.max(80, this.width / 2 - margin * 2 - padding * 2 - 5);
        int contentW = 0;
        for (Component line : lines) {
            contentW = Math.max(contentW, Math.min(this.font.width(line), maxLineWidth));
        }

        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(this.font.split(line, contentW));
        }
        int contentH = wrapped.size() * lineHeight;

        int maxBoxH = this.height - margin * 2;
        int boxH = Math.min(contentH + padding * 2, maxBoxH);
        int boxW = contentW + padding * 2;

        int viewportH = boxH - padding * 2;
        int maxScroll = Math.max(0, contentH - viewportH);
        this.allowedSpellsTooltipMaxScroll = maxScroll;
        if (maxScroll > 0) {
            boxW += 5;
        }
        this.allowedSpellsTooltipScroll = Mth.clamp(this.allowedSpellsTooltipScroll, 0, maxScroll);

        int x = mouseX + 12;
        int y = mouseY - 12;
        if (x + boxW > this.width - margin) x = mouseX - 12 - boxW;
        if (x < margin) x = margin;
        if (y + boxH > this.height - margin) y = this.height - margin - boxH;
        if (y < margin) y = margin;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        int bg = 0xF0100010;
        int border = 0xFF5000FF;
        graphics.fill(x, y, x + boxW, y + boxH, bg);
        graphics.fill(x - 1, y, x, y + boxH, border);
        graphics.fill(x + boxW, y, x + boxW + 1, y + boxH, border);
        graphics.fill(x, y - 1, x + boxW, y, border);
        graphics.fill(x, y + boxH, x + boxW, y + boxH + 1, border);

        graphics.enableScissor(x + padding, y + padding, x + padding + contentW, y + padding + viewportH);
        int cy = y + padding - this.allowedSpellsTooltipScroll;
        for (FormattedCharSequence line : wrapped) {
            graphics.drawString(this.font, line, x + padding, cy, 0xFFFFFFFF, false);
            cy += lineHeight;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackX = x + boxW - 5;
            int trackY = y + padding;
            int trackH = viewportH;
            graphics.fill(trackX, trackY, trackX + 3, trackY + trackH, 0x44FFFFFF);
            int thumbH = Math.max(8, (int) ((float) viewportH / contentH * trackH));
            int thumbY = trackY + (int) ((float) this.allowedSpellsTooltipScroll / maxScroll * (trackH - thumbH));
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xCCFFFFFF);
        }

        graphics.pose().popPose();
    }

    private void renderRightPanelScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.rightPanelContentMaxScroll <= 0) {
            return;
        }
        int panelX = this.width - RIGHT_PANEL_WIDTH - 10;
        int panelY = 20;
        int panelHeight = this.height - 60;
        int contentTop = panelY + RIGHT_PANEL_CONTENT_TOP_OFFSET;
        int viewportH = panelHeight - RIGHT_PANEL_CONTENT_TOP_OFFSET;
        if (viewportH <= RIGHT_PANEL_SCROLLBAR_THUMB_H) {
            return;
        }
        int trackX = panelX + RIGHT_PANEL_WIDTH - RIGHT_PANEL_SCROLLBAR_TRACK_W;
        graphics.fill(
                trackX,
                contentTop,
                trackX + RIGHT_PANEL_SCROLLBAR_TRACK_W,
                contentTop + viewportH,
                RIGHT_PANEL_SCROLLBAR_TRACK_ARGB);

        int thumbX = trackX + (RIGHT_PANEL_SCROLLBAR_TRACK_W - RIGHT_PANEL_SCROLLBAR_THUMB_W) / 2;
        int thumbY = computeRightPanelScrollbarThumbY(contentTop, viewportH);
        boolean hover = !this.rightPanelScrollbarDragging
                && mouseX >= thumbX
                && mouseX < thumbX + RIGHT_PANEL_SCROLLBAR_THUMB_W
                && mouseY >= thumbY
                && mouseY < thumbY + RIGHT_PANEL_SCROLLBAR_THUMB_H;
        int thumbArgb = (this.rightPanelScrollbarDragging || hover)
                ? RIGHT_PANEL_SCROLLBAR_THUMB_ACTIVE_ARGB
                : RIGHT_PANEL_SCROLLBAR_THUMB_ARGB;
        graphics.fill(
                thumbX,
                thumbY,
                thumbX + RIGHT_PANEL_SCROLLBAR_THUMB_W,
                thumbY + RIGHT_PANEL_SCROLLBAR_THUMB_H,
                thumbArgb);
    }

    private int computeRightPanelScrollbarThumbY(int contentTop, int viewportH) {
        int o = RIGHT_PANEL_SCROLL_OVERSCROLL_PX;
        int eff = Mth.clamp(this.rightPanelScrollPos - o, 0, this.rightPanelContentMaxScroll);
        float part = this.rightPanelContentMaxScroll <= 0 ? 0.0f : eff / (float) this.rightPanelContentMaxScroll;
        int thumbTravel = viewportH - RIGHT_PANEL_SCROLLBAR_THUMB_H;
        return contentTop + (int) (thumbTravel * part + 0.5f);
    }

    private boolean tryBeginRightPanelScrollbarDrag(double mouseX, double mouseY) {
        if (getRightPanelDisplayOrigin() == null || this.rightPanelContentMaxScroll <= 0) {
            return false;
        }
        int panelX = this.width - RIGHT_PANEL_WIDTH - 10;
        int panelY = 20;
        int panelHeight = this.height - 60;
        int contentTop = panelY + RIGHT_PANEL_CONTENT_TOP_OFFSET;
        int viewportH = panelHeight - RIGHT_PANEL_CONTENT_TOP_OFFSET;
        if (viewportH <= RIGHT_PANEL_SCROLLBAR_THUMB_H) {
            return false;
        }
        int trackX = panelX + RIGHT_PANEL_WIDTH - RIGHT_PANEL_SCROLLBAR_TRACK_W;
        int thumbX = trackX + (RIGHT_PANEL_SCROLLBAR_TRACK_W - RIGHT_PANEL_SCROLLBAR_THUMB_W) / 2;
        int thumbY = computeRightPanelScrollbarThumbY(contentTop, viewportH);
        if (mouseX >= thumbX
                && mouseX < thumbX + RIGHT_PANEL_SCROLLBAR_THUMB_W
                && mouseY >= thumbY
                && mouseY < thumbY + RIGHT_PANEL_SCROLLBAR_THUMB_H) {
            this.rightPanelScrollbarDragging = true;
            this.rightPanelScrollbarDragStartMouseY = mouseY;
            int o = RIGHT_PANEL_SCROLL_OVERSCROLL_PX;
            this.rightPanelScrollbarDragStartEffScroll =
                    Mth.clamp(this.rightPanelScrollPos - o, 0, this.rightPanelContentMaxScroll);
            return true;
        }
        return false;
    }

    private void applyRightPanelScrollbarDrag(double mouseY) {
        int panelHeight = this.height - 60;
        int viewportH = panelHeight - RIGHT_PANEL_CONTENT_TOP_OFFSET;
        int thumbTravel = viewportH - RIGHT_PANEL_SCROLLBAR_THUMB_H;
        if (thumbTravel <= 0 || this.rightPanelContentMaxScroll <= 0) {
            return;
        }
        double deltaY = mouseY - this.rightPanelScrollbarDragStartMouseY;
        int deltaEff = (int) Math.round(deltaY / thumbTravel * this.rightPanelContentMaxScroll);
        int newEff = Mth.clamp(this.rightPanelScrollbarDragStartEffScroll + deltaEff, 0, this.rightPanelContentMaxScroll);
        int o = RIGHT_PANEL_SCROLL_OVERSCROLL_PX;
        this.rightPanelScrollPos = newEff + o;
    }

    private final LinkedList<RenderedBadge> renderedBadges = new LinkedList<>();

    private record RenderedBadge(ResourceLocation powerId, Badge badge, int x, int y) {}

    private void renderIcon(GuiGraphics graphics, Origin origin, int x, int y) {
        String originPath = origin.id().getPath();
        String spellName = null;
        if (originPath.startsWith("cantrips/two/")) {
            spellName = originPath.substring("cantrips/two/".length());
        } else if (originPath.startsWith("cantrips/magical_secrets/")) {
            spellName = originPath.substring("cantrips/magical_secrets/".length());
        } else if (originPath.startsWith("cantrips/")) {
            spellName = originPath.substring("cantrips/".length());
        }

        if (spellName != null) {
            String namespace = resolveSpellNamespace(spellName);
            ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/spell_icons/" + spellName + ".png");
            graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }

        Optional<ResourceLocation> disciplineSpell = ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath);
        if (disciplineSpell.isPresent()) {
            ResourceLocation id = disciplineSpell.get();
            ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(
                    id.getNamespace(), "textures/gui/spell_icons/" + id.getPath() + ".png");
            graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }

        IconRenderer.render(graphics, origin.icon(), x, y);
    }

    private MutableComponent appendExtraInfo(Component orgDesc, Origin currentOrigin) {
        String originPath = currentOrigin.id().getPath();
        MutableComponent modifiedDesc = orgDesc.copy();

        if (originPath.startsWith("class/")) {
            modifiedDesc = appendEnchantmentAccess(modifiedDesc, originPath.substring("class/".length()));
        } else if (originPath.startsWith("cantrips/two/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/two/".length()));
        } else if (originPath.startsWith("cantrips/magical_secrets/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/magical_secrets/".length()));
        } else if (originPath.startsWith("cantrips/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/".length()));
        } else {
            Optional<ResourceLocation> disciplineSpell = ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath);
            if (disciplineSpell.isPresent()) {
                modifiedDesc = ElementalDisciplineSpellDisplay.appendSpellGuide(modifiedDesc, disciplineSpell.get());
            }
        }

        return modifiedDesc;
    }

    private MutableComponent appendEnchantmentAccess(MutableComponent desc, String className) {
        if (!RavenDndOriginsConfig.enableEnchantmentRestrictions()) return desc;

        List<Component> classEnchantments = EnchantmentRestrictions.getEnchantmentTextForClass(className);
        if (classEnchantments.isEmpty()) return desc;

        desc.append("\n\n").append(Component.translatable("raven_dnd_origins.gui.enchantment_access").withStyle(style -> style.withUnderlined(true).withColor(16738047)));

        String formattedClass = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase() + "s";
        for (Component enchantmentName : classEnchantments) {
            Component fullMessage = Component.translatable("raven_dnd_origins.gui.enchantment_restriction", formattedClass, enchantmentName).withStyle(style -> style.withColor(16738047));
            desc.append("\n").append(Component.literal("• ")).append(fullMessage);
        }

        return desc;
    }

    /** Spell icons and guide text are keyed by path only, so the namespace is recovered from the ISS registry. */
    private static String resolveSpellNamespace(String spellName) {
        for (AbstractSpell spell : SpellRegistry.REGISTRY) {
            if (spell.getSpellResource().getPath().equals(spellName)) {
                return spell.getSpellResource().getNamespace();
            }
        }
        return IronsSpellbooks.MODID;
    }

    private MutableComponent appendCantripDesc(MutableComponent desc, String spellName) {
        String namespace = resolveSpellNamespace(spellName);
        ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(namespace, spellName);
        Component spellDesc = Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide").withStyle(style -> style.withItalic(true));
        desc.append("\n\n").append(spellDesc);
        return desc;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (tryBeginRightPanelScrollbarDrag(mouseX, mouseY)) {
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
        if (this.allowedSpellsStripActive
                && mouseX >= this.allowedSpellsStripX
                && mouseX < this.allowedSpellsStripX + this.allowedSpellsStripW
                && mouseY >= this.allowedSpellsStripY
                && mouseY < this.allowedSpellsStripY + this.allowedSpellsStripH
                && this.allowedSpellsTooltipMaxScroll > 0) {
            int ns = this.allowedSpellsTooltipScroll - (int) scrollY * 10;
            this.allowedSpellsTooltipScroll = Mth.clamp(ns, 0, this.allowedSpellsTooltipMaxScroll);
            return true;
        }
        boolean rightOpen = getRightPanelDisplayOrigin() != null;
        boolean rightConsumed = false;
        int o = RIGHT_PANEL_SCROLL_OVERSCROLL_PX;
        int contentMax = this.rightPanelContentMaxScroll;
        int virtualMax = contentMax <= 0 ? 0 : o + contentMax + o;

        if (rightOpen && virtualMax > 0) {
            boolean canScrollRight = scrollY > 0
                    ? this.rightPanelScrollPos > 0
                    : scrollY < 0 && this.rightPanelScrollPos < virtualMax;
            if (canScrollRight) {
                int np = this.rightPanelScrollPos - (int) scrollY * 12;
                this.rightPanelScrollPos = Mth.clamp(np, 0, virtualMax);
                rightConsumed = true;
            }
        }

        if (!rightConsumed && this.activeLayerOptionsMaxScrollY > 0) {
            boolean canScrollActive = scrollY > 0
                    ? this.activeLayerOptionsScrollY > 0
                    : scrollY < 0 && this.activeLayerOptionsScrollY < this.activeLayerOptionsMaxScrollY;
            if (canScrollActive) {
                int ny = this.activeLayerOptionsScrollY - (int) scrollY * 12;
                this.activeLayerOptionsScrollY = ny < 0 ? 0 : Math.min(ny, this.activeLayerOptionsMaxScrollY);
                return true;
            }
        }

        if (rightConsumed) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && this.rightPanelScrollbarDragging) {
            applyRightPanelScrollbarDrag(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.rightPanelScrollbarDragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
