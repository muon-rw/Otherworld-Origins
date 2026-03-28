package dev.muon.otherworldorigins.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.C2SRevertLayerOriginsMessage;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import dev.muon.otherworldorigins.network.RequestLayerValidationMessage;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.C2SChooseOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import dev.muon.otherworldorigins.config.OtherworldOriginsConfig;
import dev.muon.otherworldorigins.restrictions.EnchantmentRestrictions;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.apace100.origins.badge.Badge;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.mixin.DrawContextAccessor;
import io.github.apace100.origins.origin.Impact;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import dev.muon.otherworldorigins.client.shapeshift.AnacondaMultipartHandler;
import dev.muon.otherworldorigins.client.shapeshift.FakeEntityCache;
import dev.muon.otherworldorigins.client.shapeshift.ShapeshiftRenderHelper;
import dev.muon.otherworldorigins.power.ShapeshiftPower;
import com.github.alexthe666.alexsmobs.entity.EntityAnaconda;
import com.github.alexthe666.alexsmobs.entity.EntityAnacondaPart;
import net.minecraft.client.multiplayer.ClientLevel;
import org.joml.Quaternionf;
import javax.annotation.Nullable;

public class OtherworldOriginScreen extends Screen {

    private final List<Holder<OriginLayer>> layerList;
    private int currentLayerIndex;
    private final boolean showDirtBackground;

    private final Map<Integer, Holder<Origin>> confirmedSelections = new HashMap<>();
    private final Map<Integer, List<Holder<Origin>>> layerOriginCache = new HashMap<>();

    private Holder<Origin> hoveredOrigin = null;
    private Holder<Origin> selectedOrigin = null;

    private float[] cardExpandProgress = new float[0];
    private int rightPanelScrollPos = 0;
    private int rightPanelMaxScroll = 0;
    private float time = 0.0f;

    private List<FormattedCharSequence> sheetLines = new ArrayList<>();
    private boolean dynamicPromptMode = false;

    private static final int LEFT_PANEL_WIDTH = 160;
    private static final int RIGHT_PANEL_WIDTH = 160;
    private static final int CARD_COLLAPSED_WIDTH = 16;
    private static final int CARD_EXPANDED_WIDTH = 32;
    private static final int CARD_HEIGHT = 32;
    private static final int ICON_SIZE = 16;
    private static final int COMPLETED_PORTRAIT_HEIGHT = 36;
    private static final int COMPLETED_ICON_HEIGHT = 22;
    private static final ResourceLocation CHARACTER_SHEET = OtherworldOrigins.loc("textures/gui/character_sheet.png");
    private static final ResourceLocation WINDOW = ResourceLocation.fromNamespaceAndPath("origins", "textures/gui/choose_origin.png");

    private Button selectButton;

    public OtherworldOriginScreen(List<Holder<OriginLayer>> layerList, int startLayerIndex, boolean showDirtBackground) {
        super(Component.translatable("origins.screen.choose_origin"));
        this.layerList = layerList;
        this.currentLayerIndex = startLayerIndex;
        this.showDirtBackground = showDirtBackground;
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

        OtherworldOrigins.LOGGER.info("[OWOriginScreen] init: layerList has {} layers", this.layerList.size());
        for (int i = 0; i < this.layerList.size(); i++) {
            Holder<OriginLayer> l = this.layerList.get(i);
            ResourceLocation lid = l.unwrapKey().map(ResourceKey::location).orElse(null);
            OtherworldOrigins.LOGGER.info("[OWOriginScreen]   layer[{}] = {}", i, lid);
        }
        evaluateCurrentLayer();
        computeDynamicPromptMode();
        OtherworldOrigins.LOGGER.info("[OWOriginScreen] after eval: currentLayerIndex={}, dynamicPromptMode={}, confirmedSelections={}",
                this.currentLayerIndex, this.dynamicPromptMode, this.confirmedSelections.size());
    }

