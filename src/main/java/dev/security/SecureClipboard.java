package dev.security;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Gestor del portapapeles con limpieza automática después de un tiempo.
 * Solo borra si el portapapeles sigue conteniendo lo que copiamos nosotros.
 */
public class SecureClipboard {
    private static final int CLEAR_DELAY = 30_000; // 30 segundos
    private static Timer timer;

    public static synchronized void copyToClipboard(String text) {
        if (timer != null) {
            timer.cancel();
        }

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);

        timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                clearIfUnchanged(clipboard, text);
            }
        }, CLEAR_DELAY);
    }

    private static void clearIfUnchanged(Clipboard clipboard, String copied) {
        try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
                    && copied.equals(clipboard.getData(DataFlavor.stringFlavor))) {
                clipboard.setContents(new StringSelection(""), null);
            }
        } catch (Exception ignored) {
            // el portapapeles pudo cambiar o no estar disponible; no hay nada que limpiar
        }
    }
}
