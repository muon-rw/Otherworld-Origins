package dev.muon.raven_dnd_origins.client.screen;

import dev.muon.raven_dnd_origins.RavenDndOrigins;
import dev.muon.raven_dnd_origins.config.RavenDndOriginsConfig;
import dev.muon.raven_dnd_origins.power.AllowedSpellsPower;
import dev.muon.raven_dnd_origins.power.LeveledAttributePower;
import dev.muon.raven_dnd_origins.restrictions.EnchantmentRestrictions;
import dev.muon.raven_dnd_origins.school.ModSchools;
import dev.muon.raven_dnd_origins.util.ElementalDisciplineSpellDisplay;
import dev.overgrown.apoli.client.ApoliKeyMappings;
import dev.overgrown.apoli.client.IconRenderer;
import dev.overgrown.apoli.power.ApoliPowers;
import dev.overgrown.apoli.power.Power;
import dev.overgrown.apoli.power.builtin.MultiplePower;
import dev.overgrown.origins.badge.Badge;
import dev.overgrown.origins.badge.KeybindBadge;
import dev.overgrown.origins.badge.TooltipBadge;
import dev.overgrown.origins.client.BadgeClientState;
import dev.overgrown.origins.origin.Origin;
import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector2i;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class OriginDetailPanel {

    public static final int DEFAULT_WIDTH = 160;

    private final Font font = Minecraft.getInstance().font;
    private int x;
    private int y;
    private int width;
    private int height;
    private int screenWidth;
    private int screenHeight;
    private float time;

    private static final int CONTENT_TOP_OFFSET = 40;
    /** Keeps text and the scrollbar clear of the stitching and brass corners. */
    private static final int CONTENT_INSET = 7;
    private static final int SCROLLBAR_TRACK_W = 8;
    private static final int SCROLLBAR_THUMB_W = 3;
    private static final int SCROLLBAR_THUMB_H = 27;
    private static final int ENCHANTMENT_ACCESS_COLOR = 0x5B2A86;

    /**
     * Power badges in the right panel sit next to the screen edge. Vanilla places tooltips to the right of
     * the cursor, and Origins wraps them at {@code screenWidth - mouseX}, which collapses there, so we open
     * them to the left instead, keeping vanilla's 12px gap from the cursor.
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

    private int scrollPos = 0;
    private int contentMaxScroll = 0;
    @Nullable
    private Origin displayed;

    private boolean scrollbarDragging;
    private double scrollbarDragStartMouseY;
    private int scrollbarDragStartScroll;

    private static final ResourceLocation SCHOOL_BADGE_PLACEHOLDER =
            ResourceLocation.fromNamespaceAndPath("origins", "textures/gui/badge/isaacfanta/star.png");

    private static final Map<ResourceLocation, ResourceLocation> SCHOOL_BADGES = Map.of(
            ModSchools.MARTIAL_RESOURCE, RavenDndOrigins.loc("textures/gui/spell_school/martial.png"),
            ModSchools.ARCHERY_RESOURCE, RavenDndOrigins.loc("textures/gui/spell_school/archery.png"));

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
    @Nullable
    private Component pendingTitleTooltip;
    @Nullable
    private TextLayout textLayout;
    private final Map<ResourceLocation, List<AllowedSpellBadge>> spellBadgeCache = new java.util.HashMap<>();
    private final Map<ResourceLocation, List<AllowedSpellBadge>> scalingBadgeCache = new java.util.HashMap<>();

    private boolean renderAllowedSpellsBadges(GuiGraphics graphics, int rightX, int y, int mouseX, int mouseY, Origin origin) {
        List<AllowedSpellBadge> badges = this.spellBadgeCache.computeIfAbsent(origin.id(), id -> collectAllowedSpellBadges(origin));
        boolean isSubclass = origin.id().getPath().startsWith("subclass/");
        if (badges.isEmpty() && isSubclass) {
            badges = List.of(new AllowedSpellBadge(SCHOOL_BADGE_PLACEHOLDER, List.of(
                    Component.translatable("raven_dnd_origins.gui.allowed_spells.none")
                            .withStyle(ChatFormatting.GRAY)
            )));
        }
        return renderTopRightBadgeStrip(graphics, rightX, y, mouseX, mouseY, badges
        );
    }

    private boolean renderWildshapeScalingBadges(GuiGraphics graphics, int rightX, int y, int mouseX, int mouseY, Origin origin) {
        if (!origin.id().getPath().startsWith("wildshape/")) return false;
        List<AllowedSpellBadge> badges = this.scalingBadgeCache.computeIfAbsent(origin.id(), id -> buildWildshapeScalingBadges(origin));
        return renderTopRightBadgeStrip(graphics, rightX, y, mouseX, mouseY, badges
        );
    }

    private boolean renderTopRightBadgeStrip(GuiGraphics graphics, int rightX, int y, int mouseX, int mouseY,
                                             List<AllowedSpellBadge> badges) {
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
            int color = (alpha << 24) | (DeskStyle.PAGE_RUBRIC & 0xFFFFFF);
            int x0 = startX - 1;
            int y0 = y - 1;
            int x1 = startX + stripW + 1;
            int y1 = y + 8 + 1;
            graphics.fill(x0, y0, x1, y0 + 1, color);
            graphics.fill(x0, y1 - 1, x1, y1, color);
            graphics.fill(x0, y0, x0 + 1, y1, color);
            graphics.fill(x1 - 1, y0, x1, y1, color);
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
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);

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
                    AllowedSpellBadge schoolBadge = makeSchoolBadge(entry.substring(1));
                    if (schoolBadge != null) {
                        categoryBadges.add(schoolBadge);
                    } else {
                        Component line = makeSchoolLine(entry.substring(1));
                        if (line != null) additionalLines.add(line);
                    }
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
    private AllowedSpellBadge makeSchoolBadge(String schoolId) {
        ResourceLocation loc = parseAllowedSpellsLoc(schoolId);
        ResourceLocation sprite = loc == null ? null : SCHOOL_BADGES.get(loc);
        SchoolType school = sprite == null ? null : SchoolRegistry.REGISTRY.get(loc);
        if (school == null) return null;
        List<Component> tt = new ArrayList<>();
        tt.add(Component.translatable("raven_dnd_origins.gui.allowed_spells.technique_header",
                school.getDisplayName().getString()).withStyle(ChatFormatting.GOLD));
        for (AbstractSpell spell : SpellRegistry.getSpellsForSchool(school)) {
            tt.add(Component.translatable("raven_dnd_origins.gui.allowed_spells.entry",
                    Component.translatable(spell.getComponentId())).withStyle(ChatFormatting.GRAY));
        }
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

    private record PowerLayout(ResourceLocation powerId, List<FormattedCharSequence> name, List<FormattedCharSequence> description) {}

    private record TextLayout(ResourceLocation originId, int width, List<FormattedCharSequence> description, List<PowerLayout> powers) {}

    /** Wrapped text for the displayed origin, rebuilt only when the origin or the page width changes. */
    private TextLayout textLayout(Origin origin, int textWidth) {
        TextLayout cached = this.textLayout;
        if (cached != null && cached.originId().equals(origin.id()) && cached.width() == textWidth) {
            return cached;
        }
        List<FormattedCharSequence> description = this.font.split(appendExtraInfo(origin.description(), origin), textWidth);
        List<PowerLayout> powers = new ArrayList<>();
        Player viewer = Minecraft.getInstance().player;
        for (ResourceLocation powerId : viewer != null ? origin.powersFor(viewer) : origin.powers()) {
            Power power = ApoliPowers.get(powerId);
            if (power == null || power.hidden()) continue;
            powers.add(new PowerLayout(powerId,
                    this.font.split(power.displayName(powerId), textWidth - 18),
                    this.font.split(power.displayDescription(powerId), textWidth - 2)));
        }
        this.textLayout = new TextLayout(origin.id(), textWidth, description, powers);
        return this.textLayout;
    }

    public void scrollBy(int pixels) {
        this.scrollPos = Mth.clamp(this.scrollPos + pixels, 0, this.contentMaxScroll);
    }

    public int pageHeight() {
        return Math.max(12, this.height - CONTENT_TOP_OFFSET - CONTENT_INSET - 12);
    }

    public void renderPlaceholder(GuiGraphics graphics, OriginScreenLayout.Rect bounds, Component hint) {
        this.displayed = null;
        DeskStyle.page(graphics, bounds.x(), bounds.y(), bounds.width(), bounds.height());
        int textWidth = bounds.width() - 2 * CONTENT_INSET - 8;
        int y = bounds.y() + bounds.height() / 3;
        for (FormattedCharSequence line : this.font.split(hint.copy().withStyle(ChatFormatting.ITALIC), textWidth)) {
            graphics.drawString(this.font, line, bounds.x() + (bounds.width() - this.font.width(line)) / 2, y,
                    DeskStyle.PAGE_SEPIA, false);
            y += 11;
        }
        DeskStyle.diamondRule(graphics, bounds.x() + 20, bounds.right() - 20, y + 6);
    }

    public void render(GuiGraphics graphics, @Nullable Origin displayOrigin, OriginScreenLayout.Rect bounds,
                       @Nullable Component subtitle, int mouseX, int mouseY, float time) {
        this.x = bounds.x();
        this.y = bounds.y();
        this.width = bounds.width();
        this.height = bounds.height();
        this.screenWidth = graphics.guiWidth();
        this.screenHeight = graphics.guiHeight();
        this.time = time;
        if (displayOrigin == null) {
            this.displayed = null;
            return;
        }
        if (this.displayed == null || !displayOrigin.id().equals(this.displayed.id())) {
            resetScroll();
        }
        this.displayed = displayOrigin;

        int panelX = this.x;
        int panelY = this.y;
        int panelHeight = this.height;
        int textX = panelX + CONTENT_INSET + 2;
        int textWidth = this.width - 2 * CONTENT_INSET - SCROLLBAR_TRACK_W;

        DeskStyle.page(graphics, panelX, panelY, this.width, panelHeight);

        graphics.blitSprite(DeskStyle.INITIAL, panelX + 7, panelY + 7, 24, 24);
        renderIcon(graphics, displayOrigin, panelX + 11, panelY + 11);

        int nameX = panelX + 36;

        // Badges share the subtitle line so the origin's name keeps the full header width.
        this.pendingAllowedSpellsTooltip = null;
        this.allowedSpellsStripActive = false;
        int stripRightX = panelX + this.width - 9;
        int stripY = panelY + 21;
        int stripLeft = stripRightX + 4;
        if (RavenDndOriginsConfig.enableSpellRestrictions()
                && renderAllowedSpellsBadges(graphics, stripRightX, stripY, mouseX, mouseY, displayOrigin)) {
            stripLeft = this.allowedSpellsStripX;
        } else if (renderWildshapeScalingBadges(graphics, stripRightX, stripY, mouseX, mouseY, displayOrigin)) {
            stripLeft = this.allowedSpellsStripX;
        }

        int nameRoom = panelX + this.width - 9 - nameX;
        DeskStyle.scrollingString(graphics, this.font, displayOrigin.name(), nameX, panelY + 10, nameRoom, DeskStyle.PAGE_RUBRIC);
        boolean nameOverflows = this.font.width(displayOrigin.name()) > nameRoom;
        if (subtitle != null) {
            graphics.drawString(this.font, ellipsize(subtitle, stripLeft - 4 - nameX), nameX, panelY + 21, DeskStyle.PAGE_SEPIA, false);
        }
        DeskStyle.diamondRule(graphics, panelX + 10, panelX + this.width - 10, panelY + CONTENT_TOP_OFFSET - 4);

        graphics.enableScissor(panelX + CONTENT_INSET, panelY + CONTENT_TOP_OFFSET,
                panelX + this.width - CONTENT_INSET, panelY + panelHeight - CONTENT_INSET);

        int startY = panelY + CONTENT_TOP_OFFSET + 3;
        int scroll = Mth.clamp(this.scrollPos, 0, this.contentMaxScroll);

        this.renderedBadges.clear();

        int y = startY - scroll;

        TextLayout text = textLayout(displayOrigin, textWidth);
        if (nameOverflows && mouseX >= nameX && mouseX < nameX + nameRoom && mouseY >= panelY + 9 && mouseY < panelY + 19) {
            this.pendingTitleTooltip = displayOrigin.name();
        }

        for (FormattedCharSequence line : text.description()) {
            graphics.drawString(this.font, line, textX, y, DeskStyle.PAGE_INK, false);
            y += 11;
        }

        y += 6;
        DeskStyle.diamondRule(graphics, textX, textX + textWidth, y);
        y += 7;

        for (PowerLayout powerText : text.powers()) {
            ResourceLocation powerId = powerText.powerId();
            List<FormattedCharSequence> nameLines = powerText.name();
            int tw = 0;
            int powerNameX = textX;
            if (!nameLines.isEmpty() && this.font.width(nameLines.get(0)) > 0) {
                DeskStyle.rubricMarker(graphics, textX, y);
                powerNameX += 6;
                for (int i = 0; i < nameLines.size(); i++) {
                    if (i > 0) y += 10;
                    graphics.drawString(this.font, nameLines.get(i), powerNameX, y, DeskStyle.PAGE_RUBRIC, false);
                }
                tw = this.font.width(nameLines.get(nameLines.size() - 1));
            }
            int badgeX = tw > 0 ? powerNameX + tw + 4 : textX;
            int bi = 0;
            for (Badge badge : BadgeClientState.get(powerId)) {
                int bx = badgeX + 10 * bi;
                this.renderedBadges.add(new RenderedBadge(powerId, badge, bx, y - 1));
                int spriteSize = BadgeClientState.spriteSize(badge.spriteId());
                graphics.blit(badge.spriteId(), bx, y - 1, 9, 9, 0.0F, 0.0F, spriteSize, spriteSize, spriteSize, spriteSize);
                bi++;
            }

            for (FormattedCharSequence line : powerText.description()) {
                y += 11;
                graphics.drawString(this.font, line, textX + 2, y, DeskStyle.PAGE_BODY, false);
            }
            y += 16;
        }

        int viewBottom = panelY + panelHeight - CONTENT_INSET;
        if (scroll < this.contentMaxScroll) {
            DeskStyle.continuationFade(graphics, panelX + CONTENT_INSET, panelX + this.width - CONTENT_INSET, viewBottom);
        }
        graphics.disableScissor();

        this.contentMaxScroll = Math.max(0, y + scroll - viewBottom);
        this.scrollPos = Mth.clamp(this.scrollPos, 0, this.contentMaxScroll);

        renderScrollbar(graphics, mouseX, mouseY);

        for (RenderedBadge rb : this.renderedBadges) {
            if (mouseX >= rb.x && mouseX < rb.x + 9 && mouseY >= rb.y && mouseY < rb.y + 9 && rb.badge.hasTooltip()) {
                renderBadgeTooltip(graphics, rb, mouseX, mouseY);
            }
        }

        if (this.pendingAllowedSpellsTooltip != null) {
            drawAllowedSpellsTooltip(graphics, this.pendingAllowedSpellsTooltip, mouseX, mouseY);
            this.pendingAllowedSpellsTooltip = null;
        }
        if (this.pendingTitleTooltip != null) {
            graphics.renderTooltip(this.font, this.pendingTitleTooltip, mouseX, mouseY);
            this.pendingTitleTooltip = null;
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

        int maxLineWidth = Math.max(80, this.screenWidth / 2 - margin * 2 - padding * 2 - 5);
        int contentW = 0;
        for (Component line : lines) {
            contentW = Math.max(contentW, Math.min(this.font.width(line), maxLineWidth));
        }

        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(this.font.split(line, contentW));
        }
        int contentH = wrapped.size() * lineHeight;

        int maxBoxH = this.screenHeight - margin * 2;
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
        if (x + boxW > this.screenWidth - margin) x = mouseX - 12 - boxW;
        if (x < margin) x = margin;
        if (y + boxH > this.screenHeight - margin) y = this.screenHeight - margin - boxH;
        if (y < margin) y = margin;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);

        int bg = DeskStyle.TOOLTIP_BG;
        int border = DeskStyle.BRASS;
        graphics.fill(x, y, x + boxW, y + boxH, bg);
        graphics.fill(x - 1, y, x, y + boxH, border);
        graphics.fill(x + boxW, y, x + boxW + 1, y + boxH, border);
        graphics.fill(x, y - 1, x + boxW, y, border);
        graphics.fill(x, y + boxH, x + boxW, y + boxH + 1, border);

        graphics.enableScissor(x + padding, y + padding, x + padding + contentW, y + padding + viewportH);
        int cy = y + padding - this.allowedSpellsTooltipScroll;
        for (FormattedCharSequence line : wrapped) {
            graphics.drawString(this.font, line, x + padding, cy, DeskStyle.HOVER_TEXT, false);
            cy += lineHeight;
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackX = x + boxW - 5;
            int trackY = y + padding;
            int trackH = viewportH;
            graphics.fill(trackX, trackY, trackX + 3, trackY + trackH, DeskStyle.withAlpha(DeskStyle.CREAM, 0x44));
            int thumbH = Math.max(8, (int) ((float) viewportH / contentH * trackH));
            int thumbY = trackY + (int) ((float) this.allowedSpellsTooltipScroll / maxScroll * (trackH - thumbH));
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, DeskStyle.withAlpha(DeskStyle.CREAM, 0xCC));
        }

        graphics.pose().popPose();
    }

    private int scrollbarTrackX() {
        return this.x + this.width - CONTENT_INSET - SCROLLBAR_TRACK_W;
    }

    private int scrollbarTop() {
        return this.y + CONTENT_TOP_OFFSET;
    }

    private int scrollbarViewportHeight() {
        return this.height - CONTENT_TOP_OFFSET - CONTENT_INSET;
    }

    private int scrollbarThumbX() {
        return scrollbarTrackX() + (SCROLLBAR_TRACK_W - SCROLLBAR_THUMB_W) / 2;
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        int viewportH = scrollbarViewportHeight();
        if (this.contentMaxScroll <= 0 || viewportH <= SCROLLBAR_THUMB_H) {
            return;
        }
        int contentTop = scrollbarTop();
        int thumbX = scrollbarThumbX();
        graphics.fill(thumbX, contentTop, thumbX + SCROLLBAR_THUMB_W, contentTop + viewportH, DeskStyle.PAGE_TRACK);

        int thumbY = computeScrollbarThumbY(contentTop, viewportH);
        boolean hover = !this.scrollbarDragging
                && mouseX >= scrollbarTrackX()
                && mouseX < scrollbarTrackX() + SCROLLBAR_TRACK_W
                && mouseY >= thumbY
                && mouseY < thumbY + SCROLLBAR_THUMB_H;
        boolean lit = this.scrollbarDragging || hover;
        graphics.fill(thumbX, thumbY, thumbX + SCROLLBAR_THUMB_W, thumbY + SCROLLBAR_THUMB_H,
                lit ? DeskStyle.RUBRIC_LIGHT : DeskStyle.PAGE_RUBRIC);
        graphics.fill(thumbX, thumbY + SCROLLBAR_THUMB_H - 1, thumbX + SCROLLBAR_THUMB_W, thumbY + SCROLLBAR_THUMB_H,
                DeskStyle.RUBRIC_DARK);
    }

    private int computeScrollbarThumbY(int contentTop, int viewportH) {
        int scroll = Mth.clamp(this.scrollPos, 0, this.contentMaxScroll);
        float part = this.contentMaxScroll <= 0 ? 0.0f : scroll / (float) this.contentMaxScroll;
        int thumbTravel = viewportH - SCROLLBAR_THUMB_H;
        return contentTop + (int) (thumbTravel * part + 0.5f);
    }

    private boolean tryBeginScrollbarDrag(double mouseX, double mouseY) {
        int viewportH = scrollbarViewportHeight();
        if (this.displayed == null || this.contentMaxScroll <= 0 || viewportH <= SCROLLBAR_THUMB_H) {
            return false;
        }
        int thumbY = computeScrollbarThumbY(scrollbarTop(), viewportH);
        if (mouseX >= scrollbarTrackX()
                && mouseX < scrollbarTrackX() + SCROLLBAR_TRACK_W
                && mouseY >= thumbY
                && mouseY < thumbY + SCROLLBAR_THUMB_H) {
            this.scrollbarDragging = true;
            this.scrollbarDragStartMouseY = mouseY;
            this.scrollbarDragStartScroll = Mth.clamp(this.scrollPos, 0, this.contentMaxScroll);
            return true;
        }
        return false;
    }

    private void applyScrollbarDrag(double mouseY) {
        int thumbTravel = scrollbarViewportHeight() - SCROLLBAR_THUMB_H;
        if (thumbTravel <= 0 || this.contentMaxScroll <= 0) {
            return;
        }
        double deltaY = mouseY - this.scrollbarDragStartMouseY;
        int deltaScroll = (int) Math.round(deltaY / thumbTravel * this.contentMaxScroll);
        this.scrollPos = Mth.clamp(this.scrollbarDragStartScroll + deltaScroll, 0, this.contentMaxScroll);
    }

    private Component ellipsize(Component text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        String cut = this.font.plainSubstrByWidth(text.getString(), Math.max(0, maxWidth - this.font.width(ellipsis)));
        return Component.literal(cut + ellipsis).withStyle(text.getStyle());
    }

    private final LinkedList<RenderedBadge> renderedBadges = new LinkedList<>();

    private record RenderedBadge(ResourceLocation powerId, Badge badge, int x, int y) {}

    public static void renderIcon(GuiGraphics graphics, Origin origin, int x, int y) {
        String originPath = origin.id().getPath();
        Optional<ResourceLocation> spell = ElementalDisciplineSpellDisplay.spellIdForCantripOriginPath(originPath)
                .or(() -> ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath));
        if (spell.isPresent()) {
            ResourceLocation id = spell.get();
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
        } else {
            Optional<ResourceLocation> spell = ElementalDisciplineSpellDisplay.spellIdForCantripOriginPath(originPath)
                    .or(() -> ElementalDisciplineSpellDisplay.spellIdForDisciplineOriginPath(originPath));
            if (spell.isPresent()) {
                modifiedDesc = ElementalDisciplineSpellDisplay.appendSpellGuide(modifiedDesc, spell.get());
            }
        }

        return modifiedDesc;
    }

    private MutableComponent appendEnchantmentAccess(MutableComponent desc, String className) {
        if (!RavenDndOriginsConfig.enableEnchantmentRestrictions()) return desc;

        List<Component> classEnchantments = EnchantmentRestrictions.getEnchantmentTextForClass(className);
        if (classEnchantments.isEmpty()) return desc;

        desc.append("\n\n").append(Component.translatable("raven_dnd_origins.gui.enchantment_access").withStyle(style -> style.withUnderlined(true).withColor(ENCHANTMENT_ACCESS_COLOR)));

        Component formattedClass = Component.translatableWithFallback("raven_dnd_origins.gui.class_plural." + className,
                className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase() + "s");
        for (Component enchantmentName : classEnchantments) {
            Component fullMessage = Component.translatable("raven_dnd_origins.gui.enchantment_restriction", formattedClass, enchantmentName).withStyle(style -> style.withColor(ENCHANTMENT_ACCESS_COLOR));
            desc.append("\n").append(Component.literal("• ")).append(fullMessage);
        }

        return desc;
    }

    public void resetScroll() {
        this.scrollPos = 0;
        this.contentMaxScroll = 0;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return button == 0 && tryBeginScrollbarDrag(mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        boolean overPage = mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        return scroll(mouseX, mouseY, scrollY, overPage);
    }

    /** The radial menu's page sits beside the ring rather than under the cursor, so any wheel input scrolls it. */
    public boolean mouseScrolledAnywhere(double mouseX, double mouseY, double scrollY) {
        return scroll(mouseX, mouseY, scrollY, true);
    }

    private boolean scroll(double mouseX, double mouseY, double scrollY, boolean scrollDescription) {
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
        if (!scrollDescription || this.displayed == null || this.contentMaxScroll <= 0) {
            return false;
        }
        boolean canScroll = scrollY > 0
                ? this.scrollPos > 0
                : scrollY < 0 && this.scrollPos < this.contentMaxScroll;
        if (!canScroll) {
            return false;
        }
        this.scrollPos = Mth.clamp(this.scrollPos - (int) scrollY * 12, 0, this.contentMaxScroll);
        return true;
    }

    public boolean mouseDragged(double mouseY, int button) {
        if (button != 0 || !this.scrollbarDragging) {
            return false;
        }
        applyScrollbarDrag(mouseY);
        return true;
    }

    public void mouseReleased() {
        this.scrollbarDragging = false;
    }
}
