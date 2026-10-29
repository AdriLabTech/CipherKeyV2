package dev.view;

import javax.swing.*;
import java.awt.*;

/** Aviso breve y no modal, en la parte inferior de la ventana. Sustituye a los JOptionPane informativos. */
public final class Toast {
    private static final String KEY = "ck.toast";
    private static final int DURATION_MS = 2200;

    private Toast() {}

    public static void show(Component anchor, String message) {
        JRootPane root = SwingUtilities.getRootPane(anchor);
        if (root == null) return;
        JLayeredPane layers = root.getLayeredPane();

        Object previous = layers.getClientProperty(KEY);
        if (previous instanceof Component old) {
            layers.remove(old);
        }

        JPanel toast = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x262B33));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        toast.setOpaque(false);
        JLabel text = new JLabel(message, Icons.colored("check", 16, () -> new Color(0x3CCF91)), SwingConstants.LEADING);
        text.setIconTextGap(8);
        text.setForeground(Color.WHITE);
        toast.add(text);

        Dimension size = toast.getPreferredSize();
        size.width += 12;
        toast.setBounds((layers.getWidth() - size.width) / 2, layers.getHeight() - size.height - 28, size.width, size.height);
        layers.add(toast, JLayeredPane.POPUP_LAYER);
        layers.putClientProperty(KEY, toast);
        layers.repaint();

        Timer timer = new Timer(DURATION_MS, e -> {
            layers.remove(toast);
            layers.repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }
}
