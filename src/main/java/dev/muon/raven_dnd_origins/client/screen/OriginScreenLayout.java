package dev.muon.raven_dnd_origins.client.screen;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;

/**
 * Splits the screen into the pick rail, the character sheet, and the detail panel. At Auto GUI scale
 * vanilla keeps the GUI between 320x240 and roughly 480x270, so the narrow sizes are the common case:
 * the sheet gets whatever the rail and panel leave over, and drops out entirely below {@link #MIN_SHEET_WIDTH}.
 */
public record OriginScreenLayout(Rect rail, @Nullable Rect sheet, Rect panel, @Nullable Rect button) {

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }

    public static final int BUTTON_HEIGHT = 20;
    private static final int MIN_SHEET_WIDTH = 150;
    private static final int MAX_SHEET_WIDTH = 256;
    private static final int MAX_SHEET_HEIGHT = 256;
    /** Past this the columns would drift to the screen edges, so large GUIs get a centered spread instead. */
    private static final int MAX_FRAME_WIDTH = 720;
    private static final int MAX_FRAME_HEIGHT = 420;

    public static OriginScreenLayout compute(int screenWidth, int screenHeight, boolean withButton) {
        int width = Math.min(screenWidth, MAX_FRAME_WIDTH);
        int height = Math.min(screenHeight, MAX_FRAME_HEIGHT);
        return computeFrame(width, height, withButton).offset((screenWidth - width) / 2, (screenHeight - height) / 2);
    }

    private OriginScreenLayout offset(int dx, int dy) {
        return new OriginScreenLayout(shift(rail, dx, dy), shift(sheet, dx, dy), shift(panel, dx, dy), shift(button, dx, dy));
    }

    @Nullable
    private static Rect shift(@Nullable Rect rect, int dx, int dy) {
        return rect == null ? null : new Rect(rect.x() + dx, rect.y() + dy, rect.width(), rect.height());
    }

    private static OriginScreenLayout computeFrame(int width, int height, boolean withButton) {
        int margin = width < 480 || height < 260 ? 4 : 8;
        int panelW = Mth.clamp(Math.round(width * 0.30f), 132, 200);
        int railW = Mth.clamp(Math.round(width * 0.27f), 120, 180);
        int sheetW = width - 2 * margin - panelW - railW - 2 * margin;

        Rect sheet = null;
        if (sheetW >= MIN_SHEET_WIDTH) {
            int spare = Math.max(0, sheetW - MAX_SHEET_WIDTH);
            int railGrow = Math.min(spare / 2, 200 - railW);
            int panelGrow = Math.min(spare - railGrow, 220 - panelW);
            railW += railGrow;
            panelW += panelGrow;
            sheetW = Math.min(MAX_SHEET_WIDTH, sheetW - railGrow - panelGrow);
            int columnX = margin + railW + margin;
            int columnW = width - panelW - margin - margin - columnX;
            int sheetH = Math.min(height - 2 * margin, MAX_SHEET_HEIGHT);
            sheet = new Rect(columnX + (columnW - sheetW) / 2, (height - sheetH) / 2, sheetW, sheetH);
        } else {
            railW = width - 3 * margin - panelW;
        }

        Rect rail = new Rect(margin, margin, railW, height - 2 * margin);
        int panelX = width - margin - panelW;
        // The page and button share the scroll's top and bottom edges, so tall GUIs don't strand a long page beside it.
        int columnTop = sheet != null ? sheet.y() : margin;
        int columnBottom = sheet != null ? sheet.bottom() : height - margin;
        int panelH = columnBottom - columnTop - (withButton ? BUTTON_HEIGHT + 4 : 0);
        Rect panel = new Rect(panelX, columnTop, panelW, panelH);
        Rect button = withButton ? new Rect(panelX, columnBottom - BUTTON_HEIGHT, panelW, BUTTON_HEIGHT) : null;
        return new OriginScreenLayout(rail, sheet, panel, button);
    }
}
