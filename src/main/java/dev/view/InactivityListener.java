package dev.view;

import javax.swing.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Listener que cierra la sesión tras un período de inactividad.
 */
public class InactivityListener implements AWTEventListener {
    private final Timer timer;

    public InactivityListener(int timeoutSeconds, Runnable onInactivity) {
        timer = new Timer(timeoutSeconds * 1000, e -> onInactivity.run());
        timer.setRepeats(false);
        timer.start();
    }

    /** Cambia el tiempo de inactividad y reinicia la cuenta. */
    public void setTimeoutSeconds(int timeoutSeconds) {
        timer.setInitialDelay(timeoutSeconds * 1000);
        timer.restart();
    }

    @Override
    public void eventDispatched(java.awt.AWTEvent event) {
        // Cualquier evento de teclado o ratón reinicia el timer
        if (event instanceof KeyEvent || event instanceof MouseEvent) {
            timer.restart();
        }
    }
}