    private void computeDynamicPromptMode() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            this.dynamicPromptMode = false;
            return;
        }

        boolean hasUserSelectableLayer = false;
        for (int i = this.currentLayerIndex; i < this.layerList.size(); i++) {
            Holder<OriginLayer> layer = this.layerList.get(i);

            boolean hasChoosableOrigins = layer.value().origins(player).stream()
                    .anyMatch(o -> o.isBound() && o.value().isChoosable());
            if (!hasChoosableOrigins) continue;

            hasUserSelectableLayer = true;
            ResourceLocation id = layer.unwrapKey().map(ResourceKey::location).orElse(null);
            if (id == null || !DYNAMIC_LAYER_IDS.contains(id)) {
                OtherworldOrigins.LOGGER.info("[OWOriginScreen] computeDynamic: non-dynamic selectable layer[{}] = {}", i, id);
                this.dynamicPromptMode = false;
                return;
            }
        }
        this.dynamicPromptMode = hasUserSelectableLayer;
    }

    private void evaluateCurrentLayer() {
        if (this.currentLayerIndex >= this.layerList.size()) {
            finishSelection();
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Holder<OriginLayer> currentLayer = this.layerList.get(this.currentLayerIndex);

        boolean[] autoPopulated = {false};
        IOriginContainer.get(player).ifPresent(container -> {
            if (container.hasOrigin(currentLayer)) {
                ResourceKey<Origin> existingKey = container.getOrigin(currentLayer);
                if (existingKey != null) {
                    Registry<Origin> originRegistry = OriginsAPI.getOriginsRegistry(null);
                    originRegistry.getHolder(existingKey).ifPresent(existingHolder -> {
                        this.confirmedSelections.put(this.currentLayerIndex, existingHolder);
                        autoPopulated[0] = true;
                    });
                }
            }
        });

        if (autoPopulated[0]) {
            this.currentLayerIndex++;
            evaluateCurrentLayer();
            return;
        }
        
        List<Holder<Origin>> availableOrigins = currentLayer.value().origins(player).stream()
            .filter(originHolder -> originHolder.isBound() && originHolder.value().isChoosable())
            .sorted(Comparator.comparingInt(a -> a.value().getImpact().getImpactValue()))
            .collect(Collectors.toList());

        if (availableOrigins.isEmpty()) {
            this.currentLayerIndex++;
            evaluateCurrentLayer();
            return;
        }

        this.layerOriginCache.put(this.currentLayerIndex, availableOrigins);
        this.cardExpandProgress = new float[availableOrigins.size()];
        this.selectedOrigin = null;
        this.hoveredOrigin = null;
        this.rightPanelScrollPos = 0;
        
        updateButtonStates();
        rebuildCharacterSheetText();
    }

    private void confirmSelection() {
        if (this.selectedOrigin == null || this.currentLayerIndex >= this.layerList.size()) return;

        Holder<OriginLayer> currentLayer = this.layerList.get(this.currentLayerIndex);
        ResourceLocation layerId = currentLayer.unwrapKey().map(ResourceKey::location).orElse(null);
        ResourceLocation originId = this.selectedOrigin.unwrapKey().map(ResourceKey::location).orElse(null);

        if (layerId != null && originId != null) {
            OriginsCommon.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SChooseOrigin(layerId, originId));
            
            // Optimistic local update: use (Holder, Holder) overload
            IOriginContainer.get(Minecraft.getInstance().player).ifPresent(container -> {
                container.setOrigin(currentLayer, this.selectedOrigin);
            });

            this.confirmedSelections.put(this.currentLayerIndex, this.selectedOrigin);
            
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));
            
            this.currentLayerIndex++;
            evaluateCurrentLayer();
        }
    }

    private void revertToLayer(int index) {
        if (index >= this.currentLayerIndex) return;

        List<ResourceLocation> layersToRevert = new ArrayList<>();
        for (int i = index; i < this.layerList.size(); i++) {
            this.confirmedSelections.remove(i);
            this.layerOriginCache.remove(i);
            
            Holder<OriginLayer> layer = this.layerList.get(i);
            ResourceLocation layerId = layer.unwrapKey().map(ResourceKey::location).orElse(null);
            if (layerId != null) {
                layersToRevert.add(layerId);
            }
        }

        OtherworldOrigins.CHANNEL.sendToServer(new C2SRevertLayerOriginsMessage(layersToRevert));

        // Optimistic local revert
        IOriginContainer.get(Minecraft.getInstance().player).ifPresent(container -> {
            for (ResourceLocation layerId : layersToRevert) {
                ResourceKey<OriginLayer> key = ResourceKey.create(OriginsAPI.getLayersRegistry(null).key(), layerId);
                Holder<OriginLayer> layerHolder = OriginsAPI.getLayersRegistry(null).getHolder(key).orElse(null);
                if (layerHolder != null) {
                    container.setOrigin(layerHolder.value(), Origin.EMPTY);
                }
            }
        });

        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_WORK_CARTOGRAPHER, 1.0F));

        this.currentLayerIndex = index;
        evaluateCurrentLayer();
    }

    private void finishSelection() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F));

        if (this.dynamicPromptMode) {
            Minecraft.getInstance().setScreen(null);
            return;
        }

        Set<ResourceLocation> selectedLayerIds = new HashSet<>();
        for (Holder<OriginLayer> layerHolder : this.layerList) {
            layerHolder.unwrapKey().ifPresent(key -> selectedLayerIds.add(key.location()));
        }
        ClientLayerScreenHelper.addToSelectedLayers(selectedLayerIds);

        Minecraft.getInstance().setScreen(null);
        OtherworldOrigins.CHANNEL.sendToServer(new RequestLayerValidationMessage());
    }

    private void updateButtonStates() {
        this.selectButton.active = this.selectedOrigin != null;
    }
    private boolean isPortraitLayer(Holder<OriginLayer> layer) {
        ResourceLocation id = layer.unwrapKey().map(ResourceKey::location).orElse(null);
        return id != null && (id.equals(OtherworldOrigins.loc("race")) || id.equals(OtherworldOrigins.loc("subrace")));
    }

    private static final Set<ResourceLocation> DYNAMIC_LAYER_IDS = Set.of(
        OtherworldOrigins.loc("first_feat"), OtherworldOrigins.loc("second_feat"),
        OtherworldOrigins.loc("third_feat"), OtherworldOrigins.loc("fourth_feat"),
        OtherworldOrigins.loc("fifth_feat"), OtherworldOrigins.loc("free_feat"),
        OtherworldOrigins.loc("plus_one_aptitude_resilient"), OtherworldOrigins.loc("wildshape")
    );


    private int getEffectiveCardCollapsedWidth() {
        int paperLeft = this.width / 2 - 128;
        int availableWidth = paperLeft - 10 - 4;
        int optionCount = 0;
        if (this.currentLayerIndex < this.layerList.size()) {
            List<Holder<Origin>> options = this.layerOriginCache.get(this.currentLayerIndex);
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

    @Override
    public void tick() {
        super.tick();
        updateAnimations();

        if (this.dynamicPromptMode) {
            Holder<Origin> displayOrigin = this.hoveredOrigin != null ? this.hoveredOrigin : this.selectedOrigin;
            if (displayOrigin != null && displayOrigin.isBound()) {
                ResourceLocation entityTypeId = getShapeshiftEntityType(displayOrigin);
                if (entityTypeId != null && AnacondaMultipartHandler.isAnaconda(entityTypeId)) {
                    Entity previewEntity = FakeEntityCache.getOrCreate(PREVIEW_ENTITY_CACHE_ID, entityTypeId);
                    if (previewEntity instanceof LivingEntity living) {
                        Player player = Minecraft.getInstance().player;
                        double startX = player != null ? player.getX() : 0;
                        double startY = player != null ? player.getY() : 0;
                        double startZ = player != null ? player.getZ() : 0;

                        if (living.tickCount == 0) {
                            living.setPos(startX, startY, startZ);
                        }

                        living.xo = living.getX();
                        living.yo = living.getY();
                        living.zo = living.getZ();
                        living.yBodyRotO = living.yBodyRot;
                        living.yRotO = living.getYRot();
                        living.yHeadRotO = living.yHeadRot;

                        living.tickCount++;
                        living.walkDist += 0.08f;
                        
                        float simAngleDeg = living.tickCount * 1.5f; 
                        float yawRad = (float) Math.toRadians(180 + simAngleDeg);
                        
                        double speed = 0.08;
                        double nx = living.getX() - Math.sin(yawRad) * speed;
                        double nz = living.getZ() + Math.cos(yawRad) * speed;
                        living.setPos(nx, startY, nz);
                        
                        float newYaw = 180 + simAngleDeg;
                        living.setYRot(newYaw);
                        living.yBodyRot = newYaw;
                        living.yHeadRot = newYaw;
                        
                        AnacondaMultipartHandler.tickAndPosition(PREVIEW_ENTITY_CACHE_ID, living, (com.github.alexthe666.alexsmobs.entity.EntityAnaconda) living, true);
                    }
                }
            }
        }
    }

    private void updateAnimations() {
        if (this.currentLayerIndex >= this.layerList.size()) return;
        List<Holder<Origin>> options = this.layerOriginCache.get(this.currentLayerIndex);
        if (options == null || !isPortraitLayer(this.layerList.get(this.currentLayerIndex))) return;

        for (int i = 0; i < this.cardExpandProgress.length; i++) {
            boolean isHovered = (this.hoveredOrigin != null && this.hoveredOrigin.equals(options.get(i)));
            float target = isHovered ? 1.0f : 0.0f;
            this.cardExpandProgress[i] = net.minecraft.util.Mth.lerp(0.3f, this.cardExpandProgress[i], target);
        }
    }

    private void rebuildCharacterSheetText() {
        this.sheetLines.clear();
        
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        
        Component playerName = player.getName().copy().withStyle(net.minecraft.ChatFormatting.ITALIC);
        
        String race = getOriginDisplayName(OtherworldOrigins.loc("race"));
        String subrace = getOriginDisplayName(OtherworldOrigins.loc("subrace"));
        String className = getOriginDisplayName(OtherworldOrigins.loc("class"));
        String subclassName = getOriginDisplayName(OtherworldOrigins.loc("subclass"));

        if (subrace != null && race != null && subrace.endsWith(" " + race)) {
            subrace = subrace.substring(0, subrace.length() - race.length() - 1);
        }

        if (race == null) {
            addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.main_description_name_only", playerName));
            return;
        }
        
        Component mainText;
        if (className == null) {
            if (race.equals("Other") || race.equals("Undead")) {
                mainText = Component.translatable("otherworldorigins.gui.final_confirm.main_description_race_only_no_race", 
                        playerName, subrace != null ? subrace : race);
            } else {
                mainText = Component.translatable("otherworldorigins.gui.final_confirm.main_description_race_only", 
                        playerName, subrace != null ? subrace : "", race);
            }
        } else {
            boolean hasSubclass = subclassName != null && !subclassName.isEmpty();
            String articleTarget = hasSubclass ? subclassName : className;
            boolean useAn = false;
            if (articleTarget != null && !articleTarget.isEmpty()) {
                char firstChar = Character.toLowerCase(articleTarget.charAt(0));
                useAn = (firstChar == 'a' || firstChar == 'e' || firstChar == 'i' || 
                        firstChar == 'o' || firstChar == 'u');
            }
            
            String prefix = "otherworldorigins.gui.final_confirm.";
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
                    mainText = Component.translatable(key, playerName, subclassName, className, subrace != null ? subrace : "", race);
                } else {
                    String key = prefix + (useAn ? "main_description_no_subclass_an" : "main_description_no_subclass");
                    mainText = Component.translatable(key, playerName, className, subrace != null ? subrace : "", race);
                }
            }
        }
        addSheetText(mainText);
        
        List<String> feats = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                OtherworldOrigins.loc("free_feat"), OtherworldOrigins.loc("first_feat"),
                OtherworldOrigins.loc("second_feat"), OtherworldOrigins.loc("third_feat"),
                OtherworldOrigins.loc("fourth_feat"), OtherworldOrigins.loc("fifth_feat")
        };
        for (ResourceLocation layerId : featLayerIds) {
            String featName = getOriginDisplayName(layerId);
            if (featName != null) feats.add(featName);
        }
        if (!feats.isEmpty()) {
            this.sheetLines.add(FormattedCharSequence.EMPTY);
            addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.feats_header").withStyle(style -> style.withUnderlined(true)));
            if (feats.size() == 1) {
                addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.feats_single", feats.get(0)));
            } else if (feats.size() == 2) {
                addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.feats_double", feats.get(0), feats.get(1)));
            } else {
                String allButLast = String.join(", ", feats.subList(0, feats.size() - 1));
                addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.feats_multiple", allButLast, feats.get(feats.size() - 1)));
            }
        }
        
        List<String> cantrips = new ArrayList<>();
        String cantrip1 = getOriginDisplayName(OtherworldOrigins.loc("cantrip_one"));
        String cantrip2 = getOriginDisplayName(OtherworldOrigins.loc("cantrip_two"));
        if (cantrip1 != null) cantrips.add(cantrip1);
        if (cantrip2 != null) cantrips.add(cantrip2);
        if (!cantrips.isEmpty()) {
            this.sheetLines.add(FormattedCharSequence.EMPTY);
            addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.cantrips_header").withStyle(style -> style.withUnderlined(true)));
            if (cantrips.size() == 1) {
                addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.cantrips_single", cantrips.get(0)));
            } else {
                addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.cantrips_double", cantrips.get(0), cantrips.get(1)));
            }
        }
        
        List<String> plusOnes = new ArrayList<>();
        List<String> plusTwos = new ArrayList<>();
        String plusOne1 = getOriginDisplayName(OtherworldOrigins.loc("plus_one_aptitude_one"));
        String plusOne2 = getOriginDisplayName(OtherworldOrigins.loc("plus_one_aptitude_two"));
        String plusOneResilient = getOriginDisplayName(OtherworldOrigins.loc("plus_one_aptitude_resilient"));
        String plusTwo1 = getOriginDisplayName(OtherworldOrigins.loc("plus_two_aptitude_one"));
        String plusTwo2 = getOriginDisplayName(OtherworldOrigins.loc("plus_two_aptitude_two"));
        
        if (plusOne1 != null) plusOnes.add(plusOne1);
        if (plusOne2 != null) plusOnes.add(plusOne2);
        if (plusOneResilient != null) plusOnes.add(plusOneResilient);
        if (plusTwo1 != null) plusTwos.add(plusTwo1);
        if (plusTwo2 != null) plusTwos.add(plusTwo2);
        
        if (!plusOnes.isEmpty() || !plusTwos.isEmpty()) {
            this.sheetLines.add(FormattedCharSequence.EMPTY);
            addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.aptitudes_header").withStyle(style -> style.withUnderlined(true)));
            if (!plusTwos.isEmpty()) {
                addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.aptitude_plus_two", String.join(", ", plusTwos)));
            }
            if (!plusOnes.isEmpty()) {
                addSheetText(Component.translatable("otherworldorigins.gui.final_confirm.aptitude_plus_one", String.join(", ", plusOnes)));
            }
        }
    }

    private void addSheetText(Component text) {
        this.sheetLines.addAll(this.font.split(text, 190));
    }

    private String getOriginDisplayName(ResourceLocation layerId) {
        for (Map.Entry<Integer, Holder<Origin>> entry : this.confirmedSelections.entrySet()) {
            int idx = entry.getKey();
            if (idx >= this.layerList.size()) continue;
            Holder<OriginLayer> layer = this.layerList.get(idx);
            ResourceLocation lId = layer.unwrapKey().map(ResourceKey::location).orElse(null);
            if (lId != null && lId.equals(layerId)) {
                Holder<Origin> origin = entry.getValue();
                if (origin != null && origin.isBound() && !origin.unwrapKey().map(ResourceKey::location).orElse(null).equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"))) {
                    return origin.value().getName().getString();
                }
            }
        }
        return null;
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics) {
        if (this.showDirtBackground) {
            super.renderDirtBackground(graphics);
        } else {
            super.renderBackground(graphics);
        }
    }

    private Component getTitleText() {
        if (this.currentLayerIndex >= 0 && this.currentLayerIndex < this.layerList.size()) {
            Holder<OriginLayer> currentLayer = this.layerList.get(this.currentLayerIndex);
            ResourceLocation layerId = currentLayer.unwrapKey().map(ResourceKey::location).orElse(null);

            if (layerId != null) {
                if (layerId.equals(OtherworldOrigins.loc("wildshape"))) {
                    return Component.translatable("otherworldorigins.gui.wildshape_choose_title");
                }
                
                String sourceName = null;
                if (layerId.equals(OtherworldOrigins.loc("cantrip_one"))) {
                    sourceName = getOriginDisplayName(OtherworldOrigins.loc("subrace"));
                } else if (layerId.equals(OtherworldOrigins.loc("cantrip_two"))) {
                    sourceName = getCantripTwoSourceName();
                }
                
                if (sourceName != null) {
                    return Component.translatable("otherworldorigins.gui.cantrip_choose_title", sourceName);
                }
            }
            Component titleText = currentLayer.value().title().choose();
            if (titleText != null) return titleText;
            return Component.translatable("origins.gui.choose_origin.title", currentLayer.value().name());
        }
        return this.title;
    }

    private String getCantripTwoSourceName() {
        Holder<Origin> subclassOrigin = null;
        for (Map.Entry<Integer, Holder<Origin>> entry : this.confirmedSelections.entrySet()) {
            Holder<OriginLayer> layer = this.layerList.get(entry.getKey());
            ResourceLocation lId = layer.unwrapKey().map(ResourceKey::location).orElse(null);
            if (OtherworldOrigins.loc("subclass").equals(lId)) {
                subclassOrigin = entry.getValue();
                break;
            }
        }
        
        if (subclassOrigin != null) {
            ResourceLocation originId = subclassOrigin.unwrapKey().map(ResourceKey::location).orElse(null);
            if (originId != null && (originId.equals(OtherworldOrigins.loc("subclass/rogue/arcane_trickster")) || 
                                     originId.equals(OtherworldOrigins.loc("subclass/fighter/eldritch_knight")))) {
                return subclassOrigin.value().getName().getString();
            }
        }
        return getOriginDisplayName(OtherworldOrigins.loc("class"));
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.time += delta;
        this.renderBackground(graphics);
        
        renderCenterPanel(graphics, mouseX, mouseY);
        renderLeftPanel(graphics, mouseX, mouseY);
        renderRightPanel(graphics, mouseX, mouseY);

        boolean hasInfoPanel = this.hoveredOrigin != null || this.selectedOrigin != null;
        this.selectButton.visible = hasInfoPanel;
        this.selectButton.active = this.selectedOrigin != null;

        super.render(graphics, mouseX, mouseY, delta);
    }

    private Integer findConfirmedPairIndex(int index) {
        ResourceLocation layerId = this.layerList.get(index).unwrapKey().map(ResourceKey::location).orElse(null);
        if (layerId == null) return null;

        ResourceLocation pairedId = null;
        if (layerId.equals(OtherworldOrigins.loc("race"))) pairedId = OtherworldOrigins.loc("subrace");
        else if (layerId.equals(OtherworldOrigins.loc("class"))) pairedId = OtherworldOrigins.loc("subclass");
        else return null;

        for (int j = 0; j < this.layerList.size(); j++) {
            if (j == index || !this.confirmedSelections.containsKey(j)) continue;
            ResourceLocation jId = this.layerList.get(j).unwrapKey().map(ResourceKey::location).orElse(null);
            if (pairedId.equals(jId)) return j;
        }
        return null;
    }

    private void renderLeftPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = 10;
        int y = 10;

        Set<Integer> renderedAsPair = new HashSet<>();

        for (int i = 0; i <= this.currentLayerIndex; i++) {
            if (i >= this.layerList.size()) break;
            if (renderedAsPair.contains(i)) continue;

            Holder<OriginLayer> layer = this.layerList.get(i);
            boolean isActive = (i == this.currentLayerIndex);
            boolean isCompleted = (i < this.currentLayerIndex);

            if (isCompleted && !this.confirmedSelections.containsKey(i)) continue;

            if (isActive) {
                Component layerName = layer.value().name();
                graphics.drawString(this.font, layerName, x, y, 0xFFFFFF, true);
                y += 12;

                List<Holder<Origin>> options = this.layerOriginCache.get(i);
                if (options != null && !options.isEmpty()) {
                    if (isPortraitLayer(layer)) {
                        renderPortraitRow(graphics, options, x, y, mouseX, mouseY);
                        y += CARD_HEIGHT + 10;
                    } else {
                        renderIconGrid(graphics, options, x, y, mouseX, mouseY);
                        int iconsPerRow = Math.max(1, (Math.max(40, this.width / 2 - 128 - x - 4)) / 20);
                        int rows = (options.size() + iconsPerRow - 1) / iconsPerRow;
                        y += rows * 20 + 4;
                    }
                }
            } else {
                Integer pairIdx = findConfirmedPairIndex(i);
                if (pairIdx != null) {
                    renderedAsPair.add(pairIdx);
                    renderCompletedPairRow(graphics, i, pairIdx, x, y, mouseX, mouseY);
                } else {
                    renderCompactCompletedRow(graphics, layer, this.confirmedSelections.get(i), x, y, mouseX, mouseY, i);
                }
                y += isPortraitLayer(layer) ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
            }
        }
    }

    private void renderCompletedPairRow(GuiGraphics graphics, int leftIdx, int rightIdx, int x, int y, int mouseX, int mouseY) {
        Holder<OriginLayer> leftLayer = this.layerList.get(leftIdx);
        Holder<Origin> leftOrigin = this.confirmedSelections.get(leftIdx);
        Holder<OriginLayer> rightLayer = this.layerList.get(rightIdx);
        Holder<Origin> rightOrigin = this.confirmedSelections.get(rightIdx);
        boolean portrait = isPortraitLayer(leftLayer);
        int rowH = portrait ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
        int halfW = LEFT_PANEL_WIDTH / 2;
        int nameYOff = portrait ? 12 : 4;

        boolean leftHov = mouseX >= x && mouseX < x + halfW && mouseY >= y && mouseY < y + rowH;
        boolean rightHov = mouseX >= x + halfW && mouseX < x + LEFT_PANEL_WIDTH && mouseY >= y && mouseY < y + rowH;

        if (leftHov) graphics.fill(x - 2, y - 2, x + halfW, y + rowH, 0x22FFFFFF);
        if (rightHov) graphics.fill(x + halfW, y - 2, x + LEFT_PANEL_WIDTH + 2, y + rowH, 0x22FFFFFF);

        renderCompletedItem(graphics, leftLayer, leftOrigin, x, y, nameYOff, leftHov);

        int rx = x + halfW + 4;
        renderCompletedItem(graphics, rightLayer, rightOrigin, rx, y, nameYOff, rightHov);
    }

    private void renderCompactCompletedRow(GuiGraphics graphics, Holder<OriginLayer> layer, Holder<Origin> origin, int x, int y, int mouseX, int mouseY, int layerIndex) {
        boolean portrait = isPortraitLayer(layer);
        int rowH = portrait ? COMPLETED_PORTRAIT_HEIGHT : COMPLETED_ICON_HEIGHT;
        boolean isHovered = mouseX >= x && mouseX <= x + LEFT_PANEL_WIDTH && mouseY >= y && mouseY < y + rowH;

        if (isHovered) graphics.fill(x - 2, y - 2, x + LEFT_PANEL_WIDTH, y + rowH, 0x22FFFFFF);

        int nameYOff = portrait ? 12 : 4;
        renderCompletedItem(graphics, layer, origin, x, y, nameYOff, isHovered);
    }

    private void renderCompletedItem(GuiGraphics graphics, Holder<OriginLayer> layer, Holder<Origin> origin, int x, int y, int nameYOff, boolean bright) {
        if (isPortraitLayer(layer)) {
            ResourceLocation texture = getPortraitTexture(origin.value().getIcon());
            if (!bright) RenderSystem.setShaderColor(0.6f, 0.6f, 0.6f, 1.0f);
            graphics.blit(texture, x, y, 0, 0, CARD_COLLAPSED_WIDTH, CARD_HEIGHT, 32, 32);
            if (!bright) RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            renderIcon(graphics, origin, x, y);
        }
        graphics.drawString(this.font, origin.value().getName(), x + 20, y + nameYOff, bright ? 0xFFFFFF : 0xAAAAAA, true);
    }

    private void renderPortraitRow(GuiGraphics graphics, List<Holder<Origin>> options, int startX, int startY, int mouseX, int mouseY) {
        int x = startX;
        boolean hoveredAny = false;
        int effCollapsed = getEffectiveCardCollapsedWidth();
        int effExpanded = getEffectiveCardExpandedWidth();
        
        for (int i = 0; i < options.size(); i++) {
            Holder<Origin> origin = options.get(i);
            float progress = this.cardExpandProgress[i];
            int width = (int) net.minecraft.util.Mth.lerp(progress, effCollapsed, effExpanded);
            
            boolean isHovered = mouseX >= x && mouseX < x + width && mouseY >= startY && mouseY < startY + CARD_HEIGHT;
            if (isHovered) {
                this.hoveredOrigin = origin;
                hoveredAny = true;
            }

            ResourceLocation texture = getPortraitTexture(origin.value().getIcon());
            int uvX = (32 - width) / 2;
            graphics.blit(texture, x, startY, width, CARD_HEIGHT, uvX, 0, width, CARD_HEIGHT, 32, 32);
            
            boolean isSelected = (this.selectedOrigin != null && this.selectedOrigin.equals(origin));
            if (isSelected) {
                graphics.fill(x, startY + CARD_HEIGHT + 2, x + width, startY + CARD_HEIGHT + 4, 0xFFFFFFFF);
            }
            
            x += width;
        }
        
        if (!hoveredAny) {
            if (this.selectedOrigin != null) {
                this.hoveredOrigin = this.selectedOrigin;
            } else {
                this.hoveredOrigin = null;
            }
        }
    }

    private void renderIconGrid(GuiGraphics graphics, List<Holder<Origin>> options, int startX, int startY, int mouseX, int mouseY) {
        int x = startX;
        int y = startY;
        int count = 0;
        boolean hoveredAny = false;
        int paperLeft = this.width / 2 - 128;
        int availableWidth = Math.max(40, paperLeft - startX - 4);
        int iconsPerRow = Math.max(1, availableWidth / 20);

        for (Holder<Origin> origin : options) {
            if (count > 0 && count % iconsPerRow == 0) {
                x = startX;
                y += 20;
            }
            
            boolean isHovered = mouseX >= x && mouseX < x + ICON_SIZE && mouseY >= y && mouseY < y + ICON_SIZE;
            if (isHovered) {
                this.hoveredOrigin = origin;
                hoveredAny = true;
                graphics.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2, 0x44FFFFFF);
            }
            
            renderIcon(graphics, origin, x, y);
            
            boolean isSelected = (this.selectedOrigin != null && this.selectedOrigin.equals(origin));
            if (isSelected) {
                graphics.fill(x, y + ICON_SIZE + 1, x + ICON_SIZE, y + ICON_SIZE + 2, 0xFFFFFFFF);
            }
            
            x += 20;
            count++;
        }
        
        if (!hoveredAny) {
            if (this.selectedOrigin != null) {
                this.hoveredOrigin = this.selectedOrigin;
            } else {
                this.hoveredOrigin = null;
            }
        }
    }

    private ResourceLocation getPortraitTexture(net.minecraft.world.item.ItemStack iconStack) {
        ResourceLocation itemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(iconStack.getItem());
        if (itemId != null && itemId.getNamespace().equals("otherworldorigins") && itemId.getPath().startsWith("portrait/")) {
            String portraitName = itemId.getPath().substring("portrait/".length());
            return OtherworldOrigins.loc("textures/item/origin_portrait_" + portraitName + ".png");
        }
        return OtherworldOrigins.loc("textures/item/origin_portrait_base.png");
    }

    private void renderCenterPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.dynamicPromptMode) {
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
        Holder<Origin> displayOrigin = this.hoveredOrigin != null ? this.hoveredOrigin : this.selectedOrigin;
        if (displayOrigin == null || !displayOrigin.isBound()) return;

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

        boolean isAnaconda = AnacondaMultipartHandler.isAnaconda(entityTypeId);
        if (isAnaconda) {
            scale = (int) (scale * 0.2f);
        }

        int yPos = centerY + (int) (bbHeight * scale / 2);

        float partialTick = Minecraft.getInstance().getFrameTime();

        if (isAnaconda) {
            renderAnacondaParts(graphics, living, centerX, yPos, scale, partialTick);
        } else {
            float spinAngleDeg = (float) Math.toDegrees(this.time * 0.04f);
            renderPreviewEntity(graphics, living, centerX, yPos, scale, 180 + spinAngleDeg);
        }
    }

    private void renderPreviewEntity(GuiGraphics graphics, LivingEntity living, int x, int y,
                                     int scale, float facingDeg) {
        renderPreviewEntity(graphics, living, x, y, 0, scale, facingDeg);
    }

    private void renderPreviewEntity(GuiGraphics graphics, LivingEntity living, int x, int y, float zOffset,
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

        if (zOffset != 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, zOffset);
        }

        ShapeshiftRenderHelper.setRenderingShapeshiftBody(true);
        InventoryScreen.renderEntityInInventory(graphics, x, y, scale, pose, camera, living);
        ShapeshiftRenderHelper.setRenderingShapeshiftBody(false);

        if (zOffset != 0) {
            graphics.pose().popPose();
        }

        living.yBodyRot = savedBodyRot;
        living.yBodyRotO = savedBodyRotO;
        living.setYRot(savedYRot);
        living.setXRot(savedXRot);
        living.yHeadRot = savedHeadRot;
        living.yHeadRotO = savedHeadRotO;
    }

    private void renderAnacondaParts(GuiGraphics graphics, LivingEntity head, int centerX, int yPos,
                                     int scale, float partialTick) {
        EntityAnacondaPart[] parts = AnacondaMultipartHandler.getParts(PREVIEW_ENTITY_CACHE_ID);
        if (parts == null) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) return;
            AnacondaMultipartHandler.tickAndPosition(PREVIEW_ENTITY_CACHE_ID, head, (EntityAnaconda) head, true);
            parts = AnacondaMultipartHandler.getParts(PREVIEW_ENTITY_CACHE_ID);
            if (parts == null) return;
        }

        double hx = net.minecraft.util.Mth.lerp(partialTick, head.xo, head.getX());
        double hy = net.minecraft.util.Mth.lerp(partialTick, head.yo, head.getY());
        double hz = net.minecraft.util.Mth.lerp(partialTick, head.zo, head.getZ());
        float headYaw = net.minecraft.util.Mth.lerp(partialTick, head.yBodyRotO, head.yBodyRot);

        List<PartRenderInfo> renderQueue = new ArrayList<>();
        
        renderQueue.add(new PartRenderInfo(head, centerX, yPos, 0, headYaw));

        for (int i = 0; i < parts.length; i++) {
            EntityAnacondaPart part = parts[i];
            if (part == null) continue;

            double px = net.minecraft.util.Mth.lerp(partialTick, part.xo, part.getX());
            double py = net.minecraft.util.Mth.lerp(partialTick, part.yo, part.getY());
            double pz = net.minecraft.util.Mth.lerp(partialTick, part.zo, part.getZ());

            float dx = (float)(px - hx);
            float dy = (float)(py - hy);
            float dz = (float)(pz - hz);

            org.joml.Vector3f vec = new org.joml.Vector3f(dx, dy, dz);

            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf camera = new Quaternionf().rotateX(ISOMETRIC_TILT);
            pose.mul(camera);

            vec.rotate(pose);
            vec.mul(scale, scale, -scale);

            float partYaw = net.minecraft.util.Mth.lerp(partialTick, part.yBodyRotO, part.yBodyRot);

            renderQueue.add(new PartRenderInfo(part, centerX + (int)vec.x(), yPos + (int)vec.y(), vec.z(), partYaw));
        }

        renderQueue.sort(Comparator.comparingDouble(p -> p.zOffset));

        for (PartRenderInfo info : renderQueue) {
            renderPreviewEntity(graphics, info.part, info.screenX, info.screenY, info.zOffset, scale, info.yaw);
        }
    }

    private static class PartRenderInfo {
        final LivingEntity part;
        final int screenX;
        final int screenY;
        final float zOffset;
        final float yaw;

        PartRenderInfo(LivingEntity part, int screenX, int screenY, float zOffset, float yaw) {
            this.part = part;
            this.screenX = screenX;
            this.screenY = screenY;
            this.zOffset = zOffset;
            this.yaw = yaw;
        }
    }

    @Nullable
    private ResourceLocation getShapeshiftEntityType(Holder<Origin> origin) {
        for (Holder<ConfiguredPower<?, ?>> powerHolder : origin.value().getValidPowers().toList()) {
            if (!powerHolder.isBound()) continue;
            ResourceLocation found = findShapeshiftType(powerHolder.value());
            if (found != null) return found;
        }
        return null;
    }

    @Nullable
    private ResourceLocation findShapeshiftType(ConfiguredPower<?, ?> power) {
        if (power.getFactory() instanceof ShapeshiftPower &&
                power.getConfiguration() instanceof ShapeshiftPower.Configuration config) {
            return config.entityType();
        }
        for (Holder<ConfiguredPower<?, ?>> subHolder : power.getContainedPowers().values()) {
            if (subHolder.isBound()) {
                ResourceLocation found = findShapeshiftType(subHolder.value());
                if (found != null) return found;
            }
        }
        return null;
    }

    private void renderRightPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        Holder<Origin> displayOrigin = this.hoveredOrigin != null ? this.hoveredOrigin : this.selectedOrigin;
        if (displayOrigin == null || !displayOrigin.isBound()) return;

        int panelX = this.width - RIGHT_PANEL_WIDTH - 10;
        int panelY = 20;
        int panelHeight = this.height - 60;
        int textWidth = RIGHT_PANEL_WIDTH - 16;

        graphics.fill(panelX, panelY, panelX + RIGHT_PANEL_WIDTH, panelY + panelHeight, 0x88000000);

        Origin origin = displayOrigin.value();
        renderIcon(graphics, displayOrigin, panelX + 5, panelY + 5);
        graphics.drawString(this.font, origin.getName(), panelX + 25, panelY + 9, 0xFFFFFF, true);

        Impact impact = origin.getImpact();
        int impactValue = impact.getImpactValue();
        int wOffset = impactValue * 8;
        for (int i = 0; i < 3; i++) {
            if (i < impactValue) {
                graphics.blit(WINDOW, panelX + RIGHT_PANEL_WIDTH - 35 + i * 10, panelY + 9, 176 + wOffset, 16, 8, 8);
            } else {
                graphics.blit(WINDOW, panelX + RIGHT_PANEL_WIDTH - 35 + i * 10, panelY + 9, 176, 16, 8, 8);
            }
        }
        
        if (mouseX >= panelX + RIGHT_PANEL_WIDTH - 35 && mouseX <= panelX + RIGHT_PANEL_WIDTH - 5 && mouseY >= panelY + 9 && mouseY <= panelY + 17) {
            Component ttc = Component.translatable("origins.gui.impact.impact").append(": ").append(impact.getTextComponent());
            graphics.renderTooltip(this.font, ttc, mouseX, mouseY);
        }

        graphics.enableScissor(panelX, panelY + 28, panelX + RIGHT_PANEL_WIDTH, panelY + panelHeight);

        int startY = panelY + 30;
        int y = startY - this.rightPanelScrollPos;

        this.renderedBadges.clear();

        Component orgDesc = appendExtraInfo(origin.getDescription(), displayOrigin);
        for (FormattedCharSequence line : this.font.split(orgDesc, textWidth)) {
            graphics.drawString(this.font, line, panelX + 5, y, 0xCCCCCC, false);
            y += 12;
        }

        y += 14;

        Registry<ConfiguredPower<?, ?>> powers = ApoliAPI.getPowers();
        for (Holder<ConfiguredPower<?, ?>> holder : origin.getValidPowers().toList()) {
            if (holder.isBound() && !holder.get().getData().hidden()) {
                Optional<ResourceLocation> id = holder.unwrap().map(Optional::of, powers::getResourceKey).map(ResourceKey::location);
                if (id.isPresent()) {
                    ConfiguredPower<?, ?> p = holder.get();
                    FormattedCharSequence name = Language.getInstance().getVisualOrder(this.font.substrByWidth(p.getData().getName().withStyle(ChatFormatting.UNDERLINE), textWidth));
                    graphics.drawString(this.font, name, panelX + 5, y, 0xFFFFFF, false);

                    int tw = this.font.width(name);
                    Collection<Badge> badges = BadgeManager.getPowerBadges(id.get());
                    int badgeX = panelX + 5 + tw + 4;
                    int bi = 0;

                    for (Badge badge : badges) {
                        this.renderedBadges.add(new RenderedBadge(p, badge, badgeX + 10 * bi, y - 1));
                        graphics.blit(badge.spriteId(), badgeX + 10 * bi, y - 1, 0, 0, 9, 9, 9, 9);
                        bi++;
                    }

                    List<FormattedCharSequence> drawLines = this.font.split(p.getData().getDescription(), textWidth);
                    for (FormattedCharSequence line : drawLines) {
                        y += 12;
                        graphics.drawString(this.font, line, panelX + 5 + 2, y, 0xCCCCCC, false);
                    }
                    y += 14;
                }
            }
        }

        graphics.disableScissor();

        this.rightPanelMaxScroll = Math.max(0, y + this.rightPanelScrollPos - (panelY + panelHeight));

        for (RenderedBadge rb : this.renderedBadges) {
            if (mouseX >= rb.x && mouseX < rb.x + 9 && mouseY >= rb.y && mouseY < rb.y + 9 && rb.badge.hasTooltip()) {
                int widthLimit = this.width - mouseX - 24;
                ((DrawContextAccessor)graphics).invokeDrawTooltip(this.font, rb.badge.getTooltipComponents(rb.powerType, widthLimit, this.time, this.font), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
            }
        }
    }

    private final LinkedList<RenderedBadge> renderedBadges = new LinkedList<>();

    private class RenderedBadge {
        private final ConfiguredPower<?, ?> powerType;
        private final Badge badge;
        private final int x;
        private final int y;

        public RenderedBadge(ConfiguredPower<?, ?> powerType, Badge badge, int x, int y) {
            this.powerType = powerType;
            this.badge = badge;
            this.x = x;
            this.y = y;
        }
    }

    private void renderIcon(GuiGraphics graphics, Holder<Origin> originHolder, int x, int y) {
        String originPath = originHolder.unwrapKey().map(key -> key.location().getPath()).orElse("");
        String spellName = null;
        if (originPath.startsWith("cantrips/two/")) {
            spellName = originPath.substring("cantrips/two/".length());
        } else if (originPath.startsWith("cantrips/")) {
            spellName = originPath.substring("cantrips/".length());
        }

        if (spellName != null) {
            String namespace = otherworldorigins$resolveNamespace(spellName);
            ResourceLocation iconTexture = ResourceLocation.fromNamespaceAndPath(namespace, "textures/gui/spell_icons/" + spellName + ".png");
            graphics.blit(iconTexture, x, y, 0, 0, 16, 16, 16, 16);
            return;
        }

        graphics.renderItem(originHolder.value().getIcon(), x, y);
    }

    private MutableComponent appendExtraInfo(Component orgDesc, Holder<Origin> currentOrigin) {
        String originPath = currentOrigin.unwrapKey().map(key -> key.location().getPath()).orElse("");
        MutableComponent modifiedDesc = orgDesc.copy();

        if (originPath.startsWith("class/")) {
            modifiedDesc = appendEnchantmentAccess(modifiedDesc, originPath.substring("class/".length()));
        } else if (originPath.startsWith("cantrips/two/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/two/".length()));
        } else if (originPath.startsWith("cantrips/")) {
            modifiedDesc = appendCantripDesc(modifiedDesc, originPath.substring("cantrips/".length()));
        }

        return modifiedDesc;
    }

    private MutableComponent appendEnchantmentAccess(MutableComponent desc, String className) {
        if (!OtherworldOriginsConfig.ENABLE_ENCHANTMENT_RESTRICTIONS.get()) return desc;

        List<Enchantment> classEnchantments = EnchantmentRestrictions.getEnchantmentTextForClass(className);
        if (classEnchantments.isEmpty()) return desc;

        desc.append("\n\n").append(Component.translatable("otherworldorigins.gui.enchantment_access").withStyle(style -> style.withUnderlined(true).withColor(16738047)));

        String formattedClass = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase() + "s";
        for (Enchantment enchantment : classEnchantments) {
            Component enchantmentName = Component.translatable(enchantment.getDescriptionId());
            Component fullMessage = Component.translatable("otherworldorigins.gui.enchantment_restriction", formattedClass, enchantmentName).withStyle(style -> style.withColor(16738047));
            desc.append("\n").append(Component.literal("• ")).append(fullMessage);
        }

        return desc;
    }

    private static String otherworldorigins$resolveNamespace(String spellName) {
        for (AbstractSpell spell : SpellRegistry.REGISTRY.get().getValues()) {
            if (spell.getSpellResource().getPath().equals(spellName)) {
                return spell.getSpellResource().getNamespace();
            }
        }
        return IronsSpellbooks.MODID;
    }

    private MutableComponent appendCantripDesc(MutableComponent desc, String spellName) {
        String namespace = otherworldorigins$resolveNamespace(spellName);
        ResourceLocation spellId = ResourceLocation.fromNamespaceAndPath(namespace, spellName);
        Component spellDesc = Component.translatable("spell." + spellId.getNamespace() + "." + spellId.getPath() + ".guide").withStyle(style -> style.withItalic(true));
        desc.append("\n\n").append(spellDesc);
        return desc;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int x = 10;
        int y = 10;

        Set<Integer> clickedAsPair = new HashSet<>();

        for (int i = 0; i <= this.currentLayerIndex; i++) {
            if (i >= this.layerList.size()) break;
            if (clickedAsPair.contains(i)) continue;

            Holder<OriginLayer> layer = this.layerList.get(i);
            boolean isActive = (i == this.currentLayerIndex);
            boolean isCompleted = (i < this.currentLayerIndex);

            if (isCompleted && !this.confirmedSelections.containsKey(i)) continue;

            if (isActive) {
                y += 12;

                List<Holder<Origin>> options = this.layerOriginCache.get(i);
                if (options != null && !options.isEmpty()) {
                    if (isPortraitLayer(layer)) {
                        int cx = x;
                        int effCollapsed = getEffectiveCardCollapsedWidth();
                        int effExpanded = getEffectiveCardExpandedWidth();
                        for (int j = 0; j < options.size(); j++) {
                            int width = (int) net.minecraft.util.Mth.lerp(this.cardExpandProgress[j], effCollapsed, effExpanded);
                            if (mouseX >= cx && mouseX < cx + width && mouseY >= y && mouseY < y + CARD_HEIGHT) {
                                Holder<Origin> clicked = options.get(j);
                                if (clicked.equals(this.selectedOrigin)) {
                                    confirmSelection();
                                } else {
                                    this.selectedOrigin = clicked;
                                    updateButtonStates();
                                }
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
                        for (int j = 0; j < options.size(); j++) {
                            if (count > 0 && count % iconsPerRow == 0) {
                                cx = x;
                                cy += 20;
                            }
                            if (mouseX >= cx && mouseX < cx + ICON_SIZE && mouseY >= cy && mouseY < cy + ICON_SIZE) {
                                Holder<Origin> clicked = options.get(j);
                                if (clicked.equals(this.selectedOrigin)) {
                                    confirmSelection();
                                } else {
                                    this.selectedOrigin = clicked;
                                    updateButtonStates();
                                }
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
                Integer pairIdx = findConfirmedPairIndex(i);

                if (pairIdx != null) {
                    clickedAsPair.add(pairIdx);
                    int halfW = LEFT_PANEL_WIDTH / 2;
                    if (mouseX >= x && mouseX < x + halfW && mouseY >= y && mouseY < y + rowH) {
                        revertToLayer(i);
                        return true;
                    }
                    if (mouseX >= x + halfW && mouseX < x + LEFT_PANEL_WIDTH && mouseY >= y && mouseY < y + rowH) {
                        revertToLayer(pairIdx);
                        return true;
                    }
                } else {
                    if (mouseX >= x && mouseX <= x + LEFT_PANEL_WIDTH && mouseY >= y && mouseY < y + rowH) {
                        revertToLayer(i);
                        return true;
                    }
                }
                y += rowH;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        int panelX = this.width - RIGHT_PANEL_WIDTH - 10;
        int panelY = 20;
        int panelHeight = this.height - 60;
        
        if (mouseX >= panelX && mouseX <= panelX + RIGHT_PANEL_WIDTH && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            int np = this.rightPanelScrollPos - (int)scroll * 12;
            this.rightPanelScrollPos = np < 0 ? 0 : Math.min(np, this.rightPanelMaxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}