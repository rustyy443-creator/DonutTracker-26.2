package dev.donuttracker;

import net.minecraft.client.gui.GuiGraphics;
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
        amount = new EditBox(this.font, center - 90, 92, 180, 20, Component.literal("Amount"));
        amount.setValue("100");
        amount.setHint(Component.literal("Amount (e.g. 250 or 1,250.50)"));
        addRenderableWidget(amount);

        addRenderableWidget(Button.builder(Component.literal("+ WIN"), b -> record(true))
                .bounds(center - 92, 120, 88, 22).build());
        addRenderableWidget(Button.builder(Component.literal("- LOSS"), b -> record(false))
                .bounds(center + 4, 120, 88, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Auto-detect: " + (TrackerStore.isAutoDetectEnabled() ? "ON" : "OFF")),
                b -> {
                    TrackerStore.toggleAutoDetect();
                    clearWidgets();
                    init();
                }).bounds(center - 92, 151, 184, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Reset stats"), b -> {
            TrackerStore.reset();
            status = "Statistics reset.";
        }).bounds(center - 92, 179, 88, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(center + 4, 179, 88, 20).build());
    }

    private void record(boolean win) {
        try {
            long cents = new BigDecimal(amount.getValue().replace(",", ""))
                    .movePointRight(2).longValueExact();
            if (cents <= 0) throw new NumberFormatException();
            if (win) TrackerStore.recordWin(cents, "Manual");
            else TrackerStore.recordLoss(cents, "Manual");
            status = (win ? "Win" : "Loss") + " recorded: $" + TrackerStore.money(cents);
        } catch (Exception e) {
            status = "Enter a valid positive amount, e.g. 125 or 1,250.50.";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);

        int c = this.width / 2;
        graphics.drawCenteredString(font, Component.literal("DONUT TRACKER"), c, 24, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal("Your gambling stats — stored on this PC"), c, 42, 0xAAAAAA);

        graphics.drawString(font, Component.literal("Total won:    $" + TrackerStore.money(TrackerStore.getWon())), c - 105, 62, 0x55FF55);
        graphics.drawString(font, Component.literal("Total lost:   $" + TrackerStore.money(TrackerStore.getLost())), c + 8, 62, 0xFF7777);
        long profit = TrackerStore.getProfit();
        int profitColor = profit >= 0 ? 0x55FF55 : 0xFF7777;
        graphics.drawCenteredString(font, Component.literal("NET: " + (profit >= 0 ? "+$" : "-$") + TrackerStore.money(Math.abs(profit))), c, 78, profitColor);

        graphics.drawCenteredString(font, Component.literal("Wins: " + TrackerStore.getWins() + "     Losses: " + TrackerStore.getLosses()), c, 210, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.literal(status), c, 230, 0xFFFF55);

        graphics.drawString(font, Component.literal("Recent activity"), c - 105, 252, 0xFFFFFF);
        int y = 267;
        for (String item : TrackerStore.getHistory()) {
            String shortItem = item.length() > 62 ? item.substring(0, 59) + "..." : item;
            graphics.drawString(font, Component.literal(shortItem), c - 105, y, 0xBBBBBB);
            y += 12;
            if (y > this.height - 12) break;
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(null);
    }
}
