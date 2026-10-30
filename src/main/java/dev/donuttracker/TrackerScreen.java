package dev.donuttracker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.math.BigDecimal;

public final class TrackerScreen extends Screen {
    private EditBox amount;
    private String status = "Enter an amount, then record a win or loss.";

    public TrackerScreen() {
        super(Component.literal("Donut Tracker"));
    }

    @Override
    protected void init() {
        int c = this.width / 2;
        int top = Math.max(18, (this.height - 330) / 2);

        amount = new EditBox(this.font, c - 105, top + 119, 210, 22, Component.literal("Amount"));
        amount.setValue("100");
        amount.setHint(Component.literal("Amount (e.g. 250 or 1,250.50)"));
        addRenderableWidget(amount);

        addRenderableWidget(Button.builder(Component.literal("+  MONEY IN"), b -> record(true))
                .bounds(c - 105, top + 150, 210, 24).build());
        addRenderableWidget(Button.builder(Component.literal("−  MONEY OUT"), b -> record(false))
                .bounds(c - 105, top + 180, 210, 24).build());

        addRenderableWidget(Button.builder(
                Component.literal("AUTO-DETECT  ·  " + (TrackerStore.isAutoDetectEnabled() ? "ON" : "OFF")),
                b -> {
                    TrackerStore.toggleAutoDetect();
                    clearWidgets();
                    init();
                }).bounds(c - 105, top + 212, 210, 22).build());

        addRenderableWidget(Button.builder(Component.literal("RESET"), b -> {
            TrackerStore.reset();
            status = "Balance reset to $0.";
        }).bounds(c - 105, top + 244, 100, 22).build());

        addRenderableWidget(Button.builder(Component.literal("DONE"), b -> onClose())
                .bounds(c + 5, top + 244, 100, 22).build());
    }

    private void record(boolean win) {
        try {
            long cents = new BigDecimal(amount.getValue().replace(",", ""))
                    .movePointRight(2).longValueExact();
            if (cents <= 0) throw new NumberFormatException();
            if (win) TrackerStore.recordWin(cents, "Manual");
            else TrackerStore.recordLoss(cents, "Manual");
            long balance = TrackerStore.getProfit();
            status = (win ? "+$" : "-$") + TrackerStore.money(cents) + " recorded  ·  Balance "
                    + (balance < 0 ? "-$" : "+$") + TrackerStore.money(Math.abs(balance));
        } catch (Exception e) {
            status = "Enter a valid positive amount, e.g. 125 or 1,250.50.";
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int c = this.width / 2;
        int top = Math.max(18, (this.height - 330) / 2);
        int left = c - 145;
        int panelBottom = Math.min(this.height - 12, top + 326);

        // Layered translucent panels create a soft glass-card effect.
        graphics.fill(left - 2, top - 2, c + 147, panelBottom + 2, 0x5527C7B8);
        graphics.fill(left, top, c + 145, panelBottom, 0xD9101828);
        graphics.fill(left + 1, top + 1, c + 144, panelBottom - 1, 0xAA172638);
        graphics.outline(left, top, 290, panelBottom - top, 0x8878E8DC);
        graphics.fill(left + 18, top + 10, c - 18, top + 12, 0xFF63E6D4);
        graphics.fill(c + 18, top + 10, c + 127, top + 12, 0x8863E6D4);

        String title = "DONUT TRACKER";
        graphics.text(font, title, c - font.width(title) / 2, top + 22, 0xFFFFFFFF, true);
        String subtitle = "YOUR PERSONAL DONUTSMP BALANCE";
        graphics.text(font, subtitle, c - font.width(subtitle) / 2, top + 42, 0xFF9EB8C8, false);

        // Balance glass card.
        graphics.fill(c - 122, top + 62, c + 122, top + 108, 0x88364D60);
        graphics.outline(c - 122, top + 62, 244, 46, 0x667DE7D7);
        String label = "TOTAL WIN";
        graphics.text(font, label, c - font.width(label) / 2, top + 68, 0xFFB7D5D8, false);

        long balance = TrackerStore.getProfit();
        String total = (balance < 0 ? "-$" : "+$") + TrackerStore.money(Math.abs(balance));
        int balanceColor = balance >= 0 ? 0xFF6CFFC6 : 0xFFFF858F;
        graphics.text(font, total, c - font.width(total) / 2, top + 83, balanceColor, true);

        String counts = "WINS  " + TrackerStore.getWins() + "     LOSSES  " + TrackerStore.getLosses();
        graphics.text(font, counts, c - font.width(counts) / 2, top + 276, 0xFFD6E5ED, false);
        graphics.text(font, status, c - font.width(status) / 2, top + 291, 0xFFFFE69A, false);

        String recent = "RECENT ACTIVITY";
        graphics.text(font, recent, c - font.width(recent) / 2, top + 310, 0xFF9EB8C8, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }
}
