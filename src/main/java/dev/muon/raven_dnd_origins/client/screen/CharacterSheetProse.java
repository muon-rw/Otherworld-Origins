package dev.muon.raven_dnd_origins.client.screen;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.overgrown.origins.client.OriginsClientState;
import dev.overgrown.origins.origin.Origin;
import dev.overgrown.origins.origin.OriginRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds and renders the "You are %s, ..." character-sheet prose from the player's synced origin
 * picks. Shared by {@link FinalConfirmScreen} and {@link ScopedConfirmScreen} so a scoped
 * reselection confirm shows the same complete summary as initial creation, including selections
 * that weren't re-picked that turn.
 */
final class CharacterSheetProse {

    private static final int SHEET_WIDTH = 256;
    private static final int TEXT_WIDTH = 190;
    private static final int LINE_HEIGHT = 14;

    private final List<FormattedCharSequence> lines = new ArrayList<>();
    private final Set<Integer> headerIndices = new HashSet<>();

    void rebuild(Font font, @Nullable Player player) {
        lines.clear();
        headerIndices.clear();
        if (player == null) {
            return;
        }

        Map<ResourceLocation, ResourceLocation> picks = OriginsClientState.get(player.getUUID());

        Component playerName = player.getName().copy().withStyle(ChatFormatting.ITALIC);

        String race = originDisplayName(picks, RavenDndOrigins.loc("race"));
        String subrace = originDisplayName(picks, RavenDndOrigins.loc("subrace"));
        String className = originDisplayName(picks, RavenDndOrigins.loc("class"));
        String subclassName = originDisplayName(picks, RavenDndOrigins.loc("subclass"));

        // Deduplicate race/subrace combinations
        if (subrace != null && race != null && subrace.endsWith(" " + race)) {
            subrace = subrace.substring(0, subrace.length() - race.length() - 1);
        }

        if (race != null && className != null) {
            Component raceClassHeader = Component.translatable("raven_dnd_origins.gui.final_confirm.race_class_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(font, raceClassHeader);

            Component mainText;
            boolean hasSubclass = subclassName != null && !subclassName.isEmpty();
            boolean warlockPactLine = hasSubclass
                    && selectedClassIsWarlock(picks)
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
                boolean useAn = false;
                if (subclassName != null && !subclassName.isEmpty()) {
                    char firstChar = Character.toLowerCase(subclassName.charAt(0));
                    useAn = (firstChar == 'a' || firstChar == 'e' || firstChar == 'i' ||
                            firstChar == 'o' || firstChar == 'u');
                }

                // "Other" and "Undead" races are placeholders; the subrace carries the heritage name
                if (race.equals("Other") || race.equals("Undead")) {
                    if (subrace != null && !subrace.isEmpty()) {
                        String key = useAn ? "raven_dnd_origins.gui.final_confirm.main_description_no_race_an"
                                          : "raven_dnd_origins.gui.final_confirm.main_description_no_race";
                        mainText = Component.translatable(key,
                                playerName,
                                subclassName != null ? subclassName : "",
                                className,
                                subrace);
                    } else {
                        String key = useAn ? "raven_dnd_origins.gui.final_confirm.main_description_simple_an"
                                          : "raven_dnd_origins.gui.final_confirm.main_description_simple";
                        mainText = Component.translatable(key,
                                playerName,
                                subclassName != null ? subclassName : "",
                                className);
                    }
                } else {
                    String key = useAn ? "raven_dnd_origins.gui.final_confirm.main_description_an"
                                      : "raven_dnd_origins.gui.final_confirm.main_description";
                    mainText = Component.translatable(key,
                            playerName,
                            subclassName != null ? subclassName : "",
                            className,
                            heritagePhrase(subrace, race));
                }
            }
            addWrappedText(font, mainText);
            lines.add(FormattedCharSequence.EMPTY);
        }

        List<String> featNames = getFeats(picks);
        if (!featNames.isEmpty()) {
            Component featsHeader = Component.translatable("raven_dnd_origins.gui.final_confirm.feats_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(font, featsHeader);

            addWrappedText(font, formatFeatsList(featNames));
            lines.add(FormattedCharSequence.EMPTY);
        }

        List<String> cantrips = getCantrips(picks);
        if (!cantrips.isEmpty()) {
            Component cantripsHeader = Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(font, cantripsHeader);

            addWrappedText(font, formatCantripsList(cantrips));
            lines.add(FormattedCharSequence.EMPTY);
        }

        List<Component> aptitudes = getAptitudeBonuses(picks);
        if (!aptitudes.isEmpty()) {
            Component aptitudesHeader = Component.translatable("raven_dnd_origins.gui.final_confirm.aptitudes_header")
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
                y += LINE_HEIGHT / 2;
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

    private static String heritagePhrase(@Nullable String subrace, String race) {
        if (subrace != null && !subrace.isEmpty()) {
            return subrace + " " + race;
        }
        return race;
    }

    private static boolean selectedClassIsWarlock(Map<ResourceLocation, ResourceLocation> picks) {
        ResourceLocation chosen = picks.get(RavenDndOrigins.loc("class"));
        return chosen != null && chosen.equals(RavenDndOrigins.loc("class/warlock"));
    }

    @Nullable
    private static String originDisplayName(Map<ResourceLocation, ResourceLocation> picks, ResourceLocation layerId) {
        ResourceLocation originId = picks.get(layerId);
        if (originId == null || originId.equals(OriginRegistry.EMPTY_ID)) {
            return null;
        }
        Origin origin = OriginRegistry.get(originId);
        return origin == null ? null : origin.name().getString();
    }

    private static List<String> getFeats(Map<ResourceLocation, ResourceLocation> picks) {
        List<String> feats = new ArrayList<>();
        ResourceLocation[] featLayerIds = {
                RavenDndOrigins.loc("free_feat"),
                RavenDndOrigins.loc("first_feat"),
                RavenDndOrigins.loc("second_feat"),
                RavenDndOrigins.loc("third_feat"),
                RavenDndOrigins.loc("fourth_feat"),
                RavenDndOrigins.loc("fifth_feat")
        };

        for (ResourceLocation layerId : featLayerIds) {
            String featName = originDisplayName(picks, layerId);
            if (featName != null) {
                feats.add(featName);
            }
        }

        return feats;
    }

    private static Component formatFeatsList(List<String> feats) {
        if (feats.isEmpty()) return Component.empty();

        if (feats.size() == 1) {
            return Component.translatable("raven_dnd_origins.gui.final_confirm.feats_single", feats.get(0));
        } else if (feats.size() == 2) {
            return Component.translatable("raven_dnd_origins.gui.final_confirm.feats_double", feats.get(0), feats.get(1));
        } else {
            String allButLast = String.join(", ", feats.subList(0, feats.size() - 1));
            return Component.translatable("raven_dnd_origins.gui.final_confirm.feats_multiple", allButLast, feats.get(feats.size() - 1));
        }
    }

    private static List<String> getCantrips(Map<ResourceLocation, ResourceLocation> picks) {
        List<String> cantrips = new ArrayList<>();

        String cantrip1 = originDisplayName(picks, RavenDndOrigins.loc("cantrip_one"));
        String cantrip2 = originDisplayName(picks, RavenDndOrigins.loc("cantrip_two"));

        if (cantrip1 != null) cantrips.add(cantrip1);
        if (cantrip2 != null) cantrips.add(cantrip2);

        ResourceLocation[] elementalLayers = {
                RavenDndOrigins.loc("elemental_discipline_one"),
                RavenDndOrigins.loc("elemental_discipline_two"),
                RavenDndOrigins.loc("elemental_discipline_three"),
                RavenDndOrigins.loc("elemental_discipline_four")
        };
        for (ResourceLocation layerId : elementalLayers) {
            String n = originDisplayName(picks, layerId);
            if (n != null) {
                cantrips.add(n);
            }
        }

        return cantrips;
    }

    private static Component formatCantripsList(List<String> cantrips) {
        if (cantrips.isEmpty()) return Component.empty();

        if (cantrips.size() == 1) {
            return Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_single", cantrips.get(0));
        } else if (cantrips.size() == 2) {
            return Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_double", cantrips.get(0), cantrips.get(1));
        } else {
            String allButLast = String.join(", ", cantrips.subList(0, cantrips.size() - 1));
            return Component.translatable("raven_dnd_origins.gui.final_confirm.cantrips_multiple", allButLast, cantrips.get(cantrips.size() - 1));
        }
    }

    private static List<Component> getAptitudeBonuses(Map<ResourceLocation, ResourceLocation> picks) {
        List<Component> aptitudes = new ArrayList<>();

        String plusOne1 = originDisplayName(picks, RavenDndOrigins.loc("plus_one_aptitude_one"));
        String plusOne2 = originDisplayName(picks, RavenDndOrigins.loc("plus_one_aptitude_two"));
        String plusOneResilient = originDisplayName(picks, RavenDndOrigins.loc("plus_one_aptitude_resilient"));

        String plusTwo1 = originDisplayName(picks, RavenDndOrigins.loc("plus_two_aptitude_one"));
        String plusTwo2 = originDisplayName(picks, RavenDndOrigins.loc("plus_two_aptitude_two"));

        List<String> plusOnes = new ArrayList<>();
        List<String> plusTwos = new ArrayList<>();

        if (plusOne1 != null) plusOnes.add(plusOne1);
        if (plusOne2 != null) plusOnes.add(plusOne2);
        if (plusOneResilient != null) plusOnes.add(plusOneResilient);
        if (plusTwo1 != null) plusTwos.add(plusTwo1);
        if (plusTwo2 != null) plusTwos.add(plusTwo2);

        if (!plusTwos.isEmpty()) {
            aptitudes.add(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitude_plus_two", String.join(", ", plusTwos)));
        }
        if (!plusOnes.isEmpty()) {
            aptitudes.add(Component.translatable("raven_dnd_origins.gui.final_confirm.aptitude_plus_one", String.join(", ", plusOnes)));
        }

        return aptitudes;
    }
}
