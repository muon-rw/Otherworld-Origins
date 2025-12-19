package dev.muon.otherworldorigins.client.screen;

import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.network.ResetOriginsMessage;
import dev.muon.otherworldorigins.util.ClientLayerScreenHelper;
import io.github.apace100.origins.component.OriginComponent;
import io.github.apace100.origins.content.OrbOfOriginItem;
import io.github.apace100.origins.origin.Origin;
import io.github.apace100.origins.origin.OriginLayer;
import io.github.apace100.origins.origin.OriginLayers;
import io.github.apace100.origins.registry.ModComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.ChatFormatting;

@OnlyIn(Dist.CLIENT)
public class FinalConfirmScreen extends Screen {
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 14;
    private static final ResourceLocation CHARACTER_SHEET = OtherworldOrigins.loc("textures/gui/character_sheet.png");
    private static final int SHEET_WIDTH = 256;
    private static final int SHEET_HEIGHT = 256;
    private static final int TEXT_WIDTH = 190; // Writable area width in pixels
    
    private List<FormattedCharSequence> displayLines;
    private Set<Integer> headerLineIndices;

    public FinalConfirmScreen() {
        super(Component.translatable("otherworldorigins.gui.final_confirm.title"));
        this.displayLines = new ArrayList<>();
        this.headerLineIndices = new HashSet<>();
    }

