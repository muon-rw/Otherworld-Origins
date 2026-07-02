package dev.muon.otherworldorigins.client.screen;

import dev.muon.otherworldorigins.OtherworldOrigins;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds and renders the "You are %s, ..." character-sheet prose from the player's full origin
 * container. Shared by {@link FinalConfirmScreen} and {@link ScopedConfirmScreen} so a scoped
 * reselection confirm shows the same complete summary as initial creation, including selections
 * that weren't re-picked that turn.
 */
@OnlyIn(Dist.CLIENT)
final class CharacterSheetProse {

    private static final int SHEET_WIDTH = 256;
    private static final int TEXT_WIDTH = 190;
    private static final int LINE_HEIGHT = 14;

    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private final Set<Integer> headerIndices = new HashSet<>();

    void rebuild(Font font, Player player) {
        lines.clear();
        headerIndices.clear();
        if (player == null) {
            return;
        }

        IOriginContainer originContainer = IOriginContainer.get(player).resolve().orElse(null);
        if (originContainer == null) {
            return;
        }

        Registry<OriginLayer> layerRegistry = OriginsAPI.getLayersRegistry(null);
        Registry<Origin> originRegistry = OriginsAPI.getOriginsRegistry(null);

        Component playerName = player.getName().copy().withStyle(ChatFormatting.ITALIC);

        // Get main selections
        String race = getOriginDisplayName(originContainer, layerRegistry, originRegistry, OtherworldOrigins.loc("race"));
        String subrace = getOriginDisplayName(originContainer, layerRegistry, originRegistry, OtherworldOrigins.loc("subrace"));
        String className = getOriginDisplayName(originContainer, layerRegistry, originRegistry, OtherworldOrigins.loc("class"));
        String subclassName = getOriginDisplayName(originContainer, layerRegistry, originRegistry, OtherworldOrigins.loc("subclass"));

        // Deduplicate race/subrace combinations
        if (subrace != null && race != null && subrace.endsWith(" " + race)) {
            subrace = subrace.substring(0, subrace.length() - race.length() - 1);
        }

        // Main character description
        if (race != null && className != null) {
            Component raceClassHeader = Component.translatable("otherworldorigins.gui.final_confirm.race_class_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(font, raceClassHeader);

            Component mainText;
            boolean hasSubclass = subclassName != null && !subclassName.isEmpty();
            boolean warlockPactLine = hasSubclass
                    && selectedClassIsWarlock(originContainer, layerRegistry)
                    && (!(race.equals("Other") || race.equals("Undead")) || (subrace != null && !subrace.isEmpty()));

            if (warlockPactLine) {
                String heritageData = (race.equals("Other") || race.equals("Undead"))
                        ? subrace
                        : heritagePhrase(subrace, race);
                mainText = Component.translatable(
                        "otherworldorigins.gui.final_confirm.main_description_warlock_subclass",
                        playerName,
                        subclassName,
                        heritageData);
            } else {
                // Determine if we need "a" or "an" based on the subclass name
                boolean useAn = false;
                if (subclassName != null && !subclassName.isEmpty()) {
                    char firstChar = Character.toLowerCase(subclassName.charAt(0));
                    useAn = (firstChar == 'a' || firstChar == 'e' || firstChar == 'i' ||
                            firstChar == 'o' || firstChar == 'u');
                }

                // Special handling for "Other" and "Undead" races - don't display the race name
                if (race.equals("Other") || race.equals("Undead")) {
                    if (subrace != null && !subrace.isEmpty()) {
                        // Just use subrace as the heritage
                        String key = useAn ? "otherworldorigins.gui.final_confirm.main_description_no_race_an"
                                          : "otherworldorigins.gui.final_confirm.main_description_no_race";
                        mainText = Component.translatable(key,
                                playerName,
                                subclassName != null ? subclassName : "",
                                className,
                                subrace);
                    } else {
                        // No race or subrace to display
                        String key = useAn ? "otherworldorigins.gui.final_confirm.main_description_simple_an"
                                          : "otherworldorigins.gui.final_confirm.main_description_simple";
                        mainText = Component.translatable(key,
                                playerName,
                                subclassName != null ? subclassName : "",
                                className);
                    }
                } else {
                    // Normal race display
                    String key = useAn ? "otherworldorigins.gui.final_confirm.main_description_an"
                                      : "otherworldorigins.gui.final_confirm.main_description";
                    mainText = Component.translatable(key,
                            playerName,
                            subclassName != null ? subclassName : "",
                            className,
                            heritagePhrase(subrace, race));
                }
            }
            addWrappedText(font, mainText);
            lines.add(FormattedCharSequence.EMPTY); // Blank line
        }

        // Feats
        List<String> featNames = getFeats(originContainer, layerRegistry, originRegistry);
        if (!featNames.isEmpty()) {
            Component featsHeader = Component.translatable("otherworldorigins.gui.final_confirm.feats_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(font, featsHeader);

            addWrappedText(font, formatFeatsList(featNames));
            lines.add(FormattedCharSequence.EMPTY); // Blank line
        }

        // Cantrips (includes class cantrips and elemental disciplines)
        List<String> cantrips = getCantrips(originContainer, layerRegistry, originRegistry);
        if (!cantrips.isEmpty()) {
            Component cantripsHeader = Component.translatable("otherworldorigins.gui.final_confirm.cantrips_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(font, cantripsHeader);

            addWrappedText(font, formatCantripsList(cantrips));
            lines.add(FormattedCharSequence.EMPTY); // Blank line
        }

        // Aptitude bonuses
        List<Component> aptitudes = getAptitudeBonuses(originContainer, layerRegistry, originRegistry);
        if (!aptitudes.isEmpty()) {
            Component aptitudesHeader = Component.translatable("otherworldorigins.gui.final_confirm.aptitudes_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(font, aptitudesHeader);

            for (Component aptitude : aptitudes) {
                addWrappedText(font, aptitude);
            }
        }
    }

    /** Draws the prose onto the sheet; headers centered, body text left-aligned to the text column. */
    void render(GuiGraphics graphics, Font font, int sheetX, int textStartY) {
        int y = textStartY;
        int textX = sheetX + (SHEET_WIDTH - TEXT_WIDTH) / 2;

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            if (line == FormattedCharSequence.EMPTY) {
                y += LINE_HEIGHT / 2; // Half height for blank lines
            } else {
                if (headerIndices.contains(i)) {
                    int textWidth = font.width(line);
                    graphics.drawString(font, line, sheetX + (SHEET_WIDTH - textWidth) / 2, y, 0x3F3F3F, false);
                } else {
                    graphics.drawString(font, line, textX, y, 0x3F3F3F, false);
                }
                y += LINE_HEIGHT;
            }
        }
    }

    private void addWrappedText(Font font, Component text) {
        lines.addAll(font.split(text, TEXT_WIDTH));
    }

    private void addWrappedTextAsHeader(Font font, Component text) {
        int startIndex = lines.size();
        lines.addAll(font.split(text, TEXT_WIDTH));
        for (int i = startIndex; i < lines.size(); i++) {
            headerIndices.add(i);
        }
    }

    private static String heritagePhrase(String subrace, String race) {
        if (subrace != null && !subrace.isEmpty()) {
            return subrace + " " + race;
        }
        return race;
    }

    private static boolean selectedClassIsWarlock(IOriginContainer originContainer, Registry<OriginLayer> layerRegistry) {
        ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), OtherworldOrigins.loc("class"));
        Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);
        if (layer == null) return false;
        ResourceKey<Origin> originKey = originContainer.getOrigin(layer);
        return originKey != null && originKey.location().equals(OtherworldOrigins.loc("class/warlock"));
    }

