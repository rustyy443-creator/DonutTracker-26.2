package dev.donuttracker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DonutTrackerClient implements ClientModInitializer {
    private static KeyMapping openTrackerKey;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("donuttracker", "main")
    );

    // Match the amount after "paid you", regardless of payer name or spaces after the currency symbol.
    private static final Pattern INCOMING = Pattern.compile(
            "(?i)\\bpaid\\s+you\\s+\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?\\s*[kmb]?)\\b"
    );
    private static final Pattern OUTGOING = Pattern.compile(
            "(?i)\\byou\\s+paid\\b.*?\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?\\s*[kmb]?)\\b"
    );

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
                if (client.player != null) client.gui.setScreen(new TrackerScreen());
            }
        });

        // Always-on transaction tracking. Count only explicit payment notices.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> handlePaymentMessage(message.getString()));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                handlePaymentMessage(message.getString()));
    }

    private static void handlePaymentMessage(String rawLine) {
        String line = rawLine.replaceAll("§[0-9A-FK-ORa-fk-or]", "")
                .replace('\u00a7', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (line.isEmpty() || TrackerStore.isDuplicate(line)) return;

        Matcher incoming = INCOMING.matcher(line);
        if (incoming.find()) {
            long cents = parseCents(incoming.group(1));
            if (cents > 0) TrackerStore.recordWin(cents, "Received: " + line);
            return;
        }

        Matcher outgoing = OUTGOING.matcher(line);
        if (outgoing.find()) {
            long cents = parseCents(outgoing.group(1));
            if (cents > 0) TrackerStore.recordLoss(cents, "Sent: " + line);
        }
    }

    private static long parseCents(String raw) {
        try {
            String value = raw.replace(",", "").replaceAll("\\s+", "").toLowerCase();
            BigDecimal amount;
            if (value.endsWith("k")) amount = new BigDecimal(value.substring(0, value.length() - 1)).movePointRight(3);
            else if (value.endsWith("m")) amount = new BigDecimal(value.substring(0, value.length() - 1)).movePointRight(6);
            else if (value.endsWith("b")) amount = new BigDecimal(value.substring(0, value.length() - 1)).movePointRight(9);
            else amount = new BigDecimal(value);
            return amount.movePointRight(2).longValueExact();
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
