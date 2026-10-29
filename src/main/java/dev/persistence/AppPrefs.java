package dev.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/** Preferencias de la aplicación (tema, bóvedas recientes, bloqueo). No contiene datos sensibles. */
public final class AppPrefs {
    private static final int MAX_RECENT = 6;
    private static final String SEPARATOR = "\n";

    private static Preferences node = Preferences.userRoot().node("dev/cipherkey");

    private AppPrefs() {}

    /** Permite a las pruebas usar un nodo aislado. */
    public static void useNode(Preferences testNode) {
        node = testNode;
    }

    public static boolean isDark() {
        return node.getBoolean("dark", false);
    }

    public static void setDark(boolean dark) {
        node.putBoolean("dark", dark);
    }

    public static int autoLockMinutes() {
        return Math.max(1, node.getInt("autoLockMinutes", 5));
    }

    public static void setAutoLockMinutes(int minutes) {
        node.putInt("autoLockMinutes", minutes);
    }

    /** Tamaño guardado de la ventana principal, o {@code null} si no hay. */
    public static java.awt.Dimension windowSize() {
        int w = node.getInt("windowWidth", 0), h = node.getInt("windowHeight", 0);
        return w > 0 && h > 0 ? new java.awt.Dimension(w, h) : null;
    }

    public static void setWindowSize(int width, int height) {
        node.putInt("windowWidth", width);
        node.putInt("windowHeight", height);
    }

    public static boolean isWindowMaximized() {
        return node.getBoolean("windowMaximized", false);
    }

    public static void setWindowMaximized(boolean maximized) {
        node.putBoolean("windowMaximized", maximized);
    }

    /** Bóvedas abiertas recientemente, la más reciente primero. */
    public static List<Path> recentVaults() {
        String raw = node.get("recentVaults", "");
        List<Path> result = new ArrayList<>();
        for (String s : raw.split(SEPARATOR)) {
            if (!s.isBlank()) result.add(Paths.get(s));
        }
        return result;
    }

    public static void addRecentVault(Path vault) {
        Path normalized = vault.toAbsolutePath().normalize();
        List<Path> list = recentVaults();
        list.remove(normalized);
        list.add(0, normalized);
        StringBuilder sb = new StringBuilder();
        for (Path p : list.subList(0, Math.min(MAX_RECENT, list.size()))) {
            if (sb.length() > 0) sb.append(SEPARATOR);
            sb.append(p);
        }
        node.put("recentVaults", sb.toString());
    }
}