    private static String getOriginDisplayName(IOriginContainer container, Registry<OriginLayer> layerRegistry,
                                               Registry<Origin> originRegistry, ResourceLocation layerId) {
        ResourceKey<OriginLayer> layerKey = ResourceKey.create(layerRegistry.key(), layerId);
        Holder<OriginLayer> layer = layerRegistry.getHolder(layerKey).orElse(null);

        if (layer != null) {
            ResourceKey<Origin> originKey = container.getOrigin(layer);
            if (originKey != null && !originKey.location().equals(ResourceLocation.fromNamespaceAndPath("origins", "empty"))) {
                Holder<Origin> origin = originRegistry.getHolder(originKey).orElse(null);
                if (origin != null) {
                    return origin.value().getName().getString();
                }
            }
        }
        return null;
    }

    private static List<String> getFeats(IOriginContainer container, Registry<OriginLayer> layerRegistry,
                                         Registry<Origin> originRegistry) {
        List<String> feats = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                OtherworldOrigins.loc("free_feat"),
                OtherworldOrigins.loc("first_feat"),
                OtherworldOrigins.loc("second_feat"),
                OtherworldOrigins.loc("third_feat"),
                OtherworldOrigins.loc("fourth_feat"),
                OtherworldOrigins.loc("fifth_feat")
        };