    @Override
    protected void init() {
        super.init();
        
        // Build display lines
        buildDisplayLines();
        
        int centerX = this.width / 2;
        int sheetY = (this.height - SHEET_HEIGHT) / 2;
        
        // Calculate button Y position - default is below the sheet
        int defaultButtonY = sheetY + SHEET_HEIGHT + PADDING;
        int buttonY;
        
        // Check if buttons would be fully visible at default position
        if (defaultButtonY + BUTTON_HEIGHT + PADDING <= this.height) {
            // Use default position
            buttonY = defaultButtonY;
        } else {
            // Anchor to bottom with padding
            buttonY = this.height - BUTTON_HEIGHT - PADDING * 2;
        }
        
        // Confirm button
        this.addRenderableWidget(Button.builder(
                Component.translatable("otherworldorigins.gui.final_confirm.confirm"),
                button -> {
                    // Clear tracked layers when confirming
                    ClientLayerScreenHelper.clearSelectedLayers();
                    this.onClose();
                })
                .bounds(centerX - BUTTON_WIDTH - PADDING + 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        
        // Start Over button  
        this.addRenderableWidget(Button.builder(
                Component.translatable("otherworldorigins.gui.final_confirm.start_over"),
                button -> {
                    ClientLayerScreenHelper.clearSelectedLayers(); // Clear tracked layers when starting over
                    OtherworldOrigins.CHANNEL.sendToServer(new ResetOriginsMessage());
                    this.onClose();
                })
                .bounds(centerX + PADDING + 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }
    
    private void buildDisplayLines() {
        displayLines.clear();
        headerLineIndices.clear();
        
        OriginComponent originComponent = ModComponents.ORIGIN.maybeGet(minecraft.player).orElse(null);
        if (originComponent == null) return;
        
        Component playerName = minecraft.player.getName().copy().withStyle(ChatFormatting.ITALIC);
        
        // Get main selections
        String race = getOriginDisplayName(originComponent, OtherworldOrigins.loc("race"));
        String subrace = getOriginDisplayName(originComponent, OtherworldOrigins.loc("subrace"));
        String className = getOriginDisplayName(originComponent, OtherworldOrigins.loc("class"));
        String subclassName = getOriginDisplayName(originComponent, OtherworldOrigins.loc("subclass"));
        
        // Deduplicate race/subrace combinations
        if (subrace != null && race != null && subrace.endsWith(" " + race)) {
            subrace = subrace.substring(0, subrace.length() - race.length() - 1);
        }
        
        // Main character description
        if (race != null && className != null) {
            // Add "Race and Class" header
            Component raceClassHeader = Component.translatable("otherworldorigins.gui.final_confirm.race_class_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(raceClassHeader);
            
            Component mainText;
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
                        subrace != null ? subrace : "",
                        race);
            }
            addWrappedText(mainText);
            displayLines.add(FormattedCharSequence.EMPTY); // Blank line
        }
        
        // Feats
        List<String> featNames = getFeats(originComponent);
        if (!featNames.isEmpty()) {
            // Add "Feats" header
            Component featsHeader = Component.translatable("otherworldorigins.gui.final_confirm.feats_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(featsHeader);
            
            Component featsText = formatFeatsList(featNames);
            addWrappedText(featsText);
            displayLines.add(FormattedCharSequence.EMPTY); // Blank line
        }
        
        // Cantrips
        List<String> cantrips = getCantrips(originComponent);
        if (!cantrips.isEmpty()) {
            // Add "Cantrips" header
            Component cantripsHeader = Component.translatable("otherworldorigins.gui.final_confirm.cantrips_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(cantripsHeader);
            
            Component cantripText = formatCantripsList(cantrips);
            addWrappedText(cantripText);
            displayLines.add(FormattedCharSequence.EMPTY); // Blank line
        }
        
        // Aptitude bonuses
        List<Component> aptitudes = getAptitudeBonuses(originComponent);
        if (!aptitudes.isEmpty()) {
            // Add "Aptitude Bonuses" header
            Component aptitudesHeader = Component.translatable("otherworldorigins.gui.final_confirm.aptitudes_header")
                    .withStyle(style -> style.withUnderlined(true));
            addWrappedTextAsHeader(aptitudesHeader);
            
            for (Component aptitude : aptitudes) {
                addWrappedText(aptitude);
            }
        }
    }
    
    private void addWrappedText(Component text) {
        List<FormattedCharSequence> wrapped = this.font.split(text, TEXT_WIDTH);
        displayLines.addAll(wrapped);
    }
    
    private void addWrappedTextAsHeader(Component text) {
        int startIndex = displayLines.size();
        List<FormattedCharSequence> wrapped = this.font.split(text, TEXT_WIDTH);
        displayLines.addAll(wrapped);
        // Mark all lines of this header as header lines
        for (int i = startIndex; i < displayLines.size(); i++) {
            headerLineIndices.add(i);
        }
    }

    private String getOriginDisplayName(OriginComponent originComponent, ResourceLocation layerId) {
        try {
            OriginLayer layer = null;//OriginLayers.getLayer(layerId);
            Origin origin = originComponent.getOrigin(layer);

            if (origin != null && origin != Origin.EMPTY) {
                return origin.getName().getString();
            }
        } catch (IllegalArgumentException e) {
            // Layer doesn't exist
        }
        return null;
    }
    
    private List<String> getFeats(OriginComponent originComponent) {
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
            String featName = getOriginDisplayName(originComponent, layerId);
            if (featName != null) {
                feats.add(featName);
            }
        }
        
        return feats;
    }
    
    private Component formatFeatsList(List<String> feats) {
        if (feats.isEmpty()) return Component.empty();
        
        if (feats.size() == 1) {
            return Component.translatable("otherworldorigins.gui.final_confirm.feats_single", feats.get(0));
        } else if (feats.size() == 2) {
            return Component.translatable("otherworldorigins.gui.final_confirm.feats_double", feats.get(0), feats.get(1));
        } else {
            // Join all but last with commas, then add "and" for the last one
            String allButLast = String.join(", ", feats.subList(0, feats.size() - 1));
            return Component.translatable("otherworldorigins.gui.final_confirm.feats_multiple", allButLast, feats.get(feats.size() - 1));
        }
    }
    
    private List<String> getCantrips(OriginComponent originComponent) {
        List<String> cantrips = new ArrayList<>();
        
        String cantrip1 = getOriginDisplayName(originComponent, OtherworldOrigins.loc("cantrip_one"));
        String cantrip2 = getOriginDisplayName(originComponent, OtherworldOrigins.loc("cantrip_two"));
        
        if (cantrip1 != null) cantrips.add(cantrip1);
        if (cantrip2 != null) cantrips.add(cantrip2);
        
        return cantrips;
    }
    
    private Component formatCantripsList(List<String> cantrips) {
        if (cantrips.isEmpty()) return Component.empty();
        
        if (cantrips.size() == 1) {
            return Component.translatable("otherworldorigins.gui.final_confirm.cantrips_single", cantrips.get(0));
        } else {
            return Component.translatable("otherworldorigins.gui.final_confirm.cantrips_double", cantrips.get(0), cantrips.get(1));
        }
    }
    
    private List<Component> getAptitudeBonuses(OriginComponent originComponent) {
        List<Component> aptitudes = new ArrayList<>();
        
        // Check for +1 aptitudes
        String plusOne1 = getOriginDisplayName(originComponent, OtherworldOrigins.loc("plus_one_aptitude_one"));
        String plusOne2 = getOriginDisplayName(originComponent, OtherworldOrigins.loc("plus_one_aptitude_two"));
        String plusOneResilient = getOriginDisplayName(originComponent, OtherworldOrigins.loc("plus_one_aptitude_resilient"));
        
        // Check for +2 aptitudes
        String plusTwo1 = getOriginDisplayName(originComponent, OtherworldOrigins.loc("plus_two_aptitude_one"));
        String plusTwo2 = getOriginDisplayName(originComponent, OtherworldOrigins.loc("plus_two_aptitude_two"));
        
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
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        
        // Calculate sheet position
        int sheetX = (this.width - SHEET_WIDTH) / 2;
        int sheetY = (this.height - SHEET_HEIGHT) / 2;
        
        // Render character sheet background
        graphics.blit(CHARACTER_SHEET, sheetX, sheetY, 0, 0, SHEET_WIDTH, SHEET_HEIGHT, SHEET_WIDTH, SHEET_HEIGHT);
        
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Render title above the sheet
        graphics.drawCenteredString(this.font, this.title, this.width / 2, sheetY - 20, 0xFFFFFF);
        
        // Render selections on the sheet
        int textStartY = sheetY + 40; // Start text below the top of the sheet
        int y = textStartY;
        int textX = sheetX + (SHEET_WIDTH - TEXT_WIDTH) / 2; // Center the text area on the sheet
        
        for (int i = 0; i < displayLines.size(); i++) {
            FormattedCharSequence line = displayLines.get(i);
            if (line == FormattedCharSequence.EMPTY) {
                y += LINE_HEIGHT / 2; // Half height for blank lines
            } else {
                if (headerLineIndices.contains(i)) {
                    // Center headers
                    int textWidth = this.font.width(line);
                    int centeredX = sheetX + (SHEET_WIDTH - textWidth) / 2;
                    graphics.drawString(this.font, line, centeredX, y, 0x3F3F3F, false);
                } else {
                    // Left-align regular text
                    graphics.drawString(this.font, line, textX, y, 0x3F3F3F, false);
                }
                y += LINE_HEIGHT;
            }
        }
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Don't allow ESC to close this screen
    }
}
