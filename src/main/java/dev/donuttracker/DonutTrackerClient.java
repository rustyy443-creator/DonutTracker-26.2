package dev.donuttracker;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
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
    private static final Pattern INCOMING_PLAYER = Pattern.compile(
            "(?i)\\b([A-Za-z0-9_]{1,16})\\s+paid\\s+you\\s+\\$?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?\\s*[kmb]?)\\b"
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
        ClientReceiveMessageEvents.MODIFY_GAME.register(DonutTrackerClient::modifyPaymentMessage);
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                handlePaymentMessage(message.getString()));
    }

    private static Component modifyPaymentMessage(Component message, boolean overlay) {
        String rawLine = message.getString();
        String line = rawLine.replaceAll("§[0-9A-FK-ORa-fk-or]", "")
                .replace('\u00a7', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (line.isEmpty() || overlay || TrackerStore.isDuplicate(line)) return message;

        Matcher incoming = INCOMING_PLAYER.matcher(line);
        if (incoming.find()) {
            String playerName = incoming.group(1);
            long cents = parseCents(incoming.group(2));
            if (cents <= 0) return message;
            TrackerStore.recordWin(cents, "Received: " + line);
            String doubleAmount = java.math.BigDecimal.valueOf(cents)
                    .multiply(java.math.BigDecimal.valueOf(2))
                    .movePointLeft(2).stripTrailingZeros().toPlainString();
            String tripleAmount = java.math.BigDecimal.valueOf(cents)
                    .multiply(java.math.BigDecimal.valueOf(3))
                    .movePointLeft(2).stripTrailingZeros().toPlainString();
            String originalAmount = java.math.BigDecimal.valueOf(cents)
                    .movePointLeft(2).stripTrailingZeros().toPlainString();
            Component returnButton = Component.literal(" [1X]")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.GREEN)
                            .withBold(true)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.RunCommand("/pay " + playerName + " " + originalAmount)));
            Component doubleButton = Component.literal(" [2X]")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.LIGHT_PURPLE)
                            .withBold(true)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.RunCommand("/pay " + playerName + " " + doubleAmount)));
            // Keep 3X well away from the other choices to reduce accidental clicks.
            Component tripleButton = Component.literal("                    [3X]")
                    .withStyle(style -> style
                            .withColor(ChatFormatting.GOLD)
                            .withBold(true)
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.RunCommand("/pay " + playerName + " " + tripleAmount)));
            if (TrackerStore.areChatPayButtonsEnabled()) {
                if (TrackerStore.isTriplePayButtonEnabled()) {
                    return message.copy().append(returnButton).append(doubleButton).append(tripleButton);
                }
                return message.copy().append(returnButton).append(doubleButton);
            }
            return message;
        }

        handlePaymentMessageAfterDuplicateCheck(line);
        return message;
    }

    private static void handlePaymentMessage(String rawLine) {
        String line = rawLine.replaceAll("§[0-9A-FK-ORa-fk-or]", "")
                .replace('\u00a7', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        if (line.isEmpty() || TrackerStore.isDuplicate(line)) return;
        handlePaymentMessageAfterDuplicateCheck(line);
    }

    private static void handlePaymentMessageAfterDuplicateCheck(String line) {

        Matcher incoming = INCOMING.matcher(line);
        if (incoming.find()) {
            long cents = parseCents(incoming.group(1));
            if (cents > 0) TrackerStore.recordWin(cents, "Received: " + line);
            return;
        }

        Matcher outgoing = OUTGOING.matcher(line);
        if (outgoing.find()) {
            long cents = parseCents(outgoing.group(1));
            if (cents > 0) {
                TrackerStore.recordLoss(cents, "Sent: " + line);
            }
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