        for (ResourceLocation layerId : featLayerIds) {
            String featName = getOriginDisplayName(container, layerRegistry, originRegistry, layerId);
            if (featName != null) {
                feats.add(featName);
            }
        }

        return feats;
    }

    private static Component formatFeatsList(List<String> feats) {
        if (feats.isEmpty()) return Component.empty();

        if (feats.size() == 1) {
            return Component.translatable("otherworldorigins.gui.final_confirm.feats_single", feats.get(0));
        } else if (feats.size() == 2) {
            return Component.translatable("otherworldorigins.gui.final_confirm.feats_double", feats.get(0), feats.get(1));
        } else {
            String allButLast = String.join(", ", feats.subList(0, feats.size() - 1));
            return Component.translatable("otherworldorigins.gui.final_confirm.feats_multiple", allButLast, feats.get(feats.size() - 1));
        }
    }

    private static List<String> getCantrips(IOriginContainer container, Registry<OriginLayer> layerRegistry,
                                            Registry<Origin> originRegistry) {
        List<String> cantrips = new ArrayList<>();

        String cantrip1 = getOriginDisplayName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("cantrip_one"));
        String cantrip2 = getOriginDisplayName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("cantrip_two"));

        if (cantrip1 != null) cantrips.add(cantrip1);
        if (cantrip2 != null) cantrips.add(cantrip2);

        ResourceLocation[] elementalLayers = {
                OtherworldOrigins.loc("elemental_discipline_one"),
                OtherworldOrigins.loc("elemental_discipline_two"),
                OtherworldOrigins.loc("elemental_discipline_three"),
                OtherworldOrigins.loc("elemental_discipline_four")
        };
        for (ResourceLocation layerId : elementalLayers) {
            String n = getOriginDisplayName(container, layerRegistry, originRegistry, layerId);
            if (n != null) {
                cantrips.add(n);
            }
        }

        return cantrips;
    }

    private static Component formatCantripsList(List<String> cantrips) {
        if (cantrips.isEmpty()) return Component.empty();

        if (cantrips.size() == 1) {
            return Component.translatable("otherworldorigins.gui.final_confirm.cantrips_single", cantrips.get(0));
        } else if (cantrips.size() == 2) {
            return Component.translatable("otherworldorigins.gui.final_confirm.cantrips_double", cantrips.get(0), cantrips.get(1));
        } else {
            String allButLast = String.join(", ", cantrips.subList(0, cantrips.size() - 1));
            return Component.translatable("otherworldorigins.gui.final_confirm.cantrips_multiple", allButLast, cantrips.get(cantrips.size() - 1));
        }
    }

    private static List<Component> getAptitudeBonuses(IOriginContainer container, Registry<OriginLayer> layerRegistry,
                                                      Registry<Origin> originRegistry) {
        List<Component> aptitudes = new ArrayList<>();

        String plusOne1 = getOriginDisplayName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("plus_one_aptitude_one"));
        String plusOne2 = getOriginDisplayName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("plus_one_aptitude_two"));
        String plusOneResilient = getOriginDisplayName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("plus_one_aptitude_resilient"));

        String plusTwo1 = getOriginDisplayName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("plus_two_aptitude_one"));
        String plusTwo2 = getOriginDisplayName(container, layerRegistry, originRegistry, OtherworldOrigins.loc("plus_two_aptitude_two"));

        List<String> plusOnes = new ArrayList<>();
        List<String> plusTwos = new ArrayList<>();

        if (plusOne1 != null) plusOnes.add(plusOne1);
        if (plusOne2 != null) plusOnes.add(plusOne2);
        if (plusOneResilient != null) plusOnes.add(plusOneResilient);
        if (plusTwo1 != null) plusTwos.add(plusTwo1);
        if (plusTwo2 != null) plusTwos.add(plusTwo2);

        if (!plusTwos.isEmpty()) {
            aptitudes.add(Component.translatable("otherworldorigins.gui.final_confirm.aptitude_plus_two", String.join(", ", plusTwos)));
        }
        if (!plusOnes.isEmpty()) {
            aptitudes.add(Component.translatable("otherworldorigins.gui.final_confirm.aptitude_plus_one", String.join(", ", plusOnes)));
        }

        return aptitudes;
    }
}
