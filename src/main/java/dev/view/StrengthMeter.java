package dev.view;

import dev.security.PasswordStrength;

import javax.swing.*;
import java.awt.*;

/** Barra + etiqueta con la fuerza estimada de una contraseña. */
public class StrengthMeter extends JPanel {
    private final JProgressBar bar = new JProgressBar(0, 100);
    private final JLabel label = new JLabel(" ");

    public StrengthMeter() {
        super(new BorderLayout(10, 0));
        setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 6));
        bar.setBorderPainted(false);
        label.putClientProperty("FlatLaf.styleClass", "small");
        JPanel barWrap = new JPanel(new GridBagLayout());
        barWrap.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        barWrap.add(bar, gbc);
        add(barWrap, BorderLayout.CENTER);
        label.setPreferredSize(new Dimension(86, label.getPreferredSize().height));
        add(label, BorderLayout.EAST);
    }

    public void update(char[] password) {
        update(new String(password));
    }

    public void update(String password) {
        if (password == null || password.isEmpty()) {
            bar.setValue(0);
            label.setText(" ");
            return;
        }
        PasswordStrength.Result r = PasswordStrength.evaluate(password);
        bar.setValue(Math.max(6, (int) Math.min(100, r.bits() * 100 / 90)));
        bar.setForeground(colorFor(r.level()));
        label.setText(r.level().label());
        label.setForeground(colorFor(r.level()));
    }

    private static Color colorFor(PasswordStrength.Level level) {
        return switch (level) {
            case VERY_WEAK, WEAK -> UIManager.getColor("App.danger");
            case FAIR -> UIManager.getColor("App.warning");
            case STRONG, VERY_STRONG -> UIManager.getColor("App.success");
        };
    }
}
