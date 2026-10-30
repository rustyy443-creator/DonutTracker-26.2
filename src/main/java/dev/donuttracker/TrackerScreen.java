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
        int center = this.width / 2;
        amount = new EditBox(this.font, center - 90, 112, 180, 20, Component.literal("Amount"));
        amount.setValue("100");
        amount.setHint(Component.literal("Amount (e.g. 250 or 1,250.50)"));
        addRenderableWidget(amount);

        addRenderableWidget(Button.builder(Component.literal("+ WIN / PAID TO YOU"), b -> record(true))
                .bounds(center - 92, 140, 184, 22).build());
        addRenderableWidget(Button.builder(Component.literal("- LOSS / YOU PAID"), b -> record(false))
                .bounds(center - 92, 168, 184, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Auto-detect: " + (TrackerStore.isAutoDetectEnabled() ? "ON" : "OFF")),
                b -> {
                    TrackerStore.toggleAutoDetect();
                    clearWidgets();
                    init();
                }).bounds(center - 92, 198, 184, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Reset stats"), b -> {
            TrackerStore.reset();
            status = "Balance reset to $0.";
        }).bounds(center - 92, 226, 88, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(center + 4, 226, 88, 20).build());
    }

    private void record(boolean win) {
        try {
            long cents = new BigDecimal(amount.getValue().replace(",", ""))
                    .movePointRight(2).longValueExact();
            if (cents <= 0) throw new NumberFormatException();
            if (win) TrackerStore.recordWin(cents, "Manual");
            else TrackerStore.recordLoss(cents, "Manual");
            long balance = TrackerStore.getProfit();
            status = (win ? "Added to balance: +$" : "Subtracted from balance: -$")
                    + TrackerStore.money(cents) + " | New balance: "
                    + (balance < 0 ? "-$" : "+$") + TrackerStore.money(Math.abs(balance));
        } catch (Exception e) {
            status = "Enter a valid positive amount, e.g. 125 or 1,250.50.";
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int c = this.width / 2;
        graphics.text(font, "DONUT TRACKER", c - font.width("DONUT TRACKER") / 2, 24, 0xFFFFFFFF, true);
        graphics.text(font, "Your gambling balance — stored on this PC", c - font.width("Your gambling balance — stored on this PC") / 2, 43, 0xFFAAAAAA, true);

        long balance = TrackerStore.getProfit();
        String total = "TOTAL WIN: " + (balance < 0 ? "-$" : "+$") + TrackerStore.money(Math.abs(balance));
        int balanceColor = balance >= 0 ? 0xFF55FF55 : 0xFFFF7777;
        graphics.text(font, total, c - font.width(total) / 2, 76, balanceColor, true);

        String counts = "Wins: " + TrackerStore.getWins() + "     Losses: " + TrackerStore.getLosses();
        graphics.text(font, counts, c - font.width(counts) / 2, 258, 0xFFFFFFFF, true);
        graphics.text(font, status, c - font.width(status) / 2, 278, 0xFFFFFF55, true);

        graphics.text(font, "Recent activity", c - 105, 300, 0xFFFFFFFF, true);
        int y = 315;
        for (String item : TrackerStore.getHistory()) {
            String shortItem = item.length() > 62 ? item.substring(0, 59) + "..." : item;
            graphics.text(font, shortItem, c - 105, y, 0xFFBBBBBB, true);
            y += 12;
            if (y > this.height - 12) break;
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }
}
