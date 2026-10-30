package dev.donuttracker;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Properties;

final class TrackerStore {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("donut-tracker.properties");
    private static long won;
    private static long lost;
    private static int wins;
    private static int losses;
    private static boolean autoDetect = true;
    private static boolean chatPayButtonsEnabled = true;
    private static String lastLine = "";
    private static long lastLineAt = 0L;
    private static final ArrayDeque<String> history = new ArrayDeque<>();

    private TrackerStore() {}

    static synchronized void load() {
        if (!Files.exists(FILE)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            won = Long.parseLong(p.getProperty("won", "0"));
            lost = Long.parseLong(p.getProperty("lost", "0"));
            wins = Integer.parseInt(p.getProperty("wins", "0"));
            losses = Integer.parseInt(p.getProperty("losses", "0"));
            autoDetect = Boolean.parseBoolean(p.getProperty("autoDetect", "true"));
            chatPayButtonsEnabled = Boolean.parseBoolean(p.getProperty("chatPayButtonsEnabled", "true"));
            for (int i = 0; i < 8; i++) {
                String item = p.getProperty("history." + i);
                if (item != null && !item.isBlank()) history.addLast(item);
            }
        } catch (Exception e) {
            System.err.println("[DonutTracker] Could not read local stats: " + e.getMessage());
        }
    }

    static synchronized void recordWin(long cents, String source) {
        if (cents <= 0) return;
        won += cents;
        wins++;
        addHistory("WIN  +$" + money(cents) + "  " + source);
        save();
    }

    static synchronized void recordLoss(long cents, String source) {
        if (cents <= 0) return;
        lost += cents;
        losses++;
        addHistory("LOSS -$" + money(cents) + "  " + source);
        save();
    }

    private static void addHistory(String line) {
        history.addFirst(line);
        while (history.size() > 8) history.removeLast();
    }

    static synchronized boolean isDuplicate(String line) {
        long now = System.currentTimeMillis();
        if (line.equals(lastLine) && now - lastLineAt < 1500) return true;
        lastLine = line;
        lastLineAt = now;
        return false;
    }

    static synchronized boolean isAutoDetectEnabled() { return autoDetect; }
    static synchronized boolean areChatPayButtonsEnabled() { return chatPayButtonsEnabled; }
    static synchronized void toggleChatPayButtons() { chatPayButtonsEnabled = !chatPayButtonsEnabled; save(); }
    static synchronized void toggleAutoDetect() { autoDetect = !autoDetect; save(); }
    static synchronized long getWon() { return won; }
    static synchronized long getLost() { return lost; }
    static synchronized int getWins() { return wins; }
    static synchronized int getLosses() { return losses; }
    static synchronized long getProfit() { return won - lost; }
    static synchronized java.util.List<String> getHistory() { return java.util.List.copyOf(history); }

    static String money(long cents) {
        return String.format(java.util.Locale.US, "%,.2f", cents / 100.0);
    }

    static synchronized void reset() {
        won = lost = 0;
        wins = losses = 0;
        history.clear();
        save();
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Properties p = new Properties();
            p.setProperty("won", Long.toString(won));
            p.setProperty("lost", Long.toString(lost));
            p.setProperty("wins", Integer.toString(wins));
            p.setProperty("losses", Integer.toString(losses));
            p.setProperty("autoDetect", Boolean.toString(autoDetect));
            p.setProperty("chatPayButtonsEnabled", Boolean.toString(chatPayButtonsEnabled));
            int i = 0;
            for (String item : history) p.setProperty("history." + i++, item);
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Donut Tracker local statistics - no account data");
            }
        } catch (Exception e) {
            System.err.println("[DonutTracker] Could not save local stats: " + e.getMessage());
        }
    }
}
