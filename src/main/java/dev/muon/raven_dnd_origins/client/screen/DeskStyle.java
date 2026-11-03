package dev.muon.raven_dnd_origins.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.raven_dnd_origins.RavenDndOrigins;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public final class DeskStyle {

    public static final int LEATHER_DARK = 0xFF26100A;
    public static final int GILT = 0xFFE8C060;
    public static final int CREAM = 0xFFEBDDBE;
    public static final int BRASS = 0xFFB8862B;
    public static final int BRASS_LIGHT = 0xFFF0C35A;
    public static final int BRASS_DARK = 0xFF6B4A16;
    public static final int MUTED = 0xFF8A7A60;
    public static final int INK = 0xFF3A2414;
    public static final int WET_INK = 0xFF120804;
    public static final int RUBRIC = 0xFF7A2216;
    public static final int PENCIL = 0xFF6F5838;

    public static final int PAGE_INK = 0xFF3B2618;
    public static final int PAGE_BODY = 0xFF4E3A2A;
    public static final int PAGE_RUBRIC = 0xFF8E1B1B;
    public static final int PAGE_SEPIA = 0xFF6E5034;
    public static final int PAGE_TRACK = 0x668A6A48;
    public static final int PAGE_HAIRLINE = 0xA08A6A48;
    public static final int VELLUM = 0xFFF3E2B8;
    public static final int RUBRIC_LIGHT = 0xFFB23A30;
    public static final int RUBRIC_DARK = 0xFF5A0E0E;
    public static final int TOOLTIP_BG = 0xF22A140C;
    public static final int SHADOW = 0x66000000;
    public static final int SHADOW_SOFT = 0x55000000;
    public static final int SCREEN_DIM = 0x38000000;
    public static final int HOVER_WASH = 0x22FFE8C0;
    public static final int HOVER_TEXT = 0xFFFFFFFF;

    public static final ResourceLocation PANEL = sprite("panel");
    public static final ResourceLocation PAGE = sprite("page");
    public static final ResourceLocation INITIAL = sprite("initial");
    public static final ResourceLocation TAG = sprite("tag");
    public static final ResourceLocation SOCKET = sprite("socket");
    public static final ResourceLocation SOCKET_HOVER = sprite("socket_hover");
    public static final ResourceLocation SOCKET_SELECTED = sprite("socket_selected");
    public static final ResourceLocation STRAP = sprite("strap");
    /** A plain texture rather than a sprite: its rolled edges taper, so it stretches instead of nine-slicing. */
    public static final ResourceLocation SHEET = RavenDndOrigins.loc("textures/gui/character_sheet.png");
    public static final int SHEET_SIZE = 256;
    public static final ResourceLocation BUTTON = sprite("button");
    public static final ResourceLocation BUTTON_HIGHLIGHTED = sprite("button_highlighted");
    public static final ResourceLocation BUTTON_DISABLED = sprite("button_disabled");

    public static final int SEAL_SIZE = 24;
    public static final int STRAP_WIDTH = 24;

    /** Each group gets its own silhouette as well as its own wax color, so the groups don't rely on hue alone. */
    public enum SealGroup {
        RACE("seal_race", 0xE0604A),
        CLASS("seal_class", 0x6A92E8),
        CANTRIP("seal_cantrip", 0xB080F0),
        APTITUDE("seal_aptitude", 0x7ABE6A),
        FEAT("seal_feat", 0xF0B050),
        OTHER("seal_other", 0xC0B0A0);

        public final ResourceLocation sprite;
        public final int tint;

        SealGroup(String sprite, int tint) {
            this.sprite = DeskStyle.sprite(sprite);
            this.tint = tint;
        }

        public static SealGroup of(ResourceLocation layerId) {
            String path = layerId.getPath();
            if (path.contains("race")) return RACE;
            if (path.contains("class") || path.equals("draconic_ancestry") || path.equals("wildshape")
                    || path.equals("chemical_mastery")) return CLASS;
            if (path.contains("cantrip") || path.contains("elemental") || path.contains("magical_secrets")) return CANTRIP;
            if (path.contains("aptitude")) return APTITUDE;
            if (path.contains("feat")) return FEAT;
            return OTHER;
        }
    }

    private DeskStyle() {}

    private static ResourceLocation sprite(String name) {
        return RavenDndOrigins.loc("origin_screen/" + name);
    }

    public static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0xFFFFFF);
    }

    public static void continuationFade(GuiGraphics graphics, int x0, int x1, int bottom) {
        graphics.fillGradient(x0, bottom - 14, x1, bottom, withAlpha(VELLUM, 0x00), withAlpha(VELLUM, 0xF0));
        scrollArrow(graphics, (x0 + x1) / 2, bottom - 6, false);
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, SHADOW);
        graphics.blitSprite(PANEL, x, y, width, height);
    }

    public static void page(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, SHADOW);
        graphics.blitSprite(PAGE, x, y, width, height);
    }

    public static void diamondRule(GuiGraphics graphics, int x0, int x1, int y) {
        int cx = (x0 + x1) / 2;
        graphics.fill(x0, y, cx - 5, y + 1, PAGE_HAIRLINE);
        graphics.fill(cx + 5, y, x1, y + 1, PAGE_HAIRLINE);
        for (int dy = -2; dy <= 2; dy++) {
            int half = 2 - Math.abs(dy);
            graphics.fill(cx - half, y + dy, cx + half + 1, y + dy + 1, PAGE_RUBRIC);
        }
    }

    public static void rubricMarker(GuiGraphics graphics, int x, int y) {
        graphics.fill(x + 1, y + 2, x + 2, y + 3, PAGE_RUBRIC);
        graphics.fill(x, y + 3, x + 3, y + 4, PAGE_RUBRIC);
        graphics.fill(x + 1, y + 4, x + 2, y + 5, PAGE_RUBRIC);
    }

    public static void tag(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x + 2, y + 2, x + width + 2, y + height + 2, SHADOW_SOFT);
        graphics.blitSprite(TAG, x, y, width, height);
    }

    /** Seal sprites are grayscale so we can tint them with the group's wax color. */
    public static void seal(GuiGraphics graphics, int x, int y, SealGroup group, boolean bright) {
        seal(graphics, x, y, group, bright, SEAL_SIZE);
    }

    public static void seal(GuiGraphics graphics, int x, int y, SealGroup group, boolean bright, int size) {
        int rgb = group.tint;
        float boost = bright ? 1.2f : 1.0f;
        RenderSystem.setShaderColor(
                Math.min(1f, ((rgb >> 16) & 0xFF) / 255f * boost),
                Math.min(1f, ((rgb >> 8) & 0xFF) / 255f * boost),
                Math.min(1f, (rgb & 0xFF) / 255f * boost),
                1f);
        graphics.blitSprite(group.sprite, x, y, size, size);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Pans text that overflows {@code width} back and forth, timed like vanilla's scrolling button labels. */
    public static void scrollingString(GuiGraphics graphics, Font font, Component text, int x, int y, int width, int color) {
        int textWidth = font.width(text);
        if (textWidth <= width) {
            graphics.drawString(font, text, x, y, color, false);
            return;
        }
        int overflow = textWidth - width;
        double seconds = Util.getMillis() / 1000.0;
        double period = Math.max(overflow * 0.5, 3.0);
        double sweep = Math.sin(Math.PI / 2 * Math.cos(Math.PI * 2 * seconds / period)) / 2 + 0.5;
        int offset = (int) Mth.lerp(sweep, 0.0, overflow);
        graphics.enableScissor(x, y - 1, x + width, y + 10);
        graphics.drawString(font, text, x - offset, y, color, false);
        graphics.disableScissor();
    }

    public static void pencilRule(GuiGraphics graphics, int x0, int x1, int y) {
        for (int x = x0; x < x1; x += 3) {
            graphics.fill(x, y, x + 1, y + 1, withAlpha(PENCIL, 0xB3));
        }
    }

    public static void scrollArrow(GuiGraphics graphics, int centerX, int y, boolean up) {
        for (int row = 0; row < 4; row++) {
            int half = up ? row + 1 : 4 - row;
            graphics.fill(centerX - half, y + row, centerX + half + 1, y + row + 1, PAGE_SEPIA);
        }
    }

    public static int lerpColor(float t, int from, int to) {
        int a = (int) Mth.lerp(t, (from >>> 24) & 0xFF, (to >>> 24) & 0xFF);
        int r = (int) Mth.lerp(t, (from >> 16) & 0xFF, (to >> 16) & 0xFF);
        int g = (int) Mth.lerp(t, (from >> 8) & 0xFF, (to >> 8) & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
