package dev.donuttracker;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class TrackerScreen extends Screen {
    public TrackerScreen() {
        super(Component.literal("Donut Tracker"));
    }

    @Override
    protected void init() {
        int c = this.width / 2;
        int top = Math.max(18, (this.height - 300) / 2);
        addRenderableWidget(Button.builder(Component.literal("CLOSE"), b -> onClose())
                .bounds(c - 48, top + 266, 96, 22).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int c = this.width / 2;
        int top = Math.max(18, (this.height - 300) / 2);
        int left = c - 150;
        int bottom = top + 296;

        graphics.fill(left - 2, top - 2, c + 152, bottom + 2, 0x6627C7B8);
        graphics.fill(left, top, c + 150, bottom, 0xE5101828);
        graphics.fill(left + 1, top + 1, c + 149, bottom - 1, 0xAA172638);
        graphics.outline(left, top, 300, 296, 0xAA78E8DC);
        graphics.fill(left + 18, top + 10, c - 18, top + 12, 0xFF63E6D4);
        graphics.fill(c + 18, top + 10, c + 132, top + 12, 0x8863E6D4);

        String title = "DONUT TRACKER";
        graphics.text(font, title, c - font.width(title) / 2, top + 22, 0xFFFFFFFF, true);
        String subtitle = "LIVE PAYMENT TRACKER  •  AUTO MODE";
        graphics.text(font, subtitle, c - font.width(subtitle) / 2, top + 42, 0xFF9EB8C8, false);

        graphics.fill(c - 128, top + 62, c + 128, top + 122, 0x88364D60);
        graphics.outline(c - 128, top + 62, 256, 60, 0x887DE7D7);
        String label = "TOTAL WIN";
        graphics.text(font, label, c - font.width(label) / 2, top + 70, 0xFFB7D5D8, false);

        long balance = TrackerStore.getProfit();
        String total = (balance < 0 ? "-$" : "+$") + TrackerStore.money(Math.abs(balance));
        int color = balance >= 0 ? 0xFF6CFFC6 : 0xFFFF858F;
        graphics.text(font, total, c - font.width(total) / 2, top + 89, color, true);

        String counts = "MONEY IN  " + TrackerStore.getWins() + "     MONEY OUT  " + TrackerStore.getLosses();
        graphics.text(font, counts, c - font.width(counts) / 2, top + 132, 0xFFD6E5ED, false);

        String live = "●  AUTOMATIC CHAT DETECTION ACTIVE";
        graphics.text(font, live, c - font.width(live) / 2, top + 157, 0xFF6CFFC6, false);

        String recent = "RECENT PAYMENTS";
        graphics.text(font, recent, c - font.width(recent) / 2, top + 184, 0xFF9EB8C8, false);
        List<String> history = TrackerStore.getHistory();
        if (history.isEmpty()) {
            String empty = "Waiting for a DonutSMP payment message...";
            graphics.text(font, empty, c - font.width(empty) / 2, top + 210, 0xFFB8C8D4, false);
        } else {
            int y = top + 207;
            for (int i = 0; i < Math.min(4, history.size()); i++) {
                String entry = history.get(i);
                if (entry.length() > 43) entry = entry.substring(0, 40) + "...";
                graphics.text(font, entry, c - font.width(entry) / 2, y, 0xFFD6E5ED, false);
                y += 14;
            }
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.gui.setScreen(null);
    }
}
