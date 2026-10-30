package dev.donuttracker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TrackerScreen extends Screen {
    private static final String[] NAV = {"Overview", "Payments", "Sessions", "Analytics", "Settings"};
    private static final String[] THEMES = {"Neon Glass", "Midnight", "Purple Haze", "Crimson", "Ocean", "Gold"};
    private static final int[] ACCENTS = {0xFFFF79B8, 0xFFFF83B5, 0xFFC18BFF, 0xFFFF7888, 0xFF62BFFF, 0xFFFFD16A};
    private static final int[] PANELS = {0xE5101828, 0xE50D1421, 0xE51B122B, 0xE51F101B, 0xE5101D2B, 0xE51F1A10};
    private static final Pattern AMOUNT = Pattern.compile("([+-])\\$([0-9,]+(?:\\.[0-9]{2})?)");
    private static int selectedTheme = 0;
    private static double opacity = 0.62;
    private static boolean blurEnabled = false;
    private static boolean animationsEnabled = true;
    private static boolean compactMode = false;
    private int page = 0;
    private boolean draggingOpacity = false;
    private boolean confirmReset = false;
    private String notice = "";
    private long pageOpenedAt = System.currentTimeMillis();

    public TrackerScreen() { super(Component.literal("Donut Tracker")); }

    private int accent() { return ACCENTS[selectedTheme]; }
    private int panel() { return withAlpha(PANELS[selectedTheme], opacity); }
    private static int withAlpha(int color, double factor) {
        int a = (color >>> 24) & 255;
        return (Math.max(0, Math.min(255, (int)(a * factor))) << 24) | (color & 0x00FFFFFF);
    }
    private static int rgbAlpha(int color, int a) { return ((a & 255) << 24) | (color & 0x00FFFFFF); }

    private int left() { return Math.max(12, (width - Math.min(width - 24, 1120)) / 2); }
    private int top() { return Math.max(12, (height - Math.min(height - 24, 700)) / 2); }
    private int boardW() { return Math.min(width - 24, 1120); }
    private int boardH() { return Math.min(height - 24, 700); }
    private int sideW() { return 158; }
    private int mainX() { return left() + sideW() + 22; }
    private int mainW() { return boardW() - sideW() - 44; }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        super.extractRenderState(g, mouseX, mouseY, delta);
        int x = left(), y = top(), w = boardW(), h = boardH();
        int a = accent();
        // World remains visible; only the dashboard surfaces receive translucent fills.
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x55300020);
        g.fill(x, y, x + w, y + h, withAlpha(0xE908101B, opacity));
        g.fill(x, y, x + sideW(), y + h, withAlpha(0xF0080D17, Math.min(1.0, opacity + 0.22)));
        g.fill(x + sideW(), y, x + sideW() + 1, y + h, 0x333C4658);

        drawText(g, "DONUT", x + 20, y + 22, 0xFFFFFFFF, true);
        drawText(g, "TRACKER", x + 20, y + 39, a, true);
        drawText(g, "PROFIT TRACKER", x + 20, y + 59, 0xFF8493A7, false);
        g.fill(x + 18, y + 79, x + 42, y + 81, a);

        for (int i = 0; i < NAV.length; i++) {
            int ny = y + 108 + i * 39;
            boolean selected = page == i;
            drawButton(g, x + 12, ny, sideW() - 24, 30, NAV[i], selected, mouseX, mouseY, selected);
        }
        int statusY = y + h - 58;
        g.fill(x + 18, statusY - 12, x + sideW() - 18, statusY - 11, 0x333C4658);
        drawText(g, "●", x + 20, statusY, 0xFF61E6A8, true);
        drawText(g, "ACTIVE SESSION", x + 34, statusY, 0xFFB7C5D4, false);
        drawText(g, "LOCAL TRACKING", x + 20, statusY + 17, 0xFF718096, false);

        switch (page) {
            case 0 -> drawOverview(g, mouseX, mouseY);
            case 1 -> drawPayments(g);
            case 2 -> drawSessions(g);
            case 3 -> drawAnalytics(g);
            case 4 -> drawSettings(g, mouseX, mouseY);
            default -> drawOverview(g, mouseX, mouseY);
        }
    }

    private void drawOverview(GuiGraphicsExtractor g, int mx, int my) {
        int x = mainX(), y = top() + 24, w = mainW(), a = accent();
        drawText(g, "Overview", x, y, 0xFFF5F7FB, true);
        drawText(g, "Live DonutSMP payment tracking", x, y + 22, 0xFF8E9BAD, false);

        int gap = 12, cardW = (w - gap * 3) / 4, cy = y + 54;
        long profit = TrackerStore.getProfit(), won = TrackerStore.getWon(), lost = TrackerStore.getLost();
        drawStat(g, x, cy, cardW, "TOTAL PROFIT", signedMoney(profit), "Current session", profit >= 0 ? 0xFF62E6A8 : 0xFFFF7890);
        drawStat(g, x + cardW + gap, cy, cardW, "MONEY IN", "$" + TrackerStore.money(won), "Received", 0xFF62E6A8);
        drawStat(g, x + (cardW + gap) * 2, cy, cardW, "MONEY OUT", "$" + TrackerStore.money(lost), "Spent", 0xFFFF8299);
        drawStat(g, x + (cardW + gap) * 3, cy, cardW, "PAYMENTS", Integer.toString(TrackerStore.getWins() + TrackerStore.getLosses()), "Current session", 0xFFF2F5FA);

        drawText(g, "●", x, cy + 100, 0xFF62E6A8, true);
        drawText(g, "AUTOMATIC CHAT DETECTION ACTIVE", x + 13, cy + 100, 0xFFB8C6D5, false);

        int graphY = cy + 124;
        int graphH = Math.max(100, (top() + boardH() - graphY - 180) / 2);
        drawPanel(g, x, graphY, w, graphH, 0.82);
        drawText(g, "PROFIT OVER TIME", x + 14, graphY + 13, 0xFFC5D0DE, true);
        drawGraph(g, x + 18, graphY + 39, w - 36, graphH - 54);

        int recentY = graphY + graphH + 12;
        drawPanel(g, x, recentY, w, Math.max(76, top() + boardH() - recentY - 16), 0.82);
        drawText(g, "RECENT PAYMENTS", x + 14, recentY + 12, 0xFFC5D0DE, true);
        drawRecent(g, x + 14, recentY + 34, w - 28, 3);
        drawButton(g, x + w - 124, top() + boardH() - 43, 108, 26, confirmReset ? "CONFIRM RESET" : "RESET SESSION", confirmReset, mx, my, false);
        drawButton(g, x + w - 244, top() + boardH() - 43, 108, 26, "CLOSE", false, mx, my, false);
        if (!notice.isEmpty()) drawText(g, notice, x, top() + boardH() - 38, 0xFFFFD16A, false);
    }

    private void drawPayments(GuiGraphicsExtractor g) {
        int x = mainX(), y = top() + 24, w = mainW();
        drawText(g, "Payments", x, y, 0xFFF5F7FB, true);
        drawText(g, "Your latest detected DonutSMP transactions", x, y + 22, 0xFF8E9BAD, false);
        drawPanel(g, x, y + 54, w, boardH() - 94, 0.84);
        drawRecent(g, x + 16, y + 70, w - 32, 30);
    }

    private void drawSessions(GuiGraphicsExtractor g) {
        int x = mainX(), y = top() + 24;
        drawText(g, "Sessions", x, y, 0xFFF5F7FB, true);
        drawText(g, "Current local tracking session", x, y + 22, 0xFF8E9BAD, false);
        drawPanel(g, x, y + 58, mainW(), 132, 0.84);
        drawText(g, "ACTIVE SESSION", x + 18, y + 78, 0xFF8E9BAD, false);
        drawText(g, signedMoney(TrackerStore.getProfit()), x + 18, y + 106, TrackerStore.getProfit() >= 0 ? 0xFF62E6A8 : 0xFFFF8299, true);
        drawText(g, (TrackerStore.getWins() + TrackerStore.getLosses()) + " payments recorded this session", x + 18, y + 143, 0xFFC5D0DE, false);
        drawText(g, "Use RESET SESSION on Overview when you start a new gambling session.", x, y + 210, 0xFF8E9BAD, false);
    }

    private void drawAnalytics(GuiGraphicsExtractor g) {
        int x = mainX(), y = top() + 24, w = mainW();
        drawText(g, "Analytics", x, y, 0xFFF5F7FB, true);
        drawText(g, "A quick breakdown of this session", x, y + 22, 0xFF8E9BAD, false);
        int cw = (w - 24) / 2;
        drawStat(g, x, y + 58, cw, "TOTAL RECEIVED", "$" + TrackerStore.money(TrackerStore.getWon()), "Incoming payments", 0xFF62E6A8);
        drawStat(g, x + cw + 24, y + 58, cw, "TOTAL SENT", "$" + TrackerStore.money(TrackerStore.getLost()), "Outgoing payments", 0xFFFF8299);
        drawPanel(g, x, y + 174, w, 190, 0.82);
        drawText(g, "PROFIT TREND", x + 16, y + 188, 0xFFC5D0DE, true);
        drawGraph(g, x + 18, y + 218, w - 36, 128);
    }

    private void drawSettings(GuiGraphicsExtractor g, int mx, int my) {
        int x = mainX(), y = top() + 24, w = mainW(), a = accent();
        drawText(g, "Settings", x, y, 0xFFF5F7FB, true);
        drawText(g, "Customize your Donut Tracker", x, y + 22, 0xFF8E9BAD, false);
        drawText(g, "APPEARANCE", x, y + 58, 0xFF8E9BAD, true);
        int gap = 10, cw = (w - gap * 2) / 3;
        for (int i = 0; i < THEMES.length; i++) {
            int tx = x + (i % 3) * (cw + gap), ty = y + 82 + (i / 3) * 48;
            boolean chosen = selectedTheme == i;
            drawPanel(g, tx, ty, cw, 38, chosen ? 0.95 : 0.62);
            g.fill(tx + 10, ty + 12, tx + 20, ty + 22, ACCENTS[i]);
            drawText(g, (chosen ? "✓ " : "") + THEMES[i], tx + 28, ty + 14, chosen ? a : 0xFFD0D8E3, false);
            if (chosen) g.fill(tx + 8, ty + 35, tx + cw - 8, ty + 37, a);
        }
        int sliderY = y + 194;
        drawText(g, "Panel Transparency", x, sliderY, 0xFFE1E7EF, false);
        String pct = Math.round(opacity * 100) + "%";
        drawText(g, pct, x + w - font.width(pct), sliderY, a, false);
        int sx = x, sy = sliderY + 22, sw = w;
        g.fill(sx, sy, sx + sw, sy + 4, 0xFF303A49);
        g.fill(sx, sy, sx + (int)(sw * opacity), sy + 4, a);
        g.fill(sx + (int)(sw * opacity) - 5, sy - 5, sx + (int)(sw * opacity) + 5, sy + 9, a);
        drawText(g, "Adjust the opacity of dashboard panels.", x, sy + 18, 0xFF8492A5, false);

        int ty = sy + 52;
        drawToggle(g, x, ty, w, "Background Blur", blurEnabled, mx, my);
        drawToggle(g, x, ty + 38, w, "Animations", animationsEnabled, mx, my);
        drawToggle(g, x, ty + 76, w, "Compact Mode", compactMode, mx, my);
    }

    private void drawStat(GuiGraphicsExtractor g, int x, int y, int w, String label, String value, String desc, int valueColor) {
        drawPanel(g, x, y, w, 82, 0.88);
        drawText(g, label, x + 12, y + 12, 0xFF8796AA, true);
        drawText(g, value, x + 12, y + 34, valueColor, true);
        drawText(g, desc, x + 12, y + 62, 0xFF8492A5, false);
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, double factor) {
        g.fill(x, y, x + w, y + h, withAlpha(panel(), factor));
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, 0x223D4B5D);
    }

    private void drawButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label,
                            boolean selected, int mx, int my, boolean nav) {
        boolean hover = inside(mx, my, x, y, w, h);
        if (selected) g.fill(x, y, x + w, y + h, withAlpha(0xFF263040, 0.72));
        else if (hover) g.fill(x, y, x + w, y + h, 0x553E4A5D);
        if (selected) g.fill(x + 2, y + h - 2, x + w - 2, y + h, accent());
        int color = selected ? accent() : (hover ? 0xFFFFFFFF : 0xFFB9C4D2);
        drawText(g, label, x + (nav ? 12 : (w - font.width(label)) / 2), y + (h - 8) / 2, color, selected);
    }

    private void drawToggle(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean on, int mx, int my) {
        drawText(g, label, x, y + 5, 0xFFD5DDE7, false);
        int bx = x + w - 42, by = y, bw = 34, bh = 18;
        g.fill(bx, by, bx + bw, by + bh, on ? withAlpha(accent(), 0.85) : 0xFF343D4A);
        int knobX = on ? bx + 20 : bx + 2;
        g.fill(knobX, by + 2, knobX + 12, by + 16, 0xFFF4F6FA);
    }

    private void drawRecent(GuiGraphicsExtractor g, int x, int y, int w, int limit) {
        List<String> history = TrackerStore.getHistory();
        if (history.isEmpty()) {
            drawText(g, "No payments detected yet.", x, y + 12, 0xFFD2DAE4, false);
            drawText(g, "Waiting for a DonutSMP payment message...", x, y + 30, 0xFF8492A5, false);
            return;
        }
        int shown = Math.min(limit, history.size());
        for (int i = 0; i < shown; i++) {
            String entry = history.get(i);
            boolean incoming = entry.startsWith("WIN");
            int color = incoming ? 0xFF62E6A8 : 0xFFFF8299;
            drawText(g, entry, x, y + i * 25, color, false);
            if (i < shown - 1) g.fill(x, y + i * 25 + 18, x + w, y + i * 25 + 19, 0x223D4B5D);
        }
    }

    private void drawGraph(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        if (w < 20 || h < 20) return;
        for (int i = 1; i <= 3; i++) {
            int gy = y + h * i / 4;
            g.fill(x, gy, x + w, gy + 1, 0x223D4B5D);
        }
        List<String> history = TrackerStore.getHistory();
        if (history.size() < 2) {
            drawText(g, "Waiting for session data...", x + 10, y + h / 2 - 4, 0xFF8492A5, false);
            return;
        }
        int count = Math.min(8, history.size());
        long[] values = new long[count];
        long running = 0;
        for (int i = count - 1, j = 0; i >= 0; i--, j++) {
            String item = history.get(i);
            Matcher m = AMOUNT.matcher(item);
            if (m.find()) {
                long cents = 0;
                try { cents = new java.math.BigDecimal(m.group(2).replace(",", "")).movePointRight(2).longValue(); }
                catch (Exception ignored) {}
                running += m.group(1).equals("+") ? cents : -cents;
            }
            values[j] = running;
        }
        long min = values[0], max = values[0];
        for (long v : values) { min = Math.min(min, v); max = Math.max(max, v); }
        if (max == min) { max += 100; min -= 100; }
        for (int i = 1; i < count; i++) {
            int x1 = x + (w * (i - 1)) / (count - 1);
            int x2 = x + (w * i) / (count - 1);
            int y1 = y + h - (int)((values[i - 1] - min) * (h - 12) / (double)(max - min)) - 6;
            int y2 = y + h - (int)((values[i] - min) * (h - 12) / (double)(max - min)) - 6;
            drawLine(g, x1, y1, x2, y2, accent());
            g.fill(x2 - 2, y2 - 2, x2 + 3, y2 + 3, accent());
        }
    }

    private void drawLine(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0 : i / (double)steps;
            int px = (int)Math.round(x1 + (x2 - x1) * t);
            int py = (int)Math.round(y1 + (y2 - y1) * t);
            g.fill(px, py, px + 2, py + 2, color);
        }
    }

    private void drawText(GuiGraphicsExtractor g, String text, int x, int y, int color, boolean shadow) {
        g.text(font, text, x, y, color, shadow);
    }
    private String signedMoney(long cents) {
        return (cents < 0 ? "-$" : "+$") + compactMoney(Math.abs(cents));
    }

    private String compactMoney(long cents) {
        double amount = cents / 100.0;
        if (amount >= 1_000_000_000) return shortNumber(amount / 1_000_000_000) + "B";
        if (amount >= 1_000_000) return shortNumber(amount / 1_000_000) + "M";
        if (amount >= 1_000) return shortNumber(amount / 1_000) + "K";
        return String.format(java.util.Locale.US, "%.2f", amount);
    }

    private String shortNumber(double value) {
        return String.format(java.util.Locale.US, value >= 100 ? "%.0f" : value >= 10 ? "%.1f" : "%.2f", value)
                .replaceAll("\\.0+$", "").replaceAll("(\\\\.\\\\d)0$", "$1");
    }
    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.buttonInfo().button();
        if (button != 0) return super.mouseClicked(event, doubleClick);
        int x = left(), y = top(), w = boardW();
        int navX = x + 12, navY = y + 108;
        for (int i = 0; i < NAV.length; i++) {
            if (inside(mouseX, mouseY, navX, navY + i * 39, sideW() - 24, 30)) {
                page = i; confirmReset = false; notice = ""; pageOpenedAt = System.currentTimeMillis(); return true;
            }
        }
        if (page == 0) {
            int bx = mainX() + mainW() - 124, by = top() + boardH() - 43;
            if (inside(mouseX, mouseY, bx, by, 108, 26)) {
                if (confirmReset) { TrackerStore.reset(); notice = "SESSION RESET TO $0"; confirmReset = false; }
                else { confirmReset = true; notice = "Click RESET SESSION again to confirm"; }
                return true;
            }
            int cx = mainX() + mainW() - 244;
            if (inside(mouseX, mouseY, cx, by, 108, 26)) { onClose(); return true; }
        }
        if (page == 4) {
            int sx = mainX(), sy = top() + 24 + 194 + 22, sw = mainW();
            if (inside(mouseX, mouseY, sx - 4, sy - 8, sw + 8, 20)) {
                draggingOpacity = true; setOpacity(mouseX, sx, sw); return true;
            }
            int ty = sy + 52;
            if (inside(mouseX, mouseY, sx, ty, sw, 24)) { blurEnabled = !blurEnabled; return true; }
            if (inside(mouseX, mouseY, sx, ty + 38, sw, 24)) { animationsEnabled = !animationsEnabled; return true; }
            if (inside(mouseX, mouseY, sx, ty + 76, sw, 24)) { compactMode = !compactMode; return true; }
            int gridY = top() + 24 + 82;
            int gap = 10, cw = (sw - gap * 2) / 3;
            for (int i = 0; i < THEMES.length; i++) {
                int tx = sx + (i % 3) * (cw + gap), yy = gridY + (i / 3) * 48;
                if (inside(mouseX, mouseY, tx, yy, cw, 38)) { selectedTheme = i; return true; }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void setOpacity(double mouseX, int x, int w) {
        opacity = Math.max(0.18, Math.min(0.94, (mouseX - x) / (double)w));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingOpacity && event.buttonInfo().button() == 0) {
            setOpacity(event.x(), mainX(), mainW());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.buttonInfo().button() == 0) draggingOpacity = false;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }
}