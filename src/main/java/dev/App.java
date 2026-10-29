package dev;

import dev.controller.PasswordManager;
import dev.persistence.VaultException;
import dev.view.LoginDialog;
import dev.view.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 30_000;

    // Solo es una ayuda de usabilidad: quien tenga el .ckey puede atacarlo sin pasar por la aplicación.
    private static int failedAttempts;
    private static long lockedUntil;

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            logger.warn("No se pudo establecer Nimbus L&F, usando el predeterminado.");
        }

        SwingUtilities.invokeLater(() -> {
            try {
                showLogin();
            } catch (Exception e) {
                logger.error("Error fatal en la aplicación", e);
                JOptionPane.showMessageDialog(null, "Error fatal: " + e.getMessage());
                System.exit(1);
            }
        });
    }

    public static void showLogin() {
        while (true) {
            LoginDialog login = new LoginDialog(null);
            if (!login.showDialog()) {
                System.exit(0);
            }

            long remaining = lockedUntil - System.currentTimeMillis();
            if (remaining > 0) {
                login.clearPassword();
                JOptionPane.showMessageDialog(null, "Demasiados intentos. Espere " + (remaining / 1000 + 1) + " segundos.");
                continue;
            }

            char[] password = login.getPassword();
            login.clearPassword();
            PasswordManager manager = new PasswordManager(login.getSelectedFile());
            try {
                unlockWithProgress(manager, password);
            } catch (VaultException.InvalidPassword e) {
                if (++failedAttempts >= MAX_ATTEMPTS) {
                    failedAttempts = 0;
                    lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
                }
                JOptionPane.showMessageDialog(null, "Contraseña incorrecta");
                continue;
            } catch (VaultException e) {
                logger.warn("No se pudo abrir la bóveda", e);
                JOptionPane.showMessageDialog(null, e.getMessage());
                continue;
            } finally {
                Arrays.fill(password, (char) 0);
            }

            failedAttempts = 0;
            new MainFrame(manager).setVisible(true);
            return;
        }
    }

    /** Ejecuta la derivación de clave (lenta a propósito) fuera del hilo de eventos. */
    private static void unlockWithProgress(PasswordManager manager, char[] password) throws VaultException {
        JDialog wait = new JDialog((Frame) null, "CipherKey", true);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        wait.setLayout(new BorderLayout(10, 10));
        wait.add(new JLabel("Desbloqueando bóveda…", SwingConstants.CENTER), BorderLayout.NORTH);
        wait.add(bar, BorderLayout.CENTER);
        wait.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        wait.setSize(280, 100);
        wait.setLocationRelativeTo(null);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                manager.unlock(password);
                return null;
            }

            @Override
            protected void done() {
                wait.dispose();
            }
        };
        worker.execute();
        wait.setVisible(true); // bloquea hasta que done() cierra el diálogo

        try {
            worker.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof VaultException ve) {
                throw ve;
            }
            throw new VaultException("Error inesperado: " + e.getCause(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VaultException("Operación interrumpida", e);
        }
    }
}
