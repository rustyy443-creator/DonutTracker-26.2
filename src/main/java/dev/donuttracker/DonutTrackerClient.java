package dev.donuttracker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DonutTrackerClient implements ClientModInitializer {
    private static KeyMapping openTrackerKey;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("donuttracker", "main")
    );
    private static final Pattern WIN = Pattern.compile(
            "(?i)\\b(?:you\\s+)?(?:won|win|winnings?)\\b[^0-9$]{0,24}\\$?([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");
    private static final Pattern LOSS = Pattern.compile(
            "(?i)\\b(?:you\\s+)?(?:lost|lose|loss)\\b[^0-9$]{0,24}\\$?([0-9][0-9,]*(?:\\.[0-9]{1,2})?)");

    @Override
    public void onInitializeClient() {
        TrackerStore.load();

        openTrackerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.donuttracker.open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openTrackerKey.consumeClick()) {
                if (client.player != null) {
                    client.gui.setScreen(new TrackerScreen());
                }
            }
        });

        // DonutSMP message formats vary. Only clearly worded win/loss messages
        // with a visible amount are counted; ambiguous messages are ignored.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!TrackerStore.isAutoDetectEnabled()) return;
            String line = message.getString();
            if (TrackerStore.isDuplicate(line)) return;

            Matcher win = WIN.matcher(line);
            Matcher loss = LOSS.matcher(line);
            if (win.find()) {
                TrackerStore.recordWin(parseCents(win.group(1)), "Auto: " + line);
            } else if (loss.find()) {
                TrackerStore.recordLoss(parseCents(loss.group(1)), "Auto: " + line);
            }
        });
    }

    private static long parseCents(String raw) {
        try {
            return new java.math.BigDecimal(raw.replace(",", ""))
                    .movePointRight(2).longValueExact();
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
