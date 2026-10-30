package dev.donuttracker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TrackerScreen extends Screen {
    private static final String[] STYLE_NAMES = {
            "NEON GLASS", "MIDNIGHT", "PURPLE HAZE",
            "CRIMSON", "OCEAN", "GOLD"
    };
    private static final int[] ACCENTS = {
            0xFF63E6D4, 0xFF8CA8FF, 0xFFC18BFF,
            0xFFFF7888, 0xFF62BFFF, 0xFFFFD16A
    };
    private static final int[] PANELS = {
            0xE5101828, 0xE5101425, 0xE51B122B,
            0xE51F101B, 0xE5101D2B, 0xE51F1A10
    };
    private static int selectedStyle = 0;
    private static double opacity = 0.68;
    private boolean styleTab = false;
    private boolean confirmReset = false;
    private String resetStatus = "";

    public TrackerScreen() {
        super(Component.literal("Donut Tracker"));
    }

    @Override
    protected void init() {
        clearWidgets();
        int c = this.width / 2;
        int top = Math.max(12, (this.height - 330) / 2);

        addRenderableWidget(Button.builder(Component.literal(styleTab ? "TRACKER" : "TRACKER"),
                b -> switchTab(false)).bounds(c - 112, top + 48, 108, 24).build());
        addRenderableWidget(Button.builder(Component.literal(styleTab ? "UI STYLE" : "UI STYLE"),
                b -> switchTab(true)).bounds(c + 4, top + 48, 108, 24).build());

        if (styleTab) {
            for (int i = 0; i < STYLE_NAMES.length; i++) {
                final int style = i;
                int col = i % 2;
                int row = i / 2;
                int x = c - 112 + col * 116;
                int y = top + 96 + row * 40;
                String label = (selectedStyle == i ? "✓  " : "") + STYLE_NAMES[i];
                addRenderableWidget(Button.builder(Component.literal(label), b -> {
                    selectedStyle = style;
                    init();
                }).bounds(x, y, 108, 28).build());
            }

            addRenderableWidget(new OpacitySlider(c - 112, top + 224, 224, 20));
        } else {
            addRenderableWidget(Button.builder(
                    Component.literal(confirmReset ? "CONFIRM RESET?" : "RESET SESSION"),
                    b -> {
                        if (confirmReset) {
                            TrackerStore.reset();
                            resetStatus = "SESSION RESET TO $0";
                            confirmReset = false;
                        } else {
                            confirmReset = true;
                            resetStatus = "Click again to clear this session";
                        }
                        init();
                    }).bounds(c - 112, top + 294, 108, 24).build());
            addRenderableWidget(Button.builder(Component.literal("CLOSE"), b -> onClose())
                    .bounds(c + 4, top + 294, 108, 24).build());
        }
    }

    private static final class OpacitySlider extends AbstractSliderButton {
        OpacitySlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), opacity);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal("PANEL TRANSPARENCY  " + Math.round(opacity * 100) + "%"));
        }

        @Override
        protected void applyValue() {
            opacity = value;
        }
    }

    private void switchTab(boolean styles) {
        styleTab = styles;
        init();
    }

    private static int alpha(int color, double multiplier) {
        int original = (color >>> 24) & 0xFF;
        int adjusted = (int) Math.round(original * multiplier);
        return (Math.max(0, Math.min(255, adjusted)) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int c = this.width / 2;
        int top = Math.max(12, (this.height - 330) / 2);
        int left = c - 170;
        int accent = ACCENTS[selectedStyle];
        int panel = PANELS[selectedStyle];
        int bottom = top + 326;

        // Softer glass treatment: no heavy filled frame, just a subtle glow and fine edge.
        int pulse = (int) (Math.sin(System.currentTimeMillis() / 420.0) * 15.0);
        int glowAlpha = Math.max(0x28, Math.min(0x68, 0x48 + pulse));
        graphics.fill(left - 1, top - 1, c + 171, bottom + 1, (glowAlpha << 24) | (accent & 0x00FFFFFF));
        graphics.fill(left, top, c + 170, bottom, alpha(panel, opacity));
        graphics.outline(left, top, 340, 326, alpha(accent, 0.78));
        graphics.fill(left + 22, top + 10, c - 22, top + 12, alpha(accent, 0.9));
        graphics.fill(c + 22, top + 10, c + 148, top + 12, alpha(accent, 0.55));

        String title = "DONUT TRACKER";
        graphics.text(font, title, c - font.width(title) / 2, top + 20, 0xFFFFFFFF, true);
        String subtitle = styleTab ? "MAKE YOUR TRACKER YOUR OWN" : "LIVE PAYMENT TRACKER  •  AUTO MODE";
        graphics.text(font, subtitle, c - font.width(subtitle) / 2, top + 36, 0xFFB5C9D7, false);

        if (styleTab) {
            String heading = "CHOOSE A UI STYLE";
            graphics.text(font, heading, c - font.width(heading) / 2, top + 80, accent, true);
            String hint = "DRAG TO ADJUST THE GLASS OPACITY";
            graphics.text(font, hint, c - font.width(hint) / 2, top + 254, 0xFFB5C9D7, false);
            String current = "ACTIVE: " + STYLE_NAMES[selectedStyle];
            graphics.text(font, current, c - font.width(current) / 2, top + 274, accent, true);
        } else {
            graphics.fill(c - 128, top + 82, c + 128, top + 142, alpha(0x88364D60, opacity));
            graphics.outline(c - 128, top + 82, 256, 60, alpha(accent, 0.8));
            String label = "TOTAL WIN";
            graphics.text(font, label, c - font.width(label) / 2, top + 90, 0xFFB7D5D8, false);

            long balance = TrackerStore.getProfit();
            String total = (balance < 0 ? "-$" : "+$") + TrackerStore.money(Math.abs(balance));
            int balanceColor = balance >= 0 ? 0xFF6CFFC6 : 0xFFFF858F;
            graphics.text(font, total, c - font.width(total) / 2, top + 109, balanceColor, true);

            String counts = "MONEY IN  " + TrackerStore.getWins() + "     MONEY OUT  " + TrackerStore.getLosses();
            graphics.text(font, counts, c - font.width(counts) / 2, top + 151, 0xFFD6E5ED, false);
            String live = "●  AUTOMATIC CHAT DETECTION ACTIVE";
            graphics.text(font, live, c - font.width(live) / 2, top + 174, 0xFF6CFFC6, false);

            if (!resetStatus.isEmpty()) {
                graphics.text(font, resetStatus, c - font.width(resetStatus) / 2, top + 280, 0xFFFFD16A, false);
            }

            String recent = "RECENT PAYMENTS";
            graphics.text(font, recent, c - font.width(recent) / 2, top + 198, accent, false);
            List<String> history = TrackerStore.getHistory();
            if (history.isEmpty()) {
                String empty = "Waiting for a DonutSMP payment message...";
                graphics.text(font, empty, c - font.width(empty) / 2, top + 224, 0xFFB8C8D4, false);
            } else {
                int y = top + 221;
                for (int i = 0; i < Math.min(4, history.size()); i++) {
                    String entry = history.get(i);
                    if (entry.length() > 48) entry = entry.substring(0, 45) + "...";
                    graphics.text(font, entry, c - font.width(entry) / 2, y, 0xFFD6E5ED, false);
                    y += 15;
                }
            }
        }

        // Draw widgets last so tabs, theme buttons and the slider stay bright and clickable.
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }
}
