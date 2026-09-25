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
import net.minecraft.locale.Language;
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
    private int width;
    private int screenWidth;
    private int screenHeight;
    private float time;

    /**
     * Top of the scrollable description area (below icon/name/impact); matches stock Origins scissor.
     */
    private static final int CONTENT_TOP_OFFSET = 28;
    /**
     * Track/thumb widths and thumb height from stock {@code OriginDisplayScreen} (sprite used only as a ruler).
     */
    private static final int SCROLLBAR_TRACK_W = 8;
    private static final int SCROLLBAR_THUMB_W = 6;
    private static final int SCROLLBAR_THUMB_H = 27;
    private static final int SCROLLBAR_TRACK_ARGB = 0x44FFFFFF;
    private static final int SCROLLBAR_THUMB_ARGB = 0x77FFFFFF;
    private static final int SCROLLBAR_THUMB_ACTIVE_ARGB = 0xAAFFFFFF;

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
     * Virtual scroll in the right description pane: ranges from 0 through top/bottom overscroll
     * padding plus content overflow. Effective content offset is
     * {@code clamp(virtual - overscroll, 0, contentMaxScroll)}.
     */
    private int scrollPos = 0;
    /** Pixels of description content that extend past the scissor (updated each render). */
    private int contentMaxScroll = 0;
    private static final int SCROLL_OVERSCROLL_PX = 24;
    @Nullable
    private Origin displayed;

    private boolean scrollbarDragging;
    private double scrollbarDragStartMouseY;
    private int scrollbarDragStartEffScroll;

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

    public void render(GuiGraphics graphics, @Nullable Origin displayOrigin, int x, int width,
                       int mouseX, int mouseY, float time) {
        this.x = x;
        this.width = width;
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
        int panelY = 20;
        int panelHeight = this.screenHeight - 60;
        int textWidth = this.width - 16;

        graphics.fill(panelX, panelY, panelX + this.width, panelY + panelHeight, 0x88000000);

        renderIcon(graphics, displayOrigin, panelX + 5, panelY + 5);
        graphics.drawString(this.font, displayOrigin.name(), panelX + 25, panelY + 9, 0xFFFFFF, true);

        this.pendingAllowedSpellsTooltip = null;
        this.allowedSpellsStripActive = false;
        int stripRightX = panelX + this.width - 5;
        int stripY = panelY + 9;
        boolean spellBadgesShown = RavenDndOriginsConfig.enableSpellRestrictions()
                && renderAllowedSpellsBadges(graphics, stripRightX, stripY, mouseX, mouseY, displayOrigin);
        if (!spellBadgesShown) {
            renderWildshapeScalingBadges(graphics, stripRightX, stripY, mouseX, mouseY, displayOrigin);
        }

        graphics.enableScissor(panelX, panelY + CONTENT_TOP_OFFSET, panelX + this.width, panelY + panelHeight);

        int startY = panelY + CONTENT_TOP_OFFSET + 2;
        int o = SCROLL_OVERSCROLL_PX;
        int effScroll = this.contentMaxScroll <= 0
                ? 0
                : Mth.clamp(this.scrollPos - o, 0, this.contentMaxScroll);

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

        this.contentMaxScroll = Math.max(0, y + effScroll - (panelY + panelHeight));
        int virtualMax = this.contentMaxScroll <= 0
                ? 0
                : o + this.contentMaxScroll + o;
        if (virtualMax <= 0) {
            this.scrollPos = 0;
        } else {
            this.scrollPos = Mth.clamp(this.scrollPos, 0, virtualMax);
        }

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

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.contentMaxScroll <= 0) {
            return;
        }
        int panelX = this.x;
        int panelY = 20;
        int panelHeight = this.screenHeight - 60;
        int contentTop = panelY + CONTENT_TOP_OFFSET;
        int viewportH = panelHeight - CONTENT_TOP_OFFSET;
        if (viewportH <= SCROLLBAR_THUMB_H) {
            return;
        }
        int trackX = panelX + this.width - SCROLLBAR_TRACK_W;
        graphics.fill(
                trackX,
                contentTop,
                trackX + SCROLLBAR_TRACK_W,
                contentTop + viewportH,
                SCROLLBAR_TRACK_ARGB);

        int thumbX = trackX + (SCROLLBAR_TRACK_W - SCROLLBAR_THUMB_W) / 2;
        int thumbY = computeScrollbarThumbY(contentTop, viewportH);
        boolean hover = !this.scrollbarDragging
                && mouseX >= thumbX
                && mouseX < thumbX + SCROLLBAR_THUMB_W
                && mouseY >= thumbY
                && mouseY < thumbY + SCROLLBAR_THUMB_H;
        int thumbArgb = (this.scrollbarDragging || hover)
                ? SCROLLBAR_THUMB_ACTIVE_ARGB
                : SCROLLBAR_THUMB_ARGB;
        graphics.fill(
                thumbX,
                thumbY,
                thumbX + SCROLLBAR_THUMB_W,
                thumbY + SCROLLBAR_THUMB_H,
                thumbArgb);
    }

    private int computeScrollbarThumbY(int contentTop, int viewportH) {
        int o = SCROLL_OVERSCROLL_PX;
        int eff = Mth.clamp(this.scrollPos - o, 0, this.contentMaxScroll);
        float part = this.contentMaxScroll <= 0 ? 0.0f : eff / (float) this.contentMaxScroll;
        int thumbTravel = viewportH - SCROLLBAR_THUMB_H;
        return contentTop + (int) (thumbTravel * part + 0.5f);
    }

    private boolean tryBeginScrollbarDrag(double mouseX, double mouseY) {
        if (this.displayed == null || this.contentMaxScroll <= 0) {
            return false;
        }
        int panelX = this.x;
        int panelY = 20;
        int panelHeight = this.screenHeight - 60;
        int contentTop = panelY + CONTENT_TOP_OFFSET;
        int viewportH = panelHeight - CONTENT_TOP_OFFSET;
        if (viewportH <= SCROLLBAR_THUMB_H) {
            return false;
        }
        int trackX = panelX + this.width - SCROLLBAR_TRACK_W;
        int thumbX = trackX + (SCROLLBAR_TRACK_W - SCROLLBAR_THUMB_W) / 2;
        int thumbY = computeScrollbarThumbY(contentTop, viewportH);
        if (mouseX >= thumbX
                && mouseX < thumbX + SCROLLBAR_THUMB_W
                && mouseY >= thumbY
                && mouseY < thumbY + SCROLLBAR_THUMB_H) {
            this.scrollbarDragging = true;
            this.scrollbarDragStartMouseY = mouseY;
            int o = SCROLL_OVERSCROLL_PX;
            this.scrollbarDragStartEffScroll =
                    Mth.clamp(this.scrollPos - o, 0, this.contentMaxScroll);
            return true;
        }
        return false;
    }

    private void applyScrollbarDrag(double mouseY) {
        int panelHeight = this.screenHeight - 60;
        int viewportH = panelHeight - CONTENT_TOP_OFFSET;
        int thumbTravel = viewportH - SCROLLBAR_THUMB_H;
        if (thumbTravel <= 0 || this.contentMaxScroll <= 0) {
            return;
        }
        double deltaY = mouseY - this.scrollbarDragStartMouseY;
        int deltaEff = (int) Math.round(deltaY / thumbTravel * this.contentMaxScroll);
        int newEff = Mth.clamp(this.scrollbarDragStartEffScroll + deltaEff, 0, this.contentMaxScroll);
        int o = SCROLL_OVERSCROLL_PX;
        this.scrollPos = newEff + o;
    }

    private final LinkedList<RenderedBadge> renderedBadges = new LinkedList<>();

    private record RenderedBadge(ResourceLocation powerId, Badge badge, int x, int y) {}

    public static void renderIcon(GuiGraphics graphics, Origin origin, int x, int y) {
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

    public void resetScroll() {
        this.scrollPos = SCROLL_OVERSCROLL_PX;
        this.contentMaxScroll = 0;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return button == 0 && tryBeginScrollbarDrag(mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
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
        int o = SCROLL_OVERSCROLL_PX;
        int virtualMax = this.contentMaxScroll <= 0 ? 0 : o + this.contentMaxScroll + o;
        if (this.displayed == null || virtualMax <= 0) {
            return false;
        }
        boolean canScroll = scrollY > 0
                ? this.scrollPos > 0
                : scrollY < 0 && this.scrollPos < virtualMax;
        if (!canScroll) {
            return false;
        }
        this.scrollPos = Mth.clamp(this.scrollPos - (int) scrollY * 12, 0, virtualMax);
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
